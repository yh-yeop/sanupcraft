package io.github.yhyeop.sanupcraft.test;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.resource.ModResources;

/**
 * 확장 3 D1 클라이언트 확인: 일반 지형 월드(시드 sanupcraft)에서 스폰 근처 심층암에 크롬철석이 실제로 생기는지,
 * 가장 가까운 사바나 · 메사 · 사막 근처에 보크사이트가 생기는지 블록을 세어 로그로 남기고,
 * 새 블록 여섯 개 · 아이템 여덟 개의 모습을 스크린샷으로 남긴다.
 * 스크린샷: build/run/clientGameTest/screenshots/d1-*.png
 */
public class SanupcraftD1ClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("sanupcraft")).create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");

			// 스폰(0, 0) 둘레 9x9 청크, Y -64 ~ -20 안의 크롬철석, 그 위(Y -19 이상)에는 없어야 함
			int[] chromite = server.computeOnServer(s -> scan(s.overworld(), BlockPos.ZERO, 4, ModResources.DEEPSLATE_CHROMITE_ORE, -64, -20));
			Sanupcraft.LOGGER.info("D1 클라이언트: 스폰 둘레 9x9 청크 크롬철석 Y -64~-20 = {}개 (그 밖의 높이 = {}개)", chromite[0], chromite[1]);
			if (chromite[0] <= 0) {
				throw new AssertionError("스폰 근처 심층암에 크롬철석이 하나도 없음");
			}
			if (chromite[1] != 0) {
				throw new AssertionError("크롬철석이 Y -20 위에 생김: " + chromite[1]);
			}

			// 사바나 · 메사 · 사막 중 가장 가까운 곳 근처 7x7 청크의 보크사이트 (Y 40 ~ 90)
			Pair<BlockPos, Holder<Biome>> biome = server.computeOnServer(s -> s.overworld().findClosestBiome3d(
					h -> h.is(BiomeTags.IS_SAVANNA) || h.is(BiomeTags.IS_BADLANDS) || h.is(Biomes.DESERT), BlockPos.ZERO, 6400, 32, 64));
			if (biome == null) {
				throw new AssertionError("6400칸 안에 사바나 · 메사 · 사막이 없음");
			}
			BlockPos spot = biome.getFirst();
			int[] bauxite = server.computeOnServer(s -> scan(s.overworld(), spot, 3, ModResources.BAUXITE_ORE, 40, 90));
			Sanupcraft.LOGGER.info("D1 클라이언트: {} 근처 {} 7x7 청크 보크사이트 Y 40~90 = {}개 (그 밖의 높이 = {}개)",
					biome.getSecond().unwrapKey().map(k -> k.identifier().toString()).orElse("?"), spot, bauxite[0], bauxite[1]);
			if (bauxite[0] <= 0) {
				throw new AssertionError("사바나 · 메사 · 사막 근처에 보크사이트가 하나도 없음");
			}

			// 모습: 하늘 위 평평한 판에 새 블록 여섯 개
			server.runCommand("gamemode creative @a");
			server.runCommand("fill -5 249 -7 5 249 2 minecraft:smooth_stone");
			String[] blocks = {"deepslate_chromite_ore", "bauxite_ore", "raw_chromium_block", "raw_aluminium_block", "chromium_block", "aluminium_block"};
			for (int i = 0; i < blocks.length; i++) {
				server.runCommand("setblock " + (i - 3) + " 250 0 sanupcraft:" + blocks[i]);
			}
			server.runCommand("tp @a 0.5 250 -4.5 0 12");
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("d1-blocks");

			String[] items = {"raw_chromium", "raw_aluminium", "chromium_dust", "aluminium_dust", "refined_chromium_dust", "refined_aluminium_dust",
					"chromium_ingot", "aluminium_ingot"};
			for (int i = 0; i < items.length; i++) {
				server.runCommand("item replace entity @a hotbar." + (i % 8) + " with sanupcraft:" + items[i]);
			}
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(5);
			context.takeScreenshot("d1-items");
			context.setScreen(() -> null);
		}
	}

	/** 중심 둘레 (2r+1)^2 청크에서 그 블록을 센다: {높이 범위 안, 범위 밖}. 높이 범위는 [minY, maxY]. */
	private static int[] scan(ServerLevel level, BlockPos center, int radius, Block block, int minY, int maxY) {
		int inside = 0;
		int outside = 0;
		int cx = center.getX() >> 4;
		int cz = center.getZ() >> 4;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				LevelChunk chunk = level.getChunk(x, z);
				for (int dx = 0; dx < 16; dx++) {
					for (int dz = 0; dz < 16; dz++) {
						for (int y = level.getMinY(); y < level.getMaxY(); y++) {
							if (chunk.getBlockState(pos.set(x * 16 + dx, y, z * 16 + dz)).is(block)) {
								if (y >= minY && y <= maxY) {
									inside++;
								} else {
									outside++;
								}
							}
						}
					}
				}
			}
		}
		return new int[] {inside, outside};
	}
}
