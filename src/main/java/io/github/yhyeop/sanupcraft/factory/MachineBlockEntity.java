package io.github.yhyeop.sanupcraft.factory;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import io.github.yhyeop.sanupcraft.power.SteamConsumer;

/**
 * 기계의 칸과 진행 상태. 화로와 같은 규칙: 연료를 태우는 동안 재료를 하나씩 처리한다.
 * 깔때기: 위 → 재료, 옆 → 연료, 아래 ← 결과.
 * 증기 파이프가 이어져 있으면 연료 대신 증기로 돈다 (증기가 있으면 연료를 태우지 않음).
 */
public class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, SteamConsumer {
	public static final int SLOT_INPUT = 0;
	public static final int SLOT_FUEL = 1;
	public static final int SLOT_RESULT = 2;
	public static final int DATA_LIT_TIME = 0;
	public static final int DATA_LIT_DURATION = 1;
	public static final int DATA_PROGRESS = 2;
	public static final int DATA_TOTAL_TIME = 3;
	public static final int DATA_COUNT = 4;
	public static final int STEAM_CAPACITY = 200;
	/** 증기로 돌 때 틱마다 쓰는 증기. */
	public static final int STEAM_PER_TICK = 1;

	private static final int[] SLOTS_FOR_UP = {SLOT_INPUT};
	private static final int[] SLOTS_FOR_DOWN = {SLOT_RESULT};
	private static final int[] SLOTS_FOR_SIDES = {SLOT_FUEL};

	private final MachineKind kind;
	private final RecipeManager.CachedCheck<SingleRecipeInput, MachineRecipe> quickCheck;
	private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
	private int litTime;
	private int litDuration;
	private int progress;
	private int totalTime;
	private int steam;
	private boolean steamWorking;

	private final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int dataId) {
			return switch (dataId) {
				case DATA_LIT_TIME -> steamWorking ? 1 : litTime;
				case DATA_LIT_DURATION -> steamWorking ? 1 : litDuration;
				case DATA_PROGRESS -> progress;
				case DATA_TOTAL_TIME -> totalTime;
				default -> 0;
			};
		}

		@Override
		public void set(int dataId, int value) {
			switch (dataId) {
				case DATA_LIT_TIME -> litTime = value;
				case DATA_LIT_DURATION -> litDuration = value;
				case DATA_PROGRESS -> progress = value;
				case DATA_TOTAL_TIME -> totalTime = value;
				default -> {
				}
			}
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public MachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModMachines.MACHINE_BLOCK_ENTITY, pos, state);
		this.kind = ((MachineBlock) state.getBlock()).kind();
		this.quickCheck = RecipeManager.createCheck(kind.recipeType());
	}

	public MachineKind kind() {
		return kind;
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
		boolean wasLit = machine.litTime > 0 || machine.steamWorking;
		boolean changed = false;
		machine.steamWorking = false;
		if (wasLit) {
			machine.litTime--;
		}

		ItemStack input = machine.items.get(SLOT_INPUT);
		ItemStack fuel = machine.items.get(SLOT_FUEL);
		RecipeHolder<MachineRecipe> recipe = input.isEmpty() ? null
				: machine.quickCheck.getRecipeFor(new SingleRecipeInput(input), level).orElse(null);
		ItemStack output = recipe == null ? ItemStack.EMPTY : recipe.value().output().create();
		boolean canWork = recipe != null && machine.canOutput(output);

		if (canWork) {
			machine.totalTime = recipe.value().time();
			if (machine.litTime <= 0 && machine.steam >= STEAM_PER_TICK) {
				machine.steam -= STEAM_PER_TICK;
				machine.steamWorking = true;
			} else if (machine.litTime <= 0 && !fuel.isEmpty()) {
				int burn = machine.burnDuration(level, fuel);
				if (burn > 0) {
					machine.litTime = burn;
					machine.litDuration = burn;
					fuel.shrink(1);
					changed = true;
				}
			}
			if (machine.litTime > 0 || machine.steamWorking) {
				machine.progress++;
				if (machine.progress >= machine.totalTime) {
					machine.progress = 0;
					machine.finish(input, output);
					changed = true;
				}
			} else {
				machine.progress = Mth.clamp(machine.progress - 2, 0, machine.totalTime);
			}
		} else if (machine.progress > 0) {
			machine.progress = Mth.clamp(machine.progress - 2, 0, machine.totalTime);
		}

		boolean isLit = machine.litTime > 0 || machine.steamWorking;
		if (wasLit != isLit) {
			state = state.setValue(MachineBlock.LIT, isLit);
			level.setBlockAndUpdate(pos, state);
			changed = true;
		}
		if (changed) {
			setChanged(level, pos, state);
		}
	}

	private boolean canOutput(ItemStack output) {
		if (output.isEmpty()) {
			return false;
		}
		ItemStack current = items.get(SLOT_RESULT);
		if (current.isEmpty()) {
			return true;
		}
		return ItemStack.isSameItemSameComponents(current, output)
				&& current.getCount() + output.getCount() <= Math.min(getMaxStackSize(), output.getMaxStackSize());
	}

	private void finish(ItemStack input, ItemStack output) {
		ItemStack current = items.get(SLOT_RESULT);
		if (current.isEmpty()) {
			items.set(SLOT_RESULT, output.copy());
		} else {
			current.grow(output.getCount());
		}
		input.shrink(1);
	}

	private int burnDuration(ServerLevel level, ItemStack fuel) {
		return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, getLootContext(level), 0);
	}

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.min(amount, STEAM_CAPACITY - steam);
		steam += accepted;
		return accepted;
	}

	public int steam() {
		return steam;
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.has(DataComponents.COOKING_FUEL);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		litTime = input.getIntOr("lit_time", 0);
		litDuration = input.getIntOr("lit_duration", 0);
		progress = input.getIntOr("progress", 0);
		totalTime = input.getIntOr("total_time", 0);
		steam = input.getIntOr("steam", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("lit_time", litTime);
		output.putInt("lit_duration", litDuration);
		output.putInt("progress", progress);
		output.putInt("total_time", totalTime);
		output.putInt("steam", steam);
		ContainerHelper.saveAllItems(output, items);
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new MachineMenu(containerId, inventory, this, dataAccess);
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return switch (slot) {
			case SLOT_FUEL -> isFuel(stack);
			case SLOT_RESULT -> false;
			default -> true;
		};
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return switch (direction) {
			case UP -> SLOTS_FOR_UP;
			case DOWN -> SLOTS_FOR_DOWN;
			default -> SLOTS_FOR_SIDES;
		};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot == SLOT_RESULT;
	}

	/** 테스트 · 디버그용: 지금 결과 칸. */
	public ItemStack result() {
		return items.get(SLOT_RESULT);
	}
}
