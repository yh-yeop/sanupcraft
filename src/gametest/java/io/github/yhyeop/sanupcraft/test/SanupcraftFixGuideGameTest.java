package io.github.yhyeop.sanupcraft.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/** 1.5단계 리팩터링 수정 검사 (REFACTOR.md 2-5, 묶음 Guide): 장비 레시피 · 길잡이 · 언어 파일 문구 키. */
public class SanupcraftFixGuideGameTest {
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s");

	/** 새로 넣었거나 바꾼 문구 키와 코드가 넘기는 인수 개수. 코드(F1~F3)와 어긋나면 화면에 이상한 글이 뜬다. */
	private static final Map<String, Integer> ARGUMENT_COUNTS = Map.ofEntries(
			Map.entry("block.sanupcraft.steam_quarry.not_started", 2),
			Map.entry("block.sanupcraft.steam_quarry.blocked", 2),
			Map.entry("block.sanupcraft.ender_link.sender_status", 2),
			Map.entry("block.sanupcraft.ender_receiver.titled", 2),
			Map.entry("gui.sanupcraft.machine.steam", 2),
			Map.entry("block.sanupcraft.steam_quarry.no_steam", 4),
			Map.entry("block.sanupcraft.ender_link.no_receiver", 1));

	/** 인수가 없는 새 문구 키. */
	private static final String[] PLAIN_KEYS = {
			"advancements.sanupcraft.press.title", "advancements.sanupcraft.press.description",
			"advancements.sanupcraft.crusher.description", "advancements.sanupcraft.iron_plate.description",
			"advancements.sanupcraft.alloy.description", "advancements.sanupcraft.hammer.description",
			"advancements.sanupcraft.steel.description", "advancements.sanupcraft.guardian_core.description",
			"advancements.sanupcraft.boiler.description", "advancements.sanupcraft.belt_motor.description",
			"advancements.sanupcraft.heatproof.description", "advancements.sanupcraft.quarry.description",
			"advancements.sanupcraft.ender_link.description",
			"block.sanupcraft.guardian_altar.busy", "block.sanupcraft.guardian_altar.no_room", "block.sanupcraft.guardian_altar.peaceful",
			"entity.sanupcraft.rusty_guardian.rust_warning", "entity.sanupcraft.furnace_warden.heat_warning",
			"block.sanupcraft.boiler.water_full", "block.sanupcraft.boiler.other_fuel",
			"block.sanupcraft.boiler.hint.no_water", "block.sanupcraft.boiler.hint.no_fuel",
			"block.sanupcraft.boiler.hint.steam_full", "block.sanupcraft.boiler.hint.ok",
			"gui.sanupcraft.machine.slot.input", "gui.sanupcraft.machine.slot.input2", "gui.sanupcraft.machine.slot.fuel",
			"gui.sanupcraft.machine.slot.result", "gui.sanupcraft.machine.slot.byproduct",
			"block.sanupcraft.crusher.tooltip", "block.sanupcraft.crusher.tooltip2",
			"block.sanupcraft.press.tooltip", "block.sanupcraft.assembler.tooltip",
			"block.sanupcraft.alloy_smelter.tooltip", "block.sanupcraft.heart_forge.tooltip",
			"item.sanupcraft.steel_helmet.tooltip", "item.sanupcraft.steel_chestplate.tooltip",
			"item.sanupcraft.steel_leggings.tooltip", "item.sanupcraft.steel_boots.tooltip",
			"item.sanupcraft.heatproof_helmet.tooltip", "item.sanupcraft.heatproof_chestplate.tooltip",
			"item.sanupcraft.heatproof_leggings.tooltip", "item.sanupcraft.heatproof_boots.tooltip",
			"item.sanupcraft.steel_sword.tooltip", "item.sanupcraft.heatproof_sword.tooltip",
			"item.sanupcraft.steel_hammer.tooltip", "item.sanupcraft.steel_excavator.tooltip",
			"item.sanupcraft.guardian_summoner.tooltip", "item.sanupcraft.warden_summoner.tooltip",
			"item.sanupcraft.guardian_core.tooltip",
			"block.sanupcraft.boiler.tooltip", "block.sanupcraft.steam_quarry.tooltip", "block.sanupcraft.conveyor_belt.tooltip",
			"block.sanupcraft.ender_sender.tooltip", "block.sanupcraft.ender_receiver.tooltip",
	};

	// ---- 내열 = 강철 장비 업그레이드 ----

	@GameTest
	public void heatproofArmorRecipesNeedSteelArmor(GameTestHelper helper) {
		assertNeeds(helper, "heatproof_helmet", SteelGear.STEEL_HELMET);
		assertNeeds(helper, "heatproof_chestplate", SteelGear.STEEL_CHESTPLATE);
		assertNeeds(helper, "heatproof_leggings", SteelGear.STEEL_LEGGINGS);
		assertNeeds(helper, "heatproof_boots", SteelGear.STEEL_BOOTS);
		helper.succeed();
	}

	@GameTest
	public void heatproofSwordRecipeNeedsSteelSword(GameTestHelper helper) {
		assertNeeds(helper, "heatproof_sword", SteelGear.STEEL_SWORD);
		helper.assertFalse(needs(helper, "heatproof_sword", Items.STICK), "내열 검 레시피에 막대가 남아 있음");
		helper.succeed();
	}

