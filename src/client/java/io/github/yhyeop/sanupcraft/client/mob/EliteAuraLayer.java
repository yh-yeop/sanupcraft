package io.github.yhyeop.sanupcraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import io.github.yhyeop.sanupcraft.client.boss.BossModelLayers;

/**
 * 정예 테두리: 같은 모양을 조금 부풀린 모델에 무늬가 흘러간다 (충전된 크리퍼와 같은 렌더 타입, 더하기 섞기라 색이 곧 밝기).
 * 색은 능력 색 (가속 하늘 · 반사 금 · 재생 분홍). 보통 몹에는 그리지 않는다.
 */
public class EliteAuraLayer extends RenderLayer<MobRenderState, EntityModel<MobRenderState>> {
	private static final Identifier TEXTURE = BossModelLayers.texture("elite_aura");
	private final EntityModel<MobRenderState> model;

	public EliteAuraLayer(RenderLayerParent<MobRenderState, EntityModel<MobRenderState>> renderer, EntityModel<MobRenderState> model) {
		super(renderer);
		this.model = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, MobRenderState state, float yRot, float xRot) {
		if (!state.elite || state.isInvisible) {
			return;
		}
		float t = state.ageInTicks;
		float shimmer = 0.32F + 0.12F * Mth.sin(t * 0.25F);
		if (state.deathTime > 0) {
			shimmer *= Math.max(0.0F, 1.0F - state.deathTime / 20.0F);
		}
		int rgb = state.eliteColor;
		int color = ARGB.color(255, (int) (ARGB.red(rgb) * shimmer), (int) (ARGB.green(rgb) * shimmer), (int) (ARGB.blue(rgb) * shimmer));
		collector.order(2).submitModel(model, state, poseStack,
				RenderTypes.energySwirl(TEXTURE, t * 0.01F % 1.0F, t * 0.008F % 1.0F),
				lightCoords, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
	}
}
