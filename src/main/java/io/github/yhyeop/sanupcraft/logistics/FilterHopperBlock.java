package io.github.yhyeop.sanupcraft.logistics;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 필터 깔때기. 바닐라 깔때기와 같은 모양 · 놓는 법(옆구리가 향하는 쪽이 출구) · 레드스톤 잠금이고,
 * 필터 5칸에 맞는 아이템만 지나간다. 우클릭으로 필터를 연다. 모양 · 잠금은 {@link HopperBlock}을 그대로 쓴다.
 */
public class FilterHopperBlock extends HopperBlock {
	public FilterHopperBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FilterHopperBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModLogistics.FILTER_HOPPER_BLOCK_ENTITY, FilterHopperBlockEntity::serverTick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FilterHopperBlockEntity hopper) {
			player.openMenu(hopper);
		}
		return InteractionResult.SUCCESS;
	}

	/** 위에서 떨어지는 아이템 덩어리도 필터에 맞으면 받는다. */
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof FilterHopperBlockEntity hopper) {
			hopper.entityInside(state, entity);
		}
	}
}
