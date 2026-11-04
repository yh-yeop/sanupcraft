package io.github.yhyeop.sanupcraft.mining;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 공장이 자원을 직접 캐 온다: 증기 굴착기 (2단계, 강화 기계 틀). */
public final class ModMining {
	public static final QuarryBlock STEAM_QUARRY = ModRegistry.block("steam_quarry", QuarryBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(5.0F, 6.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	/** 내열 드릴 (네더 단계): 굴착기에 끼우면 다이아 곡괭이 등급으로 캔다 (고대 잔해 · 흑요석). */
	public static final Item HEATPROOF_DRILL = ModRegistry.item("heatproof_drill",
			new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON));

	public static final BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("steam_quarry"), new BlockEntityType<>(QuarryBlockEntity::new, Set.of(STEAM_QUARRY)));

	/** 굴착기가 (쓰레기 버림이 켜져 있으면) 버리는 흔한 돌 · 흙. 데이터팩으로 바꿀 수 있다. */
	public static final TagKey<Item> QUARRY_JUNK = TagKey.create(Registries.ITEM, Sanupcraft.id("quarry_junk"));

	private ModMining() {
	}

	public static void init() {
	}
}
