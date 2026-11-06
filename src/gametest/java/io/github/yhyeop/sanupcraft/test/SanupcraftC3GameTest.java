package io.github.yhyeop.sanupcraft.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.BossParty;
import io.github.yhyeop.sanupcraft.boss.BossSummonerItem;
import io.github.yhyeop.sanupcraft.boss.BossTier;
import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.server.BossRecords;

/** C3 강화 소환석: 작업대 강화(+3까지), 보스 체력 · 피해 배율, 보상(핵 · 심장 + 업그레이드), 저장, 보스 기록의 단계. */
public class SanupcraftC3GameTest {
	// ---- 작업대 강화 ----

	/** 수호자 소환석 + 핵 조각 4개 → 단계 +1씩, +3에서 멈춘다. 재료 수가 틀리거나 다른 물건이 섞이면 안 맞는다. */
	@GameTest
	public void craftingRaisesGuardianSummonerTierAndCaps(GameTestHelper helper) {
		ItemStack summoner = new ItemStack(ModBoss.GUARDIAN_SUMMONER);
		helper.assertValueEqual(BossTier.of(summoner), 0, "처음 단계");
		for (int expected = 1; expected <= BossTier.MAX; expected++) {
			ItemStack result = craft(helper, summoner, new ItemStack(ModItems.CORE_SHARD), 4);
			helper.assertTrue(!result.isEmpty(), "단계 " + (expected - 1) + " 소환석이 강화되지 않음");
			helper.assertTrue(result.is(ModBoss.GUARDIAN_SUMMONER), "결과가 소환석이 아님: " + result);
			helper.assertValueEqual(result.getCount(), 1, "결과 개수");
			helper.assertValueEqual(BossTier.of(result), expected, "강화 뒤 단계");
			summoner = result;
		}
		helper.assertTrue(craft(helper, summoner, new ItemStack(ModItems.CORE_SHARD), 4).isEmpty(), "+3이 더 강화됨");
		// 재료 수가 모자라거나 넘치면 안 된다
		ItemStack plain = new ItemStack(ModBoss.GUARDIAN_SUMMONER);
		helper.assertTrue(craft(helper, plain, new ItemStack(ModItems.CORE_SHARD), 3).isEmpty(), "핵 조각 3개로 강화됨");
		helper.assertTrue(craft(helper, plain, new ItemStack(ModItems.CORE_SHARD), 5).isEmpty(), "핵 조각 5개로 강화됨");
		// 수호자 소환석에 심장은 안 맞는다
		helper.assertTrue(craft(helper, plain, new ItemStack(ModItems.WARDEN_HEART), 1).isEmpty(), "수호자 소환석이 심장으로 강화됨");
		helper.succeed();
	}

	/** 파수꾼 소환석 + 파수꾼의 심장 1개 → +1씩, +3에서 멈춘다. */
	@GameTest
	public void craftingRaisesWardenSummonerTierAndCaps(GameTestHelper helper) {
		ItemStack summoner = new ItemStack(ModBoss.WARDEN_SUMMONER);
		for (int expected = 1; expected <= BossTier.MAX; expected++) {
			ItemStack result = craft(helper, summoner, new ItemStack(ModItems.WARDEN_HEART), 1);
			helper.assertTrue(result.is(ModBoss.WARDEN_SUMMONER), "결과가 파수꾼 소환석이 아님: " + result);
			helper.assertValueEqual(BossTier.of(result), expected, "강화 뒤 단계");
			summoner = result;
		}
		helper.assertTrue(craft(helper, summoner, new ItemStack(ModItems.WARDEN_HEART), 1).isEmpty(), "+3이 더 강화됨");
		helper.assertTrue(craft(helper, new ItemStack(ModBoss.WARDEN_SUMMONER), new ItemStack(ModItems.CORE_SHARD), 4).isEmpty(), "파수꾼 소환석이 핵 조각으로 강화됨");
		helper.succeed();
	}

