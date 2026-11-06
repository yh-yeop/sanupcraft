package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineMenu;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.fluid.FluidContents;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.fluid.ModFluid;
import io.github.yhyeop.sanupcraft.fluid.PumpBlockEntity;
import io.github.yhyeop.sanupcraft.fluid.TankBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.BeltBlockEntity;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;
import io.github.yhyeop.sanupcraft.power.GeothermalVentBlockEntity;
import io.github.yhyeop.sanupcraft.power.ModPower;

/** 묶음 4 검사: 기계 업그레이드 (B1) · 펌프 · 탱크 (B2). */
public class SanupcraftBundle4GameTest {
	// ---- 기계 업그레이드 ----

	/** 압착기(철 주괴 → 철판, 100틱): 업그레이드 없이 100틱, 속도 하나 67틱, 둘 45틱. 한 개에 드는 증기는 같다 (약 100). */
	@GameTest
	public void speedUpgradeFinishesFaster(GameTestHelper helper) {
		int[] ticks = new int[3];
		int[] used = new int[3];
		for (int speed = 0; speed < 3; speed++) {
			BlockPos pos = new BlockPos(1 + speed * 2, 1, 2);
			MachineBlockEntity press = press(helper, pos);
			for (int i = 0; i < speed; i++) {
				press.setItem(MachineBlockEntity.SLOT_UPGRADE + i, new ItemStack(ModMachines.SPEED_UPGRADE));
			}
			ticks[speed] = runUntilResult(helper, pos, press, 300);
			used[speed] = MachineBlockEntity.STEAM_CAPACITY - press.steam();
		}
		helper.assertValueEqual(ticks[0], 100, "업그레이드 없는 틱");
		helper.assertValueEqual(ticks[1], 67, "속도 하나의 틱");
		helper.assertValueEqual(ticks[2], 45, "속도 둘의 틱");
		for (int speed = 0; speed < 3; speed++) {
			helper.assertTrue(used[speed] >= 99 && used[speed] <= 102, "속도 " + speed + "개일 때 쓴 증기: " + used[speed]);
		}
		helper.succeed();
	}

