package io.github.yhyeop.sanupcraft.gear.backpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;

import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 배낭 (M9 P5): 27칸 휴대 창고, 강철판 4개로 54칸 강철 배낭 (내용은 그대로 이어진다 - 변환 제작법). */
public final class ModBackpack {
	public static final Item BACKPACK = ModRegistry.item("backpack", p -> new BackpackItem(p, 3), new Item.Properties()
			.stacksTo(1)
			.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));

	public static final Item STEEL_BACKPACK = ModRegistry.item("steel_backpack", p -> new BackpackItem(p, 6), new Item.Properties()
			.stacksTo(1)
			.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));

	private ModBackpack() {
	}

	public static void init() {
	}
}
