package io.github.yhyeop.sanupcraft.factory;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 공장 1단계: 분쇄기 → 합금로 · 화로 → 압착기 → 조립대. 3단계: 심장 제련로 (네더 재료). */
public final class ModMachines {
	public static final MachineBlock CRUSHER = machine("crusher", MachineKind.CRUSHER);
	public static final MachineBlock PRESS = machine("press", MachineKind.PRESS);
	public static final MachineBlock ASSEMBLER = machine("assembler", MachineKind.ASSEMBLER);
	public static final MachineBlock ALLOY_SMELTER = machine("alloy_smelter", MachineKind.ALLOY);
	public static final MachineBlock HEART_FORGE = machine("heart_forge", MachineKind.FORGE);

	public static final BlockEntityType<MachineBlockEntity> MACHINE_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("machine"),
			new BlockEntityType<>(MachineBlockEntity::new, Set.of(CRUSHER, PRESS, ASSEMBLER, ALLOY_SMELTER, HEART_FORGE)));

	public static final MenuType<MachineMenu> MACHINE_MENU = Registry.register(
			BuiltInRegistries.MENU, Sanupcraft.id("machine"), new MenuType<>(MachineMenu::new, FeatureFlags.VANILLA_SET));

	public static final RecipeBookCategory RECIPE_BOOK_CATEGORY = Registry.register(
			BuiltInRegistries.RECIPE_BOOK_CATEGORY, Sanupcraft.id("machine"), new RecipeBookCategory());

	private ModMachines() {
	}

	private static MachineBlock machine(String name, MachineKind kind) {
		return ModRegistry.block(name, p -> new MachineBlock(kind, p), BlockBehaviour.Properties.of()
				.mapColor(MapColor.METAL)
				.strength(3.5F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.lightLevel(state -> state.getValue(MachineBlock.LIT) ? 8 : 0));
	}

	public static void init() {
		MachineKind.register();
	}
}
