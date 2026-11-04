package io.github.yhyeop.sanupcraft.logistics;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** 엔더 닻. 튀긴 후렴과를 들고 우클릭하면 넣는다 (깔때기로도). 빈손 우클릭: 남은 시간. */
public class EnderAnchorBlock extends BaseEntityBlock {
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public EnderAnchorBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnderAnchorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, ModLogistics.ENDER_ANCHOR_BLOCK_ENTITY,
						(l, pos, s, anchor) -> EnderAnchorBlockEntity.serverTick(serverLevel, pos, s, anchor))
				: null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!EnderAnchorBlockEntity.isFuel(stack) || !(level.getBlockEntity(pos) instanceof EnderAnchorBlockEntity anchor)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			if (anchor.insertFuel(stack) <= 0) {
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.ender_anchor.full"));
				return InteractionResult.FAIL;
			}
			level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.8F, 1.2F);
			player.sendOverlayMessage(status(anchor));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnderAnchorBlockEntity anchor) {
			player.sendOverlayMessage(status(anchor));
		}
		return InteractionResult.SUCCESS;
	}

	private static Component status(EnderAnchorBlockEntity anchor) {
		long ticks = anchor.remainingTicks();
		if (ticks <= 0) {
			return Component.translatable("block.sanupcraft.ender_anchor.empty");
		}
		long minutes = (ticks + 1199) / 1200;
		return Component.translatable("block.sanupcraft.ender_anchor.status", minutes);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(ACTIVE) && random.nextInt(2) == 0) {
			level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0, 0.05, 0.0);
		}
	}
}
