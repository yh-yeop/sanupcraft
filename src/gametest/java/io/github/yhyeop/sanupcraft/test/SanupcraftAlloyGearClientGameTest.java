package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * 확장 3 G1 · G2 · R1 클라이언트 확인: 마네킹 다섯에 청동 · 강철 · 크롬강 · 내열 · 공허 한 벌을 입혀 줄 전체 · 비스듬히 · 한 벌씩 앞뒤로 찍고,
 * 새 아이템 아이콘을 인벤토리 화면으로 찍는다.
 * 스크린샷: build/run/clientGameTest/screenshots/alloy-*.png
 */
public class SanupcraftAlloyGearClientGameTest implements FabricClientGameTest {
	private static final String[] SETS = {"bronze", "steel", "chrome_steel", "heatproof", "void"};
	private static final String[] WEAPONS = {"bronze_sword", "steel_hammer", "chrome_steel_sword", "heatproof_sword", "void_sword"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			server.runCommand("gamemode creative @a");
			server.runCommand("fill -8 -61 -12 8 -61 4 minecraft:polished_andesite");

			// 마네킹 다섯 줄 (남쪽을 봄)
			for (int i = 0; i < SETS.length; i++) {
				String tag = "alloy" + i;
				double x = -4.0 + i * 2.0 + 0.5;
				server.runCommand("summon mannequin " + x + " -60 -5.5 {Rotation:[0f,0f],immovable:1b,hide_description:1b,Tags:[\"" + tag + "\"]}");
				for (String[] part : new String[][] {{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
					server.runCommand("item replace entity @e[tag=" + tag + "] armor." + part[0] + " with sanupcraft:" + SETS[i] + "_" + part[1]);
				}
				server.runCommand("item replace entity @e[tag=" + tag + "] weapon.mainhand with sanupcraft:" + WEAPONS[i]);
			}
			server.runCommand("gamemode spectator @a");
			hud(context, false);
			shot(context, singleplayer, server, "tp @a 0.5 -58.4 1.5 180 12", "alloy-front");
			shot(context, singleplayer, server, "tp @a 5.5 -58.6 -0.5 140 14", "alloy-angle");
			// 한 벌씩 가까이: 앞 · 뒤
			for (int i = 0; i < SETS.length; i++) {
				double x = -4.0 + i * 2.0 + 0.5;
				shot(context, singleplayer, server, "tp @a " + x + " -59.6 -2.9 180 22", "alloy-" + SETS[i] + "-front");
				shot(context, singleplayer, server, "tp @a " + x + " -59.6 -8.1 0 22", "alloy-" + SETS[i] + "-back");
			}
			hud(context, true);

			// 아이콘: 새 아이템 열일곱 개
			server.runCommand("gamemode survival @a");
			server.runCommand("tp @a 0.5 -60 1.5 180 0");
			String[] items = {"bronze_ingot", "bronze_helmet", "bronze_chestplate", "bronze_leggings", "bronze_boots", "bronze_sword",
					"bronze_pickaxe", "bronze_axe", "bronze_shovel",
					"chrome_steel_ingot", "chrome_steel_helmet", "chrome_steel_chestplate", "chrome_steel_leggings", "chrome_steel_boots",
					"chrome_steel_sword", "chrome_steel_hammer", "chrome_steel_excavator"};
			for (int i = 0; i < items.length; i++) {
				String slot = i < 9 ? "hotbar." + i : "inventory." + (i - 9);
				server.runCommand("item replace entity @a " + slot + " with sanupcraft:" + items[i]);
			}
			server.runCommand("item replace entity @a armor.head with sanupcraft:chrome_steel_helmet");
			server.runCommand("item replace entity @a armor.chest with sanupcraft:chrome_steel_chestplate");
			server.runCommand("item replace entity @a armor.legs with sanupcraft:bronze_leggings");
			server.runCommand("item replace entity @a armor.feet with sanupcraft:bronze_boots");
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(5);
			context.takeScreenshot("alloy-items");
			context.setScreen(() -> null);
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server, String tp, String name) {
		server.runCommand(tp);
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(false);
		});
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1280, 720));
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
