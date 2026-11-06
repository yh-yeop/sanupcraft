package io.github.yhyeop.sanupcraft.end;

import java.util.Set;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.boss.VoidCrystal;
import io.github.yhyeop.sanupcraft.core.ModRegistry;
import io.github.yhyeop.sanupcraft.production.ProductionBlock;

/**
 * M6 엔드 최종: 공허 거상(보스 3, 엔티티 클래스는 boss) · 공허 핵 · 거상 소환석, 공허 응축기(고압 증기), 공허 장비,
 * 후렴 수확기, 차원 창고, 겉날개 가속(증기 통), 버려진 정거장 구조물, 숨결 수집기.
 */
public final class ModEnd {
	private static final ResourceKey<EntityType<?>> VOID_COLOSSUS_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("void_colossus"));
	private static final ResourceKey<EntityType<?>> VOID_CRYSTAL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("void_crystal"));

	/** 제단에 쓰면 공허 거상을 부른다 (엔드, 원점에서 500칸 밖). */
	public static final Item COLOSSUS_SUMMONER = ModRegistry.item("colossus_summoner",
			new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).fireResistant());
	/** 공허 거상의 기여자 보상. 공허 장비 재료. */
	public static final Item VOID_CORE = ModRegistry.item("void_core", new Item.Properties().rarity(Rarity.EPIC).fireResistant());

	public static final VoidCondenserBlock VOID_CONDENSER = ModRegistry.block("void_condenser", VoidCondenserBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE)
			.strength(4.0F, 9.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.NETHERITE_BLOCK)
			.lightLevel(state -> state.getValue(VoidCondenserBlock.LIT) ? 7 : 0));

	public static final ProductionBlock CHORUS_HARVESTER = ModRegistry.block("chorus_harvester",
			p -> new ProductionBlock(p, () -> ModEnd.CHORUS_HARVESTER_BLOCK_ENTITY, ChorusHarvesterBlockEntity::new),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.strength(3.5F)
					.requiresCorrectToolForDrops()
					.sound(SoundType.METAL));

	public static final DimensionalVaultBlock DIMENSIONAL_VAULT = ModRegistry.block("dimensional_vault", DimensionalVaultBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.strength(5.0F, 1200.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
			.lightLevel(state -> 5));

	public static final BreathCollectorBlock BREATH_COLLECTOR = ModRegistry.block("breath_collector", BreathCollectorBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_MAGENTA)
			.strength(3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.GLASS));

	public static final EntityType<VoidColossus> VOID_COLOSSUS = Registry.register(BuiltInRegistries.ENTITY_TYPE, VOID_COLOSSUS_KEY,
			EntityType.Builder.of(VoidColossus::new, MobCategory.MONSTER)
					.sized(2.2F, 4.3F)
					.fireImmune()
					.clientTrackingRange(12)
					.build(VOID_COLOSSUS_KEY));

	public static final EntityType<VoidCrystal> VOID_CRYSTAL = Registry.register(BuiltInRegistries.ENTITY_TYPE, VOID_CRYSTAL_KEY,
			EntityType.Builder.<VoidCrystal>of(VoidCrystal::new, MobCategory.MISC)
					.noLootTable()
					.sized(1.0F, 1.0F)
					.fireImmune()
					.clientTrackingRange(12)
					.updateInterval(Integer.MAX_VALUE)
					.build(VOID_CRYSTAL_KEY));

	public static final BlockEntityType<VoidCondenserBlockEntity> VOID_CONDENSER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("void_condenser"), new BlockEntityType<>(VoidCondenserBlockEntity::new, Set.of(VOID_CONDENSER)));

	public static final BlockEntityType<ChorusHarvesterBlockEntity> CHORUS_HARVESTER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("chorus_harvester"), new BlockEntityType<>(ChorusHarvesterBlockEntity::new, Set.of(CHORUS_HARVESTER)));

	public static final BlockEntityType<DimensionalVaultBlockEntity> DIMENSIONAL_VAULT_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("dimensional_vault"), new BlockEntityType<>(DimensionalVaultBlockEntity::new, Set.of(DIMENSIONAL_VAULT)));

	public static final BlockEntityType<BreathCollectorBlockEntity> BREATH_COLLECTOR_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("breath_collector"), new BlockEntityType<>(BreathCollectorBlockEntity::new, Set.of(BREATH_COLLECTOR)));

	/** 엔드 섬 위를 찾아 놓는 구조물 종류 (버려진 정거장). */
	public static final StructureType<EndIslandStructure> END_ISLAND = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE, Sanupcraft.id("end_island"), () -> EndIslandStructure.CODEC);

	/** 버려진 정거장 (엔드 바깥 섬): 무너진 공중 정거장 · 제단 · 상자. */
	public static final ResourceKey<Structure> ABANDONED_STATION = ResourceKey.create(Registries.STRUCTURE, Sanupcraft.id("abandoned_station"));
	public static final ResourceKey<LootTable> ABANDONED_STATION_CHEST = ResourceKey.create(Registries.LOOT_TABLE, Sanupcraft.id("chests/abandoned_station"));

	private ModEnd() {
	}

	public static void init() {
		// 장비는 블록 뒤에 탭에 들어간다
		VoidGear.init();
		ElytraBoost.init();
		FabricDefaultAttributeRegistry.register(VOID_COLOSSUS, VoidColossus.createAttributes());
	}
}
