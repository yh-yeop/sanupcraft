package io.github.yhyeop.sanupcraft.mining;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 공장이 자원을 직접 캐 온다: 증기 굴착기 (2단계, 강화 기계 틀). */
public final class ModMining {
	public static final QuarryBlock STEAM_QUARRY = ModRegistry.block("steam_quarry", QuarryBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(5.0F, 6.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("steam_quarry"), new BlockEntityType<>(QuarryBlockEntity::new, Set.of(STEAM_QUARRY)));

	private ModMining() {
	}

	public static void init() {
	}
}
