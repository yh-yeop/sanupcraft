package io.github.yhyeop.sanupcraft.boss.phase;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 보스 2페이즈 (확장 3 C4) 등록: 위험 구역 엔티티. 새 패턴의 예고 소리는 {@link io.github.yhyeop.sanupcraft.sound.ModSounds}에
 * (소리 시험이 그 목록과 sounds.json을 맞춰 본다). 2페이즈에 들어가는 소리는 보스마다 있던 화남 소리 그대로.
 */
public final class ModBossPhase {
	private static final ResourceKey<EntityType<?>> BOSS_HAZARD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("boss_hazard"));

	public static final EntityType<BossHazard> BOSS_HAZARD = Registry.register(BuiltInRegistries.ENTITY_TYPE, BOSS_HAZARD_KEY,
			EntityType.Builder.<BossHazard>of(BossHazard::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.fireImmune()
					.clientTrackingRange(10)
					.updateInterval(Integer.MAX_VALUE)
					.build(BOSS_HAZARD_KEY));

	private ModBossPhase() {
	}

	/** 정적 필드를 불러 등록만 한다. */
	public static void init() {
	}
}
