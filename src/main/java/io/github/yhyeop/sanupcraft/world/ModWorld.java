package io.github.yhyeop.sanupcraft.world;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.core.ModItems;

/**
 * 월드에서 보스를 만나는 길.
 * 오버월드: 녹슨 사당 구조물 (data/sanupcraft/worldgen, 배치는 tools/make_structures.py) — 제단과 소환석이 든 상자.
 * 네더: 바닐라 요새 상자에 파수꾼 소환석 · 핵 조각, 보루 상자에 핵 조각 · 강철 (공장 없이도 네더 탐험으로 보스 2에 닿는다).
 */
public final class ModWorld {
	public static final ResourceKey<Structure> RUSTED_SHRINE = ResourceKey.create(Registries.STRUCTURE, Sanupcraft.id("rusted_shrine"));
	public static final ResourceKey<LootTable> RUSTED_SHRINE_CHEST = ResourceKey.create(Registries.LOOT_TABLE, Sanupcraft.id("chests/rusted_shrine"));

	/** 요새 상자 하나에서 파수꾼 소환석이 나올 확률 = 2 / (2 + 3 + 5) = 20%, 핵 조각 30%. */
	static final int FORTRESS_SUMMONER_WEIGHT = 2;
	static final int FORTRESS_SHARD_WEIGHT = 3;
	static final int FORTRESS_EMPTY_WEIGHT = 5;

	private ModWorld() {
	}

	public static void init() {
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			if (key.equals(BuiltInLootTables.NETHER_BRIDGE)) {
				table.withPool(LootPool.lootPool()
						.add(item(ModBoss.WARDEN_SUMMONER, 1, FORTRESS_SUMMONER_WEIGHT))
						.add(item(ModItems.CORE_SHARD, 2, FORTRESS_SHARD_WEIGHT))
						.add(EmptyLootItem.emptyItem().setWeight(FORTRESS_EMPTY_WEIGHT)));
			} else if (key.equals(BuiltInLootTables.BASTION_OTHER) || key.equals(BuiltInLootTables.BASTION_BRIDGE)) {
				table.withPool(LootPool.lootPool()
						.add(item(ModItems.CORE_SHARD, 2, 3))
						.add(item(ModItems.STEEL_INGOT, 3, 2))
						.add(EmptyLootItem.emptyItem().setWeight(5)));
			}
		});
	}

	private static UniformContainerBase.Builder<?> item(ItemLike item, int count, int weight) {
		var entry = LootItem.lootTableItem(item).setWeight(weight);
		if (count > 1) {
			entry.apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(count)));
		}
		return entry;
	}
}
