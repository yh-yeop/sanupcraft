package io.github.yhyeop.sanupcraft.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;

/**
 * 재고 유지기. 창고 네트워크에 붙여 "견본 아이템을 N개 유지"한다. 1초(설정)마다:
 * <ol>
 *   <li>옆에 붙은 자동 제작대 · 기계의 결과 칸을 네트워크로 거둔다.</li>
 *   <li>네트워크의 그 아이템이 N개보다 적으면, 그것을 만드는 옆 자동 제작대(견본 결과가 같은 것) 또는 기계(그 아이템이 나오는 기계 레시피)에
 *       네트워크의 재료를 한 번에 최대 4번 몫까지 넣는다. 이미 넣어 둔 몫이 있으면 더 넣지 않는다.</li>
 * </ol>
 * 기계의 연료 · 증기와 자동 제작대의 증기는 따로 넣어야 한다.
 */
public class StockerBlockEntity extends BlockEntity implements StorageNetwork.Member, MenuProvider {
	public static final int TICKS_PER_CYCLE = 20;
	public static final int MAX_SETS = 4;
	public static final int MAX_TARGET = 9999;
	public static final int DATA_TARGET = 0;
	public static final int DATA_HAVE = 1;
	public static final int DATA_STATUS = 2;
	public static final int DATA_COUNT = 3;

	/** 지난번 돈 결과 (화면에 보인다). */
	public enum Status {
		IDLE, STOCKED, WORKING, MISSING, NO_PRODUCER
	}

