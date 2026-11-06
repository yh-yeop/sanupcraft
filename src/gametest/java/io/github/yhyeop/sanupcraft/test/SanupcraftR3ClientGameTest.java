package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * 확장 3 R2 · R3 클라이언트 확인.
 * R2: 다시 그린 아이콘을 인벤토리 두 화면으로 찍는다 (r3-icons-1 · 2).
 * R3: 마네킹 여섯이 손에 든 3D 도구를 앞 · 옆 · 뒤에서 (r3-held-*), 왼손 둘 (r3-offhand), 플레이어 1인칭 (r3-fp-*, 비교용 바닐라 철 검 · 쇠뇌),
 * 드릴로 흑요석을 캐는 동안 (날 회전 모델) 1인칭 · 3인칭 뒤에서 찍는다.
 * 스크린샷: build/run/clientGameTest/screenshots/r3-*.png
 */
public class SanupcraftR3ClientGameTest implements FabricClientGameTest {
	private static final String[] HELD = {"steam_drill", "chainsaw", "rivet_gun", "flamethrower", "steam_hammer", "wrench"};
	private static final String[] ICONS_1 = {
			"bronze_helmet", "bronze_chestplate", "bronze_leggings", "bronze_boots", "bronze_sword", "bronze_pickaxe", "bronze_axe", "bronze_shovel", "steel_spear",
			"steel_helmet", "steel_chestplate", "steel_leggings", "steel_boots", "steel_sword", "steel_hammer", "steel_excavator", "steam_hammer", "wrench",
			"chrome_steel_helmet", "chrome_steel_chestplate", "chrome_steel_leggings", "chrome_steel_boots", "chrome_steel_sword", "chrome_steel_hammer",
			"chrome_steel_excavator", "rusty_hammer", "furnace_shield",
			"heatproof_helmet", "heatproof_chestplate", "heatproof_leggings", "heatproof_boots", "heatproof_sword", "heatproof_drill", "void_staff", "void_sword",
			"void_helmet"};
	private static final String[] ICONS_2 = {
			"void_chestplate", "void_leggings", "void_boots", "steam_boots", "steam_jetpack", "rivet_gun", "steam_drill", "chainsaw", "flamethrower",
			"steam_canister", "vein_prospector", "debris_sonar", "steam_syringe"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			server.runCommand("fill -8 -61 -12 8 -61 4 minecraft:polished_andesite");

			// R2 아이콘
			icons(context, server, ICONS_1, "r3-icons-1");
			icons(context, server, ICONS_2, "r3-icons-2");
			server.runCommand("clear @a");

			// R3 3인칭: 마네킹 여섯 (남쪽을 봄), 오른손에 하나씩 + 왼손 둘
			for (int i = 0; i < HELD.length; i++) {
				String tag = "r3m" + i;
				double x = -5.0 + i * 2.0 + 0.5;
				server.runCommand("summon mannequin " + x + " -60 -5.5 {Rotation:[0f,0f],immovable:1b,hide_description:1b,Tags:[\"" + tag + "\"]}");
				server.runCommand("item replace entity @e[tag=" + tag + "] weapon.mainhand with sanupcraft:" + HELD[i]);
			}
			for (int i = 0; i < 2; i++) {
				String tag = "r3o" + i;
				double x = -1.0 + i * 2.0 + 0.5;
				server.runCommand("summon mannequin " + x + " -60 -9.5 {Rotation:[0f,0f],immovable:1b,hide_description:1b,Tags:[\"" + tag + "\"]}");
				server.runCommand("item replace entity @e[tag=" + tag + "] weapon.offhand with sanupcraft:" + (i == 0 ? "steam_drill" : "steam_hammer"));
			}
			server.runCommand("gamemode spectator @a");
			hud(context, false);
			shot(context, singleplayer, server, "tp @a 0.5 -58.6 1.0 180 14", "r3-held-front");
			shot(context, singleplayer, server, "tp @a 7.5 -59.2 -5.0 90 8", "r3-held-side");
			shot(context, singleplayer, server, "tp @a -6.5 -59.2 -5.0 -90 8", "r3-held-side2");
			shot(context, singleplayer, server, "tp @a 0.5 -58.6 -12.0 0 14", "r3-held-back");
			for (int i = 0; i < HELD.length; i++) {
				double x = -5.0 + i * 2.0 + 0.5;
				shot(context, singleplayer, server, "tp @a " + (x + 1.3) + " -59.3 -3.9 145 12", "r3-held-" + HELD[i]);
			}
			shot(context, singleplayer, server, "tp @a 0.5 -59.0 -6.8 180 12", "r3-offhand");
			hud(context, true);

			// R3 1인칭: 손에 하나씩
			server.runCommand("kill @e[type=mannequin]");
			server.runCommand("gamemode creative @a");
			server.runCommand("tp @a 0.5 -60 0.5 180 10");
			for (int i = 0; i < HELD.length; i++) {
				server.runCommand("item replace entity @a hotbar." + i + " with sanupcraft:" + HELD[i]);
			}
			server.runCommand("item replace entity @a weapon.offhand with sanupcraft:wrench");
			for (int i = 0; i < HELD.length; i++) {
				final int slot = i;
				context.runOnClient(client -> client.player.getInventory().setSelectedSlot(slot));
				context.waitTicks(40);
				clean(context);
				context.takeScreenshot("r3-fp-" + HELD[i]);
			}
			// 비교용 바닐라: 철 검 · 쇠뇌
			server.runCommand("item replace entity @a hotbar.6 with minecraft:iron_sword");
			server.runCommand("item replace entity @a hotbar.7 with minecraft:crossbow");
			for (int i = 6; i < 8; i++) {
				final int slot = i;
				context.runOnClient(client -> client.player.getInventory().setSelectedSlot(slot));
				context.waitTicks(40);
				clean(context);
				context.takeScreenshot("r3-fp-vanilla-" + i);
			}

			// 드릴 날 회전: 생존 모드로 앞의 흑요석을 캐는 동안 (허공을 치면 느린 도구는 손이 내려간다)
			context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
			server.runCommand("gamemode survival @a");
			server.runCommand("fill -1 -60 -2 1 -58 -2 minecraft:obsidian");
			server.runCommand("tp @a 0.5 -60 0.5 180 20");
			context.waitTicks(40);
			context.getInput().holdKey(options -> options.keyAttack);
			context.waitTicks(10);
			context.takeScreenshot("r3-fp-drill-spin");
			context.waitTicks(1);
			context.takeScreenshot("r3-fp-drill-spin2");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			for (int i = 0; i < 3; i++) {
				context.waitTicks(3);
				context.takeScreenshot("r3-tp-drill-spin" + i);
			}
			context.getInput().releaseKey(options -> options.keyAttack);
			context.waitTicks(20);
			context.takeScreenshot("r3-tp-back");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(2);
		}
	}

	private static void icons(ClientGameTestContext context, TestServerContext server, String[] items, String name) {
		server.runCommand("clear @a");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a 0.5 -60 1.5 180 0");
		for (int i = 0; i < items.length; i++) {
			String slot = i < 9 ? "hotbar." + i : "inventory." + (i - 9);
			if (i >= 36) {
				break;
			}
			server.runCommand("item replace entity @a " + slot + " with sanupcraft:" + items[i]);
		}
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
		context.waitTicks(5);
		clean(context);
		context.takeScreenshot(name);
		context.setScreen(() -> null);
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server, String tp, String name) {
		server.runCommand(tp);
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		clean(context);
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1280, 720));
	}

	private static void clean(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(false);
		});
	}

	/** 화면 글자(HUD) 켜고 끄기. */
	private static void hud(ClientGameTestContext context, boolean shown) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == shown) {
				client.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
	}
}
