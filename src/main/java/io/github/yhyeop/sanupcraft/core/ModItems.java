package io.github.yhyeop.sanupcraft.core;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** 공장과 모험대가 주고받는 재료. */
public final class ModItems {
	/** 분쇄기: 원석 → 가루 2 (+ 부산물). 화로에서 주괴로, 합금로에서 합금 재료로. */
	public static final Item IRON_DUST = ModRegistry.item("iron_dust");
	public static final Item COPPER_DUST = ModRegistry.item("copper_dust");
	public static final Item GOLD_DUST = ModRegistry.item("gold_dust");
	/** 분쇄기: 석탄 → 석탄 가루 2. 강철의 탄소. */
	public static final Item COAL_DUST = ModRegistry.item("coal_dust");
	/** 합금로: 철가루 2 + 석탄 가루 2 → 강철 주괴. 바닐라로는 못 만든다. 강철 장비 · 해머 · 기계의 재료. */
	public static final Item STEEL_INGOT = ModRegistry.item("steel_ingot");
	/** 압착기: 철 주괴 → 철판 (기계 외장). */
	public static final Item IRON_PLATE = ModRegistry.item("iron_plate");
	/** 압착기: 강철 주괴 → 강철판. */
	public static final Item STEEL_PLATE = ModRegistry.item("steel_plate");
	/** 기계 부품 (조립대: 강철판 → 2). 보일러 · 모터 · 굴착기 · 소환석 · 내열 장비의 재료. */
	public static final Item GEAR_PART = ModRegistry.item("gear_part");
	/** 녹슨 수호자 전리품. 공장 2단계로 가는 핵심 부품. */
	public static final Item GUARDIAN_CORE = ModRegistry.item("guardian_core", new Item.Properties().rarity(Rarity.RARE).fireResistant());
	/** 수호자의 핵으로 만드는 2단계 기계(벨트 모터)의 틀. */
	public static final Item REINFORCED_MACHINE_FRAME = ModRegistry.item("reinforced_machine_frame", new Item.Properties().rarity(Rarity.UNCOMMON));
	/** 분쇄기: 수호자의 핵 1 → 4. 내열 장비와 파수꾼 소환석의 재료. */
	public static final Item CORE_SHARD = ModRegistry.item("core_shard", new Item.Properties().rarity(Rarity.UNCOMMON));
	/** 용광로 파수꾼 전리품. 공장 3단계로 가는 핵심 부품 (M3). */
	public static final Item WARDEN_HEART = ModRegistry.item("warden_heart", new Item.Properties().rarity(Rarity.EPIC).fireResistant());

	private ModItems() {
	}

	public static void init() {
	}
}
