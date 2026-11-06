package io.github.yhyeop.sanupcraft.factory;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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
	/** 정밀 조립기 (M5): 고압 증기 전용. */
	public static final MachineBlock PRECISION_ASSEMBLER = machine("precision_assembler", MachineKind.PRECISION);
	/** M8 묶음 B: 세척기 (물 + 증기 · 연료) · 고압 정련기 (고압 증기 전용). */
	public static final MachineBlock WASHER = machine("washer", MachineKind.WASHER);
	public static final MachineBlock HP_REFINER = machine("hp_refiner", MachineKind.REFINER);

	/** 기계 업그레이드 (B1): 기계 화면 왼쪽 업그레이드 칸 2개에 끼운다. */
	public static final MachineUpgradeItem SPEED_UPGRADE = upgrade("speed_upgrade", MachineUpgrade.SPEED, Rarity.UNCOMMON);
	public static final MachineUpgradeItem EFFICIENCY_UPGRADE = upgrade("efficiency_upgrade", MachineUpgrade.EFFICIENCY, Rarity.COMMON);
	public static final MachineUpgradeItem DOUBLE_UPGRADE = upgrade("double_upgrade", MachineUpgrade.DOUBLE, Rarity.RARE);

	public static final BlockEntityType<MachineBlockEntity> MACHINE_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("machine"),
			new BlockEntityType<>(MachineBlockEntity::new, Set.of(CRUSHER, PRESS, ASSEMBLER, ALLOY_SMELTER, HEART_FORGE, PRECISION_ASSEMBLER, WASHER, HP_REFINER)));

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

	private static MachineUpgradeItem upgrade(String name, MachineUpgrade type, Rarity rarity) {
		return (MachineUpgradeItem) ModRegistry.item(name, p -> new MachineUpgradeItem(type, p), new Item.Properties().stacksTo(16).rarity(rarity));
	}

	public static void init() {
		MachineKind.register();
	}
}
