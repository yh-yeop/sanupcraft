package io.github.yhyeop.sanupcraft.production;

import java.util.ArrayDeque;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 벌목기: 앞쪽(앞 1~2칸, 옆 -1~+1, 바닥에서 위 1칸까지)에서 나무 원목을 찾으면 그 나무를 통째로 벤다.
 * 이어진 원목 + 자연 잎 최대 {@link TreeFelling#MAX_BLOCKS}개를 위쪽 원목부터 한 블록씩 (기본 2틱에 하나) 캐서 떨어진 것을 9칸에 모은다.
 * 자연 잎이 안 붙은 원목(통나무집 · 쌓은 원목)은 건드리지 않는다. 다 베면 칸에 있는 묘목 하나를 가장 낮은 원목 자리에 다시 심는다
 * (그 나무의 묘목이 있으면 그것, 없으면 아무 묘목. 잎에서 나온 묘목이 칸에 모이므로 보통 저절로 된다).
 * 증기는 블록마다 든다 (원목 {@link #STEAM_PER_BLOCK}, 잎은 그 1/4). 증기가 모자라면 멈췄다가 차면 이어서 한다.
 */
public class TreeFellerBlockEntity extends SteamWorkerBlockEntity {
	public static final int STEAM_CAPACITY = 800;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int TICKS_PER_BLOCK = 2;
	public static final int STEAM_PER_BLOCK = 4;
	/** 나무를 찾아보는 간격(틱). */
	private static final int SCAN_INTERVAL = 10;
	private static final ItemStack TOOL = new ItemStack(Items.IRON_AXE);

	/** 아직 안 벤 블록 (저장하지 않는다. 다시 불러오면 남은 나무를 새로 찾는다). */
	private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
	private @Nullable BlockPos replantAt;
	private @Nullable Block replantLog;
	private int felled;

	public TreeFellerBlockEntity(BlockPos pos, BlockState state) {
		super(ModProduction.TREE_FELLER_BLOCK_ENTITY, pos, state);
	}

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, BlockState state) {
		if (!flushPending()) {
			return false;
		}
		SanupcraftConfig config = SanupcraftConfig.current();
		if (queue.isEmpty()) {
			if (replantAt != null) {
				replant(level);
			}
			if (steam >= config.fellerSteamPerBlock && level.getGameTime() % SCAN_INTERVAL == 0) {
				startFelling(level, pos);
			}
			return false;
		}
		if (level.getGameTime() % config.fellerTicksPerBlock != 0) {
			return true;
		}
		return fellNext(level);
	}

	/** 앞쪽에서 나무를 찾아 베기 시작한다. 찾았으면 true. 시험이 직접 부른다. */
	public boolean startFelling(ServerLevel level, BlockPos pos) {
		Direction forward = facing();
		Direction right = forward.getClockWise();
		for (int dist = 1; dist <= 2; dist++) {
			for (int up = 0; up <= 1; up++) {
				for (int side : new int[] {0, -1, 1}) {
					BlockPos start = pos.relative(forward, dist).relative(right, side).above(up);
					if (!TreeFelling.isLog(level.getBlockState(start))) {
						continue;
					}
					TreeFelling.Tree tree = TreeFelling.scan(level, List.of(start), TreeFelling.MAX_BLOCKS);
					if (!tree.isTree()) {
						continue;
					}
					queue.clear();
					queue.addAll(tree.fellOrder());
					replantAt = tree.base(start);
					replantLog = level.getBlockState(start).getBlock();
					setChanged();
					return true;
				}
			}
		}
		return false;
	}

	/** 줄 선 블록 하나를 벤다 (증기가 모자라면 제자리). 일했으면 true. */
	private boolean fellNext(ServerLevel level) {
		int logCost = SanupcraftConfig.current().fellerSteamPerBlock;
		while (!queue.isEmpty()) {
			BlockPos target = queue.peek();
			BlockState state = level.getBlockState(target);
			boolean log = TreeFelling.isLog(state);
			if (!log && !TreeFelling.isNaturalLeaf(state)) {
				queue.poll();
				continue;
			}
			int cost = log ? logCost : TreeFelling.leafCost(logCost);
			if (steam < cost) {
				return false;
			}
			queue.poll();
			steam -= cost;
			for (ItemStack drop : Block.getDrops(state, level, target, level.getBlockEntity(target), null, TOOL)) {
				stash(drop);
			}
			level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, target, Block.getId(state));
			level.removeBlock(target, false);
			felled++;
			setChanged();
			return true;
		}
		return false;
	}

	/** 칸의 묘목 하나를 베어 낸 자리에 심는다. 묘목이 없으면 포기. */
	private void replant(ServerLevel level) {
		BlockPos at = replantAt;
		replantAt = null;
		if (at == null || !level.getBlockState(at).isAir()) {
			return;
		}
		ItemStack sapling = takeOne(stack -> stack.getItem() == preferredSapling(replantLog));
		if (sapling.isEmpty()) {
			sapling = takeOne(stack -> stack.is(ItemTags.SAPLINGS));
		}
		if (sapling.isEmpty()) {
			return;
		}
		if (sapling.getItem() instanceof BlockItem blockItem) {
			BlockState planted = blockItem.getBlock().defaultBlockState();
			if (planted.canSurvive(level, at)) {
				level.setBlock(at, planted, Block.UPDATE_ALL);
				setChanged();
				return;
			}
		}
		stash(sapling);
	}

	/** 원목 → 같은 종류 묘목 (참나무 원목 → 참나무 묘목). 못 찾으면 null. */
	private static @Nullable Item preferredSapling(@Nullable Block log) {
		if (log == null) {
			return null;
		}
		Identifier id = BuiltInRegistries.BLOCK.getKey(log);
		String path = id.getPath().replace("stripped_", "").replace("_log", "_sapling").replace("_wood", "_sapling");
		return BuiltInRegistries.ITEM.getOptional(id.withPath(path)).orElse(null);
	}

	@Override
	protected boolean acceptsInput(ItemStack stack) {
		return stack.is(ItemTags.SAPLINGS);
	}

	/** 벤 블록 수 (저장). */
	public int felled() {
		return felled;
	}

	/** 아직 안 벤 블록 수. */
	public int queued() {
		return queue.size();
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.sanupcraft.tree_feller");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		felled = input.getIntOr("felled", 0);
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("felled", felled);
	}
}
