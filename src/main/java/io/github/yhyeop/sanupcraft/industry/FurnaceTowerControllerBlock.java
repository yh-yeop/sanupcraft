package io.github.yhyeop.sanupcraft.industry;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 용광로 탑 제어기 (M8 묶음 B). 빈손 우클릭: 모양이 맞으면 재료 · 결과 칸 화면을 열고, 틀리면 첫 틀린 칸을 알려 준다.
 * 웅크리고 빈손 우클릭: 상태만. 비교기: 칸이 찬 정도 (상자와 같음).
 */
public class FurnaceTowerControllerBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public FurnaceTowerControllerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FurnaceTowerControllerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModFoundry.FURNACE_TOWER_CONTROLLER_BLOCK_ENTITY,
						(l, pos, s, tower) -> FurnaceTowerControllerBlockEntity.serverTick(serverLevel, pos, s, tower))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof FurnaceTowerControllerBlockEntity tower) {
			boolean formed = tower.check(serverLevel, pos, state);
			if (formed && !player.isShiftKeyDown()) {
				player.openMenu(tower);
			} else {
				player.sendOverlayMessage(status(serverLevel, tower));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** 상태 한 줄: 모양이 맞으면 고압 증기 · 굽는 개수 · 진행, 틀리면 첫 틀린 칸. 부르기 전에 모양을 확인해 둔다. */
	static Component status(ServerLevel level, FurnaceTowerControllerBlockEntity tower) {
		MultiblockShape.Problem problem = tower.problem();
		if (!tower.isFormed() && problem != null) {
			return MultiblockShape.problemMessage(level, problem, "block.sanupcraft.furnace_tower_controller");
		}
		Component line = Component.translatable("block.sanupcraft.furnace_tower_controller.status",
				tower.steam(), FurnaceTowerControllerBlockEntity.STEAM_CAPACITY, tower.batchUnits(), tower.progressPercent());
		String hint;
		if (tower.batchUnits() <= 0) {
			hint = "block.sanupcraft.furnace_tower_controller.hint.idle";
		} else if (tower.steam() <= 0) {
			hint = "block.sanupcraft.furnace_tower_controller.hint.no_steam";
		} else {
			hint = "block.sanupcraft.furnace_tower_controller.hint.ok";
		}
		return line.copy().append(" · ").append(Component.translatable(hint));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		if (level.getBlockEntity(pos) instanceof FurnaceTowerControllerBlockEntity tower) {
			tower.requestCheck();
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
		FurnaceTowerControllerBlockEntity.SHAPE.unform(level, pos, state.getValue(FACING));
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	/** 굽는 동안 가운데 굴뚝(빈 기둥) 위로 연기와 불꽃. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		BlockPos top = MultiblockShape.at(pos, state.getValue(FACING), 0, 1, FurnaceTowerControllerBlockEntity.SHAPE.height() - 1);
		double x = top.getX() + 0.5;
		double z = top.getZ() + 0.5;
		level.addParticle(ParticleTypes.LARGE_SMOKE, x + random.nextDouble() * 0.5 - 0.25, top.getY() + 1.0, z + random.nextDouble() * 0.5 - 0.25, 0.0, 0.07, 0.0);
		if (random.nextDouble() < 0.3) {
			level.addParticle(ParticleTypes.FLAME, x + random.nextDouble() * 0.6 - 0.3, top.getY() + 0.4, z + random.nextDouble() * 0.6 - 0.3, 0.0, 0.02, 0.0);
		}
		if (random.nextDouble() < 0.08) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.9F, 0.8F, false);
		}
	}
}
