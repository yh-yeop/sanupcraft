package io.github.yhyeop.sanupcraft.power;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 증기 계기판. 기계 · 보일러 · 분출구 · 굴착기 · 벨트 모터의 면에 붙여 놓으면 그 증기 저장률을 비교기 신호(0~15)로 내보낸다.
 * 눈금판이 보이는 쪽(FACING)이 놓은 면이고, 읽는 블록은 그 반대쪽이다. 빈손 우클릭: 저장량 · 최근 1초 들어온 양.
 */
public class SteamGaugeBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public SteamGaugeBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SteamGaugeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModPower.STEAM_GAUGE_BLOCK_ENTITY,
						(l, pos, s, gauge) -> SteamGaugeBlockEntity.serverTick(serverLevel, pos, s, gauge))
				: null;
	}

	/** 클릭한 면이 눈금판 쪽이 된다 (클릭한 블록에 붙는다). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamGaugeBlockEntity gauge) {
			SteamStorage target = gauge.target();
			if (target == null) {
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.steam_gauge.no_target"));
			} else {
				int stored = target.steamStored();
				int capacity = target.steamCapacity();
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.steam_gauge.status",
						stored, capacity, capacity > 0 ? stored * 100 / capacity : 0, target.steamReceivedLastSecond()));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof SteamGaugeBlockEntity gauge ? gauge.signal() : 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
