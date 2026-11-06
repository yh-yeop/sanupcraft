package io.github.yhyeop.sanupcraft.test;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.combat.CombatBeaconBlockEntity;
import io.github.yhyeop.sanupcraft.combat.Downed;
import io.github.yhyeop.sanupcraft.combat.FlamethrowerItem;
import io.github.yhyeop.sanupcraft.combat.ModCombat;
import io.github.yhyeop.sanupcraft.combat.SteamHammerItem;
import io.github.yhyeop.sanupcraft.combat.SteamSyringeItem;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.server.TeamData;
import io.github.yhyeop.sanupcraft.server.Teams;

/** M13 전투 검사: 강철 창 · 증기 해머 · 화염 방사기 (C1), 전투 신호기 · 쓰러짐 · 응급 증기 주사기 (C5). */
public class SanupcraftM13GameTest {
	// ---- C1 무기 ----

	/** 강철 창: 손에 들면 공격 사거리 +1.5, 피해 7, 공격 속도 1. 증기 해머: 피해 10, 속도 0.6. */
	@GameTest
	public void spearAndHammerAttributes(GameTestHelper helper) {
		ItemAttributeModifiers spear = new ItemStack(ModCombat.STEEL_SPEAR).get(DataComponents.ATTRIBUTE_MODIFIERS);
		helper.assertTrue(spear != null, "강철 창에 속성이 없음");
		helper.assertTrue(Math.abs(spear.compute(Attributes.ENTITY_INTERACTION_RANGE, 3.0, EquipmentSlot.MAINHAND) - 4.5) < 1.0E-6,
				"사거리: " + spear.compute(Attributes.ENTITY_INTERACTION_RANGE, 3.0, EquipmentSlot.MAINHAND));
		helper.assertTrue(Math.abs(spear.compute(Attributes.ENTITY_INTERACTION_RANGE, 3.0, EquipmentSlot.OFFHAND) - 3.0) < 1.0E-6, "왼손에도 사거리가 붙음");
		helper.assertTrue(Math.abs(spear.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND) - 7.0) < 1.0E-6, "창 피해");
		helper.assertTrue(Math.abs(spear.compute(Attributes.ATTACK_SPEED, 4.0, EquipmentSlot.MAINHAND) - 1.0) < 1.0E-6, "창 공격 속도");
		ItemAttributeModifiers hammer = new ItemStack(ModCombat.STEAM_HAMMER).get(DataComponents.ATTRIBUTE_MODIFIERS);
		helper.assertTrue(Math.abs(hammer.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND) - 10.0) < 1.0E-6, "해머 피해");
		helper.assertTrue(Math.abs(hammer.compute(Attributes.ATTACK_SPEED, 4.0, EquipmentSlot.MAINHAND) - 0.6) < 1.0E-6, "해머 공격 속도");
		helper.assertValueEqual(SteamCharge.capacity(new ItemStack(ModCombat.STEAM_HAMMER)), 600, "해머 증기");
		helper.succeed();
	}

	/** 증기 해머 강타: 증기 30을 쓰고 좀비를 기절시킨다 (이동 속도 0, 공격이 안 들어감), 12틱 뒤 풀린다. 증기가 모자라면 기절 없음. */
	@GameTest
	public void steamHammerStunsZombie(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 1, 2));
		zombie.setNoAi(true);
		Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 1, 2));
		pig.setNoAi(true);
		Zombie spare = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(5, 1, 5));
		spare.setNoAi(true);
		ItemStack hammer = new ItemStack(ModCombat.STEAM_HAMMER);
		SteamCharge.set(hammer, 40);

		helper.assertTrue(SteamHammerItem.strike(level, hammer, zombie, player), "강타가 안 됨");
		helper.assertValueEqual(SteamCharge.get(hammer), 10, "증기 30이 안 빠짐");
		helper.assertTrue(zombie.hasEffect(ModCombat.STUNNED), "기절이 안 걸림");
		helper.assertTrue(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) < 1.0E-6, "기절했는데 이동 속도 " + zombie.getAttributeValue(Attributes.MOVEMENT_SPEED));
		float pigHealth = pig.getHealth();
		zombie.doHurtTarget(level, pig);
		helper.assertTrue(pig.getHealth() == pigHealth, "기절한 좀비의 공격이 들어감");
		// 증기 10으로는 강타 못 함
		helper.assertFalse(SteamHammerItem.strike(level, hammer, spare, player), "증기 10으로 강타");
		helper.assertFalse(spare.hasEffect(ModCombat.STUNNED), "증기가 없는데 기절");
		helper.assertValueEqual(SteamCharge.get(hammer), 10, "실패했는데 증기가 줄음");
		helper.runAfterDelay(SteamHammerItem.STUN_TICKS + 4, () -> {
			helper.assertFalse(zombie.hasEffect(ModCombat.STUNNED), "기절이 안 풀림");
			helper.assertTrue(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.1, "풀렸는데 이동 속도가 돌아오지 않음");
			zombie.doHurtTarget(level, pig);
			helper.assertTrue(pig.getHealth() < pigHealth, "풀렸는데 공격이 안 들어감");
			helper.succeed();
		});
	}

	/** 화염 방사기: 원뿔 안 좀비에 불 · 피해, 뒤쪽 좀비는 그대로. 블레이즈 가루 하나 → 연료 40틱, 증기 4. 연료가 없으면 안 나간다. */
	@GameTest
	public void flamethrowerIgnitesZombieInCone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 from = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		Zombie target = helmeted(helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 5)));
		Zombie behind = helmeted(helper.spawn(EntityTypes.ZOMBIE, new BlockPos(6, 1, 1)));
		player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());
		ItemStack gun = new ItemStack(ModCombat.FLAMETHROWER);
		SteamCharge.set(gun, 2000);
		ItemStack powder = new ItemStack(Items.BLAZE_POWDER, 2);
		player.getInventory().setItem(3, powder);
		helper.assertFalse(target.isOnFire(), "처음부터 불이 붙어 있음");

		helper.assertTrue(FlamethrowerItem.emit(level, player, gun) == FlamethrowerItem.Result.FIRED, "안 뿜어짐");
		helper.assertTrue(target.getRemainingFireTicks() > 0, "원뿔 안 좀비에 불이 안 붙음");
		helper.assertTrue(target.getHealth() < target.getMaxHealth(), "원뿔 안 좀비가 피해를 안 받음");
		helper.assertFalse(behind.isOnFire(), "원뿔 밖 좀비에 불이 붙음");
		helper.assertValueEqual(powder.getCount(), 1, "블레이즈 가루가 안 줄음");
		helper.assertValueEqual(FlamethrowerItem.fuel(gun), FlamethrowerItem.BLAZE_POWDER_TICKS - FlamethrowerItem.PULSE_INTERVAL, "넣은 연료");
		helper.assertValueEqual(SteamCharge.get(gun), 2000 - FlamethrowerItem.STEAM_PER_PULSE, "증기");
		// 넣은 연료가 남아 있으면 가루를 더 쓰지 않는다
		helper.assertTrue(FlamethrowerItem.emit(level, player, gun) == FlamethrowerItem.Result.FIRED, "두 번째가 안 뿜어짐");
		helper.assertValueEqual(powder.getCount(), 1, "연료가 남았는데 가루를 씀");
		// 연료가 다 떨어지면 안 나가고 증기도 그대로
		FlamethrowerItem.setFuel(gun, 0);
		powder.setCount(0);
		int steam = SteamCharge.get(gun);
		helper.assertTrue(FlamethrowerItem.emit(level, player, gun) == FlamethrowerItem.Result.NO_FUEL, "연료 없이 뿜음");
		helper.assertValueEqual(SteamCharge.get(gun), steam, "연료가 없는데 증기가 줄음");
		// 마그마 크림은 20틱
		player.getInventory().setItem(4, new ItemStack(Items.MAGMA_CREAM));
		helper.assertTrue(FlamethrowerItem.emit(level, player, gun) == FlamethrowerItem.Result.FIRED, "마그마 크림으로 안 뿜어짐");
		helper.assertValueEqual(FlamethrowerItem.fuel(gun), FlamethrowerItem.MAGMA_CREAM_TICKS - FlamethrowerItem.PULSE_INTERVAL, "마그마 크림 연료");
		helper.succeed();
	}

	/** 햇빛에 타지 않게 투구를 씌운다. */
	private static Zombie helmeted(Zombie zombie) {
		zombie.setNoAi(true);
		zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		return zombie;
	}

	// ---- C5 협동 ----

	/** 전투 신호기: 주인의 팀원만 힘 · 저항, 증기 20. 반경 밖은 빼고, 주인이 팀이 없으면 모두. 증기가 모자라면 아무것도 안 한다. */
	@GameTest
	public void combatBeaconBuffsSameTeamOnly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		BlockPos rel = new BlockPos(3, 1, 3);
		helper.setBlock(rel, ModCombat.COMBAT_BEACON);
		BlockPos abs = helper.absolutePos(rel);
		CombatBeaconBlockEntity beacon = helper.getBlockEntity(rel, CombatBeaconBlockEntity.class);
		Vec3 near = Vec3.atBottomCenterOf(abs.above());
		ServerPlayer owner = inLevel(helper, "bo", near.add(1, 0, 0));
		ServerPlayer mate = inLevel(helper, "bm", near.add(-1, 0, 0));
		ServerPlayer stranger = inLevel(helper, "bs", near.add(0, 0, 1));
		ServerPlayer far = inLevel(helper, "bf", near.add(20, 0, 0));
		List<ServerPlayer> all = List.of(owner, mate, stranger, far);
		try {
			helper.assertTrue(Teams.create(server, owner, "B" + (System.nanoTime() % 100000)) == null, "팀 만들기 실패");
			TeamData.Team team = Teams.teamOf(server, owner.getUUID());
			helper.assertTrue(Teams.join(server, mate, team.name()) == null, "팀 들어가기 실패");
			beacon.setOwner(owner);

			helper.assertValueEqual(beacon.pulse(level, abs, all), 0, "증기 없이 버프");
			beacon.acceptSteam(CombatBeaconBlockEntity.CAPACITY);
			helper.assertValueEqual(beacon.pulse(level, abs, all), 2, "버프 받은 사람 수 (주인 + 팀원)");
			helper.assertTrue(owner.hasEffect(MobEffects.STRENGTH) && owner.hasEffect(MobEffects.RESISTANCE), "주인이 버프를 못 받음");
			helper.assertTrue(mate.hasEffect(MobEffects.STRENGTH) && mate.hasEffect(MobEffects.RESISTANCE), "팀원이 버프를 못 받음");
			helper.assertFalse(stranger.hasEffect(MobEffects.STRENGTH), "다른 사람이 버프를 받음");
			helper.assertFalse(far.hasEffect(MobEffects.STRENGTH), "반경 밖 사람이 버프를 받음");
			helper.assertValueEqual(beacon.steamStored(), CombatBeaconBlockEntity.CAPACITY - CombatBeaconBlockEntity.STEAM_PER_PULSE, "증기 20");

			// 팀이 없어지면 근처 모두
			Teams.disband(server, team);
			helper.assertValueEqual(beacon.pulse(level, abs, all), 3, "팀이 없는 주인: 반경 안 모두");
			helper.assertTrue(stranger.hasEffect(MobEffects.STRENGTH), "팀이 없는데 다른 사람이 못 받음");
		} finally {
			TeamData.Team left = Teams.teamOf(server, owner.getUUID());
			if (left != null) {
				Teams.disband(server, left);
			}
			all.forEach(p -> server.getPlayerList().remove(p));
		}
		helper.succeed();
	}

	/** 죽을 피해 → 쓰러짐 (체력 1). 쓰러진 동안 피해 대신 시간이 준다. 혼자 주사 못 함, 동료의 주사기로 체력 4. 60초 안에 또 죽으면 그냥 죽는다. */
	@GameTest
	public void downedPlayerIsRevivedBySyringe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		Vec3 base = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
		ServerPlayer victim = inLevel(helper, "dv", base);
		ServerPlayer friend = inLevel(helper, "df", base.add(3, 0, 0));
		try {
			boolean hurt = victim.hurtServer(level, victim.damageSources().generic(), 1000.0F);
			helper.assertTrue(victim.isAlive() && !victim.isDeadOrDying(), "쓰러지지 않고 죽음");
			helper.assertTrue(Downed.isDowned(victim), "쓰러짐 상태가 아님: 맞음=" + hurt + " 체력=" + victim.getHealth() + " 근처 동료=" + Downed.hasHelper(victim));
			helper.assertTrue(victim.getHealth() == 1.0F, "체력이 1이 아님: " + victim.getHealth());
			helper.assertTrue(victim.hasEffect(ModCombat.DOWNED) && victim.hasEffect(MobEffects.GLOWING), "쓰러짐 · 빛남 효과가 없음");

			int left = Downed.ticksLeft(victim);
			helper.assertValueEqual(left, Downed.BLEED_OUT_TICKS, "남은 시간");
			helper.assertFalse(victim.hurtServer(level, victim.damageSources().generic(), 5.0F), "쓰러진 사람이 피해를 받음");
			helper.assertTrue(victim.getHealth() == 1.0F, "쓰러진 채 체력이 줄음");
			helper.assertValueEqual(Downed.ticksLeft(victim), left - Downed.HIT_PENALTY_TICKS, "맞았는데 시간이 안 줄음");

			ItemStack syringe = new ItemStack(ModCombat.STEAM_SYRINGE);
			SteamCharge.set(syringe, 600);
			helper.assertTrue(SteamSyringeItem.inject(level, victim, syringe, victim) == SteamSyringeItem.Result.SELF, "스스로 주사함");
			helper.assertTrue(SteamSyringeItem.inject(level, friend, syringe, victim) == SteamSyringeItem.Result.REVIVED, "동료가 못 살림");
			helper.assertFalse(Downed.isDowned(victim), "살렸는데 아직 쓰러짐");
			helper.assertTrue(victim.getHealth() == 4.0F, "살린 뒤 체력: " + victim.getHealth());
			helper.assertTrue(victim.hasEffect(MobEffects.REGENERATION), "재생이 없음");
			helper.assertFalse(victim.hasEffect(ModCombat.DOWNED), "쓰러짐 효과가 남음");
			helper.assertValueEqual(SteamCharge.get(syringe), 400, "주사기 증기 200");
			helper.assertTrue(SteamSyringeItem.inject(level, friend, syringe, victim) == SteamSyringeItem.Result.NOT_DOWNED, "안 쓰러진 사람에게 주사");

			// 60초 안에 또 죽을 피해: 쓰러지지 않고 죽는다 (맞은 직후 무적 시간은 지운다)
			victim.damageCooldownTime = 0;
			boolean again = victim.hurtServer(level, victim.damageSources().generic(), 1000.0F);
			helper.assertTrue(victim.isDeadOrDying(), "대기 중인데 또 쓰러짐: 맞음=" + again + " 체력=" + victim.getHealth() + " 쓰러짐=" + Downed.isDowned(victim));
			helper.assertFalse(Downed.isDowned(victim), "죽었는데 쓰러짐 표시가 남음");
		} finally {
			server.getPlayerList().remove(victim);
			server.getPlayerList().remove(friend);
		}
		helper.succeed();
	}

	/** 쓰러진 채 시간이 다 되면 진짜로 죽는다 (출혈 피해). 증기 통의 증기로도 주사기를 쓸 수 있다 (주사기가 비었을 때). */
	@GameTest
	public void downedPlayerBleedsOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		Vec3 base = helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
		ServerPlayer victim = inLevel(helper, "bv", base);
		ServerPlayer friend = inLevel(helper, "bh", base.add(0, 0, 2));
		ServerPlayer third = inLevel(helper, "bt", base.add(2, 0, 0));
		try {
			victim.hurtServer(level, victim.damageSources().generic(), 1000.0F);
			helper.assertTrue(Downed.isDowned(victim), "쓰러지지 않음");
			long now = server.overworld().getGameTime();
			Downed.tickPlayer(victim, now + Downed.BLEED_OUT_TICKS - 1);
			helper.assertTrue(victim.isAlive() && Downed.isDowned(victim), "시간 전에 죽음");
			Downed.tickPlayer(victim, now + Downed.BLEED_OUT_TICKS);
			helper.assertTrue(victim.isDeadOrDying(), "시간이 다 됐는데 살아 있음");
			helper.assertFalse(Downed.isDowned(victim), "죽었는데 쓰러짐 표시가 남음");
			helper.assertTrue(victim.getLastDamageSource() != null && victim.getLastDamageSource().is(Downed.BLED_OUT), "출혈 피해가 아님: " + victim.getLastDamageSource());

			// 빈 주사기 + 증기 통: 통의 증기로 살린다
			friend.hurtServer(level, friend.damageSources().generic(), 1000.0F);
			helper.assertTrue(Downed.isDowned(friend), "두 번째 사람이 쓰러지지 않음 (근처에 셋째가 있음)");
			ItemStack syringe = new ItemStack(ModCombat.STEAM_SYRINGE);
			ItemStack canister = new ItemStack(io.github.yhyeop.sanupcraft.gear.SteamGear.STEAM_CANISTER);
			SteamCharge.set(canister, 250);
			third.getInventory().setItem(5, canister);
			helper.assertTrue(SteamSyringeItem.inject(level, third, syringe, friend) == SteamSyringeItem.Result.REVIVED, "증기 통으로 못 살림");
			helper.assertValueEqual(SteamCharge.get(canister), 50, "증기 통에서 200이 안 빠짐");
		} finally {
			server.getPlayerList().remove(victim);
			server.getPlayerList().remove(friend);
			server.getPlayerList().remove(third);
		}
		helper.succeed();
	}

	/** 주사기 증기가 비용(200)보다 적으면 모자란 몫만 증기 통에서 쓴다: 150 + 통 100 → 살림(주사기 0 · 통 50), 50 + 통 100(합 150) → 아무것도 안 쓰고 실패. */
	@GameTest
	public void syringeUsesCanisterOnlyForShortfall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		Vec3 base = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
		ServerPlayer first = inLevel(helper, "sf", base);
		ServerPlayer second = inLevel(helper, "ss", base.add(0, 0, 2));
		ServerPlayer user = inLevel(helper, "su", base.add(2, 0, 0));
		try {
			first.hurtServer(level, first.damageSources().generic(), 1000.0F);
			second.hurtServer(level, second.damageSources().generic(), 1000.0F);
			helper.assertTrue(Downed.isDowned(first) && Downed.isDowned(second), "둘 다 쓰러지지 않음");

			ItemStack canister = new ItemStack(io.github.yhyeop.sanupcraft.gear.SteamGear.STEAM_CANISTER);
			SteamCharge.set(canister, 100);
			user.getInventory().setItem(5, canister);

			// 합쳐도 모자람: 아무것도 안 빠진다
			ItemStack poor = new ItemStack(ModCombat.STEAM_SYRINGE);
			SteamCharge.set(poor, 50);
			helper.assertTrue(SteamSyringeItem.inject(level, user, poor, first) == SteamSyringeItem.Result.NO_STEAM, "합이 모자란데 살림");
			helper.assertValueEqual(SteamCharge.get(poor), 50, "실패했는데 주사기 증기가 빠짐");
			helper.assertValueEqual(SteamCharge.get(canister), 100, "실패했는데 증기 통이 빠짐");
			helper.assertTrue(Downed.isDowned(first), "실패했는데 일어남");

			// 주사기 150 + 통 100: 주사기를 다 쓰고 통에서 50만 쓴다
			ItemStack syringe = new ItemStack(ModCombat.STEAM_SYRINGE);
			SteamCharge.set(syringe, 150);
			helper.assertTrue(SteamSyringeItem.inject(level, user, syringe, second) == SteamSyringeItem.Result.REVIVED, "주사기 150 + 통 100으로 못 살림");
			helper.assertFalse(Downed.isDowned(second), "살렸는데 아직 쓰러짐");
			helper.assertValueEqual(SteamCharge.get(syringe), 0, "주사기 증기를 다 안 씀");
			helper.assertValueEqual(SteamCharge.get(canister), 50, "통에서 모자란 50만 안 빠짐");
		} finally {
			server.getPlayerList().remove(first);
			server.getPlayerList().remove(second);
			server.getPlayerList().remove(user);
		}
		helper.succeed();
	}

	/** 실테스트 combat 꾸러미: 무기 셋 · 신호기 · 주사기가 들어 있고 해머 · 화염 방사기는 증기가 가득. */
	@GameTest
	public void playtestCombatKit(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.COMBAT);
		helper.assertTrue(player.getInventory().countItem(ModCombat.STEEL_SPEAR) == 1, "강철 창이 없음");
		helper.assertTrue(player.getInventory().countItem(ModCombat.COMBAT_BEACON.asItem()) == 1, "전투 신호기가 없음");
		helper.assertTrue(player.getInventory().countItem(ModCombat.STEAM_SYRINGE) == 2, "주사기가 둘이 아님");
		boolean fullHammer = false;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ModCombat.STEAM_HAMMER) && SteamCharge.isFull(stack)) {
				fullHammer = true;
			}
		}
		helper.assertTrue(fullHammer, "가득 찬 증기 해머가 없음");
		helper.succeed();
	}

	// ---- 도우미 ----

	/** 월드에 들어간 생존 모드 시험 플레이어 (효과 · 메시지 패킷이 갈 연결이 있다). 끝나면 PlayerList.remove로 뺀다. */
	private static ServerPlayer inLevel(GameTestHelper helper, String name, Vec3 pos) {
		MinecraftServer server = helper.getLevel().getServer();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
		ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}

			@Override
			public boolean isClientAuthoritative() {
				return false;
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		// 접속 직후에는 클라이언트가 다 불러올 때까지(최대 60틱) 피해를 안 받는다 - 시험이라 바로 끝낸다
		for (int i = 0; i < 60; i++) {
			player.connection.tickClientLoadTimeout();
		}
		player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		return player;
	}
}
