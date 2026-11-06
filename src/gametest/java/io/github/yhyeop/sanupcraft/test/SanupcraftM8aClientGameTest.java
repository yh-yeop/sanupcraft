package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * M8 묶음 A 화면 확인 (판정 없이 스크린샷): 모양이 선 대형 보일러 (외벽 무늬 · 불 켜진 제어기),
 * 레일 위 증기 기관차 (보일러를 얹은 수레) + 화물칸 (통을 얹은 수레) + 적재 · 하역 역.
 * 기관차의 연결 칸은 저장 데이터(UUID)로 바로 잇는다.
 */
public class SanupcraftM8aClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("tp @a 0.5 -60 4.5 180 15");

			// 대형 보일러: 제어기 (0, -60, -4) 남쪽을 봄, 모양은 뒤(북쪽)로 z -4..-6
			server.runCommand("fill -1 -60 -6 1 -57 -4 sanupcraft:big_boiler_casing");
			server.runCommand("setblock 0 -59 -5 air");
			server.runCommand("setblock 0 -58 -5 air");
			server.runCommand("setblock 0 -60 -4 sanupcraft:big_boiler_controller[facing=south]");
			server.runCommand("data merge block 0 -60 -4 {water:48000}");
			server.runCommand("item replace block 0 -60 -4 container.0 with minecraft:coal 16");
			server.runCommand("setblock 2 -59 -5 sanupcraft:steam_pipe");
			server.runCommand("setblock 3 -59 -5 sanupcraft:crusher[facing=south]");

			// 레일 · 역 · 열차
			server.runCommand("fill -12 -60 0 12 -60 0 minecraft:rail[shape=east_west]");
			server.runCommand("setblock 4 -60 -1 sanupcraft:cargo_station");
			server.runCommand("setblock -4 -60 -1 sanupcraft:cargo_station[unload=true]");
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("summon sanupcraft:cargo_wagon -6.1 -60 0.5 {UUID:[I;8,1,2,3]}");
			server.runCommand("summon sanupcraft:cargo_wagon -7.7 -60 0.5 {UUID:[I;8,1,2,4]}");
			server.runCommand("summon sanupcraft:steam_locomotive -4.5 -60 0.5 {cars:[[I;8,1,2,3],[I;8,1,2,4]],running:1b,push_x:1.0d,"
					+ "fuel:{id:\"minecraft:coal\",count:8}}");
			context.waitTicks(12);
			context.takeScreenshot("sanupcraft-m8a-industry");
			context.waitTicks(10);
			context.takeScreenshot("sanupcraft-m8a-train-moving");
		}
	}
}
