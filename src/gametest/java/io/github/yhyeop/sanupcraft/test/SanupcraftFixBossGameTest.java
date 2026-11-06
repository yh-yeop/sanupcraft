package io.github.yhyeop.sanupcraft.test;

import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.boss.BossCombat;
import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.GuardianAltarBlock;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;

/**
 * 1.5단계 리팩터링 수정 검사 (REFACTOR.md 2-5, 묶음 Boss).
 * 보스는 AI를 끄고 {@code tickPattern}을 직접 불러 결정적으로 돌린다.
 */
public class SanupcraftFixBossGameTest {
	// ---- D1 돌진 방향 잠금 ----

	/** 12틱에 방향이 잠긴다. 잠긴 뒤 목표가 옆으로 움직여도 20틱 돌진에 쓰는 방향은 그대로. */
	@GameTest
	public void chargeLocksDirectionAtTwelveTicks(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		guardian.setNoAi(true);
		Player target = playerAt(helper, guardian.position().add(5.0, 0.0, 0.0));
		guardian.setTarget(target);

		guardian.startPattern(helper.getLevel(), RustyGuardian.Pattern.CHARGE);
		for (int i = 0; i < 11; i++) {
			guardian.tickPattern(helper.getLevel());
		}
		helper.assertTrue(guardian.chargeDirection().lengthSqr() < 1.0E-6, "잠그기 전인데 방향이 정해짐");
		guardian.tickPattern(helper.getLevel());
		helper.assertTrue(guardian.chargeDirection().x > 0.99, "12틱에 +x로 잠기지 않음: " + guardian.chargeDirection());

		place(target, guardian.position().add(0.0, 0.0, 5.0));
		for (int i = 0; i < 9; i++) {
			guardian.tickPattern(helper.getLevel());
		}
		helper.assertTrue(guardian.chargeDirection().x > 0.99, "목표를 따라 방향이 바뀜: " + guardian.chargeDirection());
		helper.assertTrue(Math.abs(guardian.chargeDirection().z) < 0.01, "목표를 따라 방향이 바뀜: " + guardian.chargeDirection());
		helper.succeed();
	}

	// ---- D2 증기 분출 방향 잠금 ----

	/** 10틱에 방향이 잠긴다. 그 뒤 옆으로 돌아간 대상은 분출을 맞지 않는다. */
	@GameTest
	public void blastDirectionIsLockedAtTenTicks(GameTestHelper helper) {
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		warden.setNoAi(true);
		faceSouth(warden);
		Player target = playerAt(helper, warden.position().add(0.0, 0.0, 3.0));
		warden.setTarget(target);

		warden.startPattern(helper.getLevel(), FurnaceWarden.Pattern.STEAM_BLAST);
		for (int i = 0; i < 10; i++) {
			warden.tickPattern(helper.getLevel());
		}
		place(target, warden.position().add(3.0, 0.0, 0.0));
		for (int i = 0; i < 11; i++) {
			warden.tickPattern(helper.getLevel());
		}
		helper.assertTrue(target.getHealth() == target.getMaxHealth(), "옆으로 돌았는데 분출을 맞음: " + target.getHealth());
		helper.succeed();
	}

	/** 대조: 정면에 그대로 서 있으면 잠긴 방향의 분출을 맞는다. */
	@GameTest
	public void blastHitsTargetInLockedDirection(GameTestHelper helper) {
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		warden.setNoAi(true);
		faceSouth(warden);
		Player target = playerAt(helper, warden.position().add(0.0, 0.0, 3.0));
		warden.setTarget(target);

		warden.startPattern(helper.getLevel(), FurnaceWarden.Pattern.STEAM_BLAST);
		for (int i = 0; i < 21; i++) {
			warden.tickPattern(helper.getLevel());
		}
		helper.assertTrue(target.getHealth() < target.getMaxHealth(), "정면에 서 있는데 분출을 안 맞음");
		helper.succeed();
	}

	// ---- 패턴 틱 (입자 · 잠금 경로가 예외 없이 끝까지 도는지) ----

	@GameTest
	public void everyPatternRunsToTheEnd(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		guardian.setNoAi(true);
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(6, 1, 6));
		warden.setNoAi(true);
		Player target = playerAt(helper, guardian.position().add(4.0, 0.0, 0.0));
		guardian.setTarget(target);
		warden.setTarget(target);

