package io.github.yhyeop.sanupcraft.core;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.Sanupcraft;

public final class ModTab {
	private ModTab() {
	}

	/** 모든 등록이 끝난 뒤에 부른다. */
	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Sanupcraft.id("main"), FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.sanupcraft"))
				.icon(() -> new ItemStack(ModItems.GEAR_PART))
				.displayItems((parameters, output) -> ModRegistry.TAB_ITEMS.forEach(output::accept))
				.build());
	}
}
