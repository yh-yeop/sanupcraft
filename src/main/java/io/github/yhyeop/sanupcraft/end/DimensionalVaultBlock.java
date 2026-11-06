package io.github.yhyeop.sanupcraft.end;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 차원 창고 (M6). 염료를 들고 우클릭하면 그 색이 채널 (엔더 운반기처럼). 같은 채널은 어느 차원에서나 같은 54칸 ({@link VaultData}).
 * 빈손 우클릭: 연다. 웅크리고 빈손 우클릭: 채널 잠그기 · 풀기 (주인 · 같은 팀 · OP), 못 바꾸면 채널과 잠금 상태를 보여 준다.
 */
public class DimensionalVaultBlock extends BaseEntityBlock {
	public DimensionalVaultBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DimensionalVaultBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
		super.setPlacedBy(level, pos, state, by, itemStack);
		if (!level.isClientSide() && by instanceof Player player && level.getBlockEntity(pos) instanceof DimensionalVaultBlockEntity vault) {
			vault.setPlacer(player);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		DyeColor color = stack.get(DataComponents.DYE);
		if (color == null || !(level.getBlockEntity(pos) instanceof DimensionalVaultBlockEntity vault)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			VaultData.Channel target = VaultData.get(level.getServer()).channel(color);
			if (!target.canUse(player)) {
				player.sendOverlayMessage(lockedMessage(color, target));
				return InteractionResult.FAIL;
			}
			vault.setChannel(color);
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.dimensional_vault.channel", colorName(color)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof DimensionalVaultBlockEntity vault)) {
			return InteractionResult.SUCCESS;
		}
		VaultData.Channel shared = vault.channel();
		if (shared == null) {
			return InteractionResult.SUCCESS;
		}
		DyeColor color = vault.channelColor();
		if (player.isSecondaryUseActive()) {
			if (shared.toggleLock(player)) {
				player.sendOverlayMessage(Component.translatable(shared.isLocked()
						? "block.sanupcraft.dimensional_vault.lock_on" : "block.sanupcraft.dimensional_vault.lock_off", colorName(color)));
			} else {
				player.sendOverlayMessage(lockedMessage(color, shared));
			}
		} else if (!shared.canUse(player)) {
			player.sendOverlayMessage(lockedMessage(color, shared));
		} else {
			player.openMenu(vault.menuProvider(shared));
		}
		return InteractionResult.SUCCESS;
	}

	private static Component lockedMessage(DyeColor color, VaultData.Channel channel) {
		return channel.isLocked()
				? Component.translatable("block.sanupcraft.dimensional_vault.locked", colorName(color), channel.ownerName())
				: Component.translatable("block.sanupcraft.dimensional_vault.channel", colorName(color));
	}

	private static Component colorName(DyeColor color) {
		return Component.translatable("color.minecraft." + color.getName());
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
		}
	}
}
