package io.github.yhyeop.sanupcraft.test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.BossParty;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.boss.VoidCrystal;
import io.github.yhyeop.sanupcraft.end.BreathCollectorBlockEntity;
import io.github.yhyeop.sanupcraft.end.ChorusHarvesterBlockEntity;
import io.github.yhyeop.sanupcraft.end.DimensionalVaultBlockEntity;
import io.github.yhyeop.sanupcraft.end.ElytraBoost;
import io.github.yhyeop.sanupcraft.end.EndIslandStructure;
import io.github.yhyeop.sanupcraft.end.ModEnd;
import io.github.yhyeop.sanupcraft.end.VaultData;
import io.github.yhyeop.sanupcraft.end.VoidCondenserBlockEntity;
import io.github.yhyeop.sanupcraft.end.VoidGear;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.RivetProjectile;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.nether.ModNether;

/** M6 엔드 최종: 공허 응축기, 공허 거상 · 공허 수정, 공허 장비, 후렴 수확기, 차원 창고, 겉날개 가속, 숨결 수집기, 버려진 정거장. */
public class SanupcraftM6GameTest {
	// ---- 공허 응축기 ----

	/** 아래가 막히면 안 돌고, 최저 높이까지 비우면 엔드(시험은 inEnd = true)에서만 틱당 8. 고압 파이프로 정밀 조립기에 간다. */
	@GameTest
	public void voidCondenserNeedsVoidBelow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos rel = new BlockPos(2, 3, 2);
		helper.setBlock(rel.east(), ModNether.HIGH_PRESSURE_PIPE);
		helper.setBlock(rel, ModEnd.VOID_CONDENSER);
		helper.setBlock(rel.east(2), ModMachines.PRECISION_ASSEMBLER);
		helper.setBlock(rel.below(), Blocks.STONE);
		VoidCondenserBlockEntity condenser = helper.getBlockEntity(rel, VoidCondenserBlockEntity.class);
		MachineBlockEntity precision = helper.getBlockEntity(rel.east(2), MachineBlockEntity.class);
		BlockPos abs = helper.absolutePos(rel);
		helper.assertTrue(helper.getBlockState(rel.east()).getValue(PipeBlock.WEST), "고압 파이프가 응축기에 안 이어짐");
		helper.assertTrue(condenser.status(level, abs) == VoidCondenserBlockEntity.Status.NOT_END, "오버월드 상태: " + condenser.status(level, abs));
		condenser.recheck();
		helper.assertValueEqual(condenser.work(level, abs, true), 0, "아래가 막혔는데 증기를 만듦");

		// 아래를 최저 높이까지 비운다 (끝나면 되돌린다)
		Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
		for (int y = abs.getY() - 1; y >= level.getMinY(); y--) {
			BlockPos p = abs.atY(y);
			saved.put(p, level.getBlockState(p));
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		try {
			helper.assertTrue(VoidCondenserBlockEntity.isVoidBelow(level, abs), "아래가 비었는데 공허로 안 봄");
			condenser.recheck();
			helper.assertValueEqual(condenser.work(level, abs, false), 0, "엔드가 아닌데 증기를 만듦");
			int made = 0;
			for (int i = 0; i < 10; i++) {
				condenser.recheck();
				made += condenser.work(level, abs, true);
			}
			helper.assertValueEqual(made, 10 * VoidCondenserBlockEntity.STEAM_PER_TICK, "열 틱에 만든 고압 증기");
			helper.assertTrue(precision.steam() > 0, "정밀 조립기가 고압 증기를 못 받음");
		} finally {
			saved.forEach((p, state) -> level.setBlock(p, state, Block.UPDATE_CLIENTS));
		}
		helper.succeed();
	}

	// ---- 공허 거상 ----

