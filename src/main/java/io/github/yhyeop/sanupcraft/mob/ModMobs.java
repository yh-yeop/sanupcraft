package io.github.yhyeop.sanupcraft.mob;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 확장 3 C2 새 몹 셋 + 정예: 녹슨 자동인형 · 잿불 정령 · 공허 그림자, 불씨 발사체, 스폰 알, 공허 조각.
 * 바이옴 스폰은 Fabric 바이옴 수정, 자리 규칙은 {@link SpawnPlacements}. 녹슨 사당 안 스폰은 구조물 JSON의 {@code spawn_overrides}.
 */
public final class ModMobs {
	/** 녹슨 자동인형이 나오는 오버월드 바이옴 (평원 · 숲 · 타이가 · 동굴 등). */
	public static final TagKey<Biome> SPAWNS_RUST_AUTOMATON = TagKey.create(Registries.BIOME, Sanupcraft.id("spawns_rust_automaton"));

	// 스폰 무게: 좀비 95 · 엔더맨 10 · 마그마 큐브 100 옆에 둔다
	public static final int AUTOMATON_WEIGHT = 15;
	public static final int WISP_WEIGHT = 15;
	public static final int SHADE_WEIGHT = 4;

	public static final EntityType<RustAutomaton> RUST_AUTOMATON = entity("rust_automaton",
			EntityType.Builder.of(RustAutomaton::new, MobCategory.MONSTER).sized(0.8F, 2.1F).eyeHeight(1.8F).clientTrackingRange(8));
	public static final EntityType<EmberWisp> EMBER_WISP = entity("ember_wisp",
			EntityType.Builder.of(EmberWisp::new, MobCategory.MONSTER).sized(0.7F, 1.2F).eyeHeight(0.8F).fireImmune().clientTrackingRange(8));
	public static final EntityType<VoidShade> VOID_SHADE = entity("void_shade",
			EntityType.Builder.of(VoidShade::new, MobCategory.MONSTER).sized(0.6F, 2.6F).eyeHeight(2.35F).clientTrackingRange(8));
	public static final EntityType<EmberBolt> EMBER_BOLT = entity("ember_bolt",
			EntityType.Builder.<EmberBolt>of(EmberBolt::new, MobCategory.MISC).noLootTable().sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10));

	public static final Item RUST_AUTOMATON_SPAWN_EGG = ModRegistry.item("rust_automaton_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(RUST_AUTOMATON));
	public static final Item EMBER_WISP_SPAWN_EGG = ModRegistry.item("ember_wisp_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(EMBER_WISP));
	public static final Item VOID_SHADE_SPAWN_EGG = ModRegistry.item("void_shade_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(VOID_SHADE));
	/** 공허 그림자가 떨어뜨리는 재료. 지금은 엔더 진주 레시피 (4개 → 1개), 나중에 침식 방지 모듈(G3) 재료. 공허 핵은 보스 전용 그대로. */
	public static final Item VOID_SHARD = ModRegistry.item("void_shard");

	private ModMobs() {
	}

	public static void init() {
		MobSounds.init();
		FabricDefaultAttributeRegistry.register(RUST_AUTOMATON, RustAutomaton.createAttributes());
		FabricDefaultAttributeRegistry.register(EMBER_WISP, EmberWisp.createAttributes());
		FabricDefaultAttributeRegistry.register(VOID_SHADE, VoidShade.createAttributes());

		SpawnPlacements.register(RUST_AUTOMATON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RustAutomaton::checkSpawnRules);
		SpawnPlacements.register(EMBER_WISP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
		SpawnPlacements.register(VOID_SHADE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_RUST_AUTOMATON), MobCategory.MONSTER, RUST_AUTOMATON, AUTOMATON_WEIGHT, 1, 2);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.BASALT_DELTAS, Biomes.NETHER_WASTES), MobCategory.MONSTER, EMBER_WISP, WISP_WEIGHT, 1, 2);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.END_BARRENS), MobCategory.MONSTER,
				VOID_SHADE, SHADE_WEIGHT, 1, 1);
	}

	/** 정예 생성 알 (실테스트용): 알의 엔티티 데이터에 정예 능력을 넣는다. 알 이름은 "정예 ... 생성 알" (몹 이름은 몹이 정한다). */
	public static ItemStack eliteEgg(Item egg, EliteAbility ability) {
		ItemStack stack = new ItemStack(egg);
		TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
		if (data != null) {
			CompoundTag tag = data.copyTagWithoutId();
			tag.putString(EliteMonster.SAVE_KEY, ability.id());
			stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(data.type(), tag));
		}
		stack.set(DataComponents.ITEM_NAME, Component.translatable("entity.sanupcraft.elite", egg.getName(stack)).withStyle(ability.color()));
		return stack;
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
