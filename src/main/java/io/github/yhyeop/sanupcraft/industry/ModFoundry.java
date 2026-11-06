package io.github.yhyeop.sanupcraft.industry;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 산업 규모 묶음 B (M8): 광석 3 · 4배 처리 재료(정제 가루 · 희귀 가루)와 용광로 탑 (내열 벽돌 23칸 + 제어기).
 * 세척기 · 고압 정련기는 기계 종류라서 {@code factory/ModMachines}에 있다.
 */
public final class ModFoundry {
	public static final Item REFINED_IRON_DUST = ModRegistry.item("refined_iron_dust");
	public static final Item REFINED_COPPER_DUST = ModRegistry.item("refined_copper_dust");
	public static final Item REFINED_GOLD_DUST = ModRegistry.item("refined_gold_dust");
	/** 세척 · 정련 부산물. 정밀 조립기에서 정밀 부품과 함께 두 배 업그레이드가 된다. */
	public static final Item RARE_DUST = ModRegistry.item("rare_dust", new Item.Properties().rarity(Rarity.UNCOMMON));

	public static final HeatproofBricksBlock HEATPROOF_BRICKS = ModRegistry.block("heatproof_bricks", HeatproofBricksBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.NETHER)
			.strength(4.0F, 12.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.NETHER_BRICKS));

	public static final FurnaceTowerControllerBlock FURNACE_TOWER_CONTROLLER = ModRegistry.block("furnace_tower_controller", FurnaceTowerControllerBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.NETHER)
					.strength(4.0F, 12.0F)
					.requiresCorrectToolForDrops()
					.sound(SoundType.NETHER_BRICKS)
					.lightLevel(state -> state.getValue(FurnaceTowerControllerBlock.LIT) ? 13 : 0));

	public static final BlockEntityType<HeatproofBricksBlockEntity> HEATPROOF_BRICKS_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("heatproof_bricks"),
			new BlockEntityType<>(HeatproofBricksBlockEntity::new, Set.of(HEATPROOF_BRICKS)));

	public static final BlockEntityType<FurnaceTowerControllerBlockEntity> FURNACE_TOWER_CONTROLLER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("furnace_tower_controller"),
			new BlockEntityType<>(FurnaceTowerControllerBlockEntity::new, Set.of(FURNACE_TOWER_CONTROLLER)));

	private ModFoundry() {
	}

	public static void init() {
	}
}
