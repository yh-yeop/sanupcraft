package io.github.yhyeop.sanupcraft.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** 보일러에서 파이프를 따라가며 증기를 받을 기계를 찾는다. */
public final class SteamNetwork {
	/** 보일러 하나가 따라가는 파이프 수 한도. */
	public static final int MAX_PIPES = 256;

	private SteamNetwork() {
	}

	/** 보일러에 바로 붙은 기계 + 파이프로 이어진 기계. 같은 기계는 한 번만. */
	public static List<SteamConsumer> findConsumers(Level level, BlockPos boiler) {
		return findConsumers(level, List.of(boiler));
	}

	/** 여러 칸(대형 보일러의 외벽 전부)에서 한꺼번에 찾는다. 시작 칸끼리는 서로 건너뛴다. */
	public static List<SteamConsumer> findConsumers(Level level, Collection<BlockPos> starts) {
		List<SteamConsumer> consumers = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>(starts);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(starts);
		int pipes = 0;
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (!seen.add(next) || !level.isLoaded(next)) {
					continue;
				}
				if (level.getBlockState(next).getBlock() instanceof SteamPipeBlock) {
					if (++pipes <= MAX_PIPES) {
						queue.add(next);
					}
				} else if (level.getBlockEntity(next) instanceof SteamConsumer consumer) {
					consumers.add(consumer);
				}
			}
		}
		return consumers;
	}
}
