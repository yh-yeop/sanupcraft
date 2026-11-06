package io.github.yhyeop.sanupcraft.production.grinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 경험치 탱크. 빈손 우클릭 = 레벨 하나 몫을 받는다 (웅크리고 = 전부), 유리병 우클릭 = 경험치 10점으로 경험의 병.
 * 비교기는 찬 정도를 읽는다.
 */
public class ExperienceTankBlock extends BaseEntityBlock {
	/** 유리병 하나를 경험의 병으로 바꾸는 데 드는 경험치 점수 (던졌을 때 평균 7점 안팎보다 조금 비싸게). */
	public static final int BOTTLE_COST_POINTS = 10;

	public ExperienceTankBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ExperienceTankBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(Items.GLASS_BOTTLE) || !(level.getBlockEntity(pos) instanceof ExperienceTankBlockEntity tank)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (tank.points() < BOTTLE_COST_POINTS) {
			player.sendOverlayMessage(Component.translatable("block.sanupcraft.experience_tank.bottle_short", BOTTLE_COST_POINTS));
			return InteractionResult.FAIL;
		}
		tank.takePoints(BOTTLE_COST_POINTS);
		ItemStack bottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
		boolean lastOne = stack.getCount() == 1;
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		if (lastOne && !player.hasInfiniteMaterials()) {
			player.setItemInHand(hand, bottle);
		} else if (!player.getInventory().add(bottle)) {
			player.spawnAtLocation((net.minecraft.server.level.ServerLevel) level, bottle);
		}
		level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
		player.sendOverlayMessage(status(tank));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ExperienceTankBlockEntity tank) {
			int given = giveTo(tank, player, player.isShiftKeyDown());
			if (given > 0) {
				level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			player.sendOverlayMessage(status(tank));
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * 탱크의 경험치를 플레이어에게 준다. {@code all}이면 전부, 아니면 다음 레벨까지 모자란 만큼 (레벨 하나 몫).
	 * 준 점수를 돌려준다. 시험이 직접 부른다.
	 */
	public static int giveTo(ExperienceTankBlockEntity tank, Player player, boolean all) {
		int want = all ? tank.points() : pointsToNextLevel(player);
		int taken = tank.takePoints(want);
		if (taken > 0) {
			player.giveExperiencePoints(taken);
		}
		return taken;
	}

	/** 다음 레벨까지 모자란 경험치 점수 (1 이상). */
	public static int pointsToNextLevel(Player player) {
		int need = player.getXpNeededForNextLevel();
		int have = Math.round(player.experienceProgress * need);
		return Math.max(1, need - have);
	}

	private static Component status(ExperienceTankBlockEntity tank) {
		return Component.translatable("block.sanupcraft.experience_tank.status", tank.points(), ExperienceTankBlockEntity.CAPACITY_POINTS);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof ExperienceTankBlockEntity tank ? tank.signal() : 0;
	}
}
