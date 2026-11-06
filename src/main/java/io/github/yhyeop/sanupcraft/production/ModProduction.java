package io.github.yhyeop.sanupcraft.production;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 생산 자동화 (확장 M9-P1): 작물 수확기 · 벌목기. 둘 다 증기 파이프로 돌고 거둔 것을 9칸에 모은다. */
public final class ModProduction {
	public static final ProductionBlock CROP_HARVESTER = ModRegistry.block("crop_harvester",
			p -> new ProductionBlock(p, () -> ModProduction.CROP_HARVESTER_BLOCK_ENTITY, CropHarvesterBlockEntity::new),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.5F)
					.requiresCorrectToolForDrops()
					.sound(SoundType.METAL));

	public static final ProductionBlock TREE_FELLER = ModRegistry.block("tree_feller",
			p -> new ProductionBlock(p, () -> ModProduction.TREE_FELLER_BLOCK_ENTITY, TreeFellerBlockEntity::new),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.5F)
					.requiresCorrectToolForDrops()
					.sound(SoundType.METAL));

	public static final BlockEntityType<CropHarvesterBlockEntity> CROP_HARVESTER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("crop_harvester"), new BlockEntityType<>(CropHarvesterBlockEntity::new, Set.of(CROP_HARVESTER)));

	public static final BlockEntityType<TreeFellerBlockEntity> TREE_FELLER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("tree_feller"), new BlockEntityType<>(TreeFellerBlockEntity::new, Set.of(TREE_FELLER)));

	private ModProduction() {
	}

	public static void init() {
	}
}
