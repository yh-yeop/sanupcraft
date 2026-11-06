package io.github.yhyeop.sanupcraft.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 보스 기록판 (T2). 빈손 우클릭하면 보스마다 상위 {@link BossRecords#SHOWN}개 기록을 채팅으로 보여 준다.
 * 기록은 서버 전체에서 하나라서 판을 몇 개 놓아도 같은 내용이다 (그래서 블록 엔티티가 없다).
 */
public class BossRecordBoardBlock extends Block {
	/** 기록이 없어도 늘 보이는 보스들 (진행 순서). */
	private static final List<String> KNOWN = List.of("sanupcraft:rusty_guardian", "sanupcraft:furnace_warden");

	public BossRecordBoardBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getServer() != null) {
			lines(BossRecords.get(level.getServer())).forEach(serverPlayer::sendSystemMessage);
		}
		return InteractionResult.SUCCESS;
	}

	/** 채팅에 보일 줄들 (시험에서도 쓴다). */
	public static List<Component> lines(BossRecords records) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("block.sanupcraft.boss_board.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		List<String> ids = new ArrayList<>(KNOWN);
		for (String id : records.bossIds()) {
			if (!ids.contains(id)) {
				ids.add(id);
			}
		}
		for (String id : ids) {
			lines.add(bossName(id).copy().withStyle(ChatFormatting.YELLOW));
			List<BossRecords.Entry> top = records.top(id, BossRecords.SHOWN);
			if (top.isEmpty()) {
				lines.add(Component.translatable("block.sanupcraft.boss_board.empty").withStyle(ChatFormatting.GRAY));
				continue;
			}
			for (int rank = 0; rank < top.size(); rank++) {
				BossRecords.Entry entry = top.get(rank);
				Component row = Component.translatable("block.sanupcraft.boss_board.row", rank + 1, BossRecords.formatTime(entry.ticks()),
						entry.partySize(), String.join(", ", entry.players()));
				if (entry.tier() > 0) {
					row = row.copy().append(Component.translatable("block.sanupcraft.boss_board.tier", entry.tier()));
				}
				lines.add(row);
			}
		}
		return lines;
	}

	private static Component bossName(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key != null && BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
			return BuiltInRegistries.ENTITY_TYPE.getValue(key).getDescription();
		}
		return Component.literal(id);
	}
}
