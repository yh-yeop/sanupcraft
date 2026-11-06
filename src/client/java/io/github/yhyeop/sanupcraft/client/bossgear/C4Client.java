package io.github.yhyeop.sanupcraft.client.bossgear;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import io.github.yhyeop.sanupcraft.boss.gear.ModBossGear;
import io.github.yhyeop.sanupcraft.boss.phase.ModBossPhase;

/** 확장 3 C4 클라이언트: 위험 구역은 입자만 (렌더러 없음), 공허 탄은 아이템 그림. 금 간 갑옷 레이어는 세 보스 렌더러가 단다. */
public final class C4Client {
	private C4Client() {
	}

	public static void init() {
		EntityRendererRegistry.register(ModBossPhase.BOSS_HAZARD, NoopRenderer::new);
		EntityRendererRegistry.register(ModBossGear.VOID_BOLT_ENTITY, ThrownItemRenderer::new);
	}
}
