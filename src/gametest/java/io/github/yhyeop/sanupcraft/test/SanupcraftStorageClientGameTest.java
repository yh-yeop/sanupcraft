package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;

import io.github.yhyeop.sanupcraft.client.storage.AutocrafterScreen;
import io.github.yhyeop.sanupcraft.client.storage.StockerScreen;
import io.github.yhyeop.sanupcraft.client.storage.TerminalScreen;

/**
 * M7 보관 · 제작: 서랍 앞면 렌더러, 창고 단말 · 제작 단말 · 자동 제작대 · 재고 유지기 화면을 스크린샷으로 남긴다 (사람이 본다).
 * build/run/clientGameTest/screenshots/
 */
public class SanupcraftStorageClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule show_advancement_messages false");
			// 줄 (z = -4, 남쪽을 봄): 서랍 셋 · 관 · 단말 · 큰 상자 · 제작 단말 · 자동 제작대 · 재고 유지기
			server.runCommand("setblock 37 -60 -4 sanupcraft:iron_drawer[facing=south]{drawer:{item:{id:\"minecraft:cobblestone\",count:1},count:1234}}");
			server.runCommand("setblock 38 -60 -4 sanupcraft:steel_drawer[facing=south]{drawer:{item:{id:\"minecraft:iron_ingot\",count:1},count:64,upgrades:1}}");
			server.runCommand("setblock 37 -59 -4 sanupcraft:iron_drawer[facing=south]{drawer:{item:{id:\"minecraft:oak_sapling\",count:1},count:7}}");
			server.runCommand("setblock 38 -59 -4 sanupcraft:steel_drawer[facing=south]");
			server.runCommand("setblock 39 -60 -4 sanupcraft:storage_cable");
			server.runCommand("setblock 40 -60 -4 sanupcraft:storage_terminal[facing=south]{steam:1000}");
			server.runCommand("setblock 41 -60 -4 sanupcraft:storage_cable");
			server.runCommand("setblock 42 -60 -4 minecraft:chest[facing=south]");
			server.runCommand("item replace block 42 -60 -4 container.0 with minecraft:oak_planks 40");
			server.runCommand("item replace block 42 -60 -4 container.1 with minecraft:redstone 64");
			server.runCommand("item replace block 42 -60 -4 container.2 with minecraft:diamond 3");
			server.runCommand("item replace block 42 -60 -4 container.3 with sanupcraft:steel_ingot 17");
			server.runCommand("item replace block 42 -60 -4 container.4 with minecraft:oak_log 20");
			server.runCommand("setblock 39 -59 -4 sanupcraft:storage_cable");
			server.runCommand("setblock 40 -59 -4 sanupcraft:crafting_terminal[facing=south]{steam:1000}");
			server.runCommand("setblock 43 -60 -4 sanupcraft:autocrafter[facing=south]{steam:600,pattern:{Items:[{Slot:0b,id:\"minecraft:oak_log\",count:1}]}}");
			server.runCommand("item replace block 43 -60 -4 container.0 with minecraft:oak_log 6");
			server.runCommand("item replace block 43 -60 -4 container.9 with minecraft:oak_planks 12");
			server.runCommand("setblock 44 -60 -4 sanupcraft:stocker[facing=south]{target_count:32,target:{Items:[{Slot:0b,id:\"minecraft:oak_planks\",count:1}]}}");
			server.runCommand("tp @a 40.5 -60 -0.5 180 10");
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("sanupcraft-m7-scene");
			server.runCommand("tp @a 37.5 -59.5 -2.2 180 10");
			context.waitTicks(10);
			context.takeScreenshot("sanupcraft-m7-drawers");

			// 서랍 · 단말 아이콘
			String[] items = {"iron_drawer", "steel_drawer", "drawer_upgrade", "drawer_void_upgrade", "storage_cable", "storage_terminal",
					"crafting_terminal", "autocrafter", "stocker"};
			for (int i = 0; i < items.length; i++) {
				server.runCommand("item replace entity @a hotbar." + i + " with sanupcraft:" + items[i]);
			}
			server.runCommand("item replace entity @a inventory.0 with sanupcraft:portable_terminal");
			server.runCommand("item replace entity @a inventory.1 with sanupcraft:ender_portable_terminal");
			server.runCommand("gamemode survival @a");
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(5);
			context.takeScreenshot("sanupcraft-m7-inventory");
			context.setScreen(() -> null);
			server.runCommand("clear @a");

			server.runCommand("tp @a 40.5 -60 -1.5 180 0");
			context.waitTicks(5);
			openAndShoot(context, new BlockPos(40, -60, -4), TerminalScreen.class, "sanupcraft-m7-terminal");
			openAndShoot(context, new BlockPos(40, -59, -4), TerminalScreen.class, "sanupcraft-m7-crafting-terminal");
			server.runCommand("tp @a 43.5 -60 -1.5 180 0");
			context.waitTicks(5);
			openAndShoot(context, new BlockPos(43, -60, -4), AutocrafterScreen.class, "sanupcraft-m7-autocrafter");
			server.runCommand("tp @a 44.5 -60 -1.5 180 0");
			context.waitTicks(5);
			openAndShoot(context, new BlockPos(44, -60, -4), StockerScreen.class, "sanupcraft-m7-stocker");
		}
	}

	private static void openAndShoot(ClientGameTestContext context, BlockPos pos, Class<? extends Screen> screen, String name) {
		context.getInput().lookAt(pos);
		context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(screen);
		context.waitTicks(20);
		context.takeScreenshot(name);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}
}
