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

/** 운반: 컨베이어 벨트와 벨트 모터 (2단계), 엔더 운반기 (엔드 단계). */
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

	/** 엔더 운반기 (엔드 단계). 셜커 껍데기 + 엔더 진주로 만든다. */
	public static final EnderLinkBlock ENDER_SENDER = ModRegistry.block("ender_sender", p -> new EnderLinkBlock(true, p), enderLinkProperties());
	public static final EnderLinkBlock ENDER_RECEIVER = ModRegistry.block("ender_receiver", p -> new EnderLinkBlock(false, p), enderLinkProperties());

	public static final BlockEntityType<BeltBlockEntity> BELT_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("conveyor_belt"), new BlockEntityType<>(BeltBlockEntity::new, Set.of(BELT)));

	public static final BlockEntityType<BeltMotorBlockEntity> BELT_MOTOR_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("belt_motor"), new BlockEntityType<>(BeltMotorBlockEntity::new, Set.of(BELT_MOTOR)));

	public static final BlockEntityType<EnderLinkBlockEntity> ENDER_LINK_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("ender_link"), new BlockEntityType<>(EnderLinkBlockEntity::new, Set.of(ENDER_SENDER, ENDER_RECEIVER)));

	private ModLogistics() {
	}

	public static void init() {
	}

	private static BlockBehaviour.Properties enderLinkProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE)
				.strength(3.5F, 1200.0F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.lightLevel(state -> 5);
	}
}
