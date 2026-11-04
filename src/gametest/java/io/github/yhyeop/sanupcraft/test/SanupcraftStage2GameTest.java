package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import io.github.yhyeop.sanupcraft.boss.BossParty;
import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.logistics.EnderLinkBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;

/**
 * 2단계 후보 구현 검사: 보스 인원 배율 · 기여자 보상 (D6 + A10), 굴착기 쓰레기 버림 (A5), 엔더 운반기 잠금 (B6).
 * JEI 플러그인은 클라이언트에서 JEI가 있을 때만 돌아서 여기서는 보지 않는다.
 */
public class SanupcraftStage2GameTest {
	// ---- 보스 인원 배율 ----

	/** 체력 배율: 1명 1배, 3명 2배, 5명 이상 3배에서 멈춘다. */
	@GameTest
	public void partyHealthScaleIsCapped(GameTestHelper helper) {
		helper.assertValueEqual(BossParty.healthScale(1), 1.0, "1명");
		helper.assertValueEqual(BossParty.healthScale(2), 1.5, "2명");
		helper.assertValueEqual(BossParty.healthScale(3), 2.0, "3명");
		helper.assertValueEqual(BossParty.healthScale(5), 3.0, "5명");
		helper.assertValueEqual(BossParty.healthScale(10), 3.0, "10명");
		helper.assertValueEqual(BossParty.healthScale(0), 1.0, "0명(최소 1)");
		helper.succeed();
	}

	/** 인원 3명으로 맞추면 수호자 체력이 600으로 꽉 차고, 다시 1명으로 맞추면 300으로 돌아온다 (수정자가 쌓이지 않음). */
	@GameTest
	public void partyScalesGuardianHealth(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		guardian.setNoAi(true);
		guardian.party().scale(guardian, 3);
		helper.assertValueEqual(guardian.getMaxHealth(), (float) (RustyGuardian.MAX_HEALTH * 2.0), "3명 최대 체력");
		helper.assertValueEqual(guardian.getHealth(), guardian.getMaxHealth(), "3명 현재 체력");
		guardian.party().scale(guardian, 3);
		helper.assertValueEqual(guardian.getMaxHealth(), (float) (RustyGuardian.MAX_HEALTH * 2.0), "두 번 맞춰도 같은 체력");
		guardian.party().scale(guardian, 1);
		helper.assertValueEqual(guardian.getMaxHealth(), (float) RustyGuardian.MAX_HEALTH, "1명 최대 체력");
		helper.succeed();
	}

