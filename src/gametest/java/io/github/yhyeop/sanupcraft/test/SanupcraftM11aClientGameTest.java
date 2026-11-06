package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.ModBoss;

/**
 * M11 X1 화면 확인 (판정 없이 스크린샷): 보스 전용 모델 · 텍스처 · 빛나는 레이어.
 * 녹슨 수호자와 용광로 파수꾼을 NoAI로 세우고 앞 · 뒤 · 밤 · 공격 동작을 HUD 없이 찍는다.
 * build/run/clientGameTest/screenshots/sanupcraft-m11a-*.png
 */
public class SanupcraftM11aClientGameTest implements FabricClientGameTest {
	private static final byte EVENT_ATTACK = 4;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("fill -8 -61 -6 10 -61 6 minecraft:stone_bricks");
			server.runCommand("summon sanupcraft:rusty_guardian -2.5 -60 0.5 {NoAI:1b,Rotation:[0f,0f],Tags:[\"m11a\"]}");
			server.runCommand("summon sanupcraft:furnace_warden 4.5 -60 0.5 {NoAI:1b,Rotation:[0f,0f],Tags:[\"m11a\"]}");
			server.runCommand("gamemode spectator @a");

			server.runCommand("tp @a 1.0 -57.6 10.5 180 8");
			shot(context, singleplayer, "front");
			// 가까이: 앞 · 비스듬히 · 뒤 (면 빠짐 · 텍스처 자리 확인)
			server.runCommand("tp @a -2.5 -57.6 6.0 180 8");
			shot(context, singleplayer, "guardian-front");
			server.runCommand("tp @a -7.0 -57.6 4.5 229 10");
			shot(context, singleplayer, "guardian-side");
			server.runCommand("tp @a -2.5 -57.6 -4.5 0 8");
			shot(context, singleplayer, "guardian-back");
			server.runCommand("tp @a 4.5 -58.0 5.0 180 10");
			shot(context, singleplayer, "warden-front");
			server.runCommand("tp @a 9.0 -58.0 4.5 131 12");
			shot(context, singleplayer, "warden-side");
			server.runCommand("tp @a 4.5 -57.6 -4.0 0 14");
			shot(context, singleplayer, "warden-back");
			server.runCommand("tp @a 1.0 -52.5 4.5 180 50");
			shot(context, singleplayer, "top");

			// 공격 동작: 클라이언트에 휘두르기 이벤트를 보내고 팔을 든 순간에 찍는다
			server.runCommand("tp @a 1.0 -57.6 10.5 180 8");
			context.waitTicks(5);
			server.runOnServer(s -> {
				for (Entity e : s.overworld().getEntitiesOfClass(Entity.class, new AABB(-10, -62, -10, 12, -50, 10))) {
					if (e.getType() == ModBoss.RUSTY_GUARDIAN) {
						s.overworld().broadcastEntityEvent(e, EVENT_ATTACK);
					} else if (e.getType() == ModBoss.FURNACE_WARDEN) {
						s.overworld().broadcastEntityEvent(e, FurnaceWarden.EVENT_ROAR);
					}
				}
			});
			context.waitTicks(3);
			hideHud(context);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-m11a-attack").withSize(1280, 720).disableCounterPrefix());

			// 밤: 빛나는 부분이 어둠 속에서 보이는지
			server.runCommand("time set 18000");
			server.runCommand("tp @a 1.0 -57.6 9.5 180 8");
			shot(context, singleplayer, "night");
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name) {
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		hideHud(context);
		context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-m11a-" + name).withSize(1280, 720).disableCounterPrefix());
	}

	private static void hideHud(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(false);
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
	}
}
