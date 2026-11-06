package io.github.yhyeop.sanupcraft.storage;

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
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 자동 제작대. 견본 3x3로 제작대 레시피를 기억하고, 재료 9칸(견본과 같은 칸에 같은 아이템)이 차면 증기 20을 써서 만들어 결과 칸으로 낸다.
 * 깔때기 · 벨트: 아래 ← 결과, 나머지 면 → 재료 (견본에 맞는 칸에만, 같은 재료 칸끼리는 고르게). 재고 유지기가 옆에 있으면 재료를 넣어 준다.
 * 쓰고 남는 것(빈 양동이 등)은 위에 떨군다.
 */
public class AutocrafterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, SteamConsumer, SteamStorage {
	public static final int GRID = 9;
	public static final int SLOT_OUTPUT = 9;
	public static final int SLOT_COUNT = 10;
	public static final int STEAM_CAPACITY = 1000;
	public static final int STEAM_PER_CRAFT = 20;
	public static final int TICKS_PER_CRAFT = 10;
	public static final int DATA_STEAM = 0;
	public static final int DATA_PROGRESS = 1;
	public static final int DATA_COUNT = 2;

	private static final int[] SLOTS_DOWN = {SLOT_OUTPUT};
	private static final int[] SLOTS_OTHER = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private final GhostContainer pattern = new GhostContainer(GRID);
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private int steam;
	private int timer;
	/** 견본 레시피 캐시 (견본이 바뀌거나 레시피가 다시 읽히면 버린다). */
	private @Nullable RecipeManager cachedManager;
	private @Nullable RecipeHolder<CraftingRecipe> cachedRecipe;
	private ItemStack cachedResult = ItemStack.EMPTY;
	private boolean patternDirty = true;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int id) {
			return switch (id) {
				case DATA_STEAM -> steam;
				case DATA_PROGRESS -> timer;
				default -> 0;
			};
		}

		@Override
		public void set(int id, int value) {
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public AutocrafterBlockEntity(BlockPos pos, BlockState state) {
		super(ModStorage.AUTOCRAFTER_BLOCK_ENTITY, pos, state);
		pattern.onChange(() -> {
			patternDirty = true;
			setChanged();
		});
	}

	public GhostContainer pattern() {
		return pattern;
	}

	/** 견본이 만드는 것 (서버만 안다. 레시피가 없으면 빈 스택). */
	public ItemStack patternResult() {
		refreshPattern();
		return cachedResult;
	}

	private void refreshPattern() {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		RecipeManager manager = server.getServer().getRecipeManager();
		if (!patternDirty && manager == cachedManager) {
			return;
		}
		patternDirty = false;
		cachedManager = manager;
		List<ItemStack> grid = new ArrayList<>(GRID);
		for (int i = 0; i < GRID; i++) {
			grid.add(pattern.getItem(i).copyWithCount(1));
		}
		CraftingInput input = CraftingInput.of(3, 3, grid);
		Optional<RecipeHolder<CraftingRecipe>> recipe = input.isEmpty() ? Optional.empty() : manager.getRecipeFor(RecipeType.CRAFTING, input, server);
		cachedRecipe = recipe.orElse(null);
		cachedResult = recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	/** 견본 칸 i에 놓을 수 있는 재료인가 (같은 아이템). */
	public boolean matchesPattern(int slot, ItemStack stack) {
		ItemStack sample = pattern.getItem(slot);
		return slot < GRID && !sample.isEmpty() && !stack.isEmpty() && ItemStack.isSameItem(sample, stack);
	}

	/** 지금 재료로 몇 번 만들 수 있는가 (견본이 쓰는 칸 중 가장 적은 개수). */
	public int loadedSets() {
		int min = Integer.MAX_VALUE;
		boolean any = false;
		for (int i = 0; i < GRID; i++) {
			if (!pattern.getItem(i).isEmpty()) {
				any = true;
				ItemStack stack = items.get(i);
				min = Math.min(min, matchesPattern(i, stack) ? stack.getCount() : 0);
			}
		}
		return any ? min : 0;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, AutocrafterBlockEntity crafter) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if (++crafter.timer < Math.max(1, SanupcraftConfig.current().autocrafterTicksPerCraft)) {
			return;
		}
		crafter.timer = 0;
		crafter.tryCraft(server);
	}

	/** 한 번 만들어 본다. 만들었으면 true. */
	public boolean tryCraft(ServerLevel level) {
		int cost = SanupcraftConfig.current().autocrafterSteamPerCraft;
		if (steam < cost || loadedSets() <= 0) {
			return false;
		}
		List<ItemStack> grid = new ArrayList<>(GRID);
		for (int i = 0; i < GRID; i++) {
			grid.add(pattern.getItem(i).isEmpty() ? ItemStack.EMPTY : items.get(i).copyWithCount(1));
		}
		CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, grid);
		CraftingInput input = positioned.input();
		Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		if (recipe.isEmpty()) {
			return false;
		}
		ItemStack result = recipe.get().value().assemble(input);
		ItemStack output = items.get(SLOT_OUTPUT);
		if (result.isEmpty() || !(output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
				&& output.getCount() + result.getCount() <= output.getMaxStackSize())) {
			return false;
		}
		NonNullList<ItemStack> remaining = recipe.get().value().getRemainingItems(input);
		for (int i = 0; i < GRID; i++) {
			if (!pattern.getItem(i).isEmpty()) {
				items.get(i).shrink(1);
			}
		}
		for (int y = 0; y < input.height(); y++) {
			for (int x = 0; x < input.width(); x++) {
				ItemStack rest = remaining.get(x + y * input.width());
				if (!rest.isEmpty()) {
					Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, rest);
				}
			}
		}
		if (output.isEmpty()) {
			items.set(SLOT_OUTPUT, result);
		} else {
			output.grow(result.getCount());
		}
		steam -= cost;
		setChanged();
		return true;
	}

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.max(0, Math.min(amount, STEAM_CAPACITY - steam));
		steam += accepted;
		flow.add(level != null ? level.getGameTime() : 0L, accepted);
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

	/** 깔때기 · 벨트 · 재고 유지기: 견본에 맞는 칸이고, 같은 재료 칸들 중 가장 적은 칸일 때만 (고르게 채운다). */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (!matchesPattern(slot, stack)) {
			return false;
		}
		int mine = items.get(slot).getCount();
		for (int i = 0; i < GRID; i++) {
			if (i != slot && matchesPattern(i, stack) && items.get(i).getCount() < mine) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return direction == Direction.DOWN ? SLOTS_DOWN : SLOTS_OTHER;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return slot < GRID && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot == SLOT_OUTPUT;
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new AutocrafterMenu(containerId, inventory, this, pattern, data);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		pattern.load(input, "pattern");
		steam = input.getIntOr("steam", 0);
		patternDirty = true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		pattern.save(output, "pattern");
		output.putInt("steam", steam);
	}

	/** 결과 칸 (테스트 · 재고 유지기). */
	public ItemStack output() {
		return items.get(SLOT_OUTPUT);
	}
}
