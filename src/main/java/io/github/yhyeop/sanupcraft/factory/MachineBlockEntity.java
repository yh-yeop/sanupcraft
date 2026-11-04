package io.github.yhyeop.sanupcraft.factory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import io.github.yhyeop.sanupcraft.power.SteamConsumer;

/**
 * 기계의 칸과 진행 상태. 화로처럼 연료를 태우는 동안 재료를 하나씩 처리하고, 레시피에 부산물이 있으면 확률로 부산물 칸에 낸다.
 * 증기 파이프가 이어져 있으면 연료 대신 증기로 돈다 (증기가 있으면 연료를 태우지 않음).
 * 깔때기: 위 → 재료(합금로는 두 칸, 이 기계 레시피의 재료만), 옆 → 연료, 아래 ← 결과 · 부산물 · 쓰고 남은 빈 양동이.
 */
public class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, SteamConsumer {
	public static final int SLOT_INPUT = 0;
	public static final int SLOT_FUEL = 1;
	public static final int SLOT_RESULT = 2;
	public static final int SLOT_BYPRODUCT = 3;
	public static final int SLOT_INPUT2 = 4;
	public static final int SLOT_COUNT = 5;
	public static final int DATA_LIT_TIME = 0;
	public static final int DATA_LIT_DURATION = 1;
	public static final int DATA_PROGRESS = 2;
	public static final int DATA_TOTAL_TIME = 3;
	/** 기계 종류 (MachineKind 순서). 화면이 재료 칸 수를 정하는 데 쓴다. */
	public static final int DATA_KIND = 4;
	/** 지금 모아 둔 증기 (화면의 불꽃 칸 툴팁). */
	public static final int DATA_STEAM = 5;
	public static final int DATA_COUNT = 6;
	public static final int STEAM_CAPACITY = 200;
	/** 증기로 돌 때 틱마다 쓰는 증기. */
	public static final int STEAM_PER_TICK = 1;
	/** 일이 멈춘 뒤에도 이 틱 동안은 켜진 모습을 유지한다 (증기가 띄엄띄엄 와도 불빛이 깜빡이지 않게). */
	public static final int LIT_HOLD_TICKS = 10;

	private static final int[] SLOTS_FOR_UP = {SLOT_INPUT, SLOT_INPUT2};
	private static final int[] SLOTS_FOR_DOWN = {SLOT_RESULT, SLOT_BYPRODUCT, SLOT_FUEL};
	private static final int[] SLOTS_FOR_SIDES = {SLOT_FUEL};

	private final MachineKind kind;
	private final RecipeManager.CachedCheck<MachineInput, MachineRecipe> quickCheck;
	private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int litTime;
	private int litDuration;
	private int progress;
	private int totalTime;
	private int steam;
	private boolean steamWorking;
	/** 일하지 않은 연속 틱 수 (저장하지 않음). 처음에는 쉬는 상태. */
	private int idleTicks = LIT_HOLD_TICKS;

	/** 서버가 가진 기계 레시피를 종류별로 모아 둔 것. RecipeManager가 새로 만들어지면(재로드) 다시 만든다. */
	private record RecipeCache(RecipeManager manager, Map<MachineKind, List<MachineRecipe>> byKind) {
	}

	private static volatile @Nullable RecipeCache recipeCache;

