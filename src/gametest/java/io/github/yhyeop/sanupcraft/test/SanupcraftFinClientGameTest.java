package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/**
 * 마지막 묶음 화면 확인 (판정 없이 스크린샷): 공허 거상 전용 모델 · 텍스처 · 빛나는 레이어 · 보호막 레이어와 동작.
 * NoAI 거상을 세우고 앞 · 비스듬히 · 뒤, 끌어당기기 예고 · 끌어당기는 중, 수정 보호막, 내려치기, 밤을 HUD 없이 찍는다.
 * 거상 효과음 넷도 한 번씩 튼다 (로그에 없는 소리 경고가 없어야 함).
 * build/run/clientGameTest/screenshots/sanupcraft-fin-*.png
 */
public class SanupcraftFinClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("fill -10 -61 -10 10 -61 10 minecraft:end_stone_bricks");
			server.runCommand("summon sanupcraft:void_colossus 0.5 -60 0.5 {NoAI:1b,Rotation:[0f,0f],Tags:[\"fin\"]}");
			server.runCommand("gamemode spectator @a");

			server.runCommand("tp @a 0.5 -57.3 8.5 180 -2");
			shot(context, singleplayer, "front");
			server.runCommand("tp @a 6.0 -56.8 6.0 135 3");
			shot(context, singleplayer, "angle");
			server.runCommand("tp @a -7.0 -57.3 3.0 -112 -2");
			shot(context, singleplayer, "side");
			server.runCommand("tp @a 0.5 -57.3 -7.5 0 -2");
			shot(context, singleplayer, "back");
			server.runCommand("tp @a 0.5 -56.0 4.5 180 8");
			shot(context, singleplayer, "close");

			// 끌어당기기: 예고 자세 → 실제로 끌어당기는 자세
			server.runCommand("tp @a 0.5 -57.3 8.5 180 -2");
			onColossus(server, (level, colossus) -> {
				colossus.startPattern(level, VoidColossus.Pattern.PULL);
				colossus.updateFlags(level);
			});
			shot(context, singleplayer, "pull-windup");
			onColossus(server, (level, colossus) -> {
				for (int i = 0; i < 22; i++) {
					colossus.tickPattern(level);
				}
				colossus.updateFlags(level);
			});
			shot(context, singleplayer, "pulling");
			onColossus(server, (level, colossus) -> {
				colossus.startPattern(level, VoidColossus.Pattern.NONE);
				colossus.updateFlags(level);
			});

			// 수정 보호막
			onColossus(server, (level, colossus) -> {
				colossus.spawnCrystals(level);
				colossus.updateFlags(level);
			});
			context.waitTicks(20);
			shot(context, singleplayer, "shielded");
			server.runCommand("tp @a 9.0 -55.5 9.0 135 8");
			shot(context, singleplayer, "shielded-angle");
			server.runCommand("kill @e[type=sanupcraft:void_crystal]");
			onColossus(server, (level, colossus) -> colossus.updateFlags(level));
			context.waitTicks(30);

			// 내려치기: 팔을 머리 위로 든 순간
			server.runCommand("tp @a 0.5 -57.3 8.5 180 -2");
			context.waitTicks(5);
			onColossus(server, (level, colossus) -> level.broadcastEntityEvent(colossus, VoidColossus.EVENT_ATTACK));
			context.waitTicks(3);
			hideHud(context);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-fin-attack").withSize(1280, 720).disableCounterPrefix());

			// 밤: 핵 · 눈 · 금이 어둠 속에서 보이는지
			server.runCommand("time set 18000");
			shot(context, singleplayer, "night");

			// 거상 소리 넷을 한 번씩 (클라이언트 소리 관리자가 아는지는 로그로 본다)
			context.runOnClient(client -> {
				for (var sound : new net.minecraft.sounds.SoundEvent[] {ModSounds.COLOSSUS_PULL, ModSounds.COLOSSUS_CRYSTALS, ModSounds.COLOSSUS_RAGE, ModSounds.COLOSSUS_DEATH}) {
					if (client.getSoundManager().getSoundEvent(sound.location()) == null) {
						throw new AssertionError("클라이언트가 모르는 소리: " + sound.location());
					}
					client.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 0.5F));
				}
			});
			context.waitTicks(20);
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
		}
	}

	private interface ColossusAction {
		void run(ServerLevel level, VoidColossus colossus);
	}

	private static void onColossus(TestServerContext server, ColossusAction action) {
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (VoidColossus colossus : level.getEntitiesOfClass(VoidColossus.class, new AABB(-12, -64, -12, 12, -40, 12))) {
				action.run(level, colossus);
			}
		});
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name) {
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		hideHud(context);
		context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-fin-" + name).withSize(1280, 720).disableCounterPrefix());
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
