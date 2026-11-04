package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/** 지열 증기 분출구. 용암 원천 옆에 두고 증기 파이프를 잇는다 (네더에서만). 빈손 우클릭: 상태. */
public class GeothermalVentBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public GeothermalVentBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GeothermalVentBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModPower.GEOTHERMAL_VENT_BLOCK_ENTITY,
						(l, pos, s, vent) -> GeothermalVentBlockEntity.serverTick(serverLevel, pos, s, vent))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GeothermalVentBlockEntity vent) {
			int steam = vent.steam();
			int capacity = GeothermalVentBlockEntity.STEAM_CAPACITY;
			Component message = switch (vent.status()) {
				case TOO_COLD -> Component.translatable("block.sanupcraft.geothermal_vent.too_cold");
				case NO_LAVA -> Component.translatable("block.sanupcraft.geothermal_vent.no_lava", steam, capacity);
				case RUNNING -> Component.translatable("block.sanupcraft.geothermal_vent.status", vent.lava(),
						vent.lava() * GeothermalVentBlockEntity.STEAM_PER_LAVA, steam, capacity);
			};
			player.sendOverlayMessage(message);
		}
		return InteractionResult.SUCCESS;
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
		double x = pos.getX() + 0.5 + random.nextDouble() * 0.4 - 0.2;
		double z = pos.getZ() + 0.5 + random.nextDouble() * 0.4 - 0.2;
		level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 1.05, z, 0.0, 0.12, 0.0);
		if (random.nextDouble() < 0.05) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.8F, false);
		}
	}
}
