package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.RivetGunItem;
import io.github.yhyeop.sanupcraft.gear.RivetProjectile;
import io.github.yhyeop.sanupcraft.gear.SteamBoots;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.power.SteamChargerBlockEntity;
import io.github.yhyeop.sanupcraft.power.SteamNetwork;
import io.github.yhyeop.sanupcraft.power.SteamPipeBlock;

/** 묶음 3 검사: 증기 통 · 충전대 · 리벳 · 리벳총 · 증기 부츠 (A1). */
public class SanupcraftBundle3GameTest {
	// ---- 증기 통 도우미 ----

	/** 채우기 · 꺼내기는 0 ~ 최대로 잘리고, 0이 되면 컴포넌트가 사라진다. 막대는 증기량을 보인다. */
	@GameTest
	public void steamChargeHelperClampsAndShowsBar(GameTestHelper helper) {
		ItemStack canister = new ItemStack(SteamGear.STEAM_CANISTER);
		helper.assertValueEqual(SteamCharge.capacity(canister), 1000, "증기 통 용량");
		helper.assertValueEqual(SteamCharge.get(canister), 0, "새 통의 증기");
		helper.assertFalse(canister.isBarVisible(), "빈 통에 막대가 보임");
		helper.assertValueEqual(SteamCharge.fill(canister, 1500), 1000, "넘치게 채울 때 받은 양");
		helper.assertTrue(SteamCharge.isFull(canister), "가득 찬 통");
		helper.assertValueEqual(canister.getBarWidth(), 13, "가득 찬 막대");
		helper.assertTrue(canister.isBarVisible(), "찬 통에 막대가 안 보임");
		helper.assertValueEqual(SteamCharge.take(canister, 500), 500, "꺼낸 양");
		helper.assertValueEqual(canister.getBarWidth(), 7, "반쯤 찬 막대");
		helper.assertValueEqual(SteamCharge.take(canister, 9999), 500, "모자란 만큼만 꺼냄");
		helper.assertFalse(canister.has(SteamCharge.STEAM), "0이 됐는데 컴포넌트가 남음");
		helper.assertValueEqual(SteamCharge.fill(new ItemStack(Items.STICK), 10), 0, "증기를 못 담는 아이템");
		ItemStack boots = new ItemStack(SteamGear.STEAM_BOOTS);
		helper.assertValueEqual(SteamCharge.fill(boots, 900), 500, "부츠 용량 500");
		helper.succeed();
	}

	/** 인벤토리 도우미: 첫 찬 통 찾기, 합계, 여러 통에서 합쳐 빼기 (모자라면 아무것도 안 뺀다). */
	@GameTest
	public void inventoryDrainSpansCanisters(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack empty = new ItemStack(SteamGear.STEAM_CANISTER);
		ItemStack first = new ItemStack(SteamGear.STEAM_CANISTER);
		ItemStack second = new ItemStack(SteamGear.STEAM_CANISTER);
		SteamCharge.set(first, 300);
		SteamCharge.set(second, 500);
		ItemStack boots = new ItemStack(SteamGear.STEAM_BOOTS);
		SteamCharge.set(boots, 400);
		player.getInventory().setItem(0, empty);
		player.getInventory().setItem(1, boots);
		player.getInventory().setItem(2, first);
		player.getInventory().setItem(3, second);
		helper.assertTrue(SteamCharge.findCharged(player) == first, "첫 찬 통이 아님");
		helper.assertValueEqual(SteamCharge.total(player), 800, "통 합계 (부츠 제외)");
		helper.assertFalse(SteamCharge.drain(player, 801), "모자란데 빼짐");
		helper.assertValueEqual(SteamCharge.total(player), 800, "실패했는데 증기가 줄음");
		helper.assertTrue(SteamCharge.drain(player, 600), "충분한데 못 뺌");
		helper.assertValueEqual(SteamCharge.get(first), 0, "앞 통부터 비워야 함");
		helper.assertValueEqual(SteamCharge.get(second), 200, "뒤 통의 남은 증기");
		helper.assertValueEqual(SteamCharge.get(boots), 400, "부츠 증기를 건드림");
		helper.assertTrue(SteamCharge.findCharged(player) == second, "남은 찬 통");
		helper.succeed();
	}

	// ---- 충전대 ----

