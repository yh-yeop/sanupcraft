package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.nether.LavaSteamboat;
import io.github.yhyeop.sanupcraft.nether.ModNether;

/**
 * M5 클라이언트 확인: 새 블록 · 아이템 아이콘 · 용암 증기선(바닐라 배 모델 + 전용 텍스처)이 그려지는지 스크린샷으로 남기고,
 * 네더에서 가장 가까운 버려진 용광로를 찾아 위치를 로그에 쓰고 찍는다 (제단이 실제로 놓였는지는 검사한다).
 * 스크린샷: build/run/clientGameTest/screenshots/m5-*.png
 */
public class SanupcraftM5ClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			blocksAndBoat(context, server, singleplayer);
		}
		// 기본 시험 월드(평지)는 구조물을 만들지 않으므로, 구조물을 켠 보통 월드를 따로 만든다
		try (TestSingleplayerContext singleplayer = context.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(s -> {
					s.setSeed("sanupcraft");
					s.setGenerateStructures(true);
				}).create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			abandonedFurnace(context, server, singleplayer);
		}
	}

	/** 오버월드 평지: 영혼 화로 줄 + 돌로 두른 용암 웅덩이 위의 증기선, 그다음 아이템 아이콘. */
	private static void blocksAndBoat(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("gamemode creative @a");
		server.runCommand("tp @a 40.5 -57 1.5 180 35");
		server.runCommand("setblock 37 -61 -4 minecraft:soul_soil");
		server.runCommand("setblock 37 -60 -4 sanupcraft:soul_furnace[facing=south,lit=true]");
		server.runCommand("setblock 38 -60 -4 sanupcraft:high_pressure_pipe[east=true,west=true]");
		server.runCommand("setblock 39 -60 -4 sanupcraft:precision_assembler[facing=south,lit=true]");
		server.runCommand("setblock 40 -60 -4 sanupcraft:steam_turbine[lit=true]");
		server.runCommand("setblock 41 -60 -4 sanupcraft:steam_pipe[east=true,west=true]");
		server.runCommand("setblock 42 -60 -4 sanupcraft:gold_dispenser");
		server.runCommand("setblock 43 -60 -4 sanupcraft:barter_collector");
		// 용암 웅덩이 (돌 테두리) 위에 증기선
		server.runCommand("fill 37 -61 -11 44 -60 -6 minecraft:stone");
		server.runCommand("fill 38 -60 -10 43 -60 -7 minecraft:air");
		server.runCommand("fill 38 -61 -10 43 -61 -7 minecraft:lava");
		server.runCommand("summon sanupcraft:lava_steamboat 40.5 -60.5 -8.5 {Rotation:[90f,0f]}");
		context.waitTicks(60);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("m5-blocks");
		server.runCommand("tp @a 40.5 -57 -4.0 180 50");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("m5-boat");
		Double boatY = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(LavaSteamboat.class,
				new net.minecraft.world.phys.AABB(30, -70, -20, 50, -40, 0)).stream().findFirst().map(b -> b.getY()).orElse(null));
		Sanupcraft.LOGGER.info("M5 클라이언트: 용암 위 증기선 y = {} (용암 윗면 약 -60.11, 바닥이 0.12 잠김 = -60.23)", boatY);
		if (boatY == null) {
			throw new AssertionError("증기선이 없음 (용암에 사라짐?)");
		}

		String[] items = {"soul_furnace", "high_pressure_pipe", "precision_assembler", "steam_turbine", "gilded_quartz", "precision_part",
				"gold_dispenser", "barter_collector", "debris_sonar"};
		for (int i = 0; i < items.length; i++) {
			server.runCommand("item replace entity @a hotbar." + i + " with sanupcraft:" + items[i]);
		}
		server.runCommand("item replace entity @a inventory.0 with sanupcraft:lava_steamboat");
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
		context.waitTicks(5);
		context.takeScreenshot("m5-inventory");
		context.setScreen(() -> null);
	}

	/** 네더에서 가장 가까운 버려진 용광로를 찾아 위치를 로그에 쓰고, 제단 근처에서 찍는다. */
	private static void abandonedFurnace(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		BlockPos found = server.computeOnServer(s -> {
			ServerLevel nether = s.getLevel(Level.NETHER);
			Holder<Structure> furnace = nether.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ModNether.ABANDONED_FURNACE);
			var pair = nether.getChunkSource().getGenerator().findNearestMapStructure(nether, HolderSet.direct(furnace), BlockPos.ZERO, 100, false);
			return pair == null ? null : pair.getFirst();
		});
		if (found == null) {
			throw new AssertionError("네더에서 버려진 용광로를 찾지 못함");
		}
		Sanupcraft.LOGGER.info("M5 클라이언트: 가장 가까운 버려진 용광로 (네더) = {}", found);
		server.runCommand("gamemode spectator @a");
		server.runCommand("execute in minecraft:the_nether run tp @a " + found.getX() + " 110 " + found.getZ());
		context.waitTicks(100);
		singleplayer.getConnection().waitForChunksRender();
		BlockPos altar = server.computeOnServer(s -> {
			ServerLevel nether = s.getLevel(Level.NETHER);
			for (BlockPos p : BlockPos.betweenClosed(found.offset(-24, 0, -24).atY(32), found.offset(24, 0, 24).atY(120))) {
				if (nether.getBlockState(p).is(ModBoss.GUARDIAN_ALTAR)) {
					return p.immutable();
				}
			}
			return null;
		});
		if (altar == null) {
			throw new AssertionError("버려진 용광로 근처에 제단이 없음: " + found);
		}
		String below = server.computeOnServer(s -> s.getLevel(Level.NETHER).getBlockState(altar.below(2)).toString());
		Sanupcraft.LOGGER.info("M5 클라이언트: 버려진 용광로 제단 = {}, 바닥 아래 블록 = {}", altar, below);
		server.runCommand("execute in minecraft:the_nether run tp @a " + (altar.getX() + 0.5) + " " + (altar.getY() + 5) + " " + (altar.getZ() + 9.5) + " 180 30");
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("m5-abandoned-furnace");
	}
}
