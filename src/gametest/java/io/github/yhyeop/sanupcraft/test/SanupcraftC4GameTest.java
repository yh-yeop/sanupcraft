package io.github.yhyeop.sanupcraft.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.PartyBossMonster;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.boss.gear.BossGearDrops;
import io.github.yhyeop.sanupcraft.boss.gear.FurnaceShieldItem;
import io.github.yhyeop.sanupcraft.boss.gear.ModBossGear;
import io.github.yhyeop.sanupcraft.boss.gear.RustyHammerItem;
import io.github.yhyeop.sanupcraft.boss.gear.VoidBolt;
import io.github.yhyeop.sanupcraft.boss.gear.VoidStaffItem;
import io.github.yhyeop.sanupcraft.boss.phase.BossHazard;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.effect.ModEffects;
import io.github.yhyeop.sanupcraft.end.ModEnd;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.server.BossRecords;

/** 확장 3 C4: 보스 2페이즈(한 번 · 저장 · 새 패턴의 효과 · 크리에이티브 제외), 전용 장비 보상(확률 설정), 장비 효과 셋, 실테스트 꾸러미. */
public class SanupcraftC4GameTest {
	// ---- 2페이즈 ----

	/** 체력이 절반 이하가 되는 순간 한 번만 들어가고, 체력이 차도 2페이즈 그대로. 다음 패턴은 새 패턴. */
	@GameTest
	public void phaseTwoTriggersOnceAtHalfHealth(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		boss.setNoAi(true);
		helper.assertFalse(boss.isPhaseTwo(), "처음부터 2페이즈");
		boss.setHealth(boss.getMaxHealth() * 0.51F);
		helper.assertFalse(boss.checkPhaseTwo(level), "51%에서 2페이즈로 들어감");
		helper.assertValueEqual(boss.upcomingPattern(), RustyGuardian.Pattern.CHARGE, "1페이즈 첫 패턴");
		boss.setHealth(boss.getMaxHealth() * PartyBossMonster.PHASE_TWO_HEALTH);
		helper.assertTrue(boss.checkPhaseTwo(level), "50%에서 2페이즈로 안 들어감");
		helper.assertTrue(boss.isPhaseTwo(), "2페이즈 표시가 없음");
		helper.assertValueEqual(boss.upcomingPattern(), RustyGuardian.Pattern.RUST_STORM, "2페이즈 첫 패턴");
		helper.assertFalse(boss.checkPhaseTwo(level), "2페이즈에 두 번 들어감");
		boss.setHealth(boss.getMaxHealth());
		helper.assertFalse(boss.checkPhaseTwo(level), "체력을 채우고 다시 들어감");
		helper.assertTrue(boss.isPhaseTwo(), "체력을 채우니 1페이즈로 돌아감");
		helper.assertValueEqual(boss.phaseTwoEntries(), 1, "2페이즈에 들어간 횟수");

		// 파수꾼 · 거상도 새 패턴부터
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(7, 1, 7));
		warden.setNoAi(true);
		warden.setHealth(warden.getMaxHealth() * 0.3F);
		helper.assertTrue(warden.checkPhaseTwo(level), "파수꾼이 2페이즈로 안 들어감");
		helper.assertValueEqual(warden.upcomingPattern(), FurnaceWarden.Pattern.LAVA_PILLARS, "파수꾼 2페이즈 첫 패턴");
		VoidColossus colossus = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(3, 1, 7));
		colossus.setNoAi(true);
		helper.assertValueEqual(colossus.upcomingPattern(), VoidColossus.Pattern.VOLLEY, "거상 1페이즈 첫 패턴");
		colossus.setHealth(colossus.getMaxHealth() * 0.3F);
		helper.assertTrue(colossus.checkPhaseTwo(level), "거상이 2페이즈로 안 들어감");
		helper.assertValueEqual(colossus.upcomingPattern(), VoidColossus.Pattern.RIFT, "거상 2페이즈 첫 패턴");

		for (String id : new String[] {"rusty_guardian_phase_two", "furnace_warden_phase_two", "void_colossus_phase_two", "boss_gear"}) {
			helper.assertTrue(level.getServer().getAdvancements().get(Sanupcraft.id(id)) != null, "발전 과제가 없음: " + id);
		}
		boss.discard();
		warden.discard();
		colossus.discard();
		helper.succeed();
	}

	/** 2페이즈는 저장된다: 읽은 보스는 체력이 차 있어도 2페이즈이고 다시 들어가지 않는다. 예전 저장(키 없음)은 체력으로 정한다. */
	@GameTest
	public void phaseTwoSurvivesSaveAndLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		boss.setNoAi(true);
		boss.setHealth(boss.getMaxHealth() * 0.4F);
		helper.assertTrue(boss.checkPhaseTwo(level), "2페이즈로 안 들어감");
		boss.setHealth(boss.getMaxHealth());
		CompoundTag tag = save(helper, boss);
		helper.assertTrue(tag.getBooleanOr("phase_two", false), "저장 태그에 phase_two가 없음");

		RustyGuardian loaded = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(6, 1, 6));
		loaded.setNoAi(true);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertTrue(loaded.isPhaseTwo(), "읽은 보스가 1페이즈");
		helper.assertValueEqual(loaded.getHealth(), loaded.getMaxHealth(), "읽은 체력");
		loaded.setHealth(loaded.getMaxHealth() * 0.2F);
		helper.assertFalse(loaded.checkPhaseTwo(level), "읽은 보스가 2페이즈에 다시 들어감");
		helper.assertValueEqual(loaded.phaseTwoEntries(), 0, "읽은 보스가 2페이즈에 들어간 횟수");

		// 예전 저장: 키가 없으면 체력이 절반 아래인지로 (화남을 이미 알렸는지와 같은 뜻)
		RustyGuardian fresh = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 6));
		fresh.setNoAi(true);
		fresh.setHealth(fresh.getMaxHealth() * 0.4F);
		CompoundTag old = save(helper, fresh);
		old.remove("phase_two");
		RustyGuardian oldLow = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(6, 1, 2));
		oldLow.setNoAi(true);
		oldLow.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), old));
		helper.assertTrue(oldLow.isPhaseTwo(), "예전 저장(체력 40%)이 1페이즈");
		fresh.setHealth(fresh.getMaxHealth() * 0.8F);
		CompoundTag oldHigh = save(helper, fresh);
		oldHigh.remove("phase_two");
		oldLow.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), oldHigh));
		helper.assertFalse(oldLow.isPhaseTwo(), "예전 저장(체력 80%)이 2페이즈");
		for (RustyGuardian b : List.of(boss, loaded, fresh, oldLow)) {
			b.discard();
		}
		helper.succeed();
	}

	/** 녹 폭풍: 장판 자리는 서바이벌 플레이어 발밑 (크리에이티브는 고르지 않음), 30틱 뒤 장판 다섯, 안에 서면 녹 II. 크리에이티브는 장판 안이어도 안 걸린다. */
	@GameTest(environment = "sanupcraft-test:alone_c4_1")
	public void rustStormRustsSurvivalNotCreative(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(4, 1, 4));
		boss.setNoAi(true);
		Player survival = playerAt(helper, GameType.SURVIVAL, boss.position().add(5.0, 0.0, 0.0));
		Player creative = playerAt(helper, GameType.CREATIVE, boss.position().add(-5.0, 0.0, 0.0));
		boss.startPattern(level, RustyGuardian.Pattern.RUST_STORM);
		List<Vec3> zones = boss.rustZones();
		helper.assertValueEqual(zones.size(), RustyGuardian.RUST_STORM_ZONES, "장판 수");
		helper.assertTrue(zones.contains(survival.position()), "서바이벌 발밑에 장판이 없음");
		helper.assertFalse(zones.contains(creative.position()), "크리에이티브 발밑에 장판을 고름");
		for (int i = 0; i < RustyGuardian.RUST_STORM_WINDUP - 1; i++) {
			boss.tickPattern(level);
		}
		helper.assertTrue(hazards(helper, BossHazard.Kind.RUST_PUDDLE).isEmpty(), "예고 중에 장판이 깔림");
		boss.tickPattern(level);
		List<BossHazard> puddles = hazards(helper, BossHazard.Kind.RUST_PUDDLE);
		helper.assertValueEqual(puddles.size(), RustyGuardian.RUST_STORM_ZONES, "깔린 장판 수");
		puddles.forEach(p -> p.affect(level));
		assertEffect(helper, survival, ModEffects.RUST, BossHazard.PUDDLE_RUST_LEVEL, "서바이벌 녹");

		BossHazard underCreative = BossHazard.spawn(level, BossHazard.Kind.RUST_PUDDLE, creative.position(), 100, boss);
		helper.assertValueEqual(underCreative.affect(level), 0, "크리에이티브 발밑 장판이 건 수");
		helper.assertFalse(creative.hasEffect(ModEffects.RUST), "크리에이티브에 녹이 걸림");
		// 강철 한 벌은 장판에서도 녹 면역
		Player steel = playerAt(helper, GameType.SURVIVAL, creative.position().add(0.0, 0.0, 1.0));
		wear(steel, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		underCreative.affect(level);
		helper.assertFalse(steel.hasEffect(ModEffects.RUST), "강철 한 벌인데 장판 녹이 걸림");

		for (int i = 0; i < 20 && boss.pattern() != RustyGuardian.Pattern.NONE; i++) {
			boss.tickPattern(level);
		}
		helper.assertValueEqual(boss.pattern(), RustyGuardian.Pattern.NONE, "녹 폭풍이 안 끝남");
		cleanUp(helper, boss, survival, creative, steel);
		helper.succeed();
	}

	/** 용암 기둥: 서바이벌 발밑에만 원 (최대 4), 솟으면 피해 6 + 과열 II. 크리에이티브는 기둥 안이어도 멀쩡. */
	@GameTest(environment = "sanupcraft-test:alone_c4_2")
	public void lavaPillarsBurnSurvivalNotCreative(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FurnaceWarden boss = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(4, 1, 4));
		boss.setNoAi(true);
		Player survival = playerAt(helper, GameType.SURVIVAL, boss.position().add(5.0, 0.0, 0.0));
		Player creative = playerAt(helper, GameType.CREATIVE, boss.position().add(-5.0, 0.0, 0.0));
		boss.startPattern(level, FurnaceWarden.Pattern.LAVA_PILLARS);
		helper.assertValueEqual(boss.pillars(), List.of(survival.position()), "기둥 자리");
		boss.setPillars(List.of(survival.position(), creative.position()));
		float before = survival.getHealth();
		helper.assertValueEqual(boss.eruptPillars(level), 1, "기둥에 맞은 수");
		helper.assertTrue(survival.getHealth() < before, "서바이벌이 안 다침");
		assertEffect(helper, survival, ModEffects.OVERHEAT, FurnaceWarden.PILLAR_OVERHEAT_LEVEL, "서바이벌 과열");
		helper.assertValueEqual(creative.getHealth(), creative.getMaxHealth(), "크리에이티브 체력");
		helper.assertFalse(creative.hasEffect(ModEffects.OVERHEAT), "크리에이티브에 과열이 걸림");

		// 예고 → 솟음 → 끝이 패턴 틱으로도 돈다
		boss.startPattern(level, FurnaceWarden.Pattern.LAVA_PILLARS);
		for (int i = 0; i < 60 && boss.pattern() != FurnaceWarden.Pattern.NONE; i++) {
			boss.tickPattern(level);
		}
		helper.assertValueEqual(boss.pattern(), FurnaceWarden.Pattern.NONE, "용암 기둥이 안 끝남");
		cleanUp(helper, boss, survival, creative);
		helper.succeed();
	}

	/** 공허 균열: 서바이벌 근처에 셋, 닿으면 공허 침식 II, 둘레 플레이어를 조금 끌어당긴다. 크리에이티브는 닿아도 · 둘레여도 그대로. */
	@GameTest(environment = "sanupcraft-test:alone_c4_3")
	public void voidRiftsErodeSurvivalNotCreative(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VoidColossus boss = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(4, 1, 4));
		boss.setNoAi(true);
		Player survival = playerAt(helper, GameType.SURVIVAL, boss.position().add(6.0, 0.0, 0.0));
		Player creative = playerAt(helper, GameType.CREATIVE, boss.position().add(-6.0, 0.0, 0.0));
		boss.startPattern(level, VoidColossus.Pattern.RIFT);
		List<Vec3> spots = boss.riftSpots();
		helper.assertValueEqual(spots.size(), VoidColossus.RIFTS, "균열 수");
		for (Vec3 spot : spots) {
			helper.assertTrue(spot.distanceTo(survival.position()) <= 2.6, "균열이 서바이벌 근처가 아님: " + spot);
		}
		boss.setRiftSpots(List.of(survival.position(), creative.position()));
		List<BossHazard> rifts = boss.openRifts(level);
		helper.assertValueEqual(rifts.size(), 2, "열린 균열 수");
		int eroded = rifts.stream().mapToInt(r -> r.affect(level)).sum();
		helper.assertValueEqual(eroded, 1, "침식이 걸린 수");
		assertEffect(helper, survival, ModEffects.VOID_EROSION, BossHazard.RIFT_EROSION_LEVEL, "서바이벌 침식");
		helper.assertFalse(creative.hasEffect(ModEffects.VOID_EROSION), "크리에이티브에 침식이 걸림");

		// 둘레 3칸 플레이어는 균열 쪽으로 끌려간다 (크리에이티브는 안 끌림)
		Player near = playerAt(helper, GameType.SURVIVAL, survival.position().add(0.0, 0.0, 3.0));
		Player creativeNear = playerAt(helper, GameType.CREATIVE, creative.position().add(0.0, 0.0, 3.0));
		near.setDeltaMovement(Vec3.ZERO);
		creativeNear.setDeltaMovement(Vec3.ZERO);
		rifts.forEach(r -> r.pull(level));
		helper.assertTrue(near.getDeltaMovement().z < 0.0, "둘레 플레이어가 균열 쪽으로 안 끌림: " + near.getDeltaMovement());
		helper.assertValueEqual(creativeNear.getDeltaMovement(), Vec3.ZERO, "크리에이티브가 끌림");

		for (int i = 0; i < 60 && boss.pattern() != VoidColossus.Pattern.NONE; i++) {
			boss.tickPattern(level);
		}
		helper.assertValueEqual(boss.pattern(), VoidColossus.Pattern.NONE, "공허 균열이 안 끝남");
		cleanUp(helper, boss, survival, creative, near, creativeNear);
		helper.succeed();
	}

	/** 주인 보스가 사라지면 위험 구역도 곧 사라진다. 수명이 다해도 사라진다. */
	@GameTest(environment = "sanupcraft-test:alone_c4_4", maxTicks = 100)
	public void hazardsVanishWithOwnerOrTime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(4, 1, 4));
		boss.setNoAi(true);
		BossHazard owned = BossHazard.spawn(level, BossHazard.Kind.RUST_PUDDLE, boss.position().add(3.0, 0.0, 0.0), 1000, boss);
		BossHazard shortLived = BossHazard.spawn(level, BossHazard.Kind.VOID_RIFT, boss.position().add(-3.0, 0.0, 0.0), 5, null);
		boss.discard();
		helper.succeedWhen(() -> {
			helper.assertTrue(owned.isRemoved(), "주인이 없는데 장판이 남음");
			helper.assertTrue(shortLived.isRemoved(), "수명이 다했는데 균열이 남음");
		});
	}

	// ---- 전용 장비 보상 ----

	/** 확률을 1로 두면 기여자가 보스마다 그 장비를 받고, 0이면 안 받는다. 기본 확률 = 10% + 단계마다 5%. */
	@GameTest(environment = "sanupcraft-test:alone_c4_5")
	public void bossGearDropsToContributors(GameTestHelper helper) {
		helper.assertValueEqual(BossGearDrops.chance(0), 0.10, "기본 확률");
		helper.assertTrue(Math.abs(BossGearDrops.chance(2) - 0.20) < 1.0E-9, "단계 2 확률: " + BossGearDrops.chance(2));
		helper.assertTrue(SanupcraftConfig.defaults().toJson().contains("chancePerTier"), "설정 파일에 bossGear 구역이 없음");
		SanupcraftConfig before = SanupcraftConfig.current();
		SanupcraftConfig forced = SanupcraftConfig.defaults();
		forced.bossGearChance = 1.0;
		SanupcraftConfig.use(forced);
		try {
			ServerPlayer a = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
			ServerPlayer b = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
			ServerPlayer c = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
			kill(helper, helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2)), a);
			kill(helper, helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(6, 1, 2)), b);
			kill(helper, helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(4, 1, 6)), c);
			helper.assertValueEqual(count(a, ModBossGear.RUSTY_HAMMER), 1, "수호자 기여자의 녹슨 망치");
			helper.assertValueEqual(count(b, ModBossGear.FURNACE_SHIELD), 1, "파수꾼 기여자의 화구 방패");
			helper.assertValueEqual(count(c, ModBossGear.VOID_STAFF), 1, "거상 기여자의 공허 지팡이");

			forced.bossGearChance = 0.0;
			forced.bossGearChancePerTier = 0.0;
			ServerPlayer d = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
			kill(helper, helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 6)), d);
			helper.assertValueEqual(count(d, ModBossGear.RUSTY_HAMMER), 0, "확률 0인데 장비를 받음");
		} finally {
			SanupcraftConfig.use(before);
		}
		BossRecords records = BossRecords.get(helper.getLevel().getServer());
		for (String id : new String[] {"sanupcraft:rusty_guardian", "sanupcraft:furnace_warden", "sanupcraft:void_colossus"}) {
			records.clear(id);
		}
		helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, helper.getBounds().inflate(16.0)).forEach(orb -> orb.discard());
		helper.succeed();
	}

	// ---- 장비 효과 ----

	/** 녹슨 망치: 공격력 9 · 매우 느림, 맞은 상대는 녹 II. 모듈 칸 2 표시. */
	@GameTest
	public void rustyHammerRustsOnHit(GameTestHelper helper) {
		ItemStack hammer = new ItemStack(ModBossGear.RUSTY_HAMMER);
		ItemAttributeModifiers modifiers = hammer.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		double damage = 1.0; // 플레이어 기본 공격력
		double speed = 4.0;
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
				damage += entry.modifier().amount();
			} else if (entry.attribute().is(Attributes.ATTACK_SPEED)) {
				speed += entry.modifier().amount();
			}
		}
		helper.assertTrue(Math.abs(damage - RustyHammerItem.DAMAGE) < 1.0E-6, "망치 공격력: " + damage);
		helper.assertTrue(speed < 1.0, "망치가 느리지 않음: " + speed);
		IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(3, 1, 3));
		golem.setNoAi(true);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		hammer.hurtEnemy(golem, player);
		assertEffect(helper, golem, ModEffects.RUST, RustyHammerItem.RUST_LEVEL, "망치 녹");

		for (Item gear : new Item[] {ModBossGear.RUSTY_HAMMER, ModBossGear.FURNACE_SHIELD, ModBossGear.VOID_STAFF}) {
			helper.assertValueEqual(ModBossGear.moduleSlots(new ItemStack(gear)), ModBossGear.MODULE_SLOTS, "모듈 칸: " + gear);
		}
		helper.assertValueEqual(ModBossGear.moduleSlots(new ItemStack(SteelGear.STEEL_SWORD)), 0, "보스 장비가 아닌 것의 모듈 칸");
		helper.assertValueEqual(ModBossGear.MODULE_SLOTS, 2, "보스 장비 모듈 칸 수");
		golem.discard();
		helper.succeed();
	}

	/** 화구 방패로 근접 공격을 막으면 때린 쪽이 과열 I, 막은 쪽은 안 다친다. 때린 엔티티가 없는 피해는 아무 일 없다. */
	@GameTest
	public void furnaceShieldBurnsMeleeAttacker(GameTestHelper helper) {
		IronGolem defender = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(3, 1, 2));
		IronGolem attacker = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(3, 1, 5));
		defender.setNoAi(true);
		attacker.setNoAi(true);
		defender.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBossGear.FURNACE_SHIELD));
		defender.startUsingItem(InteractionHand.MAIN_HAND);
		helper.runAfterDelay(10, () -> {
			face(defender, attacker);
			helper.assertTrue(defender.isBlocking(), "화구 방패를 들었는데 막는 중이 아님");
			float before = defender.getHealth();
			defender.hurtServer(helper.getLevel(), defender.damageSources().mobAttack(attacker), 6.0F);
			helper.assertValueEqual(defender.getHealth(), before, "막았는데 다침");
			assertEffect(helper, attacker, ModEffects.OVERHEAT, FurnaceShieldItem.OVERHEAT_LEVEL, "때린 쪽 과열");
			helper.assertFalse(defender.hasEffect(ModEffects.OVERHEAT), "막은 쪽에 과열이 걸림");
			helper.assertFalse(FurnaceShieldItem.burnAttacker(defender, defender.damageSources().magic()), "때린 엔티티가 없는데 과열을 검");
			defender.discard();
			attacker.discard();
			helper.succeed();
		});
	}

	/** 공허 지팡이: 쏘면 쿨다운 30틱 · 내구도 1, 쿨다운 중에는 안 나간다. 탄에 맞으면 피해 + 공허 침식 I. */
	@GameTest(maxTicks = 60)
	public void voidStaffBoltErodes(GameTestHelper helper) {
		IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(3, 1, 5));
		golem.setNoAi(true);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 1.5));
		Vec3 to = golem.position().add(0.0, 1.2, 0.0).subtract(at.add(0.0, player.getEyeHeight(), 0.0));
		float yaw = (float) (Math.atan2(to.z, to.x) * 180.0 / Math.PI) - 90.0F;
		float pitch = (float) -(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180.0 / Math.PI);
		player.snapTo(at.x, at.y, at.z, yaw, pitch);
		helper.getLevel().addFreshEntity(player);
		ItemStack staff = new ItemStack(ModBossGear.VOID_STAFF);
		player.setItemInHand(InteractionHand.MAIN_HAND, staff);
		float before = golem.getHealth();
		helper.assertTrue(VoidStaffItem.fire(helper.getLevel(), player, InteractionHand.MAIN_HAND), "첫 발이 안 나감");
		helper.assertTrue(player.getCooldowns().isOnCooldown(staff), "쿨다운이 안 걸림");
		helper.assertFalse(VoidStaffItem.fire(helper.getLevel(), player, InteractionHand.MAIN_HAND), "쿨다운 중에 나감");
		helper.assertValueEqual(staff.getDamageValue(), 1, "지팡이 내구도");
		helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(VoidBolt.class, helper.getBounds().inflate(4.0)).size(), 1, "날아가는 탄 수");
		helper.succeedWhen(() -> {
			helper.assertTrue(golem.getHealth() < before, "탄에 맞은 골렘이 안 다침");
			assertEffect(helper, golem, ModEffects.VOID_EROSION, VoidBolt.EROSION_LEVEL, "탄 침식");
			player.discard();
			golem.discard();
		});
	}

	/** 실테스트 단계 'bossgear'는 장비 셋과 소환석 셋을 준다. */
	@GameTest
	public void playtestBossGearKit(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.BOSSGEAR);
		for (Item item : new Item[] {ModBossGear.RUSTY_HAMMER, ModBossGear.FURNACE_SHIELD, ModBossGear.VOID_STAFF,
				ModBoss.GUARDIAN_SUMMONER, ModBoss.WARDEN_SUMMONER, ModEnd.COLOSSUS_SUMMONER}) {
			helper.assertTrue(count(player, item) > 0, "꾸러미에 없음: " + item);
		}
		helper.succeed();
	}

	// ---- 도우미 ----

	private static CompoundTag save(GameTestHelper helper, PartyBossMonster boss) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		boss.saveWithoutId(output);
		return output.buildResult();
	}

	/** 월드에 들어가 있는 모의 플레이어 (패턴 판정은 월드의 플레이어만 찾는다). */
	private static Player playerAt(GameTestHelper helper, GameType type, Vec3 pos) {
		Player player = helper.makeMockPlayer(type);
		type.updatePlayerAbilities(player.getAbilities());
		player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		return player;
	}

	private static List<BossHazard> hazards(GameTestHelper helper, BossHazard.Kind kind) {
		return helper.getLevel().getEntitiesOfClass(BossHazard.class, helper.getBounds().inflate(16.0), h -> h.kind() == kind && h.isAlive());
	}

	private static void assertEffect(GameTestHelper helper, LivingEntity entity, Holder<MobEffect> effect, int level, String what) {
		MobEffectInstance instance = entity.getEffect(effect);
		helper.assertTrue(instance != null, what + ": 효과가 없음");
		helper.assertValueEqual(instance.getAmplifier(), level - 1, what + " 단계");
	}

	private static void wear(LivingEntity entity, Item head, Item chest, Item legs, Item feet) {
		entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
		entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
		entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
		entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
	}

	/** {@code who}가 {@code target} 쪽을 보게 한다 (방패는 바라보는 쪽만 막는다). */
	private static void face(LivingEntity who, LivingEntity target) {
		Vec3 to = target.position().subtract(who.position());
		float yaw = (float) (Math.atan2(to.z, to.x) * 180.0 / Math.PI) - 90.0F;
		who.setYRot(yaw);
		who.setYHeadRot(yaw);
		who.setYBodyRot(yaw);
	}

	private static void kill(GameTestHelper helper, PartyBossMonster boss, ServerPlayer killer) {
		boss.setNoAi(true);
		boss.party().scale(boss, 1);
		boss.hurtServer(helper.getLevel(), boss.damageSources().playerAttack(killer), 100000.0F);
		helper.assertTrue(boss.isDeadOrDying(), "보스가 죽지 않음: " + boss);
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

	private static void cleanUp(GameTestHelper helper, LivingEntity... entities) {
		for (LivingEntity entity : entities) {
			entity.discard();
		}
		helper.getLevel().getEntitiesOfClass(BossHazard.class, helper.getBounds().inflate(16.0)).forEach(h -> h.discard());
	}
}
