package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.GuardianAltarBlock;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/** 첫 마일스톤 순환: 공장 라인 → 강철 장비 → 보스 → 다음 단계 부품. */
public class SanupcraftGameTest {
	private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);

	@GameTest
	public void modLoads(GameTestHelper helper) {
		helper.assertTrue(FabricLoader.getInstance().isModLoaded(Sanupcraft.MOD_ID), "sanupcraft가 로드되지 않음");
		helper.succeed();
	}

	// ---- 공장 ----

	@GameTest(maxTicks = 300)
	public void crusherTurnsRawIronIntoTwoDust(GameTestHelper helper) {
		runMachine(helper, ModMachines.CRUSHER, Items.RAW_IRON, ModItems.IRON_DUST, 2);
	}

	@GameTest(maxTicks = 300)
	public void pressTurnsIngotIntoPlate(GameTestHelper helper) {
		runMachine(helper, ModMachines.PRESS, Items.IRON_INGOT, ModItems.IRON_PLATE, 1);
	}

	@GameTest(maxTicks = 300)
	public void assemblerTurnsPlateIntoGearPart(GameTestHelper helper) {
		runMachine(helper, ModMachines.ASSEMBLER, ModItems.IRON_PLATE, ModItems.GEAR_PART, 1);
	}

	@GameTest(maxTicks = 300)
	public void machineWithoutFuelDoesNothing(GameTestHelper helper) {
		helper.setBlock(MACHINE_POS, ModMachines.CRUSHER);
		MachineBlockEntity machine = helper.getBlockEntity(MACHINE_POS, MachineBlockEntity.class);
		machine.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON));
		helper.runAfterDelay(200, () -> {
			helper.assertTrue(machine.result().isEmpty(), "연료 없이 결과가 나옴");
			helper.succeed();
		});
	}

	/** 한 칸에 재료 8개를 넣으면 연료(석탄 1개 = 1600틱)로 다 처리한다. 처리량 확인. */
	@GameTest(maxTicks = 1300)
	public void crusherProcessesAStack(GameTestHelper helper) {
		helper.setBlock(MACHINE_POS, ModMachines.CRUSHER);
		MachineBlockEntity machine = helper.getBlockEntity(MACHINE_POS, MachineBlockEntity.class);
		machine.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 8));
		machine.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 1));
		helper.succeedWhen(() -> helper.assertValueEqual(machine.result().getCount(), 16, "철가루 개수"));
	}

	@GameTest
	public void recipesLoad(GameTestHelper helper) {
		String[] ids = {
				"crusher", "press", "assembler",
				"crushing/raw_iron", "crushing/iron_ore", "pressing/iron_plate", "assembling/gear_part",
				"iron_ingot_from_smelting_iron_dust",
				"steel_helmet", "steel_chestplate", "steel_leggings", "steel_boots", "steel_sword",
				"guardian_summoner", "guardian_altar", "reinforced_machine_frame",
		};
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String id : ids) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id(id));
			helper.assertTrue(recipes.byKey(key).isPresent(), "레시피가 없음: " + id);
		}
		helper.succeed();
	}

	// ---- 장비 ----

	@GameTest
	public void fullSteelSetIsRustproof(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		wearSteel(player);
		helper.assertTrue(SteelGear.wearsFullSet(player), "강철 한 벌인데 인정 안 됨");
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
		helper.assertFalse(SteelGear.wearsFullSet(player), "부츠가 철인데 한 벌로 인정됨");
		helper.succeed();
	}

	/**
	 * 녹 공격: 강철 한 벌이 없으면 1.5배 아프고 느려진다.
	 * (모의 플레이어는 틱이 돌지 않아 갑옷 방어력은 안 붙는다. 실제로는 강철 쪽이 더 덜 아프다.)
	 */
	@GameTest
	public void rustHitsHarderWithoutSteel(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
		Player steel = helper.makeMockPlayer(GameType.SURVIVAL);
		wearSteel(steel);

		guardian.hitWithRust(helper.getLevel(), bare, 8.0F);
		guardian.hitWithRust(helper.getLevel(), steel, 8.0F);
		float bareLoss = bare.getMaxHealth() - bare.getHealth();
		float steelLoss = steel.getMaxHealth() - steel.getHealth();

		helper.assertTrue(bareLoss >= steelLoss * RustyGuardian.RUST_MULTIPLIER - 0.01F, "맨몸 피해 " + bareLoss + ", 강철 피해 " + steelLoss);
		helper.assertTrue(bare.hasEffect(MobEffects.SLOWNESS), "맨몸인데 구속이 안 걸림");
		helper.assertFalse(steel.hasEffect(MobEffects.SLOWNESS), "강철 한 벌인데 구속이 걸림");
		helper.succeed();
	}

	@GameTest
	public void steelSwordHurtsGuardianMore(GameTestHelper helper) {
		RustyGuardian a = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(1, 1, 1));
		RustyGuardian b = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(5, 1, 5));
		Player withSteel = helper.makeMockPlayer(GameType.SURVIVAL);
		withSteel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SteelGear.STEEL_SWORD));
		Player withIron = helper.makeMockPlayer(GameType.SURVIVAL);
		withIron.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));

		a.hurtServer(helper.getLevel(), a.damageSources().playerAttack(withSteel), 10.0F);
		b.hurtServer(helper.getLevel(), b.damageSources().playerAttack(withIron), 10.0F);
		float steelDamage = a.getMaxHealth() - a.getHealth();
		float ironDamage = b.getMaxHealth() - b.getHealth();
		helper.assertTrue(steelDamage > ironDamage * 1.3F, "강철 검 " + steelDamage + ", 철 검 " + ironDamage);
		helper.succeed();
	}

	// ---- 보스 ----

	@GameTest
	public void guardianHasBossHealth(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(2, 1, 2));
		helper.assertValueEqual(guardian.getMaxHealth(), (float) RustyGuardian.MAX_HEALTH, "최대 체력");
		helper.succeed();
	}

	/** 다른 테스트의 수호자가 근처에 있으면 안 되므로 따로 돌린다. */
	@GameTest(environment = "sanupcraft-test:alone")
	public void altarSummonsOneGuardian(GameTestHelper helper) {
		BlockPos altar = new BlockPos(2, 1, 2);
		helper.setBlock(altar, ModBoss.GUARDIAN_ALTAR);
		RustyGuardian first = GuardianAltarBlock.summon(helper.getLevel(), helper.absolutePos(altar));
		helper.assertTrue(first != null, "제단이 수호자를 부르지 못함");
		helper.assertTrue(GuardianAltarBlock.summon(helper.getLevel(), helper.absolutePos(altar)) == null, "근처에 있는데 또 부름");
		helper.assertEntityPresent(ModBoss.RUSTY_GUARDIAN);
		helper.succeed();
	}

	@GameTest
	public void guardianSummonsMinions(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		int summoned = guardian.summonMinions(helper.getLevel());
		helper.assertValueEqual(summoned, RustyGuardian.MINIONS_PER_SUMMON, "졸개 수");
		helper.assertEntityPresent(EntityTypes.SILVERFISH);
		helper.succeed();
	}

	/** 보스를 잡으면 수호자의 핵 → 2단계 부품의 재료가 나온다. */
	@GameTest
	public void guardianDropsCore(GameTestHelper helper) {
		RustyGuardian guardian = helper.spawn(ModBoss.RUSTY_GUARDIAN, new BlockPos(3, 1, 3));
		guardian.kill(helper.getLevel());
		helper.succeedWhen(() -> helper.assertItemEntityPresent(ModItems.GUARDIAN_CORE));
	}

	// ---- 도우미 ----

	private static void runMachine(GameTestHelper helper, Block block, Item input, Item expected, int count) {
		helper.setBlock(MACHINE_POS, block);
		MachineBlockEntity machine = helper.getBlockEntity(MACHINE_POS, MachineBlockEntity.class);
		machine.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(input));
		machine.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			ItemStack result = machine.result();
			helper.assertTrue(result.is(expected), "결과가 " + expected + "이 아님: " + result);
			helper.assertValueEqual(result.getCount(), count, "결과 개수");
		});
	}

	private static void wearSteel(Player player) {
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SteelGear.STEEL_HELMET));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SteelGear.STEEL_CHESTPLATE));
		player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SteelGear.STEEL_LEGGINGS));
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(SteelGear.STEEL_BOOTS));
	}
}
