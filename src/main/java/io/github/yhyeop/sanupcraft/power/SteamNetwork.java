package io.github.yhyeop.sanupcraft.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 보일러에서 파이프를 따라가며 증기를 받을 기계를 찾는다.
 * 고압 증기(M5)는 같은 방식으로 고압 파이프만 따라가며 {@link HPSteamConsumer}만 찾는다. 두 망은 섞이지 않는다.
 * 대형 보일러(M8)는 외벽 전부를 시작 칸으로 넘긴다.
 */
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
		return find(level, starts, block -> block instanceof SteamPipeBlock,
				be -> be instanceof SteamConsumer consumer && consumer.takesNormalSteam() ? consumer : null);
	}

	/** 영혼 화로에 바로 붙은 고압 기계 + 고압 파이프로 이어진 고압 기계. */
	public static List<HPSteamConsumer> findHighPressureConsumers(Level level, BlockPos source) {
		return findHighPressureConsumers(level, List.of(source));
	}

	public static List<HPSteamConsumer> findHighPressureConsumers(Level level, Collection<BlockPos> starts) {
		return find(level, starts, block -> block instanceof HighPressurePipeBlock,
				be -> be instanceof HPSteamConsumer consumer && consumer.takesHighPressureSteam() ? consumer : null);
	}

	/** {@code isPipe} 블록을 따라가며, {@code asConsumer}가 null이 아닌 블록 엔티티를 모은다. */
	private static <T> List<T> find(Level level, Collection<BlockPos> starts, Predicate<Block> isPipe, Function<BlockEntity, T> asConsumer) {
		List<T> consumers = new ArrayList<>();
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
				if (isPipe.test(level.getBlockState(next).getBlock())) {
					if (++pipes <= MAX_PIPES) {
						queue.add(next);
					}
				} else {
					BlockEntity be = level.getBlockEntity(next);
					T consumer = be == null ? null : asConsumer.apply(be);
					if (consumer != null) {
						consumers.add(consumer);
					}
				}
			}
		}
		return consumers;
	}
}
