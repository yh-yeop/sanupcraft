package io.github.yhyeop.sanupcraft.storage;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 창고 단말 · 제작 단말. 이어진 창고 네트워크 전체를 한 화면으로 보여 준다.
 * 화면이 열려 있는 동안 증기를 틱당 1 쓰고(설정), 증기가 없으면 열리지 않는다 (열린 화면도 닫힌다).
 * 주인 잠금은 엔더 운반기와 같다. 휴대 단말도 이 블록을 통해 연다.
 */
public class TerminalBlockEntity extends BlockEntity implements StorageNetwork.Member, SteamConsumer, SteamStorage,
		ExtendedMenuProvider<TerminalMenu.OpenData> {
	public static final int STEAM_CAPACITY = 1000;
	public static final int STEAM_PER_TICK = 1;

	private final OwnerLock lock = new OwnerLock();
	private final StorageNetwork.Cache network = new StorageNetwork.Cache();
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private int steam;
	private int users;

	public TerminalBlockEntity(BlockPos pos, BlockState state) {
		super(ModStorage.TERMINAL_BLOCK_ENTITY, pos, state);
	}

	public boolean isCrafting() {
		return getBlockState().getBlock() instanceof TerminalBlock terminal && terminal.isCrafting();
	}

	public OwnerLock lock() {
		return lock;
	}

	public StorageNetwork network() {
		return network.get(level, worldPosition);
	}

	/** 쓸 증기가 있는가 (설정이 0이면 늘 true). */
	public boolean hasPower() {
		int cost = SanupcraftConfig.current().terminalSteamPerTick;
		return cost <= 0 || steam >= cost;
	}

	void addUser() {
		users++;
	}

	void removeUser() {
		users = Math.max(0, users - 1);
	}

	public int users() {
		return users;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TerminalBlockEntity terminal) {
		if (terminal.users > 0) {
			int cost = SanupcraftConfig.current().terminalSteamPerTick;
			if (cost > 0) {
				terminal.steam = Math.max(0, terminal.steam - cost);
				terminal.setChanged();
			}
		}
	}

	/** 잠금 · 증기를 확인하고 연다. 못 열면 까닭을 알린다. */
	public boolean tryOpen(Player player, net.minecraft.world.MenuProvider provider) {
		if (!lock.canUse(player)) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.storage_terminal.locked", lock.ownerName()));
			return false;
		}
		if (!hasPower()) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.storage_terminal.no_steam"));
			return false;
		}
		player.openMenu(provider);
		return true;
	}

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.max(0, Math.min(amount, STEAM_CAPACITY - steam));
		steam += accepted;
		flow.add(level != null ? level.getGameTime() : 0L, accepted);
		if (accepted > 0) {
			setChanged();
		}
		return accepted;
	}

	@Override
	public int steamStored() {
		return steam;
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(level != null ? level.getGameTime() : 0L);
	}

	// ---- 화면 (블록을 직접 열 때) ----

	@Override
	public TerminalMenu.OpenData getScreenOpeningData(ServerPlayer player) {
		return new TerminalMenu.OpenData(isCrafting());
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new TerminalMenu(containerId, inventory, this, p -> Container.stillValidBlockEntity(this, p) && hasPower() && lock.canUse(p));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		steam = input.getIntOr("steam", 0);
		lock.load(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("steam", steam);
		lock.save(output);
	}

	/** 테스트용: 서버 월드 (없으면 null). */
	ServerLevel serverLevel() {
		return level instanceof ServerLevel server ? server : null;
	}
}
