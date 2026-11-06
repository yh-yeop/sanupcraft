package io.github.yhyeop.sanupcraft.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * 공허 거상 보호막: 공허 수정이 살아 있는 동안 몸을 조금 부풀린 모양에 보라 무늬가 흘러간다 (충전된 크리퍼 · 위더 갑옷과 같은 렌더 타입).
 * 세기는 상태의 {@code shieldAnimation}(수정이 생기면 천천히 켜지고 다 부서지면 꺼짐)을 따라 밝아진다 (더하기 섞기라 색이 곧 밝기).
 */
public class VoidShieldLayer extends RenderLayer<VoidColossusRenderState, VoidColossusModel> {
	private static final Identifier TEXTURE = BossModelLayers.texture("void_colossus_shield");
	private final VoidColossusModel model;

	public VoidShieldLayer(RenderLayerParent<VoidColossusRenderState, VoidColossusModel> renderer, EntityModelSet models) {
		super(renderer);
		this.model = new VoidColossusModel(models.bakeLayer(BossModelLayers.VOID_COLOSSUS_SHIELD));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, VoidColossusRenderState state, float yRot, float xRot) {
		float strength = Mth.clamp(state.shieldAnimation, 0.0F, 1.0F);
		if (strength <= 0.0F || state.isInvisible) {
			return;
		}
		float t = state.ageInTicks;
		float shimmer = strength * (0.75F + 0.25F * Mth.sin(t * 0.35F));
		int color = ARGB.color(255, (int) (170 * shimmer), (int) (70 * shimmer), (int) (230 * shimmer));
		collector.order(2).submitModel(model, state, poseStack,
				RenderTypes.energySwirl(TEXTURE, Mth.cos(t * 0.02F) * 2.0F % 1.0F, t * 0.012F % 1.0F),
				lightCoords, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
	}
}
