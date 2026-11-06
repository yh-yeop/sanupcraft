package io.github.yhyeop.sanupcraft.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 대형 보일러 외벽. 혼자서는 그냥 벽이고, 제어기가 모양을 확인하면 {@link #FORMED}로 무늬가 바뀐다.
 * 부서지면 둘레의 제어기에 바로 다시 확인하라고 알린다. 증기 파이프가 붙는다 (외벽에 붙은 파이프로도 증기가 나간다).
 */
public class BigBoilerCasingBlock extends Block {
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	public BigBoilerCasingBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FORMED, false)
				.setValue(CasingLook.X, 0).setValue(CasingLook.Y, 0).setValue(CasingLook.Z, 0)); // 확장 M15 R5
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		BigBoilerControllerBlockEntity.SHAPE.notifyControllers(level, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED, CasingLook.X, CasingLook.Y, CasingLook.Z);
	}
}
