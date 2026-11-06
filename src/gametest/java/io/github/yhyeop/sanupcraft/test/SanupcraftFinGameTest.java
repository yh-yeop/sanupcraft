package io.github.yhyeop.sanupcraft.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.BossParty;
import io.github.yhyeop.sanupcraft.boss.BossSummonerItem;
import io.github.yhyeop.sanupcraft.boss.BossTier;
import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.PartyBoss;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.end.ModEnd;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.server.BossRecords;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/**
 * 마지막 묶음 (fin): 보스 체력 상한 보정(세 보스 공통), 공허 거상 강화 단계(C3), 거상 효과음(X2).
 * 체력 상한: 최대 체력 속성은 1024에서 잘리지만 받는 피해를 상한 / 원래 체력만큼 줄여서, 실제로 버티는 체력은 원래 값이다.
 */
public class SanupcraftFinGameTest {
	// ---- 체력 상한 보정 ----

	/** 수호자 5명 +3: 300 × 3 × 2.5 = 2250. 체력 칸은 1024지만 마법 피해 225를 맞으면 정확히 10%가 준다, 2250을 다 맞아야 쓰러진다. */
	@GameTest
	public void guardianHealthCapKeepsIntendedHealth(GameTestHelper helper) {
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		try {
			checkCappedHealth(helper, boss, RustyGuardian.MAX_HEALTH, 2250.0);
		} finally {
			boss.discard(); // 쓰러지는 동안에도 근처 제단 시험이 보스로 센다
			cleanUp(helper);
		}
		helper.succeed();
	}

	/** 파수꾼 5명 +3: 450 × 3 × 2.5 = 3375. */
	@GameTest(environment = "sanupcraft-test:alone_warden")
	public void wardenHealthCapKeepsIntendedHealth(GameTestHelper helper) {
		FurnaceWarden boss = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		try {
			checkCappedHealth(helper, boss, FurnaceWarden.MAX_HEALTH, 3375.0);
		} finally {
			boss.discard(); // 쓰러지는 동안에도 근처 제단 시험이 보스로 센다
			cleanUp(helper);
		}
		helper.succeed();
	}

