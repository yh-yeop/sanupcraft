package io.github.yhyeop.sanupcraft.gear.special;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;
import io.github.yhyeop.sanupcraft.gear.ChromeSteelGear;
import io.github.yhyeop.sanupcraft.gear.SteamChargedItem;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.module.ModuleSlots;

/**
 * 확장 3 G5 특수 장비 · G6 멀티툴.
 * <ul>
 * <li>잠수 헬멧: 증기 1000 = 산소. 물속에서 증기를 조금씩 쓰며 수중 호흡 + 맑은 시야 ({@link DivingHelmet})</li>
 * <li>광부 갑옷 한 벌: 헬멧 = 머리 조명 · 어둠 효과 막기 · 유독 가스 감지 · 가스 면역, 한 벌 = 캐는 속도 +20% · 낙하 피해 -50% ({@link MinerGear})</li>
 * <li>증기 파워 아머 한 벌: 흉갑에 증기 8000. 한 벌 + 증기 = 힘 II · 점프 강화 · 넉백 저항 · 한 칸 반 오르기 ({@link PowerArmor}),
 * 제트팩 모듈을 끼우면 흉갑이 제트팩처럼 뜬다</li>
 * <li>멀티툴: 곡괭이 · 삽 · 도끼 · 괭이 한 자루, 증기 4000 ({@link MultitoolItem})</li>
 * </ul>
 */
public final class ModSpecialGear {
	public static final ResourceKey<EquipmentAsset> DIVING_ASSET = asset("diving_helmet");
	public static final ResourceKey<EquipmentAsset> MINER_ASSET = asset("miner");
	public static final ResourceKey<EquipmentAsset> POWER_ASSET = asset("power_armor");

	/** 잠수 헬멧: 강철 투구와 같은 방어 2. 수리 = 강철 주괴. */
	public static final ArmorMaterial DIVING_ARMOR = new ArmorMaterial(
			22, defense(0, 0, 0, 2), 10, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, SteelGear.REPAIRS_STEEL, DIVING_ASSET);
	/** 광부 갑옷: 2 · 5 · 7 · 2 = 16 (크롬강 18보다 조금 낮다), 단단함 1. 수리 = 크롬강 주괴. */
	public static final ArmorMaterial MINER_ARMOR = new ArmorMaterial(
			25, defense(2, 5, 7, 2), 12, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, ChromeSteelGear.REPAIRS_CHROME_STEEL, MINER_ASSET);
	/** 증기 파워 아머: 네더라이트와 같은 3 · 6 · 8 · 3 = 20, 단단함 3, 넉백 저항 0.1. 수리 = 네더라이트 주괴. */
	public static final ArmorMaterial POWER_ARMOR = new ArmorMaterial(
			37, defense(3, 6, 8, 3), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F, ItemTags.REPAIRS_NETHERITE_ARMOR, POWER_ASSET);

	public static final Item DIVING_HELMET = ModRegistry.item("diving_helmet", p -> new SteamChargedItem(p, DivingHelmet.CAPACITY),
			new Item.Properties().humanoidArmor(DIVING_ARMOR, ArmorType.HELMET).rarity(Rarity.UNCOMMON));

	public static final Item MINER_HELMET = armor("miner_helmet", MINER_ARMOR, ArmorType.HELMET, Rarity.UNCOMMON);
	public static final Item MINER_CHESTPLATE = armor("miner_chestplate", MINER_ARMOR, ArmorType.CHESTPLATE, Rarity.UNCOMMON);
	public static final Item MINER_LEGGINGS = armor("miner_leggings", MINER_ARMOR, ArmorType.LEGGINGS, Rarity.UNCOMMON);
	public static final Item MINER_BOOTS = armor("miner_boots", MINER_ARMOR, ArmorType.BOOTS, Rarity.UNCOMMON);

	public static final Item POWER_HELMET = armor("power_armor_helmet", POWER_ARMOR, ArmorType.HELMET, Rarity.RARE);
	/** 증기 8000을 담는다 (충전대에서 채움). 한 벌 효과와 제트팩 모듈이 이 증기를 쓴다. */
	public static final Item POWER_CHESTPLATE = ModRegistry.item("power_armor_chestplate", p -> new SteamChargedItem(p, PowerArmor.CAPACITY),
			new Item.Properties().humanoidArmor(POWER_ARMOR, ArmorType.CHESTPLATE).fireResistant().rarity(Rarity.RARE));
	public static final Item POWER_LEGGINGS = armor("power_armor_leggings", POWER_ARMOR, ArmorType.LEGGINGS, Rarity.RARE);
	public static final Item POWER_BOOTS = armor("power_armor_boots", POWER_ARMOR, ArmorType.BOOTS, Rarity.RARE);

	/** 곡괭이 + 삽 + 도끼 + 괭이, 다이아 등급. 증기 4000. */
	public static final Item MULTITOOL = ModRegistry.item("multitool", MultitoolItem::new, MultitoolItem.properties());

	private ModSpecialGear() {
	}

	public static void init() {
		ModuleSlots.register(DIVING_HELMET, 1);
		for (Item item : new Item[] {MINER_HELMET, MINER_CHESTPLATE, MINER_LEGGINGS, MINER_BOOTS}) {
			ModuleSlots.register(item, 2);
		}
		for (Item item : new Item[] {POWER_HELMET, POWER_CHESTPLATE, POWER_LEGGINGS, POWER_BOOTS, MULTITOOL}) {
			ModuleSlots.register(item, 3);
		}
		SpecialGearTicks.init();
	}

	private static Item armor(String name, ArmorMaterial material, ArmorType type, Rarity rarity) {
		Item.Properties properties = new Item.Properties().humanoidArmor(material, type).rarity(rarity);
		if (material == POWER_ARMOR) {
			properties.fireResistant();
		}
		return ModRegistry.item(name, properties);
	}

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id(name));
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
