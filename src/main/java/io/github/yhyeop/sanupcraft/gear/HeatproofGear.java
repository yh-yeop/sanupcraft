package io.github.yhyeop.sanupcraft.gear;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 2단계 장비. 장비 부품을 많이 쓰고 핵 조각이 들어간다. 강철보다 튼튼하고(네더라이트 근처),
 * 한 벌을 다 입으면 용광로 파수꾼의 과열(불 붙음 + 피해 1.5배)을 막는다. 내열 검은 파수꾼에게 1.5배.
 */
public final class HeatproofGear {
	/** 내열 장비 수리 재료 (핵 조각). */
	public static final TagKey<Item> REPAIRS_HEATPROOF = TagKey.create(Registries.ITEM, Sanupcraft.id("repairs_heatproof"));
	public static final ResourceKey<EquipmentAsset> HEATPROOF_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id("heatproof"));

	public static final ArmorMaterial HEATPROOF_ARMOR = new ArmorMaterial(
			30, defense(3, 6, 8, 3), 12, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.05F, REPAIRS_HEATPROOF, HEATPROOF_ASSET);
	public static final ToolMaterial HEATPROOF_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 900, 7.5F, 3.5F, 14, REPAIRS_HEATPROOF);

	public static final Item HEATPROOF_HELMET = armor("heatproof_helmet", ArmorType.HELMET);
	public static final Item HEATPROOF_CHESTPLATE = armor("heatproof_chestplate", ArmorType.CHESTPLATE);
	public static final Item HEATPROOF_LEGGINGS = armor("heatproof_leggings", ArmorType.LEGGINGS);
	public static final Item HEATPROOF_BOOTS = armor("heatproof_boots", ArmorType.BOOTS);
	public static final Item HEATPROOF_SWORD = ModRegistry.item("heatproof_sword",
			new Item.Properties().sword(HEATPROOF_TOOL, 3.0F, -2.4F).fireResistant().rarity(Rarity.UNCOMMON));

	private HeatproofGear() {
	}

	public static void init() {
	}

	/** 내열 갑옷 네 부위를 다 입었나. */
	public static boolean wearsFullSet(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(HEATPROOF_HELMET)
				&& entity.getItemBySlot(EquipmentSlot.CHEST).is(HEATPROOF_CHESTPLATE)
				&& entity.getItemBySlot(EquipmentSlot.LEGS).is(HEATPROOF_LEGGINGS)
				&& entity.getItemBySlot(EquipmentSlot.FEET).is(HEATPROOF_BOOTS);
	}

	private static Item armor(String name, ArmorType type) {
		return ModRegistry.item(name, new Item.Properties().humanoidArmor(HEATPROOF_ARMOR, type).fireResistant().rarity(Rarity.UNCOMMON));
	}

	private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
		Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
		map.put(ArmorType.BOOTS, boots);
		map.put(ArmorType.LEGGINGS, legs);
		map.put(ArmorType.CHESTPLATE, chest);
		map.put(ArmorType.HELMET, helmet);
		return map;
	}
}
