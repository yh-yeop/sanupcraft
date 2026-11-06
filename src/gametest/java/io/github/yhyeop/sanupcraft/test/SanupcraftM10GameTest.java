package io.github.yhyeop.sanupcraft.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.logistics.EnderLinkBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.server.BossRecordBoardBlock;
import io.github.yhyeop.sanupcraft.server.BossRecords;
import io.github.yhyeop.sanupcraft.server.GoalBoardBlockEntity;
import io.github.yhyeop.sanupcraft.server.GuideBook;
import io.github.yhyeop.sanupcraft.server.ModServer;
import io.github.yhyeop.sanupcraft.server.TeamData;
import io.github.yhyeop.sanupcraft.server.Teams;

/** M10 10명 서버 운영 검사: 팀 (T1) · 보스 기록 (T2) · 공용 목표판 (T3) · 설명서 (T4). */
public class SanupcraftM10GameTest {
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s");

	/** 코드가 넘기는 인수 개수 (같거나 더 많이 넘기면 된다 — 문구가 쓰는 최대 번호 이상). */
	private static final Map<String, Integer> ARGUMENT_COUNTS = Map.ofEntries(
			Map.entry("block.sanupcraft.boss_board.row", 4), Map.entry("block.sanupcraft.boss_board.tier", 1),
			Map.entry("block.sanupcraft.boss_board.new_best", 4), Map.entry("block.sanupcraft.boss_board.ranked", 3),
			Map.entry("block.sanupcraft.goal_board.status", 4), Map.entry("block.sanupcraft.goal_board.added", 2),
			Map.entry("block.sanupcraft.goal_board.goal_text", 2), Map.entry("block.sanupcraft.goal_board.done", 1),
			Map.entry("block.sanupcraft.goal_board.done_with", 2), Map.entry("block.sanupcraft.goal_board.top_title", 1),
			Map.entry("block.sanupcraft.goal_board.top_row", 3), Map.entry("commands.sanupcraft.goal.set", 2),
			Map.entry("commands.sanupcraft.team.created", 1), Map.entry("commands.sanupcraft.team.joined", 1),
			Map.entry("commands.sanupcraft.team.lock_on", 1), Map.entry("commands.sanupcraft.team.lock_off", 1),
			Map.entry("commands.sanupcraft.team.entry", 4), Map.entry("commands.sanupcraft.team.entry_locked", 4),
			Map.entry("commands.sanupcraft.team.joined_notice", 2), Map.entry("commands.sanupcraft.team.left_notice", 1),
			Map.entry("commands.sanupcraft.team.new_owner", 1));

	// ---- T1 팀 ----

