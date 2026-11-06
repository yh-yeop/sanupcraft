package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.effect.ModEffects;
import io.github.yhyeop.sanupcraft.end.VoidGear;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.mob.EliteAbility;
import io.github.yhyeop.sanupcraft.mob.EliteMonster;
import io.github.yhyeop.sanupcraft.mob.EmberBolt;
import io.github.yhyeop.sanupcraft.mob.EmberWisp;
import io.github.yhyeop.sanupcraft.mob.ModMobs;
import io.github.yhyeop.sanupcraft.mob.RustAutomaton;
import io.github.yhyeop.sanupcraft.mob.VoidShade;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.world.ModWorld;

/** 확장 3 C2 새 몹 셋 + 정예: 타격 효과 (막는 장비), 불씨 과열, 눈덩이, 등 뒤 순간이동, 정예 체력 · 저장 · 반사 · 발전 과제, 스폰 규칙, 실테스트 단계. */
public class SanupcraftC2MobGameTest {
	// ---- 녹슨 자동인형 ----

	/** 자동인형이 때리면 녹 I (설정 시간), 강철 한 벌은 안 걸린다. */
	@GameTest
	public void automatonHitAppliesRust(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RustAutomaton automaton = helper.spawn(ModMobs.RUST_AUTOMATON, new BlockPos(2, 1, 2));
		automaton.setNoAi(true);
		helper.assertValueEqual(automaton.getAttributeValue(Attributes.ATTACK_DAMAGE), 4.0, "자동인형 공격력");
		Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
		Player steel = helper.makeMockPlayer(GameType.SURVIVAL);
		wear(steel, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		helper.assertTrue(automaton.doHurtTarget(level, bare), "맨몸을 못 때림");
		automaton.doHurtTarget(level, steel);
		MobEffectInstance rust = bare.getEffect(ModEffects.RUST);
		helper.assertTrue(rust != null, "맞았는데 녹이 안 걸림");
		helper.assertValueEqual(rust.getAmplifier(), RustAutomaton.RUST_LEVEL - 1, "녹 단계");
		helper.assertValueEqual(rust.getDuration(), RustAutomaton.RUST_TICKS, "녹 시간");
		helper.assertTrue(bare.getHealth() < bare.getMaxHealth(), "맨몸이 피해를 안 받음");
		helper.assertFalse(steel.hasEffect(ModEffects.RUST), "강철 한 벌인데 녹이 걸림");
		helper.succeed();
	}

	// ---- 잿불 정령 ----

	/** 불씨에 맞으면 과열 I (설정 시간), 불은 붙지 않는다. 내열 한 벌은 맞아도 과열이 없다. */
	@GameTest(maxTicks = 100)
	public void emberBoltAppliesOverheat(GameTestHelper helper) {
		EmberWisp wisp = helper.spawn(ModMobs.EMBER_WISP, new BlockPos(1, 4, 1));
		wisp.setNoAi(true);
		wisp.setNoGravity(true);
		IronGolem bare = dummy(helper, new BlockPos(5, 1, 1));
		IronGolem suited = dummy(helper, new BlockPos(1, 1, 5));
		wear(suited, HeatproofGear.HEATPROOF_HELMET, HeatproofGear.HEATPROOF_CHESTPLATE, HeatproofGear.HEATPROOF_LEGGINGS, HeatproofGear.HEATPROOF_BOOTS);
		shoot(helper, wisp, bare);
		shoot(helper, wisp, suited);
		helper.succeedWhen(() -> {
			MobEffectInstance heat = bare.getEffect(ModEffects.OVERHEAT);
			helper.assertTrue(heat != null, "불씨에 맞았는데 과열이 없음");
			helper.assertValueEqual(heat.getAmplifier(), EmberWisp.OVERHEAT_LEVEL - 1, "과열 단계");
			helper.assertTrue(heat.getDuration() > EmberWisp.OVERHEAT_TICKS - 60, "과열 시간이 짧음: " + heat.getDuration());
			helper.assertFalse(bare.isOnFire(), "불씨가 불을 붙임");
			helper.assertTrue(suited.getHealth() < suited.getMaxHealth(), "내열 골렘이 아직 안 맞음");
			helper.assertFalse(suited.hasEffect(ModEffects.OVERHEAT), "내열 한 벌인데 과열이 걸림");
		});
	}

	/** 정령은 불에 안 타고, 눈덩이는 피해 4를 준다 (바닐라 눈덩이는 블레이즈가 아니면 0). 물에 약하다. */
	@GameTest
	public void wispWeakToSnowballs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EmberWisp wisp = helper.spawn(ModMobs.EMBER_WISP, new BlockPos(2, 1, 2));
		wisp.setNoAi(true);
		helper.assertTrue(wisp.fireImmune(), "정령이 불에 탐");
		helper.assertTrue(wisp.isSensitiveToWater(), "정령이 물에 약하지 않음");
		float max = wisp.getHealth();
		Snowball snowball = new Snowball(level, wisp.getX(), wisp.getY() + 3.0, wisp.getZ(), new ItemStack(Items.SNOWBALL));
		wisp.hurtServer(level, wisp.damageSources().thrown(snowball, null), 0.0F);
		helper.assertValueEqual(wisp.getHealth(), max - EmberWisp.SNOWBALL_DAMAGE, "눈덩이 피해");
		helper.succeed();
	}

