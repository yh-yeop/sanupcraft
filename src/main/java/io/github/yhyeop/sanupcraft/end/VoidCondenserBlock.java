package io.github.yhyeop.sanupcraft.end;

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

/** 공허 응축기 (M6). 엔드에서 바로 아래가 끝까지 비어 있는(공허) 자리에 매달아 두면 고압 증기를 낸다. 고압 파이프로 잇는다. 빈손 우클릭: 상태. */
public class VoidCondenserBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public VoidCondenserBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VoidCondenserBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModEnd.VOID_CONDENSER_BLOCK_ENTITY,
						(l, pos, s, condenser) -> VoidCondenserBlockEntity.serverTick(serverLevel, pos, s, condenser))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof VoidCondenserBlockEntity condenser) {
			player.sendOverlayMessage(statusMessage(serverLevel, pos, condenser));
		}
		return InteractionResult.SUCCESS;
	}

	/** 빈손 우클릭 문구: 고압 증기 + 안 도는 이유 하나. */
	public static Component statusMessage(ServerLevel level, BlockPos pos, VoidCondenserBlockEntity condenser) {
		int steam = condenser.steam();
		int capacity = VoidCondenserBlockEntity.STEAM_CAPACITY;
		return switch (condenser.status(level, pos)) {
			case NOT_END -> Component.translatable("block.sanupcraft.void_condenser.not_end");
			case NOT_VOID -> Component.translatable("block.sanupcraft.void_condenser.not_void", steam, capacity);
			case RUNNING -> Component.translatable("block.sanupcraft.void_condenser.status", VoidCondenserBlockEntity.steamPerTick(), steam, capacity);
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		double x = pos.getX() + 0.5 + random.nextDouble() * 0.6 - 0.3;
		double z = pos.getZ() + 0.5 + random.nextDouble() * 0.6 - 0.3;
		level.addParticle(ParticleTypes.REVERSE_PORTAL, x, pos.getY() - 0.05, z, 0.0, 0.08, 0.0);
		level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.1, 0.0);
	}
}
