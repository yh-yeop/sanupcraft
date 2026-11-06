package io.github.yhyeop.sanupcraft.gear;

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
 * 확장 3 G2 크롬강 장비 (강철과 내열 사이). 크롬강 주괴 = 강철 주괴 + 크롬 주괴 (합금로).
 * 강철보다 조금 강하고(방어 18 · 단단함 1.5, 검 6.75) 내구도가 높다 (갑옷 내구 배수 28, 도구 800).
 * 한 벌이면 녹 면역이고, 녹슨 수호자의 "강철 한 벌" 판정에도 들어간다 ({@link SteelGear#wearsFullSet}에서 부위마다 인정).
 * 해머 · 굴삭 삽은 강철 것처럼 3x3 ({@link AreaMining}). 강철 장비 + 크롬 주괴로 올려 만들면 마법 부여 · 이름 · 내구도가 그대로 간다
 * (제작대 변환 레시피 {@code crafting_transmute}, 크롬 주괴 수 = 그 장비의 주괴 수).
 */
public final class ChromeSteelGear {
	/** 크롬강 장비 수리 재료 (크롬강 주괴). */
	public static final TagKey<Item> REPAIRS_CHROME_STEEL = TagKey.create(Registries.ITEM, Sanupcraft.id("repairs_chrome_steel"));
	public static final ResourceKey<EquipmentAsset> CHROME_STEEL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id("chrome_steel"));

	/** 부츠 2 · 각반 6 · 흉갑 8 · 투구 2 = 18 (강철 17, 내열 20). */
	public static final ArmorMaterial CHROME_STEEL_ARMOR = new ArmorMaterial(
			28, BronzeGear.defense(2, 6, 8, 2), 12, SoundEvents.ARMOR_EQUIP_IRON, 1.5F, 0.0F, REPAIRS_CHROME_STEEL, CHROME_STEEL_ASSET);
	/** 강철(500 · 6.5 · 2.5)과 내열(900 · 7.5 · 3.0) 사이. 등급은 강철과 같은 철 등급. */
	public static final ToolMaterial CHROME_STEEL_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_IRON_TOOL, 800, 7.0F, 2.75F, 12, REPAIRS_CHROME_STEEL);
	/** 해머 · 굴삭 삽: 캐는 속도는 강철 것처럼 60%. */
	public static final ToolMaterial CHROME_STEEL_AREA_TOOL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_IRON_TOOL, 800, 7.0F * SteelGear.AREA_SPEED_FACTOR, 2.75F, 12, REPAIRS_CHROME_STEEL);

	public static final Item CHROME_STEEL_INGOT = ModRegistry.item("chrome_steel_ingot");
	public static final Item CHROME_STEEL_HELMET = armor("chrome_steel_helmet", ArmorType.HELMET);
	public static final Item CHROME_STEEL_CHESTPLATE = armor("chrome_steel_chestplate", ArmorType.CHESTPLATE);
	public static final Item CHROME_STEEL_LEGGINGS = armor("chrome_steel_leggings", ArmorType.LEGGINGS);
	public static final Item CHROME_STEEL_BOOTS = armor("chrome_steel_boots", ArmorType.BOOTS);
	/** 1 + 3 + 2.75 = 6.75. */
	public static final Item CHROME_STEEL_SWORD = ModRegistry.item("chrome_steel_sword", new Item.Properties().sword(CHROME_STEEL_TOOL, 3.0F, -2.4F));
	public static final Item CHROME_STEEL_HAMMER = ModRegistry.item("chrome_steel_hammer",
			new Item.Properties().pickaxe(CHROME_STEEL_AREA_TOOL, 3.0F, -3.0F));
	public static final Item CHROME_STEEL_EXCAVATOR = ModRegistry.item("chrome_steel_excavator",
			new Item.Properties().shovel(CHROME_STEEL_AREA_TOOL, 2.5F, -3.1F));

	private ChromeSteelGear() {
	}

	/** 등록만 (클래스를 불러 아이템을 올린다). 3x3 캐기는 {@link AreaMining}이 강철 것과 같이 본다. */
	public static void init() {
	}

	private static Item armor(String name, ArmorType type) {
		return ModRegistry.item(name, new Item.Properties().humanoidArmor(CHROME_STEEL_ARMOR, type));
	}
}
