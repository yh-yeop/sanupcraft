package io.github.yhyeop.sanupcraft.hazard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 깊은 동굴 공기 칸(Y 0 아래)에 유독 가스 덩어리를 깐다. 시작 칸에서 이어진 공기 칸을 반경 3 안에서 최대 {@code max_blocks}칸 채운다.
 * 불 · 용암 · 횃불 옆 칸은 건너뛴다 (깔자마자 터지지 않게). 설정 {@code hazards.toxicGas}가 꺼져 있으면 아무것도 깔지 않는다.
 */
public record ToxicGasPocketFeature(int maxBlocks, int maxY) implements Feature {
	/** 월드 생성에서 쓰는 높이 한계 (이 높이 아래에만 놓는다). */
	public static final int DEFAULT_MAX_Y = 0;

	public static final MapCodec<ToxicGasPocketFeature> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Codec.intRange(1, 64).fieldOf("max_blocks").forGetter(ToxicGasPocketFeature::maxBlocks),
					Codec.INT.optionalFieldOf("max_y", DEFAULT_MAX_Y).forGetter(ToxicGasPocketFeature::maxY)).apply(i, ToxicGasPocketFeature::new));
	/** 시작 칸에서 이 거리(칸)를 넘지 않는다 (이웃 청크 영역 안에 머문다). */
	private static final int REACH = 3;

	@Override
	public MapCodec<ToxicGasPocketFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!SanupcraftConfig.current().hazardToxicGas || origin.getY() >= maxY || !level.getBlockState(origin).isAir()) {
			return false;
		}
		Set<BlockPos> seen = new HashSet<>();
		List<BlockPos> chosen = new ArrayList<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && chosen.size() < maxBlocks) {
			BlockPos pos = queue.poll();
			if (!nearIgnition(level, pos)) {
				chosen.add(pos);
			}
			List<Direction> order = new ArrayList<>(List.of(Direction.values()));
			Collections.shuffle(order, new Random(random.nextLong()));
			for (Direction direction : order) {
				BlockPos next = pos.relative(direction);
				if (!seen.contains(next) && next.getY() < maxY && Math.abs(next.getX() - origin.getX()) <= REACH
						&& Math.abs(next.getY() - origin.getY()) <= REACH && Math.abs(next.getZ() - origin.getZ()) <= REACH
						&& level.getBlockState(next).isAir()) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		for (BlockPos pos : chosen) {
			level.setBlock(pos, ModHazard.TOXIC_GAS.defaultBlockState(), 2);
		}
		return !chosen.isEmpty();
	}

	private static boolean nearIgnition(WorldGenLevel level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (ToxicGasBlock.isIgnitionSource(level.getBlockState(pos.relative(direction)))) {
				return true;
			}
		}
		return false;
	}
}
