package io.github.yhyeop.sanupcraft.production;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 나무 한 그루 찾기. 벌목기와 전기톱이 같이 쓴다.
 * 원목은 26방향으로 이어진 것(대각선 가지 포함), 잎은 원목에서 {@link #LEAF_REACH}칸 안의 자연 잎(플레이어가 놓은 잎은 제외)이다.
 * 한 번에 {@link #MAX_BLOCKS}개까지만 모으고, 시작점에서 가로 {@link #RADIUS}칸 밖은 따라가지 않는다 (옆 나무 숲 전체를 베지 않게).
 * 자연 잎이 하나도 안 붙은 원목 무더기(통나무집 · 쌓아 둔 원목)는 나무가 아니라고 보고 건드리지 않는다.
 */
public final class TreeFelling {
	public static final int MAX_BLOCKS = 256;
	public static final int RADIUS = 8;
	public static final int LEAF_REACH = 4;

	private TreeFelling() {
	}

	/** 찾은 나무. 원목과 잎 (겹치지 않음). */
	public record Tree(List<BlockPos> logs, List<BlockPos> leaves) {
		/** 자연 잎이 붙은 원목이면 나무다. */
		public boolean isTree() {
			return !logs.isEmpty() && !leaves.isEmpty();
		}

		public int size() {
			return logs.size() + leaves.size();
		}

		/** 베는 순서: 위쪽 원목부터 아래로, 그다음 잎. */
		public List<BlockPos> fellOrder() {
			List<BlockPos> order = new ArrayList<>(logs);
			order.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
			order.addAll(leaves);
			return order;
		}

		/** 가장 낮은 원목 (묘목을 다시 심을 자리). 시작점에 가장 가까운 것. */
		public BlockPos base(BlockPos near) {
			BlockPos best = logs.get(0);
			for (BlockPos pos : logs) {
				if (pos.getY() < best.getY() || (pos.getY() == best.getY() && pos.distSqr(near) < best.distSqr(near))) {
					best = pos;
				}
			}
			return best;
		}
	}

	public static boolean isLog(BlockState state) {
		return state.is(BlockTags.LOGS);
	}

	/** 플레이어가 놓지 않은 (저절로 시드는) 잎. */
	public static boolean isNaturalLeaf(BlockState state) {
		return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
	}

	/** 잎 한 칸의 증기 값: 원목 값의 1/4 (최소 1). */
	public static int leafCost(int logCost) {
		return Math.max(1, logCost / 4);
	}

	/**
	 * {@code seeds}(원목이어야 함)에서 이어진 나무를 찾는다. 시드가 원목이 아니면 그 시드는 무시한다.
	 * 원목과 잎을 합쳐 {@code cap}개를 넘기지 않는다.
	 */
	public static Tree scan(Level level, Collection<BlockPos> seeds, int cap) {
		Set<BlockPos> logs = new LinkedHashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		BlockPos origin = null;
		for (BlockPos seed : seeds) {
			if (isLog(level.getBlockState(seed)) && logs.add(seed)) {
				queue.add(seed);
				if (origin == null) {
					origin = seed;
				}
			}
		}
		if (origin == null) {
			return new Tree(List.of(), List.of());
		}
		while (!queue.isEmpty() && logs.size() < cap) {
			BlockPos pos = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos next = pos.offset(dx, dy, dz);
						if (logs.size() >= cap || Math.abs(next.getX() - origin.getX()) > RADIUS || Math.abs(next.getZ() - origin.getZ()) > RADIUS
								|| logs.contains(next) || !level.isLoaded(next) || !isLog(level.getBlockState(next))) {
							continue;
						}
						logs.add(next);
						queue.add(next);
					}
				}
			}
		}

		// 잎: 원목에서 여섯 방향으로 번지며 LEAF_REACH칸까지
		List<BlockPos> leaves = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>(logs);
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>(logs);
		for (int depth = 0; depth < LEAF_REACH && !frontier.isEmpty() && logs.size() + leaves.size() < cap; depth++) {
			ArrayDeque<BlockPos> nextFrontier = new ArrayDeque<>();
			for (BlockPos pos : frontier) {
				for (Direction direction : Direction.values()) {
					BlockPos next = pos.relative(direction);
					if (logs.size() + leaves.size() >= cap || !seen.add(next) || !level.isLoaded(next)) {
						continue;
					}
					if (isNaturalLeaf(level.getBlockState(next))) {
						leaves.add(next);
						nextFrontier.add(next);
					}
				}
			}
			frontier = nextFrontier;
		}
		return new Tree(new ArrayList<>(logs), leaves);
	}
}