	/** 기본 체력 600, 인원 배율(3명 = 2배). 엔드 원점에서 500칸 밖에서만 깨어난다. */
	@GameTest(environment = "sanupcraft-test:alone_colossus")
	public void colossusHealthAndSummonRules(GameTestHelper helper) {
		// 오버월드 제단은 거상을 부르지 않는다 (근처에 보스가 없어도)
		helper.assertTrue(io.github.yhyeop.sanupcraft.boss.GuardianAltarBlock.summon(helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 6)), ModEnd.VOID_COLOSSUS) == null,
				"오버월드 제단이 거상을 부름");
		VoidColossus colossus = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(3, 1, 3));
		colossus.setNoAi(true);
		helper.assertValueEqual((double) colossus.getMaxHealth(), VoidColossus.MAX_HEALTH, "기본 체력");
		colossus.party().scale(colossus, 3);
		// 바닐라 최대 체력 상한(1024)을 넘는 몫은 받는 피해를 줄여 맞춘다
		double wanted = VoidColossus.MAX_HEALTH * BossParty.healthScale(3);
		helper.assertValueEqual(colossus.uncappedMaxHealth(), wanted, "3명 체력 (상한 전)");
		helper.assertValueEqual((double) colossus.getMaxHealth(), Math.min(wanted, 1024.0), "3명 체력 (속성)");
		helper.assertTrue(Math.abs(colossus.damageScale() - colossus.getMaxHealth() / wanted) < 1.0E-4, "피해 배율: " + colossus.damageScale());
		helper.assertValueEqual(colossus.getHealth(), colossus.getMaxHealth(), "체력이 꽉 차지 않음");
		helper.assertTrue(VoidColossus.wakeProblem(Level.OVERWORLD, new BlockPos(1000, 64, 0)) == VoidColossus.WakeProblem.NOT_END, "오버월드에서 깨어남");
		helper.assertTrue(VoidColossus.wakeProblem(Level.END, new BlockPos(300, 64, 300)) == VoidColossus.WakeProblem.TOO_CLOSE, "중앙 섬 가까이서 깨어남");
		helper.assertTrue(VoidColossus.wakeProblem(Level.END, new BlockPos(400, 64, 400)) == null, "566칸 밖인데 안 깨어남");
		helper.succeed();
	}

	/** 수정이 살아 있으면 거상은 무적. 리벳 · 화살 · 근접이 수정을 깎고, 다 부수면 거상이 피해를 받는다. */
	@GameTest(environment = "sanupcraft-test:alone_colossus", maxTicks = 100)
	public void crystalsShieldColossus(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VoidColossus colossus = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(4, 1, 4));
		colossus.setNoAi(true);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(4.5, 1.0, 0.5)));
		List<VoidCrystal> crystals = colossus.spawnCrystals(level);
		helper.assertValueEqual(crystals.size(), VoidColossus.CRYSTALS, "수정 수");
		helper.assertTrue(colossus.isShielded(level), "수정이 있는데 무적이 아님");
		float before = colossus.getHealth();
		helper.assertFalse(colossus.hurtServer(level, colossus.damageSources().playerAttack(player), 20.0F), "수정이 있는데 피해를 받음");
		helper.assertValueEqual(colossus.getHealth(), before, "수정이 있는데 체력이 줄음");

		// 시험 자리 안으로 옮긴다 (둘레 6칸은 시험 구역 밖이라 화살이 경계에 막힌다)
		VoidCrystal rivetTarget = crystals.get(0);
		VoidCrystal arrowTarget = crystals.get(1);
		rivetTarget.setPos(helper.absoluteVec(new Vec3(1.5, 3.0, 5.5)));
		arrowTarget.setPos(helper.absoluteVec(new Vec3(7.0, 3.0, 5.5)));
		RivetProjectile rivet = new RivetProjectile(level, player);
		aimAt(rivet, rivetTarget);
		level.addFreshEntity(rivet);
		Arrow arrow = new Arrow(level, 0, 0, 0, new ItemStack(Items.ARROW), null);
		arrow.setOwner(player);
		aimAt(arrow, arrowTarget);
		level.addFreshEntity(arrow);

		helper.runAfterDelay(20, () -> {
			helper.assertTrue(rivetTarget.health() < VoidCrystal.MAX_HEALTH, "리벳이 수정을 못 깎음: " + rivetTarget.health());
			helper.assertTrue(arrowTarget.health() < VoidCrystal.MAX_HEALTH, "화살이 수정을 못 깎음: " + arrowTarget.health() + " 화살 " + arrow.position() + " 남음=" + !arrow.isRemoved() + " 수정 " + arrowTarget.position() + " 거상 " + colossus.position());
			helper.assertTrue(colossus.isShielded(level), "수정 둘이 남았는데 무적이 풀림");
			// 근접으로 나머지를 부순다
			for (VoidCrystal crystal : crystals) {
				if (crystal.isAlive()) {
					crystal.hurtServer(level, crystal.damageSources().playerAttack(player), 100.0F);
				}
			}
			helper.assertFalse(colossus.isShielded(level), "수정을 다 부쉈는데 무적");
			helper.assertTrue(colossus.hurtServer(level, colossus.damageSources().playerAttack(player), 20.0F), "수정이 없는데 피해를 안 받음");
			helper.assertTrue(colossus.getHealth() < before, "체력이 안 줄음");
			// 거상 자신의 피해(셜커 탄 등)로는 수정이 안 깨진다
			List<VoidCrystal> again = colossus.spawnCrystals(level);
			helper.assertFalse(again.get(0).hurtServer(level, colossus.damageSources().mobAttack(colossus), 100.0F), "거상이 제 수정을 깸");
			again.forEach(VoidCrystal::discard);
			helper.succeed();
		});
	}

	/** 기여자 각자 공허 핵을 받는다 (5% 미만은 못 받음, 바닥에 떨어지지 않음). */
	@GameTest(environment = "sanupcraft-test:alone_colossus")
	public void contributorsGetVoidCores(GameTestHelper helper) {
		VoidColossus colossus = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(3, 1, 3));
		colossus.setNoAi(true);
		ServerPlayer killer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ServerPlayer assistant = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ServerPlayer bystander = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		float needed = colossus.getMaxHealth() * BossParty.CONTRIBUTION_SHARE;
		colossus.party().record(assistant, needed + 1.0F);
		colossus.party().record(bystander, needed - 1.0F);
		colossus.hurtServer(helper.getLevel(), colossus.damageSources().playerAttack(killer), 10000.0F);
		helper.assertTrue(colossus.isDeadOrDying(), "거상이 죽지 않음");
		helper.assertValueEqual(count(killer, ModEnd.VOID_CORE), 1, "막타 친 사람의 공허 핵");
		helper.assertValueEqual(count(assistant, ModEnd.VOID_CORE), 1, "도운 사람의 공허 핵");
		helper.assertValueEqual(count(bystander, ModEnd.VOID_CORE), 0, "5% 미만인 사람의 공허 핵");
		helper.assertItemEntityNotPresent(ModEnd.VOID_CORE);
		helper.succeed();
	}

	// ---- 공허 장비 ----

	/** 한 벌을 입고 최저 높이 − 10 아래로 떨어지면 마지막 땅으로 올라오고 느린 낙하. 쿨다운 동안은 다시 안 구한다. 한 벌이 아니면 안 구한다. */
	@GameTest
	public void voidGearRescuesFromVoid(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = offlinePlayer(helper);
		try {
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VoidGear.VOID_HELMET));
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(VoidGear.VOID_CHESTPLATE));
			player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(VoidGear.VOID_LEGGINGS));
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(VoidGear.VOID_BOOTS));
			helper.assertTrue(VoidGear.wearsFullSet(player), "한 벌로 안 침");
			helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
			Vec3 ground = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
			player.setPos(ground);
			player.setOnGround(true);
			helper.assertTrue(VoidGear.rememberSafe(player), "서 있는 땅을 기억하지 않음");

			long now = level.getGameTime();
			helper.assertFalse(VoidGear.rescue(player, now), "공허가 아닌데 구함");
			player.setOnGround(false);
			player.setPos(ground.x, level.getMinY() - 20, ground.z);
			helper.assertTrue(VoidGear.rescue(player, now), "공허에서 안 구함");
			helper.assertTrue(Math.abs(player.getY() - ground.y) < 0.01, "기억한 땅으로 안 올라옴: y=" + player.getY());
			helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "느린 낙하가 없음");

			player.setPos(ground.x, level.getMinY() - 20, ground.z);
			helper.assertFalse(VoidGear.rescue(player, now + 100), "쿨다운 중에 또 구함");
			helper.assertTrue(VoidGear.rescue(player, now + VoidGear.RESCUE_COOLDOWN), "쿨다운이 지났는데 안 구함");

			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			player.setPos(ground.x, level.getMinY() - 20, ground.z);
			helper.assertFalse(VoidGear.rescue(player, now + VoidGear.RESCUE_COOLDOWN * 3L), "한 벌이 아닌데 구함");
		} finally {
			VoidGear.forget(player);
			player.discard();
		}
		helper.succeed();
	}

	// ---- 후렴 수확기 ----

	/** 다 자란 후렴 식물(시든 꽃): 맨 아래 줄기만 남기고 거두고, 그 위에 새 꽃을 심는다. 자라는 꽃이 있으면 안 거둔다. */
	@GameTest
	public void chorusHarvesterHarvestsGrownPlant(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos harvesterPos = new BlockPos(1, 2, 1);
		helper.setBlock(harvesterPos, ModEnd.CHORUS_HARVESTER);
		ChorusHarvesterBlockEntity harvester = helper.getBlockEntity(harvesterPos, ChorusHarvesterBlockEntity.class);
		harvester.acceptSteam(ChorusHarvesterBlockEntity.STEAM_CAPACITY);
		BlockPos base = new BlockPos(3, 1, 3);
		helper.setBlock(base, Blocks.END_STONE);
		int height = 12;
		for (int y = 1; y <= height; y++) {
			helper.setBlock(base.above(y), Blocks.CHORUS_PLANT);
		}
		BlockPos flower = base.above(height + 1);
		helper.setBlock(flower, Blocks.CHORUS_FLOWER.defaultBlockState().setValue(ChorusFlowerBlock.AGE, 3));
		BlockPos bottom = helper.absolutePos(base.above());
		helper.assertTrue(harvester.findRipe(level, helper.absolutePos(harvesterPos)) == null, "자라는 꽃이 있는데 거둘 것으로 봄");

		helper.setBlock(flower, Blocks.CHORUS_FLOWER.defaultBlockState().setValue(ChorusFlowerBlock.AGE, ChorusFlowerBlock.DEAD_AGE));
		helper.assertValueEqual(harvester.findRipe(level, helper.absolutePos(harvesterPos)), bottom, "맨 아래 줄기");
		helper.assertTrue(harvester.harvest(level, bottom), "안 거둠");
		helper.assertTrue(helper.getBlockState(base.above()).is(Blocks.CHORUS_PLANT), "맨 아래 줄기가 없어짐");
		BlockState replanted = helper.getBlockState(base.above(2));
		helper.assertTrue(replanted.is(Blocks.CHORUS_FLOWER) && replanted.getValue(ChorusFlowerBlock.AGE) == 0, "새 꽃을 안 심음: " + replanted);
		for (int y = 3; y <= height + 1; y++) {
			helper.assertTrue(helper.getBlockState(base.above(y)).isAir(), "줄기가 남음: " + y);
		}
		int fruit = 0;
		for (int i = 0; i < harvester.getContainerSize(); i++) {
			ItemStack stack = harvester.getItem(i);
			helper.assertTrue(stack.isEmpty() || stack.is(Items.CHORUS_FRUIT), "후렴과가 아닌 것: " + stack);
			fruit += stack.getCount();
		}
		helper.assertTrue(fruit > 0, "후렴과를 하나도 못 모음");
		helper.assertValueEqual(harvester.steamStored(), ChorusHarvesterBlockEntity.STEAM_CAPACITY - ChorusHarvesterBlockEntity.STEAM_PER_PLANT, "쓴 증기");
		helper.succeed();
	}

	// ---- 차원 창고 ----

	/** 같은 채널이면 다른 블록 · 다른 차원(엔드)에서도 같은 칸, 다른 채널은 따로. 깔때기 쪽(Container)도 같은 칸을 본다. 부숴도 내용은 남는다. */
	@GameTest
	public void dimensionalVaultSharesChannel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		DyeColor color = DyeColor.LIME;
		VaultData.Channel channel = VaultData.get(level.getServer()).channel(color);
		channel.clearContent();
		helper.setBlock(new BlockPos(1, 1, 1), ModEnd.DIMENSIONAL_VAULT);
		helper.setBlock(new BlockPos(4, 1, 1), ModEnd.DIMENSIONAL_VAULT);
		DimensionalVaultBlockEntity a = helper.getBlockEntity(new BlockPos(1, 1, 1), DimensionalVaultBlockEntity.class);
		DimensionalVaultBlockEntity b = helper.getBlockEntity(new BlockPos(4, 1, 1), DimensionalVaultBlockEntity.class);
		a.setChannel(color);
		b.setChannel(DyeColor.BROWN);

		ServerLevel end = level.getServer().getLevel(Level.END);
		BlockPos endPos = new BlockPos(0, 120, 320);
		end.setBlockAndUpdate(endPos, ModEnd.DIMENSIONAL_VAULT.defaultBlockState());
		DimensionalVaultBlockEntity far = (DimensionalVaultBlockEntity) end.getBlockEntity(endPos);
		try {
			far.setChannel(color);
			a.setItem(0, new ItemStack(Items.DIAMOND, 5));
			helper.assertTrue(far.channel() == a.channel(), "엔드 창고가 다른 칸 묶음을 봄");
			helper.assertTrue(far.getItem(0).is(Items.DIAMOND) && far.getItem(0).getCount() == 5, "엔드 창고에서 다이아가 안 보임");
			helper.assertTrue(b.getItem(0).isEmpty(), "다른 채널에 다이아가 보임");
			far.removeItem(0, 2);
			helper.assertValueEqual(a.getItem(0).getCount(), 3, "엔드에서 뺀 것이 반영 안 됨");
			helper.assertTrue(VaultData.get(level.getServer()).isDirty(), "바뀐 내용을 저장 표시 안 함");
			b.setChannel(color);
			helper.assertValueEqual(b.getItem(0).getCount(), 3, "채널을 바꾼 창고에 내용이 안 보임");
			helper.destroyBlock(new BlockPos(1, 1, 1));
			helper.assertItemEntityNotPresent(Items.DIAMOND);
			helper.assertValueEqual(channel.getItem(0).getCount(), 3, "부쉈더니 내용이 사라짐");
		} finally {
			end.removeBlock(endPos, false);
			channel.clearContent();
		}
		helper.succeed();
	}

	/** 잠긴 채널: 주인이 아닌 사람은 못 열고, 주인이 아닌 사람이 놓은 창고는 깔때기가 못 쓴다. */
	@GameTest
	public void dimensionalVaultLock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VaultData.Channel channel = VaultData.get(level.getServer()).channel(DyeColor.PINK);
		channel.release();
		ServerPlayer owner = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		ServerPlayer stranger = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		try {
			helper.assertTrue(channel.canUse(stranger), "안 잠긴 채널을 못 씀");
			helper.assertTrue(channel.toggleLock(owner), "주인 없는 채널을 못 잠금");
			helper.assertTrue(channel.isLocked() && channel.canUse(owner), "주인이 못 씀");
			helper.assertFalse(channel.canUse(stranger), "남이 잠긴 채널을 씀");
			helper.assertFalse(channel.toggleLock(stranger), "남이 잠금을 풂");
			helper.assertTrue(channel.automationAllowed(level.getServer(), owner.getUUID()), "주인이 놓은 창고의 깔때기가 막힘");
			helper.assertFalse(channel.automationAllowed(level.getServer(), stranger.getUUID()), "남이 놓은 창고의 깔때기가 잠긴 채널을 씀");

			helper.setBlock(new BlockPos(1, 1, 1), ModEnd.DIMENSIONAL_VAULT);
			DimensionalVaultBlockEntity vault = helper.getBlockEntity(new BlockPos(1, 1, 1), DimensionalVaultBlockEntity.class);
			vault.setPlacer(stranger);
			vault.setChannel(DyeColor.PINK);
			helper.assertValueEqual(vault.getContainerSize(), 0, "잠긴 채널을 남의 창고가 깔때기에 보여 줌");
			vault.setPlacer(owner);
			helper.assertValueEqual(vault.getContainerSize(), VaultData.SLOTS, "주인의 창고가 칸을 안 보여 줌");
		} finally {
			channel.release();
		}
		helper.succeed();
	}

	// ---- 겉날개 가속 ----

	/** 나는 중에만 가속, 증기 100을 쓰고 몸에 붙은 폭죽이 생긴다. 증기가 모자라면 안 쓴다. */
	@GameTest
	public void elytraBoostUsesSteam(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(2.5, 3.0, 2.5)));
		ItemStack canister = new ItemStack(SteamGear.STEAM_CANISTER);
		SteamCharge.set(canister, 250);
		helper.assertTrue(ElytraBoost.boost(level, player, canister) == ElytraBoost.Result.NOT_FLYING, "날지 않는데 가속");
		helper.assertValueEqual(SteamCharge.get(canister), 250, "날지 않는데 증기를 씀");

		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
		player.startFallFlying();
		helper.assertTrue(player.isFallFlying(), "시험 플레이어가 날지 않음");
		helper.assertTrue(ElytraBoost.boost(level, player, canister) == ElytraBoost.Result.BOOSTED, "가속 안 됨");
		helper.assertValueEqual(SteamCharge.get(canister), 150, "가속 한 번에 쓴 증기");
		AABB around = player.getBoundingBox().inflate(3.0);
		helper.assertFalse(level.getEntitiesOfClass(FireworkRocketEntity.class, around).isEmpty(), "몸에 붙은 폭죽이 없음");
		ElytraBoost.boost(level, player, canister);
		helper.assertTrue(ElytraBoost.boost(level, player, canister) == ElytraBoost.Result.NO_STEAM, "증기 50으로 가속됨");
		helper.assertValueEqual(SteamCharge.get(canister), 50, "모자랄 때 증기를 씀");
		level.getEntitiesOfClass(FireworkRocketEntity.class, around).forEach(FireworkRocketEntity::discard);
		helper.succeed();
	}

	// ---- 숨결 수집기 ----

	/** 용의 숨결 구름이 8칸 안에 있으면 유리병 하나를 용의 숨결로, 다른 구름 · 구름 없음은 안 바꾼다. */
	@GameTest
	public void breathCollectorFillsBottles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModEnd.BREATH_COLLECTOR);
		BreathCollectorBlockEntity collector = helper.getBlockEntity(pos, BreathCollectorBlockEntity.class);
		collector.setItem(0, new ItemStack(Items.GLASS_BOTTLE, 3));
		BlockPos abs = helper.absolutePos(pos);
		helper.assertFalse(collector.collect(level, abs), "구름이 없는데 채움");

		Vec3 at = helper.absoluteVec(new Vec3(5.5, 1.0, 1.5));
		AreaEffectCloud plain = new AreaEffectCloud(level, at.x, at.y, at.z);
		plain.setRadius(3.0F);
		plain.setDuration(600);
		level.addFreshEntity(plain);
		helper.assertFalse(collector.collect(level, abs), "보통 구름으로 채움");
		plain.discard();

		AreaEffectCloud breath = new AreaEffectCloud(level, at.x, at.y, at.z);
		breath.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
		breath.setRadius(3.0F);
		breath.setDuration(600);
		level.addFreshEntity(breath);
		helper.assertTrue(collector.collect(level, abs), "숨결 구름 옆에서 안 채움");
		helper.assertValueEqual(collector.count(Items.DRAGON_BREATH), 1, "용의 숨결 수");
		helper.assertValueEqual(collector.count(Items.GLASS_BOTTLE), 2, "남은 유리병");
		helper.assertTrue(breath.getRadius() < 3.0F, "구름이 줄지 않음");
		helper.assertFalse(collector.canPlaceItemThroughFace(0, new ItemStack(Items.STONE), null), "깔때기가 돌을 넣음");
		helper.assertFalse(collector.canTakeItemThroughFace(0, new ItemStack(Items.GLASS_BOTTLE), net.minecraft.core.Direction.DOWN), "깔때기가 빈 병을 뺌");
		breath.discard();
		helper.succeed();
	}

	// ---- 버려진 정거장 ----

	/** 구조물 · 묶음 · 종류가 등록돼 있고, 틀에 제단 하나 · 상자 하나(정거장 전리품). */
	@GameTest
	public void abandonedStationRegisteredAndTemplate(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		var structure = access.lookupOrThrow(Registries.STRUCTURE).getValue(ModEnd.ABANDONED_STATION);
		helper.assertTrue(structure instanceof EndIslandStructure, "버려진 정거장 구조물이 없거나 종류가 다름: " + structure);
		helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE_SET)
				.get(ResourceKey.create(Registries.STRUCTURE_SET, Sanupcraft.id("abandoned_stations"))).isPresent(), "구조물 묶음이 등록 안 됨");
		StructureTemplate template = helper.getLevel().getStructureTemplateManager().get(Sanupcraft.id("abandoned_station"))
				.orElseThrow(() -> helper.assertionException("abandoned_station.nbt를 못 읽음"));
		StructurePlaceSettings settings = new StructurePlaceSettings();
		helper.assertValueEqual(template.filterBlocks(BlockPos.ZERO, settings, ModBoss.GUARDIAN_ALTAR).size(), 1, "제단 수");
		List<StructureTemplate.StructureBlockInfo> chests = template.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST);
		helper.assertValueEqual(chests.size(), 1, "상자 수");
		String loot = chests.get(0).nbt() == null ? "" : chests.get(0).nbt().getString("LootTable").orElse("");
		helper.assertValueEqual(loot, ModEnd.ABANDONED_STATION_CHEST.identifier().toString(), "상자 전리품 테이블");
		helper.assertTrue(template.filterBlocks(BlockPos.ZERO, settings, Blocks.PURPUR_PILLAR).size() >= 4, "다리가 없음");
		helper.succeed();
	}

	/** 상자 100번: 셜커 껍데기 · 엔더 진주 · 증기 통 · 정밀 부품 · 파수꾼의 심장이 다 나온다. */
	@GameTest
	public void abandonedStationChestLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(ModEnd.ABANDONED_STATION_CHEST);
		Map<Item, Integer> counts = new LinkedHashMap<>();
		List<Item> wanted = new ArrayList<>(List.of(Items.SHULKER_SHELL, Items.ENDER_PEARL, SteamGear.STEAM_CANISTER, ModNether.PRECISION_PART,
				io.github.yhyeop.sanupcraft.core.ModItems.WARDEN_HEART, Items.ENDER_EYE));
		RandomSource random = RandomSource.create(20261108L);
		for (int i = 0; i < 100; i++) {
			LootParams params = new LootParams.Builder(level)
					.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
					.create(LootContextParamSets.CHEST);
			for (ItemStack stack : table.getRandomItems(params, random)) {
				counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		for (Item item : wanted) {
			helper.assertTrue(counts.getOrDefault(item, 0) > 0, "정거장 상자에 안 나옴: " + item + " · " + counts);
		}
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 연결 없는 모의 플레이어: 순간 이동 · 효과 · 메시지 패킷을 보내지 않는다 (M10 시험의 모의 플레이어와 같은 방식). */
	private static ServerPlayer offlinePlayer(GameTestHelper helper) {
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "m6-void"), net.minecraft.server.level.ClientInformation.createDefault()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}

			@Override
			public boolean teleportTo(ServerLevel level, double x, double y, double z, java.util.Set<net.minecraft.world.entity.Relative> relatives,
					float yRot, float xRot, boolean resetCamera) {
				setPos(x, y, z);
				return true;
			}

			@Override
			protected void onEffectAdded(net.minecraft.world.effect.MobEffectInstance effect, net.minecraft.world.entity.Entity source) {
			}

			@Override
			public void sendSystemMessage(net.minecraft.network.chat.Component message, boolean overlay) {
			}
		};
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		return player;
	}

	private static void aimAt(net.minecraft.world.entity.projectile.Projectile projectile, VoidCrystal target) {
		Vec3 center = target.position().add(0.0, 0.5, 0.0);
		Vec3 start = center.add(0.0, 0.0, -2.5);
		projectile.setPos(start);
		projectile.shoot(0.0, 0.0, 1.0, 1.5F, 0.0F);
	}

	private static int count(Player player, Item item) {
		int total = 0;
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}
}
