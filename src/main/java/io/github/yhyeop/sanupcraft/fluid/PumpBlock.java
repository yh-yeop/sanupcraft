package io.github.yhyeop.sanupcraft.fluid;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/** 펌프. 증기 파이프를 잇고 물 · 용암 위에 놓는다. 빈손 우클릭: 상태, 빈 양동이: 안의 유체를 1양동이 퍼 낸다. */
public class PumpBlock extends BaseEntityBlock {
	public PumpBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PumpBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModFluid.PUMP_BLOCK_ENTITY, (l, pos, s, pump) -> PumpBlockEntity.serverTick(serverLevel, pos, s, pump))
				: null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof PumpBlockEntity pump)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		return FluidBuckets.use(pump, false, stack, level, pos, player, hand);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PumpBlockEntity pump) {
			String hint = switch (pump.status()) {
				case FULL -> "block.sanupcraft.pump.hint.full";
				case NO_STEAM -> "block.sanupcraft.pump.hint.no_steam";
				case NO_SOURCE -> "block.sanupcraft.pump.hint.no_source";
				case RUNNING -> "block.sanupcraft.pump.hint.ok";
			};
			player.sendOverlayMessage(FluidBuckets.contents(pump).copy()
					.append(" · ").append(Component.translatable("block.sanupcraft.pump.steam", pump.steamStored(), pump.steamCapacity()))
					.append(" · ").append(Component.translatable(hint, SanupcraftConfig.current().pumpRadius, PumpBlockEntity.steamPerBucket())));
		}
		return InteractionResult.SUCCESS;
	}
}
