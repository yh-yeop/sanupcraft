package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * 확장 3 G5 · G6 클라이언트 확인: 마네킹 셋(광부 한 벌 + 멀티툴 · 증기 파워 아머 · 잠수 헬멧)을 줄 · 한 벌씩 앞뒤로 찍고,
 * 멀티툴을 든 1인칭 · 3인칭, 새 아이콘(인벤토리), 막힌 굴에서 광부 헬멧 머리 조명을 찍는다.
 * 스크린샷: build/run/clientGameTest/screenshots/sanupcraft-g5-*.png
 */
public class SanupcraftSpecialGearClientGameTest implements FabricClientGameTest {
	private static final String[][] SETS = {
			{"miner_helmet", "miner_chestplate", "miner_leggings", "miner_boots", "multitool"},
			{"power_armor_helmet", "power_armor_chestplate", "power_armor_leggings", "power_armor_boots", "multitool"},
			{"diving_helmet", "", "", "", ""},
	};
	private static final String[] NAMES = {"miner", "power", "diving"};

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
			server.runCommand("fill -6 -61 -12 6 -61 4 minecraft:polished_andesite");

			String[] parts = {"head", "chest", "legs", "feet"};
			for (int i = 0; i < SETS.length; i++) {
				String tag = "g5set" + i;
				double x = -2.0 + i * 2.0 + 0.5;
				server.runCommand("summon mannequin " + x + " -60 -5.5 {Rotation:[0f,0f],immovable:1b,hide_description:1b,Tags:[\"" + tag + "\"]}");
				for (int p = 0; p < 4; p++) {
					if (!SETS[i][p].isEmpty()) {
						server.runCommand("item replace entity @e[tag=" + tag + "] armor." + parts[p] + " with sanupcraft:" + SETS[i][p]);
					}
				}
				if (!SETS[i][4].isEmpty()) {
					server.runCommand("item replace entity @e[tag=" + tag + "] weapon.mainhand with sanupcraft:" + SETS[i][4]);
				}
			}
			server.runCommand("gamemode spectator @a");
			hud(context, false);
			shot(context, singleplayer, server, "tp @a 0.5 -58.6 0.5 180 14", "row");
			shot(context, singleplayer, server, "tp @a 4.5 -58.8 -1.5 135 16", "angle");
			for (int i = 0; i < SETS.length; i++) {
				double x = -2.0 + i * 2.0 + 0.5;
				shot(context, singleplayer, server, "tp @a " + x + " -59.6 -2.9 180 22", NAMES[i] + "-front");
				shot(context, singleplayer, server, "tp @a " + x + " -59.6 -8.1 0 22", NAMES[i] + "-back");
			}
			shot(context, singleplayer, server, "tp @a -0.5 -59.2 -3.6 200 30", "multitool-mannequin");

			// 손에 든 멀티툴: 1인칭 · 3인칭
			server.runCommand("gamemode survival @a");
			server.runCommand("tp @a 0.5 -60 1.5 180 10");
			server.runCommand("clear @a");
			server.runCommand("item replace entity @a weapon.mainhand with sanupcraft:multitool");
			hud(context, true);
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			clearChat(context);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-g5-multitool-firstperson").withSize(1280, 720).disableCounterPrefix());
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			hud(context, false);
			context.waitTicks(20);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-g5-multitool-thirdperson").withSize(1280, 720).disableCounterPrefix());
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-g5-multitool-thirdperson-back").withSize(1280, 720).disableCounterPrefix());
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			hud(context, true);

			// 아이콘
			String[] items = {"diving_helmet", "miner_helmet", "miner_chestplate", "miner_leggings", "miner_boots", "power_armor_helmet",
					"power_armor_chestplate", "power_armor_leggings", "power_armor_boots", "multitool", "module_jetpack"};
			for (int i = 0; i < items.length; i++) {
				String slot = i < 9 ? "hotbar." + i : "inventory." + (i - 9);
				server.runCommand("item replace entity @a " + slot + " with sanupcraft:" + items[i]);
			}
			server.runCommand("item replace entity @a armor.head with sanupcraft:miner_helmet");
			server.runCommand("item replace entity @a armor.chest with sanupcraft:power_armor_chestplate");
			server.runCommand("item replace entity @a armor.legs with sanupcraft:miner_leggings");
			server.runCommand("item replace entity @a armor.feet with sanupcraft:power_armor_boots");
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-g5-items").withSize(1280, 720).disableCounterPrefix());
			context.setScreen(() -> null);

			// 머리 조명: 막힌 돌 굴 (밤) 안에서 광부 헬멧
			server.runCommand("time set midnight");
			server.runCommand("fill 20 -61 -4 28 -55 4 minecraft:stone");
			server.runCommand("fill 21 -60 -3 27 -57 3 minecraft:air");
			server.runCommand("tp @a 24.5 -60 2.5 180 10");
			server.runCommand("clear @a");
			server.runCommand("item replace entity @a armor.head with sanupcraft:miner_helmet");
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			hud(context, false);
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-g5-headlamp-on").withSize(1280, 720).disableCounterPrefix());
			server.runCommand("item replace entity @a armor.head with minecraft:air");
			context.waitTicks(20);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-g5-headlamp-off").withSize(1280, 720).disableCounterPrefix());
			hud(context, true);
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server, String tp, String name) {
		server.runCommand(tp);
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		clearChat(context);
		context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-g5-" + name).withSize(1280, 720).disableCounterPrefix());
	}

	private static void clearChat(ClientGameTestContext context) {
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
