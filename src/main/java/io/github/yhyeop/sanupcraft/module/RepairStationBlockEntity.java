package io.github.yhyeop.sanupcraft.module;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 증기 수리대 (G4). 칸 둘: 장비 · 재료 (그 장비의 수리 재료 — 아이템의 {@code repairable} 컴포넌트, 우리 장비는 {@code #sanupcraft:repairs_…} 태그).
 * 재료 하나를 넣으면 최대 내구도의 25%만큼 "수리할 몫"이 생기고, 틱마다 증기 4를 쓰며 몫에서 조금씩 (빈 장비가 10초에 다 차는 빠르기) 내구도를 채운다.
 * 장비는 그대로 두고 닳은 정도만 고치므로 마법 부여 · 모듈 · 이름이 그대로다. 수리 비용(모루)도 늘지 않는다.
 * 장비를 꺼내면 남은 몫은 사라진다 (재료 하나치 안쪽). 깔때기: 위로 재료(장비가 올라간 뒤 그 장비의 재료만), 옆으로 장비를 넣고, 아래로는 다 고친 장비만 빠진다.
 */
public class RepairStationBlockEntity extends BlockEntity implements WorldlyContainer, SteamConsumer, SteamStorage {
	public static final int SLOT_GEAR = 0;
	public static final int SLOT_MATERIAL = 1;
	public static final int STEAM_CAPACITY = 1000;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int STEAM_PER_TICK = 4;
	public static final int TICKS_FOR_FULL = 200;
	public static final int MATERIAL_PERCENT = 25;
	private static final int[] TOP = {SLOT_MATERIAL};
	private static final int[] SIDES = {SLOT_GEAR};
	private static final int[] BOTTOM = {SLOT_GEAR};

	private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private int steam;
	/** 재료로 받아 두고 아직 채우지 않은 내구도. */
	private int pool;

	public RepairStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModModules.REPAIR_STATION_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, RepairStationBlockEntity station) {
		if (station.repairTick() > 0 && level.getGameTime() % 6 == 0) {
			level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 1, 0.15, 0.0, 0.15, 0.01);
		}
	}

	/** 한 틱 수리. 채운 내구도를 돌려준다. 시험이 직접 부른다. */
	public int repairTick() {
		ItemStack gear = items.get(SLOT_GEAR);
		if (gear.isEmpty() || !gear.isDamageableItem() || !gear.isDamaged()) {
			return 0;
		}
		SanupcraftConfig config = SanupcraftConfig.current();
		ItemStack material = items.get(SLOT_MATERIAL);
		if (pool <= 0 && !material.isEmpty() && gear.isValidRepairItem(material)) {
			material.shrink(1);
			pool += Math.max(1, gear.getMaxDamage() * config.repairMaterialPercent / 100);
			setChanged();
		}
		if (pool <= 0 || steam < config.repairSteamPerTick) {
			return 0;
		}
		int rate = Math.max(1, (gear.getMaxDamage() + config.repairTicksForFull - 1) / config.repairTicksForFull);
		int amount = Math.min(rate, Math.min(pool, gear.getDamageValue()));
		gear.setDamageValue(gear.getDamageValue() - amount);
		pool -= amount;
		steam -= config.repairSteamPerTick;
		setChanged();
		return amount;
	}

	public ItemStack gear() {
		return items.get(SLOT_GEAR);
	}

	public ItemStack material() {
		return items.get(SLOT_MATERIAL);
	}

	/** 이 장비를 이 수리대에 올릴 수 있나 (내구도가 있고 수리 재료가 정해진 것). */
	public static boolean accepts(ItemStack stack) {
		return stack.isDamageableItem() && stack.has(net.minecraft.core.component.DataComponents.REPAIRABLE);
	}

	/** 상태 줄: 장비 · 내구도 · 재료 · 증기. */
	public Component status() {
		ItemStack gear = gear();
		if (gear.isEmpty()) {
			return Component.translatable("block.sanupcraft.steam_repair_station.status_empty", steam, STEAM_CAPACITY);
		}
		int durability = gear.getMaxDamage() - gear.getDamageValue();
		return Component.translatable("block.sanupcraft.steam_repair_station.status", gear.getHoverName(), durability, gear.getMaxDamage(),
				material().getCount(), steam, STEAM_CAPACITY);
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

	// ---- 칸 ----

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return direction == Direction.UP ? TOP : direction == Direction.DOWN ? BOTTOM : SIDES;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == SLOT_GEAR) {
			return accepts(stack);
		}
		ItemStack gear = items.get(SLOT_GEAR);
		return gear.isEmpty() || gear.isValidRepairItem(stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		// 깔때기는 장비가 올라간 뒤에 그 장비의 재료만 넣는다 (아무거나 끼면 칸이 막혀 자동화가 멈춘다). 손으로 넣는 길(canPlaceItem)은 그대로.
		if (slot == SLOT_GEAR) {
			return items.get(SLOT_GEAR).isEmpty() && accepts(stack);
		}
		ItemStack gear = items.get(SLOT_GEAR);
		return !gear.isEmpty() && gear.isValidRepairItem(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot == SLOT_GEAR && direction == Direction.DOWN && !stack.isDamaged();
	}

	@Override
	public int getContainerSize() {
		return 2;
	}

	@Override
	public boolean isEmpty() {
		return items.get(0).isEmpty() && items.get(1).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
		if (!removed.isEmpty()) {
			if (slot == SLOT_GEAR) {
				pool = 0;
			}
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot == SLOT_GEAR) {
			pool = 0;
		}
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == SLOT_GEAR && !ItemStack.isSameItemSameComponents(items.get(SLOT_GEAR), stack)) {
			pool = 0;
		}
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return stack.isDamageableItem() ? 1 : Math.min(64, stack.getMaxStackSize());
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.set(0, ItemStack.EMPTY);
		items.set(1, ItemStack.EMPTY);
		pool = 0;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.set(0, ItemStack.EMPTY);
		items.set(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		steam = Math.max(0, Math.min(STEAM_CAPACITY, input.getIntOr("steam", 0)));
		pool = Math.max(0, input.getIntOr("repair_pool", 0));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("steam", steam);
		output.putInt("repair_pool", pool);
	}
}
