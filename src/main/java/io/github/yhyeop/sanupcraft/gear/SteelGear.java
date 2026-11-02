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
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 공장에서 만드는 모험대 장비. 철보다 튼튼하고(다이아 근처), 한 벌을 다 입으면 녹슨 수호자의 녹 공격을 막는다.
 * 강철 검은 수호자에게 피해가 더 들어간다.
 */
public final class SteelGear {
	/** 강철 장비 수리 재료 (철판). */
	public static final TagKey<Item> REPAIRS_STEEL = TagKey.create(Registries.ITEM, Sanupcraft.id("repairs_steel"));
	public static final ResourceKey<EquipmentAsset> STEEL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id("steel"));

	public static final ArmorMaterial STEEL_ARMOR = new ArmorMaterial(
			22, defense(3, 6, 7, 3), 12, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, REPAIRS_STEEL, STEEL_ASSET);
	public static final ToolMaterial STEEL_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 6.5F, 2.5F, 14, REPAIRS_STEEL);

	public static final Item STEEL_HELMET = armor("steel_helmet", ArmorType.HELMET);
	public static final Item STEEL_CHESTPLATE = armor("steel_chestplate", ArmorType.CHESTPLATE);
	public static final Item STEEL_LEGGINGS = armor("steel_leggings", ArmorType.LEGGINGS);
	public static final Item STEEL_BOOTS = armor("steel_boots", ArmorType.BOOTS);
	public static final Item STEEL_SWORD = ModRegistry.item("steel_sword", new Item.Properties().sword(STEEL_TOOL, 3.0F, -2.4F));

	private SteelGear() {
	}

	public static void init() {
	}

	/** 강철 갑옷 네 부위를 다 입었나. */
	public static boolean wearsFullSet(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(STEEL_HELMET)
				&& entity.getItemBySlot(EquipmentSlot.CHEST).is(STEEL_CHESTPLATE)
				&& entity.getItemBySlot(EquipmentSlot.LEGS).is(STEEL_LEGGINGS)
				&& entity.getItemBySlot(EquipmentSlot.FEET).is(STEEL_BOOTS);
	}

	private static Item armor(String name, ArmorType type) {
		return ModRegistry.item(name, new Item.Properties().humanoidArmor(STEEL_ARMOR, type));
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
