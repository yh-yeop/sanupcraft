package io.github.yhyeop.sanupcraft.test;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.effect.Resistances;
import io.github.yhyeop.sanupcraft.gear.Jetpack;
import io.github.yhyeop.sanupcraft.gear.ProductionGear;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.gear.special.DivingHelmet;
import io.github.yhyeop.sanupcraft.gear.special.HeadLamp;
import io.github.yhyeop.sanupcraft.gear.special.MinerGear;
import io.github.yhyeop.sanupcraft.gear.special.ModSpecialGear;
import io.github.yhyeop.sanupcraft.gear.special.MultitoolItem;
import io.github.yhyeop.sanupcraft.gear.special.PowerArmor;
import io.github.yhyeop.sanupcraft.hazard.ModHazard;
import io.github.yhyeop.sanupcraft.hazard.ToxicGasBlock;
import io.github.yhyeop.sanupcraft.module.GearModule;
import io.github.yhyeop.sanupcraft.module.GearModules;
import io.github.yhyeop.sanupcraft.module.ModuleSlots;
import io.github.yhyeop.sanupcraft.nether.ModNether;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;

/** 확장 3 G5 특수 장비 (잠수 헬멧 · 광부 갑옷 · 증기 파워 아머 · 제트팩 모듈) · G6 멀티툴 검사. 효과는 좀비로 본다 (가짜 플레이어는 효과 패킷을 못 보낸다). */
public class SanupcraftSpecialGearGameTest {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Item[] MINER_SET = {ModSpecialGear.MINER_HELMET, ModSpecialGear.MINER_CHESTPLATE, ModSpecialGear.MINER_LEGGINGS,
			ModSpecialGear.MINER_BOOTS};
	private static final Item[] POWER_SET = {ModSpecialGear.POWER_HELMET, ModSpecialGear.POWER_CHESTPLATE, ModSpecialGear.POWER_LEGGINGS,
			ModSpecialGear.POWER_BOOTS};

