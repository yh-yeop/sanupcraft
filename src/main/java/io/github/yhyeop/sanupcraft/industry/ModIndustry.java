package io.github.yhyeop.sanupcraft.industry;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;
import io.github.yhyeop.sanupcraft.power.BoilerBlock;

/** 산업 규모 (M8): 다중 블록 기계. 대형 보일러 (외벽 33칸 + 제어기). */
public final class ModIndustry {
	public static final BigBoilerCasingBlock BIG_BOILER_CASING = ModRegistry.block("big_boiler_casing", BigBoilerCasingBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(4.0F, 8.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final BigBoilerControllerBlock BIG_BOILER_CONTROLLER = ModRegistry.block("big_boiler_controller", BigBoilerControllerBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(4.0F, 8.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
			.lightLevel(state -> state.getValue(BoilerBlock.LIT) ? 13 : 0));

	public static final BlockEntityType<BigBoilerControllerBlockEntity> BIG_BOILER_CONTROLLER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("big_boiler_controller"),
			new BlockEntityType<>(BigBoilerControllerBlockEntity::new, Set.of(BIG_BOILER_CONTROLLER)));

	private ModIndustry() {
	}

	public static void init() {
	}
}
