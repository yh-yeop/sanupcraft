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

/** 증기 파이프. 다른 파이프 · 보일러 · 증기를 받는 기계 쪽으로 이어진 모양이 된다. */
public class SteamPipeBlock extends PipeBlock {
	public SteamPipeBlock(BlockBehaviour.Properties properties) {
		super(5.0F, properties);
		BlockState state = stateDefinition.any();
		for (var property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		registerDefaultState(state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return connections(context.getLevel(), context.getClickedPos(), defaultBlockState());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, neighbourPos, neighbourState));
	}

	private static BlockState connections(BlockGetter level, BlockPos pos, BlockState state) {
		for (Direction direction : Direction.values()) {
			BlockPos neighbour = pos.relative(direction);
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, neighbour, level.getBlockState(neighbour)));
		}
		return state;
	}

	private static boolean connectsTo(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof SteamPipeBlock
				|| state.getBlock() instanceof BoilerBlock
				|| state.getBlock() instanceof GeothermalVentBlock
				|| state.getBlock() instanceof SteamTurbineBlock
				|| state.getBlock() instanceof io.github.yhyeop.sanupcraft.industry.BigBoilerCasingBlock // M8 묶음 A: 대형 보일러 외벽
				|| level.getBlockEntity(pos) instanceof SteamConsumer consumer && consumer.takesNormalSteam();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}
}
