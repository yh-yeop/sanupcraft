package io.github.yhyeop.sanupcraft.logistics;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 엔더 송신기 · 수신기. 염료를 들고 우클릭하면 그 색이 채널이 된다. 빈손 우클릭: 송신기는 채널과 이어진 수신기 수를 보여 주고,
 * 수신기는 연다 (웅크리고 우클릭하면 채널을 보여 준다).
 */
public class EnderLinkBlock extends BaseEntityBlock {
	private final boolean sender;

	public EnderLinkBlock(boolean sender, BlockBehaviour.Properties properties) {
		super(properties);
		this.sender = sender;
	}

	public boolean isSender() {
		return sender;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnderLinkBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel ? createTickerHelper(type, ModLogistics.ENDER_LINK_BLOCK_ENTITY, EnderLinkBlockEntity::serverTick) : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		DyeColor color = stack.get(DataComponents.DYE);
		if (color == null || !(level.getBlockEntity(pos) instanceof EnderLinkBlockEntity link)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			link.setChannel(color);
			player.sendOverlayMessage(channelMessage(link));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnderLinkBlockEntity link) {
			if (link.isSender()) {
				int count = link.linkedReceivers();
				Component channel = colorName(link);
				player.sendOverlayMessage(count > 0
						? Component.translatable("block.sanupcraft.ender_link.sender_status", channel, count)
						: Component.translatable("block.sanupcraft.ender_link.no_receiver", channel));
			} else if (player.isSecondaryUseActive()) {
				player.sendOverlayMessage(channelMessage(link));
			} else {
				player.openMenu(link);
			}
		}
		return InteractionResult.SUCCESS;
	}

	private static Component channelMessage(EnderLinkBlockEntity link) {
		return Component.translatable("block.sanupcraft.ender_link.channel", colorName(link));
	}

	private static Component colorName(EnderLinkBlockEntity link) {
		return Component.translatable("color.minecraft." + link.channel().getName());
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(),
					0.0, sender ? 0.4 : -0.4, 0.0);
		}
	}
}
