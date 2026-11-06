package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.industry.BigBoilerCasingBlock;
import io.github.yhyeop.sanupcraft.industry.BigBoilerControllerBlockEntity;
import io.github.yhyeop.sanupcraft.industry.ModIndustry;
import io.github.yhyeop.sanupcraft.industry.MultiblockShape;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.power.BoilerBlock;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.transport.CargoStationBlock;
import io.github.yhyeop.sanupcraft.transport.CargoStationBlockEntity;
import io.github.yhyeop.sanupcraft.transport.CargoWagon;
import io.github.yhyeop.sanupcraft.transport.ModTransport;
import io.github.yhyeop.sanupcraft.transport.SteamLocomotive;
import io.github.yhyeop.sanupcraft.transport.TrainCoupling;

/** M8 묶음 A 검사: 대형 보일러 (I1) · 증기 기관차 · 화물 열차 (I5). */
public class SanupcraftM8aGameTest {
	/** 제어기 자리 (북쪽을 봄): 모양은 x 1..3, z 1..3, y 1..4. */
	private static final BlockPos CONTROLLER = new BlockPos(2, 1, 1);
	private static final Direction FACING = Direction.NORTH;

	// ---- 대형 보일러 ----

	/** 외벽이 하나 빠지면 그 칸을, 빈칸이 막히면 그 칸을 알려 주고, 다 맞으면 모양이 서고 외벽 무늬가 바뀐다. */
	@GameTest
	public void bigBoilerFormsOnlyWithRightShape(GameTestHelper helper) {
		BigBoilerControllerBlockEntity controller = buildBigBoiler(helper);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(CONTROLLER);
		BlockState state = level.getBlockState(abs);
		helper.assertValueEqual(BigBoilerControllerBlockEntity.SHAPE.casings(CONTROLLER, FACING).size(), 33, "외벽 칸 수");
		helper.assertValueEqual(BigBoilerControllerBlockEntity.SHAPE.positions(CONTROLLER, FACING).size(), 36, "모양 칸 수");

		BlockPos corner = new BlockPos(3, 4, 3);
		helper.setBlock(corner, Blocks.AIR);
		helper.assertFalse(controller.check(level, abs, state), "외벽 하나가 빠졌는데 모양이 섬");
		helper.assertValueEqual(controller.problem().pos(), helper.absolutePos(corner), "알려 준 틀린 칸");
		helper.assertValueEqual(controller.problem().expected(), MultiblockShape.Part.CASING, "틀린 칸에 와야 하는 것");

		helper.setBlock(corner, ModIndustry.BIG_BOILER_CASING);
		BlockPos hollow = new BlockPos(2, 3, 2);
		helper.setBlock(hollow, Blocks.STONE);
		helper.assertFalse(controller.check(level, abs, state), "가운데가 막혔는데 모양이 섬");
		helper.assertValueEqual(controller.problem().pos(), helper.absolutePos(hollow), "막힌 빈칸");
		helper.assertValueEqual(controller.problem().expected(), MultiblockShape.Part.HOLLOW, "빈칸이어야 함");

		helper.setBlock(hollow, Blocks.AIR);
		helper.assertTrue(controller.check(level, abs, state), "모양이 맞는데 서지 않음: " + controller.problem());
		helper.assertTrue(controller.problem() == null, "맞는데 틀린 칸이 남음");
		helper.assertTrue(helper.getBlockState(corner).getValue(BigBoilerCasingBlock.FORMED), "외벽 무늬가 바뀌지 않음");
		helper.succeed();
	}