		for (RustyGuardian.Pattern pattern : RustyGuardian.Pattern.values()) {
			if (pattern == RustyGuardian.Pattern.NONE) {
				continue;
			}
			guardian.startPattern(helper.getLevel(), pattern);
			for (int i = 0; i < 100 && guardian.pattern() != RustyGuardian.Pattern.NONE; i++) {
				guardian.tickPattern(helper.getLevel());
			}
			helper.assertTrue(guardian.pattern() == RustyGuardian.Pattern.NONE, "수호자 패턴이 안 끝남: " + pattern);
		}
		for (FurnaceWarden.Pattern pattern : FurnaceWarden.Pattern.values()) {
			if (pattern == FurnaceWarden.Pattern.NONE) {
				continue;
			}
			warden.startPattern(helper.getLevel(), pattern);
			for (int i = 0; i < 100 && warden.pattern() != FurnaceWarden.Pattern.NONE; i++) {
				warden.tickPattern(helper.getLevel());
			}
			helper.assertTrue(warden.pattern() == FurnaceWarden.Pattern.NONE, "파수꾼 패턴이 안 끝남: " + pattern);
		}
		helper.succeed();
	}

	// ---- D3 방패 ----

	@GameTest
	public void meleeDisablesShieldForFiveSeconds(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(6, 1, 6));
		helper.assertValueEqual(guardian.getSecondsToDisableBlocking(), 5.0F, "수호자 방패 끄는 시간");
		helper.assertValueEqual(warden.getSecondsToDisableBlocking(), 5.0F, "파수꾼 방패 끄는 시간");
		helper.succeed();
	}

	/** 패턴 타격은 막는 중인 방패를 깨고 그대로 맞힌다. */
	@GameTest
	public void patternStrikeBreaksShield(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(6, 1, 6));
		IronGolem dummy = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(4, 1, 4));
		dummy.setNoAi(true);
		dummy.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHIELD));
		dummy.startUsingItem(InteractionHand.MAIN_HAND);

		// 방패는 올린 지 몇 틱 뒤부터 막는다.
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(dummy.isBlocking(), "방패를 들었는데 막는 중이 아님");
			float before = dummy.getHealth();
			guardian.hitWithRust(helper.getLevel(), dummy, 5.0F, true);
			helper.assertFalse(dummy.isBlocking(), "수호자 패턴 타격 뒤에도 방패를 듦");
			helper.assertTrue(dummy.getHealth() < before, "수호자 패턴 타격이 방패에 막힘");

			dummy.startUsingItem(InteractionHand.MAIN_HAND);
			helper.runAfterDelay(10, () -> {
				helper.assertTrue(dummy.isBlocking(), "방패를 다시 들었는데 막는 중이 아님");
				float again = dummy.getHealth();
				warden.hitWithHeat(helper.getLevel(), dummy, 5.0F, true);
				helper.assertFalse(dummy.isBlocking(), "파수꾼 패턴 타격 뒤에도 방패를 듦");
				helper.assertTrue(dummy.getHealth() < again, "파수꾼 패턴 타격이 방패에 막힘");
				helper.succeed();
			});
		});
	}

	// ---- D5 내려찍기 · 도약 범위 ----

	/** 경고 고리 반경(slamReach) 안은 맞고 바로 밖은 안 맞는다. */
	@GameTest
	public void slamHitsInsideReachOnly(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		guardian.setNoAi(true);
		double reach = guardian.slamReach();
		helper.assertTrue(reach > RustyGuardian.SLAM_RADIUS, "slamReach가 기본 반경보다 크지 않음: " + reach);
		Player inside = playerAt(helper, guardian.position().add(reach - 0.25, 0.0, 0.0));
		Player outside = playerAt(helper, guardian.position().add(-(reach + 0.25), 0.0, 0.0));

		int hits = guardian.slam(helper.getLevel());

		helper.assertValueEqual(hits, 1, "내려찍기에 맞은 수");
		helper.assertTrue(inside.getHealth() < inside.getMaxHealth(), "고리 안인데 안 맞음");
		helper.assertTrue(outside.getHealth() == outside.getMaxHealth(), "고리 밖인데 맞음");
		helper.succeed();
	}

	/** 도약 착지 예측: 앞으로 나아가고, 높이는 시작 높이. 거리는 밀어낸 힘의 약 10배 안팎. */
	@GameTest
	public void predictLandingFollowsAirFriction(GameTestHelper helper) {
		Vec3 start = new Vec3(10.0, 5.0, 20.0);
		Vec3 landing = FurnaceWarden.predictLanding(start, new Vec3(0.9, 1.0, 0.0));
		double travelled = landing.x - start.x;
		helper.assertTrue(landing.y == start.y, "착지 높이가 시작 높이가 아님: " + landing.y);
		helper.assertTrue(Math.abs(landing.z - start.z) < 1.0E-9, "옆으로 샘: " + landing.z);
		helper.assertTrue(travelled > 8.0 && travelled < 10.0, "착지 거리 " + travelled + " (밀어낸 0.9의 약 10배 기대)");
		helper.succeed();
	}

	// ---- D8 거미줄 · 구속 ----

	@GameTest
	public void bossesRefuseSlowness(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(6, 1, 6));
		helper.assertFalse(guardian.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2)), "수호자에게 구속이 걸림");
		helper.assertFalse(warden.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2)), "파수꾼에게 구속이 걸림");
		helper.assertFalse(guardian.hasEffect(MobEffects.SLOWNESS), "수호자에게 구속이 남음");
		helper.assertFalse(warden.hasEffect(MobEffects.SLOWNESS), "파수꾼에게 구속이 남음");
		helper.assertTrue(guardian.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0)), "나약함은 먹혀야 함");
		helper.succeed();
	}

	// ---- D10 제단 공간 ----

	@GameTest
	public void altarNeedsRoomAbove(GameTestHelper helper) {
		BlockPos altar = new BlockPos(3, 1, 3);
		helper.setBlock(altar, ModBoss.GUARDIAN_ALTAR);
		BlockPos abs = helper.absolutePos(altar);
		helper.assertTrue(GuardianAltarBlock.hasRoom(helper.getLevel(), abs, ModBoss.RUSTY_GUARDIAN), "비어 있는데 공간이 없다고 함");
		helper.assertTrue(GuardianAltarBlock.hasRoom(helper.getLevel(), abs, ModBoss.FURNACE_WARDEN), "비어 있는데 파수꾼 공간이 없다고 함");

		helper.setBlock(altar.above(2), Blocks.STONE);
		helper.assertFalse(GuardianAltarBlock.hasRoom(helper.getLevel(), abs, ModBoss.RUSTY_GUARDIAN), "2칸 위가 막혔는데 공간이 있다고 함");
		helper.assertFalse(GuardianAltarBlock.hasRoom(helper.getLevel(), abs, ModBoss.FURNACE_WARDEN), "2칸 위가 막혔는데 파수꾼 공간이 있다고 함");
		helper.succeed();
	}

	// ---- D11 이름표 ----

	@GameTest
	public void nameTagRenamesBossBar(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(6, 1, 6));
		guardian.setCustomName(Component.literal("철덩이"));
		warden.setCustomName(Component.literal("불덩이"));
		helper.assertTrue(guardian.bossBarName().getString().equals("철덩이"), "수호자 보스 바 제목: " + guardian.bossBarName().getString());
		helper.assertTrue(warden.bossBarName().getString().equals("불덩이"), "파수꾼 보스 바 제목: " + warden.bossBarName().getString());
		helper.succeed();
	}

	// ---- A1 짝: 내열 검도 수호자에게 1.5배 ----

	@GameTest
	public void heatproofSwordHurtsGuardianMore(GameTestHelper helper) {
		RustyGuardian a = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(1, 1, 1));
		RustyGuardian b = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(5, 1, 5));
		Player withHeatproof = helper.makeMockPlayer(GameType.SURVIVAL);
		withHeatproof.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HeatproofGear.HEATPROOF_SWORD));
		Player withIron = helper.makeMockPlayer(GameType.SURVIVAL);
		withIron.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));

		a.hurtServer(helper.getLevel(), a.damageSources().playerAttack(withHeatproof), 10.0F);
		b.hurtServer(helper.getLevel(), b.damageSources().playerAttack(withIron), 10.0F);
		float heatproofDamage = a.getMaxHealth() - a.getHealth();
		float ironDamage = b.getMaxHealth() - b.getHealth();
		helper.assertTrue(heatproofDamage > ironDamage * 1.3F, "내열 검 " + heatproofDamage + ", 철 검 " + ironDamage);
		helper.succeed();
	}

	// ---- 도우미 ----

	private static void place(Entity entity, Vec3 pos) {
		entity.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
	}

	/** 월드에 들어가 있는 모의 플레이어 (패턴 판정은 월드의 플레이어만 찾는다). */
	private static Player playerAt(GameTestHelper helper, Vec3 pos) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		place(player, pos);
		helper.getLevel().addFreshEntity(player);
		return player;
	}

	/** 남쪽(+z)을 바라보게 한다. */
	private static void faceSouth(FurnaceWarden warden) {
		warden.setYRot(0.0F);
		warden.setYHeadRot(0.0F);
		warden.setYBodyRot(0.0F);
	}

	// ---- 3차 2단계 (REFACTOR.md 7-8, 7-10) ----

	/** 안내 쿨다운은 플레이어마다 따로: 같은 사람은 200틱 안에 한 번, 다른 사람은 바로, 200틱이 지나면 다시. */
	@GameTest
	public void bossWarningCooldownPerPlayer(GameTestHelper helper) {
		BossCombat.PlayerCooldown cooldown = new BossCombat.PlayerCooldown(200);
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		helper.assertTrue(cooldown.tryUse(a, 1000L), "처음인데 막힘");
		helper.assertFalse(cooldown.tryUse(a, 1000L), "같은 시각 두 번째가 통과");
		helper.assertTrue(cooldown.tryUse(b, 1000L), "다른 사람이 막힘");
		helper.assertFalse(cooldown.tryUse(a, 1199L), "199틱 뒤에 통과");
		helper.assertTrue(cooldown.tryUse(a, 1200L), "200틱 뒤에 막힘");
		helper.succeed();
	}
}
