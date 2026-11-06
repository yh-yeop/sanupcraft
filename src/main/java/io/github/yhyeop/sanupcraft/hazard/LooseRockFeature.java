package io.github.yhyeop.sanupcraft.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 깊은 동굴(Y 0 아래) 천장의 돌 몇 칸을 무른 바위로 바꾼다. 시작 칸(공기)에서 위로 올라가 처음 만나는 돌 · 심층암을 고르고,
 * 그 둘레 같은 높이의 천장 돌도 최대 {@code max_blocks}칸까지 바꾼다. 설정 {@code hazards.looseRock}이 꺼져 있으면 아무것도 바꾸지 않는다.
 */
public record LooseRockFeature(int maxBlocks, int maxY) implements Feature {
	/** 월드 생성에서 쓰는 높이 한계 (이 높이 아래에만 놓는다). */
	public static final int DEFAULT_MAX_Y = 0;

	public static final MapCodec<LooseRockFeature> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Codec.intRange(1, 16).fieldOf("max_blocks").forGetter(LooseRockFeature::maxBlocks),
					Codec.INT.optionalFieldOf("max_y", DEFAULT_MAX_Y).forGetter(LooseRockFeature::maxY)).apply(i, LooseRockFeature::new));
	/** 천장을 찾으려고 위로 올라가는 최대 칸. */
	private static final int SEARCH_UP = 8;

	@Override
	public MapCodec<LooseRockFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!SanupcraftConfig.current().hazardLooseRock || origin.getY() >= maxY || !level.getBlockState(origin).isAir()) {
			return false;
		}
		BlockPos.MutableBlockPos cursor = origin.mutable();
		for (int i = 0; i < SEARCH_UP; i++) {
			cursor.move(Direction.UP);
			BlockState state = level.getBlockState(cursor);
			if (state.isAir() || state.is(ModHazard.TOXIC_GAS)) {
				continue;
			}
			if (!isRock(state)) {
				return false;
			}
			return placeCluster(level, random, cursor.immutable());
		}
		return false;
	}

	private boolean placeCluster(WorldGenLevel level, RandomSource random, BlockPos ceiling) {
		int placed = 0;
		int wanted = 1 + random.nextInt(maxBlocks);
		for (int dx = -1; dx <= 1 && placed < wanted; dx++) {
			for (int dz = -1; dz <= 1 && placed < wanted; dz++) {
				BlockPos pos = ceiling.offset(dx, 0, dz);
				if (isRock(level.getBlockState(pos)) && isOpen(level.getBlockState(pos.below())) && (placed == 0 || random.nextInt(3) != 0)) {
					level.setBlock(pos, ModHazard.LOOSE_ROCK.defaultBlockState(), 2);
					placed++;
				}
			}
		}
		return placed > 0;
	}

	private static boolean isOpen(BlockState state) {
		return state.isAir() || state.is(ModHazard.TOXIC_GAS);
	}

	private static boolean isRock(BlockState state) {
		return state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
	}
}