	/** 만들기 · 들어가기 · 잠금 · 내보내기 · 나가기(주인 승계) · 마지막 사람이 나가면 사라짐, 그리고 스코어보드 팀에 같이 맞춰진다. */
	@GameTest
	public void teamLifecycleMirrorsScoreboard(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = player(helper, "own");
		ServerPlayer friend = player(helper, "fri");
		ServerPlayer stranger = player(helper, "str");
		String name = "T" + (System.nanoTime() % 100000);
		try {
			helper.assertTrue(Teams.create(server, owner, name) == null, "팀 만들기 실패");
			helper.assertValueEqual(Teams.create(server, owner, "다른이름"), "commands.sanupcraft.team.already_in", "팀이 있는데 또 만들기");
			helper.assertValueEqual(Teams.create(server, friend, name.toLowerCase()), "commands.sanupcraft.team.name_taken", "같은 이름(대소문자 무시)");
			helper.assertValueEqual(Teams.create(server, stranger, "열세글자를훨씬넘는긴이름입니다"), "commands.sanupcraft.team.bad_name", "너무 긴 이름");

			TeamData.Team team = Teams.teamOf(server, owner.getUUID());
			helper.assertTrue(team != null && team.owner().equals(owner.getUUID()), "만든 사람이 주인이 아님");
			PlayerTeam board = server.getScoreboard().getPlayerTeam("sc_" + team.id());
			helper.assertTrue(board != null, "스코어보드 팀이 없음");
			helper.assertTrue(board.getPlayerPrefix().getString().contains(name), "이름표 접두에 팀 이름이 없음: " + board.getPlayerPrefix().getString());
			helper.assertTrue(board.getColor().isPresent(), "팀 색이 없음");
			helper.assertTrue(board.getPlayers().contains(owner.getScoreboardName()), "주인이 스코어보드 팀에 없음");

			// 기본은 열림 → 들어가기. 잠그면 못 들어온다. 주인이 아니면 잠글 수 없다
			helper.assertTrue(Teams.join(server, friend, name) == null, "열린 팀에 못 들어감");
			helper.assertValueEqual(Teams.toggleLock(server, friend), "commands.sanupcraft.team.owner_only", "팀원이 잠금");
			helper.assertTrue(Teams.toggleLock(server, owner) == null && team.locked(), "주인이 잠그지 못함");
			helper.assertValueEqual(Teams.join(server, stranger, name), "commands.sanupcraft.team.locked", "잠긴 팀에 들어감");
			helper.assertTrue(Teams.toggleLock(server, owner) == null && !team.locked(), "주인이 잠금을 풀지 못함");
			helper.assertTrue(Teams.join(server, stranger, name) == null, "풀린 팀에 못 들어감");
			helper.assertValueEqual(team.members().size(), 3, "팀원 수");

			// 내보내기: 주인만, 자기 자신은 안 됨
			helper.assertValueEqual(Teams.kick(server, friend, stranger.getScoreboardName()), "commands.sanupcraft.team.owner_only", "팀원이 내보내기");
			helper.assertValueEqual(Teams.kick(server, owner, owner.getScoreboardName()), "commands.sanupcraft.team.kick_self", "자기 자신 내보내기");
			helper.assertTrue(Teams.kick(server, owner, stranger.getScoreboardName()) == null, "주인이 내보내지 못함");
			helper.assertTrue(Teams.teamOf(server, stranger.getUUID()) == null, "내보냈는데 아직 팀원");
			helper.assertValueEqual(Teams.leave(server, stranger), "commands.sanupcraft.team.not_in", "팀이 없는 사람이 나가기");

			// 주인이 나가면 다음 사람이 주인, 마지막 사람이 나가면 팀이 사라진다
			helper.assertTrue(Teams.leave(server, owner) == null, "주인이 나가지 못함");
			helper.assertTrue(team.owner().equals(friend.getUUID()), "주인이 넘어가지 않음");
			helper.assertTrue(server.getScoreboard().getPlayersTeam(owner.getScoreboardName()) != board, "나간 사람이 스코어보드 팀에 남음");
			helper.assertTrue(Teams.leave(server, friend) == null, "마지막 사람이 나가지 못함");
			helper.assertTrue(Teams.data(server).byName(name) == null, "빈 팀이 남음");
			helper.assertTrue(server.getScoreboard().getPlayerTeam("sc_" + team.id()) == null, "스코어보드 팀이 남음");
		} finally {
			cleanup(server, owner, friend, stranger);
		}
		helper.succeed();
	}

	/** 팀 색 바꾸기는 주인만, 스코어보드 팀 색도 바뀐다. */
	@GameTest
	public void teamColorFollowsOwner(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = player(helper, "own");
		ServerPlayer friend = player(helper, "fri");
		try {
			helper.assertTrue(Teams.create(server, owner, "C" + (System.nanoTime() % 100000)) == null, "팀 만들기 실패");
			TeamData.Team team = Teams.teamOf(server, owner.getUUID());
			Teams.join(server, friend, team.name());
			helper.assertValueEqual(Teams.setColor(server, friend, net.minecraft.world.scores.TeamColor.BLUE), "commands.sanupcraft.team.owner_only", "팀원이 색 바꾸기");
			helper.assertTrue(Teams.setColor(server, owner, net.minecraft.world.scores.TeamColor.BLUE) == null, "주인이 색을 못 바꿈");
			PlayerTeam board = server.getScoreboard().getPlayerTeam("sc_" + team.id());
			helper.assertTrue(board.getColor().orElseThrow() == net.minecraft.world.scores.TeamColor.BLUE, "스코어보드 팀 색이 안 바뀜");
		} finally {
			cleanup(server, owner, friend);
		}
		helper.succeed();
	}

