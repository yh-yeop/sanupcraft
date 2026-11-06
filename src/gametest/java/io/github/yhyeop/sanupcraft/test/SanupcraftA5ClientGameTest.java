package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.client.storage.TerminalScreen;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.industry.BigBoilerControllerBlockEntity;
import io.github.yhyeop.sanupcraft.industry.FurnaceTowerControllerBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.BeltMotorBlockEntity;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;
import io.github.yhyeop.sanupcraft.power.SteamTurbineBlockEntity;

/**
 * 확장 M15 R4 · R5 · R6 화면 확인 (판정은 모양이 섰는지만): 돌고 있는 분쇄기 · 압착기 · 터빈 · 굴착기 · 벨트 모터의 움직이는 부품을
 * 틈을 두고 두 장씩, 모양이 선 대형 보일러 · 용광로 탑의 이어진 외벽, 통일한 분쇄기 · 세척기 · 제작 단말 화면을 HUD 없이 찍는다.
 * build/run/clientGameTest/screenshots/a5-*.png
 */
public class SanupcraftA5ClientGameTest implements FabricClientGameTest {
	private static final BlockPos CRUSHER = new BlockPos(0, -60, 0);
	private static final BlockPos PRESS = new BlockPos(2, -60, 0);
	private static final BlockPos TURBINE = new BlockPos(4, -60, 0);
	private static final BlockPos QUARRY = new BlockPos(30, -58, 0);
	private static final BlockPos MOTOR = new BlockPos(8, -60, 0);
	private static final BlockPos WASHER = new BlockPos(10, -60, 0);
	private static final BlockPos BOILER = new BlockPos(15, -60, 0);
	private static final BlockPos TOWER = new BlockPos(20, -60, 0);
	private static final BlockPos TERMINAL = new BlockPos(24, -60, 0);

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode creative @a");
			server.runCommand("setblock 0 -60 0 sanupcraft:crusher[facing=south]");
			server.runCommand("setblock 2 -60 0 sanupcraft:press[facing=south]");
			server.runCommand("setblock 4 -60 0 sanupcraft:steam_turbine");
			// 굴착기는 다른 기계에서 떨어진 공중에 (아래 드릴이 보이게, 캐는 9x9가 다른 기계에 닿지 않게)
			server.runCommand("setblock 30 -58 0 sanupcraft:steam_quarry");
			server.runCommand("setblock 8 -60 0 sanupcraft:belt_motor[facing=south]");
			server.runCommand("setblock 10 -60 0 sanupcraft:washer[facing=south]");
			// 대형 보일러: 제어기 남쪽을 봄, 뒤로 3칸 (x 14..16, z -2..0, y -60..-57), 가운데 (15, -59..-58, -1) 빈칸
			server.runCommand("fill 14 -60 -2 16 -57 0 sanupcraft:big_boiler_casing");
			server.runCommand("fill 15 -59 -1 15 -58 -1 minecraft:air");
			server.runCommand("setblock 15 -60 0 sanupcraft:big_boiler_controller[facing=south]");
			// 용광로 탑: x 19..21, z -2..0, y -60..-58, 가운데 기둥 (20, *, -1) 빈칸
			server.runCommand("fill 19 -60 -2 21 -58 0 sanupcraft:heatproof_bricks");
			server.runCommand("fill 20 -60 -1 20 -58 -1 minecraft:air");
			server.runCommand("setblock 20 -60 0 sanupcraft:furnace_tower_controller[facing=south]");
			server.runCommand("setblock 24 -60 0 sanupcraft:crafting_terminal[facing=south]{steam:1000}");
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				MachineBlockEntity washer = (MachineBlockEntity) level.getBlockEntity(WASHER);
				washer.fill(FluidKind.WATER, 2500, false);
				washer.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 16));
				washer.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
			});
			run(context, server, 40);
			boolean formed = server.computeOnServer(s -> ((BigBoilerControllerBlockEntity) s.overworld().getBlockEntity(BOILER)).isFormed()
					&& ((FurnaceTowerControllerBlockEntity) s.overworld().getBlockEntity(TOWER)).isFormed());
			String running = server.computeOnServer(s -> {
				StringBuilder text = new StringBuilder();
				for (BlockPos pos : new BlockPos[] {CRUSHER, PRESS, TURBINE, QUARRY, MOTOR}) {
					text.append(s.overworld().getBlockState(pos)).append(' ');
				}
				return text.toString();
			});
			Sanupcraft.LOGGER.info("a5 클라이언트: 모양 = {}, 기계 = {}", formed, running);
			if (!formed) {
				throw new AssertionError("대형 보일러 · 용광로 탑 모양이 서지 않음");
			}

			// 기계 줄: 두 장 (부품이 움직였는지). 관전자로 (손 · 떨어짐 없이)
			server.runCommand("gamemode spectator @a");
			server.runCommand("tp @a 4.5 -58.9 4.8 180 20");
			shot(context, singleplayer, server, "machines-1", 20);
			shot(context, singleplayer, server, "machines-2", 3);
			// 가까이: 분쇄기 · 압착기, 터빈 (위에서), 굴착기 드릴 (옆에서), 벨트 모터
			server.runCommand("tp @a 1.0 -59.0 2.8 180 25");
			shot(context, singleplayer, server, "crusher-press-1", 10);
			shot(context, singleplayer, server, "crusher-press-2", 4);
			server.runCommand("tp @a 4.5 -59.3 1.9 180 50");
			shot(context, singleplayer, server, "turbine-1", 10);
			shot(context, singleplayer, server, "turbine-2", 2);
			server.runCommand("tp @a 30.5 -60.0 2.6 180 -5");
			shot(context, singleplayer, server, "quarry-1", 10);
			shot(context, singleplayer, server, "quarry-2", 2);
			server.runCommand("tp @a 8.5 -60.0 2.6 180 12");
			shot(context, singleplayer, server, "motor-1", 10);
			shot(context, singleplayer, server, "motor-2", 3);
			// 다중 블록: 앞 · 비스듬히 · 위
			server.runCommand("tp @a 17.5 -58.1 7.5 180 12");
			shot(context, singleplayer, server, "multiblock-front", 20);
			server.runCommand("tp @a 25.0 -55.0 5.0 129 25");
			shot(context, singleplayer, server, "multiblock-side", 10);
			server.runCommand("tp @a 17.5 -51.0 3.5 180 60");
			shot(context, singleplayer, server, "multiblock-top", 10);

			// 화면 (HUD를 다시 켠다)
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			server.runCommand("gamemode survival @a");
			server.runCommand("tp @a 4.5 -59 4.5 180 10");
			open(context, server, CRUSHER);
			context.takeScreenshot(TestScreenshotOptions.of("a5-crusher-screen").disableCounterPrefix());
			context.setScreen(() -> null);
			open(context, server, WASHER);
			context.takeScreenshot(TestScreenshotOptions.of("a5-washer-screen").disableCounterPrefix());
			context.setScreen(() -> null);
			// 제작 단말은 바라보고 우클릭으로 연다 (단말 시험과 같은 방법)
			server.runCommand("tp @a 24.5 -60 2.5 180 0");
			run(context, server, 5);
			context.getInput().lookAt(TERMINAL);
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(TerminalScreen.class);
			run(context, server, 10);
			context.takeScreenshot(TestScreenshotOptions.of("a5-terminal-screen").disableCounterPrefix());
			context.setScreen(() -> null);
			server.runCommand("gamemode creative @a");
		}
	}

	/** 기계마다 증기 · 재료를 채운다 (돌고 있게). */
	private static void feed(TestServerContext server) {
		// 터빈은 내보낼 곳이 없으면 멈추니, 만든 증기를 비운다
		server.runCommand("data merge block 4 -60 0 {steam:0}");
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			MachineBlockEntity crusher = (MachineBlockEntity) level.getBlockEntity(CRUSHER);
			crusher.acceptSteam(MachineBlockEntity.STEAM_CAPACITY);
			crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 32));
			crusher.setItem(MachineBlockEntity.SLOT_RESULT, ItemStack.EMPTY);
			MachineBlockEntity press = (MachineBlockEntity) level.getBlockEntity(PRESS);
			press.acceptSteam(MachineBlockEntity.STEAM_CAPACITY);
			press.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 32));
			press.setItem(MachineBlockEntity.SLOT_RESULT, ItemStack.EMPTY);
			((SteamTurbineBlockEntity) level.getBlockEntity(TURBINE)).acceptHighPressureSteam(40);
			((QuarryBlockEntity) level.getBlockEntity(QUARRY)).acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
			((BeltMotorBlockEntity) level.getBlockEntity(MOTOR)).acceptSteam(BeltMotorBlockEntity.STEAM_CAPACITY);
			MachineBlockEntity washer = (MachineBlockEntity) level.getBlockEntity(WASHER);
			washer.acceptSteam(MachineBlockEntity.STEAM_CAPACITY / 2);
		});
	}

	private static void run(ClientGameTestContext context, TestServerContext server, int ticks) {
		for (int i = 0; i < ticks; i++) {
			feed(server);
			context.waitTick();
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server, String name, int wait) {
		run(context, server, wait);
		if (wait >= 10) {
			singleplayer.getConnection().waitForChunksRender();
		}
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.gui.hud.getChat().clearMessages(false);
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		context.takeScreenshot(TestScreenshotOptions.of("a5-" + name).withSize(1280, 720).disableCounterPrefix());
	}

	/** 서버에서 그 블록의 화면을 연다 (첫 플레이어). */
	private static void open(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
		server.runOnServer((MinecraftServer s) -> {
			ServerPlayer player = s.getPlayerList().getPlayers().get(0);
			player.openMenu((MenuProvider) s.overworld().getBlockEntity(pos));
		});
		run(context, server, 10);
	}
}
