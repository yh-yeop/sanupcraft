package io.github.yhyeop.sanupcraft.power;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 공장 2단계 동력: 보일러가 증기를 만들고 파이프로 나눈다. */
public final class ModPower {
	public static final BoilerBlock BOILER = ModRegistry.block("boiler", BoilerBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
			.lightLevel(state -> state.getValue(BoilerBlock.LIT) ? 10 : 0));

	public static final SteamPipeBlock STEAM_PIPE = ModRegistry.block("steam_pipe", SteamPipeBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.strength(1.5F)
			.sound(SoundType.COPPER)
			.noOcclusion());

	public static final BlockEntityType<BoilerBlockEntity> BOILER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("boiler"), new BlockEntityType<>(BoilerBlockEntity::new, Set.of(BOILER)));

	private ModPower() {
	}

	public static void init() {
	}
}