	/** 잠수 헬멧: 머리가 물에 잠기고 증기가 있으면 1초마다 증기 1을 쓰고 수중 호흡 · 야간 투시. 물 밖이나 증기가 없으면 아무것도 안 하고, 야간 투시는 거둔다. */
	@GameTest
	public void divingHelmetBreathesUnderwater(GameTestHelper helper) {
		BlockPos wet = new BlockPos(1, 1, 1);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = 0; dy <= 2; dy++) {
					boolean wall = dx != 0 || dz != 0;
					helper.setBlock(wet.offset(dx + 1, dy, dz + 1), wall ? Blocks.GLASS : Blocks.WATER);
				}
			}
		}
		Zombie diver = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, wet.offset(1, 0, 1));
		ItemStack helmet = new ItemStack(ModSpecialGear.DIVING_HELMET);
		SteamCharge.set(helmet, 100);
		diver.setItemSlot(EquipmentSlot.HEAD, helmet);
		helper.assertTrue(DivingHelmet.submerged(diver), "물속인데 잠긴 것으로 안 봄");
		helper.assertTrue(DivingHelmet.tickSecond(diver), "물속에서 증기를 안 씀");
		helper.assertValueEqual(SteamCharge.get(diver.getItemBySlot(EquipmentSlot.HEAD)), 100 - SanupcraftConfig.current().divingSteamPerSecond, "쓴 증기");
		helper.assertTrue(diver.hasEffect(MobEffects.WATER_BREATHING), "수중 호흡이 없음");
		helper.assertTrue(diver.hasEffect(MobEffects.NIGHT_VISION), "물속 시야(야간 투시)가 없음");
		// 증기가 없으면 아무것도 안 함
		SteamCharge.set(diver.getItemBySlot(EquipmentSlot.HEAD), 0);
		diver.removeEffect(MobEffects.WATER_BREATHING);
		helper.assertFalse(DivingHelmet.tickSecond(diver), "증기 없이 작동");
		helper.assertFalse(diver.hasEffect(MobEffects.WATER_BREATHING), "증기 없이 수중 호흡");
		helper.assertFalse(diver.hasEffect(MobEffects.NIGHT_VISION), "증기가 떨어졌는데 야간 투시가 남음");
		// 물 밖: 증기를 안 쓴다
		Zombie dry = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 5));
		ItemStack dryHelmet = new ItemStack(ModSpecialGear.DIVING_HELMET);
		SteamCharge.set(dryHelmet, 100);
		dry.setItemSlot(EquipmentSlot.HEAD, dryHelmet);
		helper.assertFalse(DivingHelmet.tickSecond(dry), "물 밖에서 작동");
		helper.assertValueEqual(SteamCharge.get(dry.getItemBySlot(EquipmentSlot.HEAD)), 100, "물 밖에서 증기를 씀");
		// 물약 야간 투시는 거두지 않는다
		dry.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600));
		DivingHelmet.tickSecond(dry);
		helper.assertTrue(dry.hasEffect(MobEffects.NIGHT_VISION), "물약 야간 투시를 거둠");
		helper.assertValueEqual(SteamCharge.capacity(helmet), DivingHelmet.CAPACITY, "잠수 헬멧 증기 용량");
		helper.succeed();
	}

	/** 광부 헬멧: 어둠 효과가 안 걸리고(이미 걸린 것도 풀림), 유독 가스 면역, 4칸 안 가스를 찾고 그 밖은 못 찾는다. */
	@GameTest
	public void minerHelmetBlocksDarknessAndGas(GameTestHelper helper) {
		Zombie miner = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Zombie plain = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(7, 1, 7));
		plain.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200));
		helper.assertTrue(plain.hasEffect(MobEffects.DARKNESS), "헬멧 없는 좀비에 어둠이 안 걸림 (시험이 틀림)");
		miner.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200));
		helper.assertTrue(miner.hasEffect(MobEffects.DARKNESS), "헬멧 쓰기 전 어둠");
		miner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModSpecialGear.MINER_HELMET));
		MinerGear.tick(miner);
		helper.assertFalse(miner.hasEffect(MobEffects.DARKNESS), "헬멧을 쓰고도 어둠이 남음");
		miner.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200));
		helper.assertFalse(miner.hasEffect(MobEffects.DARKNESS), "헬멧을 썼는데 어둠이 걸림");
		// 가스
		helper.assertTrue(Resistances.toxicGasImmune(miner), "광부 헬멧인데 가스 면역이 아님");
		helper.assertFalse(Resistances.toxicGasImmune(plain), "헬멧 없이 가스 면역");
		ToxicGasBlock.poison(miner);
		helper.assertFalse(miner.hasEffect(MobEffects.POISON), "광부 헬멧에 독이 걸림");
		helper.assertTrue(MinerGear.nearestGas(miner, MinerGear.GAS_WARN_RANGE) == null, "가스가 없는데 찾음");
		BlockPos far = new BlockPos(1, 2, 7);
		helper.setBlock(far, ModHazard.TOXIC_GAS);
		helper.assertTrue(MinerGear.nearestGas(miner, MinerGear.GAS_WARN_RANGE) == null, "4칸 밖 가스를 찾음");
		BlockPos near = new BlockPos(3, 2, 2);
		helper.setBlock(near, ModHazard.TOXIC_GAS);
		helper.assertValueEqual(MinerGear.nearestGas(miner, MinerGear.GAS_WARN_RANGE), helper.absolutePos(near), "가까운 가스 자리");
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
		helper.assertFalse(MinerGear.warnGas(player), "헬멧 없이 경고");
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModSpecialGear.MINER_HELMET));
		helper.assertTrue(MinerGear.warnGas(player), "헬멧을 썼는데 가스 경고가 없음");
		helper.succeed();
	}

	/** 광부 한 벌: 캐는 속도 1.2배 · 낙하 피해 배율 0.5, 하나라도 벗으면 풀림. 실제 낙하 피해도 반 이하. */
	@GameTest
	public void minerSetMinesFasterAndHalvesFall(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		wear(player, MINER_SET);
		MinerGear.tick(player);
		helper.assertValueEqual(player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), 1.2, "캐는 속도");
		helper.assertValueEqual(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 0.5, "낙하 피해 배율");
		player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
		MinerGear.tick(player);
		helper.assertValueEqual(player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), 1.0, "한 벌이 아닌데 캐는 속도");
		helper.assertValueEqual(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 1.0, "한 벌이 아닌데 낙하 배율");

		Zombie bare = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		Zombie suited = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 5));
		wear(suited, MINER_SET);
		MinerGear.tick(suited);
		bare.causeFallDamage(13.0, 1.0F, bare.damageSources().fall());
		suited.causeFallDamage(13.0, 1.0F, suited.damageSources().fall());
		float bareLoss = bare.getMaxHealth() - bare.getHealth();
		float suitedLoss = suited.getMaxHealth() - suited.getHealth();
		helper.assertTrue(bareLoss >= 8.0F, "맨몸 낙하 피해가 너무 작음 (시험이 틀림): " + bareLoss);
		helper.assertTrue(suitedLoss <= Math.ceil(bareLoss / 2.0F) && suitedLoss > 0, "광부 한 벌 낙하 피해: " + suitedLoss + " / 맨몸 " + bareLoss);
		helper.succeed();
	}

	/** 머리 조명: 막힌 어두운 칸에서 머리 칸에 빛 블록을 놓고, 헬멧을 벗으면 지운다. 밝은 곳에서는 안 놓는다. */
	@GameTest(maxTicks = 80)
	public void headLampLightsDarkness(GameTestHelper helper) {
		// 돌로 막은 1x2 빈칸 (하늘 빛이 빠지도록 몇 틱 기다린다)
		BlockPos foot = new BlockPos(2, 2, 2);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = -1; dy <= 2; dy++) {
					BlockPos pos = foot.offset(dx, dy, dz);
					boolean inside = dx == 0 && dz == 0 && (dy == 0 || dy == 1);
					helper.setBlock(pos, inside ? Blocks.AIR : Blocks.STONE);
				}
			}
		}
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModSpecialGear.MINER_HELMET));
		player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 1, 6))));
		helper.runAfterDelay(30, () -> {
			// 밝은 곳 (트인 하늘 · 낮이 아니면 건너뜀)
			ServerLevel level = helper.getLevel();
			if (level.isBrightOutside()) {
				helper.assertTrue(HeadLamp.tick(player) == null, "밝은 곳에서 조명을 놓음");
			}
			player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(foot)));
			BlockPos lamp = HeadLamp.tick(player);
			helper.assertValueEqual(lamp, helper.absolutePos(foot.above()), "조명 자리 (머리 칸)");
			helper.assertBlockPresent(Blocks.LIGHT, foot.above());
			helper.assertValueEqual(HeadLamp.tick(player), lamp, "다시 불러도 같은 자리");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			helper.assertTrue(HeadLamp.tick(player) == null, "헬멧을 벗었는데 조명이 남음");
			helper.assertBlockPresent(Blocks.AIR, foot.above());
			helper.succeed();
		});
	}

	/** 파워 아머: 한 벌 + 흉갑 증기면 힘 II · 점프 강화 · 넉백 저항 +0.5 · 오르는 높이 +0.5, 1초마다 증기를 쓴다. 증기가 없거나 한 벌이 아니면 다 풀린다. */
	@GameTest
	public void powerArmorBuffsOnlyWithSteam(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		wear(zombie, POWER_SET);
		double baseKnockback = zombie.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		double baseStep = zombie.getAttributeValue(Attributes.STEP_HEIGHT);
		PowerArmor.tick(zombie, true);
		helper.assertFalse(zombie.hasEffect(MobEffects.STRENGTH), "증기 없이 힘");
		ItemStack chest = zombie.getItemBySlot(EquipmentSlot.CHEST);
		SteamCharge.set(chest, 100);
		PowerArmor.tick(zombie, true);
		int cost = SanupcraftConfig.current().powerArmorSteamPerSecond;
		helper.assertValueEqual(SteamCharge.get(zombie.getItemBySlot(EquipmentSlot.CHEST)), 100 - cost, "1초에 쓴 증기");
		helper.assertTrue(zombie.hasEffect(MobEffects.STRENGTH) && zombie.getEffect(MobEffects.STRENGTH).getAmplifier() == 1, "힘 II가 없음");
		helper.assertTrue(zombie.hasEffect(MobEffects.JUMP_BOOST), "점프 강화가 없음");
		helper.assertValueEqual(zombie.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), baseKnockback + PowerArmor.KNOCKBACK_RESISTANCE, "넉백 저항");
		helper.assertValueEqual(zombie.getAttributeValue(Attributes.STEP_HEIGHT), baseStep + PowerArmor.STEP_HEIGHT, "오르는 높이");
		PowerArmor.tick(zombie, false);
		helper.assertValueEqual(SteamCharge.get(zombie.getItemBySlot(EquipmentSlot.CHEST)), 100 - cost, "1초가 아닌 틱에 증기를 씀");
		// 부츠를 벗으면 풀림
		zombie.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
		PowerArmor.tick(zombie, true);
		helper.assertFalse(zombie.hasEffect(MobEffects.STRENGTH), "한 벌이 아닌데 힘");
		helper.assertValueEqual(SteamCharge.get(zombie.getItemBySlot(EquipmentSlot.CHEST)), 100 - cost, "한 벌이 아닌데 증기를 씀");
		helper.assertValueEqual(zombie.getAttributeValue(Attributes.STEP_HEIGHT), baseStep, "한 벌이 아닌데 오르는 높이");
		// 증기가 떨어지면 풀림
		zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModSpecialGear.POWER_BOOTS));
		SteamCharge.set(zombie.getItemBySlot(EquipmentSlot.CHEST), cost);
		PowerArmor.tick(zombie, true);
		helper.assertValueEqual(SteamCharge.get(zombie.getItemBySlot(EquipmentSlot.CHEST)), 0, "남은 증기");
		helper.assertFalse(zombie.hasEffect(MobEffects.STRENGTH), "증기가 떨어졌는데 힘이 남음");
		helper.assertValueEqual(zombie.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), baseKnockback, "증기가 떨어졌는데 넉백 저항");
		// 물약 힘은 거두지 않는다
		zombie.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 3600, 1));
		PowerArmor.tick(zombie, true);
		helper.assertTrue(zombie.hasEffect(MobEffects.STRENGTH), "물약 힘을 거둠");
		helper.assertValueEqual(SteamCharge.capacity(chest), PowerArmor.CAPACITY, "흉갑 증기 용량");
		helper.succeed();
	}

	/** 제트팩 모듈: 파워 아머 흉갑에만 끼워지고, 끼우면 흉갑 증기로 뜬다 (서버 처리를 직접 부름). */
	@GameTest
	public void jetpackModuleLiftsPowerChest(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack chest = new ItemStack(ModSpecialGear.POWER_CHESTPLATE);
		SteamCharge.set(chest, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, chest);
		helper.assertFalse(Jetpack.thrust(player), "모듈 없이 뜸");
		helper.assertTrue(GearModules.check(new ItemStack(SteelGear.STEEL_CHESTPLATE), GearModule.JETPACK) == GearModules.Check.WRONG_KIND,
				"강철 흉갑에 제트팩 모듈이 끼워짐");
		helper.assertTrue(GearModules.check(new ItemStack(ProductionGear.STEAM_JETPACK), GearModule.JETPACK) == GearModules.Check.WRONG_KIND,
				"제트팩에 제트팩 모듈이 끼워짐");
		helper.assertTrue(GearModules.check(new ItemStack(ModSpecialGear.POWER_HELMET), GearModule.JETPACK) == GearModules.Check.WRONG_KIND,
				"투구에 제트팩 모듈이 끼워짐");
		helper.assertTrue(GearModules.check(new ItemStack(ModSpecialGear.MULTITOOL), GearModule.JETPACK) == GearModules.Check.WRONG_KIND,
				"멀티툴에 제트팩 모듈이 끼워짐");
		helper.assertTrue(GearModules.install(player.getItemBySlot(EquipmentSlot.CHEST), GearModule.JETPACK), "파워 아머 흉갑에 안 끼워짐");
		helper.assertTrue(Jetpack.worn(player) == player.getItemBySlot(EquipmentSlot.CHEST), "모듈 낀 흉갑을 제트팩으로 안 봄");
		helper.assertTrue(Jetpack.thrust(player), "모듈을 끼웠는데 안 뜸");
		helper.assertValueEqual(player.getDeltaMovement().y, Jetpack.liftedSpeed(0.0), "위 속도");
		helper.assertValueEqual(SteamCharge.get(player.getItemBySlot(EquipmentSlot.CHEST)), 1000 - SanupcraftConfig.current().jetpackSteamPerTick,
				"흉갑 증기를 씀");
		SteamCharge.set(player.getItemBySlot(EquipmentSlot.CHEST), 0);
		helper.assertFalse(Jetpack.thrust(player), "증기 없이 뜸");
		helper.assertTrue(GearModule.JETPACK.item() != null, "제트팩 모듈 아이템이 없음");
		helper.succeed();
	}

	/** 멀티툴: 돌 · 흙 · 원목 · 짚단을 맞는 도구처럼 빨리 캐고 드롭을 받는다 (블록당 증기 10). 흑요석도 다이아 등급으로 드롭. 증기가 없으면 맨손. */
	@GameTest
	public void multitoolMinesEverything(GameTestHelper helper) {
		ItemStack tool = charged(ModSpecialGear.MULTITOOL);
		helper.assertFalse(tool.isDamageableItem(), "멀티툴에 내구도가 있음");
		helper.assertValueEqual(SteamCharge.capacity(tool), MultitoolItem.CAPACITY, "증기 용량");
		for (var state : List.of(Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(),
				Blocks.HAY_BLOCK.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), Blocks.GRAVEL.defaultBlockState())) {
			helper.assertValueEqual(tool.getDestroySpeed(state), MultitoolItem.SPEED, "캐는 속도: " + state);
		}
		helper.assertTrue(tool.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "흑요석 (다이아 등급)");
		helper.assertTrue(tool.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "다이아 광석");

		BlockPos stone = new BlockPos(1, 1, 1);
		BlockPos dirt = new BlockPos(3, 1, 1);
		BlockPos log = new BlockPos(5, 1, 1);
		helper.setBlock(stone, Blocks.STONE);
		helper.setBlock(dirt, Blocks.DIRT);
		helper.setBlock(log, Blocks.OAK_LOG);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.MAINHAND, tool);
		for (BlockPos pos : List.of(stone, dirt, log)) {
			helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(pos)), "못 캠: " + pos);
			helper.assertBlockPresent(Blocks.AIR, pos);
		}
		helper.assertItemEntityPresent(Items.COBBLESTONE, stone, 2.0);
		helper.assertItemEntityPresent(Items.DIRT, dirt, 2.0);
		helper.assertItemEntityPresent(Items.OAK_LOG, log, 2.0);
		int per = SanupcraftConfig.current().multitoolSteamPerBlock;
		helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), MultitoolItem.CAPACITY - 3 * per, "쓴 증기 (3블록)");
		// 증기가 한 블록치보다 적으면 맨손
		SteamCharge.set(player.getMainHandItem(), per - 1);
		helper.assertValueEqual(player.getMainHandItem().getDestroySpeed(Blocks.STONE.defaultBlockState()), 1.0F, "증기 없는 속도");
		helper.assertFalse(player.getMainHandItem().isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "증기 없는데 드롭");
		helper.succeed();
	}

	/** 멀티툴 우클릭: 흙 → 밭 (괭이), 원목 → 벗긴 원목 (도끼), 웅크리면 잔디 → 흙길 (삽). 증기는 안 쓴다. */
	@GameTest
	public void multitoolTillsAndStrips(GameTestHelper helper) {
		BlockPos dirt = new BlockPos(1, 1, 1);
		BlockPos log = new BlockPos(3, 1, 1);
		BlockPos grass = new BlockPos(5, 1, 1);
		helper.setBlock(dirt, Blocks.DIRT);
		helper.setBlock(log, Blocks.OAK_LOG);
		helper.setBlock(grass, Blocks.GRASS_BLOCK);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack tool = charged(ModSpecialGear.MULTITOOL);
		player.setItemSlot(EquipmentSlot.MAINHAND, tool);
		helper.assertTrue(use(helper, player, dirt).consumesAction(), "흙을 못 갊");
		helper.assertBlockPresent(Blocks.FARMLAND, dirt);
		helper.assertTrue(use(helper, player, log).consumesAction(), "원목을 못 벗김");
		helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, log);
		player.setShiftKeyDown(true);
		helper.assertTrue(use(helper, player, grass).consumesAction(), "웅크리고 흙길을 못 만듦");
		helper.assertBlockPresent(Blocks.DIRT_PATH, grass);
		helper.assertValueEqual(SteamCharge.get(player.getMainHandItem()), MultitoolItem.CAPACITY, "우클릭에 증기를 씀");
		helper.succeed();
	}

	/** 모듈 칸: 잠수 헬멧 1, 광부 갑옷 2, 파워 아머 3, 멀티툴 3 (도구 종류, 범위 모듈은 안 됨). */
	@GameTest
	public void moduleSlotsRegistered(GameTestHelper helper) {
		helper.assertValueEqual(ModuleSlots.capacity(new ItemStack(ModSpecialGear.DIVING_HELMET)), 1, "잠수 헬멧 칸");
		for (Item item : MINER_SET) {
			helper.assertValueEqual(ModuleSlots.capacity(new ItemStack(item)), 2, "광부 갑옷 칸: " + item);
		}
		for (Item item : POWER_SET) {
			helper.assertValueEqual(ModuleSlots.capacity(new ItemStack(item)), 3, "파워 아머 칸: " + item);
		}
		ItemStack tool = new ItemStack(ModSpecialGear.MULTITOOL);
		helper.assertValueEqual(ModuleSlots.capacity(tool), 3, "멀티툴 칸");
		helper.assertTrue(ModuleSlots.kinds(tool).contains(GearModule.Kind.TOOL), "멀티툴이 도구가 아님");
		helper.assertTrue(GearModules.install(tool, GearModule.EFFICIENCY), "멀티툴에 효율 모듈이 안 끼워짐");
		helper.assertTrue(GearModules.check(tool, GearModule.AREA) == GearModules.Check.WRONG_KIND, "멀티툴에 범위 모듈이 끼워짐");
		helper.assertTrue(GearModules.install(new ItemStack(ModSpecialGear.MINER_HELMET), GearModule.NIGHT_VISION), "광부 헬멧에 모듈이 안 끼워짐");
		helper.assertTrue(new ItemStack(ModSpecialGear.MINER_HELMET).is(net.minecraft.tags.ItemTags.HEAD_ARMOR), "광부 헬멧 태그");
		helper.assertTrue(new ItemStack(ModSpecialGear.POWER_CHESTPLATE).is(net.minecraft.tags.ItemTags.CHEST_ARMOR), "파워 흉갑 태그");
		helper.succeed();
	}

	/** 레시피: 멀티툴(드릴 + 전기톱 + 정밀 부품, 드릴의 모듈을 이어 받음) · 광부 헬멧 · 파워 아머 흉갑(네더라이트에서, 마법 부여 계승) · 잠수 헬멧 · 제트팩 모듈. */
	@GameTest
	public void recipesCraft(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack drill = new ItemStack(ProductionGear.STEAM_DRILL);
		GearModules.install(drill, GearModule.EFFICIENCY);
		ItemStack precision = new ItemStack(ModNether.PRECISION_PART);
		ItemStack multitool = craft(level, 3, 2, List.of(drill, new ItemStack(ProductionGear.CHAINSAW), precision, precision,
				new ItemStack(Items.IRON_HOE), new ItemStack(io.github.yhyeop.sanupcraft.gear.SteamGear.STEAM_CANISTER)));
		helper.assertTrue(multitool.is(ModSpecialGear.MULTITOOL), "멀티툴 레시피: " + multitool);
		helper.assertTrue(GearModules.has(multitool, GearModule.EFFICIENCY), "드릴 모듈이 안 옮겨짐");

		ItemStack c = new ItemStack(io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_INGOT);
		helper.assertTrue(craft(level, 3, 2, List.of(c, new ItemStack(Items.LANTERN), c, c, ItemStack.EMPTY, c)).is(ModSpecialGear.MINER_HELMET),
				"광부 헬멧 레시피");

		Holder<Enchantment> protection = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		ItemStack netherite = new ItemStack(Items.NETHERITE_CHESTPLATE);
		netherite.enchant(protection, 3);
		ItemStack g = new ItemStack(ModItems.GEAR_PART);
		ItemStack powerChest = craft(level, 3, 3, List.of(g, new ItemStack(io.github.yhyeop.sanupcraft.gear.SteamGear.STEAM_CANISTER), g,
				precision, netherite, precision, g, new ItemStack(io.github.yhyeop.sanupcraft.power.ModPower.STEAM_PIPE), g));
		helper.assertTrue(powerChest.is(ModSpecialGear.POWER_CHESTPLATE), "파워 아머 흉갑 레시피: " + powerChest);
		helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(protection, powerChest), 3, "보호가 안 옮겨짐");

		ItemStack glass = new ItemStack(Items.GLASS);
		ItemStack copper = new ItemStack(Items.COPPER_INGOT);
		List<ItemStack> diving = new ArrayList<>(List.of(copper, glass, copper, glass, new ItemStack(SteelGear.STEEL_HELMET), glass, ItemStack.EMPTY,
				new ItemStack(io.github.yhyeop.sanupcraft.gear.SteamGear.STEAM_CANISTER), ItemStack.EMPTY));
		helper.assertTrue(craft(level, 3, 3, diving).is(ModSpecialGear.DIVING_HELMET), "잠수 헬멧 레시피");
		helper.assertTrue(craft(level, 2, 1, List.of(new ItemStack(io.github.yhyeop.sanupcraft.module.ModModules.MODULE_FRAME),
				new ItemStack(ProductionGear.STEAM_JETPACK))).is(GearModule.JETPACK.item()), "제트팩 모듈 레시피");
		helper.succeed();
	}

	/** 실테스트 꾸러미 `specialgear` · 발전 과제 · 설정 구역. */
	@GameTest
	public void playtestKitAndAdvancements(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.SPECIALGEAR);
		for (Item item : List.of(ModSpecialGear.DIVING_HELMET, ModSpecialGear.MINER_HELMET, ModSpecialGear.MINER_BOOTS, ModSpecialGear.POWER_HELMET,
				ModSpecialGear.POWER_CHESTPLATE, ModSpecialGear.POWER_BOOTS, ModSpecialGear.MULTITOOL, GearModule.JETPACK.item())) {
			helper.assertTrue(player.getInventory().countItem(item) > 0, "꾸러미에 없음: " + item);
		}
		boolean jetChest = false;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			jetChest |= stack.is(ModSpecialGear.POWER_CHESTPLATE) && GearModules.has(stack, GearModule.JETPACK) && SteamCharge.isFull(stack);
		}
		helper.assertTrue(jetChest, "제트팩 모듈 낀 가득 찬 흉갑을 주지 않음");
		var advancements = helper.getLevel().getServer().getAdvancements();
		for (String id : List.of("diving_helmet", "miner_armor", "power_armor", "multitool")) {
			helper.assertTrue(advancements.get(Sanupcraft.id(id)) != null, "발전 과제가 없음: " + id);
		}
		String json = SanupcraftConfig.defaults().toJson();
		for (String key : List.of("\"divingHelmet\"", "\"minerArmor\"", "\"powerArmor\"", "\"multitool\"", "lampLevel", "fallReductionPercent")) {
			helper.assertTrue(json.contains(key), "설정에 없음: " + key);
		}
		helper.succeed();
	}

	private static net.minecraft.world.InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos abs = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0), Direction.UP, abs, false);
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	private static ItemStack charged(Item item) {
		ItemStack stack = new ItemStack(item);
		SteamCharge.set(stack, SteamCharge.capacity(stack));
		return stack;
	}

	private static void wear(LivingEntity entity, Item[] set) {
		for (int i = 0; i < SLOTS.length; i++) {
			entity.setItemSlot(SLOTS[i], new ItemStack(set[i]));
		}
	}

	private static ItemStack craft(ServerLevel level, int width, int height, List<ItemStack> items) {
		CraftingInput input = CraftingInput.of(width, height, items);
		return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
	}
}
