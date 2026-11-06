package io.github.yhyeop.sanupcraft.fluid;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 유체 (B2): 증기로 도는 펌프가 원천을 빨아 탱크 · 보일러로 보낸다. 양은 mB 정수, 우리 인터페이스 {@link FluidHolder}. */
public final class ModFluid {
	/** 탱크 아이템에 남은 유체. */
	public static final DataComponentType<FluidContents> FLUID = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Sanupcraft.id("fluid"),
			DataComponentType.<FluidContents>builder().persistent(FluidContents.CODEC).networkSynchronized(FluidContents.STREAM_CODEC).build());

	public static final PumpBlock PUMP = ModRegistry.block("pump", PumpBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final TankBlock TANK = ModRegistry.block("tank", TankBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL), TankItem::new);

	public static final BlockEntityType<PumpBlockEntity> PUMP_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("pump"), new BlockEntityType<>(PumpBlockEntity::new, Set.of(PUMP)));

	public static final BlockEntityType<TankBlockEntity> TANK_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("tank"), new BlockEntityType<>(TankBlockEntity::new, Set.of(TANK)));

	private ModFluid() {
	}

	public static void init() {
	}
}
