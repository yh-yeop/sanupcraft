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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.fluid.FluidHolder;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.fluid.TankBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 보일러: 연료를 태우는 동안 물을 증기로 바꾸고, 파이프로 이어진 기계에 나눠 준다.
 * 연료 칸 하나 (깔때기로도 넣을 수 있음). 옆에 물 원천이 있으면 물이 계속 찬다.
 * 물은 {@link FluidHolder}로도 받는다: 옆의 펌프가 밀어 넣고, 옆의 물 탱크에서는 보일러가 틱당 100까지 끌어온다.
 * 증기를 실제로 만든 틱에만 연료가 탄다 (증기가 가득하거나 물이 없으면 불이 붙은 채로 기다린다).
 * 대형 보일러 제어기가 이 클래스를 물려받아 용량 · 틱당 증기 · 연료 칸 수와 증기 내보내기만 바꾼다.
 */
public class BoilerBlockEntity extends BlockEntity implements WorldlyContainer, SteamStorage, FluidHolder {
	public static final int WATER_CAPACITY = 8000;
	public static final int STEAM_CAPACITY = 1000;
	/** 타는 동안 틱마다 만드는 증기 (= 쓰는 물). 1단계 기계 4대를 돌릴 수 있다. */
	public static final int STEAM_PER_TICK = 4;
	public static final int BUCKET = 1000;
	private static final int WATER_FROM_SOURCE_PER_TICK = 100;
	/** 증기를 못 만든 채 이 틱이 지나면 LIT를 끈다 (연료는 그대로 남아 있음). */
	private static final int LIT_HOLD_TICKS = 10;
	private final int[] slots;

	private NonNullList<ItemStack> items;
	private int water;
	private int steam;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private int litTime;
	private int litDuration;
	/** 증기를 만들지 못한 연속 틱 수 (저장하지 않음). */
	private int idleTicks = LIT_HOLD_TICKS;
	private final SteamOutlet outlet = new SteamOutlet();

	public BoilerBlockEntity(BlockPos pos, BlockState state) {
		this(ModPower.BOILER_BLOCK_ENTITY, pos, state, 1);
	}

