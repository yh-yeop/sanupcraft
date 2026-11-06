package io.github.yhyeop.sanupcraft.boss;

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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

public final class ModBoss {
	private static final ResourceKey<EntityType<?>> RUSTY_GUARDIAN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("rusty_guardian"));
	private static final ResourceKey<EntityType<?>> FURNACE_WARDEN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("furnace_warden"));

	/** 철 골렘보다 한 뼘 크다 (렌더러에서 1.3배). */
	public static final EntityType<RustyGuardian> RUSTY_GUARDIAN = Registry.register(BuiltInRegistries.ENTITY_TYPE, RUSTY_GUARDIAN_KEY,
			EntityType.Builder.of(RustyGuardian::new, MobCategory.MONSTER)
					.sized(1.8F, 3.5F)
					.fireImmune()
					.clientTrackingRange(10)
					.build(RUSTY_GUARDIAN_KEY));

	/** 파괴수보다 조금 크다 (렌더러에서 1.2배). */
	public static final EntityType<FurnaceWarden> FURNACE_WARDEN = Registry.register(BuiltInRegistries.ENTITY_TYPE, FURNACE_WARDEN_KEY,
			EntityType.Builder.of(FurnaceWarden::new, MobCategory.MONSTER)
					.sized(2.3F, 2.6F)
					.fireImmune()
					.clientTrackingRange(10)
					.build(FURNACE_WARDEN_KEY));

	public static final GuardianAltarBlock GUARDIAN_ALTAR = ModRegistry.block("guardian_altar", GuardianAltarBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_ORANGE)
			.strength(5.0F, 1200.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	/** 제단에 쓰면 녹슨 수호자를 부른다. 장비 부품으로 만든다. */
	public static final Item GUARDIAN_SUMMONER = ModRegistry.item("guardian_summoner", BossSummonerItem::new, new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
	/** 제단에 쓰면 용광로 파수꾼을 부른다. 핵 조각으로 만든다. */
	public static final Item WARDEN_SUMMONER = ModRegistry.item("warden_summoner", BossSummonerItem::new, new Item.Properties().rarity(Rarity.RARE).stacksTo(16).fireResistant());

	private ModBoss() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(RUSTY_GUARDIAN, RustyGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(FURNACE_WARDEN, FurnaceWarden.createAttributes());
	}
}
