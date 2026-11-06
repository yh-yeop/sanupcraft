package io.github.yhyeop.sanupcraft.hazard;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 확장 3 D5 위험 요소: 유독 가스 (깊은 동굴 공기 칸의 보이지 않는 블록)와 무른 바위 (깊은 동굴 천장의 떨어지는 돌).
 * 둘 다 월드 생성 지형지물로 드물게 깔린다 (data/sanupcraft/worldgen/placed_feature). 설정 {@code hazards.*}로 끈다.
 */
public final class ModHazard {
	public static final ToxicGasBlock TOXIC_GAS = registerGas();

	public static final LooseRockBlock LOOSE_ROCK = ModRegistry.block("loose_rock", LooseRockBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.DEEPSLATE)
			.strength(2.5F, 3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.DEEPSLATE));

	public static final ResourceKey<PlacedFeature> TOXIC_GAS_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, Sanupcraft.id("toxic_gas_pocket"));
	public static final ResourceKey<PlacedFeature> LOOSE_ROCK_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, Sanupcraft.id("loose_rock_ceiling"));

	private ModHazard() {
	}

	/** 가스는 아이템이 없다 (손에 넣을 수 없는 블록). */
	private static ToxicGasBlock registerGas() {
		ResourceKey<net.minecraft.world.level.block.Block> key = ResourceKey.create(Registries.BLOCK, Sanupcraft.id("toxic_gas"));
		return Registry.register(BuiltInRegistries.BLOCK, key, new ToxicGasBlock(BlockBehaviour.Properties.of()
				.setId(key)
				.mapColor(MapColor.COLOR_LIGHT_GREEN)
				.replaceable()
				.noCollision()
				.noOcclusion()
				.noLootTable()
				.pushReaction(PushReaction.POPPED)));
	}

	public static void init() {
		register("toxic_gas_pocket", ToxicGasPocketFeature.CODEC);
		register("loose_rock_ceiling", LooseRockFeature.CODEC);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION, TOXIC_GAS_PLACED);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION, LOOSE_ROCK_PLACED);
	}

	private static void register(String name, MapCodec<? extends Feature> codec) {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Sanupcraft.id(name), codec);
	}
}