	/** 충전대는 모아 둔 증기를 틱당 20씩 통에 채운다 (모자라면 있는 만큼, 가득 차면 멈춤). */
	@GameTest(maxTicks = 100)
	public void chargerFillsCanisterFromSteam(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModPower.STEAM_CHARGER);
		SteamChargerBlockEntity charger = helper.getBlockEntity(pos, SteamChargerBlockEntity.class);
		ItemStack canister = new ItemStack(SteamGear.STEAM_CANISTER);
		charger.setItem(0, canister);
		helper.assertValueEqual(charger.acceptSteam(150), 150, "충전대가 받은 증기");
		helper.assertValueEqual(charger.acceptSteam(150), 50, "용량 200을 넘겨 받음");
		helper.assertValueEqual(charger.steamCapacity(), 200, "충전대 용량");
		helper.assertValueEqual(charger.charge(), 20, "한 틱에 채우는 양");
		helper.assertValueEqual(charger.steamStored(), 180, "채운 만큼 줄어든 증기");
		// 나머지는 서버 틱이 알아서 채운다 (200 → 통 200)
		helper.succeedWhen(() -> {
			helper.assertTrue(SteamCharge.get(canister) >= 180, "통이 안 참: " + SteamCharge.get(canister));
			helper.assertValueEqual(charger.steamStored(), 0, "남은 충전대 증기");
			helper.assertValueEqual(SteamCharge.get(canister), 200, "통의 증기");
		});
	}

	/** 부츠도 같은 방식으로 채우고, 가득 차면 더 받지 않는다. 증기가 없는 칸(막대기 등)은 놓을 수 없다. */
	@GameTest
	public void chargerFillsBootsAndRespectsRate(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModPower.STEAM_CHARGER);
		SteamChargerBlockEntity charger = helper.getBlockEntity(pos, SteamChargerBlockEntity.class);
		ItemStack boots = new ItemStack(SteamGear.STEAM_BOOTS);
		SteamCharge.set(boots, 490);
		charger.setItem(0, boots);
		charger.acceptSteam(200);
		helper.assertValueEqual(charger.charge(), 10, "부츠에 남은 자리만큼");
		helper.assertTrue(SteamCharge.isFull(boots), "부츠가 안 참");
		helper.assertValueEqual(charger.charge(), 0, "가득 찬 부츠에 더 채움");
		helper.assertValueEqual(charger.steamStored(), 190, "쓰지 않은 증기");
		helper.assertFalse(charger.canPlaceItem(0, new ItemStack(Items.STICK)), "막대기를 놓을 수 있음");

		// 설정으로 속도를 바꾸면 따라간다
		try {
			SanupcraftConfig config = SanupcraftConfig.defaults();
			config.chargerSteamPerTick = 5;
			SanupcraftConfig.use(config);
			ItemStack canister = new ItemStack(SteamGear.STEAM_CANISTER);
			charger.setItem(0, canister);
			helper.assertValueEqual(charger.charge(), 5, "설정한 속도");
		} finally {
			SanupcraftConfig.resetToDefaults();
		}
		helper.succeed();
	}

	/** 깔때기: 빈 칸에는 증기 아이템만 들어가고, 가득 찬 것만 나온다. */
	@GameTest
	public void chargerHopperRules(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModPower.STEAM_CHARGER);
		SteamChargerBlockEntity charger = helper.getBlockEntity(pos, SteamChargerBlockEntity.class);
		ItemStack canister = new ItemStack(SteamGear.STEAM_CANISTER);
		HopperBlockEntity.addItem(null, charger, new ItemStack(Items.COAL), Direction.UP);
		helper.assertTrue(charger.isEmpty(), "석탄이 들어감");
		ItemStack rest = HopperBlockEntity.addItem(null, charger, canister.copy(), Direction.UP);
		helper.assertTrue(rest.isEmpty(), "증기 통을 안 받음: " + rest);
		helper.assertTrue(charger.stored().is(SteamGear.STEAM_CANISTER), "통이 안 들어옴");
		ItemStack second = HopperBlockEntity.addItem(null, charger, new ItemStack(SteamGear.STEAM_CANISTER), Direction.UP);
		helper.assertFalse(second.isEmpty(), "찬 칸에 또 들어감");
		helper.assertFalse(charger.canTakeItemThroughFace(0, charger.stored(), Direction.DOWN), "덜 찬 통이 나감");
		SteamCharge.set(charger.stored(), 1000);
		helper.assertTrue(charger.canTakeItemThroughFace(0, charger.stored(), Direction.DOWN), "가득 찬 통이 안 나감");
		helper.succeed();
	}

	/** 보일러 - 파이프 - 충전대: 파이프가 충전대 쪽으로 이어지고, 증기망의 소비자로 잡힌다. */
	@GameTest
	public void chargerJoinsSteamNetwork(GameTestHelper helper) {
		BlockPos boiler = new BlockPos(1, 1, 1);
		BlockPos pipe = new BlockPos(2, 1, 1);
		BlockPos chargerPos = new BlockPos(3, 1, 1);
		// 파이프를 먼저 놓고 양옆을 놓아야 파이프 모양이 이웃을 따라 이어진다
		helper.setBlock(pipe, ModPower.STEAM_PIPE);
		helper.setBlock(boiler, ModPower.BOILER);
		helper.setBlock(chargerPos, ModPower.STEAM_CHARGER);
		BlockState pipeState = helper.getBlockState(pipe);
		helper.assertTrue(pipeState.getValue(SteamPipeBlock.EAST), "파이프가 충전대 쪽으로 안 이어짐");
		SteamChargerBlockEntity charger = helper.getBlockEntity(chargerPos, SteamChargerBlockEntity.class);
		helper.assertTrue(SteamNetwork.findConsumers(helper.getLevel(), helper.absolutePos(boiler)).contains(charger), "증기망이 충전대를 못 찾음");
		helper.succeed();
	}

	// ---- 리벳 ----

	/** 압착기: 철판 1 → 리벳 4. */
	@GameTest(maxTicks = 300)
	public void pressTurnsPlateIntoFourRivets(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.PRESS);
		MachineBlockEntity press = helper.getBlockEntity(pos, MachineBlockEntity.class);
		press.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.IRON_PLATE));
		press.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			helper.assertTrue(press.result().is(SteamGear.RIVET), "결과가 리벳이 아님: " + press.result());
			helper.assertValueEqual(press.result().getCount(), 4, "리벳 개수");
		});
	}

	// ---- 리벳총 ----

	/** 쏘면 리벳 1 + 증기 20을 쓰고 리벳이 하나 날아가며 쿨다운이 걸린다. 쿨다운 중에는 안 나간다. */
	@GameTest
	public void rivetGunConsumesRivetAndSteam(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
		ItemStack gun = new ItemStack(SteamGear.RIVET_GUN);
		player.setItemInHand(InteractionHand.MAIN_HAND, gun);
		ItemStack rivets = new ItemStack(SteamGear.RIVET, 5);
		ItemStack canister = new ItemStack(SteamGear.STEAM_CANISTER);
		SteamCharge.set(canister, 50);
		player.getInventory().setItem(1, rivets);
		player.getInventory().setItem(2, canister);

		helper.assertTrue(RivetGunItem.fire(helper.getLevel(), player, gun) == RivetGunItem.Result.FIRED, "쏘지 못함");
		helper.assertValueEqual(rivets.getCount(), 4, "리벳이 줄지 않음");
		helper.assertValueEqual(SteamCharge.get(canister), 30, "증기 20이 안 빠짐");
		helper.assertTrue(player.getCooldowns().isOnCooldown(gun), "쿨다운이 안 걸림");
		int projectiles = helper.getLevel().getEntitiesOfClass(RivetProjectile.class, player.getBoundingBox().inflate(4.0)).size();
		helper.assertValueEqual(projectiles, 1, "날아가는 리벳 수");
		helper.assertTrue(RivetGunItem.fire(helper.getLevel(), player, gun) == RivetGunItem.Result.COOLDOWN, "쿨다운 중인데 쏨");
		helper.assertValueEqual(rivets.getCount(), 4, "쿨다운 중에 리벳이 줄음");

		// 쿨다운을 풀고 증기를 모자라게: 나가지 않고 아무것도 안 줄어든다
		player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(gun));
		SteamCharge.set(canister, 10);
		helper.assertTrue(RivetGunItem.fire(helper.getLevel(), player, gun) == RivetGunItem.Result.NO_STEAM, "증기 10으로 쏨");
		helper.assertValueEqual(rivets.getCount(), 4, "실패했는데 리벳이 줄음");
		helper.assertValueEqual(SteamCharge.get(canister), 10, "실패했는데 증기가 줄음");
		helper.assertFalse(player.getCooldowns().isOnCooldown(gun), "실패했는데 쿨다운이 걸림");
		// 리벳이 없으면 안 나간다
		SteamCharge.set(canister, 100);
		rivets.setCount(0);
		helper.assertTrue(RivetGunItem.fire(helper.getLevel(), player, gun) == RivetGunItem.Result.NO_RIVET, "리벳 없이 쏨");
		helper.assertValueEqual(SteamCharge.get(canister), 100, "리벳이 없는데 증기가 줄음");
		helper.succeed();
	}

	/** 창작 모드는 리벳 · 증기가 들지 않는다. */
	@GameTest
	public void rivetGunIsFreeInCreative(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
		player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
		ItemStack gun = new ItemStack(SteamGear.RIVET_GUN);
		player.setItemInHand(InteractionHand.MAIN_HAND, gun);
		helper.assertTrue(RivetGunItem.fire(helper.getLevel(), player, gun) == RivetGunItem.Result.FIRED, "창작 모드에서 못 쏨");
		helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(RivetProjectile.class, player.getBoundingBox().inflate(4.0)).size(), 1, "리벳 수");
		helper.succeed();
	}

	/** 날아간 리벳이 몹을 맞히면 피해 6을 주고 사라진다. */
	@GameTest(maxTicks = 100)
	public void rivetDamagesMob(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
		Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(6, 1, 3));
		cow.setNoAi(true);
		float before = cow.getHealth();
		RivetProjectile rivet = new RivetProjectile(helper.getLevel(), player);
		Vec3 start = helper.absoluteVec(new Vec3(2.5, 1.5, 3.5));
		rivet.setPos(start);
		rivet.shoot(1.0, 0.0, 0.0, RivetProjectile.SPEED, 0.0F);
		helper.getLevel().addFreshEntity(rivet);
		helper.succeedWhen(() -> {
			helper.assertValueEqual(cow.getHealth(), before - 6.0F, "소가 입은 피해");
			helper.assertTrue(rivet.isRemoved(), "맞은 리벳이 안 사라짐");
		});
	}

	// ---- 증기 부츠 ----

	/** 공중에서 웅크리기 시작하면 돌진: 증기 40, 속도가 붙고 쿨다운이 걸린다. 땅 · 쿨다운 · 증기 부족에서는 안 나간다. */
	@GameTest
	public void bootsDashCostsSteamAndMoves(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(2.5, 3.0, 2.5)));
		ItemStack boots = new ItemStack(SteamGear.STEAM_BOOTS);
		SteamCharge.set(boots, 100);
		player.setItemSlot(EquipmentSlot.FEET, boots);
		player.setDeltaMovement(Vec3.ZERO);

		// 땅에서 웅크리면 아무 일도 없다
		player.setOnGround(true);
		player.setShiftKeyDown(false);
		SteamBoots.tickPlayer(player);
		player.setShiftKeyDown(true);
		SteamBoots.tickPlayer(player);
		helper.assertValueEqual(SteamCharge.get(boots), 100, "땅에서 돌진함");

		// 공중에서 웅크리기를 새로 시작
		player.setShiftKeyDown(false);
		SteamBoots.tickPlayer(player);
		player.setOnGround(false);
		player.fallDistance = 8.0;
		player.setShiftKeyDown(true);
		SteamBoots.tickPlayer(player);
		helper.assertValueEqual(SteamCharge.get(boots), 60, "돌진 증기 40");
		helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 1.0, "속도가 안 붙음: " + player.getDeltaMovement());
		helper.assertTrue(player.getCooldowns().isOnCooldown(boots), "쿨다운이 안 걸림");
		helper.assertTrue(player.fallDistance <= 4.0, "낙하 거리가 줄지 않음: " + player.fallDistance);

		// 쿨다운 중에 다시 웅크리면 안 나간다
		player.setShiftKeyDown(false);
		SteamBoots.tickPlayer(player);
		player.setShiftKeyDown(true);
		SteamBoots.tickPlayer(player);
		helper.assertValueEqual(SteamCharge.get(boots), 60, "쿨다운 중에 돌진함");

		// 쿨다운이 끝나도 증기가 40 미만이면 안 나간다 (아무것도 안 줄어든다)
		player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(boots));
		SteamCharge.set(boots, 30);
		player.setShiftKeyDown(false);
		SteamBoots.tickPlayer(player);
		player.setShiftKeyDown(true);
		SteamBoots.tickPlayer(player);
		helper.assertValueEqual(SteamCharge.get(boots), 30, "증기가 모자란데 돌진함");
		helper.succeed();
	}

	/** 증기가 있으면 낙하 피해 절반 (바닐라는 7, 부츠는 3), 맞은 하트 1칸당 증기 5. 증기가 없으면 그대로. */
	@GameTest
	public void bootsHalveFallDamage(GameTestHelper helper) {
		var level = helper.getLevel();
		Player plain = helper.makeMockPlayer(GameType.SURVIVAL);
		float plainBefore = plain.getHealth();
		plain.causeFallDamage(10.0, 1.0F, level.damageSources().fall());
		float plainLoss = plainBefore - plain.getHealth();
		helper.assertValueEqual(plainLoss, 7.0F, "맨발 낙하 피해");

		Player wearer = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack boots = new ItemStack(SteamGear.STEAM_BOOTS);
		SteamCharge.set(boots, 100);
		wearer.setItemSlot(EquipmentSlot.FEET, boots);
		SteamBoots.tickPlayer(wearer);
		float before = wearer.getHealth();
		wearer.causeFallDamage(10.0, 1.0F, level.damageSources().fall());
		helper.assertValueEqual(before - wearer.getHealth(), 3.0F, "증기 부츠 낙하 피해");
		helper.assertTrue(SteamCharge.get(boots) < 100 && SteamCharge.get(boots) >= 85, "낙하 피해로 증기가 안 줄었거나 너무 줄음: " + SteamCharge.get(boots));

		// 증기가 바닥나면 일반 피해
		Player dry = helper.makeMockPlayer(GameType.SURVIVAL);
		dry.setItemSlot(EquipmentSlot.FEET, new ItemStack(SteamGear.STEAM_BOOTS));
		SteamBoots.tickPlayer(dry);
		float dryBefore = dry.getHealth();
		dry.causeFallDamage(10.0, 1.0F, level.damageSources().fall());
		helper.assertValueEqual(dryBefore - dry.getHealth(), 7.0F, "증기 없는 부츠 낙하 피해");
		helper.succeed();
	}

	/** 증기 부츠는 강철 한 벌의 신발로 친다 (녹 방어). */
	@GameTest
	public void steamBootsCountAsSteelSet(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SteelGear.STEEL_HELMET));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SteelGear.STEEL_CHESTPLATE));
		player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SteelGear.STEEL_LEGGINGS));
		helper.assertFalse(SteelGear.wearsFullSet(player), "신발이 없는데 한 벌로 침");
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(SteamGear.STEAM_BOOTS));
		helper.assertTrue(SteelGear.wearsFullSet(player), "증기 부츠를 신발로 안 침");
		helper.succeed();
	}

	/** 설정 기본값은 각 클래스의 상수. */
	@GameTest
	public void configDefaultsMatchConstants(GameTestHelper helper) {
		SanupcraftConfig defaults = SanupcraftConfig.defaults();
		helper.assertValueEqual(defaults.rivetGunSteam, RivetGunItem.STEAM_COST, "리벳총 증기");
		helper.assertValueEqual((float) defaults.rivetGunDamage, RivetGunItem.DAMAGE, "리벳 피해");
		helper.assertValueEqual(defaults.rivetGunCooldown, RivetGunItem.COOLDOWN_TICKS, "리벳총 쿨다운");
		helper.assertValueEqual(defaults.bootsDashSteam, SteamBoots.DASH_COST, "돌진 증기");
		helper.assertValueEqual(defaults.bootsDashCooldown, SteamBoots.DASH_COOLDOWN, "돌진 쿨다운");
		helper.assertValueEqual(defaults.chargerSteamPerTick, SteamChargerBlockEntity.TRANSFER_PER_TICK, "충전 속도");
		SanupcraftConfig again = SanupcraftConfig.parse(defaults.toJson()).config();
		helper.assertValueEqual(again.toJson(), defaults.toJson(), "다시 읽은 설정");
		helper.succeed();
	}
}