	private final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int dataId) {
			return switch (dataId) {
				case DATA_LIT_TIME -> steamWorking ? 1 : litTime;
				case DATA_LIT_DURATION -> steamWorking ? 1 : litDuration;
				case DATA_PROGRESS -> progress;
				case DATA_TOTAL_TIME -> totalTime;
				case DATA_KIND -> kind.ordinal();
				case DATA_STEAM -> steam;
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

	private MachineInput input() {
		return new MachineInput(items.get(SLOT_INPUT), items.get(SLOT_INPUT2));
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
		boolean changed = false;
		machine.steamWorking = false;
		if (machine.litTime > 0) {
			machine.litTime--;
		}

		ItemStack fuel = machine.items.get(SLOT_FUEL);
		MachineInput input = machine.input();
		RecipeHolder<MachineRecipe> recipe = input.isEmpty() ? null : machine.quickCheck.getRecipeFor(input, level).orElse(null);
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
					ItemStack remainder = consumeFuel(fuel);
					if (!remainder.isEmpty()) {
						if (fuel.isEmpty()) {
							// 바닐라 화로처럼 (용암 양동이 → 빈 양동이) 남은 것을 연료 칸에 둔다
							machine.items.set(SLOT_FUEL, remainder);
						} else {
							Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remainder);
						}
					}
					changed = true;
				}
			}
			if (machine.litTime > 0 || machine.steamWorking) {
				machine.progress++;
				if (machine.progress >= machine.totalTime) {
					machine.progress = 0;
					machine.finish(level, recipe.value(), input, output);
					changed = true;
				}
			} else {
				machine.progress = Mth.clamp(machine.progress - 2, 0, machine.totalTime);
			}
		} else if (machine.progress > 0) {
			machine.progress = Mth.clamp(machine.progress - 2, 0, machine.totalTime);
		}

		// 증기가 모자라 일했다 쉬었다 해도 불빛이 깜빡이지 않게, 멈춘 뒤 LIT_HOLD_TICKS 동안은 켜 둔다
		boolean active = machine.litTime > 0 || machine.steamWorking;
		machine.idleTicks = active ? 0 : Math.min(machine.idleTicks + 1, LIT_HOLD_TICKS);
		boolean isLit = active || machine.idleTicks < LIT_HOLD_TICKS;
		if (state.getValue(MachineBlock.LIT) != isLit) {
			state = state.setValue(MachineBlock.LIT, isLit);
			level.setBlockAndUpdate(pos, state);
			changed = true;
		}
		if (changed) {
			setChanged(level, pos, state);
		}
	}

	private boolean canOutput(ItemStack output) {
		return fits(items.get(SLOT_RESULT), output);
	}

	private boolean fits(ItemStack current, ItemStack add) {
		if (add.isEmpty()) {
			return false;
		}
		if (current.isEmpty()) {
			return true;
		}
		return ItemStack.isSameItemSameComponents(current, add)
				&& current.getCount() + add.getCount() <= Math.min(getMaxStackSize(), add.getMaxStackSize());
	}

	/** 결과를 내고 재료를 덜고, 부산물은 확률로 (부산물 칸이 차 있으면 버려진다). */
	private void finish(ServerLevel level, MachineRecipe recipe, MachineInput input, ItemStack output) {
		merge(SLOT_RESULT, output);
		recipe.byproduct().ifPresent(byproduct -> {
			if (level.getRandom().nextFloat() < byproduct.chance()) {
				ItemStack extra = byproduct.item().create();
				if (fits(items.get(SLOT_BYPRODUCT), extra)) {
					merge(SLOT_BYPRODUCT, extra);
				}
			}
		});
		recipe.consume(input);
	}

	private void merge(int slot, ItemStack stack) {
		ItemStack current = items.get(slot);
		if (current.isEmpty()) {
			items.set(slot, stack.copy());
		} else {
			current.grow(stack.getCount());
		}
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

	/**
	 * 연료 하나를 쓴다. 쓰고 남는 것(용암 양동이 → 빈 양동이)을 돌려주고, 놓을 곳은 부르는 쪽이 정한다 (없으면 빈 스택).
	 * 기계 · 보일러가 같이 쓴다.
	 */
	public static ItemStack consumeFuel(ItemStack fuel) {
		Item item = fuel.getItem();
		ItemStackTemplate remainder = item.getCraftingRemainder();
		fuel.shrink(1);
		return remainder == null ? ItemStack.EMPTY : remainder.create();
	}

	/** 이 종류의 기계 레시피 중 stack이 재료(첫째 · 둘째)인 게 있는가. 서버가 아니면 알 수 없으니 true. */
	private boolean acceptsAsInput(ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return true;
		}
		for (MachineRecipe recipe : recipesOf(serverLevel, kind)) {
			if (recipe.acceptsIngredient(stack)) {
				return true;
			}
		}
		return false;
	}

	/** 깔때기 · 벨트가 틱마다 부르므로, 바닐라 포함 1000개가 넘는 레시피를 매번 훑지 않고 RecipeManager별로 모아 둔다. */
	private static List<MachineRecipe> recipesOf(ServerLevel level, MachineKind kind) {
		RecipeManager manager = level.getServer().getRecipeManager();
		RecipeCache cache = recipeCache;
		if (cache == null || cache.manager() != manager) {
			Map<MachineKind, List<MachineRecipe>> byKind = new EnumMap<>(MachineKind.class);
			for (MachineKind each : MachineKind.values()) {
				byKind.put(each, new ArrayList<>());
			}
			for (RecipeHolder<?> holder : manager.getRecipes()) {
				if (holder.value() instanceof MachineRecipe recipe) {
					byKind.get(recipe.kind()).add(recipe);
				}
			}
			cache = new RecipeCache(manager, byKind);
			recipeCache = cache;
		}
		return cache.byKind().get(kind);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
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
			case SLOT_INPUT -> true;
			case SLOT_INPUT2 -> kind.twoInputs();
			case SLOT_FUEL -> isFuel(stack);
			default -> false;
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
		if (!canPlaceItem(slot, stack)) {
			return false;
		}
		if (slot != SLOT_INPUT && slot != SLOT_INPUT2) {
			return true;
		}
		// 깔때기 · 벨트는 이 기계의 레시피 재료만 넣는다 (흙 하나가 재료 칸을 막지 않게). 손으로 넣는 건 그대로.
		if (!acceptsAsInput(stack)) {
			return false;
		}
		if (!kind.twoInputs()) {
			return true;
		}
		// 합금로: 한 재료가 두 칸을 다 차지해 다른 재료가 못 들어오는 교착을 막는다
		ItemStack first = items.get(SLOT_INPUT);
		ItemStack second = items.get(SLOT_INPUT2);
		if (slot == SLOT_INPUT) {
			return sameStack(first, stack) || (first.isEmpty() && !sameStack(second, stack));
		}
		return sameStack(second, stack) || (second.isEmpty() && !first.isEmpty() && !sameStack(first, stack));
	}

	private static boolean sameStack(ItemStack current, ItemStack stack) {
		return !current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		// 연료 칸에는 쓰고 남은 빈 양동이만 아래로 꺼낼 수 있다 (바닐라 화로와 같음)
		return slot == SLOT_RESULT || slot == SLOT_BYPRODUCT || (slot == SLOT_FUEL && direction == Direction.DOWN && stack.is(Items.BUCKET));
	}

	/** 테스트 · 디버그용: 지금 결과 칸. */
	public ItemStack result() {
		return items.get(SLOT_RESULT);
	}

	/** 테스트 · 디버그용: 지금 부산물 칸. */
	public ItemStack byproduct() {
		return items.get(SLOT_BYPRODUCT);
	}
}
