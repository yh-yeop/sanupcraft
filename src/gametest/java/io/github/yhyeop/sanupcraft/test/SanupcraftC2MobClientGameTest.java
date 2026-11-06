package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.mob.EliteMonster;

/**
 * 확장 3 C2 · R7 화면 확인 (판정 없이 스크린샷): 새 몹 셋의 전용 모델 · 텍스처 · 빛나는 레이어, 정예 (테두리 · 크기 · 이름).
 * 자동인형 · 정령 · 그림자를 보통 · 정예 둘씩 NoAI로 세우고 앞 · 가까이 · 비스듬히 · 뒤 · 공격 자세 · 밤을 HUD 없이 찍는다.
 * build/run/clientGameTest/screenshots/sanupcraft-c2-*.png
 */
public class SanupcraftC2MobClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("fill -10 -61 -8 10 -61 6 minecraft:stone_bricks");
			summon(server, "rust_automaton", -6.5, -60.0, "");
			summon(server, "rust_automaton", -3.5, -60.0, "speed");
			summon(server, "ember_wisp", -0.5, -59.2, "");
			summon(server, "ember_wisp", 2.0, -59.2, "regen");
			summon(server, "void_shade", 5.0, -60.0, "");
			summon(server, "void_shade", 7.5, -60.0, "reflect");
			server.runCommand("gamemode spectator @a");

			server.runCommand("tp @a 0.5 -57.5 9.5 180 10");
			shot(context, singleplayer, "front");
			server.runCommand("tp @a -5.0 -58.0 3.6 180 8");
			shot(context, singleplayer, "automaton");
			server.runCommand("tp @a 0.75 -58.6 2.8 180 6");
			shot(context, singleplayer, "wisp");
			server.runCommand("tp @a 6.25 -57.6 4.3 180 4");
			shot(context, singleplayer, "shade");
			server.runCommand("tp @a -9.5 -57.8 4.0 225 10");
			shot(context, singleplayer, "side");
			server.runCommand("tp @a 0.5 -57.5 -6.5 0 10");
			shot(context, singleplayer, "back");

			// 공격 자세: 노리는 중 (팔을 앞으로) + 휘두르기
			server.runOnServer(s -> {
				for (Mob mob : s.overworld().getEntitiesOfClass(Mob.class, new AABB(-12, -62, -10, 12, -50, 8), e -> e instanceof EliteMonster)) {
					mob.setAggressive(true);
				}
			});
			server.runCommand("tp @a 0.5 -57.5 9.5 180 10");
			context.waitTicks(20);
			server.runOnServer(s -> {
				for (Mob mob : s.overworld().getEntitiesOfClass(Mob.class, new AABB(-12, -62, -10, 12, -50, 8), e -> e instanceof EliteMonster)) {
					mob.swingForAttack(InteractionHand.MAIN_HAND);
				}
			});
			context.waitTicks(2);
			hideHud(context);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-c2-attack").withSize(1280, 720).disableCounterPrefix());
			server.runCommand("tp @a -5.0 -58.0 3.6 180 8");
			shot(context, singleplayer, "automaton-aggressive");

			// 밤: 빛나는 부분 (정예는 더 밝고 테두리가 빛난다)
			server.runCommand("time set 18000");
			server.runCommand("tp @a 0.5 -57.5 9.5 180 10");
			shot(context, singleplayer, "night");
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
		}
	}

	private static void summon(TestServerContext server, String name, double x, double y, String elite) {
		String nbt = "{NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Rotation:[0f,0f]" + (elite.isEmpty() ? "" : ",SanupcraftElite:\"" + elite + "\"") + "}";
		server.runCommand("summon sanupcraft:" + name + " " + x + " " + y + " 0.5 " + nbt);
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name) {
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		hideHud(context);
		context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-c2-" + name).withSize(1280, 720).disableCounterPrefix());
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
