package io.github.yhyeop.sanupcraft.core;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** 공장과 모험대가 주고받는 재료. */
public final class ModItems {
	/** 분쇄기: 철광석 · 원석 → 철가루 2개. 화로에서 주괴로. */
	public static final Item IRON_DUST = ModRegistry.item("iron_dust");
	/** 압착기: 주괴 → 철판. */
	public static final Item IRON_PLATE = ModRegistry.item("iron_plate");
	/** 조립대: 철판 → 장비 부품. 강철 장비와 소환석의 재료. */
	public static final Item GEAR_PART = ModRegistry.item("gear_part");
	/** 녹슨 수호자 전리품. 공장 2단계로 가는 핵심 부품. */
	public static final Item GUARDIAN_CORE = ModRegistry.item("guardian_core", new Item.Properties().rarity(Rarity.RARE).fireResistant());
	/** 수호자의 핵으로 만드는 2단계 기계의 틀 (M2에서 쓰임). */
	public static final Item REINFORCED_MACHINE_FRAME = ModRegistry.item("reinforced_machine_frame", new Item.Properties().rarity(Rarity.UNCOMMON));

	private ModItems() {
	}

	public static void init() {
	}
}
