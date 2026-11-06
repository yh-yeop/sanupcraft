package io.github.yhyeop.sanupcraft.production;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 작물 수확기 · 벌목기 블록. 앞(놓을 때 플레이어가 바라본 쪽)이 일하는 방향이다.
 * 빈손 우클릭: 9칸 보관함 열기. 웅크리고 빈손 우클릭: 상태(증기 · 하는 일).
 * 증기 파이프는 블록 엔티티가 {@code SteamConsumer}라서 저절로 이어진다.
 */
public class ProductionBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	private final Supplier<BlockEntityType<? extends SteamWorkerBlockEntity>> type;
	private final BiFunction<BlockPos, BlockState, SteamWorkerBlockEntity> factory;

	public ProductionBlock(BlockBehaviour.Properties properties, Supplier<BlockEntityType<? extends SteamWorkerBlockEntity>> type,
			BiFunction<BlockPos, BlockState, SteamWorkerBlockEntity> factory) {
		super(properties);
		this.type = type;
		this.factory = factory;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return factory.apply(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		if (!(level instanceof ServerLevel serverLevel) || given != type.get()) {
			return null;
		}
		return (l, pos, s, entity) -> {
			if (entity instanceof SteamWorkerBlockEntity worker) {
				worker.serverTick(serverLevel, pos, s);
			}
		};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamWorkerBlockEntity worker) {
			if (player.isSecondaryUseActive()) {
				player.sendOverlayMessage(status(worker));
			} else {
				player.openMenu(worker);
			}
		}
		return InteractionResult.SUCCESS;
	}

	private static Component status(SteamWorkerBlockEntity worker) {
		String key = worker.getBlockState().getBlock().getDescriptionId();
		String state = worker.hasPending() ? ".full" : worker.steamStored() <= 0 ? ".no_steam" : worker.isActive() ? ".working" : ".idle";
		return Component.translatable(key + ".status" + state, worker.steamStored(), worker.steamCapacity());
	}

	/** 놓을 때 바라본 쪽이 일하는 앞이다 (밭을 보고 놓으면 앞면이 밭을 향한다). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
		builder.add(FACING);
	}
}
