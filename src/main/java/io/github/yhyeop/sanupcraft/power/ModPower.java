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

/** 공장 2단계 동력: 보일러가 증기를 만들고 파이프로 나눈다. 네더에서는 지열 분출구가 대신한다. */
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

	/** 지열 증기 분출구 (네더 단계): 용암 원천으로 물 · 연료 없이 증기. 네더에서만. */
	public static final GeothermalVentBlock GEOTHERMAL_VENT = ModRegistry.block("geothermal_vent", GeothermalVentBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.NETHER)
			.strength(3.5F, 9.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.NETHER_BRICKS)
			.lightLevel(state -> state.getValue(GeothermalVentBlock.LIT) ? 12 : 3));

	/** 증기 계기판: 붙은 기계 · 보일러의 증기 저장률을 비교기 신호로. */
	public static final SteamGaugeBlock STEAM_GAUGE = ModRegistry.block("steam_gauge", SteamGaugeBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(1.5F)
			.sound(SoundType.METAL)
			.noOcclusion());

	/** 증기 충전대: 파이프에서 받은 증기를 증기 통 · 증기 부츠에 채운다. */
	public static final SteamChargerBlock STEAM_CHARGER = ModRegistry.block("steam_charger", SteamChargerBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final BlockEntityType<BoilerBlockEntity> BOILER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("boiler"), new BlockEntityType<>(BoilerBlockEntity::new, Set.of(BOILER)));

	public static final BlockEntityType<GeothermalVentBlockEntity> GEOTHERMAL_VENT_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("geothermal_vent"), new BlockEntityType<>(GeothermalVentBlockEntity::new, Set.of(GEOTHERMAL_VENT)));

	public static final BlockEntityType<SteamGaugeBlockEntity> STEAM_GAUGE_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("steam_gauge"), new BlockEntityType<>(SteamGaugeBlockEntity::new, Set.of(STEAM_GAUGE)));

	public static final BlockEntityType<SteamChargerBlockEntity> STEAM_CHARGER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("steam_charger"), new BlockEntityType<>(SteamChargerBlockEntity::new, Set.of(STEAM_CHARGER)));

	private ModPower() {
	}

	public static void init() {
	}
}
