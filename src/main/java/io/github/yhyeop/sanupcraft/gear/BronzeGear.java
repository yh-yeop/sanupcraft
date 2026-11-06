package io.github.yhyeop.sanupcraft.gear;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 확장 3 G1 청동 장비 (장비 서열 맨 아래: 청동 → 철 → 강철 → 크롬강 …).
 * 청동 주괴 = 구리 3 + 알루미늄 1: 합금로면 4개, 공장이 없어도 제작대에서 3개 (조금 손해).
 * 철보다 조금 약하다: 갑옷 방어 13 (철 15), 검 5.5 (철 6), 곡괭이 · 도끼 · 삽은 철과 같은 등급이지만 느리고 내구도가 낮다.
 */
public final class BronzeGear {
	/** 청동 장비 수리 재료 (청동 주괴). */
	public static final TagKey<Item> REPAIRS_BRONZE = TagKey.create(Registries.ITEM, Sanupcraft.id("repairs_bronze"));
	public static final ResourceKey<EquipmentAsset> BRONZE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id("bronze"));

	/** 부츠 2 · 각반 4 · 흉갑 5 · 투구 2 = 13. 내구 배수 13 (철 15 · 구리 11). */
	public static final ArmorMaterial BRONZE_ARMOR = new ArmorMaterial(
			13, defense(2, 4, 5, 2), 10, SoundEvents.ARMOR_EQUIP_COPPER, 0.0F, 0.0F, REPAIRS_BRONZE, BRONZE_ASSET);
	/** 철 등급 (철 250 · 6.0 · 2.0) 보다 내구 · 속도 · 공격이 조금 낮다. */
	public static final ToolMaterial BRONZE_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_IRON_TOOL, 220, 5.5F, 1.5F, 12, REPAIRS_BRONZE);

	public static final Item BRONZE_INGOT = ModRegistry.item("bronze_ingot");
	public static final Item BRONZE_HELMET = armor("bronze_helmet", ArmorType.HELMET);
	public static final Item BRONZE_CHESTPLATE = armor("bronze_chestplate", ArmorType.CHESTPLATE);
	public static final Item BRONZE_LEGGINGS = armor("bronze_leggings", ArmorType.LEGGINGS);
	public static final Item BRONZE_BOOTS = armor("bronze_boots", ArmorType.BOOTS);
	/** 1 + 3 + 1.5 = 5.5. */
	public static final Item BRONZE_SWORD = ModRegistry.item("bronze_sword", new Item.Properties().sword(BRONZE_TOOL, 3.0F, -2.4F));
	public static final Item BRONZE_PICKAXE = ModRegistry.item("bronze_pickaxe", new Item.Properties().pickaxe(BRONZE_TOOL, 1.0F, -2.8F));
	public static final Item BRONZE_AXE = ModRegistry.item("bronze_axe", new Item.Properties().axe(BRONZE_TOOL, 6.0F, -3.1F));
	public static final Item BRONZE_SHOVEL = ModRegistry.item("bronze_shovel", new Item.Properties().shovel(BRONZE_TOOL, 1.5F, -3.0F));

	private BronzeGear() {
	}

	/** 등록만 (클래스를 불러 아이템을 올린다). */
	public static void init() {
	}

	private static Item armor(String name, ArmorType type) {
		return ModRegistry.item(name, new Item.Properties().humanoidArmor(BRONZE_ARMOR, type));
	}

	static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
		Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
		map.put(ArmorType.BOOTS, boots);
		map.put(ArmorType.LEGGINGS, legs);
		map.put(ArmorType.CHESTPLATE, chest);
		map.put(ArmorType.HELMET, helmet);
		return map;
	}
}
