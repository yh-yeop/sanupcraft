package io.github.yhyeop.sanupcraft.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * 고압 파이프 (M5). 보통 증기 파이프와 같은 모양이지만 망이 따로다: 다른 고압 파이프 · 영혼 화로 · 고압 기계(정밀 조립기 · 터빈)에만 잇는다.
 * 보통 파이프와 붙여 놓아도 이어지지 않는다.
 */
public class HighPressurePipeBlock extends PipeBlock {
	public HighPressurePipeBlock(BlockBehaviour.Properties properties) {
		super(5.0F, properties);
		BlockState state = stateDefinition.any();
		for (var property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		registerDefaultState(state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		for (Direction direction : Direction.values()) {
			BlockPos neighbour = context.getClickedPos().relative(direction);
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(context.getLevel(), neighbour, context.getLevel().getBlockState(neighbour)));
		}
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, neighbourPos, neighbourState));
	}

	private static boolean connectsTo(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof HighPressurePipeBlock
				|| state.getBlock() instanceof SoulFurnaceBlock
				|| state.getBlock() instanceof io.github.yhyeop.sanupcraft.end.VoidCondenserBlock // M6 공허 응축기
				|| level.getBlockEntity(pos) instanceof HPSteamConsumer consumer && consumer.takesHighPressureSteam();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}
}
