package io.github.yhyeop.sanupcraft.production.grinder;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 생산 2 (M9 P2): 몹 분쇄기 (증기로 위 칸 몹을 베고 경험치를 모은다)와 경험치 탱크. */
public final class ModGrinder {
	public static final MobGrinderBlock MOB_GRINDER = ModRegistry.block("mob_grinder", MobGrinderBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final ExperienceTankBlock EXPERIENCE_TANK = ModRegistry.block("experience_tank", ExperienceTankBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.EMERALD)
			.strength(2.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL), ExperienceTankItem::new);

	public static final BlockEntityType<MobGrinderBlockEntity> MOB_GRINDER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("mob_grinder"), new BlockEntityType<>(MobGrinderBlockEntity::new, Set.of(MOB_GRINDER)));

	public static final BlockEntityType<ExperienceTankBlockEntity> EXPERIENCE_TANK_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("experience_tank"), new BlockEntityType<>(ExperienceTankBlockEntity::new, Set.of(EXPERIENCE_TANK)));

	private ModGrinder() {
	}

	public static void init() {
	}
}
