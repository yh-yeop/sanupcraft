package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.boss.GuardianAltarBlock;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.logistics.EnderLinkBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;

/** M3 바닐라 연결: 네더(파수꾼 · 내열 세트 효과 · 심장 제련로) → 엔드(엔더 운반기), 길잡이 발전 과제. */
public class SanupcraftM3GameTest {
	/** 네더 쪽 시험 자리. 테스트마다 다른 칸을 써서 겹치지 않게. */
	private static final BlockPos NETHER_BASE = new BlockPos(0, 100, 0);

	// ---- 네더 ----

	@GameTest
	public void heatproofSetGivesFireResistance(GameTestHelper helper) {
		Player suited = helper.makeMockPlayer(GameType.SURVIVAL);
		suited.setItemSlot(EquipmentSlot.HEAD, new ItemStack(HeatproofGear.HEATPROOF_HELMET));
		suited.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HeatproofGear.HEATPROOF_CHESTPLATE));
		suited.setItemSlot(EquipmentSlot.LEGS, new ItemStack(HeatproofGear.HEATPROOF_LEGGINGS));
		suited.setItemSlot(EquipmentSlot.FEET, new ItemStack(HeatproofGear.HEATPROOF_BOOTS));
		Player steel = helper.makeMockPlayer(GameType.SURVIVAL);
		steel.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SteelGear.STEEL_CHESTPLATE));

		helper.assertTrue(HeatproofGear.applySetBonus(suited), "내열 한 벌인데 세트 효과가 안 걸림");
		helper.assertTrue(suited.hasEffect(MobEffects.FIRE_RESISTANCE), "화염 저항이 없음");
		helper.assertFalse(HeatproofGear.applySetBonus(steel), "강철인데 세트 효과가 걸림");
		helper.succeed();
	}

	@GameTest
	public void wardenWakesInNether(GameTestHelper helper) {
		ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
		BlockPos altar = netherSpot(nether, 0);
		nether.setBlockAndUpdate(altar, ModBoss.GUARDIAN_ALTAR.defaultBlockState());
		helper.assertTrue(GuardianAltarBlock.canWakeHere(nether, ModBoss.FURNACE_WARDEN), "네더인데 깨어날 수 없다고 함");
		FurnaceWarden warden = GuardianAltarBlock.summon(nether, altar, ModBoss.FURNACE_WARDEN);
		helper.assertTrue(warden != null, "네더 제단이 파수꾼을 부르지 못함");
		warden.discard();
		nether.setBlockAndUpdate(altar, Blocks.AIR.defaultBlockState());
		helper.succeed();
	}

	@GameTest(maxTicks = 600)
	public void heartForgeDoublesAncientDebris(GameTestHelper helper) {
		runForge(helper, Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP, 2);
	}

	@GameTest(maxTicks = 300)
	public void heartForgeTurnsBlazeRodIntoFourPowder(GameTestHelper helper) {
		runForge(helper, Items.BLAZE_ROD, Items.BLAZE_POWDER, 4);
	}

	// ---- 엔드 ----

	/** 오버월드 송신기 → 네더 수신기 (같은 빨강 채널). 파랑 송신기는 받을 곳이 없어 받지 않는다. */
	@GameTest
	public void enderLinkSendsAcrossDimensions(GameTestHelper helper) {
		ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
		BlockPos receiverPos = netherSpot(nether, 4);
		nether.setBlockAndUpdate(receiverPos, ModLogistics.ENDER_RECEIVER.defaultBlockState());
		EnderLinkBlockEntity receiver = (EnderLinkBlockEntity) nether.getBlockEntity(receiverPos);
		receiver.setChannel(DyeColor.RED);

		BlockPos red = new BlockPos(1, 1, 1);
		BlockPos blue = new BlockPos(3, 1, 1);
		helper.setBlock(red, ModLogistics.ENDER_SENDER);
		helper.setBlock(blue, ModLogistics.ENDER_SENDER);
		EnderLinkBlockEntity redSender = helper.getBlockEntity(red, EnderLinkBlockEntity.class);
		EnderLinkBlockEntity blueSender = helper.getBlockEntity(blue, EnderLinkBlockEntity.class);
		redSender.setChannel(DyeColor.RED);
		blueSender.setChannel(DyeColor.BLUE);

		ItemStack rest = HopperBlockEntity.addItem(null, redSender, new ItemStack(Items.RAW_IRON, 5), null);
		helper.assertTrue(rest.isEmpty(), "송신기가 다 받지 않음: " + rest);
		helper.assertValueEqual(receiver.countItem(Items.RAW_IRON), 5, "네더 수신기에 도착한 원석");
		helper.assertFalse(blueSender.canPlaceItem(0, new ItemStack(Items.RAW_IRON)), "받을 수신기 없는 채널이 아이템을 받음");

		nether.setBlockAndUpdate(receiverPos, Blocks.AIR.defaultBlockState());
		helper.succeed();
	}

	// ---- 길잡이 ----

	@GameTest
	public void advancementGuideLoads(GameTestHelper helper) {
		String[] ids = {"root", "raw_iron", "crusher", "press", "alloy", "hammer", "quarry", "iron_plate", "steel", "guardian_core", "boiler", "belt_motor", "heatproof",
				"warden_summoner", "warden_heart", "heart_forge", "shulker_shell", "ender_link", "geothermal_vent", "heatproof_drill", "ender_anchor", "rusted_shrine", "belt_sorter", "rivet_gun", "steam_boots"};
		var advancements = helper.getLevel().getServer().getAdvancements();
		for (String id : ids) {
			helper.assertTrue(advancements.get(Sanupcraft.id(id)) != null, "발전 과제가 없음: " + id);
		}
		helper.succeed();
	}

	// ---- 실테스트 명령 ----

	@GameTest
	public void playtestBoss1KitEquipsSteel(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.BOSS1);
		helper.assertTrue(SteelGear.wearsFullSet(player), "강철 한 벌을 입히지 않음");
		helper.assertTrue(player.getInventory().countItem(ModBoss.GUARDIAN_SUMMONER) > 0, "소환석을 주지 않음");
		helper.succeed();
	}

	@GameTest
	public void playtestPlatformIsSafeInNether(GameTestHelper helper) {
		ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
		BlockPos spot = PlaytestCommand.safePlatform(nether, NETHER_BASE.offset(16, -40, 16));
		helper.assertTrue(nether.getBlockState(spot.below()).is(Blocks.OBSIDIAN), "발판이 흑요석이 아님");
		helper.assertTrue(nether.getBlockState(spot).isAir() && nether.getBlockState(spot.above()).isAir(), "설 자리가 비어 있지 않음");
		helper.succeed();
	}

	// ---- 도우미 ----

	private static void runForge(GameTestHelper helper, Item input, Item expected, int count) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModMachines.HEART_FORGE);
		MachineBlockEntity forge = helper.getBlockEntity(pos, MachineBlockEntity.class);
		forge.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(input));
		forge.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			helper.assertTrue(forge.result().is(expected), "결과가 " + expected + "이 아님: " + forge.result());
			helper.assertValueEqual(forge.result().getCount(), count, "결과 개수");
		});
	}

	/** 네더의 시험 자리 (청크를 불러온다). */
	private static BlockPos netherSpot(ServerLevel nether, int offset) {
		BlockPos pos = NETHER_BASE.offset(offset, 0, 0);
		nether.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		return pos;
	}
}