	/** 단계가 다른 소환석은 쌓이지 않고, 단계 0이면 컴포넌트가 없어 강화 전과 같이 쌓인다. 이름 · 데이터 저장도 본다. */
	@GameTest
	public void tierIsAnItemComponent(GameTestHelper helper) {
		ItemStack plain = new ItemStack(ModBoss.GUARDIAN_SUMMONER);
		ItemStack plus2 = BossTier.stack(ModBoss.GUARDIAN_SUMMONER, 2);
		helper.assertFalse(ItemStack.isSameItemSameComponents(plain, plus2), "단계가 다른데 같이 쌓임");
		ItemStack back = BossTier.stack(ModBoss.GUARDIAN_SUMMONER, 2);
		BossTier.set(back, 0);
		helper.assertTrue(ItemStack.isSameItemSameComponents(plain, back), "단계 0이 강화 전과 다름");
		helper.assertValueEqual(BossTier.of(BossTier.stack(ModBoss.GUARDIAN_SUMMONER, 9)), BossTier.MAX, "단계 상한");

		var json = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, plus2).getOrThrow();
		ItemStack loaded = ItemStack.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		helper.assertValueEqual(BossTier.of(loaded), 2, "저장했다 읽은 단계");

		Component name = plus2.getHoverName();
		helper.assertTrue(name.getContents() instanceof TranslatableContents contents && contents.getKey().equals("item.sanupcraft.boss_tier.name"),
				"소환석 이름에 +N이 안 붙음: " + name);
		helper.assertTrue(plain.getHoverName().getContents() instanceof TranslatableContents plainContents
				&& plainContents.getKey().equals("item.sanupcraft.guardian_summoner"), "강화 전 소환석 이름이 바뀜");
		helper.assertTrue(ModBoss.GUARDIAN_SUMMONER instanceof BossSummonerItem && ModBoss.WARDEN_SUMMONER instanceof BossSummonerItem, "소환석이 BossSummonerItem이 아님");
		helper.succeed();
	}

	// ---- 체력 · 피해 배율 ----

	/** 인원 1명: 단계 2 수호자 체력은 단계 0의 2배. 인원 배율 위에 곱한다 (2명 + 단계 2 = 1.5 x 2.0). */
	@GameTest
	public void tierScalesHealthOnTopOfParty(GameTestHelper helper) {
		RustyGuardian plain = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		plain.setNoAi(true);
		plain.party().scale(plain, 1);
		RustyGuardian tier2 = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(5, 1, 5));
		tier2.setNoAi(true);
		tier2.party().scale(tier2, 1);
		tier2.party().setTier(tier2, 2);
		helper.assertValueEqual(plain.getMaxHealth(), (float) RustyGuardian.MAX_HEALTH, "단계 0 체력");
		helper.assertValueEqual(tier2.getMaxHealth(), plain.getMaxHealth() * 2.0F, "단계 2 최대 체력");
		helper.assertValueEqual(tier2.getHealth(), tier2.getMaxHealth(), "단계 2 현재 체력");

		tier2.party().scale(tier2, 2);
		tier2.party().setTier(tier2, 2);
		helper.assertValueEqual(tier2.getMaxHealth(), (float) (RustyGuardian.MAX_HEALTH * 1.5 * 2.0), "2명 + 단계 2 체력");
		// 단계를 다시 걸어도 수정자가 쌓이지 않는다. 0으로 되돌리면 인원 배율만 남는다
		tier2.party().setTier(tier2, 2);
		helper.assertValueEqual(tier2.getMaxHealth(), (float) (RustyGuardian.MAX_HEALTH * 1.5 * 2.0), "같은 단계를 다시 걸어도 같은 체력");
		tier2.party().setTier(tier2, 0);
		helper.assertValueEqual(tier2.getMaxHealth(), (float) (RustyGuardian.MAX_HEALTH * 1.5), "단계 0으로 되돌린 체력");
		// 바닐라 최대 체력 속성 한도(1024)에서 멈춘다: 3명 x 단계 3 = 1500 -> 1024
		tier2.party().scale(tier2, 3);
		tier2.party().setTier(tier2, 3);
		helper.assertValueEqual(tier2.getMaxHealth(), 1024.0F, "최대 체력 속성 한도");
		helper.succeed();
	}

	/** 공격력 속성에 단계 수정자(x(1 + 0.25 단계))가 붙고, 패턴 피해도 같은 배율로 아프다. 파수꾼도 같다. */
	@GameTest
	public void tierScalesDamage(GameTestHelper helper) {
		RustyGuardian plain = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(1, 1, 1));
		RustyGuardian tier2 = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(5, 1, 1));
		plain.setNoAi(true);
		tier2.setNoAi(true);
		tier2.party().setTier(tier2, 2);
		double base = plain.getAttributeValue(Attributes.ATTACK_DAMAGE);
		helper.assertValueEqual(tier2.getAttributeValue(Attributes.ATTACK_DAMAGE), base * 1.5, "단계 2 공격력 속성");
		helper.assertValueEqual(tier2.party().outgoingDamageScale(), 1.5, "단계 2 패턴 피해 배율");
		helper.assertValueEqual(plain.party().outgoingDamageScale(), 1.0, "단계 0 패턴 피해 배율");

		// 패턴 타격이 실제로 1.5배 아픈가 (갑옷 없는 쇠골렘 둘에게 같은 타격)
		IronGolem golemA = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(1, 1, 5));
		IronGolem golemB = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(5, 1, 5));
		golemA.setNoAi(true);
		golemB.setNoAi(true);
		float healthA = golemA.getHealth();
		float healthB = golemB.getHealth();
		plain.hitWithRust(helper.getLevel(), golemA, 8.0F, true);
		tier2.hitWithRust(helper.getLevel(), golemB, 8.0F, true);
		float lostA = healthA - golemA.getHealth();
		float lostB = healthB - golemB.getHealth();
		helper.assertTrue(lostA > 0.0F, "단계 0 패턴 타격이 안 들어감");
		helper.assertTrue(Math.abs(lostB - lostA * 1.5F) < 0.05F, "단계 2 패턴 피해 " + lostB + " (단계 0: " + lostA + ")");

		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		warden.setNoAi(true);
		double wardenBase = warden.getAttributeValue(Attributes.ATTACK_DAMAGE);
		warden.party().setTier(warden, 1);
		helper.assertValueEqual(warden.getAttributeValue(Attributes.ATTACK_DAMAGE), wardenBase * 1.25, "파수꾼 단계 1 공격력 속성");
		helper.assertValueEqual(warden.party().outgoingDamageScale(), 1.25, "파수꾼 단계 1 패턴 피해 배율");
		helper.succeed();
	}

	/** 설정 값을 바꾸면 배율도 따라간다 (bossTier 구역). */
	@GameTest
	public void tierMultipliersFollowConfig(GameTestHelper helper) {
		SanupcraftConfig before = SanupcraftConfig.current();
		SanupcraftConfig changed = SanupcraftConfig.defaults();
		changed.bossTierHealthPerTier = 1.0;
		changed.bossTierDamagePerTier = 0.5;
		SanupcraftConfig.use(changed);
		try {
			helper.assertValueEqual(BossTier.healthScale(2), 3.0, "설정 체력 배율");
			helper.assertValueEqual(BossTier.damageScale(2), 2.0, "설정 피해 배율");
		} finally {
			SanupcraftConfig.use(before);
		}
		helper.assertValueEqual(BossTier.healthScale(2), 2.0, "기본 체력 배율");
		helper.assertTrue(SanupcraftConfig.defaults().toJson().contains("healthPerTier"), "설정 파일에 bossTier 구역이 없음");
		helper.succeed();
	}

	// ---- 제단 ----

	/** 제단에 강화 소환석을 쓰면 그 단계 보스가 나오고 소환석 하나가 줄며, 보스 바 제목에 +N이 붙는다. +3이면 발전 과제. */
	@GameTest(environment = "sanupcraft-test:alone")
	public void altarSummonsTieredBoss(GameTestHelper helper) {
		BlockPos altar = new BlockPos(3, 1, 3);
		helper.setBlock(altar, ModBoss.GUARDIAN_ALTAR);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(3.5, 1.0, 6.5)));
		ItemStack summoner = BossTier.stack(ModBoss.GUARDIAN_SUMMONER, BossTier.MAX);
		summoner.setCount(2);
		BlockState state = helper.getLevel().getBlockState(helper.absolutePos(altar));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(altar)), net.minecraft.core.Direction.UP, helper.absolutePos(altar), false);
		state.useItemOn(summoner, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);

		List<RustyGuardian> bosses = helper.getLevel().getEntitiesOfClass(RustyGuardian.class, helper.getBounds().inflate(8.0));
		helper.assertValueEqual(bosses.size(), 1, "소환된 수호자 수");
		RustyGuardian boss = bosses.get(0);
		helper.assertValueEqual(boss.party().tier(), BossTier.MAX, "소환된 단계");
		int party = boss.party().size();
		helper.assertValueEqual(boss.getMaxHealth(), (float) Math.min(1024.0, RustyGuardian.MAX_HEALTH * BossParty.healthScale(party) * 2.5), "소환된 체력");
		helper.assertValueEqual(summoner.getCount(), 1, "소환석이 하나 줄어야 함");
		helper.assertTrue(boss.bossBarName().getContents() instanceof TranslatableContents contents && contents.getKey().equals("item.sanupcraft.boss_tier.name"),
				"보스 바 제목에 +N이 안 붙음: " + boss.bossBarName());
		var advancement = helper.getLevel().getServer().getAdvancements().get(Sanupcraft.id(BossTier.ADVANCEMENT));
		helper.assertTrue(advancement != null, "발전 과제 '더 강한 적'이 없음");
		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "+3을 불렀는데 발전 과제를 못 받음");
		boss.discard();
		helper.succeed();
	}

	/** 소환석 단계가 낮으면 발전 과제는 없다. 단계 0 소환은 전과 같다. */
	@GameTest(environment = "sanupcraft-test:alone")
	public void altarSummonWithoutTierIsUnchanged(GameTestHelper helper) {
		RustyGuardian boss = io.github.yhyeop.sanupcraft.boss.GuardianAltarBlock.summon(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), ModBoss.RUSTY_GUARDIAN);
		helper.assertTrue(boss != null, "소환되지 않음");
		helper.assertValueEqual(boss.party().tier(), 0, "단계");
		helper.assertValueEqual(boss.getAttributeValue(Attributes.ATTACK_DAMAGE), 9.0, "공격력");
		boss.discard();
		helper.succeed();
	}

	// ---- 보상 · 기록 · 저장 ----

	/** 단계 2: 기여자 각자 핵 3개 + 업그레이드 하나. 기여가 모자란 사람은 못 받는다. 단계 0은 업그레이드 없음. */
	@GameTest
	public void tieredGuardianPaysExtraCoresAndAnUpgrade(GameTestHelper helper) {
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		boss.setNoAi(true);
		boss.party().scale(boss, 1);
		boss.party().setTier(boss, 2);
		ServerPlayer killer = mockPlayer(helper);
		ServerPlayer assistant = mockPlayer(helper);
		ServerPlayer bystander = mockPlayer(helper);
		boss.party().record(assistant, boss.getMaxHealth() * 0.5F);
		boss.party().record(bystander, boss.getMaxHealth() * 0.01F);
		boss.hurtServer(helper.getLevel(), boss.damageSources().playerAttack(killer), 100000.0F);
		helper.assertTrue(boss.isDeadOrDying(), "수호자가 죽지 않음");
		helper.assertValueEqual(count(killer, ModItems.GUARDIAN_CORE), 3, "막타 친 사람의 핵 (1 + 단계 2)");
		helper.assertValueEqual(count(assistant, ModItems.GUARDIAN_CORE), 3, "도운 사람의 핵");
		helper.assertValueEqual(upgrades(killer), 1, "막타 친 사람의 업그레이드");
		helper.assertValueEqual(upgrades(assistant), 1, "도운 사람의 업그레이드");
		helper.assertValueEqual(count(bystander, ModItems.GUARDIAN_CORE), 0, "기여가 모자란 사람의 핵");
		helper.assertValueEqual(upgrades(bystander), 0, "기여가 모자란 사람의 업그레이드");

		// 단계 0은 전과 같다: 핵 1개, 업그레이드 없음
		RustyGuardian plain = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(6, 1, 6));
		plain.setNoAi(true);
		ServerPlayer solo = mockPlayer(helper);
		plain.hurtServer(helper.getLevel(), plain.damageSources().playerAttack(solo), 100000.0F);
		helper.assertValueEqual(count(solo, ModItems.GUARDIAN_CORE), 1, "단계 0 핵");
		helper.assertValueEqual(upgrades(solo), 0, "단계 0 업그레이드");
		cleanUp(helper, "sanupcraft:rusty_guardian");
		helper.succeed();
	}

	/** 파수꾼도 같다: 단계 1이면 심장 2개 + 업그레이드 하나. */
	@GameTest(environment = "sanupcraft-test:alone_warden")
	public void tieredWardenPaysExtraHeartsAndAnUpgrade(GameTestHelper helper) {
		FurnaceWarden boss = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		boss.setNoAi(true);
		boss.party().scale(boss, 1);
		boss.party().setTier(boss, 1);
		ServerPlayer killer = mockPlayer(helper);
		boss.hurtServer(helper.getLevel(), boss.damageSources().playerAttack(killer), 100000.0F);
		helper.assertTrue(boss.isDeadOrDying(), "파수꾼이 죽지 않음");
		helper.assertValueEqual(count(killer, ModItems.WARDEN_HEART), 2, "심장 (1 + 단계 1)");
		helper.assertValueEqual(upgrades(killer), 1, "업그레이드");
		cleanUp(helper, "sanupcraft:furnace_warden");
		helper.succeed();
	}

	/** 보스 기록에 강화 단계가 남는다. */
	@GameTest
	public void bossRecordKeepsTier(GameTestHelper helper) {
		// 시험 월드는 실행마다 이어 쓰여서 상위 10개가 0틱 기록으로 차 있을 수 있다 -> 먼저 비운다
		BossRecords.get(helper.getLevel().getServer()).clear("sanupcraft:rusty_guardian");
		String name = "tr" + (System.nanoTime() % 100000);
		ServerPlayer killer = namedPlayer(helper, name);
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		boss.setNoAi(true);
		boss.party().scale(boss, 1);
		boss.party().setTier(boss, 3);
		boss.hurtServer(helper.getLevel(), boss.damageSources().playerAttack(killer), 100000.0F);
		helper.assertTrue(boss.isDeadOrDying(), "수호자가 죽지 않음");
		List<BossRecords.Entry> mine = BossRecords.get(helper.getLevel().getServer()).top("sanupcraft:rusty_guardian", BossRecords.KEEP).stream()
				.filter(e -> e.players().contains(name)).toList();
		helper.assertValueEqual(mine.size(), 1, "기록 수");
		helper.assertValueEqual(mine.get(0).tier(), 3, "기록의 강화 단계");
		cleanUp(helper, "sanupcraft:rusty_guardian");
		helper.succeed();
	}

	/** 단계는 보스에 저장된다: 저장했다 읽으면 단계 · 체력 · 공격력 · 보스 바 제목이 그대로. */
	@GameTest
	public void tierSurvivesSaveAndLoad(GameTestHelper helper) {
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		boss.setNoAi(true);
		boss.party().scale(boss, 2);
		boss.party().setTier(boss, 2);
		float maxHealth = boss.getMaxHealth();
		double attack = boss.getAttributeValue(Attributes.ATTACK_DAMAGE);

		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		boss.saveWithoutId(output);
		var tag = output.buildResult();

		RustyGuardian loaded = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(6, 1, 6));
		loaded.setNoAi(true);
		helper.assertValueEqual(loaded.party().tier(), 0, "새 보스의 단계");
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
		helper.assertValueEqual(loaded.party().tier(), 2, "읽은 단계");
		helper.assertValueEqual(loaded.party().size(), 2, "읽은 인원");
		helper.assertValueEqual(loaded.getMaxHealth(), maxHealth, "읽은 최대 체력");
		helper.assertValueEqual(loaded.getAttributeValue(Attributes.ATTACK_DAMAGE), attack, "읽은 공격력");
		helper.assertTrue(loaded.bossBarName().getContents() instanceof TranslatableContents contents && contents.getKey().equals("item.sanupcraft.boss_tier.name"),
				"읽은 보스 바 제목에 +N이 없음: " + loaded.bossBarName());
		helper.succeed();
	}

	/** 플레이테스트 단계 'tiers'는 +1 ~ +3 소환석과 재료를 준다. */
	@GameTest
	public void playtestTiersKitGivesSummonersAndMaterials(GameTestHelper helper) {
		ServerPlayer player = mockPlayer(helper);
		io.github.yhyeop.sanupcraft.playtest.PlaytestCommand.apply(player, io.github.yhyeop.sanupcraft.playtest.PlaytestCommand.Stage.TIERS);
		List<Integer> tiers = new ArrayList<>();
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(ModBoss.GUARDIAN_SUMMONER)) {
				tiers.add(BossTier.of(stack));
			}
		}
		helper.assertTrue(tiers.containsAll(List.of(0, 1, 2, 3)), "수호자 소환석 단계가 모자람: " + tiers);
		helper.assertTrue(count(player, ModItems.CORE_SHARD) >= 12, "핵 조각이 모자람");
		helper.assertTrue(count(player, ModItems.WARDEN_HEART) >= 3, "파수꾼의 심장이 모자람");
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 3x3 작업대에 소환석 하나 + 재료 {@code count}개를 놓고 레시피 결과를 본다 (안 맞으면 빈 스택). */
	private static ItemStack craft(GameTestHelper helper, ItemStack summoner, ItemStack material, int count) {
		List<ItemStack> grid = new ArrayList<>();
		grid.add(summoner.copyWithCount(1));
		for (int i = 0; i < count; i++) {
			grid.add(material.copyWithCount(1));
		}
		while (grid.size() < 9) {
			grid.add(ItemStack.EMPTY);
		}
		CraftingInput input = CraftingInput.of(3, 3, grid);
		Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
		return recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	/** 보스를 잡은 시험의 뒷정리: 시험 월드가 이어 쓰이므로 보스 기록을 비우고, 떨어진 경험치 구슬을 치운다 (이웃 시험을 방해하지 않게). */
	private static void cleanUp(GameTestHelper helper, String bossId) {
		BossRecords.get(helper.getLevel().getServer()).clear(bossId);
		helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, helper.getBounds().inflate(16.0)).forEach(orb -> orb.discard());
	}

	private static ServerPlayer mockPlayer(GameTestHelper helper) {
		return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
	}

	private static ServerPlayer namedPlayer(GameTestHelper helper, String name) {
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name),
				ClientInformation.createDefault()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}

			@Override
			public boolean isClientAuthoritative() {
				return false;
			}

			@Override
			public void sendSystemMessage(Component message, boolean overlay) {
			}
		};
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		return player;
	}

	private static int count(ServerPlayer player, Item item) {
		int total = 0;
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** 가진 기계 업그레이드(속도 · 효율 · 두 배) 개수. */
	private static int upgrades(ServerPlayer player) {
		return count(player, ModMachines.SPEED_UPGRADE) + count(player, ModMachines.EFFICIENCY_UPGRADE) + count(player, ModMachines.DOUBLE_UPGRADE);
	}
}
