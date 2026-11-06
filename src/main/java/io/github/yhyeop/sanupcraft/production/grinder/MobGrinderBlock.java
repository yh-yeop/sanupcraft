package io.github.yhyeop.sanupcraft.production.grinder;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 몹 분쇄기. 증기 파이프를 이어 두면 위 칸의 몹을 벤다. 빈손 우클릭: 안 저장고의 경험치를 받는다 (없으면 상태).
 */
public class MobGrinderBlock extends BaseEntityBlock {
	public MobGrinderBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MobGrinderBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModGrinder.MOB_GRINDER_BLOCK_ENTITY,
						(l, pos, s, grinder) -> MobGrinderBlockEntity.serverTick(serverLevel, pos, s, grinder))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MobGrinderBlockEntity grinder) {
			int points = grinder.takeBufferPoints(grinder.bufferPoints());
			if (points > 0) {
				player.giveExperiencePoints(points);
				level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.mob_grinder.status", grinder.steamStored(), grinder.steamCapacity(),
					points, grinder.bufferPoints()));
		}
		return InteractionResult.SUCCESS;
	}
}