	/** 모양이 서면 틱당 증기가 보일러의 6배이고, 외벽에 붙은 파이프로 옆 기계(압착기)에 증기가 간다. */
	@GameTest(maxTicks = 100)
	public void bigBoilerMakesSixfoldSteamAndFeedsMachine(GameTestHelper helper) {
		BigBoilerControllerBlockEntity controller = buildBigBoiler(helper);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(CONTROLLER);
		controller.fill(FluidKind.WATER, BigBoilerControllerBlockEntity.WATER_CAPACITY, false);
		controller.setItem(0, new ItemStack(Items.COAL, 8));
		helper.assertTrue(controller.check(level, abs, level.getBlockState(abs)), "모양이 서지 않음: " + controller.problem());

		int before = controller.steam();
		BoilerBlockEntity.serverTick(level, abs, level.getBlockState(abs), controller);
		helper.assertValueEqual(controller.steam() - before, BoilerBlockEntity.STEAM_PER_TICK * 6, "한 틱에 만든 증기 (보일러 6대 몫)");

		// 동쪽 벽 (x 3) 가운데에 파이프, 그 너머에 압착기
		helper.setBlock(new BlockPos(4, 2, 2), ModPower.STEAM_PIPE);
		helper.setBlock(new BlockPos(5, 2, 2), ModMachines.PRESS);
		MachineBlockEntity press = helper.getBlockEntity(new BlockPos(5, 2, 2), MachineBlockEntity.class);
		press.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 4));
		helper.succeedWhen(() -> helper.assertTrue(press.steam() > 0 || !press.result().isEmpty(), "외벽의 파이프로 증기가 오지 않음"));
	}

	/** 외벽을 부수면 바로 (다음 틱) 모양이 풀리고 다른 외벽의 무늬도 꺼지며 불이 꺼진다. */
	@GameTest(maxTicks = 60)
	public void breakingCasingUnformsBigBoiler(GameTestHelper helper) {
		BigBoilerControllerBlockEntity controller = buildBigBoiler(helper);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(CONTROLLER);
		helper.assertTrue(controller.check(level, abs, level.getBlockState(abs)), "모양이 서지 않음: " + controller.problem());
		BlockPos other = new BlockPos(1, 4, 3);
		helper.assertTrue(helper.getBlockState(other).getValue(BigBoilerCasingBlock.FORMED), "처음부터 무늬가 꺼져 있음");

		helper.destroyBlock(new BlockPos(3, 2, 2));
		helper.succeedWhen(() -> {
			helper.assertFalse(controller.isFormed(), "외벽을 부쉈는데 모양이 그대로");
			helper.assertFalse(helper.getBlockState(other).getValue(BigBoilerCasingBlock.FORMED), "남은 외벽 무늬가 켜져 있음");
		});
	}

	// ---- 증기 기관차 · 화물 열차 ----

	/** 석탄을 넣고 출발하면 곧은 레일을 따라 달린다 (화로 수레보다 빠르다: 최고 속도 0.34 > 0.2). */
	@GameTest(maxTicks = 60)
	public void locomotiveWithFuelMovesAlongRail(GameTestHelper helper) {
		straightRail(helper);
		SteamLocomotive locomotive = helper.spawn(ModTransport.STEAM_LOCOMOTIVE, new Vec3(1.5, 1.0, 2.5));
		locomotive.addFuel(new ItemStack(Items.COAL, 2));
		locomotive.start(new Vec3(1.0, 0.0, 0.0));
		helper.assertTrue(SteamLocomotive.MAX_SPEED > 0.2, "기관차가 화로 수레보다 느림");
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.relativeVec(locomotive.position()).x >= 5.5, "기관차가 가지 않음: " + helper.relativeVec(locomotive.position()));
			helper.assertTrue(locomotive.burnTicks() < SteamLocomotive.TICKS_PER_COAL, "연료가 타지 않음");
			helper.assertValueEqual(locomotive.fuelCount(), 1, "남은 석탄");
		});
	}

	/** 증기 통의 증기로도 달린다 (연료가 없을 때). */
	@GameTest(maxTicks = 60)
	public void locomotiveRunsOnSteam(GameTestHelper helper) {
		straightRail(helper);
		SteamLocomotive locomotive = helper.spawn(ModTransport.STEAM_LOCOMOTIVE, new Vec3(1.5, 1.0, 2.5));
		helper.assertValueEqual(locomotive.addSteam(10_000), SteamLocomotive.STEAM_CAPACITY, "받은 증기");
		locomotive.start(new Vec3(1.0, 0.0, 0.0));
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.relativeVec(locomotive.position()).x >= 5.5, "증기로 가지 않음");
			helper.assertTrue(locomotive.steam() < SteamLocomotive.STEAM_CAPACITY, "증기를 쓰지 않음");
		});
	}

	/** 연결 고리로 이은 화물칸이 간격을 두고 따라온다. 이은 칸을 다시 누르면 떨어진다. */
	@GameTest(maxTicks = 100)
	public void coupledWagonFollowsLocomotive(GameTestHelper helper) {
		straightRail(helper);
		ServerLevel level = helper.getLevel();
		SteamLocomotive locomotive = helper.spawn(ModTransport.STEAM_LOCOMOTIVE, new Vec3(3.5, 1.0, 2.5));
		CargoWagon wagon = helper.spawn(ModTransport.CARGO_WAGON, new Vec3(1.9, 1.0, 2.5));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		helper.assertTrue(TrainCoupling.toggle(level, player, wagon) == locomotive, "화물칸이 기관차에 이어지지 않음");
		helper.assertValueEqual(locomotive.cars().size(), 1, "연결된 칸 수");
		TrainCoupling.toggle(level, player, wagon);
		helper.assertValueEqual(locomotive.cars().size(), 0, "다시 누르면 떨어져야 함");
		TrainCoupling.toggle(level, player, wagon);
		helper.assertValueEqual(locomotive.cars().size(), 1, "다시 이은 칸 수");

		locomotive.addFuel(new ItemStack(Items.COAL));
		locomotive.start(new Vec3(1.0, 0.0, 0.0));
		helper.succeedWhen(() -> {
			double wagonX = helper.relativeVec(wagon.position()).x;
			helper.assertTrue(wagonX >= 3.6, "화물칸이 따라오지 않음: " + wagonX);
			double gap = locomotive.distanceTo(wagon);
			helper.assertTrue(gap > 1.1 && gap < 2.6, "간격이 이상함: " + gap);
		});
	}

	/** 꺼진 동력 레일(정거장)에서 서서 기다리고, 레일에 신호가 들어오면 다시 간다. */
	@GameTest(maxTicks = 200)
	public void locomotiveWaitsAtUnpoweredRail(GameTestHelper helper) {
		straightRail(helper);
		helper.setBlock(new BlockPos(5, 1, 2), Blocks.POWERED_RAIL);
		SteamLocomotive locomotive = helper.spawn(ModTransport.STEAM_LOCOMOTIVE, new Vec3(1.5, 1.0, 2.5));
		locomotive.addFuel(new ItemStack(Items.COAL));
		locomotive.start(new Vec3(1.0, 0.0, 0.0));
		helper.startSequence()
				.thenWaitUntil(() -> {
					double x = helper.relativeVec(locomotive.position()).x;
					helper.assertTrue(x >= 5.0 && x < 6.0 && locomotive.getDeltaMovement().horizontalDistance() < 0.01, "정거장에 서지 않음: " + x);
					helper.assertTrue(locomotive.waitingAtStation(), "기다리는 상태가 아님");
				})
				.thenIdle(10)
				.thenExecute(() -> helper.assertTrue(helper.relativeVec(locomotive.position()).x < 6.0, "기다리지 않고 지나감"))
				.thenExecute(() -> helper.setBlock(new BlockPos(5, 1, 3), Blocks.REDSTONE_BLOCK))
				.thenWaitUntil(() -> helper.assertTrue(helper.relativeVec(locomotive.position()).x >= 6.5, "신호가 들어왔는데 출발하지 않음"))
				.thenSucceed();
	}

	/** 적재 모드: 역의 아이템이 옆 레일에 선 화물칸으로. 하역 모드: 다시 역으로. */
	@GameTest(maxTicks = 100)
	public void stationLoadsAndUnloadsWagon(GameTestHelper helper) {
		BlockPos stationPos = new BlockPos(2, 1, 2);
		helper.setBlock(new BlockPos(3, 0, 2), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 1, 2), Blocks.RAIL);
		helper.setBlock(stationPos, ModTransport.CARGO_STATION);
		CargoStationBlockEntity station = helper.getBlockEntity(stationPos, CargoStationBlockEntity.class);
		station.setItem(0, new ItemStack(Items.IRON_INGOT, 20));
		CargoWagon wagon = helper.spawn(ModTransport.CARGO_WAGON, new Vec3(3.5, 1.0, 2.5));
		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertValueEqual(wagon.countItem(Items.IRON_INGOT), 20, "화물칸에 실린 철 주괴");
					helper.assertValueEqual(station.itemCount(), 0, "역에 남은 아이템");
				})
				.thenExecute(() -> helper.setBlock(stationPos, helper.getBlockState(stationPos).setValue(CargoStationBlock.UNLOAD, true)))
				.thenWaitUntil(() -> {
					helper.assertValueEqual(station.countItem(Items.IRON_INGOT), 20, "역으로 내린 철 주괴");
					helper.assertTrue(wagon.isEmpty(), "화물칸이 비지 않음");
				})
				.thenSucceed();
	}

	// ---- 레시피 · 실테스트 ----

	@GameTest
	public void m8aRecipesLoad(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : new String[] {"big_boiler_casing", "big_boiler_controller", "steam_locomotive", "cargo_wagon", "coupler", "cargo_station"}) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id));
			helper.assertTrue(recipes.byKey(key).isPresent(), "레시피가 없음: " + id);
		}
		helper.succeed();
	}

	@GameTest
	public void playtestIndustryKit(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.INDUSTRY);
		var inventory = player.getInventory();
		helper.assertTrue(inventory.countItem(ModIndustry.BIG_BOILER_CASING.asItem()) >= 33, "외벽이 33개 안 됨");
		helper.assertTrue(inventory.countItem(ModIndustry.BIG_BOILER_CONTROLLER.asItem()) >= 1, "제어기가 없음");
		helper.assertTrue(inventory.countItem(ModTransport.STEAM_LOCOMOTIVE_ITEM) >= 1, "기관차가 없음");
		helper.assertTrue(inventory.countItem(ModTransport.CARGO_WAGON_ITEM) >= 1, "화물칸이 없음");
		helper.assertTrue(inventory.countItem(ModTransport.COUPLER) >= 1, "연결 고리가 없음");
		helper.assertTrue(inventory.countItem(ModTransport.CARGO_STATION.asItem()) >= 1, "역이 없음");
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 제어기 (북쪽을 봄) + 외벽 33칸을 쌓는다. 모양 확인은 하지 않은 채로 돌려준다. */
	static BigBoilerControllerBlockEntity buildBigBoiler(GameTestHelper helper) {
		helper.setBlock(CONTROLLER, ModIndustry.BIG_BOILER_CONTROLLER.defaultBlockState().setValue(BoilerBlock.FACING, FACING));
		for (BlockPos casing : BigBoilerControllerBlockEntity.SHAPE.casings(CONTROLLER, FACING)) {
			helper.setBlock(casing, ModIndustry.BIG_BOILER_CASING);
		}
		return helper.getBlockEntity(CONTROLLER, BigBoilerControllerBlockEntity.class);
	}

	/** z 2 줄에 x 0..7 곧은 레일 (밑에 돌). */
	private static void straightRail(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 1, 2), Blocks.RAIL);
		}
	}
}
