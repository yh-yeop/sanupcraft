package io.github.yhyeop.sanupcraft.test;

import java.util.function.BiConsumer;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.PartyBossMonster;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/**
 * 확장 3 C4 · R7 화면 확인 (판정 없이 스크린샷): 세 보스의 1페이즈 · 2페이즈(금 간 갑옷 + 더 센 빛) 모습, 밤의 2페이즈,
 * 새 패턴 셋의 예고 · 위험 구역 입자, 보스 전용 장비 셋을 손에 든 모습 (1인칭 · 3인칭 · 방패 막기 · 인벤토리).
 * 새 소리 다섯을 한 번씩 튼다 (로그에 모르는 소리 경고가 없어야 함).
 * build/run/clientGameTest/screenshots/sanupcraft-c4-*.png
 */
public class SanupcraftC4ClientGameTest implements FabricClientGameTest {
	private static final int GUARDIAN_X = -14;
	private static final int WARDEN_X = 0;
	private static final int COLOSSUS_X = 14;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("fill -26 -61 -12 26 -61 12 minecraft:smooth_stone");
			server.runCommand("summon sanupcraft:rusty_guardian " + GUARDIAN_X + ".5 -60 0.5 {NoAI:1b,Rotation:[0f,0f]}");
			server.runCommand("summon sanupcraft:furnace_warden " + WARDEN_X + ".5 -60 0.5 {NoAI:1b,Rotation:[0f,0f]}");
			server.runCommand("summon sanupcraft:void_colossus " + COLOSSUS_X + ".5 -60 0.5 {NoAI:1b,Rotation:[0f,0f]}");
			server.runCommand("gamemode spectator @a");

			// 1페이즈
			frame(server, GUARDIAN_X, 6.0);
			shot(context, singleplayer, "guardian-p1");
			frame(server, WARDEN_X, 6.0);
			shot(context, singleplayer, "warden-p1");
			frame(server, COLOSSUS_X, 7.0);
			shot(context, singleplayer, "colossus-p1");

			// 2페이즈: 체력 40% → 금 간 갑옷 + 더 센 빛
			onBosses(server, (level, boss) -> {
				boss.setHealth(boss.getMaxHealth() * 0.4F);
				boss.checkPhaseTwo(level);
			});
			context.waitTicks(40); // 화남 입자가 가라앉게
			frame(server, GUARDIAN_X, 6.0);
			shot(context, singleplayer, "guardian-p2");
			server.runCommand("tp @a " + (GUARDIAN_X + 3.0) + " -57.0 3.5 135 10");
			shot(context, singleplayer, "guardian-p2-close");
			frame(server, WARDEN_X, 6.0);
			shot(context, singleplayer, "warden-p2");
			frame(server, COLOSSUS_X, 7.0);
			shot(context, singleplayer, "colossus-p2");
			server.runCommand("time set 18000");
			server.runCommand("tp @a 0.5 -55.0 16.0 180 12");
			shot(context, singleplayer, "night-p2");
			server.runCommand("time set 1000");

			// 새 패턴: 예고 고리 → 위험 구역 (구경꾼은 대상이 아니라 자리는 보스 둘레에 고른다)
			frame(server, GUARDIAN_X, 11.0);
			onBosses(server, (level, boss) -> {
				if (boss instanceof RustyGuardian guardian) {
					guardian.startPattern(level, RustyGuardian.Pattern.RUST_STORM);
					for (int i = 0; i < 12; i++) {
						guardian.tickPattern(level);
					}
				}
			});
			shotNow(context, "rust-storm-windup");
			onBosses(server, (level, boss) -> {
				if (boss instanceof RustyGuardian guardian) {
					for (int i = 0; i < RustyGuardian.RUST_STORM_WINDUP; i++) {
						guardian.tickPattern(level);
					}
				}
			});
			context.waitTicks(30);
			shotNow(context, "rust-puddles");