	/** 효율: 증기 30% 절약 (100 → 70, 둘이면 49). 연료는 한 개가 1/0.7배 오래 타고, 속도는 1/1.5배 짧게 탄다. */
	@GameTest
	public void efficiencyUpgradeUsesLessSteamAndFuel(GameTestHelper helper) {
		int[] used = new int[3];
		for (int eff = 0; eff < 3; eff++) {
			BlockPos pos = new BlockPos(1 + eff * 2, 1, 2);
			MachineBlockEntity press = press(helper, pos);
			for (int i = 0; i < eff; i++) {
				press.setItem(MachineBlockEntity.SLOT_UPGRADE + i, new ItemStack(ModMachines.EFFICIENCY_UPGRADE));
			}
			helper.assertValueEqual(runUntilResult(helper, pos, press, 300), 100, "효율은 속도를 바꾸지 않음");
			used[eff] = MachineBlockEntity.STEAM_CAPACITY - press.steam();
		}
		helper.assertTrue(used[0] >= 99 && used[0] <= 101, "업그레이드 없이 쓴 증기: " + used[0]);
		helper.assertTrue(used[1] >= 69 && used[1] <= 71, "효율 하나로 쓴 증기: " + used[1]);
		helper.assertTrue(used[2] >= 48 && used[2] <= 50, "효율 둘로 쓴 증기: " + used[2]);

		// 연료: 석탄 1600틱 → 효율 2286틱, 속도 1067틱
		BlockPos fuelPos = new BlockPos(1, 1, 4);
		helper.setBlock(fuelPos, ModMachines.PRESS);
		MachineBlockEntity fueled = helper.getBlockEntity(fuelPos, MachineBlockEntity.class);
		fueled.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 4));
		fueled.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		fueled.setItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(ModMachines.EFFICIENCY_UPGRADE));
		tick(helper, fuelPos, fueled);
		helper.assertValueEqual(fueled.litDuration(), 2286, "효율로 늘어난 석탄 연소 틱");

		BlockPos fastPos = new BlockPos(3, 1, 4);
		helper.setBlock(fastPos, ModMachines.PRESS);
		MachineBlockEntity fast = helper.getBlockEntity(fastPos, MachineBlockEntity.class);
		fast.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 4));
		fast.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		fast.setItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(ModMachines.SPEED_UPGRADE));
		tick(helper, fastPos, fast);
		helper.assertValueEqual(fast.litDuration(), 1067, "속도로 줄어든 석탄 연소 틱");
		helper.succeed();
	}

	/** 두 배: 굴림은 하나에 25% (고정 씨앗으로 4000번, 느슨한 범위). 확률을 1로 두면 결과가 업그레이드 수만큼 더 나오고, 결과 칸이 차면 덤은 버린다. */
	@GameTest
	public void doubleUpgradeYieldsExtra(GameTestHelper helper) {
		RandomSource random = RandomSource.create(20261106L);
		int single = 0;
		int pair = 0;
		for (int i = 0; i < 4000; i++) {
			single += MachineBlockEntity.extraResults(random, 1, 0.25);
			int two = MachineBlockEntity.extraResults(random, 2, 0.25);
			helper.assertTrue(two <= 2, "둘로 덤이 2개 넘게: " + two);
			pair += two;
		}
		helper.assertTrue(single > 850 && single < 1150, "하나의 덤 수 (기대 1000): " + single);
		helper.assertTrue(pair > 1800 && pair < 2200, "둘의 덤 수 (기대 2000): " + pair);
		helper.assertValueEqual(MachineBlockEntity.extraResults(random, 0, 1.0), 0, "업그레이드 없으면 덤 없음");

		// 확률 1: 이 틱 안에서 끝내고 바로 되돌린다 (다른 시험에 새지 않게)
		try {
			SanupcraftConfig config = SanupcraftConfig.defaults();
			config.upgradeDoubleChance = 1.0;
			SanupcraftConfig.use(config);
			BlockPos pos = new BlockPos(1, 1, 2);
			MachineBlockEntity press = press(helper, pos);
			press.setItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(ModMachines.DOUBLE_UPGRADE));
			press.setItem(MachineBlockEntity.SLOT_UPGRADE + 1, new ItemStack(ModMachines.DOUBLE_UPGRADE));
			runUntilResult(helper, pos, press, 300);
			helper.assertValueEqual(press.result().getCount(), 3, "두 배 둘 + 확률 1의 철판 수");

			BlockPos fullPos = new BlockPos(3, 1, 2);
			MachineBlockEntity full = press(helper, fullPos);
			full.setItem(MachineBlockEntity.SLOT_RESULT, new ItemStack(ModItems.IRON_PLATE, 63));
			full.setItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(ModMachines.DOUBLE_UPGRADE));
			for (int i = 0; i < 120 && full.result().getCount() < 64; i++) {
				tick(helper, fullPos, full);
			}
			helper.assertValueEqual(full.result().getCount(), 64, "결과 칸이 차면 덤은 넘치지 않음");
		} finally {
			SanupcraftConfig.resetToDefaults();
		}
		helper.succeed();
	}

	/** 깔때기 · 벨트는 업그레이드를 어느 면으로도 못 넣고 못 뺀다. 손(화면)으로는 넣고, Shift 클릭은 칸마다 하나씩. 비교기는 업그레이드를 세지 않는다. */
	@GameTest
	public void hoppersCannotTouchUpgrades(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		ItemStack speed = new ItemStack(ModMachines.SPEED_UPGRADE);
		for (Direction direction : Direction.values()) {
			ItemStack rest = HopperBlockEntity.addItem(null, crusher, speed.copy(), direction);
			helper.assertFalse(rest.isEmpty(), "깔때기가 업그레이드를 넣음: " + direction);
			for (int slot : crusher.getSlotsForFace(direction)) {
				helper.assertTrue(slot < MachineBlockEntity.SLOT_UPGRADE, "업그레이드 칸이 면에 열림: " + direction);
			}
			helper.assertFalse(crusher.canTakeItemThroughFace(MachineBlockEntity.SLOT_UPGRADE, speed, direction), "업그레이드를 뺄 수 있음: " + direction);
		}
		helper.assertFalse(BeltBlockEntity.insert(crusher, speed.copy(), Direction.NORTH).isEmpty(), "벨트가 업그레이드를 넣음");
		for (int slot = 0; slot < MachineBlockEntity.SLOT_COUNT; slot++) {
			helper.assertTrue(crusher.getItem(slot).isEmpty(), "어딘가에 들어감: 칸 " + slot);
		}
		helper.assertTrue(crusher.canPlaceItem(MachineBlockEntity.SLOT_UPGRADE, speed), "업그레이드 칸에 업그레이드");
		helper.assertFalse(crusher.canPlaceItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(Items.COAL)), "업그레이드 칸에 석탄");
		helper.assertFalse(crusher.canPlaceItemThroughFace(MachineBlockEntity.SLOT_UPGRADE, speed, null), "면 없이 업그레이드 칸에 넣음");

		// Shift 클릭: 업그레이드 3개 → 두 칸에 하나씩 (칸마다 최대 1), 하나는 남는다
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.getInventory().setItem(0, new ItemStack(ModMachines.SPEED_UPGRADE, 3));
		AbstractContainerMenu menu = crusher.createMenu(0, player.getInventory(), player);
		helper.assertTrue(menu instanceof MachineMenu, "기계 화면이 아님");
		int hotbar0 = MachineBlockEntity.SLOT_COUNT + 27;
		// 화면의 Shift 클릭은 같은 아이템이 남아 있는 동안 quickMoveStack을 되풀이한다 (한 번에 빈 칸 하나씩)
		menu.quickMoveStack(player, hotbar0);
		menu.quickMoveStack(player, hotbar0);
		helper.assertValueEqual(crusher.upgrade(0).getCount(), 1, "첫 업그레이드 칸");
		helper.assertValueEqual(crusher.upgrade(1).getCount(), 1, "둘째 업그레이드 칸");
		helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 1, "남은 업그레이드");
		helper.assertTrue(crusher.getItem(MachineBlockEntity.SLOT_INPUT).isEmpty(), "재료 칸으로 들어감");
		helper.assertValueEqual(crusher.comparatorSignal(), 0, "업그레이드만 든 기계의 비교기 신호");
		helper.succeed();
	}

	/** 저장 · 불러오기에 업그레이드가 남고, 옛 칸 번호(재료 · 연료 · 결과)도 그대로다. */
	@GameTest
	public void upgradesSurviveSaveAndLoad(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.ALLOY_SMELTER);
		MachineBlockEntity smelter = helper.getBlockEntity(pos, MachineBlockEntity.class);
		smelter.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 3));
		smelter.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(Items.COAL, 5));
		smelter.setItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(ModMachines.SPEED_UPGRADE));
		smelter.setItem(MachineBlockEntity.SLOT_UPGRADE + 1, new ItemStack(ModMachines.DOUBLE_UPGRADE));
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		BlockEntity copy = BlockEntity.loadStatic(smelter.getBlockPos(), smelter.getBlockState(), smelter.saveWithFullMetadata(registries), registries);
		helper.assertTrue(copy instanceof MachineBlockEntity, "불러온 것이 기계가 아님");
		MachineBlockEntity loaded = (MachineBlockEntity) copy;
		helper.assertTrue(loaded.upgrade(0).is(ModMachines.SPEED_UPGRADE), "첫 업그레이드: " + loaded.upgrade(0));
		helper.assertTrue(loaded.upgrade(1).is(ModMachines.DOUBLE_UPGRADE), "둘째 업그레이드: " + loaded.upgrade(1));
		helper.assertValueEqual(loaded.getItem(MachineBlockEntity.SLOT_INPUT).getCount(), 3, "재료 칸");
		helper.assertValueEqual(loaded.getItem(MachineBlockEntity.SLOT_INPUT2).getCount(), 5, "둘째 재료 칸");
		helper.assertValueEqual(loaded.upgradeEffects().doubles(), 1, "불러온 두 배 수");
		helper.succeed();
	}

	// ---- 펌프 · 탱크 ----

	/** 펌프 아래 물 원천을 빨아(블록이 사라짐) 옆 탱크로 보낸다. 증기가 모자라면 돌지 않는다. */
	@GameTest(maxTicks = 100)
	public void pumpDrainsWaterIntoTank(GameTestHelper helper) {
		BlockPos water = new BlockPos(2, 1, 2);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			helper.setBlock(water.relative(side), Blocks.STONE);
		}
		helper.setBlock(water, Blocks.WATER);
		BlockPos pumpPos = water.above();
		BlockPos tankPos = pumpPos.east();
		helper.setBlock(pumpPos, ModFluid.PUMP);
		helper.setBlock(tankPos, ModFluid.TANK);
		PumpBlockEntity pump = helper.getBlockEntity(pumpPos, PumpBlockEntity.class);
		TankBlockEntity tank = helper.getBlockEntity(tankPos, TankBlockEntity.class);
		ServerLevel level = helper.getLevel();
		BlockPos absPump = helper.absolutePos(pumpPos);

		helper.assertValueEqual(PumpBlockEntity.offsets(5).getFirst(), new BlockPos(0, -1, 0), "가장 먼저 보는 칸 = 바로 아래");
		helper.assertTrue(pump.findSource(level, absPump) != null, "아래 물을 못 찾음");
		helper.assertTrue(pump.status() == PumpBlockEntity.Status.NO_STEAM, "증기 없을 때 상태: " + pump.status());
		helper.assertFalse(pump.pumpOnce(level, absPump), "증기 없이 빨아들임");
		// 딱 한 번 몫의 증기만 준다 (이웃 시험의 물까지 빨지 않게)
		pump.acceptSteam(PumpBlockEntity.STEAM_PER_BUCKET);
		helper.assertTrue(pump.pumpOnce(level, absPump), "증기가 있는데 못 빨아들임");
		helper.assertFalse(level.getFluidState(helper.absolutePos(water)).isSource(), "원천이 남아 있음");
		helper.assertValueEqual(pump.fluidAmount(), FluidKind.BUCKET, "펌프 안의 물");
		helper.assertValueEqual(pump.steamStored(), 0, "쓴 증기");
		helper.assertValueEqual(pump.fill(FluidKind.WATER, 1000, false), 0, "펌프에 밖에서 넣음");
		helper.succeedWhen(() -> {
			helper.assertValueEqual(tank.fluidAmount(), FluidKind.BUCKET, "탱크로 간 물");
			helper.assertTrue(tank.fluid() == FluidKind.WATER, "탱크의 유체: " + tank.fluid());
			helper.assertValueEqual(pump.fluidAmount(), 0, "펌프에 남은 물");
		});
	}

	/** 탱크: 양동이로 붓고 퍼 내고, 다른 유체 · 가득 · 모자람은 거절. 부수면 내용이 아이템에 남고 다시 놓으면 그대로. */
	@GameTest
	public void tankBucketsAndKeepsContents(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModFluid.TANK);
		TankBlockEntity tank = helper.getBlockEntity(pos, TankBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos abs = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
		BlockState state = helper.getBlockState(pos);

		helper.assertValueEqual(use(helper, state, player, hit, Items.BUCKET), InteractionResult.FAIL, "빈 탱크에서 퍼 냄");
		helper.assertValueEqual(use(helper, state, player, hit, Items.WATER_BUCKET), InteractionResult.SUCCESS, "물 붓기");
		helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "빈 양동이를 안 돌려줌: " + player.getMainHandItem());
		helper.assertValueEqual(tank.fluidAmount(), 1000, "부은 양");
		helper.assertValueEqual(use(helper, state, player, hit, Items.LAVA_BUCKET), InteractionResult.FAIL, "물 탱크에 용암");
		helper.assertTrue(player.getMainHandItem().is(Items.LAVA_BUCKET), "실패했는데 용암을 가져감");
		helper.assertValueEqual(use(helper, state, player, hit, Items.BUCKET), InteractionResult.SUCCESS, "퍼 내기");
		helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "물 양동이를 안 돌려줌: " + player.getMainHandItem());
		helper.assertTrue(tank.fluid() == null && tank.fluidAmount() == 0, "다 퍼 냈는데 남음");
		helper.assertValueEqual(use(helper, state, player, hit, Items.LAVA_BUCKET), InteractionResult.SUCCESS, "빈 탱크에 용암");
		tank.set(FluidKind.LAVA, TankBlockEntity.CAPACITY);
		helper.assertValueEqual(use(helper, state, player, hit, Items.LAVA_BUCKET), InteractionResult.FAIL, "가득 찬 탱크에 붓기");
		helper.assertValueEqual(state.useWithoutItem(helper.getLevel(), player, hit), InteractionResult.SUCCESS, "빈손 우클릭");

		// 부수기: 전리품에 유체 컴포넌트, 다시 놓으면 그대로
		tank.set(FluidKind.LAVA, 7000);
		List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), abs, tank);
		helper.assertValueEqual(drops.size(), 1, "전리품 수");
		ItemStack dropped = drops.getFirst();
		FluidContents contents = dropped.get(ModFluid.FLUID);
		helper.assertTrue(contents != null && contents.kind() == FluidKind.LAVA && contents.amount() == 7000, "아이템에 남은 유체: " + contents);
		BlockPos againPos = new BlockPos(4, 1, 2);
		helper.setBlock(againPos, ModFluid.TANK);
		TankBlockEntity again = helper.getBlockEntity(againPos, TankBlockEntity.class);
		again.applyComponentsFromItemStack(dropped);
		helper.assertTrue(again.fluid() == FluidKind.LAVA && again.fluidAmount() == 7000, "다시 놓은 탱크: " + again.fluid() + " " + again.fluidAmount());
		tank.set(null, 0);
		helper.assertTrue(Block.getDrops(state, helper.getLevel(), abs, tank).getFirst().get(ModFluid.FLUID) == null, "빈 탱크 아이템에 컴포넌트");
		helper.succeed();
	}

	/** 지열 분출구는 용암이 1양동이 이상 든 옆 탱크를 용암 원천 하나로 센다 (용암은 줄지 않는다). */
	@GameTest
	public void ventCountsLavaTank(GameTestHelper helper) {
		BlockPos ventPos = new BlockPos(2, 1, 2);
		BlockPos tankPos = ventPos.east();
		helper.setBlock(ventPos, ModPower.GEOTHERMAL_VENT);
		helper.setBlock(tankPos, ModFluid.TANK);
		GeothermalVentBlockEntity vent = helper.getBlockEntity(ventPos, GeothermalVentBlockEntity.class);
		TankBlockEntity tank = helper.getBlockEntity(tankPos, TankBlockEntity.class);
		BlockPos abs = helper.absolutePos(ventPos);

		tank.set(FluidKind.LAVA, 999);
		vent.work(helper.getLevel(), abs, true);
		helper.assertValueEqual(vent.lava(), 0, "1양동이가 안 되는 탱크");
		tank.set(FluidKind.LAVA, 1000);
		int made = vent.work(helper.getLevel(), abs, true);
		helper.assertValueEqual(vent.lava(), 1, "용암 탱크를 원천으로");
		helper.assertValueEqual(made, GeothermalVentBlockEntity.STEAM_PER_LAVA, "만든 증기");
		helper.assertValueEqual(tank.fluidAmount(), 1000, "탱크 용암이 줄음");
		tank.set(FluidKind.WATER, TankBlockEntity.CAPACITY);
		vent.work(helper.getLevel(), abs, true);
		helper.assertValueEqual(vent.lava(), 0, "물 탱크를 용암으로 셈");
		helper.succeed();
	}

	/** 보일러: 옆 펌프가 물을 밀어 넣고, 옆 물 탱크에서는 틱당 100을 끌어온다. 용암은 안 받는다. */
	@GameTest
	public void boilerTakesWaterFromPumpAndTank(GameTestHelper helper) {
		BlockPos boilerPos = new BlockPos(2, 1, 2);
		BlockPos pumpPos = boilerPos.west();
		BlockPos tankPos = boilerPos.east();
		helper.setBlock(boilerPos, ModPower.BOILER);
		helper.setBlock(pumpPos, ModFluid.PUMP);
		helper.setBlock(tankPos, ModFluid.TANK);
		BoilerBlockEntity boiler = helper.getBlockEntity(boilerPos, BoilerBlockEntity.class);
		PumpBlockEntity pump = helper.getBlockEntity(pumpPos, PumpBlockEntity.class);
		TankBlockEntity tank = helper.getBlockEntity(tankPos, TankBlockEntity.class);
		ServerLevel level = helper.getLevel();

		pump.setFluid(FluidKind.WATER, 2000);
		helper.assertValueEqual(pump.push(level, helper.absolutePos(pumpPos)), PumpBlockEntity.PUSH_PER_TICK, "펌프가 보낸 물");
		helper.assertValueEqual(boiler.water(), PumpBlockEntity.PUSH_PER_TICK, "보일러가 받은 물");
		tank.set(FluidKind.WATER, 5000);
		helper.assertValueEqual(boiler.drawFromTanks(level, helper.absolutePos(boilerPos)), 100, "탱크에서 끌어온 물");
		helper.assertValueEqual(tank.fluidAmount(), 4900, "탱크에 남은 물");
		helper.assertValueEqual(boiler.fill(FluidKind.LAVA, 1000, false), 0, "보일러에 용암");
		helper.assertValueEqual(boiler.drain(FluidKind.WATER, 1000, false), 0, "보일러에서 물을 빼 감");
		tank.set(FluidKind.LAVA, 5000);
		helper.assertValueEqual(boiler.drawFromTanks(level, helper.absolutePos(boilerPos)), 0, "용암 탱크에서 끌어옴");
		pump.setFluid(FluidKind.LAVA, 2000);
		helper.assertValueEqual(pump.push(level, helper.absolutePos(pumpPos)), 0, "보일러에 용암을 보냄");
		helper.succeed();
	}

	/** 진짜 비교기가 탱크의 찬 정도를 읽는다 (비면 0, 조금이면 1, 반이면 8, 가득이면 15). */
	@GameTest(maxTicks = 100)
	public void tankDrivesComparator(GameTestHelper helper) {
		BlockPos tankPos = new BlockPos(2, 1, 2);
		BlockPos comparatorPos = tankPos.south();
		helper.setBlock(tankPos, ModFluid.TANK);
		helper.setBlock(comparatorPos.below(), Blocks.STONE);
		helper.setBlock(comparatorPos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
		TankBlockEntity tank = helper.getBlockEntity(tankPos, TankBlockEntity.class);
		helper.assertValueEqual(tank.signal(), 0, "빈 탱크 신호");
		tank.set(FluidKind.WATER, 1);
		helper.assertValueEqual(tank.signal(), 1, "조금 찬 탱크 신호");
		tank.set(FluidKind.WATER, TankBlockEntity.CAPACITY / 2);
		helper.assertValueEqual(tank.signal(), 8, "반 찬 탱크 신호");
		tank.set(FluidKind.WATER, TankBlockEntity.CAPACITY);
		helper.succeedWhen(() -> helper.assertValueEqual(
				helper.getBlockEntity(comparatorPos, ComparatorBlockEntity.class).getOutputSignal(), 15, "가득 찬 탱크의 비교기 출력"));
	}

	// ---- 도우미 ----

	/** 철 주괴 하나 · 증기 가득인 압착기. */
	private static MachineBlockEntity press(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModMachines.PRESS);
		MachineBlockEntity press = helper.getBlockEntity(pos, MachineBlockEntity.class);
		press.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT));
		press.acceptSteam(MachineBlockEntity.STEAM_CAPACITY);
		return press;
	}

	/** 이 틱 안에서 기계를 한 틱 돌린다. */
	private static void tick(GameTestHelper helper, BlockPos pos, MachineBlockEntity machine) {
		BlockPos abs = helper.absolutePos(pos);
		MachineBlockEntity.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), machine);
	}

	/** 결과가 나올 때까지 이 틱 안에서 돌리고, 걸린 틱 수를 돌려준다. */
	private static int runUntilResult(GameTestHelper helper, BlockPos pos, MachineBlockEntity machine, int max) {
		for (int i = 1; i <= max; i++) {
			tick(helper, pos, machine);
			if (!machine.result().isEmpty()) {
				return i;
			}
		}
		throw helper.assertionException(Component.literal("결과가 " + max + "틱 안에 안 나옴"));
	}

	private static InteractionResult use(GameTestHelper helper, BlockState state, Player player, BlockHitResult hit, net.minecraft.world.item.Item item) {
		ItemStack stack = new ItemStack(item);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return state.useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
	}
}
