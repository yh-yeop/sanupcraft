package io.github.yhyeop.sanupcraft.power;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 보일러: 연료를 태우는 동안 물을 증기로 바꾸고, 파이프로 이어진 기계에 나눠 준다.
 * 연료 칸 하나 (깔때기로도 넣을 수 있음). 옆에 물 원천이 있으면 물이 계속 찬다.
 * 증기를 실제로 만든 틱에만 연료가 탄다 (증기가 가득하거나 물이 없으면 불이 붙은 채로 기다린다).
 */
public class BoilerBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int WATER_CAPACITY = 8000;
	public static final int STEAM_CAPACITY = 1000;
	/** 타는 동안 틱마다 만드는 증기 (= 쓰는 물). 1단계 기계 4대를 돌릴 수 있다. */
	public static final int STEAM_PER_TICK = 4;
	public static final int BUCKET = 1000;
	private static final int WATER_FROM_SOURCE_PER_TICK = 100;
	/** 증기를 못 만든 채 이 틱이 지나면 LIT를 끈다 (연료는 그대로 남아 있음). */
	private static final int LIT_HOLD_TICKS = 10;
	private static final int[] SLOTS = {0};

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private int water;
	private int steam;
	private int litTime;
	private int litDuration;
	/** 증기를 만들지 못한 연속 틱 수 (저장하지 않음). */
	private int idleTicks = LIT_HOLD_TICKS;
	private final SteamOutlet outlet = new SteamOutlet();

	public BoilerBlockEntity(BlockPos pos, BlockState state) {
		super(ModPower.BOILER_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
		if (boiler.touchesWaterSource(level, pos)) {
			boiler.water = Math.min(WATER_CAPACITY, boiler.water + WATER_FROM_SOURCE_PER_TICK);
		}
		ItemStack fuel = boiler.items.get(0);
		if (boiler.litTime <= 0 && !fuel.isEmpty() && boiler.water > 0 && boiler.steam < STEAM_CAPACITY) {
			int burn = burnDuration(level, boiler, fuel);
			if (burn > 0) {
				boiler.litTime = burn;
				boiler.litDuration = burn;
				ItemStack remainder = MachineBlockEntity.consumeFuel(fuel);
				if (!remainder.isEmpty()) {
					// 용암 양동이 → 빈 양동이: 보일러에는 빼낼 칸이 없으니 위에 떨군다
					Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remainder);
				}
			}
		}
		int made = 0;
		if (boiler.litTime > 0) {
			made = Math.min(STEAM_PER_TICK, Math.min(STEAM_CAPACITY - boiler.steam, boiler.water));
			if (made > 0) {
				boiler.steam += made;
				boiler.water -= made;
				boiler.litTime--;
			}
		}
		boiler.idleTicks = made > 0 ? 0 : Math.min(boiler.idleTicks + 1, LIT_HOLD_TICKS);
		boiler.steam = boiler.outlet.distribute(level, pos, boiler.steam);

		boolean isLit = boiler.isLit();
		if (state.getValue(BoilerBlock.LIT) != isLit) {
			level.setBlockAndUpdate(pos, state.setValue(BoilerBlock.LIT, isLit));
		}
		setChanged(level, pos, state);
	}

	private boolean touchesWaterSource(ServerLevel level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			var fluid = level.getFluidState(pos.relative(direction));
			if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
				return true;
			}
		}
		return false;
	}

	private static int burnDuration(ServerLevel level, BoilerBlockEntity boiler, ItemStack fuel) {
		LootContext context = new LootContext.Builder(new LootParams.Builder(level)
				.withParameter(LootContextParams.BLOCK_STATE, boiler.getBlockState())
				.withParameter(LootContextParams.BLOCK_ENTITY, boiler)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(boiler.getBlockPos()))
				.withParameter(LootContextParams.CONTAINER, boiler)
				.create(LootContextParamSets.CONTAINER_PROCESS))
				.create(Optional.empty());
		return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
	}

	/** 물 양동이 하나를 붓는다. 다 들어가면 true. */
	public boolean pourBucket() {
		if (water + BUCKET > WATER_CAPACITY) {
			return false;
		}
		water += BUCKET;
		setChanged();
		return true;
	}

	public int water() {
		return water;
	}

	public int steam() {
		return steam;
	}

	/** 불이 켜져 있는가: 타는 중이고 최근에 증기를 만들었을 때 (멈춘 채 기다리는 동안은 꺼진 모습). */
	public boolean isLit() {
		return litTime > 0 && idleTicks < LIT_HOLD_TICKS;
	}

	/** 남은 연소 틱 (멈춘 채 기다리는 동안에도 남아 있다). */
	public int litTime() {
		return litTime;
	}

	public int fuelCount() {
		return items.get(0).getCount();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		water = input.getIntOr("water", 0);
		steam = input.getIntOr("steam", 0);
		litTime = input.getIntOr("lit_time", 0);
		litDuration = input.getIntOr("lit_duration", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("water", water);
		output.putInt("steam", steam);
		output.putInt("lit_time", litTime);
		output.putInt("lit_duration", litDuration);
	}

	// ---- 연료 칸 (깔때기 · 우클릭) ----

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
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
		setChanged();
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return stack.has(DataComponents.COOKING_FUEL);
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return false;
	}
}