	@GameTest
	public void enderSenderRecipeNeedsWardenHeart(GameTestHelper helper) {
		assertNeeds(helper, "ender_sender", ModItems.WARDEN_HEART);
		helper.succeed();
	}

	@GameTest
	public void heatproofSwordHitsAsHardAsDiamond(GameTestHelper helper) {
		double heatproof = attackOf(HeatproofGear.HEATPROOF_SWORD);
		double diamond = attackOf(Items.DIAMOND_SWORD);
		helper.assertTrue(Math.abs(heatproof - diamond) < 0.001, "내열 검 공격력 " + heatproof + ", 다이아 검 " + diamond);
		helper.succeed();
	}

	@GameTest
	public void mixedSteelAndHeatproofCountsAsRustproof(GameTestHelper helper) {
		Player mixed = helper.makeMockPlayer(GameType.SURVIVAL);
		mixed.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SteelGear.STEEL_HELMET));
		mixed.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HeatproofGear.HEATPROOF_CHESTPLATE));
		mixed.setItemSlot(EquipmentSlot.LEGS, new ItemStack(HeatproofGear.HEATPROOF_LEGGINGS));
		mixed.setItemSlot(EquipmentSlot.FEET, new ItemStack(SteelGear.STEEL_BOOTS));
		helper.assertTrue(SteelGear.wearsFullSet(mixed), "강철 + 내열을 섞은 한 벌이 녹 방어로 인정되지 않음");

		Player missing = helper.makeMockPlayer(GameType.SURVIVAL);
		missing.setItemSlot(EquipmentSlot.HEAD, new ItemStack(HeatproofGear.HEATPROOF_HELMET));
		missing.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SteelGear.STEEL_CHESTPLATE));
		missing.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SteelGear.STEEL_LEGGINGS));
		helper.assertFalse(SteelGear.wearsFullSet(missing), "부츠가 없는데 녹 방어로 인정됨");
		helper.succeed();
	}

	// ---- 길잡이 ----

	@GameTest
	public void pressAdvancementSitsBetweenCrusherAndAlloy(GameTestHelper helper) {
		var advancements = helper.getLevel().getServer().getAdvancements();
		var press = advancements.get(Sanupcraft.id("press"));
		var alloy = advancements.get(Sanupcraft.id("alloy"));
		helper.assertTrue(press != null && alloy != null, "press 또는 alloy 발전 과제가 없음");
		helper.assertTrue(press.value().parent().filter(Sanupcraft.id("crusher")::equals).isPresent(), "press의 부모가 crusher가 아님");
		helper.assertTrue(alloy.value().parent().filter(Sanupcraft.id("press")::equals).isPresent(), "alloy의 부모가 press가 아님");
		helper.succeed();
	}

	// ---- 문구 키 ----

	/** ko_kr · en_us 둘 다에 새 키가 있고, %s 개수가 코드가 넘기는 인수 개수와 같다. */
	@GameTest
	public void langKeysExistWithMatchingArguments(GameTestHelper helper) {
		JsonObject ko = readLang("ko_kr.json");
		JsonObject en = readLang("en_us.json");
		for (String key : PLAIN_KEYS) {
			checkKey(helper, key, 0, ko, en);
		}
		for (var entry : ARGUMENT_COUNTS.entrySet()) {
			checkKey(helper, entry.getKey(), entry.getValue(), ko, en);
		}
		helper.succeed();
	}

	// ---- 도우미 ----

	private static void checkKey(GameTestHelper helper, String key, int expected, JsonObject ko, JsonObject en) {
		for (var lang : Map.of("ko_kr", ko, "en_us", en).entrySet()) {
			helper.assertTrue(lang.getValue().has(key), lang.getKey() + "에 문구가 없음: " + key);
			String text = lang.getValue().get(key).getAsString();
			helper.assertValueEqual(argumentCount(text), expected, lang.getKey() + "의 인수 개수: " + key);
		}
	}

	/** %s는 차례대로, %2$s는 번호대로 센다. 쓰는 인수의 최대 번호가 곧 코드가 넘겨야 할 개수. */
	private static int argumentCount(String text) {
		Matcher matcher = PLACEHOLDER.matcher(text);
		int sequential = 0;
		int highest = 0;
		while (matcher.find()) {
			if (matcher.group(1) == null) {
				sequential++;
				highest = Math.max(highest, sequential);
			} else {
				highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
			}
		}
		return highest;
	}

	private static JsonObject readLang(String file) {
		String path = "/assets/sanupcraft/lang/" + file;
		try (InputStream in = SanupcraftFixGuideGameTest.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("언어 파일이 없음: " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException("언어 파일을 읽지 못함: " + path, e);
		}
	}

	private static void assertNeeds(GameTestHelper helper, String recipe, Item ingredient) {
		helper.assertTrue(needs(helper, recipe, ingredient), recipe + " 레시피 재료에 " + ingredient + "이 없음");
	}

	/** 레시피(placementInfo)의 재료 중 이 아이템을 받는 칸이 있나. */
	private static boolean needs(GameTestHelper helper, String recipe, Item item) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id(recipe));
		var holder = helper.getLevel().getServer().getRecipeManager().byKey(key);
		helper.assertTrue(holder.isPresent(), "레시피가 없음: " + recipe);
		return holder.get().value().placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(item)));
	}

	private static double attackOf(Item sword) {
		return new ItemStack(sword).get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ATTACK_DAMAGE, 0.0, EquipmentSlot.MAINHAND);
	}
}
