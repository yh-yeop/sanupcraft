package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.GuardianAltarBlock;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.effect.ModEffects;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.logistics.BeltBlock;
import io.github.yhyeop.sanupcraft.logistics.BeltMotorBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;
import io.github.yhyeop.sanupcraft.power.ModPower;

/** M2 순환: 증기 동력 → 컨베이어 자동화 → 내열 장비 → 용광로 파수꾼 → 3단계 부품. */
public class SanupcraftM2GameTest {
	// ---- 증기 ----

	@GameTest(maxTicks = 100)
	public void boilerMakesSteam(GameTestHelper helper) {
		BoilerBlockEntity boiler = boilerWithWater(helper, new BlockPos(1, 1, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(boiler.isLit(), "보일러가 타지 않음");
			helper.assertTrue(boiler.steam() > 0, "증기가 없음");
		});
	}

	@GameTest(maxTicks = 100)
	public void boilerFillsFromWaterSource(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModPower.BOILER);
		helper.setBlock(pos.above(), Blocks.WATER);
		BoilerBlockEntity boiler = helper.getBlockEntity(pos, BoilerBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(boiler.water() >= 1000, "물이 차지 않음: " + boiler.water()));
	}

	@GameTest
	public void pipeConnectsToBoilerAndMachine(GameTestHelper helper) {
		// 파이프를 먼저 두고 양옆에 보일러 · 분쇄기를 놓으면 이웃 갱신으로 모양이 맞춰진다
		helper.setBlock(new BlockPos(2, 1, 1), ModPower.STEAM_PIPE);
		helper.setBlock(new BlockPos(1, 1, 1), ModPower.BOILER);
		helper.setBlock(new BlockPos(3, 1, 1), ModMachines.CRUSHER);
		helper.runAfterDelay(2, () -> {
			BlockState pipe = helper.getBlockState(new BlockPos(2, 1, 1));
			helper.assertTrue(pipe.getValue(PipeBlock.WEST) && pipe.getValue(PipeBlock.EAST), "파이프가 양쪽에 안 이어짐: " + pipe);
			helper.assertFalse(pipe.getValue(PipeBlock.UP), "빈 쪽으로 이어짐");
			helper.succeed();
		});
	}

	/** 보일러 → 파이프 2칸 → 분쇄기: 연료 칸의 석탄을 안 쓰고 증기로 돈다. */
	@GameTest(maxTicks = 300)
	public void steamRunsCrusherWithoutBurningFuel(GameTestHelper helper) {
		boilerWithWater(helper, new BlockPos(1, 1, 1));
		helper.setBlock(new BlockPos(2, 1, 1), ModPower.STEAM_PIPE);
		helper.setBlock(new BlockPos(3, 1, 1), ModPower.STEAM_PIPE);
		helper.setBlock(new BlockPos(4, 1, 1), ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(new BlockPos(4, 1, 1), MachineBlockEntity.class);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON));
		crusher.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			helper.assertValueEqual(crusher.result().getCount(), 2, "철가루");
			helper.assertValueEqual(crusher.getItem(MachineBlockEntity.SLOT_FUEL).getCount(), 1, "남은 석탄 (증기로 돌았으면 그대로)");
		});
	}

	// ---- 컨베이어 ----

	/** 보일러 → 파이프 → 모터 → 벨트 3칸(남쪽) → 상자. 벨트에 올린 원석이 상자에 들어간다. */
	@GameTest(maxTicks = 300)
	public void poweredBeltCarriesItemIntoChest(GameTestHelper helper) {
		ChestBlockEntity chest = beltLine(helper, true);
		helper.succeedWhen(() -> helper.assertValueEqual(chest.countItem(Items.RAW_IRON), 3, "상자에 든 원석"));
	}

	@GameTest(maxTicks = 100)
	public void beltWithoutMotorStaysStill(GameTestHelper helper) {
		ChestBlockEntity chest = beltLine(helper, false);
		helper.runAfterDelay(80, () -> {
			helper.assertValueEqual(chest.countItem(Items.RAW_IRON), 0, "모터 없이 상자에 들어감");
			helper.assertItemEntityPresent(Items.RAW_IRON);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void motorPowersTheWholeLine(GameTestHelper helper) {
		beltLine(helper, true);
		BeltMotorBlockEntity motor = helper.getBlockEntity(new BlockPos(3, 1, 1), BeltMotorBlockEntity.class);
		helper.succeedWhen(() -> {
			helper.assertValueEqual(motor.poweredBelts(), 3, "모터가 돌리는 벨트 수");
			helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 4)).getValue(BeltBlock.POWERED), "끝 벨트가 안 돎");
		});
	}

	/** 깔때기가 벨트에 아이템을 올린다. */
	@GameTest(maxTicks = 100)
	public void hopperPutsItemsOnBelt(GameTestHelper helper) {
		BlockPos belt = new BlockPos(2, 1, 2);
		helper.setBlock(belt, ModLogistics.BELT.defaultBlockState().setValue(BeltBlock.FACING, Direction.EAST));
		BlockPos hopper = belt.above();
		helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 2));
		helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.COAL, belt, 1.0));
	}

	// ---- 2단계 재료 · 장비 ----

	@GameTest(maxTicks = 300)
	public void crusherSplitsCoreIntoFourShards(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.GUARDIAN_CORE));
		crusher.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			helper.assertTrue(crusher.result().is(ModItems.CORE_SHARD), "핵 조각이 아님: " + crusher.result());
			helper.assertValueEqual(crusher.result().getCount(), 4, "핵 조각 수");
		});
	}

	@GameTest
	public void recipesLoad(GameTestHelper helper) {
		String[] ids = {
				"boiler", "steam_pipe", "conveyor_belt", "conveyor_belt_from_kelp", "belt_motor",
				"crushing/guardian_core",
				"heatproof_helmet", "heatproof_chestplate", "heatproof_leggings", "heatproof_boots", "heatproof_sword",
				"warden_summoner",
		};
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : ids) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id));
			helper.assertTrue(recipes.byKey(key).isPresent(), "레시피가 없음: " + id);
		}
		helper.succeed();
	}

	/**
	 * 과열: 내열 한 벌이 없으면 1.5배 아프고 과열 효과가 걸린다 (모의 플레이어는 갑옷 방어력이 안 붙는다).
	 * 확장 3 C3에서 불 붙음을 과열 효과로 옮겼다 (같은 피해량, 물에 들어가면 풀림).
	 */
	@GameTest
	public void heatHitsHarderWithoutHeatproof(GameTestHelper helper) {
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
		Player suited = helper.makeMockPlayer(GameType.SURVIVAL);
		wearHeatproof(suited);

		warden.hitWithHeat(helper.getLevel(), bare, 8.0F);
		warden.hitWithHeat(helper.getLevel(), suited, 8.0F);
		float bareLoss = bare.getMaxHealth() - bare.getHealth();
		float suitedLoss = suited.getMaxHealth() - suited.getHealth();

		helper.assertTrue(bareLoss >= suitedLoss * FurnaceWarden.HEAT_MULTIPLIER - 0.01F, "맨몸 피해 " + bareLoss + ", 내열 피해 " + suitedLoss);
		helper.assertTrue(bare.hasEffect(ModEffects.OVERHEAT), "맨몸인데 과열이 안 걸림");
		helper.assertFalse(suited.hasEffect(ModEffects.OVERHEAT), "내열 한 벌인데 과열이 걸림");
		helper.succeed();
	}

	@GameTest
	public void heatproofSwordHurtsWardenMore(GameTestHelper helper) {
		FurnaceWarden a = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(1, 1, 1));
		FurnaceWarden b = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(5, 1, 5));
		Player withHeatproof = helper.makeMockPlayer(GameType.SURVIVAL);
		withHeatproof.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HeatproofGear.HEATPROOF_SWORD));
		Player withDiamond = helper.makeMockPlayer(GameType.SURVIVAL);
		withDiamond.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

		a.hurtServer(helper.getLevel(), a.damageSources().playerAttack(withHeatproof), 12.0F);
		b.hurtServer(helper.getLevel(), b.damageSources().playerAttack(withDiamond), 12.0F);
		float heatproofDamage = a.getMaxHealth() - a.getHealth();
		float diamondDamage = b.getMaxHealth() - b.getHealth();
		helper.assertTrue(heatproofDamage > diamondDamage * 1.3F, "내열 검 " + heatproofDamage + ", 다이아 검 " + diamondDamage);
		helper.succeed();
	}

	// ---- 보스 2 ----

	@GameTest
	public void wardenHasBossHealthAndIgnoresFire(GameTestHelper helper) {
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		helper.assertValueEqual(warden.getMaxHealth(), (float) FurnaceWarden.MAX_HEALTH, "최대 체력");
		helper.assertFalse(warden.hurtServer(helper.getLevel(), warden.damageSources().inFire(), 10.0F), "불 피해를 받음");
		helper.assertFalse(warden.hurtServer(helper.getLevel(), warden.damageSources().fall(), 10.0F), "낙하 피해를 받음");
		helper.succeed();
	}

	/** 파수꾼은 네더에서만 깨어난다 (M3). 오버월드 제단에서는 부르지 못하고, 수호자는 그대로 부를 수 있다. */
	@GameTest(environment = "sanupcraft-test:alone_warden")
	public void wardenWontWakeInOverworld(GameTestHelper helper) {
		BlockPos altar = new BlockPos(3, 1, 3);
		helper.setBlock(altar, ModBoss.GUARDIAN_ALTAR);
		helper.assertFalse(GuardianAltarBlock.canWakeHere(helper.getLevel(), ModBoss.FURNACE_WARDEN), "오버월드에서 깨어날 수 있다고 함");
		helper.assertTrue(GuardianAltarBlock.summon(helper.getLevel(), helper.absolutePos(altar), ModBoss.FURNACE_WARDEN) == null, "오버월드에서 파수꾼을 부름");
		helper.assertEntityNotPresent(ModBoss.FURNACE_WARDEN);
		helper.assertTrue(GuardianAltarBlock.summon(helper.getLevel(), helper.absolutePos(altar)) != null, "수호자를 부르지 못함");
		helper.succeed();
	}

	/** 파수꾼을 잡으면 파수꾼의 심장 → 3단계 재료가 나온다. */
	@GameTest
	public void wardenDropsHeart(GameTestHelper helper) {
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		warden.kill(helper.getLevel());
		helper.succeedWhen(() -> helper.assertItemEntityPresent(ModItems.WARDEN_HEART));
	}

	// ---- 도우미 ----

	private static BoilerBlockEntity boilerWithWater(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModPower.BOILER);
		BoilerBlockEntity boiler = helper.getBlockEntity(pos, BoilerBlockEntity.class);
		for (int i = 0; i < BoilerBlockEntity.WATER_CAPACITY / BoilerBlockEntity.BUCKET; i++) {
			boiler.pourBucket();
		}
		boiler.setItem(0, new ItemStack(Items.COAL, 4));
		return boiler;
	}

	/**
	 * (1,1,1) 보일러 → (2,1,1) 파이프 → (3,1,1) 모터 → (3,1,2~4) 남쪽 벨트 → (3,1,5) 상자.
	 * 첫 벨트에 원석 3개를 올려 둔다. withMotor가 false면 보일러 · 파이프 · 모터 없이 벨트와 상자만.
	 */
	private static ChestBlockEntity beltLine(GameTestHelper helper, boolean withMotor) {
		if (withMotor) {
			boilerWithWater(helper, new BlockPos(1, 1, 1));
			helper.setBlock(new BlockPos(2, 1, 1), ModPower.STEAM_PIPE);
			helper.setBlock(new BlockPos(3, 1, 1), ModLogistics.BELT_MOTOR);
		}
		for (int z = 2; z <= 4; z++) {
			helper.setBlock(new BlockPos(3, 1, z), ModLogistics.BELT.defaultBlockState().setValue(BeltBlock.FACING, Direction.SOUTH));
		}
		helper.setBlock(new BlockPos(3, 1, 5), Blocks.CHEST);
		BlockPos start = helper.absolutePos(new BlockPos(3, 1, 2));
		ItemEntity item = new ItemEntity(helper.getLevel(), start.getX() + 0.5, start.getY() + BeltBlock.HEIGHT + 0.05, start.getZ() + 0.5,
				new ItemStack(Items.RAW_IRON, 3), 0.0, 0.0, 0.0);
		helper.getLevel().addFreshEntity(item);
		return helper.getBlockEntity(new BlockPos(3, 1, 5), ChestBlockEntity.class);
	}

	private static void wearHeatproof(Player player) {
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(HeatproofGear.HEATPROOF_HELMET));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HeatproofGear.HEATPROOF_CHESTPLATE));
		player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(HeatproofGear.HEATPROOF_LEGGINGS));
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(HeatproofGear.HEATPROOF_BOOTS));
	}
}
