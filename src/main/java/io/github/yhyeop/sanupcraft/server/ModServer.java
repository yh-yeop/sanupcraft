package io.github.yhyeop.sanupcraft.server;

import java.util.Set;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 10명 서버 운영 (M10): 팀 (T1), 보스 기록판 (T2), 공용 목표판 (T3), 설명서 (T4).
 * 팀 · 기록 · 설명서 지급 명단은 월드 저장 데이터라서 서버가 바뀌어도 이어진다.
 */
public final class ModServer {
	public static final BossRecordBoardBlock BOSS_RECORD_BOARD = ModRegistry.block("boss_record_board", BossRecordBoardBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.GOLD)
			.strength(3.5F, 1200.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final GoalBoardBlock GOAL_BOARD = ModRegistry.block("goal_board", GoalBoardBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.strength(5.0F, 1200.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final BlockEntityType<GoalBoardBlockEntity> GOAL_BOARD_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("goal_board"), new BlockEntityType<>(GoalBoardBlockEntity::new, Set.of(GOAL_BOARD)));

	/** 설명서: 처음 접속하면 받고, 책 + 철판으로도 만든다. 쓰는 법은 {@link GuideBook}. */
	public static final Item GUIDE_BOOK = ModRegistry.item("guide_book", GuideBookItem::new, new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.UNCOMMON)
			.component(DataComponents.WRITTEN_BOOK_CONTENT, GuideBook.content()));

	private ModServer() {
	}

	public static void init() {
		Teams.init();
		// /sanupcraft team (누구나) · goal (OP). 뿌리 sanupcraft는 다른 명령과 합쳐진다
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("sanupcraft")
				.then(TeamCommand.node())
				.then(GoalCommand.node(context))));
	}
}
