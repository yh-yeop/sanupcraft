package io.github.yhyeop.sanupcraft.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** 양동이로 유체 블록(탱크 · 펌프)에 붓고 퍼 내기. 클라이언트는 아이템만 보고 성공으로 치고, 서버가 실제로 판단한다. */
public final class FluidBuckets {
	private FluidBuckets() {
	}

	/**
	 * 물 · 용암 양동이: 붓기 ({@code canPour}일 때만). 빈 양동이: 1양동이 이상 들어 있으면 퍼 내기.
	 * 양동이가 아니면 {@link InteractionResult#TRY_WITH_EMPTY_HAND}.
	 */
	public static InteractionResult use(FluidHolder holder, boolean canPour, ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
		FluidKind poured = FluidKind.ofBucket(stack);
		if (poured != null && canPour) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (holder.fill(poured, FluidKind.BUCKET, true) < FluidKind.BUCKET) {
				FluidKind inside = holder.fluid();
				player.sendOverlayMessage(Component.translatable(inside != null && inside != poured
						? "block.sanupcraft.tank.other_fluid" : "block.sanupcraft.tank.full"));
				return InteractionResult.FAIL;
			}
			holder.fill(poured, FluidKind.BUCKET, false);
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
			level.playSound(null, pos, poured.emptySound(), SoundSource.BLOCKS, 1.0F, 1.0F);
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.BUCKET)) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			FluidKind inside = holder.fluid();
			if (inside == null || inside.bucket() == null || holder.drain(inside, FluidKind.BUCKET, true) < FluidKind.BUCKET) {
				player.sendOverlayMessage(Component.translatable("block.sanupcraft.tank.not_enough"));
				return InteractionResult.FAIL;
			}
			holder.drain(inside, FluidKind.BUCKET, false);
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(inside.bucket())));
			level.playSound(null, pos, inside.fillSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/** "물 4000/16000 mB" 또는 "비었다 · 0/16000 mB". */
	public static Component contents(FluidHolder holder) {
		FluidKind kind = holder.fluid();
		if (kind == null) {
			return Component.translatable("block.sanupcraft.tank.empty", holder.fluidCapacity());
		}
		return Component.translatable("block.sanupcraft.tank.contents", Component.translatable(kind.translationKey()), holder.fluidAmount(), holder.fluidCapacity());
	}
}
