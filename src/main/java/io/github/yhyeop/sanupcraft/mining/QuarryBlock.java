package io.github.yhyeop.sanupcraft.mining;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 증기 굴착기. 증기 파이프를 잇고 위에 상자(또는 깔때기 · 벨트)를 둔다. 빈손 우클릭: 상태. */
public class QuarryBlock extends BaseEntityBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public QuarryBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new QuarryBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModMining.QUARRY_BLOCK_ENTITY, (l, pos, s, quarry) -> QuarryBlockEntity.serverTick(serverLevel, pos, s, quarry))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
			// 시작 전에는 layer가 0이라 첫 층(굴착기 바로 아래)을 대신 보여 준다
			int layer = quarry.started() ? quarry.layer() : pos.getY() - 1;
			int steam = quarry.steam();
			int capacity = QuarryBlockEntity.STEAM_CAPACITY;
			Component message = switch (quarry.status()) {
				case NOT_STARTED -> Component.translatable("block.sanupcraft.steam_quarry.not_started", steam, capacity);
				case NO_STEAM -> Component.translatable("block.sanupcraft.steam_quarry.no_steam", layer, quarry.mined(), steam, capacity);
				case BLOCKED -> Component.translatable("block.sanupcraft.steam_quarry.blocked", layer, quarry.mined());
				case FINISHED -> Component.translatable("block.sanupcraft.steam_quarry.finished", layer, quarry.mined(), steam, capacity);
				case RUNNING -> Component.translatable("block.sanupcraft.steam_quarry.status", layer, quarry.mined(), steam, capacity);
			};
			player.sendOverlayMessage(message);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(POWERED)) {
			level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0, 0.05, 0.0);
		}
	}
}
