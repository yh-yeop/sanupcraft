package io.github.yhyeop.sanupcraft.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 무른 바위 (D5). 깊은 동굴 천장에 박힌, 금이 간 심층암. 아래가 비면 모래처럼 떨어지고 (맞으면 아프다), 곡괭이로 캐면 깨진 심층암 조각을 준다.
 * 설정 {@code hazards.looseRock}이 꺼져 있으면 떨어지지 않는 평범한 돌이다.
 */
public class LooseRockBlock extends FallingBlock {
	/** 떨어지는 바위가 맞은 생물에게 주는 피해 (블록당 2, 최대 20). */
	public static final float FALL_DAMAGE_PER_BLOCK = 2.0F;
	public static final int FALL_DAMAGE_MAX = 20;

	public LooseRockBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (SanupcraftConfig.current().hazardLooseRock) {
			super.tick(state, level, pos, random);
		}
	}

	@Override
	protected void falling(FallingBlockEntity entity) {
		entity.setHurtsEntities(FALL_DAMAGE_PER_BLOCK, FALL_DAMAGE_MAX);
	}

	@Override
	public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
		return 0xFF4D4D58;
	}
}