			frame(server, WARDEN_X, 11.0);
			onBosses(server, (level, boss) -> {
				if (boss instanceof FurnaceWarden warden) {
					warden.startPattern(level, FurnaceWarden.Pattern.LAVA_PILLARS);
					warden.setPillars(java.util.List.of(warden.position().add(-3.0, 0.0, 3.0), warden.position().add(3.0, 0.0, 3.0),
							warden.position().add(0.0, 0.0, 5.0)));
					for (int i = 0; i < 20; i++) {
						warden.tickPattern(level);
					}
				}
			});
			shotNow(context, "lava-pillar-windup");
			onBosses(server, (level, boss) -> {
				if (boss instanceof FurnaceWarden warden) {
					for (int i = 0; i < 6; i++) {
						warden.tickPattern(level);
					}
				}
			});
			context.waitTicks(3);
			shotNow(context, "lava-pillars");

			frame(server, COLOSSUS_X, 12.0);
			onBosses(server, (level, boss) -> {
				if (boss instanceof VoidColossus colossus) {
					colossus.startPattern(level, VoidColossus.Pattern.RIFT);
					colossus.setRiftSpots(java.util.List.of(colossus.position().add(-4.0, 0.0, 4.0), colossus.position().add(4.0, 0.0, 4.0),
							colossus.position().add(0.0, 0.0, 6.0)));
					for (int i = 0; i <= VoidColossus.RIFT_WINDUP; i++) {
						colossus.tickPattern(level);
					}
				}
			});
			context.waitTicks(30);
			shotNow(context, "void-rifts");
			server.runCommand("kill @e[type=sanupcraft:boss_hazard]");

			// 보스 전용 장비: 손에 든 모습
			server.runCommand("gamemode survival @a");
			server.runCommand("tp @a 0.5 -60 -8.0 180 0");
			server.runCommand("clear @a");
			server.runCommand("item replace entity @a weapon.mainhand with sanupcraft:rusty_hammer");
			server.runCommand("item replace entity @a weapon.offhand with sanupcraft:furnace_shield");
			server.runCommand("item replace entity @a hotbar.1 with sanupcraft:void_staff");
			server.runCommand("item replace entity @a hotbar.2 with sanupcraft:furnace_shield");
			shotWithHand(context, singleplayer, "gear-firstperson");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			shot(context, singleplayer, "gear-thirdperson");
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(10);
			shotNow(context, "gear-blocking-thirdperson");
			context.getInput().releaseKey(options -> options.keyUse);
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.getInput().holdKey(options -> options.keyUse);
			shotWithHand(context, singleplayer, "gear-blocking-firstperson");
			context.getInput().releaseKey(options -> options.keyUse);
			server.runCommand("item replace entity @a weapon.mainhand with sanupcraft:void_staff");
			server.runCommand("item replace entity @a weapon.offhand with minecraft:air");
			shotWithHand(context, singleplayer, "staff-firstperson");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			shot(context, singleplayer, "staff-thirdperson");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("item replace entity @a hotbar.0 with sanupcraft:rusty_hammer");
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-c4-gear-inventory").withSize(1280, 720).disableCounterPrefix());
			context.setScreen(() -> null);

			// 새 소리 다섯
			context.runOnClient(client -> {
				for (SoundEvent sound : new SoundEvent[] {ModSounds.GUARDIAN_RUST_STORM, ModSounds.WARDEN_LAVA_PILLAR, ModSounds.WARDEN_LAVA_ERUPT,
						ModSounds.COLOSSUS_RIFT, ModSounds.VOID_STAFF_SHOOT}) {
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

	/** 보스 {@code x} 앞(남쪽) {@code distance}칸에서 북쪽을 본다. */
	private static void frame(TestServerContext server, int x, double distance) {
		server.runCommand("tp @a " + (x + 0.5) + " -57.3 " + (0.5 + distance) + " 180 5");
	}

	private static void onBosses(TestServerContext server, BiConsumer<ServerLevel, PartyBossMonster> action) {
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (PartyBossMonster boss : level.getEntitiesOfClass(PartyBossMonster.class, new AABB(-30, -64, -15, 30, -40, 15))) {
				action.accept(level, boss);
			}
		});
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name) {
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		shotNow(context, name);
	}

	/** HUD를 켠 채로 (1인칭 손은 HUD를 숨기면 안 보인다). */
	private static void shotWithHand(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name) {
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(false);
			if (client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-c4-" + name).withSize(1280, 720).disableCounterPrefix());
	}

	private static void shotNow(ClientGameTestContext context, String name) {
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(false);
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of("sanupcraft-c4-" + name).withSize(1280, 720).disableCounterPrefix());
	}
}
