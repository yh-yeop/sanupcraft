package io.github.yhyeop.sanupcraft.combat;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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

/** 전투 신호기 (C5). 놓은 사람이 주인 (주인의 팀에게만 버프). 빈손 우클릭: 증기 · 주인 · 반경. */
public class CombatBeaconBlock extends BaseEntityBlock {
	public CombatBeaconBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CombatBeaconBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModCombat.COMBAT_BEACON_BLOCK_ENTITY, (l, pos, s, be) -> be.serverTick(serverLevel, pos))
				: null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
		super.setPlacedBy(level, pos, state, by, itemStack);
		if (!level.isClientSide() && by instanceof Player player && level.getBlockEntity(pos) instanceof CombatBeaconBlockEntity beacon) {
			beacon.setOwner(player);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CombatBeaconBlockEntity beacon) {
			player.sendOverlayMessage(beacon.status());
		}
		return InteractionResult.SUCCESS;
	}

	/** 상태 문구 (주인이 없으면 "모두"). */
	static Component ownerText(CombatBeaconBlockEntity beacon) {
		return beacon.ownerName().isEmpty() ? Component.translatable("block.sanupcraft.combat_beacon.everyone") : Component.literal(beacon.ownerName());
	}
}
