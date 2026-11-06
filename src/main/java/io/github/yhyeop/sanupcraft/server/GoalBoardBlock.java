package io.github.yhyeop.sanupcraft.server;

import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 공용 목표판 (T3). 목표 아이템을 들고 우클릭하면 든 것을 전부 넣는다 (크리에이티브는 줄지 않고 세기만 한다).
 * 빈손 우클릭: 진행도. 웅크리고 빈손 우클릭: 많이 넣은 사람 상위 3명. 로직은 {@link GoalBoardBlockEntity}.
 */
public class GoalBoardBlock extends BaseEntityBlock {
	public GoalBoardBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GoalBoardBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof GoalBoardBlockEntity board) || !board.isGoalItem(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			int accepted = board.insert(player, stack);
			if (accepted > 0 && !player.hasInfiniteMaterials()) {
				stack.shrink(accepted);
			}
			// 방금 마무리됐으면 목표가 이미 다음 것으로 넘어가 있다
			player.sendOverlayMessage(accepted > 0 ? Component.translatable("block.sanupcraft.goal_board.added", accepted, board.status()) : board.status());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GoalBoardBlockEntity board) {
			if (player.isSecondaryUseActive()) {
				player.sendSystemMessage(Component.translatable("block.sanupcraft.goal_board.top_title", board.completedGoals()).withStyle(ChatFormatting.GOLD));
				if (board.topContributors(3).isEmpty()) {
					player.sendSystemMessage(Component.translatable("block.sanupcraft.goal_board.no_contributors").withStyle(ChatFormatting.GRAY));
				}
				int rank = 1;
				for (Map.Entry<String, Integer> entry : board.topContributors(3)) {
					player.sendSystemMessage(Component.translatable("block.sanupcraft.goal_board.top_row", rank++, entry.getKey(), entry.getValue()));
				}
			} else {
				player.sendOverlayMessage(board.status());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
