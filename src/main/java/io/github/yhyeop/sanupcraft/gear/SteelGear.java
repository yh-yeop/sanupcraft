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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 강철 주괴(합금로)로 만드는 장비. 철과 다이아 사이 (방어 17 · 단단함 1, 내구도 철의 1.5배).
 * 갑옷 한 벌을 다 입으면(내열 갑옷으로 올려 입어도) 녹슨 수호자의 녹 공격을 막고, 강철 검은 수호자에게 피해가 더 들어간다.
 * 강철 해머 · 굴삭 삽은 3x3으로 캔다 (AreaMining). 대신 캐는 속도가 강철 곡괭이 · 삽의 60%.
 * 강철 주괴 1 = 철가루 2(원석 철 1) + 석탄 가루 2: 철과 같은 원석으로 더 좋은 장비, 대가는 석탄과 기계.
 */
public final class SteelGear {
	/** 강철 장비 수리 재료 (강철 주괴). */
	public static final TagKey<Item> REPAIRS_STEEL = TagKey.create(Registries.ITEM, Sanupcraft.id("repairs_steel"));
	public static final ResourceKey<EquipmentAsset> STEEL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id("steel"));

	public static final ArmorMaterial STEEL_ARMOR = new ArmorMaterial(
			22, defense(2, 6, 7, 2), 10, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, REPAIRS_STEEL, STEEL_ASSET);
	public static final ToolMaterial STEEL_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 6.5F, 2.5F, 10, REPAIRS_STEEL);
	/** 해머 · 굴삭 삽: 강철과 같지만 캐는 속도 60% (9칸을 한 번에 캐는 대가). */
	public static final float AREA_SPEED_FACTOR = 0.6F;
	public static final ToolMaterial STEEL_AREA_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 6.5F * AREA_SPEED_FACTOR, 2.5F, 10, REPAIRS_STEEL);

	public static final Item STEEL_HELMET = armor("steel_helmet", ArmorType.HELMET);
	public static final Item STEEL_CHESTPLATE = armor("steel_chestplate", ArmorType.CHESTPLATE);
	public static final Item STEEL_LEGGINGS = armor("steel_leggings", ArmorType.LEGGINGS);
	public static final Item STEEL_BOOTS = armor("steel_boots", ArmorType.BOOTS);
	public static final Item STEEL_SWORD = ModRegistry.item("steel_sword", new Item.Properties().sword(STEEL_TOOL, 3.0F, -2.4F));
	/** 3x3 곡괭이. 블록마다 내구도를 쓰고, 웅크리면 한 칸만. */
	public static final Item STEEL_HAMMER = ModRegistry.item("steel_hammer", new Item.Properties().pickaxe(STEEL_AREA_TOOL, 3.0F, -3.0F));
	/** 3x3 삽. */
	public static final Item STEEL_EXCAVATOR = ModRegistry.item("steel_excavator", new Item.Properties().shovel(STEEL_AREA_TOOL, 2.5F, -3.1F));

	private SteelGear() {
	}

	public static void init() {
		AreaMining.init();
	}

	/**
	 * 녹 방어 한 벌을 다 입었나. 부위마다 강철 또는 내열이면 된다 (내열은 강철의 윗단계라 섞어 입어도 인정).
	 * 내열로 올려 입은 사람이 핵을 모으려고 수호자를 다시 잡을 때 녹 공격을 맞지 않게.
	 */
	public static boolean wearsFullSet(LivingEntity entity) {
		return wears(entity, EquipmentSlot.HEAD, STEEL_HELMET, HeatproofGear.HEATPROOF_HELMET)
				&& wears(entity, EquipmentSlot.CHEST, STEEL_CHESTPLATE, HeatproofGear.HEATPROOF_CHESTPLATE)
				&& wears(entity, EquipmentSlot.LEGS, STEEL_LEGGINGS, HeatproofGear.HEATPROOF_LEGGINGS)
				&& wears(entity, EquipmentSlot.FEET, STEEL_BOOTS, HeatproofGear.HEATPROOF_BOOTS);
	}

	private static boolean wears(LivingEntity entity, EquipmentSlot slot, Item steel, Item heatproof) {
		ItemStack worn = entity.getItemBySlot(slot);
		return worn.is(steel) || worn.is(heatproof);
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
