package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;

/**
 * 증기 충전대. 증기망(파이프)에서 받은 증기를 안에 조금 모아 두었다가, 한 칸 짐칸의 증기 통 · 증기 부츠 같은
 * "증기를 담는 아이템"에 틱마다 채운다 (기본 20/틱). 가득 차면 깔때기로 뽑을 수 있다 (넣기는 빈 칸일 때만).
 */
public class SteamChargerBlockEntity extends BlockEntity implements WorldlyContainer, SteamConsumer, SteamStorage {
	public static final int STEAM_CAPACITY = 200;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int TRANSFER_PER_TICK = 20;
	private static final int[] SLOTS = {0};

	private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private int steam;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	/** 이 틱에 채웠나 (연기 표시용, 저장하지 않음). */
	private boolean charging;

	public SteamChargerBlockEntity(BlockPos pos, BlockState state) {
		super(ModPower.STEAM_CHARGER_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SteamChargerBlockEntity charger) {
		int moved = charger.charge();
		if (moved > 0 && level.getGameTime() % 8 == 0) {
			level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 1, 0.1, 0.0, 0.1, 0.01);
		}
	}

	/** 한 틱: 모아 둔 증기를 칸의 아이템에 채운다. 채운 양을 돌려준다. 시험이 직접 부른다. */
	public int charge() {
		ItemStack stack = items.get(0);
		int moved = 0;
		if (steam > 0 && SteamCharge.holdsSteam(stack)) {
			int rate = SanupcraftConfig.current().chargerSteamPerTick;
			moved = SteamCharge.fill(stack, Math.min(steam, rate));
			if (moved > 0) {
				steam -= moved;
				setChanged();
			}
		}
		charging = moved > 0;
		return moved;
	}

	public boolean isCharging() {
		return charging;
	}

	public ItemStack stored() {
		return items.get(0);
	}

	// ---- 증기망 ----

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.max(0, Math.min(amount, STEAM_CAPACITY - steam));
		steam += accepted;
		if (accepted > 0) {
			flow.add(level != null ? level.getGameTime() : 0L, accepted);
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

	// ---- 칸 (깔때기: 빈 칸에 증기 아이템만 넣고, 가득 차면 뽑는다) ----

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return SteamCharge.holdsSteam(stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return items.get(0).isEmpty() && SteamCharge.holdsSteam(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return SteamCharge.isFull(stack);
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return items.get(0).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(0);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, 0, amount);
		if (!removed.isEmpty()) {
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, 0);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(0, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.set(0, ItemStack.EMPTY);
	}

	/** 부서질 때 칸의 아이템을 떨군다. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel serverLevel) {
			Containers.dropContents(serverLevel, pos, this);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.set(0, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		steam = Math.max(0, Math.min(STEAM_CAPACITY, input.getIntOr("steam", 0)));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("steam", steam);
	}
}