	/** 물려받는 블록 엔티티용 (대형 보일러 제어기): 연료 칸 수를 정한다. */
	protected BoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int fuelSlots) {
		super(type, pos, state);
		items = NonNullList.withSize(fuelSlots, ItemStack.EMPTY);
		slots = new int[fuelSlots];
		for (int i = 0; i < fuelSlots; i++) {
			slots[i] = i;
		}
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
		boiler.tick(level, pos, state);
	}

	/** 한 틱: 물 받기 → 연료 태우기 → 증기 만들기 → 내보내기 → LIT 맞추기. */
	protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
		if (touchesWaterSource(level, pos)) {
			water = Math.min(waterCapacity(), water + WATER_FROM_SOURCE_PER_TICK);
		} else {
			drawFromTanks(level, pos);
		}
		boolean running = canRun();
		ItemStack fuel = nextFuel();
		if (running && litTime <= 0 && !fuel.isEmpty() && water > 0 && steam < steamCapacity()) {
			int burn = burnDuration(level, this, fuel);
			if (burn > 0) {
				litTime = burn;
				litDuration = burn;
				ItemStack remainder = MachineBlockEntity.consumeFuel(fuel);
				if (!remainder.isEmpty()) {
					// 용암 양동이 → 빈 양동이: 보일러에는 빼낼 칸이 없으니 위에 떨군다
					Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remainder);
				}
			}
		}
		int made = 0;
		if (running && litTime > 0) {
			made = Math.min(steamPerTick(), Math.min(steamCapacity() - steam, water));
			if (made > 0) {
				steam += made;
				flow.add(level.getGameTime(), made);
				water -= made;
				litTime = Math.max(0, litTime - burnPerTick());
			}
		}
		idleTicks = made > 0 ? 0 : Math.min(idleTicks + 1, LIT_HOLD_TICKS);
		steam = distributeSteam(level, pos, steam);

		boolean isLit = isLit();
		if (state.getValue(BoilerBlock.LIT) != isLit) {
			level.setBlockAndUpdate(pos, state.setValue(BoilerBlock.LIT, isLit));
		}
		setChanged(level, pos, state);
	}

	/** 다음에 태울 연료 (앞 칸부터). 없으면 빈 스택. */
	private ItemStack nextFuel() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** 지금 불을 붙이고 증기를 만들어도 되는가. 대형 보일러는 모양이 맞을 때만. */
	protected boolean canRun() {
		return true;
	}

	/** 틱당 만드는 증기 (= 쓰는 물). */
	protected int steamPerTick() {
		return STEAM_PER_TICK;
	}

	/** 증기를 만든 틱마다 줄어드는 연소 틱. */
	protected int burnPerTick() {
		return 1;
	}

	public int waterCapacity() {
		return WATER_CAPACITY;
	}

	/** 증기를 나눠 주고 남은 양을 돌려준다. */
	protected int distributeSteam(ServerLevel level, BlockPos pos, int steam) {
		return outlet.distribute(level, pos, steam);
	}

	/**
	 * 손에 든 연료를 넣는다 (빈 칸 또는 같은 연료 칸에, 앞 칸부터).
	 * 받을 칸이 하나도 없으면 false (다른 연료가 차 있음). 같은 연료가 꽉 찼을 때는 그대로 true.
	 */
	public boolean insertFuel(ItemStack stack) {
		boolean accepted = false;
		for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
			ItemStack current = items.get(i);
			if (current.isEmpty()) {
				items.set(i, stack.split(stack.getCount()));
				accepted = true;
			} else if (ItemStack.isSameItemSameComponents(current, stack)) {
				int move = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
				current.grow(move);
				stack.shrink(move);
				accepted = true;
			}
		}
		setChanged();
		return accepted;
	}

	/** 옆의 물 탱크에서 틱당 {@link #WATER_FROM_SOURCE_PER_TICK}까지 끌어온다. 끌어온 양을 돌려준다. */
	public int drawFromTanks(ServerLevel level, BlockPos pos) {
		int want = Math.min(WATER_FROM_SOURCE_PER_TICK, waterCapacity() - water);
		int drawn = 0;
		for (Direction direction : Direction.values()) {
			if (drawn >= want) {
				break;
			}
			if (level.getBlockEntity(pos.relative(direction)) instanceof TankBlockEntity tank) {
				drawn += tank.drain(FluidKind.WATER, want - drawn, false);
			}
		}
		water += drawn;
		return drawn;
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
		if (water + BUCKET > waterCapacity()) {
			return false;
		}
		water += BUCKET;
		setChanged();
		return true;
	}

	// ---- 물 (FluidHolder: 물만 받고, 빼 가지는 못한다) ----

	@Override
	public @Nullable FluidKind fluid() {
		return water > 0 ? FluidKind.WATER : null;
	}

	@Override
	public int fluidAmount() {
		return water;
	}

	@Override
	public int fluidCapacity() {
		return waterCapacity();
	}

	@Override
	public int fill(FluidKind kind, int amount, boolean simulate) {
		if (kind != FluidKind.WATER || amount <= 0) {
			return 0;
		}
		int accepted = Math.min(amount, waterCapacity() - water);
		if (accepted > 0 && !simulate) {
			water += accepted;
			setChanged();
		}
		return accepted;
	}

	@Override
	public int drain(FluidKind kind, int amount, boolean simulate) {
		return 0;
	}

	public int water() {
		return water;
	}

	public int steam() {
		return steam;
	}

	@Override
	public int steamStored() {
		return steam;
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	/** 보일러는 만든 양을 센다. */
	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(level != null ? level.getGameTime() : 0L);
	}

	/** 불이 켜져 있는가: 타는 중이고 최근에 증기를 만들었을 때 (멈춘 채 기다리는 동안은 꺼진 모습). */
	public boolean isLit() {
		return litTime > 0 && idleTicks < LIT_HOLD_TICKS;
	}

	/** 남은 연소 틱 (멈춘 채 기다리는 동안에도 남아 있다). */
	public int litTime() {
		return litTime;
	}

	/** 연료 칸에 든 개수 (칸이 여럿이면 합). */
	public int fuelCount() {
		int count = 0;
		for (ItemStack stack : items) {
			count += stack.getCount();
		}
		return count;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(items.size(), ItemStack.EMPTY);
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
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return fuelCount() == 0;
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
		return slots;
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
