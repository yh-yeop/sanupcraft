package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.effect.ModEffects;
import io.github.yhyeop.sanupcraft.effect.Resistances;
import io.github.yhyeop.sanupcraft.end.ModEnd;
import io.github.yhyeop.sanupcraft.end.VoidGear;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/** 확장 3 C3 상태 이상: 녹 · 과열 · 공허 침식의 속성 · 피해, 막는 장비, 물에 풀리는 과열, 보스 타격이 거는 효과. */
public class SanupcraftStatusEffectGameTest {
	// ---- 녹 ----

	/** 녹은 단계마다 방어 −2. 풀리면 돌아온다. */
	@GameTest
	public void rustLowersArmorPerLevel(GameTestHelper helper) {
		IronGolem golem = dummy(helper, new BlockPos(2, 1, 2));
		golem.getAttribute(Attributes.ARMOR).setBaseValue(10.0);
		helper.assertTrue(ModEffects.inflict(golem, ModEffects.RUST, 200, 2, null), "녹이 안 걸림");
		helper.assertValueEqual(golem.getAttributeValue(Attributes.ARMOR), 6.0, "녹 II 방어");
		golem.removeEffect(ModEffects.RUST);
		helper.assertValueEqual(golem.getAttributeValue(Attributes.ARMOR), 10.0, "녹이 풀린 뒤 방어");
		helper.assertTrue(ModEffects.inflict(golem, ModEffects.RUST, 200, 1, null), "녹 I이 안 걸림");
		helper.assertValueEqual(golem.getAttributeValue(Attributes.ARMOR), 8.0, "녹 I 방어");
		helper.succeed();
	}

	/** 강철 한 벌이면 녹 면역: 걸리지 않고, 억지로 건 녹도 다음 틱에 풀린다. 한 부위라도 빠지면 걸린다. */
	@GameTest
	public void steelSetIsRustImmune(GameTestHelper helper) {
		Player steel = helper.makeMockPlayer(GameType.SURVIVAL);
		wear(steel, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		helper.assertTrue(Resistances.rustImmune(steel), "강철 한 벌인데 녹 면역이 아님");
		helper.assertFalse(ModEffects.inflict(steel, ModEffects.RUST, 200, 2, null), "강철 한 벌인데 녹이 걸림");
		helper.assertFalse(steel.hasEffect(ModEffects.RUST), "강철 한 벌인데 녹이 남음");
		steel.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
		helper.assertFalse(Resistances.rustImmune(steel), "부츠가 철인데 녹 면역");
		helper.assertTrue(ModEffects.inflict(steel, ModEffects.RUST, 200, 2, null), "부츠가 철인데 녹이 안 걸림");

		IronGolem golem = dummy(helper, new BlockPos(3, 1, 3));
		golem.getAttribute(Attributes.ARMOR).setBaseValue(10.0);
		golem.addEffect(new MobEffectInstance(ModEffects.RUST, 200, 1));
		wear(golem, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(golem.hasEffect(ModEffects.RUST), "강철을 다 입었는데 녹이 안 풀림");
			helper.assertTrue(golem.getAttributeValue(Attributes.ARMOR) >= 10.0, "녹이 풀렸는데 방어가 안 돌아옴");
			helper.succeed();
		});
	}

	// ---- 과열 ----

