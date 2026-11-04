package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlock;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineMenu;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.power.BoilerBlock;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;
import io.github.yhyeop.sanupcraft.power.ModPower;

/** 1.5단계 리팩터링 수정 검사 (REFACTOR.md 2-5, 묶음 Factory): 깔때기 입력 거름 · 보일러 연료 아낌 · 양동이 반환 · 증기 나눔. */
public class SanupcraftFixFactoryGameTest {
	private static final BlockPos POS = new BlockPos(3, 1, 3);

	/** B1: 합금로 둘째 칸에 석탄 가루가 있어도, 위 깔때기의 석탄 가루 2 + 철가루 2가 교착 없이 강철이 된다. */
	@GameTest(maxTicks = 500)
	public void alloyHopperDoesNotDeadlock(GameTestHelper helper) {
		MachineBlockEntity smelter = machine(helper, POS, ModMachines.ALLOY_SMELTER);
		smelter.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModItems.COAL_DUST, 2));
		smelter.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.setBlock(POS.above(), Blocks.HOPPER);
		HopperBlockEntity hopper = helper.getBlockEntity(POS.above(), HopperBlockEntity.class);
		hopper.setItem(0, new ItemStack(ModItems.COAL_DUST, 2));
		hopper.setItem(1, new ItemStack(ModItems.IRON_DUST, 2));
		helper.succeedWhen(() -> helper.assertTrue(smelter.result().is(ModItems.STEEL_INGOT), "강철이 안 나옴: 재료 "
				+ smelter.getItem(MachineBlockEntity.SLOT_INPUT) + " / " + smelter.getItem(MachineBlockEntity.SLOT_INPUT2)));
	}

	/** B1: 합금로 재료 칸 규칙을 직접 확인한다 (같은 재료가 두 칸을 다 차지하지 않는다). */
	@GameTest
	public void alloyFacesRejectDuplicates(GameTestHelper helper) {
		MachineBlockEntity smelter = machine(helper, POS, ModMachines.ALLOY_SMELTER);
		ItemStack coal = new ItemStack(ModItems.COAL_DUST);
		ItemStack iron = new ItemStack(ModItems.IRON_DUST);
		// 둘 다 빔: 어느 재료든 첫 칸으로
		helper.assertTrue(smelter.canPlaceItemThroughFace(MachineBlockEntity.SLOT_INPUT, coal, Direction.UP), "빈 합금로 첫 칸");
		helper.assertFalse(smelter.canPlaceItemThroughFace(MachineBlockEntity.SLOT_INPUT2, coal, Direction.UP), "첫 칸이 빈데 둘째 칸으로");
		// 둘째 칸에 석탄 가루: 석탄 가루는 둘째 칸으로만, 철가루는 첫 칸으로
		smelter.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModItems.COAL_DUST));
		helper.assertFalse(smelter.canPlaceItemThroughFace(MachineBlockEntity.SLOT_INPUT, coal, Direction.UP), "같은 재료를 첫 칸에도");
		helper.assertTrue(smelter.canPlaceItemThroughFace(MachineBlockEntity.SLOT_INPUT2, coal, Direction.UP), "둘째 칸에 같은 재료 이어 넣기");
		helper.assertTrue(smelter.canPlaceItemThroughFace(MachineBlockEntity.SLOT_INPUT, iron, Direction.UP), "철가루는 첫 칸으로");
		// 이 기계의 재료가 아닌 것은 깔때기로 못 넣는다. 손으로 넣는 것(canPlaceItem)은 그대로.
		ItemStack dirt = new ItemStack(Items.DIRT);
		helper.assertFalse(smelter.canPlaceItemThroughFace(MachineBlockEntity.SLOT_INPUT, dirt, Direction.UP), "합금로에 흙");
		helper.assertTrue(smelter.canPlaceItem(MachineBlockEntity.SLOT_INPUT, dirt), "손으로 넣는 건 막지 않는다");
		helper.succeed();
	}

	/** B1(+A5): 분쇄기 위 깔때기에 흙 + 원석 철 → 원석 철만 들어가 철가루가 나오고, 흙은 깔때기에 남는다. */
	@GameTest(maxTicks = 500)
	public void crusherHopperFiltersJunk(GameTestHelper helper) {
		MachineBlockEntity crusher = machine(helper, POS, ModMachines.CRUSHER);
		crusher.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.setBlock(POS.above(), Blocks.HOPPER);
		HopperBlockEntity hopper = helper.getBlockEntity(POS.above(), HopperBlockEntity.class);
		hopper.setItem(0, new ItemStack(Items.DIRT, 4));
		hopper.setItem(1, new ItemStack(Items.RAW_IRON));
		helper.succeedWhen(() -> {
			helper.assertTrue(crusher.result().is(ModItems.IRON_DUST), "철가루가 안 나옴: " + crusher.result());
			helper.assertValueEqual(hopper.getItem(0).getCount(), 4, "깔때기에 남은 흙");
			helper.assertTrue(crusher.getItem(MachineBlockEntity.SLOT_INPUT).isEmpty(), "재료 칸에 흙이 들어감");
		});
	}

	/** B5: 증기가 가득 차 쓸 데가 없으면 불은 꺼진 모습이 되고 연료(litTime)는 거의 줄지 않는다. */
	@GameTest(maxTicks = 400)
	public void boilerSavesFuelWhenSteamFull(GameTestHelper helper) {
		BoilerBlockEntity boiler = boilerWithWater(helper, POS, BoilerBlockEntity.WATER_CAPACITY / BoilerBlockEntity.BUCKET);
		boiler.setItem(0, new ItemStack(Items.COAL));
		helper.runAfterDelay(320, () -> {
			helper.assertValueEqual(boiler.steam(), BoilerBlockEntity.STEAM_CAPACITY, "증기");
			// 증기 1000을 채우는 250틱만 탔어야 한다 (예전에는 320틱 내내 닳음 → 1280 이하)
			helper.assertTrue(boiler.litTime() > 1340, "연료가 줄어듦: litTime=" + boiler.litTime());
			helper.assertFalse(boiler.isLit(), "멈춘 채로 켜져 있음");
			helper.assertFalse(helper.getBlockState(POS).getValue(BoilerBlock.LIT), "블록 상태가 켜져 있음");
			helper.succeed();
		});
	}

	/** B7: 기계가 용암 양동이를 연료로 쓰면 연료 칸에 빈 양동이가 남고, 아래 면으로만 꺼낼 수 있다. */
	@GameTest(maxTicks = 100)
	public void machineLeavesEmptyBucketInFuelSlot(GameTestHelper helper) {
		MachineBlockEntity crusher = machine(helper, POS, ModMachines.CRUSHER);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON));
		crusher.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.LAVA_BUCKET));
		helper.succeedWhen(() -> {
			ItemStack fuel = crusher.getItem(MachineBlockEntity.SLOT_FUEL);
			helper.assertTrue(fuel.is(Items.BUCKET), "연료 칸: " + fuel);
			helper.assertTrue(crusher.canTakeItemThroughFace(MachineBlockEntity.SLOT_FUEL, fuel, Direction.DOWN), "빈 양동이를 아래로 못 꺼냄");
			helper.assertFalse(crusher.canTakeItemThroughFace(MachineBlockEntity.SLOT_FUEL, fuel, Direction.NORTH), "옆으로 꺼냄");
			helper.assertFalse(crusher.canTakeItemThroughFace(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL), Direction.DOWN), "연료를 아래로 꺼냄");
		});
	}

	/** B7: 보일러가 용암 양동이를 연료로 쓰면 빈 양동이가 위에 떨어진다. */
	@GameTest(maxTicks = 100)
	public void boilerDropsEmptyBucket(GameTestHelper helper) {
		BoilerBlockEntity boiler = boilerWithWater(helper, POS, 1);
		boiler.setItem(0, new ItemStack(Items.LAVA_BUCKET));
		helper.succeedWhen(() -> {
			helper.assertTrue(boiler.fuelCount() == 0, "연료가 안 닳음");
			helper.assertItemEntityPresent(Items.BUCKET, POS.above(), 2.0);
		});
	}

	/** B8: 증기가 모자라도(보일러 1 : 분쇄기 5) 모든 기계가 번갈아 받아 다섯 대 모두 결과를 낸다. */
	@GameTest(maxTicks = 400)
	public void scarceSteamIsSharedAmongAll(GameTestHelper helper) {
		boilerWithWater(helper, POS, BoilerBlockEntity.WATER_CAPACITY / BoilerBlockEntity.BUCKET).setItem(0, new ItemStack(Items.COAL, 2));
		BlockPos[] spots = {POS.west(), POS.east(), POS.north(), POS.south(), POS.above()};
		MachineBlockEntity[] crushers = new MachineBlockEntity[spots.length];
		for (int i = 0; i < spots.length; i++) {
			crushers[i] = machine(helper, spots[i], ModMachines.CRUSHER);
			crushers[i].setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.BONE));
		}
		helper.succeedWhen(() -> {
			for (int i = 0; i < crushers.length; i++) {
				helper.assertTrue(!crushers[i].result().isEmpty(), "기계 " + i + "가 굶음 (증기 " + crushers[i].steam() + ")");
			}
		});
	}

	/** C9: 화면이 보는 증기 값이 기계에 모인 증기와 같다. */
	@GameTest
	public void menuReportsSteam(GameTestHelper helper) {
		MachineBlockEntity crusher = machine(helper, POS, ModMachines.CRUSHER);
		crusher.acceptSteam(50);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		AbstractContainerMenu menu = crusher.createMenu(1, player.getInventory(), player);
		helper.assertTrue(menu instanceof MachineMenu, "기계 메뉴가 아님: " + menu);
		helper.assertValueEqual(((MachineMenu) menu).steam(), 50, "메뉴의 증기");
		helper.succeed();
	}

	/** B8: 증기가 띄엄띄엄 와서 일이 잠깐 끊겨도 LIT(불빛)이 바로 꺼지지 않는다. */
	@GameTest(maxTicks = 100)
	public void machineKeepsLitThroughShortPause(GameTestHelper helper) {
		MachineBlockEntity crusher = machine(helper, POS, ModMachines.CRUSHER);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.BONE));
		crusher.acceptSteam(3);
		helper.runAfterDelay(3, () -> helper.assertTrue(helper.getBlockState(POS).getValue(MachineBlock.LIT), "증기를 쓰는 중인데 꺼짐"));
		// 증기 3을 다 쓴 직후(약 4~6틱)에는 아직 켜져 있고, 한참 뒤에는 꺼진다
		helper.runAfterDelay(8, () -> helper.assertTrue(helper.getBlockState(POS).getValue(MachineBlock.LIT), "멈춘 직후 바로 꺼짐"));
		helper.runAfterDelay(40, () -> {
			helper.assertFalse(helper.getBlockState(POS).getValue(MachineBlock.LIT), "한참 멈췄는데 켜져 있음");
			helper.succeed();
		});
	}

	// ---- 도우미 ----

	private static MachineBlockEntity machine(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block);
		return helper.getBlockEntity(pos, MachineBlockEntity.class);
	}

	private static BoilerBlockEntity boilerWithWater(GameTestHelper helper, BlockPos pos, int buckets) {
		helper.setBlock(pos, ModPower.BOILER);
		BoilerBlockEntity boiler = helper.getBlockEntity(pos, BoilerBlockEntity.class);
		for (int i = 0; i < buckets; i++) {
			boiler.pourBucket();
		}
		return boiler;
	}
}