	/** 엔더 수신기 잠금: 주인과 같은 팀이면 열고 관리할 수 있다. 팀이 다르거나 나가면 다시 막힌다. */
	@GameTest
	public void teamMembersShareEnderLink(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = player(helper, "own");
		ServerPlayer mate = player(helper, "mat");
		ServerPlayer other = player(helper, "oth");
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModLogistics.ENDER_RECEIVER);
		EnderLinkBlockEntity receiver = helper.getBlockEntity(pos, EnderLinkBlockEntity.class);
		receiver.setOwner(owner);
		receiver.setLocked(true);
		try {
			helper.assertFalse(receiver.canUse(mate), "팀이 없는데 잠긴 수신기가 열림");
			helper.assertTrue(Teams.create(server, owner, "E" + (System.nanoTime() % 100000)) == null, "팀 만들기 실패");
			String team = Teams.teamOf(server, owner.getUUID()).name();
			helper.assertFalse(receiver.canUse(mate), "팀원이 아닌데 열림");
			Teams.join(server, mate, team);
			Teams.create(server, other, "O" + (System.nanoTime() % 100000));
			helper.assertTrue(receiver.canUse(mate), "같은 팀인데 잠긴 수신기가 안 열림");
			helper.assertTrue(receiver.canManage(mate), "같은 팀인데 잠금을 못 바꿈");
			helper.assertFalse(receiver.canUse(other), "다른 팀인데 열림");
			Teams.leave(server, mate);
			helper.assertFalse(receiver.canUse(mate), "팀을 나갔는데 열림");
		} finally {
			cleanup(server, owner, mate, other);
		}
		helper.succeed();
	}

	// ---- T2 보스 기록 ----

	/** 소환(scale) → 처치하면 기여자 이름 · 인원 · 시간이 서버 기록에 남는다. 명령어로 불러낸 보스(소환 시각 모름)는 안 남는다. */
	@GameTest
	public void bossKillIsRecorded(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		// 시험 월드는 실행마다 이어 쓰여 0틱 기록이 쌓인다 → 이 시험은 한 틱 안에 끝나니 먼저 비운다
		BossRecords.get(server).clear("sanupcraft:rusty_guardian");
		String killerName = "rk" + (System.nanoTime() % 100000);
		String helperName = "rh" + (System.nanoTime() % 100000);
		ServerPlayer killer = player(helper, killerName);
		ServerPlayer assistant = player(helper, helperName);

		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		guardian.setNoAi(true);
		guardian.party().scale(guardian, 2);
		guardian.party().record(assistant, guardian.getMaxHealth() * 0.5F);
		guardian.hurtServer(helper.getLevel(), guardian.damageSources().playerAttack(killer), 10000.0F);
		helper.assertTrue(guardian.isDeadOrDying(), "수호자가 죽지 않음");

		List<BossRecords.Entry> mine = BossRecords.get(server).top("sanupcraft:rusty_guardian", BossRecords.KEEP).stream()
				.filter(e -> e.players().contains(killerName)).toList();
		helper.assertValueEqual(mine.size(), 1, "기록 수");
		BossRecords.Entry entry = mine.get(0);
		helper.assertTrue(entry.players().contains(helperName), "도운 사람이 기록에 없음: " + entry.players());
		helper.assertValueEqual(entry.partySize(), 2, "기록의 인원");
		helper.assertTrue(entry.ticks() >= 0 && entry.ticks() < 200, "시간이 이상함: " + entry.ticks());

		// 명령어로 불러낸 보스: scale을 안 거쳐서 소환 시각이 없다 → 기록 없음
		String ghost = "rg" + (System.nanoTime() % 100000);
		ServerPlayer ghostKiller = player(helper, ghost);
		RustyGuardian summoned = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(5, 1, 5));
		summoned.setNoAi(true);
		summoned.hurtServer(helper.getLevel(), summoned.damageSources().playerAttack(ghostKiller), 10000.0F);
		helper.assertTrue(summoned.isDeadOrDying(), "두 번째 수호자가 죽지 않음");
		helper.assertTrue(BossRecords.get(server).top("sanupcraft:rusty_guardian", BossRecords.KEEP).stream().noneMatch(e -> e.players().contains(ghost)),
				"소환 시각을 모르는 보스가 기록됨");
		helper.succeed();
	}

	/** 기록은 빠른 순, 같은 시간이면 먼저 세운 쪽이 위, 상위 10개만 남고, 저장했다 읽어도 그대로. */
	@GameTest
	public void bossRecordsRankAndPersist(GameTestHelper helper) {
		BossRecords records = new BossRecords();
		helper.assertValueEqual(records.add("sanupcraft:rusty_guardian", new BossRecords.Entry(2400, List.of("a"), 1, 0)), 1, "첫 기록 순위");
		helper.assertValueEqual(records.add("sanupcraft:rusty_guardian", new BossRecords.Entry(1200, List.of("b", "c"), 2, 0)), 1, "더 빠른 기록");
		helper.assertValueEqual(records.add("sanupcraft:rusty_guardian", new BossRecords.Entry(2400, List.of("d"), 1, 0)), 3, "같은 시간은 뒤로");
		helper.assertValueEqual(records.add("sanupcraft:furnace_warden", new BossRecords.Entry(6000, List.of("a"), 1, 2)), 1, "다른 보스는 따로");
		List<BossRecords.Entry> top = records.top("sanupcraft:rusty_guardian", 3);
		helper.assertValueEqual(top.get(0).players(), List.of("b", "c"), "1위");
		helper.assertValueEqual(top.get(1).players(), List.of("a"), "2위");
		helper.assertValueEqual(top.get(2).players(), List.of("d"), "3위");
		helper.assertValueEqual(top.get(0).seconds(), 60, "초 환산");
		helper.assertValueEqual(BossRecords.formatTime(top.get(1).ticks()), "2:00", "시간 표기");

		for (int i = 0; i < 20; i++) {
			records.add("x", new BossRecords.Entry(100 + i, List.of("p" + i), 1, 0));
		}
		helper.assertValueEqual(records.top("x", 50).size(), BossRecords.KEEP, "보관 개수");
		helper.assertValueEqual(records.add("x", new BossRecords.Entry(99999, List.of("late"), 1, 0)), 0, "순위 밖 기록");

		var tag = BossRecords.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, records).getOrThrow();
		BossRecords loaded = BossRecords.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
		helper.assertValueEqual(loaded.top("sanupcraft:rusty_guardian", 3), top, "저장 후 다시 읽은 기록");
		helper.assertValueEqual(loaded.top("sanupcraft:furnace_warden", 1).get(0).tier(), 2, "저장된 강화 단계");
		helper.succeed();
	}

	/** 기록판 글: 보스마다 상위 3개만, 기록이 없는 알려진 보스도 줄이 있다. */
	@GameTest
	public void bossBoardShowsTopThree(GameTestHelper helper) {
		BossRecords records = new BossRecords();
		for (int i = 0; i < 5; i++) {
			records.add("sanupcraft:rusty_guardian", new BossRecords.Entry(1000 + i * 100, List.of("p" + i), 1 + i, i == 0 ? 3 : 0));
		}
		List<String> text = BossRecordBoardBlock.lines(records).stream().map(Component::getString).toList();
		String all = String.join("\n", text);
		helper.assertTrue(all.contains("p0") && all.contains("p1") && all.contains("p2"), "상위 3개가 없음: " + all);
		helper.assertFalse(all.contains("p3") || all.contains("p4"), "4위 이하가 보임: " + all);
		helper.assertTrue(all.contains("3"), "강화 단계가 안 보임");
		helper.assertTrue(text.stream().anyMatch(line -> line.contains(Component.translatable("entity.sanupcraft.furnace_warden").getString())),
				"기록이 없는 파수꾼 줄이 없음");
		helper.succeed();
	}

	// ---- T3 공용 목표판 ----

	/** 목표 아이템만 받고, 진행도가 오르고, 다 차면 보상 상자가 놓이고 다음 기본 목표로 넘어간다. */
	@GameTest
	public void goalBoardCompletesAndAdvances(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModServer.GOAL_BOARD);
		GoalBoardBlockEntity board = helper.getBlockEntity(pos, GoalBoardBlockEntity.class);
		helper.assertValueEqual(board.goalItemId(), Sanupcraft.id("steel_ingot"), "첫 기본 목표 아이템");
		helper.assertValueEqual(board.goalCount(), 1000, "첫 기본 목표 개수");
		helper.assertValueEqual(board.insert(null, new ItemStack(Items.IRON_INGOT, 10)), 0, "목표가 아닌 아이템을 받음");
		helper.assertTrue(board.canPlaceItem(0, new ItemStack(ModItems.STEEL_INGOT)), "목표 아이템을 받지 않음");
		helper.assertFalse(board.canPlaceItem(0, new ItemStack(Items.IRON_INGOT)), "목표가 아닌 아이템을 받으려 함");

		ServerPlayer player = player(helper, "gp" + (System.nanoTime() % 100000));
		helper.assertValueEqual(board.insert(player, new ItemStack(ModItems.STEEL_INGOT, 64)), 64, "넣은 개수");
		helper.assertValueEqual(board.progress(), 64, "진행도");
		helper.assertValueEqual(board.percent(), 6, "퍼센트");
		helper.assertValueEqual(board.topContributors(3).get(0).getKey(), player.getGameProfile().name(), "기여자");

		// 작은 목표로 바꿔 마무리: 모자란 만큼만 받는다
		board.setGoal(Sanupcraft.id("steel_ingot"), 5);
		helper.assertValueEqual(board.progress(), 0, "목표를 바꾸면 진행도 0");
		helper.assertValueEqual(board.insert(player, new ItemStack(ModItems.STEEL_INGOT, 64)), 5, "마지막에 받는 개수");
		helper.assertValueEqual(board.completedGoals(), 1, "완료한 목표 수");
		helper.assertValueEqual(board.goalItemId(), Sanupcraft.id("iron_plate"), "다음 기본 목표 아이템");
		helper.assertValueEqual(board.goalCount(), 2000, "다음 기본 목표 개수");
		helper.assertValueEqual(board.progress(), 0, "다음 목표 진행도");
		helper.assertTrue(board.topContributors(3).isEmpty(), "새 목표에 옛 기여자가 남음");

		helper.assertBlockPresent(Blocks.CHEST, pos.above());
		ChestBlockEntity chest = helper.getBlockEntity(pos.above(), ChestBlockEntity.class);
		helper.assertTrue(GoalBoardBlockEntity.REWARD_LOOT.equals(chest.getLootTable()), "상자에 보상 전리품 표가 안 걸림");
		chest.unpackLootTable(null);
		helper.assertFalse(chest.isEmpty(), "보상 상자가 비어 있음");
		helper.succeed();
	}

	/** 오른쪽 클릭으로 든 목표 아이템을 전부 넣고, 목표가 아닌 것은 무시한다. */
	@GameTest
	public void goalBoardRightClickTakesHeldStack(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModServer.GOAL_BOARD);
		GoalBoardBlockEntity board = helper.getBlockEntity(pos, GoalBoardBlockEntity.class);
		ServerPlayer player = player(helper, "rc" + (System.nanoTime() % 100000));
		BlockPos abs = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
		BlockState state = helper.getLevel().getBlockState(abs);

		ItemStack steel = new ItemStack(ModItems.STEEL_INGOT, 30);
		player.setItemInHand(InteractionHand.MAIN_HAND, steel);
		state.useItemOn(steel, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
		helper.assertValueEqual(board.progress(), 30, "우클릭으로 넣은 진행도");
		helper.assertTrue(steel.isEmpty(), "넣은 아이템이 손에서 안 줄어듦");

		ItemStack iron = new ItemStack(Items.IRON_INGOT, 5);
		state.useItemOn(iron, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
		helper.assertValueEqual(board.progress(), 30, "목표가 아닌 아이템이 들어감");
		helper.assertValueEqual(iron.getCount(), 5, "목표가 아닌 아이템이 줄어듦");
		helper.succeed();
	}

	/** 깔때기도 넣는다 (한 번에 하나씩). */
	@GameTest(maxTicks = 100)
	public void goalBoardAcceptsHopper(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModServer.GOAL_BOARD);
		GoalBoardBlockEntity board = helper.getBlockEntity(pos, GoalBoardBlockEntity.class);
		helper.setBlock(pos.above(), Blocks.HOPPER);
		HopperBlockEntity hopper = helper.getBlockEntity(pos.above(), HopperBlockEntity.class);
		hopper.setItem(0, new ItemStack(ModItems.STEEL_INGOT, 2));
		hopper.setItem(1, new ItemStack(Items.COBBLESTONE, 2));
		helper.succeedWhen(() -> {
			helper.assertValueEqual(board.progress(), 2, "깔때기가 넣은 진행도");
			helper.assertValueEqual(hopper.countItem(Items.COBBLESTONE), 2, "목표가 아닌 아이템은 안 옮겨짐");
		});
	}

	/** 깔때기 · 벨트가 스택째 넘겨도(26.3 tryMoveInItem) 남는 몫은 사라지지 않고 판 위에 떨어진다. */
	@GameTest
	public void goalBoardDropsOverflow(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModServer.GOAL_BOARD);
		GoalBoardBlockEntity board = helper.getBlockEntity(pos, GoalBoardBlockEntity.class);
		board.setGoal(Sanupcraft.id("steel_ingot"), 10);
		HopperBlockEntity.addItem(null, board, new ItemStack(ModItems.STEEL_INGOT, 64), Direction.UP);
		helper.assertValueEqual(board.completedGoals(), 1, "남은 목표만큼 채워 완료");
		int dropped = 0;
		BlockPos abs = helper.absolutePos(pos);
		for (net.minecraft.world.entity.item.ItemEntity entity : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(abs).inflate(3.0))) {
			if (entity.getItem().is(ModItems.STEEL_INGOT)) {
				dropped += entity.getItem().getCount();
			}
		}
		helper.assertValueEqual(dropped, 54, "초과분이 판 둘레에 떨어짐");
		helper.succeed();
	}

	/** 저장했다 다시 읽어도 목표 · 진행도 · 기여자가 그대로. */
	@GameTest
	public void goalBoardKeepsProgressAcrossSave(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModServer.GOAL_BOARD);
		GoalBoardBlockEntity board = helper.getBlockEntity(pos, GoalBoardBlockEntity.class);
		ServerPlayer player = player(helper, "sv" + (System.nanoTime() % 100000));
		board.setGoal(Sanupcraft.id("rivet"), 5000);
		board.insert(player, new ItemStack(SteamGear.RIVET, 12));
		CompoundTag tag = board.saveWithFullMetadata(helper.getLevel().registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(helper.absolutePos(pos), board.getBlockState(), tag, helper.getLevel().registryAccess());
		helper.assertTrue(loaded instanceof GoalBoardBlockEntity, "다시 읽은 블록 엔티티 종류");
		GoalBoardBlockEntity copy = (GoalBoardBlockEntity) loaded;
		helper.assertValueEqual(copy.goalItemId(), Sanupcraft.id("rivet"), "읽은 목표 아이템");
		helper.assertValueEqual(copy.goalCount(), 5000, "읽은 목표 개수");
		helper.assertValueEqual(copy.progress(), 12, "읽은 진행도");
		helper.assertValueEqual(copy.topContributors(1).get(0).getValue(), 12, "읽은 기여 개수");
		helper.succeed();
	}

	@GameTest
	public void goalRewardLootTableExists(GameTestHelper helper) {
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(GoalBoardBlockEntity.REWARD_LOOT);
		helper.assertTrue(table != LootTable.EMPTY, "sanupcraft:goal_reward 전리품 표가 없음");
		helper.succeed();
	}

	// ---- T4 설명서 ----

	/** 설명서는 쪽이 있고, 모든 쪽 글이 두 언어 파일에 있다. 아이템 기본 컴포넌트로 들어 있어서 레시피로 만들어도 같다. */
	@GameTest
	public void guideBookHasPages(GameTestHelper helper) {
		ItemStack book = GuideBook.create();
		WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
		helper.assertTrue(content != null, "설명서에 책 내용이 없음");
		helper.assertValueEqual(content.pages().size(), GuideBook.PAGES, "쪽 수");
		helper.assertTrue(content.pages().size() >= 10, "쪽이 너무 적음");
		JsonObject ko = readLang("ko_kr.json");
		JsonObject en = readLang("en_us.json");
		for (int page = 1; page <= GuideBook.PAGES; page++) {
			for (JsonObject lang : List.of(ko, en)) {
				String key = "book.sanupcraft.guide." + page;
				helper.assertTrue(lang.has(key), "쪽 글이 없음: " + key);
				helper.assertTrue(lang.get(key).getAsString().length() <= 260, "한 쪽에 글이 너무 길다: " + key);
			}
		}
		helper.assertFalse(ko.has("book.sanupcraft.guide." + (GuideBook.PAGES + 1)), "쪽 수보다 문구가 더 있음");
		helper.succeed();
	}

	/** 처음 접속에만 준다: 두 번째는 주지 않는다. */
	@GameTest
	public void guideBookGivenOnlyOnce(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = player(helper, "gb" + (System.nanoTime() % 100000));
		helper.assertTrue(GuideBook.giveFirstJoin(server, player), "처음인데 주지 않음");
		helper.assertValueEqual(player.getInventory().countItem(ModServer.GUIDE_BOOK), 1, "받은 설명서 수");
		helper.assertFalse(GuideBook.giveFirstJoin(server, player), "두 번째에 또 줌");
		helper.assertValueEqual(player.getInventory().countItem(ModServer.GUIDE_BOOK), 1, "두 번째 뒤 설명서 수");
		ServerPlayer another = player(helper, "gc" + (System.nanoTime() % 100000));
		helper.assertTrue(GuideBook.giveFirstJoin(server, another), "다른 사람인데 주지 않음");
		helper.succeed();
	}

	// ---- 레시피 · 문구 · 길잡이 · 실테스트 ----

	@GameTest
	public void m10RecipesAndAdvancementsLoad(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : new String[] {"guide_book", "boss_record_board", "goal_board"}) {
			var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Sanupcraft.id(id));
			helper.assertTrue(recipes.byKey(key).isPresent(), "레시피가 없음: " + id);
		}
		var advancements = helper.getLevel().getServer().getAdvancements();
		for (String id : new String[] {"goal_done", "guide_book"}) {
			helper.assertTrue(advancements.get(Sanupcraft.id(id)) != null, "발전 과제가 없음: " + id);
		}
		helper.succeed();
	}

	@GameTest
	public void m10LangKeysHaveMatchingArguments(GameTestHelper helper) {
		JsonObject ko = readLang("ko_kr.json");
		JsonObject en = readLang("en_us.json");
		for (var entry : ARGUMENT_COUNTS.entrySet()) {
			for (var lang : Map.of("ko_kr", ko, "en_us", en).entrySet()) {
				helper.assertTrue(lang.getValue().has(entry.getKey()), lang.getKey() + "에 문구가 없음: " + entry.getKey());
				int used = argumentCount(lang.getValue().get(entry.getKey()).getAsString());
				helper.assertTrue(used <= entry.getValue(), lang.getKey() + "의 인수 " + used + "개가 코드가 넘기는 " + entry.getValue() + "개보다 많음: " + entry.getKey());
			}
		}
		for (String key : new String[] {"block.sanupcraft.boss_record_board", "block.sanupcraft.goal_board", "item.sanupcraft.guide_book",
				"block.sanupcraft.boss_record_board.tooltip", "block.sanupcraft.goal_board.tooltip", "item.sanupcraft.guide_book.tooltip",
				"commands.sanupcraft.team.help", "commands.sanupcraft.team.locked", "commands.sanupcraft.team.name_taken", "commands.sanupcraft.team.owner_only",
				"commands.sanupcraft.team.already_in", "commands.sanupcraft.team.not_in", "commands.sanupcraft.team.not_found", "commands.sanupcraft.team.bad_name",
				"commands.sanupcraft.team.no_member", "commands.sanupcraft.team.kick_self", "commands.sanupcraft.goal.not_looking",
				"advancements.sanupcraft.goal_done.title", "advancements.sanupcraft.goal_done.description",
				"advancements.sanupcraft.guide_book.title", "advancements.sanupcraft.guide_book.description"}) {
			helper.assertTrue(ko.has(key) && en.has(key), "문구 키가 두 언어에 없음: " + key);
		}
		helper.succeed();
	}

	@GameTest
	public void playtestServerKitGivesBoardsAndBook(GameTestHelper helper) {
		ServerPlayer player = player(helper, "pk" + (System.nanoTime() % 100000));
		PlaytestCommand.apply(player, PlaytestCommand.Stage.SERVER);
		helper.assertTrue(player.getInventory().countItem(ModServer.BOSS_RECORD_BOARD.asItem()) > 0, "보스 기록판을 주지 않음");
		helper.assertTrue(player.getInventory().countItem(ModServer.GOAL_BOARD.asItem()) > 0, "공용 목표판을 주지 않음");
		helper.assertTrue(player.getInventory().countItem(ModServer.GUIDE_BOOK) > 0, "설명서를 주지 않음");
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 이름이 따로 있는 모의 플레이어 (기본 모의 플레이어는 이름이 모두 같아서 스코어보드 팀 검사가 안 된다). 연결이 없어서 메시지는 버린다. */
	private static ServerPlayer player(GameTestHelper helper, String name) {
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name),
				ClientInformation.createDefault()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}

			@Override
			public boolean isClientAuthoritative() {
				return false;
			}

			@Override
			public void sendSystemMessage(Component message, boolean overlay) {
			}
		};
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		return player;
	}

	/** 시험이 서버 전체 저장 데이터에 남긴 팀을 치운다. */
	private static void cleanup(MinecraftServer server, ServerPlayer... players) {
		List<TeamData.Team> mine = new ArrayList<>();
		for (ServerPlayer player : players) {
			TeamData.Team team = Teams.teamOf(server, player.getUUID());
			if (team != null && !mine.contains(team)) {
				mine.add(team);
			}
		}
		mine.forEach(team -> Teams.disband(server, team));
	}

	private static int argumentCount(String text) {
		Matcher matcher = PLACEHOLDER.matcher(text);
		int sequential = 0;
		int highest = 0;
		while (matcher.find()) {
			if (matcher.group(1) == null) {
				sequential++;
				highest = Math.max(highest, sequential);
			} else {
				highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
			}
		}
		return highest;
	}

	private static JsonObject readLang(String file) {
		String path = "/assets/sanupcraft/lang/" + file;
		try (InputStream in = SanupcraftM10GameTest.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("언어 파일이 없음: " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException("언어 파일을 읽지 못함: " + path, e);
		}
	}
}
