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
		registerDefaultState(stateDefinition.any().setValue(FORMED, false)
				.setValue(CasingLook.X, 0).setValue(CasingLook.Y, 0).setValue(CasingLook.Z, 0)); // 확장 M15 R5
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HeatproofBricksBlockEntity(pos, state);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		FurnaceTowerControllerBlockEntity.SHAPE.notifyControllers(level, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED, CasingLook.X, CasingLook.Y, CasingLook.Z);
	}
}
