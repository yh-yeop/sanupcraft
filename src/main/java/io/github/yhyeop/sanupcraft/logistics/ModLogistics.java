package io.github.yhyeop.sanupcraft.logistics;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 공장 2단계 운반: 컨베이어 벨트와 벨트 모터. */
public final class ModLogistics {
	public static final BeltBlock BELT = ModRegistry.block("conveyor_belt", BeltBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY)
			.strength(1.0F)
			.sound(SoundType.WOOL)
			.noOcclusion());

	public static final BeltMotorBlock BELT_MOTOR = ModRegistry.block("belt_motor", BeltMotorBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final BlockEntityType<BeltBlockEntity> BELT_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("conveyor_belt"), new BlockEntityType<>(BeltBlockEntity::new, Set.of(BELT)));

	public static final BlockEntityType<BeltMotorBlockEntity> BELT_MOTOR_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("belt_motor"), new BlockEntityType<>(BeltMotorBlockEntity::new, Set.of(BELT_MOTOR)));

	private ModLogistics() {
	}

	public static void init() {
	}
}
