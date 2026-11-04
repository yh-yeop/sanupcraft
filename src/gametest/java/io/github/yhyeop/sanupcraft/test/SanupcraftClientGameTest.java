package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.phys.AABB;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.client.MachineScreen;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/**
 * 진짜 클라이언트를 띄워 텍스처 · 기계 화면 · 갑옷 · 보스가 그려지는지 스크린샷으로 남긴다.
 * 판정은 하지 않고 사람이 본다: build/run/clientGameTest/screenshots/
 * 편의 모드와 같이 돌리면(-PpackMods) 시체 모드가 강철 갑옷을 남기는지도 확인한다.
 */
public class SanupcraftClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();

			// 플레이어는 (0, ~, 0)에서 북쪽(-z)을 본다. 앞 4칸에 기계 줄, 그 뒤에 갑옷 거치대와 보스.
			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("tp @a 0.5 -60 0.5 180 15");
			server.runCommand("setblock -2 -60 -4 sanupcraft:crusher[facing=south]");
			server.runCommand("setblock -1 -60 -4 sanupcraft:press[facing=south]");
			server.runCommand("setblock 0 -60 -4 sanupcraft:assembler[facing=south,lit=true]");
			server.runCommand("setblock 1 -60 -4 sanupcraft:guardian_altar");
			server.runCommand("summon armor_stand 2.5 -60 -3.5 {Rotation:[180f,0f],Tags:[\"steel\"]}");
			server.runCommand("item replace entity @e[tag=steel] armor.head with sanupcraft:steel_helmet");
			server.runCommand("item replace entity @e[tag=steel] armor.chest with sanupcraft:steel_chestplate");
			server.runCommand("item replace entity @e[tag=steel] armor.legs with sanupcraft:steel_leggings");
			server.runCommand("item replace entity @e[tag=steel] armor.feet with sanupcraft:steel_boots");
			server.runCommand("item replace entity @e[tag=steel] weapon.mainhand with sanupcraft:steel_sword");
			server.runCommand("summon sanupcraft:rusty_guardian 0.5 -60 -9.5 {NoAI:1b,Rotation:[0f,0f]}");
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("sanupcraft-scene");

			// 아이템 아이콘: 핫바와 인벤토리에 넣고 인벤토리 화면
			String[] items = {"iron_dust", "iron_plate", "gear_part", "guardian_core", "reinforced_machine_frame",
					"guardian_summoner", "steel_sword", "steel_helmet", "steel_chestplate"};
			for (int i = 0; i < items.length; i++) {
				server.runCommand("item replace entity @a hotbar." + i + " with sanupcraft:" + items[i]);
			}
			server.runCommand("item replace entity @a inventory.0 with sanupcraft:steel_leggings");
			server.runCommand("item replace entity @a inventory.1 with sanupcraft:steel_boots");
			server.runCommand("item replace entity @a inventory.2 with sanupcraft:crusher");
			server.runCommand("item replace entity @a inventory.3 with sanupcraft:press");
			server.runCommand("item replace entity @a inventory.4 with sanupcraft:assembler");
			server.runCommand("item replace entity @a inventory.5 with sanupcraft:guardian_altar");
			server.runCommand("gamemode survival @a");
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(5);
			context.takeScreenshot("sanupcraft-inventory");
			context.setScreen(() -> null);

			// 기계 화면: 분쇄기를 보고 우클릭
			server.runCommand("item replace entity @a hotbar.0 with minecraft:air");
			server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
			context.getInput().lookAt(new BlockPos(-2, -60, -4));
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(MachineScreen.class);
			context.takeScreenshot("sanupcraft-machine-screen");
			context.setScreen(() -> null);

			m2Scene(context, server, singleplayer);

			if (FabricLoader.getInstance().isModLoaded("corpsegraves")) {
				checkCorpse(context, server);
			}
		}
	}

	/** M2: 보일러 → 파이프 → 벨트 모터 → 돌아가는 벨트 → 상자, 내열 갑옷 거치대, 용광로 파수꾼. */
	private static void m2Scene(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer) {
		server.runCommand("gamemode creative @a");
		server.runCommand("setblock 17 -60 -4 sanupcraft:boiler[facing=south]");
		server.runCommand("data merge block 17 -60 -4 {water:8000}");
		server.runCommand("item replace block 17 -60 -4 container.0 with minecraft:coal 16");
		server.runCommand("setblock 18 -60 -4 sanupcraft:steam_pipe");
		server.runCommand("setblock 19 -60 -4 sanupcraft:steam_pipe");
		server.runCommand("setblock 20 -60 -4 sanupcraft:belt_motor[facing=south]");
		for (int x = 21; x <= 23; x++) {
			server.runCommand("setblock " + x + " -60 -4 sanupcraft:conveyor_belt[facing=east]");
		}
		server.runCommand("setblock 24 -60 -4 minecraft:chest[facing=west]");
		server.runCommand("summon armor_stand 26.5 -60 -3.5 {Rotation:[180f,0f],Tags:[\"heat\"]}");
		server.runCommand("item replace entity @e[tag=heat] armor.head with sanupcraft:heatproof_helmet");
		server.runCommand("item replace entity @e[tag=heat] armor.chest with sanupcraft:heatproof_chestplate");
		server.runCommand("item replace entity @e[tag=heat] armor.legs with sanupcraft:heatproof_leggings");
		server.runCommand("item replace entity @e[tag=heat] armor.feet with sanupcraft:heatproof_boots");
		server.runCommand("item replace entity @e[tag=heat] weapon.mainhand with sanupcraft:heatproof_sword");
		server.runCommand("summon sanupcraft:furnace_warden 15.5 -60 -9.5 {NoAI:1b,Rotation:[20f,0f]}");
		server.runCommand("tp @a 21.5 -60 0.5 180 25");
		for (int i = 0; i < 3; i++) {
			server.runCommand("summon item 21.5 -59.7 -3.5 {Item:{id:\"minecraft:raw_iron\",count:1},PickupDelay:32767}");
			context.waitTicks(4);
		}
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot("sanupcraft-m2-scene");
		server.runCommand("tp @a 15.5 -59 -3.5 180 10");
		context.waitTicks(10);
		context.takeScreenshot("sanupcraft-warden");
		server.runCommand("gamemode survival @a");
	}

	/** 모드팩의 시체 모드: 강철 갑옷을 입고 죽으면 시체(마네킹)가 갑옷을 입은 채 남는다 (CorpseGravesCompat). */
	private static void checkCorpse(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("item replace entity @a armor.head with sanupcraft:steel_helmet");
		server.runCommand("item replace entity @a armor.chest with sanupcraft:steel_chestplate");
		server.runCommand("item replace entity @a armor.legs with sanupcraft:steel_leggings");
		server.runCommand("item replace entity @a armor.feet with sanupcraft:steel_boots");
		server.runCommand("tp @a 4.5 -60 -2.5 180 0");
		context.waitTicks(5);
		server.runCommand("kill @a");
		context.waitForScreen(DeathScreen.class);
		context.waitTicks(30);
		context.clickScreenButton("deathScreen.respawn");
		context.waitForScreen(null);
		server.runCommand("tp @a 4.5 -60 1.5 180 20");
		server.waitFor(s -> !corpses(s).isEmpty(), 200);
		// CorpseGravesCompat가 displayArmor를 켜므로 시체가 강철 갑옷을 입고 있어야 한다
		server.waitFor(s -> corpses(s).stream().anyMatch(m -> m.getItemBySlot(EquipmentSlot.CHEST).is(SteelGear.STEEL_CHESTPLATE)), 100);
		context.waitTicks(20);
		server.runOnServer(s -> corpses(s).forEach(m -> Sanupcraft.LOGGER.info("시체 장비: {} / {} / {} / {}",
				m.getItemBySlot(EquipmentSlot.HEAD), m.getItemBySlot(EquipmentSlot.CHEST),
				m.getItemBySlot(EquipmentSlot.LEGS), m.getItemBySlot(EquipmentSlot.FEET))));
		context.takeScreenshot("sanupcraft-corpse");
	}

	private static List<Mannequin> corpses(MinecraftServer server) {
		return server.overworld().getEntitiesOfClass(Mannequin.class, new AABB(-16, -64, -16, 16, -50, 16), m -> true);
	}
}
