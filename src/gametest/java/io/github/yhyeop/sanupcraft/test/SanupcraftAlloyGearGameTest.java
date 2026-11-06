package io.github.yhyeop.sanupcraft.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.effect.ModEffects;
import io.github.yhyeop.sanupcraft.effect.Resistances;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineInput;
import io.github.yhyeop.sanupcraft.factory.MachineKind;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.AreaMining;
import io.github.yhyeop.sanupcraft.gear.BronzeGear;
import io.github.yhyeop.sanupcraft.gear.ChromeSteelGear;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.resource.ModResources;

/** 확장 3 G1 · G2 검사: 청동 · 크롬강 합금, 갑옷 · 무기 수치, 크롬강 한 벌 녹 면역 · 수호자 판정, 3x3, 강철 → 크롬강 올리기. */
public class SanupcraftAlloyGearGameTest {
	private static final Item[] BRONZE_SET = {BronzeGear.BRONZE_HELMET, BronzeGear.BRONZE_CHESTPLATE, BronzeGear.BRONZE_LEGGINGS, BronzeGear.BRONZE_BOOTS};
	private static final Item[] CHROME_SET = {ChromeSteelGear.CHROME_STEEL_HELMET, ChromeSteelGear.CHROME_STEEL_CHESTPLATE,
			ChromeSteelGear.CHROME_STEEL_LEGGINGS, ChromeSteelGear.CHROME_STEEL_BOOTS};
	private static final Item[] STEEL_SET = {SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS};
	private static final Item[] HEATPROOF_SET = {HeatproofGear.HEATPROOF_HELMET, HeatproofGear.HEATPROOF_CHESTPLATE,
			HeatproofGear.HEATPROOF_LEGGINGS, HeatproofGear.HEATPROOF_BOOTS};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** 합금로 레시피: 구리 3 + 알루미늄 1 → 청동 4, 강철 + 크롬 → 크롬강 1. 재료 칸 순서는 상관없다. */
	@GameTest
	public void alloyRecipesMakeBronzeAndChromeSteel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineRecipe bronze = alloy(level, new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(ModResources.ALUMINIUM_INGOT));
		helper.assertTrue(bronze != null && bronze.output().create().is(BronzeGear.BRONZE_INGOT), "청동 합금 레시피가 없음");
		helper.assertValueEqual(bronze.output().create().getCount(), 4, "청동 주괴 수");
		helper.assertTrue(alloy(level, new ItemStack(ModResources.ALUMINIUM_INGOT), new ItemStack(Items.COPPER_INGOT, 3)) != null, "재료 칸을 바꾸면 안 됨");
		helper.assertTrue(alloy(level, new ItemStack(Items.COPPER_INGOT, 2), new ItemStack(ModResources.ALUMINIUM_INGOT)) == null, "구리 2개로 청동이 됨");
		MachineRecipe chrome = alloy(level, new ItemStack(ModItems.STEEL_INGOT), new ItemStack(ModResources.CHROMIUM_INGOT));
		helper.assertTrue(chrome != null && chrome.output().create().is(ChromeSteelGear.CHROME_STEEL_INGOT), "크롬강 합금 레시피가 없음");
		helper.assertValueEqual(chrome.output().create().getCount(), 1, "크롬강 주괴 수");
		helper.succeed();
	}

	/** 합금로를 실제로 돌리면 청동 주괴 4개가 나오고 재료를 쓴다. */
	@GameTest
	public void alloySmelterRunsBronze(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, ModMachines.ALLOY_SMELTER);
		MachineBlockEntity smelter = helper.getBlockEntity(pos, MachineBlockEntity.class);
		smelter.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.COPPER_INGOT, 6));
		smelter.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModResources.ALUMINIUM_INGOT, 1));
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(pos);
		for (int i = 0; i < 400; i++) {
			smelter.acceptSteam(MachineBlockEntity.STEAM_CAPACITY);
			MachineBlockEntity.serverTick(level, abs, level.getBlockState(abs), smelter);
		}
		helper.assertTrue(smelter.result().is(BronzeGear.BRONZE_INGOT), "청동이 아님: " + smelter.result());
		helper.assertValueEqual(smelter.result().getCount(), 4, "한 번 굽기의 청동 수 (알루미늄 하나뿐)");
		helper.assertValueEqual(smelter.getItem(MachineBlockEntity.SLOT_INPUT).getCount(), 3, "남은 구리");
		helper.succeed();
	}

	/** 공장 없이: 제작대에서 구리 3 + 알루미늄 1 → 청동 3. 청동 장비도 제작대 레시피. */
	@GameTest
	public void bronzeCraftsWithoutFactory(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack out = craft(level, 2, 2, List.of(new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT),
				new ItemStack(Items.COPPER_INGOT), new ItemStack(ModResources.ALUMINIUM_INGOT)));
		helper.assertTrue(out.is(BronzeGear.BRONZE_INGOT) && out.getCount() == 3, "제작대 청동이 3개가 아님: " + out);
		ItemStack b = new ItemStack(BronzeGear.BRONZE_INGOT);
		ItemStack s = new ItemStack(Items.STICK);
		ItemStack e = ItemStack.EMPTY;
		helper.assertTrue(craft(level, 3, 3, List.of(b, b, b, e, s, e, e, s, e)).is(BronzeGear.BRONZE_PICKAXE), "청동 곡괭이 레시피");
		helper.assertTrue(craft(level, 3, 3, List.of(b, b, b, b, e, b, b, e, b)).is(BronzeGear.BRONZE_LEGGINGS), "청동 각반 레시피");
		helper.assertTrue(craft(level, 1, 3, List.of(b, s, s)).is(BronzeGear.BRONZE_SHOVEL), "청동 삽 레시피");
		ItemStack c = new ItemStack(ChromeSteelGear.CHROME_STEEL_INGOT);
		helper.assertTrue(craft(level, 3, 3, List.of(c, c, c, c, c, c, e, s, e)).is(ChromeSteelGear.CHROME_STEEL_HAMMER), "크롬강 해머 레시피");
		helper.succeed();
	}

	/** 방어: 청동 13 (철 15보다 낮게), 강철 17, 크롬강 18 (단단함 1.5), 내열 20. 검: 청동 5.5 · 크롬강 6.75 (강철 6.5 · 내열 7 사이). */
	@GameTest
	public void armorAndWeaponValues(GameTestHelper helper) {
		helper.assertValueEqual(armor(BRONZE_SET), 13.0, "청동 방어");
		helper.assertValueEqual(armor(new Item[] {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS}), 15.0, "철 방어");
		helper.assertValueEqual(armor(STEEL_SET), 17.0, "강철 방어");
		helper.assertValueEqual(armor(CHROME_SET), 18.0, "크롬강 방어");
		helper.assertValueEqual(armor(HEATPROOF_SET), 20.0, "내열 방어");
		helper.assertValueEqual(sum(new ItemStack(ChromeSteelGear.CHROME_STEEL_CHESTPLATE), Attributes.ARMOR_TOUGHNESS), 1.5, "크롬강 단단함");
		helper.assertValueEqual(1.0 + sum(new ItemStack(BronzeGear.BRONZE_SWORD), Attributes.ATTACK_DAMAGE), 5.5, "청동 검");
		helper.assertValueEqual(1.0 + sum(new ItemStack(SteelGear.STEEL_SWORD), Attributes.ATTACK_DAMAGE), 6.5, "강철 검");
		helper.assertValueEqual(1.0 + sum(new ItemStack(ChromeSteelGear.CHROME_STEEL_SWORD), Attributes.ATTACK_DAMAGE), 6.75, "크롬강 검");
		// 내구도: 청동 < 철, 크롬강 > 강철
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_PICKAXE).getMaxDamage() < new ItemStack(Items.IRON_PICKAXE).getMaxDamage(), "청동 곡괭이가 철보다 튼튼함");
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_CHESTPLATE).getMaxDamage() < new ItemStack(Items.IRON_CHESTPLATE).getMaxDamage(), "청동 흉갑이 철보다 튼튼함");
		helper.assertTrue(new ItemStack(ChromeSteelGear.CHROME_STEEL_HAMMER).getMaxDamage() > new ItemStack(SteelGear.STEEL_HAMMER).getMaxDamage(), "크롬강 해머 내구");
		helper.assertTrue(new ItemStack(ChromeSteelGear.CHROME_STEEL_CHESTPLATE).getMaxDamage() > new ItemStack(SteelGear.STEEL_CHESTPLATE).getMaxDamage(), "크롬강 흉갑 내구");
		helper.succeed();
	}

	/** 청동 곡괭이 · 도끼 · 삽: 철과 같은 등급 (다이아 광석까지), 대신 철보다 느리다. */
	@GameTest
	public void bronzeToolsAreIronTierButSlower(GameTestHelper helper) {
		ItemStack pick = new ItemStack(BronzeGear.BRONZE_PICKAXE);
		helper.assertTrue(pick.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()), "철광석을 못 캠");
		helper.assertTrue(pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "다이아 광석을 못 캠 (철 등급)");
		helper.assertFalse(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "흑요석을 캠");
		helper.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) < new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState()),
				"청동 곡괭이가 철보다 빠름");
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_AXE).getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1.0F, "청동 도끼가 나무를 못 벰");
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_SHOVEL).getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1.0F, "청동 삽이 흙을 못 팜");
		helper.succeed();
	}

	/** 크롬강 한 벌 = 녹 면역이고 수호자의 "강철 한 벌" 판정에 든다. 강철 · 내열과 섞어도, 하나라도 빠지면 아님. */
	@GameTest
	public void chromeSteelSetIsRustImmune(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		wear(player, CHROME_SET);
		helper.assertTrue(Resistances.rustImmune(player), "크롬강 한 벌인데 녹 면역이 아님");
		helper.assertTrue(SteelGear.wearsFullSet(player), "크롬강 한 벌이 수호자 판정에 안 듦");
		helper.assertFalse(ModEffects.inflict(player, ModEffects.RUST, 100, 2, null), "크롬강 한 벌에 녹이 걸림");
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SteelGear.STEEL_HELMET));
		player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(HeatproofGear.HEATPROOF_LEGGINGS));
		helper.assertTrue(SteelGear.wearsFullSet(player), "강철 · 크롬강 · 내열을 섞은 한 벌이 안 듦");
		player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
		helper.assertFalse(Resistances.rustImmune(player), "부츠가 없는데 녹 면역");
		// 효과가 실제로 걸리는지는 몹으로 (가짜 플레이어는 효과 패킷을 못 보낸다)
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
		wear(zombie, CHROME_SET);
		helper.assertFalse(ModEffects.inflict(zombie, ModEffects.RUST, 100, 1, null), "크롬강 한 벌 좀비에 녹이 걸림");
		wear(zombie, BRONZE_SET);
		helper.assertFalse(Resistances.rustImmune(zombie), "청동 한 벌이 녹 면역");
		helper.assertTrue(ModEffects.inflict(zombie, ModEffects.RUST, 100, 1, null), "청동 한 벌에 녹이 안 걸림");
		helper.succeed();
	}

	/** 크롬강 해머: 강철 해머처럼 3x3 (흑요석처럼 훨씬 단단한 것은 남김), 블록마다 내구도. 굴삭 삽도 3x3 도구. */
	@GameTest
	public void chromeHammerMinesThreeByThree(GameTestHelper helper) {
		BlockPos center = new BlockPos(3, 3, 3);
		List<BlockPos> plane = AreaMining.plane(center, Direction.NORTH);
		helper.setBlock(center, Blocks.STONE);
		for (BlockPos pos : plane) {
			helper.setBlock(pos, Blocks.STONE);
		}
		BlockPos hard = plane.get(0);
		helper.setBlock(hard, Blocks.OBSIDIAN);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ChromeSteelGear.CHROME_STEEL_HAMMER));
		int broken = AreaMining.breakAround(helper.getLevel(), player, helper.absolutePos(center), Blocks.STONE.defaultBlockState(), Direction.NORTH);
		helper.assertValueEqual(broken, 7, "같이 캔 블록 수 (흑요석 제외)");
		helper.assertBlockPresent(Blocks.OBSIDIAN, hard);
		helper.assertTrue(player.getMainHandItem().getDamageValue() >= 7, "해머 내구도를 안 씀");
		helper.assertTrue(AreaMining.isAreaTool(new ItemStack(ChromeSteelGear.CHROME_STEEL_EXCAVATOR)), "크롬강 굴삭 삽이 3x3 도구가 아님");
		float speed = new ItemStack(ChromeSteelGear.CHROME_STEEL_HAMMER).getDestroySpeed(Blocks.STONE.defaultBlockState());
		helper.assertValueEqual(speed, ChromeSteelGear.CHROME_STEEL_TOOL.speed() * SteelGear.AREA_SPEED_FACTOR, "크롬강 해머 속도");
		helper.assertTrue(speed > new ItemStack(SteelGear.STEEL_HAMMER).getDestroySpeed(Blocks.STONE.defaultBlockState()), "크롬강 해머가 강철 해머보다 느림");
		helper.succeed();
	}

	/** 강철 장비 + 크롬 주괴 → 크롬강 장비: 마법 부여 · 이름 · 닳은 정도가 그대로 간다. 크롬 주괴 수가 모자라면 안 된다. */
	@GameTest
	public void upgradeKeepsEnchantments(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Holder<Enchantment> sharpness = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
		Holder<Enchantment> protection = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		ItemStack sword = new ItemStack(SteelGear.STEEL_SWORD);
		sword.enchant(sharpness, 3);
		sword.set(DataComponents.CUSTOM_NAME, Component.literal("아끼는 검"));
		sword.setDamageValue(40);
		ItemStack out = craft(level, 3, 1, List.of(sword, new ItemStack(ModResources.CHROMIUM_INGOT), new ItemStack(ModResources.CHROMIUM_INGOT)));
		helper.assertTrue(out.is(ChromeSteelGear.CHROME_STEEL_SWORD), "강철 검 + 크롬 2 → 크롬강 검이 아님: " + out);
		helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(sharpness, out), 3, "날카로움이 안 옮겨짐");
		helper.assertValueEqual(out.getHoverName().getString(), "아끼는 검", "이름이 안 옮겨짐");
		helper.assertValueEqual(out.getDamageValue(), 40, "닳은 정도");
		helper.assertTrue(craft(level, 2, 1, List.of(sword, new ItemStack(ModResources.CHROMIUM_INGOT))).isEmpty(), "크롬 1개로 올려짐");

		ItemStack chest = new ItemStack(SteelGear.STEEL_CHESTPLATE);
		chest.enchant(protection, 2);
		List<ItemStack> grid = new ArrayList<>(Collections.nCopies(8, new ItemStack(ModResources.CHROMIUM_INGOT)));
		grid.add(4, chest);
		ItemStack chromeChest = craft(level, 3, 3, grid);
		helper.assertTrue(chromeChest.is(ChromeSteelGear.CHROME_STEEL_CHESTPLATE), "강철 흉갑 + 크롬 8 → 크롬강 흉갑이 아님: " + chromeChest);
		helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(protection, chromeChest), 2, "보호가 안 옮겨짐");
		helper.succeed();
	}

	/** 크롬강 갑옷 · 검에서도 내열로 올릴 수 있다 (강철 → 크롬강 → 내열이 끊기지 않게). */
	@GameTest
	public void heatproofFromChromeSteel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack g = new ItemStack(ModItems.GEAR_PART);
		ItemStack shard = new ItemStack(ModItems.CORE_SHARD);
		ItemStack helmet = new ItemStack(ChromeSteelGear.CHROME_STEEL_HELMET);
		helper.assertTrue(craft(level, 3, 2, List.of(g, shard, g, g, helmet, g)).is(HeatproofGear.HEATPROOF_HELMET), "크롬강 투구 → 내열 투구");
		ItemStack sword = new ItemStack(ChromeSteelGear.CHROME_STEEL_SWORD);
		helper.assertTrue(craft(level, 1, 3, List.of(shard, g, sword)).is(HeatproofGear.HEATPROOF_SWORD), "크롬강 검 → 내열 검");
		helper.succeed();
	}

	/** 태그: 갑옷 · 도구 바닐라 태그 (마법 부여대), 수리 재료, 규약 주괴 태그. */
	@GameTest
	public void tagsAreSet(GameTestHelper helper) {
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_HELMET).is(net.minecraft.tags.ItemTags.HEAD_ARMOR), "청동 투구 태그");
		helper.assertTrue(new ItemStack(ChromeSteelGear.CHROME_STEEL_BOOTS).is(net.minecraft.tags.ItemTags.FOOT_ARMOR), "크롬강 부츠 태그");
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_AXE).is(net.minecraft.tags.ItemTags.AXES), "청동 도끼 태그");
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_PICKAXE).is(net.minecraft.tags.ItemTags.PICKAXES), "청동 곡괭이 태그");
		helper.assertTrue(new ItemStack(ChromeSteelGear.CHROME_STEEL_HAMMER).is(net.minecraft.tags.ItemTags.PICKAXES), "크롬강 해머 태그");
		helper.assertTrue(new ItemStack(ChromeSteelGear.CHROME_STEEL_SWORD).is(net.minecraft.tags.ItemTags.SWORDS), "크롬강 검 태그");
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_INGOT).is(BronzeGear.REPAIRS_BRONZE), "청동 수리 재료");
		helper.assertTrue(new ItemStack(ChromeSteelGear.CHROME_STEEL_INGOT).is(ChromeSteelGear.REPAIRS_CHROME_STEEL), "크롬강 수리 재료");
		helper.assertTrue(new ItemStack(BronzeGear.BRONZE_INGOT).is(net.minecraft.tags.TagKey.create(Registries.ITEM,
				net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ingots"))), "c:ingots 태그");
		helper.succeed();
	}

	/** 실테스트 꾸러미 `alloys`: 합금 재료 · 합금로 · 두 세트 · 올려 볼 마법 부여 강철 검. */
	@GameTest
	public void playtestAlloysKit(GameTestHelper helper) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.ALLOYS);
		helper.assertTrue(player.getInventory().countItem(ModResources.ALUMINIUM_INGOT) > 0, "알루미늄 주괴를 주지 않음");
		helper.assertTrue(player.getInventory().countItem(ModMachines.ALLOY_SMELTER.asItem()) > 0, "합금로를 주지 않음");
		helper.assertTrue(player.getInventory().countItem(BronzeGear.BRONZE_CHESTPLATE) > 0, "청동 흉갑을 주지 않음");
		helper.assertTrue(player.getInventory().countItem(ChromeSteelGear.CHROME_STEEL_HAMMER) > 0, "크롬강 해머를 주지 않음");
		boolean enchantedSword = false;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			enchantedSword |= stack.is(SteelGear.STEEL_SWORD) && stack.isEnchanted();
		}
		helper.assertTrue(enchantedSword, "마법 부여된 강철 검을 주지 않음");
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Sanupcraft.id("chrome_steel")) != null, "크롬강 발전 과제");
		helper.succeed();
	}

	private static MachineRecipe alloy(ServerLevel level, ItemStack first, ItemStack second) {
		return level.recipeAccess().getRecipeFor(MachineKind.ALLOY.recipeType(), new MachineInput(first, second), level)
				.map(RecipeHolder::value).orElse(null);
	}

	private static ItemStack craft(ServerLevel level, int width, int height, List<ItemStack> items) {
		CraftingInput input = CraftingInput.of(width, height, items);
		return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	private static double armor(Item[] set) {
		double total = 0;
		for (Item item : set) {
			total += sum(new ItemStack(item), Attributes.ARMOR);
		}
		return total;
	}

	private static double sum(ItemStack stack, Holder<Attribute> attribute) {
		double total = 0;
		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().equals(attribute)) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}

	private static void wear(net.minecraft.world.entity.LivingEntity player, Item[] set) {
		for (int i = 0; i < SLOTS.length; i++) {
			player.setItemSlot(SLOTS[i], new ItemStack(set[i]));
		}
	}
}
