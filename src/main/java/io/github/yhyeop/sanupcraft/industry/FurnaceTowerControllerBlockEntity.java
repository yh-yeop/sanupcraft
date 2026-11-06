package io.github.yhyeop.sanupcraft.industry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.factory.MachineKind;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;
import io.github.yhyeop.sanupcraft.power.HPSteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 용광로 탑 제어기 (M8 묶음 B). 3x3 · 높이 3 내열 벽돌 모양({@link #SHAPE})이 맞을 때만 돈다.
 * 재료 9칸(0..8) · 결과 9칸(9..17). 한 번에 레시피 하나를 최대 {@link #BATCH_SIZE}개 굽는다: 합금로 레시피를 먼저, 없으면 화로(바닐라 굽기) 레시피.
 * 걸리는 시간은 그 레시피 하나의 시간 (화로 = 보통 200틱), 고압 증기는 개수 × {@link #STEAM_PER_ITEM}을 시간 동안 나눠 쓴다.
 * 고압 증기는 제어기 또는 아무 외벽에 붙은 고압 파이프로 받는다. 깔때기: 아래 ← 결과, 나머지 면 → 재료 (구울 수 있는 것만).
 * 경험치는 주지 않는다.
 */
public class FurnaceTowerControllerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, HPSteamConsumer, SteamStorage {
	public static final int INPUT_SLOTS = 9;
	public static final int OUTPUT_SLOTS = 9;
	public static final int SLOT_COUNT = INPUT_SLOTS + OUTPUT_SLOTS;
	public static final int BATCH_SIZE = 8;
	public static final int STEAM_PER_ITEM = 50;
	public static final int STEAM_CAPACITY = 1000;
	public static final int CHECK_INTERVAL = 40;
	/** 굽던 것이 없을 때 재료를 다시 살피는 간격. */
	public static final int SEARCH_INTERVAL = 10;
	/** 일이 멈춘 뒤에도 이 틱 동안은 불 켜진 모습 (증기가 띄엄띄엄 와도 깜빡이지 않게). */
	public static final int LIT_HOLD_TICKS = 10;

	/** 3x3 · 높이 3. 제어기는 앞면 가운데 맨 아래, 가운데 기둥(제어기 뒤 1칸, 세 층)은 비우고 나머지 23칸은 내열 벽돌. */
	public static final MultiblockShape SHAPE = new MultiblockShape(3, 3, (side, depth, up) -> {
		if (side == 0 && depth == 0 && up == 0) {
			return MultiblockShape.Part.CONTROLLER;
		}
		return side == 0 && depth == 1 ? MultiblockShape.Part.HOLLOW : MultiblockShape.Part.CASING;
	}, state -> state.getBlock() instanceof HeatproofBricksBlock);

	private static final int[] SLOTS_INPUT = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	private static final int[] SLOTS_OUTPUT = {9, 10, 11, 12, 13, 14, 15, 16, 17};

	/** 지금 굽는 한 묶음: 재료(둘째는 합금만) · 개수당 결과 · 개수 · 시간. 저장하지 않고 다시 찾는다. */
	record Batch(Ingredient first, int firstCount, @Nullable Ingredient second, int secondCount, ItemStack resultEach, int units, int time) {
	}

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int steam;
	private int progress;
	private boolean formed;
	private MultiblockShape.@Nullable Problem problem;
	private int checkTimer;
	private int searchTimer;
	private @Nullable Batch batch;
	private double steamCarry;
	private int idleTicks = LIT_HOLD_TICKS;
	private final SteamFlowMeter flow = new SteamFlowMeter();

	/** 합금로 레시피 목록. RecipeManager가 새로 만들어지면(재로드) 다시 모은다. */
	private record AlloyCache(RecipeManager manager, List<MachineRecipe> recipes) {
	}

	private static volatile @Nullable AlloyCache alloyCache;

	public FurnaceTowerControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModFoundry.FURNACE_TOWER_CONTROLLER_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FurnaceTowerControllerBlockEntity tower) {
		tower.tick(level, pos, state);
	}

	private void tick(ServerLevel level, BlockPos pos, BlockState state) {
		if (--checkTimer <= 0) {
			check(level, pos, state);
		}
		boolean working = false;
		if (!formed) {
			batch = null;
			progress = 0;
		} else {
			if (batch == null && --searchTimer <= 0) {
				searchTimer = SEARCH_INTERVAL;
				batch = findBatch(level);
				if (batch == null) {
					progress = 0;
				}
			}
			if (batch != null) {
				working = work(level);
			}
		}
		idleTicks = working ? 0 : Math.min(idleTicks + 1, LIT_HOLD_TICKS);
		boolean lit = working || idleTicks < LIT_HOLD_TICKS;
		if (state.getValue(FurnaceTowerControllerBlock.LIT) != lit) {
			level.setBlock(pos, state.setValue(FurnaceTowerControllerBlock.LIT, lit), Block.UPDATE_ALL);
		}
	}

	/** 한 틱 굽는다. 증기가 모자라면 기다린다. 다 구우면 결과를 내고 다음 묶음을 찾는다. 일했으면 true. */
	private boolean work(ServerLevel level) {
		Batch current = batch;
		double perTick = (double) current.units() * SanupcraftConfig.current().furnaceTowerSteamPerItem / current.time();
		double need = steamCarry + perTick;
		int whole = (int) need;
		if (steam < whole || (perTick > 0 && steam <= 0)) {
			return false;
		}
		steam -= whole;
		steamCarry = need - whole;
		progress++;
		if (progress >= current.time()) {
			progress = 0;
			finish(current);
			batch = null;
			searchTimer = 0;
		}
		setChanged();
		return true;
	}

	/** 다 구운 묶음: 재료가 그새 줄었으면 남은 만큼만, 결과 칸에 들어가는 만큼만. */
	private void finish(Batch done) {
		int units = Math.min(done.units(), available(done));
		units = Math.min(units, fitUnits(done.resultEach(), units));
		if (units <= 0) {
			return;
		}
		take(done.first(), done.firstCount() * units);
		if (done.second() != null) {
			take(done.second(), done.secondCount() * units);
		}
		insert(done.resultEach(), units);
	}

	/** 지금 재료로 구울 묶음. 합금로 레시피를 먼저 보고, 없으면 재료 칸 순서대로 화로 레시피. */
	@Nullable Batch findBatch(ServerLevel level) {
		int limit = Math.max(1, SanupcraftConfig.current().furnaceTowerBatchSize);
		RecipeManager manager = level.recipeAccess();
		for (MachineRecipe recipe : alloyRecipes(manager)) {
			Batch candidate = new Batch(recipe.ingredient(), recipe.count(), recipe.ingredient2().orElse(null), recipe.count2(),
					recipe.output().create(), 0, recipe.time());
			Batch sized = sized(candidate, limit);
			if (sized != null) {
				return sized;
			}
		}
		for (int slot = 0; slot < INPUT_SLOTS; slot++) {
			ItemStack stack = items.get(slot);
			if (stack.isEmpty()) {
				continue;
			}
			SingleRecipeInput input = new SingleRecipeInput(stack);
			Optional<RecipeHolder<SmeltingRecipe>> found = manager.getRecipeFor(RecipeType.SMELTING, input, level);
			if (found.isPresent()) {
				SmeltingRecipe recipe = found.get().value();
				Batch candidate = new Batch(recipe.input(), 1, null, 0, recipe.assemble(input), 0, Math.max(1, recipe.cookingTime()));
				Batch sized = sized(candidate, limit);
				if (sized != null) {
					return sized;
				}
			}
		}
		return null;
	}

	/** 재료 · 결과 칸 자리를 보고 개수를 정한 묶음. 하나도 못 구우면 null. */
	private @Nullable Batch sized(Batch candidate, int limit) {
		if (candidate.resultEach().isEmpty()) {
			return null;
		}
		int units = Math.min(limit, available(candidate));
		units = fitUnits(candidate.resultEach(), units);
		if (units <= 0) {
			return null;
		}
		return new Batch(candidate.first(), candidate.firstCount(), candidate.second(), candidate.secondCount(), candidate.resultEach(), units, candidate.time());
	}

	/** 재료 칸으로 몇 개 몫을 만들 수 있나. 한 칸은 첫째 재료로 먼저 센다. */
	private int available(Batch batch) {
		int first = 0;
		int second = 0;
		for (int slot = 0; slot < INPUT_SLOTS; slot++) {
			ItemStack stack = items.get(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (batch.first().test(stack)) {
				first += stack.getCount();
			} else if (batch.second() != null && batch.second().test(stack)) {
				second += stack.getCount();
			}
		}
		int units = first / batch.firstCount();
		if (batch.second() != null) {
			units = Math.min(units, second / batch.secondCount());
		}
		return units;
	}

	/** 결과 {@code each} × n이 결과 칸에 들어가는 가장 큰 n (최대 {@code units}). */
	private int fitUnits(ItemStack each, int units) {
		for (int n = units; n > 0; n--) {
			if (room(each) >= each.getCount() * n) {
				return n;
			}
		}
		return 0;
	}

	/** 결과 칸에 {@code stack}과 같은 것을 더 넣을 수 있는 개수. */
	private int room(ItemStack stack) {
		int max = Math.min(getMaxStackSize(), stack.getMaxStackSize());
		int room = 0;
		for (int slot = INPUT_SLOTS; slot < SLOT_COUNT; slot++) {
			ItemStack current = items.get(slot);
			if (current.isEmpty()) {
				room += max;
			} else if (ItemStack.isSameItemSameComponents(current, stack)) {
				room += Math.max(0, max - current.getCount());
			}
		}
		return room;
	}

	private void take(Ingredient ingredient, int count) {
		for (int slot = 0; slot < INPUT_SLOTS && count > 0; slot++) {
			ItemStack stack = items.get(slot);
			if (!stack.isEmpty() && ingredient.test(stack)) {
				int taken = Math.min(count, stack.getCount());
				stack.shrink(taken);
				count -= taken;
			}
		}
	}

	private void insert(ItemStack each, int units) {
		int left = each.getCount() * units;
		int max = Math.min(getMaxStackSize(), each.getMaxStackSize());
		for (int slot = INPUT_SLOTS; slot < SLOT_COUNT && left > 0; slot++) {
			ItemStack current = items.get(slot);
			if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, each)) {
				int add = Math.min(left, max - current.getCount());
				if (add > 0) {
					current.grow(add);
					left -= add;
				}
			}
		}
		for (int slot = INPUT_SLOTS; slot < SLOT_COUNT && left > 0; slot++) {
			if (items.get(slot).isEmpty()) {
				int add = Math.min(left, max);
				items.set(slot, each.copyWithCount(add));
				left -= add;
			}
		}
	}

	// ---- 모양 ----

	/** 다음 틱에 모양을 다시 확인한다 (벽돌이 부서짐 · 제어기 옆이 바뀜). */
	public void requestCheck() {
		checkTimer = 0;
	}

	/** 모양을 지금 확인하고 벽돌 무늬 · 고압 증기 연결을 맞춘다. 맞으면 true. */
	public boolean check(ServerLevel level, BlockPos pos, BlockState state) {
		checkTimer = CHECK_INTERVAL;
		Direction facing = state.getValue(FurnaceTowerControllerBlock.FACING);
		problem = SHAPE.check(level, pos, facing);
		boolean now = problem == null;
		boolean changed = now != formed;
		if (changed) {
			formed = now;
			setChanged();
		}
		if (now || changed) {
			setCasingsFormed(level, pos, facing, now);
		}
		return formed;
	}

	/** 벽돌 무늬를 켜거나 끄고, 고압 증기를 넘길 제어기를 알려 준다. 이웃 갱신 없이. */
	static void setCasingsFormed(ServerLevel level, BlockPos controller, Direction facing, boolean formed) {
		for (BlockPos casing : SHAPE.casings(controller, facing)) {
			BlockState state = level.getBlockState(casing);
			if (!(state.getBlock() instanceof HeatproofBricksBlock)) {
				continue;
			}
			if (state.getValue(HeatproofBricksBlock.FORMED) != formed) {
				level.setBlock(casing, state.setValue(HeatproofBricksBlock.FORMED, formed), Block.UPDATE_CLIENTS);
			}
			if (level.getBlockEntity(casing) instanceof HeatproofBricksBlockEntity bricks) {
				bricks.link(formed ? controller.immutable() : null);
			}
		}
	}

	public boolean isFormed() {
		return formed;
	}

	public MultiblockShape.@Nullable Problem problem() {
		return problem;
	}

	/** 지금 굽는 개수 (없으면 0). */
	public int batchUnits() {
		return batch == null ? 0 : batch.units();
	}

	/** 지금 묶음의 진행 (0..100). */
	public int progressPercent() {
		return batch == null ? 0 : progress * 100 / batch.time();
	}

	// ---- 고압 증기 ----

	@Override
	public int acceptHighPressureSteam(int amount) {
		if (!formed || amount <= 0) {
			return 0;
		}
		int accepted = Math.min(amount, STEAM_CAPACITY - steam);
		if (accepted > 0) {
			steam += accepted;
			flow.add(level != null ? level.getGameTime() : 0L, accepted);
			setChanged();
		}
		return accepted;
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

	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(level != null ? level.getGameTime() : 0L);
	}

	// ---- 칸 ----

	/** 깔때기 · 벨트 입력 거름: 화로에서 구울 수 있거나 합금로 재료인 것. 서버가 아니면 true. */
	private boolean accepts(ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return true;
		}
		if (serverLevel.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(stack)) {
			return true;
		}
		for (MachineRecipe recipe : alloyRecipes(serverLevel.recipeAccess())) {
			if (recipe.acceptsIngredient(stack)) {
				return true;
			}
		}
		return false;
	}

	/** 합금로 레시피들 (깔때기 · 벨트가 자주 부르므로 모아 둔다). */
	private static List<MachineRecipe> alloyRecipes(RecipeManager manager) {
		AlloyCache cache = alloyCache;
		if (cache == null || cache.manager() != manager) {
			List<MachineRecipe> list = new ArrayList<>();
			for (RecipeHolder<?> holder : manager.getRecipes()) {
				if (holder.value() instanceof MachineRecipe recipe && recipe.kind() == MachineKind.ALLOY) {
					list.add(recipe);
				}
			}
			cache = new AlloyCache(manager, List.copyOf(list));
			alloyCache = cache;
		}
		return cache.recipes();
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return direction == Direction.DOWN ? SLOTS_OUTPUT : SLOTS_INPUT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < INPUT_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return slot < INPUT_SLOTS && accepts(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot >= INPUT_SLOTS;
	}

	/** 테스트 · 디버그용: 결과 칸 전부. */
	public List<ItemStack> outputs() {
		List<ItemStack> list = new ArrayList<>();
		for (int slot = INPUT_SLOTS; slot < SLOT_COUNT; slot++) {
			list.add(items.get(slot));
		}
		return list;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		steam = input.getIntOr("steam", 0);
		progress = input.getIntOr("progress", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("steam", steam);
		output.putInt("progress", progress);
		ContainerHelper.saveAllItems(output, items);
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	/** 바닐라 9x2 상자 화면: 윗줄 재료, 아랫줄 결과. */
	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x2, containerId, inventory, this, 2);
	}

	@Override
	public int getContainerSize() {
		return SLOT_COUNT;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}
}