	// ---- 공허 그림자 ----

	/** 그림자가 때리면 공허 침식 I (설정 시간), 공허 한 벌은 안 걸린다. */
	@GameTest
	public void shadeHitAppliesErosion(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VoidShade shade = helper.spawn(ModMobs.VOID_SHADE, new BlockPos(2, 1, 2));
		shade.setNoAi(true);
		Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
		Player suited = helper.makeMockPlayer(GameType.SURVIVAL);
		wear(suited, VoidGear.VOID_HELMET, VoidGear.VOID_CHESTPLATE, VoidGear.VOID_LEGGINGS, VoidGear.VOID_BOOTS);
		shade.doHurtTarget(level, bare);
		shade.doHurtTarget(level, suited);
		MobEffectInstance erosion = bare.getEffect(ModEffects.VOID_EROSION);
		helper.assertTrue(erosion != null, "맞았는데 공허 침식이 안 걸림");
		helper.assertValueEqual(erosion.getAmplifier(), VoidShade.EROSION_LEVEL - 1, "침식 단계");
		helper.assertValueEqual(erosion.getDuration(), VoidShade.EROSION_TICKS, "침식 시간");
		helper.assertValueEqual(bare.getMaxHealth(), 20.0F - 2.0F * VoidShade.EROSION_LEVEL, "침식 최대 체력");
		helper.assertFalse(suited.hasEffect(ModEffects.VOID_EROSION), "공허 한 벌인데 침식이 걸림");
		helper.succeed();
	}

	/** 그림자는 대상이 보는 방향의 등 뒤 (3칸 안) 로 순간이동하고 대상을 바라본다. */
	@GameTest
	public void shadeTeleportsBehindTarget(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		VoidShade shade = helper.spawn(ModMobs.VOID_SHADE, new BlockPos(1, 1, 6));
		shade.setNoAi(true);
		IronGolem target = dummy(helper, new BlockPos(4, 1, 4));
		target.setYRot(0.0F);
		target.setYHeadRot(0.0F);
		helper.assertTrue(shade.teleportBehind(target), "순간이동 실패");
		Vec3 offset = shade.position().subtract(target.position());
		double distance = Math.hypot(offset.x, offset.z);
		helper.assertTrue(distance <= 3.5 && distance >= 1.0, "등 뒤 거리가 이상함: " + distance);
		// 보는 방향 (yRot 0 = +Z) 의 반대쪽
		helper.assertTrue(offset.z < -0.5, "등 뒤가 아님: " + offset);
		helper.succeed();
	}

	// ---- 정예 ----

	/** 정예: 최대 체력 2배 (체력 가득), 능력 색 이름, 저장했다 읽어도 그대로 (체력을 다시 채우지 않는다). */
	@GameTest
	public void eliteDoublesHealthAndSurvivesSave(GameTestHelper helper) {
		RustAutomaton automaton = helper.spawn(ModMobs.RUST_AUTOMATON, new BlockPos(2, 1, 2));
		automaton.setNoAi(true);
		float base = automaton.getMaxHealth();
		helper.assertFalse(automaton.isElite(), "새 자동인형이 정예");
		automaton.makeElite(EliteAbility.REGEN);
		helper.assertTrue(automaton.isElite(), "정예가 안 됨");
		helper.assertValueEqual(automaton.getMaxHealth(), base * 2.0F, "정예 최대 체력");
		helper.assertValueEqual(automaton.getHealth(), base * 2.0F, "정예 체력이 가득이 아님");
		helper.assertTrue(automaton.getCustomName() != null && automaton.getCustomName().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("entity.sanupcraft.elite"), "정예 이름이 없음: " + automaton.getCustomName());
		automaton.setHealth(base * 1.5F);

		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		automaton.saveWithoutId(output);
		var tag = output.buildResult();
		helper.assertTrue(tag.getStringOr(EliteMonster.SAVE_KEY, "").equals("regen"), "저장에 정예 능력이 없음: " + tag);

		RustAutomaton loaded = helper.spawn(ModMobs.RUST_AUTOMATON, new BlockPos(5, 1, 5));
		loaded.setNoAi(true);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
		helper.assertTrue(loaded.isElite(), "읽은 자동인형이 정예가 아님");
		helper.assertValueEqual(loaded.eliteAbility(), EliteAbility.REGEN, "읽은 능력");
		helper.assertValueEqual(loaded.getMaxHealth(), base * 2.0F, "읽은 최대 체력 (수정자가 두 번 붙으면 안 됨)");
		helper.assertValueEqual(loaded.getHealth(), base * 1.5F, "읽은 체력");
		helper.succeed();
	}

