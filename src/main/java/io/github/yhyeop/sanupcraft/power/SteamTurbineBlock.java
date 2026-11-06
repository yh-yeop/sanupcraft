package io.github.yhyeop.sanupcraft.power;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.nether.ModNether;

/** 증기 터빈 (M5). 고압 파이프와 보통 증기 파이프를 함께 잇는다: 고압 1 → 보통 3. 빈손 우클릭: 상태. */
public class SteamTurbineBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public SteamTurbineBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SteamTurbineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModNether.STEAM_TURBINE_BLOCK_ENTITY, (l, pos, s, turbine) -> SteamTurbineBlockEntity.serverTick(serverLevel, pos, s, turbine))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamTurbineBlockEntity turbine) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.steam_turbine.status",
					turbine.highPressure(), SteamTurbineBlockEntity.HP_CAPACITY, SanupcraftConfig.current().turbineMultiplier,
					turbine.steam(), SteamTurbineBlockEntity.STEAM_CAPACITY));
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
		level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + random.nextDouble() * 0.4 - 0.2, pos.getY() + 1.05,
				pos.getZ() + 0.5 + random.nextDouble() * 0.4 - 0.2, 0.0, 0.1, 0.0);
		if (random.nextFloat() < 0.15F) {
			// M11 X2: 돌 때 옆으로 김이 빠진다
			Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5 + side.getStepX() * 0.55, pos.getY() + 0.5, pos.getZ() + 0.5 + side.getStepZ() * 0.55,
					side.getStepX() * 0.1, 0.02, side.getStepZ() * 0.1);
		}
		if (random.nextDouble() < 0.06) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.MINECART_INSIDE, SoundSource.BLOCKS, 0.15F, 1.6F, false);
		}
	}
}
