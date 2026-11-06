package io.github.yhyeop.sanupcraft.resource;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 확장 3 · D1 새 광석 2종 (M12).
 * 크롬철석: 오버월드 깊은 곳 (심층암만) → 원석 크롬 → 크롬 가루 → 크롬 주괴.
 * 보크사이트: 사바나 · 메사 · 사막 지표 → 원석 알루미늄 → 알루미늄 가루 → 알루미늄 주괴.
 * 광석 배치는 데이터({@code data/sanupcraft/worldgen/feature · placed_feature})이고, 여기서는 바이옴에 붙이기만 한다.
 */
public final class ModResources {
	// ---- 크롬 ----
	public static final Item RAW_CHROMIUM = ModRegistry.item("raw_chromium");
	public static final Item CHROMIUM_DUST = ModRegistry.item("chromium_dust");
	public static final Item REFINED_CHROMIUM_DUST = ModRegistry.item("refined_chromium_dust");
	public static final Item CHROMIUM_INGOT = ModRegistry.item("chromium_ingot");

	// ---- 알루미늄 ----
	public static final Item RAW_ALUMINIUM = ModRegistry.item("raw_aluminium");
	public static final Item ALUMINIUM_DUST = ModRegistry.item("aluminium_dust");
	public static final Item REFINED_ALUMINIUM_DUST = ModRegistry.item("refined_aluminium_dust");
	public static final Item ALUMINIUM_INGOT = ModRegistry.item("aluminium_ingot");

	/** 크롬철석은 심층암 변종뿐이다 (Y -64 ~ -20이라 돌 변종이 나올 자리가 없다). 철 곡괭이부터. */
	public static final Block DEEPSLATE_CHROMITE_ORE = ModRegistry.block("deepslate_chromite_ore",
			p -> new DropExperienceBlock(UniformInt.of(1, 3), p), BlockBehaviour.Properties.of()
					.mapColor(MapColor.DEEPSLATE)
					.requiresCorrectToolForDrops()
					.strength(4.5F, 3.0F)
					.sound(SoundType.DEEPSLATE));
	/** 보크사이트는 돌 곡괭이부터. */
	public static final Block BAUXITE_ORE = ModRegistry.block("bauxite_ore",
			p -> new DropExperienceBlock(UniformInt.of(0, 2), p), BlockBehaviour.Properties.of()
					.mapColor(MapColor.TERRACOTTA_BROWN)
					.requiresCorrectToolForDrops()
					.strength(3.0F, 3.0F)
					.sound(SoundType.STONE));
	public static final Block RAW_CHROMIUM_BLOCK = ModRegistry.block("raw_chromium_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F));
	public static final Block RAW_ALUMINIUM_BLOCK = ModRegistry.block("raw_aluminium_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F));
	public static final Block CHROMIUM_BLOCK = ModRegistry.block("chromium_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL));
	public static final Block ALUMINIUM_BLOCK = ModRegistry.block("aluminium_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.SNOW)
			.requiresCorrectToolForDrops()
			.strength(3.0F, 6.0F)
			.sound(SoundType.METAL));

	public static final ResourceKey<PlacedFeature> CHROMITE_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, Sanupcraft.id("ore_chromite"));
	public static final ResourceKey<PlacedFeature> BAUXITE_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, Sanupcraft.id("ore_bauxite"));

	private ModResources() {
	}

	public static void init() {
		BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), GenerationStep.Decoration.UNDERGROUND_ORES, CHROMITE_PLACED);
		BiomeModifications.addFeature(
				BiomeSelectors.tag(BiomeTags.IS_SAVANNA)
						.or(BiomeSelectors.tag(BiomeTags.IS_BADLANDS))
						.or(BiomeSelectors.includeByKey(Biomes.DESERT)),
				GenerationStep.Decoration.UNDERGROUND_ORES, BAUXITE_PLACED);
	}
}