	/** 과열 II, 45틱: 남은 시간이 40 · 20일 때 두 번, 단계만큼(2) 피해 = 4. 피해 종류는 sanupcraft:overheat (불 피해). */
	@GameTest(maxTicks = 100)
	public void overheatDamagesEverySecond(GameTestHelper helper) {
		IronGolem golem = dummy(helper, new BlockPos(2, 1, 2));
		float max = golem.getMaxHealth();
		helper.assertTrue(ModEffects.inflict(golem, ModEffects.OVERHEAT, 45, 2, null), "과열이 안 걸림");
		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(golem.getHealth(), max, "남은 시간이 40이 되기 전에 피해");
			helper.runAfterDelay(50, () -> {
				helper.assertFalse(golem.hasEffect(ModEffects.OVERHEAT), "시간이 지났는데 과열이 남음");
				helper.assertValueEqual(golem.getHealth(), max - 4.0F, "과열 II 두 번 피해");
				DamageSource last = golem.getLastDamageSource();
				helper.assertTrue(last != null && last.is(ModEffects.OVERHEAT_DAMAGE), "피해 종류가 과열이 아님: " + last);
				helper.assertTrue(last.is(DamageTypeTags.IS_FIRE) && last.is(DamageTypeTags.BYPASSES_ARMOR), "과열이 불 · 방어 무시 피해가 아님");
				helper.succeed();
			});
		});
	}

	/** 물에 서 있으면 과열이 바로 풀리고 피해가 없다. 물 밖 골렘은 그대로. */
	@GameTest
	public void waterClearsOverheat(GameTestHelper helper) {
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.WATER);
		IronGolem wet = dummy(helper, new BlockPos(2, 1, 2));
		IronGolem dry = dummy(helper, new BlockPos(5, 1, 5));
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(wet.isInWater(), "골렘이 물에 안 들어감");
			float max = wet.getMaxHealth();
			ModEffects.inflict(wet, ModEffects.OVERHEAT, 200, 1, null);
			ModEffects.inflict(dry, ModEffects.OVERHEAT, 200, 1, null);
			helper.runAfterDelay(3, () -> {
				helper.assertFalse(wet.hasEffect(ModEffects.OVERHEAT), "물에 있는데 과열이 안 풀림");
				helper.assertValueEqual(wet.getHealth(), max, "물에서 과열 피해를 받음");
				helper.assertTrue(dry.hasEffect(ModEffects.OVERHEAT), "물 밖인데 과열이 풀림");
				helper.succeed();
			});
		});
	}

	/** 내열 한 벌이면 과열이 걸리지 않고, 억지로 건 과열도 피해 없이 풀린다 (파수꾼 규칙과 같다: 내열이면 과열 없음). */
	@GameTest
	public void heatproofSetResistsOverheat(GameTestHelper helper) {
		Player suited = helper.makeMockPlayer(GameType.SURVIVAL);
		wear(suited, HeatproofGear.HEATPROOF_HELMET, HeatproofGear.HEATPROOF_CHESTPLATE, HeatproofGear.HEATPROOF_LEGGINGS, HeatproofGear.HEATPROOF_BOOTS);
		helper.assertTrue(Resistances.heatResistant(suited), "내열 한 벌인데 과열 저항이 아님");
		helper.assertFalse(ModEffects.inflict(suited, ModEffects.OVERHEAT, 200, 3, null), "내열 한 벌인데 과열이 걸림");

		IronGolem golem = dummy(helper, new BlockPos(3, 1, 3));
		wear(golem, HeatproofGear.HEATPROOF_HELMET, HeatproofGear.HEATPROOF_CHESTPLATE, HeatproofGear.HEATPROOF_LEGGINGS, HeatproofGear.HEATPROOF_BOOTS);
		float max = golem.getMaxHealth();
		golem.addEffect(new MobEffectInstance(ModEffects.OVERHEAT, 200, 2));
		helper.runAfterDelay(3, () -> {
			helper.assertFalse(golem.hasEffect(ModEffects.OVERHEAT), "내열을 입었는데 과열이 안 풀림");
			helper.assertValueEqual(golem.getHealth(), max, "내열을 입었는데 과열 피해");
			helper.succeed();
		});
	}

	// ---- 공허 침식 ----

	/** 공허 침식은 단계마다 최대 체력 −2. 체력도 새 최대에 맞춰 깎이고, 풀리면 최대가 돌아온다. */
	@GameTest
	public void voidErosionLowersMaxHealth(GameTestHelper helper) {
		IronGolem golem = dummy(helper, new BlockPos(2, 1, 2));
		float max = golem.getMaxHealth();
		helper.assertTrue(ModEffects.inflict(golem, ModEffects.VOID_EROSION, 200, 2, null), "공허 침식이 안 걸림");
		helper.assertValueEqual(golem.getMaxHealth(), max - 4.0F, "공허 침식 II 최대 체력");
		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(golem.getHealth(), max - 4.0F, "체력이 새 최대로 안 깎임");
			golem.removeEffect(ModEffects.VOID_EROSION);
			helper.assertValueEqual(golem.getMaxHealth(), max, "풀린 뒤 최대 체력");
			helper.succeed();
		});
	}

	/** 공허 한 벌이면 공허 침식을 막는다. */
	@GameTest
	public void voidSetResistsErosion(GameTestHelper helper) {
		Player suited = helper.makeMockPlayer(GameType.SURVIVAL);
		wear(suited, VoidGear.VOID_HELMET, VoidGear.VOID_CHESTPLATE, VoidGear.VOID_LEGGINGS, VoidGear.VOID_BOOTS);
		helper.assertTrue(Resistances.voidResistant(suited), "공허 한 벌인데 침식 저항이 아님");
		helper.assertFalse(ModEffects.inflict(suited, ModEffects.VOID_EROSION, 200, 1, null), "공허 한 벌인데 침식이 걸림");
		helper.assertValueEqual(suited.getMaxHealth(), 20.0F, "공허 한 벌인데 최대 체력이 줄음");
		Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(ModEffects.inflict(bare, ModEffects.VOID_EROSION, 200, 1, null), "맨몸인데 침식이 안 걸림");
		helper.assertValueEqual(bare.getMaxHealth(), 18.0F, "침식 I 최대 체력");
		helper.succeed();
	}

	// ---- 보스 타격 ----

	/** 수호자의 녹 공격: 맨몸은 녹(설정 단계 · 시간) + 구속, 강철 한 벌은 둘 다 없음. 설정 단계 0이면 녹을 걸지 않는다. */
	@GameTest
	public void guardianRustHitAppliesRust(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		guardian.setNoAi(true);
		Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
		Player steel = helper.makeMockPlayer(GameType.SURVIVAL);
		wear(steel, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		guardian.hitWithRust(level, bare, 4.0F);
		guardian.hitWithRust(level, steel, 4.0F);
		MobEffectInstance rust = bare.getEffect(ModEffects.RUST);
		helper.assertTrue(rust != null, "맨몸인데 녹이 안 걸림");
		helper.assertValueEqual(rust.getAmplifier(), RustyGuardian.RUST_LEVEL - 1, "녹 단계");
		helper.assertValueEqual(rust.getDuration(), RustyGuardian.RUST_TICKS, "녹 시간");
		helper.assertTrue(bare.hasEffect(MobEffects.SLOWNESS), "맨몸인데 구속이 안 걸림 (전과 같아야 함)");
		helper.assertFalse(steel.hasEffect(ModEffects.RUST), "강철 한 벌인데 녹이 걸림");

		Player other = helper.makeMockPlayer(GameType.SURVIVAL);
		SanupcraftConfig config = SanupcraftConfig.current();
		int saved = config.guardianRustLevel;
		try {
			config.guardianRustLevel = 0;
			guardian.hitWithRust(level, other, 4.0F);
		} finally {
			config.guardianRustLevel = saved;
		}
		helper.assertFalse(other.hasEffect(ModEffects.RUST), "녹 단계 0인데 녹이 걸림");
		helper.assertTrue(other.hasEffect(MobEffects.SLOWNESS), "녹 단계 0이어도 구속은 걸려야 함");
		helper.succeed();
	}

	/** 파수꾼의 과열 공격: 맨몸은 과열(설정 단계 · 시간), 내열 한 벌은 없음. 1.5배 피해 규칙은 그대로. */
	@GameTest
	public void wardenHeatHitAppliesOverheat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		warden.setNoAi(true);
		Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
		Player suited = helper.makeMockPlayer(GameType.SURVIVAL);
		wear(suited, HeatproofGear.HEATPROOF_HELMET, HeatproofGear.HEATPROOF_CHESTPLATE, HeatproofGear.HEATPROOF_LEGGINGS, HeatproofGear.HEATPROOF_BOOTS);
		warden.hitWithHeat(level, bare, 4.0F, true);
		warden.hitWithHeat(level, suited, 4.0F, true);
		MobEffectInstance heat = bare.getEffect(ModEffects.OVERHEAT);
		helper.assertTrue(heat != null, "맨몸인데 과열이 안 걸림");
		helper.assertValueEqual(heat.getAmplifier(), FurnaceWarden.OVERHEAT_LEVEL - 1, "과열 단계");
		helper.assertValueEqual(heat.getDuration(), FurnaceWarden.OVERHEAT_TICKS, "과열 시간");
		helper.assertFalse(bare.isOnFire(), "과열로 옮겼는데 불도 붙음");
		helper.assertFalse(suited.hasEffect(ModEffects.OVERHEAT), "내열 한 벌인데 과열이 걸림");
		float bareLoss = bare.getMaxHealth() - bare.getHealth();
		float suitedLoss = suited.getMaxHealth() - suited.getHealth();
		helper.assertTrue(bareLoss >= suitedLoss * FurnaceWarden.HEAT_MULTIPLIER - 0.01F, "1.5배 규칙이 깨짐: 맨몸 " + bareLoss + ", 내열 " + suitedLoss);
		helper.succeed();
	}

	/** 거상의 끌어당기기: 끌려가는 플레이어는 공허 침식 I, 공허 한 벌은 안 걸린다. */
	@GameTest
	public void colossusPullAppliesErosion(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VoidColossus colossus = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(3, 1, 3));
		colossus.setNoAi(true);
		// 시험 구역 안에 둔다 (6칸이면 구역 밖이라 묶음 배치에 따라 로드 안 된 청크에 걸려 들어가지 못할 때가 있었다). 끌어당김은 2칸 밖이면 된다
		Player bare = playerAt(helper, colossus.position().add(3.5, 0.0, 0.0));
		Player suited = playerAt(helper, colossus.position().add(0.0, 0.0, 3.5));
		wear(suited, VoidGear.VOID_HELMET, VoidGear.VOID_CHESTPLATE, VoidGear.VOID_LEGGINGS, VoidGear.VOID_BOOTS);
		helper.assertTrue(colossus.pull(level) >= 2, "둘 다 안 끌려감");
		MobEffectInstance erosion = bare.getEffect(ModEffects.VOID_EROSION);
		helper.assertTrue(erosion != null, "끌려갔는데 공허 침식이 안 걸림");
		helper.assertValueEqual(erosion.getAmplifier(), VoidColossus.EROSION_LEVEL - 1, "침식 단계");
		helper.assertValueEqual(bare.getMaxHealth(), 20.0F - 2.0F * VoidColossus.EROSION_LEVEL, "침식 최대 체력");
		helper.assertFalse(suited.hasEffect(ModEffects.VOID_EROSION), "공허 한 벌인데 침식이 걸림");
		bare.discard();
		suited.discard();
		helper.succeed();
	}

	// ---- 도우미 ----

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

	/** 월드에 들어가 있는 모의 플레이어 (패턴 판정은 월드의 플레이어만 찾는다). */
	private static Player playerAt(GameTestHelper helper, Vec3 pos) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		return player;
	}
}
