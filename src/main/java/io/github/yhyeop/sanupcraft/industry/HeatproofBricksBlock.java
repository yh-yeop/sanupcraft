package io.github.yhyeop.sanupcraft.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 내열 벽돌 (M8 묶음 B): 용광로 탑 외벽. 혼자서는 벽이고, 제어기가 모양을 확인하면 {@link #FORMED}로 무늬가 바뀐다.
 * 고압 파이프가 붙는다: 붙은 고압 증기를 모양이 선 제어기로 넘긴다 ({@link HeatproofBricksBlockEntity}).
 * 부서지면 둘레의 제어기에 바로 다시 확인하라고 알린다.
 */
public class HeatproofBricksBlock extends BaseEntityBlock {
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	public HeatproofBricksBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FORMED, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HeatproofBricksBlockEntity(pos, state);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		notifyControllers(level, pos);
	}

	/** 이 칸을 쓸 수 있는 제어기 (가로 ±2, 아래로 0..2칸)에 다시 확인하라고 알린다. */
	static void notifyControllers(ServerLevel level, BlockPos pos) {
		int below = FurnaceTowerControllerBlockEntity.SHAPE.height() - 1;
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-2, -below, -2), pos.offset(2, 0, 2))) {
			if (level.getBlockEntity(near) instanceof FurnaceTowerControllerBlockEntity controller) {
				controller.requestCheck();
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED);
	}
}
