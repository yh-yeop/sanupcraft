package io.github.yhyeop.sanupcraft.resource.vein;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 확장 3 · D2 광맥 탐사 (M12) 등록: 광맥 탐사기. 광맥 자체는 {@link Veins} (시드로 정함 + 레벨 저장 데이터). */
public final class ModVeins {
	public static final Item VEIN_PROSPECTOR = ModRegistry.item("vein_prospector", ProspectorItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	private ModVeins() {
	}

	public static void init() {
	}
}
