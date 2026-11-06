package io.github.yhyeop.sanupcraft.gear;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 생산 · 장비 (확장 M9): 증기 제트팩 · 증기 드릴 · 전기톱. 셋 다 증기 통과 같은 증기 컴포넌트에 증기를 담고
 * (충전대에서 채운다), 내구도는 쓰지 않는다. 동작은 {@link Jetpack}, {@link SteamDrillItem}, {@link ChainsawItem}, {@link ToolMining}.
 */
public final class ProductionGear {
	public static final ResourceKey<EquipmentAsset> JETPACK_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id("steam_jetpack"));
	/** 방어는 가죽 수준(3)으로 낮다. 제트팩은 "가슴 칸 장비"일 뿐 갑옷 역할은 아니다. */
	public static final ArmorMaterial JETPACK_ARMOR = new ArmorMaterial(
			15, defense(0, 0, 3, 0), 5, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, SteelGear.REPAIRS_STEEL, JETPACK_ASSET);

	/** 가슴 칸, 증기 3000. 점프 키를 누르는 동안 올라간다 (틱당 증기 5). */
	public static final Item STEAM_JETPACK = ModRegistry.item("steam_jetpack", p -> new SteamChargedItem(p, Jetpack.CAPACITY),
			new Item.Properties().humanoidArmor(JETPACK_ARMOR, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON));

	/** 곡괭이 + 삽, 다이아 등급. 증기 2000. */
	public static final Item STEAM_DRILL = ModRegistry.item("steam_drill", SteamDrillItem::new,
			tool(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 8.0F, 3.0F, -2.8F, 0.0F, BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_SHOVEL));

	/** 도끼 + 나무 통째 베기. 증기 2000. */
	public static final Item CHAINSAW = ModRegistry.item("chainsaw", ChainsawItem::new,
			tool(BlockTags.INCORRECT_FOR_IRON_TOOL, 8.0F, 6.0F, -3.0F, 5.0F, BlockTags.MINEABLE_WITH_AXE)
					.delayedComponent(DataComponents.BLOCK_TRANSFORMER, context -> context.getOrThrow(BlockTransformers.AXE)));

	private ProductionGear() {
	}

	public static void init() {
		Jetpack.init();
		ToolMining.init();
	}

	/**
	 * 내구도 없는 증기 공구 속성. {@code incorrect} 태그의 블록은 드롭을 못 받고, {@code mineable}의 블록을 {@code speed}로 캔다.
	 * {@code blockSeconds}가 0보다 크면 방패를 그만큼 못 쓰게 만든다 (도끼처럼).
	 */
	@SafeVarargs
	private static Item.Properties tool(TagKey<Block> incorrect, float speed, float damage, float attackSpeed, float blockSeconds, TagKey<Block>... mineable) {
		HolderGetter<Block> lookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
		List<Tool.Rule> rules = new java.util.ArrayList<>();
		rules.add(Tool.Rule.deniesDrops(lookup.getOrThrow(incorrect)));
		for (TagKey<Block> tag : mineable) {
			rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(tag), speed));
		}
		Item.Properties properties = new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
				.component(DataComponents.TOOL, new Tool(rules, 1.0F, 0, true))
				.attributes(ItemAttributeModifiers.builder()
						.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
						.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
						.build());
		if (blockSeconds > 0) {
			properties.component(DataComponents.WEAPON, new Weapon(0, blockSeconds));
		}
		return properties;
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
