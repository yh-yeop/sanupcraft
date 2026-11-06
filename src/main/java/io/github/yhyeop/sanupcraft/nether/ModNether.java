package io.github.yhyeop.sanupcraft.nether;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;
import io.github.yhyeop.sanupcraft.power.HighPressurePipeBlock;
import io.github.yhyeop.sanupcraft.power.SoulFurnaceBlock;
import io.github.yhyeop.sanupcraft.power.SoulFurnaceBlockEntity;
import io.github.yhyeop.sanupcraft.power.SteamTurbineBlock;
import io.github.yhyeop.sanupcraft.power.SteamTurbineBlockEntity;

/**
 * M5 네더 심화 2: 영혼 화로 · 고압 파이프 · 증기 터빈 (고압 증기망, 블록 클래스는 power), 정밀 부품 (정밀 조립기는 factory),
 * 금 분배기 · 교역 수집판, 잔해 탐지기, 용암 증기선, 버려진 용광로 구조물.
 */
public final class ModNether {
	private static final ResourceKey<EntityType<?>> LAVA_STEAMBOAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("lava_steamboat"));

	/** 영혼 모래 · 영혼 흙 위에서 블레이즈 가루로 고압 증기. */
	public static final SoulFurnaceBlock SOUL_FURNACE = ModRegistry.block("soul_furnace", SoulFurnaceBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.strength(3.5F, 9.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.NETHER_BRICKS)
			.lightLevel(state -> state.getValue(SoulFurnaceBlock.LIT) ? 10 : 0));

	public static final HighPressurePipeBlock HIGH_PRESSURE_PIPE = ModRegistry.block("high_pressure_pipe", HighPressurePipeBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY)
			.strength(2.0F, 6.0F)
			.sound(SoundType.NETHERITE_BLOCK)
			.noOcclusion());

	/** 고압 1 → 보통 3. */
	public static final SteamTurbineBlock STEAM_TURBINE = ModRegistry.block("steam_turbine", SteamTurbineBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	/** 합금로: 석영 1 + 금가루 2 → 금박 석영 (정밀 부품 재료). */
	public static final Item GILDED_QUARTZ = ModRegistry.item("gilded_quartz");
	/** 정밀 조립기: 강철판 + 금박 석영 → 정밀 부품. 최종 장비 재료. */
	public static final Item PRECISION_PART = ModRegistry.item("precision_part", new Item.Properties().rarity(Rarity.UNCOMMON));

	public static final GoldDispenserBlock GOLD_DISPENSER = ModRegistry.block("gold_dispenser", GoldDispenserBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.GOLD)
			.strength(3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final BarterCollectorBlock BARTER_COLLECTOR = ModRegistry.block("barter_collector", BarterCollectorBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final Item DEBRIS_SONAR = ModRegistry.item("debris_sonar", DebrisSonarItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());

	public static final EntityType<LavaSteamboat> LAVA_STEAMBOAT_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, LAVA_STEAMBOAT_KEY,
			EntityType.Builder.<LavaSteamboat>of((type, level) -> new LavaSteamboat(type, level, () -> ModNether.LAVA_STEAMBOAT), MobCategory.MISC)
					.noLootTable()
					.fireImmune()
					.sized(1.375F, 0.5625F)
					.eyeHeight(0.5625F)
					.clientTrackingRange(10)
					.build(LAVA_STEAMBOAT_KEY));

	public static final Item LAVA_STEAMBOAT = ModRegistry.item("lava_steamboat", p -> new BoatItem(LAVA_STEAMBOAT_ENTITY, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());

	public static final BlockEntityType<SoulFurnaceBlockEntity> SOUL_FURNACE_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("soul_furnace"), new BlockEntityType<>(SoulFurnaceBlockEntity::new, Set.of(SOUL_FURNACE)));

	public static final BlockEntityType<SteamTurbineBlockEntity> STEAM_TURBINE_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("steam_turbine"), new BlockEntityType<>(SteamTurbineBlockEntity::new, Set.of(STEAM_TURBINE)));

	public static final BlockEntityType<GoldDispenserBlockEntity> GOLD_DISPENSER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("gold_dispenser"), new BlockEntityType<>(GoldDispenserBlockEntity::new, Set.of(GOLD_DISPENSER)));

	public static final BlockEntityType<BarterCollectorBlockEntity> BARTER_COLLECTOR_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("barter_collector"), new BlockEntityType<>(BarterCollectorBlockEntity::new, Set.of(BARTER_COLLECTOR)));

	/** 네더 바닥을 찾아 놓는 구조물 종류 (버려진 용광로). */
	public static final StructureType<NetherFloorStructure> NETHER_FLOOR = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE, Sanupcraft.id("nether_floor"), () -> NetherFloorStructure.CODEC);

	/** 버려진 용광로 (네더 현무암 삼각주 · 네더 황무지): 부서진 심장 제련로 · 수호자 제단 · 상자. */
	public static final ResourceKey<Structure> ABANDONED_FURNACE = ResourceKey.create(Registries.STRUCTURE, Sanupcraft.id("abandoned_furnace"));
	public static final ResourceKey<LootTable> ABANDONED_FURNACE_CHEST = ResourceKey.create(Registries.LOOT_TABLE, Sanupcraft.id("chests/abandoned_furnace"));

	private ModNether() {
	}

	public static void init() {
	}
}
