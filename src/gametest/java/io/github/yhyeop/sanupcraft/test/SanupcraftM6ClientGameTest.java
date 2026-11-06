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
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.end.ModEnd;

/**
 * M6 클라이언트 확인: 새 블록 · 아이템 아이콘 · 공허 거상(임시: 보라 철 골렘) · 공허 수정(작은 엔드 수정 + 빛줄기)이 그려지는지 스크린샷으로 남기고,
 * 보통 월드의 엔드에서 가장 가까운 버려진 정거장을 찾아 위치를 로그에 쓰고 찍는다 (제단이 실제로 놓였는지 검사한다).
 * 스크린샷: build/run/clientGameTest/screenshots/m6-*.png
 */
public class SanupcraftM6ClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			blocksAndBoss(context, server, singleplayer);
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(s -> {
					s.setSeed("sanupcraft");
					s.setGenerateStructures(true);
				}).create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			abandonedStation(context, server, singleplayer);
		}
	}

	/** 오버월드 평지: 새 블록 줄, 공허 거상 + 수정 넷, 그다음 아이템 아이콘. */
	private static void blocksAndBoss(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("gamemode creative @a");
		server.runCommand("difficulty normal");
		server.runCommand("tp @a 40.5 -56 8.5 180 20");
		server.runCommand("setblock 37 -60 -2 sanupcraft:void_condenser[lit=true]");
		server.runCommand("setblock 38 -60 -2 sanupcraft:high_pressure_pipe[east=true,west=true]");
		server.runCommand("setblock 39 -60 -2 sanupcraft:chorus_harvester[facing=south]");
		server.runCommand("setblock 40 -60 -2 sanupcraft:dimensional_vault");
		server.runCommand("setblock 41 -60 -2 sanupcraft:breath_collector");
		server.runCommand("summon sanupcraft:void_colossus 40.5 -60 -10.5 {NoAI:1b,Rotation:[0f,0f]}");
		context.waitTicks(5);
		server.runOnServer(s -> {
			for (VoidColossus colossus : s.overworld().getEntitiesOfClass(VoidColossus.class, new AABB(30, -70, -20, 50, -40, 0))) {
				colossus.spawnCrystals(s.overworld());
			}
		});
		context.waitTicks(60);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("m6-blocks-boss");

		String[] items = {"colossus_summoner", "void_core", "void_helmet", "void_chestplate", "void_leggings", "void_boots", "void_sword",
				"void_condenser", "chorus_harvester"};
		for (int i = 0; i < items.length; i++) {
			server.runCommand("item replace entity @a hotbar." + i + " with sanupcraft:" + items[i]);
		}
		server.runCommand("item replace entity @a inventory.0 with sanupcraft:dimensional_vault");
		server.runCommand("item replace entity @a inventory.1 with sanupcraft:breath_collector");
		server.runCommand("item replace entity @a armor.head with sanupcraft:void_helmet");
		server.runCommand("item replace entity @a armor.chest with sanupcraft:void_chestplate");
		server.runCommand("item replace entity @a armor.legs with sanupcraft:void_leggings");
		server.runCommand("item replace entity @a armor.feet with sanupcraft:void_boots");
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
		context.waitTicks(5);
		context.takeScreenshot("m6-inventory");
		context.setScreen(() -> null);
	}

	/** 엔드에서 (1200, 0) 근처 가장 가까운 버려진 정거장을 찾아 위치를 로그에 쓰고, 제단 근처에서 찍는다. */
	private static void abandonedStation(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		BlockPos found = server.computeOnServer(s -> {
			ServerLevel end = s.getLevel(Level.END);
			Holder<Structure> station = end.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ModEnd.ABANDONED_STATION);
			var pair = end.getChunkSource().getGenerator().findNearestMapStructure(end, HolderSet.direct(station), new BlockPos(1200, 0, 0), 100, false);
			return pair == null ? null : pair.getFirst();
		});
		if (found == null) {
			throw new AssertionError("엔드에서 버려진 정거장을 찾지 못함");
		}
		Sanupcraft.LOGGER.info("M6 클라이언트: 가장 가까운 버려진 정거장 (엔드) = {}", found);
		server.runCommand("gamemode spectator @a");
		server.runCommand("execute in minecraft:the_end run tp @a " + found.getX() + " 120 " + found.getZ());
		context.waitTicks(100);
		singleplayer.getConnection().waitForChunksRender();
		BlockPos altar = server.computeOnServer(s -> {
			ServerLevel end = s.getLevel(Level.END);
			for (BlockPos p : BlockPos.betweenClosed(found.offset(-24, 0, -24).atY(10), found.offset(24, 0, 24).atY(120))) {
				if (end.getBlockState(p).is(ModBoss.GUARDIAN_ALTAR)) {
					return p.immutable();
				}
			}
			return null;
		});
		if (altar == null) {
			throw new AssertionError("버려진 정거장 근처에 제단이 없음: " + found);
		}
		String legFoot = server.computeOnServer(s -> {
			ServerLevel end = s.getLevel(Level.END);
			// 다리 (1, 0, 1) 아래 블록: 틀의 제단은 (5, 6, 5) → 다리 발은 제단에서 (-4, -6, -4) (회전에 따라 다름 → 네 귀퉁이를 다 본다)
			StringBuilder out = new StringBuilder();
			for (int dx = -4; dx <= 4; dx += 8) {
				for (int dz = -4; dz <= 4; dz += 8) {
					BlockPos foot = altar.offset(dx, -6, dz);
					out.append(foot).append('=').append(end.getBlockState(foot).getBlock()).append(" 아래 ").append(end.getBlockState(foot.below()).getBlock()).append("; ");
				}
			}
			return out.toString();
		});
		Sanupcraft.LOGGER.info("M6 클라이언트: 버려진 정거장 제단 = {}, 다리 발 = {}", altar, legFoot);
		server.runCommand("execute in minecraft:the_end run tp @a " + (altar.getX() + 0.5) + " " + (altar.getY() + 6) + " " + (altar.getZ() + 14.5) + " 180 25");
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("m6-abandoned-station");
	}
}
