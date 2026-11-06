package io.github.yhyeop.sanupcraft.module;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.combat.ModCombat;
import io.github.yhyeop.sanupcraft.end.VoidGear;
import io.github.yhyeop.sanupcraft.gear.ChromeSteelGear;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.ProductionGear;
import io.github.yhyeop.sanupcraft.gear.RivetGunItem;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.gear.SteamToolItem;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/**
 * 장비마다 모듈 칸 수 (G3). 한 곳의 표: 청동 · 철 이하 0, 강철 · 다이아 1, 크롬강 · 내열 2, 네더라이트 · 공허 3,
 * 증기 공구(드릴 · 전기톱 · 증기 해머) · 제트팩 · 증기 부츠 · 리벳총 2. 무기는 그 금속을 따른다 (강철 창 1).
 *
 * <p>새 장비를 더하는 법 (둘 중 하나, 큰 값이 이긴다):
 * <ul>
 * <li>코드 한 줄: {@code ModuleSlots.register(아이템, 2);} (종류를 정하려면 {@code register(아이템, 2, GearModule.Kind.WEAPON)})</li>
 * <li>데이터: 아이템 태그 {@code sanupcraft:module_slots/1} · {@code /2} · {@code /3}에 아이템 id를 넣는다</li>
 * </ul>
 * 종류(갑옷 · 도구 · 무기 · 리벳총)는 따로 안 정하면 아이템을 보고 알아서 고른다 ({@link #kinds}).
 */
public final class ModuleSlots {
	/** 한 장비의 최대 칸 (작업대 화면의 칸 수). */
	public static final int MAX = 3;
	public static final TagKey<Item> SLOTS_1 = tag("module_slots/1");
	public static final TagKey<Item> SLOTS_2 = tag("module_slots/2");
	public static final TagKey<Item> SLOTS_3 = tag("module_slots/3");

	private static final Map<Item, Integer> EXTRA = new HashMap<>();
	private static final Map<Item, Set<GearModule.Kind>> EXTRA_KINDS = new HashMap<>();
	/** 기본 표. 다른 장비 클래스를 처음 쓸 때 만든다 (등록 순서 = 탭 순서를 건드리지 않게). */
	private static Map<Item, Integer> table;

	private ModuleSlots() {
	}

	/** 장비 칸 수를 정한다 (다른 묶음 · 나중 장비용). 이미 있으면 큰 값. */
	public static synchronized void register(Item item, int slots) {
		EXTRA.merge(item, Math.max(0, Math.min(MAX, slots)), Math::max);
	}

	/** 칸 수와 종류를 같이 정한다 (아이템만 보고 종류를 못 고를 때). */
	public static synchronized void register(Item item, int slots, GearModule.Kind... kinds) {
		register(item, slots);
		Set<GearModule.Kind> set = EnumSet.noneOf(GearModule.Kind.class);
		set.addAll(java.util.List.of(kinds));
		EXTRA_KINDS.put(item, set);
	}

	/** 이 장비의 모듈 칸 수 (0이면 모듈을 못 끼운다). */
	public static int capacity(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		int slots = table().getOrDefault(stack.getItem(), 0);
		Integer extra = EXTRA.get(stack.getItem());
		if (extra != null) {
			slots = Math.max(slots, extra);
		}
		if (stack.is(SLOTS_3)) {
			slots = Math.max(slots, 3);
		} else if (stack.is(SLOTS_2)) {
			slots = Math.max(slots, 2);
		} else if (stack.is(SLOTS_1)) {
			slots = Math.max(slots, 1);
		}
		return slots;
	}

	/**
	 * 이 장비가 받는 모듈 종류. 갑옷 = 머리 · 가슴 · 다리 · 발에 입는 것, 리벳총, 도구 = 곡괭이 · 삽 · 도끼 · 괭이 태그와 증기 공구,
	 * 무기 = 검 · 창 · 도끼 · 근접 무기 마법 부여 태그와 증기 해머 · 강철 창, 그 밖에 도구가 아니면서 공격력이 있는 것.
	 */
	public static Set<GearModule.Kind> kinds(ItemStack stack) {
		Set<GearModule.Kind> fixed = EXTRA_KINDS.get(stack.getItem());
		if (fixed != null) {
			return fixed;
		}
		Set<GearModule.Kind> kinds = EnumSet.noneOf(GearModule.Kind.class);
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
			kinds.add(GearModule.Kind.ARMOR);
		}
		if (stack.getItem() instanceof RivetGunItem) {
			kinds.add(GearModule.Kind.RIVET_GUN);
		}
		if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.HOES)
				|| stack.getItem() instanceof SteamToolItem) {
			kinds.add(GearModule.Kind.TOOL);
		}
		if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.SPEARS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.MELEE_WEAPON_ENCHANTABLE)
				|| stack.is(ModCombat.STEAM_HAMMER) || stack.is(ModCombat.STEEL_SPEAR)
				|| kinds.isEmpty() && hasAttackDamage(stack)) {
			kinds.add(GearModule.Kind.WEAPON);
		}
		return kinds;
	}

	private static boolean hasAttackDamage(ItemStack stack) {
		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().equals(Attributes.ATTACK_DAMAGE) && entry.modifier().amount() > 0) {
				return true;
			}
		}
		return false;
	}

	private static synchronized Map<Item, Integer> table() {
		if (table == null) {
			Map<Item, Integer> map = new HashMap<>();
			put(map, 1, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS,
					SteelGear.STEEL_SWORD, SteelGear.STEEL_HAMMER, SteelGear.STEEL_EXCAVATOR, ModCombat.STEEL_SPEAR,
					Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
					Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE);
			put(map, 2, ChromeSteelGear.CHROME_STEEL_HELMET, ChromeSteelGear.CHROME_STEEL_CHESTPLATE, ChromeSteelGear.CHROME_STEEL_LEGGINGS,
					ChromeSteelGear.CHROME_STEEL_BOOTS, ChromeSteelGear.CHROME_STEEL_SWORD, ChromeSteelGear.CHROME_STEEL_HAMMER,
					ChromeSteelGear.CHROME_STEEL_EXCAVATOR,
					HeatproofGear.HEATPROOF_HELMET, HeatproofGear.HEATPROOF_CHESTPLATE, HeatproofGear.HEATPROOF_LEGGINGS,
					HeatproofGear.HEATPROOF_BOOTS, HeatproofGear.HEATPROOF_SWORD,
					ProductionGear.STEAM_DRILL, ProductionGear.CHAINSAW, ProductionGear.STEAM_JETPACK, SteamGear.STEAM_BOOTS,
					SteamGear.RIVET_GUN, ModCombat.STEAM_HAMMER);
			put(map, 3, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
					Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
					VoidGear.VOID_HELMET, VoidGear.VOID_CHESTPLATE, VoidGear.VOID_LEGGINGS, VoidGear.VOID_BOOTS, VoidGear.VOID_SWORD);
			table = map;
		}
		return table;
	}

	private static void put(Map<Item, Integer> map, int slots, Item... items) {
		for (Item item : items) {
			map.put(item, slots);
		}
	}

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, Sanupcraft.id(path));
	}
}