	/** 정예는 자연 스폰 때만 굴린다: 확률 1이면 자연 스폰은 정예, 명령 · 생성 알은 보통. 가속은 이동 속도 +30%. */
	@GameTest
	public void eliteRollsOnlyOnNaturalSpawn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SanupcraftConfig config = SanupcraftConfig.current();
		double saved = config.mobEliteChance;
		try {
			config.mobEliteChance = 1.0;
			VoidShade natural = helper.spawn(ModMobs.VOID_SHADE, new BlockPos(2, 1, 2));
			natural.setNoAi(true);
			natural.finalizeSpawn(level, level.getCurrentDifficultyAt(natural.blockPosition()), EntitySpawnReason.NATURAL, null);
			helper.assertTrue(natural.isElite(), "확률 1인데 자연 스폰이 정예가 아님");
			VoidShade egg = helper.spawn(ModMobs.VOID_SHADE, new BlockPos(5, 1, 5));
			egg.setNoAi(true);
			egg.finalizeSpawn(level, level.getCurrentDifficultyAt(egg.blockPosition()), EntitySpawnReason.SPAWN_ITEM_USE, null);
			helper.assertFalse(egg.isElite(), "생성 알로 나온 그림자가 정예");
		} finally {
			config.mobEliteChance = saved;
		}
		EmberWisp fast = helper.spawn(ModMobs.EMBER_WISP, new BlockPos(3, 1, 5));
		fast.setNoAi(true);
		double speed = fast.getAttributeValue(Attributes.MOVEMENT_SPEED);
		fast.makeElite(EliteAbility.SPEED);
		helper.assertTrue(Math.abs(fast.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * (1.0 + EliteAbility.SPEED_BONUS)) < 1.0E-6, "가속 정예 속도");
		helper.succeed();
	}

	/** 반사 정예를 때리면 때린 쪽이 피해의 30%를 돌려받는다. 정예를 잡은 플레이어는 발전 과제 '정예 사냥꾼'. */
	@GameTest
	public void eliteReflectsAndAwardsAdvancement(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VoidShade shade = helper.spawn(ModMobs.VOID_SHADE, new BlockPos(2, 1, 2));
		shade.setNoAi(true);
		shade.makeElite(EliteAbility.REFLECT);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 4.5)));
		IronGolem attacker = dummy(helper, new BlockPos(2, 1, 5));
		float before = attacker.getHealth();
		shade.hurtServer(level, shade.damageSources().mobAttack(attacker), 10.0F);
		helper.assertValueEqual(attacker.getHealth(), before - 10.0F * EliteAbility.REFLECT_SHARE, "반사 피해");

		RustAutomaton elite = helper.spawn(ModMobs.RUST_AUTOMATON, new BlockPos(5, 1, 5));
		elite.setNoAi(true);
		elite.makeElite(EliteAbility.SPEED);
		elite.hurtServer(level, elite.damageSources().playerAttack(player), 1000.0F);
		helper.assertFalse(elite.isAlive(), "정예가 안 쓰러짐");
		var advancement = level.getServer().getAdvancements().get(Sanupcraft.id(EliteMonster.ADVANCEMENT));
		helper.assertTrue(advancement != null, "발전 과제 '정예 사냥꾼'이 없음");
		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "정예를 잡았는데 발전 과제를 못 받음");
		shade.discard();
		helper.succeed();
	}

	// ---- 스폰 규칙 ----

	/** 바이옴 스폰 (평원 · 동굴 = 자동인형, 현무암 삼각주 · 황무지 = 정령, 엔드 고지 = 그림자), 자리 규칙, 녹슨 사당 몹 목록. */
	@GameTest
	public void spawnRulesRegistered(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		assertSpawns(helper, biomes.getValueOrThrow(Biomes.PLAINS), ModMobs.RUST_AUTOMATON, "평원");
		assertSpawns(helper, biomes.getValueOrThrow(Biomes.DRIPSTONE_CAVES), ModMobs.RUST_AUTOMATON, "점적석 동굴");
		assertSpawns(helper, biomes.getValueOrThrow(Biomes.BASALT_DELTAS), ModMobs.EMBER_WISP, "현무암 삼각주");
		assertSpawns(helper, biomes.getValueOrThrow(Biomes.NETHER_WASTES), ModMobs.EMBER_WISP, "네더 황무지");
		assertSpawns(helper, biomes.getValueOrThrow(Biomes.END_HIGHLANDS), ModMobs.VOID_SHADE, "엔드 고지");
		helper.assertFalse(spawns(biomes.getValueOrThrow(Biomes.THE_END), ModMobs.VOID_SHADE), "중앙 섬에도 그림자가 나옴");
		helper.assertFalse(spawns(biomes.getValueOrThrow(Biomes.DESERT), ModMobs.RUST_AUTOMATON), "사막에도 자동인형이 나옴");
		for (EntityType<?> type : new EntityType<?>[] {ModMobs.RUST_AUTOMATON, ModMobs.EMBER_WISP, ModMobs.VOID_SHADE}) {
			helper.assertTrue(SpawnPlacements.getPlacementType(type) == SpawnPlacementTypes.ON_GROUND, "자리 규칙이 없음: " + type);
		}
		var shrine = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ModWorld.RUSTED_SHRINE);
		StructureSpawnOverride override = shrine.spawnOverrides().get(MobCategory.MONSTER);
		helper.assertTrue(override != null && override.spawns().unwrap().stream().anyMatch(w -> w.value().type() == ModMobs.RUST_AUTOMATON),
				"녹슨 사당 몹 목록에 자동인형이 없음");
		helper.succeed();
	}

	/** 실테스트 'mobs': 생성 알 셋 + 정예 알 (엔티티 데이터에 정예 능력) + 막는 장비 · 눈덩이. */
	@GameTest
	public void playtestMobsKit(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.MOBS);
		var inventory = player.getInventory();
		helper.assertTrue(inventory.countItem(ModMobs.RUST_AUTOMATON_SPAWN_EGG) >= 5, "자동인형 알이 모자람");
		helper.assertTrue(inventory.countItem(ModMobs.EMBER_WISP_SPAWN_EGG) >= 5, "정령 알이 모자람");
		helper.assertTrue(inventory.countItem(ModMobs.VOID_SHADE_SPAWN_EGG) >= 5, "그림자 알이 모자람");
		helper.assertTrue(inventory.countItem(Items.SNOWBALL) > 0, "눈덩이가 없음");
		helper.assertTrue(inventory.countItem(VoidGear.VOID_CHESTPLATE) > 0, "공허 장비가 없음");
		boolean eliteEgg = false;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			var data = inventory.getItem(i).get(DataComponents.ENTITY_DATA);
			if (data != null && data.contains(EliteMonster.SAVE_KEY)) {
				eliteEgg = true;
			}
		}
		helper.assertTrue(eliteEgg, "정예 알이 없음");
		helper.succeed();
	}

	// ---- 도우미 ----

	private static void shoot(GameTestHelper helper, EmberWisp wisp, LivingEntity target) {
		Vec3 from = wisp.position().add(0.0, 0.6, 0.0);
		Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
		EmberBolt bolt = new EmberBolt(helper.getLevel(), wisp, to.subtract(from).normalize());
		bolt.setPos(from.x, from.y, from.z);
		helper.getLevel().addFreshEntity(bolt);
	}

	private static void assertSpawns(GameTestHelper helper, Biome biome, EntityType<?> type, String name) {
		helper.assertTrue(spawns(biome, type), name + " 몹 목록에 " + type + "이 없음");
	}

	private static boolean spawns(Biome biome, EntityType<?> type) {
		return biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY)
				.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream()
				.map(w -> w.value()).anyMatch((MobSpawnSettings.SpawnerData data) -> data.type() == type);
	}

	private static IronGolem dummy(GameTestHelper helper, BlockPos pos) {
		IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, pos);
		golem.setNoAi(true);
		return golem;
	}

	private static void wear(LivingEntity entity, Item head, Item chest, Item legs, Item feet) {
		entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
		entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
		entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
		entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
	}
}