	/** 패턴 규모: 화염 장판은 인원수만큼 3 ~ 6개, 졸개는 3명마다 하나씩 (3 → 6). */
	@GameTest
	public void patternsGrowWithParty(GameTestHelper helper) {
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		warden.setNoAi(true);
		helper.assertValueEqual(warden.maxRings(), 3, "혼자 장판 수");
		warden.party().scale(warden, 5);
		helper.assertValueEqual(warden.maxRings(), 5, "5명 장판 수");
		warden.party().scale(warden, 10);
		helper.assertValueEqual(warden.maxRings(), 6, "10명 장판 수");

		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(1, 1, 1));
		guardian.setNoAi(true);
		helper.assertValueEqual(guardian.minionsPerSummon(), RustyGuardian.MINIONS_PER_SUMMON, "혼자 졸개 수");
		guardian.party().scale(guardian, 10);
		helper.assertValueEqual(guardian.minionsPerSummon(), 6, "10명 졸개 수");
		helper.succeed();
	}

	/**
	 * 최대 체력의 5% 이상 깎은 사람은 각자 핵을 받는다. 5% 미만은 못 받는다. 기여자가 있으면 바닥에 핵을 떨구지 않는다.
	 * 막타를 친 사람의 피해도 죽기 전에 세어진다.
	 */
	@GameTest
	public void eachContributorGetsCore(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		guardian.setNoAi(true);
		ServerPlayer killer = mockPlayer(helper);
		ServerPlayer helperPlayer = mockPlayer(helper);
		ServerPlayer bystander = mockPlayer(helper);
		float needed = guardian.getMaxHealth() * BossParty.CONTRIBUTION_SHARE;
		guardian.party().record(helperPlayer, needed + 1.0F);
		guardian.party().record(bystander, needed - 1.0F);

		guardian.hurtServer(helper.getLevel(), guardian.damageSources().playerAttack(killer), 10000.0F);
		helper.assertTrue(guardian.isDeadOrDying(), "수호자가 죽지 않음");
		helper.assertTrue(guardian.party().damageBy(killer.getUUID()) > 0.0F, "막타 피해가 세어지지 않음");
		helper.assertValueEqual(count(killer, ModItems.GUARDIAN_CORE), 1, "막타 친 사람의 핵");
		helper.assertValueEqual(count(helperPlayer, ModItems.GUARDIAN_CORE), 1, "도운 사람의 핵");
		helper.assertValueEqual(count(bystander, ModItems.GUARDIAN_CORE), 0, "5% 미만인 사람의 핵");
		helper.assertItemEntityNotPresent(ModItems.GUARDIAN_CORE);
		helper.succeed();
	}

	/** 파수꾼도 같다: 심장을 기여자 각자에게. */
	@GameTest(environment = "sanupcraft-test:alone_warden")
	public void eachContributorGetsHeart(GameTestHelper helper) {
		FurnaceWarden warden = helper.spawn(ModBoss.FURNACE_WARDEN, new BlockPos(3, 1, 3));
		warden.setNoAi(true);
		ServerPlayer killer = mockPlayer(helper);
		ServerPlayer other = mockPlayer(helper);
		warden.party().record(other, warden.getMaxHealth() * 0.5F);
		warden.hurtServer(helper.getLevel(), warden.damageSources().playerAttack(killer), 10000.0F);
		helper.assertValueEqual(count(killer, ModItems.WARDEN_HEART), 1, "막타 친 사람의 심장");
		helper.assertValueEqual(count(other, ModItems.WARDEN_HEART), 1, "도운 사람의 심장");
		helper.assertItemEntityNotPresent(ModItems.WARDEN_HEART);
		helper.succeed();
	}

	// ---- 굴착기 쓰레기 버림 ----

	/** 기본(버림 켜짐): 돌(→ 조약돌)은 버리고 철광석(→ 원석 철)만 위 상자로 올린다. */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 300)
	public void quarryVoidsJunkByDefault(GameTestHelper helper) {
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		helper.setBlock(quarryPos.below(), Blocks.STONE);
		helper.setBlock(quarryPos.below().west(), Blocks.IRON_ORE);
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		helper.assertTrue(quarry.voidsJunk(), "쓰레기 버림이 기본으로 꺼져 있음");
		quarry.acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
		ChestBlockEntity chest = helper.getBlockEntity(quarryPos.above(), ChestBlockEntity.class);
		int firstLayer = helper.absolutePos(quarryPos).getY() - 1;
		helper.succeedWhen(() -> {
			helper.assertTrue(quarry.layer() < firstLayer, "아직 첫 층을 다 지나가지 않음: " + quarry.layer());
			helper.assertTrue(chest.countItem(Items.RAW_IRON) >= 1, "원석 철이 올라오지 않음");
			helper.assertValueEqual(chest.countItem(Items.COBBLESTONE), 0, "버려야 할 조약돌");
			helper.assertTrue(quarry.voided() >= 1, "버린 수가 세어지지 않음");
		});
	}

	// ---- 엔더 운반기 잠금 ----

	/** 잠그면 주인만 열고 채널을 바꾼다. 풀면 누구나. 주인이 없으면 누구나 관리(처음 잠그는 사람이 주인). */
	@GameTest
	public void lockedReceiverOnlyForOwner(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModLogistics.ENDER_RECEIVER);
		EnderLinkBlockEntity receiver = helper.getBlockEntity(pos, EnderLinkBlockEntity.class);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);

		helper.assertTrue(receiver.canManage(stranger), "주인이 없는데 관리 불가");
		receiver.setOwner(owner);
		helper.assertTrue(receiver.canUse(stranger), "잠그지 않았는데 남이 못 씀");
		helper.assertTrue(!receiver.canManage(stranger), "남이 잠금을 관리함");

		receiver.setLocked(true);
		helper.assertTrue(receiver.canUse(owner), "주인이 못 씀");
		helper.assertTrue(!receiver.canUse(stranger), "잠갔는데 남이 씀");

		receiver.setLocked(false);
		helper.assertTrue(receiver.canUse(stranger), "풀었는데 남이 못 씀");
		helper.succeed();
	}

	// ---- 도우미 ----

	private static ServerPlayer mockPlayer(GameTestHelper helper) {
		return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
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
