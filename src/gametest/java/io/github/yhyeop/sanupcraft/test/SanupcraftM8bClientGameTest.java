package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.industry.FurnaceTowerControllerBlockEntity;

/**
 * M8 묶음 B 클라이언트 확인: 용광로 탑(모양이 선 벽돌 무늬 · 불 켜진 제어기) · 세척기 · 고압 정련기 블록, 아이템 아이콘,
 * 세척기 화면(물 눈금) · 고압 정련기 화면 · 용광로 탑 화면을 스크린샷으로 남긴다.
 * 스크린샷: build/run/clientGameTest/screenshots/m8b-*.png
 */
public class SanupcraftM8bClientGameTest implements FabricClientGameTest {
	private static final BlockPos TOWER = new BlockPos(40, -60, -4);
	private static final BlockPos WASHER = new BlockPos(37, -60, -4);
	private static final BlockPos REFINER = new BlockPos(43, -60, -4);

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
			// 제어기는 남쪽을 보고, 탑은 북쪽으로 3칸 (x 39..41, z -6..-4, y -60..-58), 가운데 기둥 (40, *, -5)은 빈칸
			server.runCommand("fill 39 -60 -6 41 -58 -4 sanupcraft:heatproof_bricks");
			server.runCommand("fill 40 -60 -5 40 -58 -5 minecraft:air");
			server.runCommand("setblock 40 -60 -4 sanupcraft:furnace_tower_controller[facing=south]");
			server.runCommand("setblock 37 -60 -4 sanupcraft:washer[facing=south]");
			server.runCommand("setblock 43 -60 -4 sanupcraft:hp_refiner[facing=south]");
			server.runOnServer(s -> {
				FurnaceTowerControllerBlockEntity tower = (FurnaceTowerControllerBlockEntity) s.overworld().getBlockEntity(TOWER);
				tower.setItem(0, new ItemStack(ModItems.IRON_DUST, 32));
				MachineBlockEntity washer = (MachineBlockEntity) s.overworld().getBlockEntity(WASHER);
				washer.fill(FluidKind.WATER, 2500, false);
				washer.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 16));
				washer.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
				MachineBlockEntity refiner = (MachineBlockEntity) s.overworld().getBlockEntity(REFINER);
				refiner.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_GOLD, 8));
			});
			for (int i = 0; i < 40; i++) {
				server.runOnServer(s -> {
					((FurnaceTowerControllerBlockEntity) s.overworld().getBlockEntity(TOWER)).acceptHighPressureSteam(100);
					((MachineBlockEntity) s.overworld().getBlockEntity(REFINER)).acceptHighPressureSteam(MachineBlockEntity.STEAM_CAPACITY);
				});
				context.waitTick();
			}
			boolean formed = server.computeOnServer(s -> ((FurnaceTowerControllerBlockEntity) s.overworld().getBlockEntity(TOWER)).isFormed());
			Sanupcraft.LOGGER.info("M8b 클라이언트: 용광로 탑 모양 = {}", formed);
			if (!formed) {
				throw new AssertionError("용광로 탑 모양이 서지 않음");
			}
			server.runCommand("tp @a 40.5 -57 2.5 180 30");
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("m8b-blocks");

			server.runCommand("gamemode survival @a");
			open(context, server, WASHER);
			context.takeScreenshot("m8b-washer-screen");
			context.setScreen(() -> null);
			open(context, server, REFINER);
			context.takeScreenshot("m8b-refiner-screen");
			context.setScreen(() -> null);
			open(context, server, TOWER);
			context.takeScreenshot("m8b-tower-screen");
			context.setScreen(() -> null);

			String[] items = {"washer", "hp_refiner", "refined_iron_dust", "refined_copper_dust", "refined_gold_dust", "rare_dust",
					"heatproof_bricks", "furnace_tower_controller"};
			for (int i = 0; i < items.length; i++) {
				server.runCommand("item replace entity @a hotbar." + i + " with sanupcraft:" + items[i]);
			}
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(5);
			context.takeScreenshot("m8b-inventory");
			context.setScreen(() -> null);
		}
	}

	/** 서버에서 그 블록의 화면을 연다 (첫 플레이어). */
	private static void open(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
		server.runOnServer((MinecraftServer s) -> {
			ServerPlayer player = s.getPlayerList().getPlayers().get(0);
			player.openMenu((MenuProvider) s.overworld().getBlockEntity(pos));
		});
		context.waitTicks(10);
	}
}