	private final GhostContainer target = new GhostContainer(1);
	private final StorageNetwork.Cache network = new StorageNetwork.Cache();
	private int targetCount = 64;
	private int timer;
	private Status status = Status.IDLE;
	private long lastHave;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int id) {
			return switch (id) {
				case DATA_TARGET -> targetCount;
				case DATA_HAVE -> (int) Math.min(Short.MAX_VALUE, lastHave);
				case DATA_STATUS -> status.ordinal();
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

	public StockerBlockEntity(BlockPos pos, BlockState state) {
		super(ModStorage.STOCKER_BLOCK_ENTITY, pos, state);
		target.onChange(this::setChanged);
	}

	public GhostContainer target() {
		return target;
	}

	public int targetCount() {
		return targetCount;
	}

	public void setTargetCount(int count) {
		targetCount = Mth.clamp(count, 0, MAX_TARGET);
		setChanged();
	}

	public Status status() {
		return status;
	}

	public StorageNetwork network() {
		return network.get(level, worldPosition);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, StockerBlockEntity stocker) {
		if (level instanceof ServerLevel server && ++stocker.timer >= Math.max(1, SanupcraftConfig.current().stockerTicksPerCycle)) {
			stocker.timer = 0;
			stocker.cycle(server);
		}
	}

	/** 한 번 돈다 (거두기 → 모자라면 재료 넣기). */
	public void cycle(ServerLevel level) {
		StorageNetwork net = network();
		List<BlockEntity> neighbours = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			BlockEntity be = level.getBlockEntity(worldPosition.relative(direction));
			if (be instanceof AutocrafterBlockEntity || be instanceof MachineBlockEntity) {
				neighbours.add(be);
			}
		}
		for (BlockEntity be : neighbours) {
			collect(net, be);
		}
		ItemStack wanted = target.getItem(0);
		if (wanted.isEmpty()) {
			status = Status.IDLE;
			lastHave = 0;
			return;
		}
		lastHave = net.count(wanted);
		if (lastHave >= targetCount) {
			status = Status.STOCKED;
			return;
		}
		long deficit = targetCount - lastHave;
		boolean producer = false;
		for (BlockEntity be : neighbours) {
			Status result = be instanceof AutocrafterBlockEntity crafter ? feedCrafter(level, net, crafter, wanted, deficit)
					: feedMachine(level, net, (MachineBlockEntity) be, wanted, deficit);
			if (result == Status.WORKING) {
				status = Status.WORKING;
				return;
			}
			producer |= result == Status.MISSING;
		}
		status = producer ? Status.MISSING : Status.NO_PRODUCER;
	}

	/** 결과 칸의 것을 네트워크로 (안 들어가는 만큼은 남긴다). */
	private static void collect(StorageNetwork net, BlockEntity be) {
		int[] slots = be instanceof AutocrafterBlockEntity ? new int[] {AutocrafterBlockEntity.SLOT_OUTPUT}
				: new int[] {MachineBlockEntity.SLOT_RESULT, MachineBlockEntity.SLOT_BYPRODUCT};
		net.minecraft.world.Container container = (net.minecraft.world.Container) be;
		for (int slot : slots) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack rest = net.insert(stack);
			if (rest.getCount() != stack.getCount()) {
				container.setItem(slot, rest);
				container.setChanged();
			}
		}
	}

	private static int setsFor(long deficit, int perSet) {
		return (int) Math.max(1, Math.min(MAX_SETS, (deficit + perSet - 1) / Math.max(1, perSet)));
	}

	/** 자동 제작대: 견본 결과가 그 아이템이면 재료 몫을 넣는다. 만드는 곳이 아니면 IDLE. */
	private static Status feedCrafter(ServerLevel level, StorageNetwork net, AutocrafterBlockEntity crafter, ItemStack wanted, long deficit) {
		ItemStack result = crafter.patternResult();
		if (result.isEmpty() || !ItemStack.isSameItemSameComponents(result, wanted)) {
			return Status.IDLE;
		}
		int sets = setsFor(deficit, result.getCount()) - crafter.loadedSets();
		if (sets <= 0) {
			return Status.WORKING;
		}
		// 한 몫에 드는 재료를 종류별로 세고, 네트워크에 다 있을 때만 넣는다
		GhostContainer pattern = crafter.pattern();
		Map<StorageNetwork.Key, Integer> need = new HashMap<>();
		for (int i = 0; i < AutocrafterBlockEntity.GRID; i++) {
			if (!pattern.getItem(i).isEmpty()) {
				need.merge(new StorageNetwork.Key(pattern.getItem(i)), sets, Integer::sum);
			}
		}
		for (var entry : need.entrySet()) {
			if (net.count(entry.getKey().stack()) < entry.getValue()) {
				return Status.MISSING;
			}
		}
		for (int i = 0; i < AutocrafterBlockEntity.GRID; i++) {
			ItemStack sample = pattern.getItem(i);
			if (sample.isEmpty()) {
				continue;
			}
			ItemStack current = crafter.getItem(i);
			if (!current.isEmpty() && !crafter.matchesPattern(i, current)) {
				continue;
			}
			int room = sample.getMaxStackSize() - current.getCount();
			ItemStack got = net.extract(sample, Math.min(sets, room));
			if (got.isEmpty()) {
				continue;
			}
			if (current.isEmpty()) {
				crafter.setItem(i, got);
			} else {
				current.grow(got.getCount());
			}
		}
		crafter.setChanged();
		return Status.WORKING;
	}

	/** 기계: 그 아이템이 나오는 이 기계의 레시피를 찾아 재료를 넣는다. */
	private static Status feedMachine(ServerLevel level, StorageNetwork net, MachineBlockEntity machine, ItemStack wanted, long deficit) {
		boolean known = false;
		for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
			if (!(holder.value() instanceof MachineRecipe recipe) || recipe.kind() != machine.kind()) {
				continue;
			}
			ItemStack out = recipe.output().create();
			if (!ItemStack.isSameItem(out, wanted)) {
				continue;
			}
			known = true;
			int sets = setsFor(deficit, out.getCount());
			boolean first = fill(net, machine, MachineBlockEntity.SLOT_INPUT, recipe.ingredient(), recipe.count(), sets);
			if (first && (recipe.ingredient2().isEmpty()
					|| fill(net, machine, MachineBlockEntity.SLOT_INPUT2, recipe.ingredient2().get(), recipe.count2(), sets))) {
				machine.setChanged();
				return Status.WORKING;
			}
			if (first) {
				// 둘째 재료가 모자라 다음 레시피로 넘어가도, 첫째 칸은 이미 바뀌었을 수 있다 (표시 안 하면 이웃 청크에서 재시작 때 유실)
				machine.setChanged();
			}
		}
		return known ? Status.MISSING : Status.IDLE;
	}

	/**
	 * 기계 재료 칸 하나를 채운다: 이미 한 몫 이상 있으면 그대로 true, 아니면 네트워크에서 재료를 sets 몫만큼 넣는다.
	 * 네트워크에 한 몫도 없으면 false.
	 */
	private static boolean fill(StorageNetwork net, MachineBlockEntity machine, int slot, Ingredient ingredient, int perSet, int sets) {
		ItemStack current = machine.getItem(slot);
		if (!current.isEmpty()) {
			if (!ingredient.test(current)) {
				return false;
			}
			if (current.getCount() >= perSet) {
				return true;
			}
		}
		ItemStack source = findIngredient(net, ingredient, current, perSet);
		if (source == null) {
			return false;
		}
		int max = source.getMaxStackSize() - current.getCount();
		ItemStack got = net.extract(source, Math.min(max, perSet * sets - current.getCount()));
		if (got.isEmpty()) {
			return false;
		}
		if (current.isEmpty()) {
			machine.setItem(slot, got);
		} else {
			current.grow(got.getCount());
		}
		return true;
	}

	/** 재료에 맞고 네트워크에 한 몫 이상 있는 아이템 (칸에 이미 든 것이 있으면 그것과 같은 것만). */
	private static @Nullable ItemStack findIngredient(StorageNetwork net, Ingredient ingredient, ItemStack current, int perSet) {
		for (var entry : net.contents().entrySet()) {
			ItemStack stack = entry.getKey().stack();
			if (ingredient.test(stack) && entry.getValue() + current.getCount() >= perSet
					&& (current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack))) {
				return stack;
			}
		}
		return null;
	}

	// ---- 화면 ----

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new StockerMenu(containerId, inventory, this, data);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		target.load(input, "target");
		targetCount = Mth.clamp(input.getIntOr("target_count", 64), 0, MAX_TARGET);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		target.save(output, "target");
		output.putInt("target_count", targetCount);
	}
}
