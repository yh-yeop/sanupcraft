package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.nether.BarterCollectorBlockEntity;
import io.github.yhyeop.sanupcraft.nether.DebrisSonarItem;
import io.github.yhyeop.sanupcraft.nether.GoldDispenserBlockEntity;
import io.github.yhyeop.sanupcraft.nether.LavaSteamboat;
import io.github.yhyeop.sanupcraft.nether.ModNether;
import io.github.yhyeop.sanupcraft.nether.NetherFloorStructure;
import io.github.yhyeop.sanupcraft.power.GeothermalVentBlockEntity;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.power.SoulFurnaceBlockEntity;
import io.github.yhyeop.sanupcraft.power.SteamTurbineBlockEntity;

/** M5 네더 심화 2: 영혼 화로 · 고압 증기망 · 정밀 조립기 · 터빈, 금 분배기 · 교역 수집판, 잔해 탐지기, 용암 증기선, 버려진 용광로. */
public class SanupcraftM5GameTest {
	// ---- 영혼 화로 · 고압 증기 ----

	/** 영혼 흙 위의 화로만 고압 증기를 만든다 (틱당 4). 돌 위는 "영혼 땅이 아니다". */
	@GameTest
	public void soulFurnaceRunsOnlyOnSoulSoil(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.SOUL_SOIL);
		helper.setBlock(new BlockPos(4, 1, 1), Blocks.STONE);
		SoulFurnaceBlockEntity onSoul = furnace(helper, new BlockPos(1, 2, 1));
		SoulFurnaceBlockEntity onStone = furnace(helper, new BlockPos(4, 2, 1));
		for (int i = 0; i < 10; i++) {
			onSoul.work(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)));
			onStone.work(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 1)));
		}
		helper.assertValueEqual(onSoul.steam(), 10 * SoulFurnaceBlockEntity.STEAM_PER_TICK, "영혼 흙 위 화로의 고압 증기");
		helper.assertValueEqual(onSoul.fuelCount(), 3, "가루 하나만 탔어야 함");
		helper.assertValueEqual(onStone.steam(), 0, "돌 위 화로가 증기를 만듦");
		helper.assertValueEqual(onStone.fuelCount(), 4, "돌 위 화로가 가루를 태움");
		helper.assertTrue(onStone.status() == SoulFurnaceBlockEntity.Status.NO_SOUL_GROUND, "돌 위 상태: " + onStone.status());
		// 가루 하나 = 400 → 100틱 뒤 다음 가루
		for (int i = 0; i < 95; i++) {
			onSoul.work(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)));
		}
		helper.assertValueEqual(onSoul.steam(), 420, "가루 하나 반의 증기");
		helper.assertValueEqual(onSoul.fuelCount(), 2, "두 번째 가루");
		helper.succeed();
	}

	/** 고압 증기는 보통 기계로 가지 않고, 보통 증기는 고압 기계로 가지 않는다. 파이프도 서로 잇지 않는다. */
	@GameTest
	public void highPressureAndNormalSteamDoNotMix(GameTestHelper helper) {
		// 영혼 화로 → 고압 파이프 → 압착기 (보통 기계)
		// 파이프를 먼저 놓아야 이웃이 놓일 때 파이프 모양이 갱신된다 (setBlock은 놓는 블록의 처음 모양을 계산하지 않음)
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.SOUL_SOIL);
		helper.setBlock(new BlockPos(2, 2, 1), ModNether.HIGH_PRESSURE_PIPE);
		SoulFurnaceBlockEntity furnace = furnace(helper, new BlockPos(1, 2, 1));
		helper.setBlock(new BlockPos(3, 2, 1), ModMachines.PRESS);
		MachineBlockEntity press = helper.getBlockEntity(new BlockPos(3, 2, 1), MachineBlockEntity.class);
		// 지열 분출구 → 증기 파이프 → 정밀 조립기 (고압 기계)
		helper.setBlock(new BlockPos(0, 2, 4), Blocks.LAVA);
		helper.setBlock(new BlockPos(2, 2, 4), ModPower.STEAM_PIPE);
		helper.setBlock(new BlockPos(1, 2, 4), ModPower.GEOTHERMAL_VENT);
		GeothermalVentBlockEntity vent = helper.getBlockEntity(new BlockPos(1, 2, 4), GeothermalVentBlockEntity.class);
		helper.setBlock(new BlockPos(3, 2, 4), ModMachines.PRECISION_ASSEMBLER);
		MachineBlockEntity precision = helper.getBlockEntity(new BlockPos(3, 2, 4), MachineBlockEntity.class);

		int made = 0;
		for (int i = 0; i < 30; i++) {
			furnace.work(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)));
			made += vent.work(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 4)), true);
		}
		helper.assertTrue(made > 0, "분출구가 증기를 안 만듦");
		helper.assertValueEqual(press.steam(), 0, "고압 증기가 보통 기계로 감");
		helper.assertValueEqual(precision.steam(), 0, "보통 증기가 고압 기계로 감");
		helper.assertValueEqual(press.acceptHighPressureSteam(50), 0, "보통 기계가 고압 증기를 받음");
		helper.assertValueEqual(precision.acceptSteam(50), 0, "고압 기계가 보통 증기를 받음");
		helper.assertFalse(helper.getBlockState(new BlockPos(2, 2, 1)).getValue(PipeBlock.EAST), "고압 파이프가 압착기 쪽으로 이어짐");
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 1)).getValue(PipeBlock.WEST), "고압 파이프가 화로 쪽으로 안 이어짐");
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 4)).getValue(PipeBlock.WEST), "증기 파이프가 분출구 쪽으로 안 이어짐");
		helper.assertFalse(helper.getBlockState(new BlockPos(2, 2, 4)).getValue(PipeBlock.EAST), "증기 파이프가 정밀 조립기 쪽으로 이어짐");
		helper.assertFalse(precision.canPlaceItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL)), "정밀 조립기 연료 칸에 석탄이 들어감");
		helper.succeed();
	}

	/** 영혼 화로 → 고압 파이프 → 정밀 조립기: 강철판 + 금박 석영 → 정밀 부품 (300틱). */
	@GameTest(maxTicks = 600)
	public void precisionAssemblerCraftsWithHighPressureSteam(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.SOUL_SAND);
		furnace(helper, new BlockPos(1, 2, 1));
		helper.setBlock(new BlockPos(2, 2, 1), ModNether.HIGH_PRESSURE_PIPE);
		helper.setBlock(new BlockPos(3, 2, 1), ModNether.HIGH_PRESSURE_PIPE);
		helper.setBlock(new BlockPos(4, 2, 1), ModMachines.PRECISION_ASSEMBLER);
		MachineBlockEntity precision = helper.getBlockEntity(new BlockPos(4, 2, 1), MachineBlockEntity.class);
		precision.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.STEEL_PLATE));
		precision.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModNether.GILDED_QUARTZ));
		helper.succeedWhen(() -> {
			helper.assertTrue(precision.result().is(ModNether.PRECISION_PART), "정밀 부품이 아직 없음 (증기 " + precision.steam() + ")");
			helper.assertTrue(precision.getItem(MachineBlockEntity.SLOT_INPUT).isEmpty(), "강철판이 안 줄음");
		});
	}

	/** 정밀 조립기에도 기계 업그레이드가 듣는다: 속도 하나면 300틱 → 200틱. */
	@GameTest
	public void precisionAssemblerTakesUpgrades(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.PRECISION_ASSEMBLER);
		MachineBlockEntity precision = helper.getBlockEntity(pos, MachineBlockEntity.class);
		precision.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.STEEL_PLATE));
		precision.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModNether.GILDED_QUARTZ));
		precision.setItem(MachineBlockEntity.SLOT_UPGRADE, new ItemStack(ModMachines.SPEED_UPGRADE));
		BlockPos abs = helper.absolutePos(pos);
		for (int tick = 1; tick <= 400; tick++) {
			precision.acceptHighPressureSteam(MachineBlockEntity.STEAM_CAPACITY);
			MachineBlockEntity.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), precision);
			if (!precision.result().isEmpty()) {
				helper.assertValueEqual(tick, 200, "속도 업그레이드를 낀 정밀 조립기의 틱");
				helper.succeed();
				return;
			}
		}
		throw helper.assertionException(Component.literal("400틱 안에 정밀 부품이 안 나옴"));
	}

	/** 터빈: 고압 10 → 보통 30을 보통 증기 파이프로 압착기에. */
	@GameTest
	public void turbineConvertsOneToThree(GameTestHelper helper) {
		BlockPos turbinePos = new BlockPos(1, 1, 1);
		helper.setBlock(new BlockPos(2, 1, 1), ModPower.STEAM_PIPE);
		helper.setBlock(turbinePos, ModNether.STEAM_TURBINE);
		helper.setBlock(new BlockPos(3, 1, 1), ModMachines.PRESS);
		SteamTurbineBlockEntity turbine = helper.getBlockEntity(turbinePos, SteamTurbineBlockEntity.class);
		MachineBlockEntity press = helper.getBlockEntity(new BlockPos(3, 1, 1), MachineBlockEntity.class);
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 1)).getValue(PipeBlock.WEST), "증기 파이프가 터빈에 안 이어짐");
		helper.assertValueEqual(turbine.acceptHighPressureSteam(10), 10, "터빈이 고압 증기를 안 받음");
		helper.assertValueEqual(turbine.work(helper.getLevel(), helper.absolutePos(turbinePos)), 8, "한 틱에 바꾼 고압 증기 (상한 8)");
		turbine.work(helper.getLevel(), helper.absolutePos(turbinePos));
		helper.assertValueEqual(turbine.highPressure(), 0, "남은 고압 증기");
		helper.assertValueEqual(press.steam(), 30, "압착기가 받은 보통 증기");
		helper.succeed();
	}

	// ---- 금 분배기 · 교역 수집판 ----

	/** 아기 피글린에게는 던지지 않고, 어른 피글린 쪽으로 금괴 하나를 던진다. */
	@GameTest
	public void goldDispenserThrowsToAdultPiglin(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 3);
		helper.setBlock(pos, ModNether.GOLD_DISPENSER);
		GoldDispenserBlockEntity dispenser = helper.getBlockEntity(pos, GoldDispenserBlockEntity.class);
		dispenser.setItem(0, new ItemStack(Items.GOLD_INGOT, 5));
		Piglin baby = helper.spawnWithNoFreeWill(EntityTypes.PIGLIN, new BlockPos(4, 1, 3));
		baby.setBaby(true);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(pos);
		helper.assertTrue(dispenser.throwGold(level, abs) == null, "아기 피글린에게 던짐");

		Piglin adult = helper.spawnWithNoFreeWill(EntityTypes.PIGLIN, new BlockPos(5, 1, 3));
		ItemEntity thrown = dispenser.throwGold(level, abs);
		helper.assertTrue(thrown != null, "어른 피글린에게 안 던짐");
		helper.assertTrue(thrown.getItem().is(Items.GOLD_INGOT), "던진 것이 금괴가 아님");
		Vec3 toPiglin = adult.position().subtract(thrown.position());
		Vec3 motion = thrown.getDeltaMovement();
		helper.assertTrue(motion.x * toPiglin.x + motion.z * toPiglin.z > 0, "피글린 쪽으로 안 날아감: " + motion);
		helper.assertTrue(thrown.entityTags().contains(GoldDispenserBlockEntity.THROWN_TAG), "던진 금괴에 표시가 없음");
		helper.assertValueEqual(dispenser.goldCount(), 4, "남은 금괴");
		helper.succeed();
	}

	/** 수집판: 반경 3 안 아이템을 빨아들이고, 분배기가 막 던진 금괴와 먼 아이템은 남긴다. 깔때기로 뺀다. */
	@GameTest(maxTicks = 100)
	public void barterCollectorVacuumsItems(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, ModNether.BARTER_COLLECTOR);
		helper.setBlock(pos.below(), Blocks.HOPPER);
		helper.setBlock(pos.below(2), Blocks.CHEST);
		BarterCollectorBlockEntity collector = helper.getBlockEntity(pos, BarterCollectorBlockEntity.class);
		ServerLevel level = helper.getLevel();
		ItemEntity near = drop(helper, new Vec3(5.5, 3.0, 4.5), new ItemStack(Items.QUARTZ, 5));
		ItemEntity gold = drop(helper, new Vec3(2.5, 3.0, 3.5), new ItemStack(Items.GOLD_INGOT));
		gold.addTag(GoldDispenserBlockEntity.THROWN_TAG);
		ItemEntity far = drop(helper, new Vec3(3.5, 3.0, 7.9), new ItemStack(Items.FLINT));
		far.setPos(helper.absoluteVec(new Vec3(3.5, 3.0, 11.5)));

		int taken = collector.sweep(level, helper.absolutePos(pos));
		helper.assertValueEqual(taken, 5, "빨아들인 개수");
		helper.assertTrue(near.isRemoved(), "가까운 아이템이 남음");
		helper.assertTrue(gold.isAlive(), "막 던진 금괴를 빨아들임");
		helper.assertTrue(far.isAlive(), "먼 아이템을 빨아들임");
		helper.assertFalse(collector.canPlaceItemThroughFace(0, new ItemStack(Items.STONE), null), "깔때기가 수집판에 넣을 수 있음");
		helper.succeedWhen(() -> helper.assertContainerContains(pos.below(2), Items.QUARTZ));
	}

	// ---- 잔해 탐지기 ----

	/** 증기가 모자라면 안 쓰이고, 있으면 20을 쓰고 가장 가까운 고대 잔해를 찾는다. */
	@GameTest
	public void debrisSonarFindsDebrisAndUsesSteam(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
		helper.setBlock(new BlockPos(6, 1, 1), Blocks.ANCIENT_DEBRIS);
		helper.setBlock(new BlockPos(7, 6, 7), Blocks.ANCIENT_DEBRIS);
		ServerLevel level = helper.getLevel();

		ItemStack empty = new ItemStack(ModNether.DEBRIS_SONAR);
		SteamCharge.set(empty, 10);
		helper.assertTrue(DebrisSonarItem.ping(level, player, empty) == DebrisSonarItem.Result.NO_STEAM, "증기 10으로 쓰임");
		helper.assertValueEqual(SteamCharge.get(empty), 10, "실패했는데 증기가 빠짐");

		ItemStack sonar = new ItemStack(ModNether.DEBRIS_SONAR);
		SteamCharge.set(sonar, SteamCharge.capacity(sonar));
		helper.assertValueEqual(SteamCharge.capacity(sonar), DebrisSonarItem.CAPACITY, "탐지기 저장량");
		helper.assertTrue(DebrisSonarItem.ping(level, player, sonar) == DebrisSonarItem.Result.FOUND, "잔해를 못 찾음");
		helper.assertValueEqual(SteamCharge.get(sonar), DebrisSonarItem.CAPACITY - DebrisSonarItem.STEAM_COST, "쓴 증기");
		helper.assertTrue(DebrisSonarItem.ping(level, player, sonar) == DebrisSonarItem.Result.COOLDOWN, "쿨다운 중에 쓰임");
		BlockPos nearest = DebrisSonarItem.findNearest(level, player.blockPosition(), DebrisSonarItem.RADIUS);
		helper.assertValueEqual(nearest, helper.absolutePos(new BlockPos(6, 1, 1)), "가장 가까운 잔해");
		helper.assertValueEqual(DebrisSonarItem.direction(5, 0), "e", "동쪽");
		helper.assertValueEqual(DebrisSonarItem.direction(0, -5), "n", "북쪽");
		helper.assertValueEqual(DebrisSonarItem.direction(-4, 4), "sw", "남서쪽");
		helper.succeed();
	}

	// ---- 용암 증기선 ----

	/** 용암 웅덩이에 떨군 증기선이 가라앉지 않고 용암 위에 떠 있고, 타지 않는다. 탄 돼지는 화염 저항을 받는다. */
	@GameTest(maxTicks = 200)
	public void lavaSteamboatFloatsOnLava(GameTestHelper helper) {
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 6; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				for (int y = 1; y <= 3; y++) {
					boolean wall = x == 0 || x == 6 || z == 0 || z == 6;
					helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE : Blocks.LAVA);
				}
			}
		}
		LavaSteamboat boat = ModNether.LAVA_STEAMBOAT_ENTITY.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
		Vec3 start = helper.absoluteVec(new Vec3(3.5, 4.5, 3.5));
		boat.setInitialPos(start.x, start.y, start.z);
		helper.getLevel().addFreshEntity(boat);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 5, 3));
		pig.startRiding(boat);
		double surface = helper.absoluteVec(new Vec3(0, 3, 0)).y + 8.0F / 9.0F;
		helper.assertTrue(boat.fireImmune(), "증기선이 불 면역이 아님");
		double[] earlier = new double[1];
		helper.runAfterDelay(90, () -> earlier[0] = boat.getY());
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(boat.isAlive(), "증기선이 부서짐");
			helper.assertTrue(boat.getY() > surface - 0.3 && boat.getY() < surface,
					"용암 위에 얕게 안 떠 있음: y=" + boat.getY() + " 용암 윗면=" + surface);
			helper.assertTrue(Math.abs(boat.getY() - earlier[0]) < 0.02, "아직 출렁임: " + earlier[0] + " → " + boat.getY());
			helper.assertFalse(boat.isOnFire(), "증기선이 탐");
			helper.assertTrue(pig.isPassenger(), "돼지가 내림");
			helper.assertTrue(pig.hasEffect(MobEffects.FIRE_RESISTANCE), "탄 돼지에게 화염 저항이 없음");
			helper.succeed();
		});
	}

	// ---- 버려진 용광로 ----

	/** 구조물 · 구조물 묶음 · 구조물 종류가 등록돼 있다. */
	@GameTest
	public void abandonedFurnaceIsRegistered(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		var structure = access.lookupOrThrow(Registries.STRUCTURE).getValue(ModNether.ABANDONED_FURNACE);
		helper.assertTrue(structure instanceof NetherFloorStructure, "버려진 용광로 구조물이 없거나 종류가 다름: " + structure);
		helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE_SET)
				.get(ResourceKey.create(Registries.STRUCTURE_SET, Sanupcraft.id("abandoned_furnaces"))).isPresent(), "구조물 묶음이 등록 안 됨");
		// 실제로 조각이 든 구조물이 생기는지 (시험 서버의 네더는 평지라 바닥은 늘 찾는다). 직소 깊이 0이면 조각이 비어 생기지 않았다
		ServerLevel nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
		var generator = nether.getChunkSource().getGenerator();
		var randomState = nether.getChunkSource().randomState();
		var holder = access.lookupOrThrow(Registries.STRUCTURE).getOrThrow(ModNether.ABANDONED_FURNACE);
		var start = structure.generate(holder, net.minecraft.world.level.Level.NETHER, access, generator, generator.getBiomeSource(),
				randomState.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),
				randomState, nether.getStructureTemplateManager(), nether.getSeed(), new net.minecraft.world.level.ChunkPos(3, 3), 0, nether, b -> true);
		helper.assertTrue(start.isValid(), "버려진 용광로 구조물이 비어 있음 (조각 없음)");
		helper.assertValueEqual(start.getPieces().size(), 1, "조각 수");
		helper.succeed();
	}

	/** 틀에 수호자 제단 · 부서진 심장 제련로 · 상자(버려진 용광로 전리품)가 하나씩 있다. */
	@GameTest
	public void abandonedFurnaceTemplateHasAltarForgeAndChest(GameTestHelper helper) {
		StructureTemplate template = helper.getLevel().getStructureTemplateManager().get(Sanupcraft.id("abandoned_furnace"))
				.orElseThrow(() -> helper.assertionException("abandoned_furnace.nbt를 못 읽음"));
		StructurePlaceSettings settings = new StructurePlaceSettings();
		helper.assertValueEqual(template.filterBlocks(BlockPos.ZERO, settings, ModBoss.GUARDIAN_ALTAR).size(), 1, "제단 수");
		List<StructureTemplate.StructureBlockInfo> forges = template.filterBlocks(BlockPos.ZERO, settings, ModMachines.HEART_FORGE);
		helper.assertValueEqual(forges.size(), 1, "심장 제련로 수");
		helper.assertFalse(forges.get(0).state().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT), "심장 제련로가 켜져 있음");
		List<StructureTemplate.StructureBlockInfo> chests = template.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST);
		helper.assertValueEqual(chests.size(), 1, "상자 수");
		String loot = chests.get(0).nbt() == null ? "" : chests.get(0).nbt().getString("LootTable").orElse("");
		helper.assertValueEqual(loot, ModNether.ABANDONED_FURNACE_CHEST.identifier().toString(), "상자 전리품 테이블");
		helper.succeed();
	}

	/** 상자 100번: 파수꾼 소환석이 대략 절반, 핵 조각 · 블레이즈 막대는 자주, 정밀 부품은 가끔. */
	@GameTest
	public void abandonedFurnaceChestLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(ModNether.ABANDONED_FURNACE_CHEST);
		int summoners = 0;
		int shards = 0;
		int rods = 0;
		int parts = 0;
		// 씨앗을 0, 1, 2 …로 매번 새로 만들면 첫 난수가 거의 같아 확률 검사가 안 된다 → 난수 하나를 이어 쓴다
		RandomSource random = RandomSource.create(20261107L);
		for (int i = 0; i < 100; i++) {
			LootParams params = new LootParams.Builder(level)
					.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
					.create(LootContextParamSets.CHEST);
			List<ItemStack> items = table.getRandomItems(params, random);
			summoners += count(items, ModBoss.WARDEN_SUMMONER);
			shards += count(items, ModItems.CORE_SHARD);
			rods += count(items, Items.BLAZE_ROD);
			parts += count(items, ModNether.PRECISION_PART);
		}
		helper.assertTrue(summoners >= 25 && summoners <= 75, "소환석: " + summoners);
		helper.assertTrue(shards > 0 && rods > 0 && parts > 0, "핵 조각 " + shards + " · 블레이즈 막대 " + rods + " · 정밀 부품 " + parts);
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 블레이즈 가루 4개를 넣은 영혼 화로. */
	private static SoulFurnaceBlockEntity furnace(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModNether.SOUL_FURNACE);
		SoulFurnaceBlockEntity furnace = helper.getBlockEntity(pos, SoulFurnaceBlockEntity.class);
		furnace.setItem(0, new ItemStack(Items.BLAZE_POWDER, 4));
		return furnace;
	}

	private static ItemEntity drop(GameTestHelper helper, Vec3 relative, ItemStack stack) {
		Vec3 at = helper.absoluteVec(relative);
		ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
		entity.setDeltaMovement(Vec3.ZERO);
		entity.setNoGravity(true);
		helper.getLevel().addFreshEntity(entity);
		return entity;
	}

	private static int count(List<ItemStack> items, Item item) {
		return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