	/** 거상 5명 +3: 600 × 3 × 2.5 = 4500 (수정이 없을 때). */
	@GameTest(environment = "sanupcraft-test:alone_colossus")
	public void colossusHealthCapKeepsIntendedHealth(GameTestHelper helper) {
		VoidColossus boss = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(3, 1, 3));
		helper.assertFalse(boss.isShielded(helper.getLevel()), "NoAI 거상에 수정이 있음");
		try {
			checkCappedHealth(helper, boss, VoidColossus.MAX_HEALTH, 4500.0);
		} finally {
			boss.discard(); // 쓰러지는 동안에도 근처 제단 시험이 보스로 센다
			cleanUp(helper);
		}
		helper.succeed();
	}

	/** 상한 밑이면 보정이 없다: 수호자 1명 단계 0은 마법 피해 30에 체력 30이 준다. */
	@GameTest
	public void healthUnderCapIsNotScaled(GameTestHelper helper) {
		RustyGuardian boss = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		boss.setNoAi(true);
		boss.party().scale(boss, 1);
		helper.assertValueEqual(boss.party().incomingDamageScale(boss), 1.0F, "받는 피해 배율");
		helper.assertValueEqual(BossParty.intendedMaxHealth(boss), RustyGuardian.MAX_HEALTH, "원래 체력");
		boss.hurtServer(helper.getLevel(), boss.damageSources().magic(), 30.0F);
		helper.assertTrue(Math.abs(boss.getHealth() - (RustyGuardian.MAX_HEALTH - 30.0)) < 0.01, "체력 " + boss.getHealth());
		boss.discard();
		helper.succeed();
	}

	/**
	 * 5명 +3으로 걸고: 원래 체력이 {@code intended}, 최대 체력 = 1024, 받는 피해 배율 = 1024 / intended.
	 * 마법 피해(갑옷 무시)로 10%를 깎으면 체력 비율이 0.9, 원래 체력 − 10까지 맞아도 살아 있고, 20을 더 맞으면 쓰러진다.
	 */
	private static <T extends Monster & PartyBoss> void checkCappedHealth(GameTestHelper helper, T boss, double base, double intended) {
		boss.setNoAi(true);
		boss.party().scale(boss, 5);
		boss.party().setTier(boss, 3);
		helper.assertValueEqual(base * BossParty.healthScale(5) * BossTier.healthScale(3), intended, "기대 체력 계산");
		helper.assertValueEqual(BossParty.intendedMaxHealth(boss), intended, "원래 체력 (상한 전)");
		helper.assertValueEqual((double) boss.getMaxHealth(), BossParty.healthCap(), "최대 체력 속성 (상한)");
		helper.assertValueEqual(boss.getHealth(), boss.getMaxHealth(), "체력이 꽉 차지 않음");
		float scale = boss.party().incomingDamageScale(boss);
		helper.assertTrue(Math.abs(scale - 1024.0 / intended) < 1.0E-5, "받는 피해 배율 " + scale);

		hit(helper, boss, (float) (intended * 0.1));
		float fraction = boss.getHealth() / boss.getMaxHealth();
		helper.assertTrue(Math.abs(fraction - 0.9F) < 0.002F, "10% 피해 뒤 체력 비율 " + fraction);
		hit(helper, boss, (float) (intended * 0.9 - 10.0));
		helper.assertTrue(boss.isAlive() && !boss.isDeadOrDying(), "원래 체력 − 10을 맞고 쓰러짐 (체력 " + boss.getHealth() + ")");
		helper.assertTrue(boss.getHealth() > 0.0F && boss.getHealth() < 10.0F * scale + 0.5F, "남은 체력 " + boss.getHealth());
		hit(helper, boss, 20.0F);
		helper.assertTrue(boss.isDeadOrDying(), "원래 체력을 넘게 맞았는데 살아 있음 (체력 " + boss.getHealth() + ")");
	}

	/** 갑옷을 무시하는 마법 피해. 맞은 뒤 피해 쿨다운을 지워서 다음 타격도 다 들어가게 한다. */
	private static void hit(GameTestHelper helper, Monster boss, float damage) {
		boss.damageCooldownTime = 0;
		helper.assertTrue(boss.hurtServer(helper.getLevel(), boss.damageSources().magic(), damage), "피해가 안 들어감: " + damage);
		boss.damageCooldownTime = 0;
	}

	// ---- 공허 거상 강화 (C3) ----

	/** 거상 소환석 + 공허 핵 1개 → +1씩, +3에서 멈춘다. 다른 재료로는 안 된다. 이름에 +N. */
	@GameTest
	public void craftingRaisesColossusSummonerTier(GameTestHelper helper) {
		helper.assertTrue(ModEnd.COLOSSUS_SUMMONER instanceof BossSummonerItem, "거상 소환석이 BossSummonerItem이 아님");
		ItemStack summoner = new ItemStack(ModEnd.COLOSSUS_SUMMONER);
		for (int expected = 1; expected <= BossTier.MAX; expected++) {
			ItemStack result = craft(helper, summoner, new ItemStack(ModEnd.VOID_CORE), 1);
			helper.assertTrue(result.is(ModEnd.COLOSSUS_SUMMONER), "결과가 거상 소환석이 아님: " + result);
			helper.assertValueEqual(BossTier.of(result), expected, "강화 뒤 단계");
			summoner = result;
		}
		helper.assertTrue(craft(helper, summoner, new ItemStack(ModEnd.VOID_CORE), 1).isEmpty(), "+3이 더 강화됨");
		ItemStack plain = new ItemStack(ModEnd.COLOSSUS_SUMMONER);
		helper.assertTrue(craft(helper, plain, new ItemStack(ModEnd.VOID_CORE), 2).isEmpty(), "공허 핵 2개로 강화됨");
		helper.assertTrue(craft(helper, plain, new ItemStack(ModItems.WARDEN_HEART), 1).isEmpty(), "거상 소환석이 심장으로 강화됨");
		helper.assertTrue(summoner.getHoverName().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("item.sanupcraft.boss_tier.name"), "강화한 거상 소환석 이름에 +N이 없음: " + summoner.getHoverName());
		helper.succeed();
	}

	/** 단계 2 거상: 체력 × 2 (1명 1200 → 1024 + 보정), 공격력 속성 × 1.5, 셜커 탄 피해 4 → 6, 보스 바 "+2". 저장했다 읽어도 그대로. */
	@GameTest(environment = "sanupcraft-test:alone_colossus")
	public void colossusTierScalesHealthDamageAndBar(GameTestHelper helper) {
		VoidColossus colossus = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(3, 1, 3));
		colossus.setNoAi(true);
		colossus.party().scale(colossus, 1);
		double attack = colossus.getAttributeValue(Attributes.ATTACK_DAMAGE);
		IronGolem target = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(1, 1, 1));
		target.setNoAi(true);
		helper.assertValueEqual(colossus.bullet(helper.getLevel(), target).damage(), VoidColossus.BULLET_DAMAGE, "단계 0 셜커 탄 피해");

		colossus.party().setTier(colossus, 2);
		helper.assertValueEqual(colossus.party().tier(), 2, "단계");
		helper.assertValueEqual(BossParty.intendedMaxHealth(colossus), VoidColossus.MAX_HEALTH * 2.0, "원래 체력");
		helper.assertValueEqual((double) colossus.getMaxHealth(), Math.min(BossParty.healthCap(), VoidColossus.MAX_HEALTH * 2.0), "최대 체력 속성");
		helper.assertValueEqual(colossus.getAttributeValue(Attributes.ATTACK_DAMAGE), attack * 1.5, "공격력 속성");
		helper.assertValueEqual(colossus.party().outgoingDamageScale(), 1.5, "패턴 피해 배율");
		helper.assertValueEqual(colossus.bullet(helper.getLevel(), target).damage(), VoidColossus.BULLET_DAMAGE * 1.5F, "단계 2 셜커 탄 피해");
		helper.assertTrue(colossus.bossBarName().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("item.sanupcraft.boss_tier.name"), "보스 바 제목에 +N이 없음: " + colossus.bossBarName());

		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		colossus.saveWithoutId(output);
		VoidColossus loaded = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(6, 1, 6));
		loaded.setNoAi(true);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
		helper.assertValueEqual(loaded.party().tier(), 2, "읽은 단계");
		helper.assertValueEqual(loaded.party().incomingDamageScale(loaded), colossus.party().incomingDamageScale(colossus), "읽은 받는 피해 배율");
		helper.assertTrue(loaded.bossBarName().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("item.sanupcraft.boss_tier.name"), "읽은 보스 바 제목에 +N이 없음: " + loaded.bossBarName());
		colossus.discard();
		loaded.discard();
		target.discard();
		helper.succeed();
	}

	/** 단계 1 거상을 잡으면 기여자는 공허 핵 2개 + 기계 업그레이드 하나. */
	@GameTest(environment = "sanupcraft-test:alone_colossus")
	public void tieredColossusPaysExtraCoresAndAnUpgrade(GameTestHelper helper) {
		VoidColossus colossus = helper.spawn(ModEnd.VOID_COLOSSUS, new BlockPos(3, 1, 3));
		colossus.setNoAi(true);
		colossus.party().scale(colossus, 1);
		colossus.party().setTier(colossus, 1);
		ServerPlayer killer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		colossus.hurtServer(helper.getLevel(), colossus.damageSources().playerAttack(killer), 100000.0F);
		helper.assertTrue(colossus.isDeadOrDying(), "거상이 죽지 않음");
		helper.assertValueEqual(count(killer, ModEnd.VOID_CORE), 2, "공허 핵 (1 + 단계 1)");
		helper.assertValueEqual(count(killer, ModMachines.SPEED_UPGRADE) + count(killer, ModMachines.EFFICIENCY_UPGRADE)
				+ count(killer, ModMachines.DOUBLE_UPGRADE), 1, "업그레이드");
		colossus.discard();
		cleanUp(helper);
		helper.succeed();
	}

	// ---- 거상 효과음 (X2) ----

	/** 거상 소리 넷(끌어당기기 예고 · 수정 소환 · 분노 · 쓰러짐)이 등록되어 있고 소리 목록에 들어 있다 (sounds.json · 자막은 M11b 시험이 전부 본다). */
	@GameTest
	public void colossusSoundsRegistered(GameTestHelper helper) {
		for (String name : new String[] {"boss.colossus.pull", "boss.colossus.crystals", "boss.colossus.rage", "boss.colossus.death"}) {
			var event = BuiltInRegistries.SOUND_EVENT.getValue(Sanupcraft.id(name));
			helper.assertTrue(event != null, "등록 안 된 소리: " + name);
			helper.assertTrue(ModSounds.all().contains(event), "소리 목록에 없음: " + name);
		}
		helper.assertTrue(ModSounds.COLOSSUS_DEATH != ModSounds.GUARDIAN_DEATH && ModSounds.COLOSSUS_RAGE != ModSounds.BOSS_PHASE, "거상 소리가 다른 보스 것과 같음");
		helper.succeed();
	}

	// ---- 도우미 ----

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

	private static int count(ServerPlayer player, Item item) {
		int total = 0;
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** 보스를 잡은 시험의 뒷정리: 시험 자리에 떨어진 핵 · 경험치 구슬을 치운다. 거상 기록은 이 묶음 시험만 남기므로 비운다. */
	private static void cleanUp(GameTestHelper helper) {
		BossRecords.get(helper.getLevel().getServer()).clear("sanupcraft:void_colossus");
		var area = helper.getBounds().inflate(2.0);
		helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, area).forEach(orb -> orb.discard());
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(ModItems.GUARDIAN_CORE)
				|| item.getItem().is(ModItems.WARDEN_HEART) || item.getItem().is(ModEnd.VOID_CORE)).forEach(item -> item.discard());
	}
}
