package io.github.yhyeop.sanupcraft.test;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.client.MachineScreen;
import io.github.yhyeop.sanupcraft.world.ModWorld;

/**
 * 소개 페이지용 스크린샷. 환경 변수 SANUPCRAFT_PROMO=1 일 때만 돈다 (평소 runClientGameTest에는 안 낀다).
 * HUD · 채팅 · 알림 없이 한국어 화면으로 1600x900에 찍는다: build/run/clientGameTest/screenshots/promo-*.png
 */
public class SanupcraftPromoShots implements FabricClientGameTest {
	private static final int W = 1600;
	private static final int H = 900;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!"1".equals(System.getenv("SANUPCRAFT_PROMO"))) {
			return;
		}
		CompletableFuture<Void> reload = context.computeOnClient(client -> {
			client.options.languageCode = "ko_kr";
			client.getLanguageManager().setSelected("ko_kr");
			client.options.gamma().set(1.0);
			client.options.bobView().set(false);
			return client.reloadResourcePacks();
		});
		context.waitFor(client -> reload.isDone(), 2400);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("time set 1000");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			server.runCommand("gamerule advance_weather false");

			factory(context, server, singleplayer);
			bosses(context, server, singleplayer);
			gear(context, server, singleplayer);
			screens(context, server);
			nether(context, server, singleplayer);
			end(context, server, singleplayer);
		}
		shrine(context);
	}

	/** 일반 지형 월드에서 가장 가까운 녹슨 사당을 찾아 찍는다 (월드 생성에 실제로 나오는지도 이걸로 본다). */
	private static void shrine(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("sanupcraft")).create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			BlockPos found = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				Holder<Structure> shrine = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ModWorld.RUSTED_SHRINE);
				var pair = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(shrine), BlockPos.ZERO, 100, false);
				return pair == null ? null : pair.getFirst();
			});
			if (found == null) {
				throw new AssertionError("녹슨 사당을 찾지 못함");
			}
			Sanupcraft.LOGGER.info("녹슨 사당: {}", found);
			server.runCommand("gamemode spectator @a");
			server.runCommand("tp @a " + found.getX() + " 140 " + found.getZ());
			context.waitTicks(100);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos altar = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (BlockPos p : BlockPos.betweenClosed(found.offset(-20, 0, -20).atY(level.getMinY()), found.offset(20, 0, 20).atY(200))) {
					if (level.getBlockState(p).is(ModBoss.GUARDIAN_ALTAR)) {
						return p.immutable();
					}
				}
				return null;
			});
			if (altar == null) {
				throw new AssertionError("사당 근처에 제단이 없음: " + found);
			}
			Sanupcraft.LOGGER.info("녹슨 사당 제단: {}", altar);
			// 카메라와 사당 사이의 나뭇잎을 걷어 낸다 (숲에 생기면 잎에 가린다)
			int ax = altar.getX();
			int ay = altar.getY();
			int az = altar.getZ();
			server.runCommand("fill " + (ax - 7) + " " + (ay - 1) + " " + (az - 6) + " " + (ax + 7) + " " + (ay + 12) + " " + (az + 12) + " air replace #minecraft:leaves");
			server.runCommand("tp @a " + (ax + 0.5) + " " + (ay + 6) + " " + (az + 10.5) + " 180 28");
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			shot(context, "shrine");
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(false);
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of("promo-" + name).withSize(W, H).disableCounterPrefix());
	}

	/** 화면(GUI)은 배율 2 · 창 크기 그대로 찍는다 (tools/promo_crop.py가 가운데를 잘라 2배로 키운다). */
	private static void guiShot(ClientGameTestContext context, String name) {
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeGui();
			client.gui.toastManager().clear();
		});
		context.waitTicks(3);
		context.takeScreenshot(TestScreenshotOptions.of("promo-gui-" + name).disableCounterPrefix());
		context.runOnClient(client -> {
			client.options.guiScale().set(0);
			client.resizeGui();
		});
	}

	private static void mannequin(TestServerContext server, String tag, double x, double z, float yaw, String set, String weapon) {
		server.runCommand("summon mannequin " + x + " -60 " + z + " {Rotation:[" + yaw + "f,0f],immovable:1b,hide_description:1b,Tags:[\"" + tag + "\"]}");
		for (String[] part : new String[][] {{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
			server.runCommand("item replace entity @e[tag=" + tag + "] armor." + part[0] + " with sanupcraft:" + set + "_" + part[1]);
		}
		server.runCommand("item replace entity @e[tag=" + tag + "] weapon.mainhand with sanupcraft:" + weapon);
	}

	/** 오버월드 증기 공장: 보일러 둘 → 파이프 → 분쇄기 · 합금로 · 압착기 · 조립대, 앞쪽 벨트, 뒤쪽 굴착기가 판 구덩이와 녹슨 수호자. */
	private static void factory(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("gamemode creative @a");
		server.runCommand("fill -9 -61 -9 9 -61 3 minecraft:smooth_stone");
		server.runCommand("fill -9 -61 -6 9 -61 -5 minecraft:polished_andesite");
		// 보일러 둘 (양 끝) + 파이프 줄 (z=-8), 기계는 그 앞 (z=-7)
		for (int x : new int[] {-7, 7}) {
			server.runCommand("setblock " + x + " -60 -8 sanupcraft:boiler[facing=south]");
			server.runCommand("data merge block " + x + " -60 -8 {water:8000}");
			server.runCommand("item replace block " + x + " -60 -8 container.0 with minecraft:coal 64");
		}
		server.runCommand("fill -6 -60 -8 6 -60 -8 sanupcraft:steam_pipe");
		String[][] machines = {
				{"-5", "crusher", "container.0 with minecraft:raw_iron 32"},
				{"-3", "alloy_smelter", "container.0 with sanupcraft:iron_dust 32"},
				{"-1", "press", "container.0 with sanupcraft:steel_ingot 32"},
				{"1", "assembler", "container.0 with sanupcraft:steel_plate 32"},
				{"3", "crusher", "container.0 with minecraft:raw_copper 32"},
		};
		for (String[] m : machines) {
			server.runCommand("setblock " + m[0] + " -60 -7 sanupcraft:" + m[1] + "[facing=south]");
			server.runCommand("item replace block " + m[0] + " -60 -7 " + m[2]);
		}
		server.runCommand("item replace block -3 -60 -7 container.4 with sanupcraft:coal_dust 32");
		server.runCommand("setblock 5 -60 -7 minecraft:chest[facing=south]");
		server.runCommand("setblock -7 -60 -7 minecraft:chest[facing=south]");
		// 앞쪽 벨트: 모터(오른쪽 파이프 기둥 옆) → 왼쪽으로 → 상자
		server.runCommand("fill 8 -60 -8 8 -60 -4 sanupcraft:steam_pipe");
		server.runCommand("setblock 7 -60 -4 sanupcraft:belt_motor[facing=south]");
		server.runCommand("fill 6 -60 -4 -6 -60 -4 sanupcraft:conveyor_belt[facing=west]");
		server.runCommand("setblock -7 -60 -4 minecraft:chest[facing=east]");
		// 뒤쪽 굴착기: 아래 9x9 (z -18..-10)를 판다. 증기는 직접 채운다
		server.runCommand("setblock 0 -60 -14 sanupcraft:steam_quarry");
		server.runCommand("setblock 0 -59 -14 minecraft:chest");
		server.runCommand("summon sanupcraft:rusty_guardian -8.5 -60 -14.5 {NoAI:1b,Rotation:[220f,0f]}");
		mannequin(server, "w1", -3.5, -1.5, 160, "steel", "steel_hammer");
		mannequin(server, "w2", 4.5, -0.5, 200, "steel", "steel_sword");
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 0.5 -55.5 4.5 180 27");
		for (int i = 0; i < 60; i++) {
			if (i % 2 == 0) {
				server.runCommand("summon item 5.5 -59.7 -3.5 {Item:{id:\"minecraft:raw_iron\",count:1},PickupDelay:32767}");
			}
			if (i % 4 == 0) {
				server.runCommand("data merge block 0 -60 -14 {steam:1000}");
			}
			context.waitTicks(8);
		}
		singleplayer.getConnection().waitForChunksRender();
		shot(context, "factory");
		server.runCommand("tp @a -4.5 -57.6 -2.0 205 26");
		context.waitTicks(10);
		shot(context, "factory-close");
	}

	/** 보스 두 마리: 녹슨 수호자 앞의 강철 모험대, 용광로 파수꾼. */
	private static void bosses(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("fill -40 -61 -40 -20 -61 -20 minecraft:stone_bricks");
		server.runCommand("setblock -30 -60 -36 sanupcraft:guardian_altar");
		server.runCommand("summon sanupcraft:rusty_guardian -29.5 -60 -32.5 {NoAI:1b,Rotation:[0f,0f]}");
		mannequin(server, "p1", -31.5, -27.5, 170, "steel", "steel_sword");
		mannequin(server, "p2", -27.5, -28.5, 200, "steel", "steel_hammer");
		server.runCommand("tp @a -29.0 -58.6 -23.0 180 4");
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		shot(context, "guardian");
	}

	/** 장비 줄: 강철 한 벌 · 내열 한 벌. */
	private static void gear(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("fill 30 -61 -10 40 -61 0 minecraft:polished_deepslate");
		mannequin(server, "g1", 33.5, -5.5, 160, "steel", "steel_sword");
		mannequin(server, "g2", 35.5, -6.0, 180, "steel", "steel_hammer");
		mannequin(server, "g3", 37.5, -5.5, 200, "heatproof", "heatproof_sword");
		server.runCommand("tp @a 35.5 -58.7 -1.6 180 9");
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		shot(context, "gear");
	}

	/** 화면: 합금로 (한국어 UI), 길잡이 발전 과제. */
	private static void screens(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("gamemode creative @a");
		server.runCommand("tp @a -2.5 -60 -5.5 180 30");
		context.waitTicks(10);
		context.getInput().lookAt(new BlockPos(-3, -60, -7));
		context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(MachineScreen.class);
		context.waitTicks(10);
		guiShot(context, "alloy-screen");
		context.setScreen(() -> null);

		server.runCommand("advancement grant @a until sanupcraft:ender_anchor");
		context.waitTicks(10);
		context.setScreen(() -> new AdvancementsScreen(context.computeOnClient(client -> client.getConnection().getAdvancements())));
		context.waitTicks(10);
		guiShot(context, "guide");
		context.setScreen(() -> null);
	}

	/** 네더: 용광로 파수꾼, 그리고 지열 분출구 전초기지. */
	private static void nether(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("gamemode survival @a");
		server.runCommand("execute as @p run sanupcraft playtest nether");
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		server.runCommand("gamemode creative @a");
		server.runCommand("execute at @p run fill ~-6 ~-1 ~2 ~6 ~-1 ~14 minecraft:polished_blackstone_bricks");
		server.runCommand("execute at @p run fill ~-6 ~ ~2 ~6 ~6 ~14 minecraft:air");
		server.runCommand("execute at @p run setblock ~-5 ~-1 ~10 minecraft:lava");
		server.runCommand("execute at @p run setblock ~5 ~-1 ~10 minecraft:lava");
		server.runCommand("execute at @p run setblock ~-4 ~ ~13 minecraft:glowstone");
		server.runCommand("execute at @p run setblock ~4 ~ ~13 minecraft:glowstone");
		server.runCommand("execute at @p run setblock ~ ~ ~12 sanupcraft:guardian_altar");
		server.runCommand("execute at @p run summon sanupcraft:furnace_warden ~0.5 ~ ~9 {NoAI:1b,Rotation:[180f,0f]}");
		server.runCommand("execute at @p run summon mannequin ~-1.5 ~ ~4 {Rotation:[-10f,0f],immovable:1b,hide_description:1b,Tags:[\"h1\"]}");
		server.runCommand("execute at @p run summon mannequin ~2.5 ~ ~5 {Rotation:[20f,0f],immovable:1b,hide_description:1b,Tags:[\"h2\"]}");
		for (String tag : new String[] {"h1", "h2"}) {
			for (String[] part : new String[][] {{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				server.runCommand("item replace entity @e[tag=" + tag + "] armor." + part[0] + " with sanupcraft:heatproof_" + part[1]);
			}
			server.runCommand("item replace entity @e[tag=" + tag + "] weapon.mainhand with sanupcraft:heatproof_sword");
		}
		server.runCommand("gamemode spectator @a");
		server.runCommand("execute as @p at @p run tp @s ~0.5 ~1.8 ~-1 0 9");
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		shot(context, "warden");

		// 전초기지: 바닥 아래 용암 원천 둘 → 분출구 → 파이프 → 굴착기 (드릴) · 심장 제련로, 엔더 송신기 · 닻
		server.runCommand("kill @e[type=sanupcraft:furnace_warden]");
		server.runCommand("kill @e[type=mannequin]");
		server.runCommand("execute at @p run fill ~-6 ~-1.8 ~2 ~6 ~4 ~14 minecraft:air");
		server.runCommand("execute at @p run fill ~-6 ~-2.8 ~2 ~6 ~-2.8 ~14 minecraft:polished_blackstone_bricks");
		server.runCommand("execute at @p run fill ~-3 ~-3.8 ~5 ~-2 ~-3.8 ~5 minecraft:lava");
		server.runCommand("execute at @p run setblock ~-2 ~-2.8 ~5 sanupcraft:geothermal_vent");
		server.runCommand("execute at @p run setblock ~-3 ~-2.8 ~5 sanupcraft:geothermal_vent");
		server.runCommand("execute at @p run fill ~-1 ~-1.8 ~5 ~3 ~-1.8 ~5 sanupcraft:steam_pipe");
		server.runCommand("execute at @p run fill ~-2 ~-1.8 ~5 ~-3 ~-1.8 ~5 sanupcraft:steam_pipe");
		server.runCommand("execute at @p run setblock ~-1 ~-1.8 ~6 sanupcraft:heart_forge[facing=south]");
		server.runCommand("execute at @p run setblock ~4 ~-1.8 ~5 sanupcraft:steam_quarry");
		server.runCommand("execute at @p run setblock ~4 ~-0.8 ~5 sanupcraft:ender_sender");
		server.runCommand("execute at @p run setblock ~1 ~-1.8 ~8 sanupcraft:ender_anchor");
		server.runCommand("execute at @p run item replace block ~1 ~-1.8 ~8 container.0 with minecraft:popped_chorus_fruit 16");
		server.runCommand("execute at @p run data merge block ~4 ~-1.8 ~5 {drill:1b}");
		server.runCommand("execute as @p at @p run tp @s ~0.5 ~0.5 ~-1 15 30");
		context.waitTicks(200);
		singleplayer.getConnection().waitForChunksRender();
		shot(context, "outpost");
	}

	/** 엔드: 엔더 송신기 · 수신기와 닻. */
	private static void end(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("gamemode survival @a");
		server.runCommand("execute as @p run sanupcraft playtest end");
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		server.runCommand("execute at @p run setblock ~-1 ~ ~3 sanupcraft:ender_receiver");
		server.runCommand("execute at @p run setblock ~1 ~ ~3 sanupcraft:ender_anchor");
		server.runCommand("execute at @p run item replace block ~1 ~ ~3 container.0 with minecraft:popped_chorus_fruit 16");
		server.runCommand("gamemode spectator @a");
		server.runCommand("execute as @p at @p run tp @s ~ ~1.5 ~-1 10 25");
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		shot(context, "end");
	}
}
