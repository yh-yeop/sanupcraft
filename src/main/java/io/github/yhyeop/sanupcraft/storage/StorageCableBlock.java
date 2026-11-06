package io.github.yhyeop.sanupcraft.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * 창고 관. 다른 관 · 창고 단말 · 재고 유지기 · 보관함(상자 · 서랍 …) 쪽으로 이어진 모양이 된다.
 * 놓거나 부수거나 옆이 바뀌면 창고 네트워크 캐시를 버린다.
 */
public class StorageCableBlock extends PipeBlock {
	public StorageCableBlock(BlockBehaviour.Properties properties) {
		super(4.0F, properties);
		BlockState state = stateDefinition.any();
		for (var property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		registerDefaultState(state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		BlockGetter level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		for (Direction direction : Direction.values()) {
			BlockPos neighbour = pos.relative(direction);
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, neighbour, level.getBlockState(neighbour)));
		}
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		StorageNetwork.invalidate();
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, neighbourPos, neighbourState));
	}

	static boolean connectsTo(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof StorageCableBlock
				|| level.getBlockEntity(pos) instanceof StorageNetwork.Member
				|| StorageNetwork.isStorage(level.getBlockEntity(pos));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		StorageNetwork.invalidate();
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		StorageNetwork.invalidate();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}
}
