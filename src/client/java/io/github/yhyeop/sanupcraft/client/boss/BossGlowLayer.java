package io.github.yhyeop.sanupcraft.client.boss;

import java.util.function.ToDoubleFunction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * 보스의 빛나는 부분 (눈 · 화구 등). 바닐라 눈 레이어처럼 어둠 속에서도 밝게 그리고,
 * 밝기(0~1)는 상태에서 받아 투명도로 맥박치게 한다. 텍스처는 빛나는 칸만 칠하고 나머지는 투명.
 */
public class BossGlowLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	private final RenderType renderType;
	private final ToDoubleFunction<S> brightness;

	public BossGlowLayer(RenderLayerParent<S, M> renderer, Identifier texture, ToDoubleFunction<S> brightness) {
		super(renderer);
		this.renderType = RenderTypes.eyes(texture);
		this.brightness = brightness;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		float alpha = Mth.clamp((float) brightness.applyAsDouble(state), 0.0F, 1.0F);
		if (alpha <= 0.0F) {
			return;
		}
		collector.order(1).submitModel(getParentModel(), state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY,
				ARGB.white(alpha), null, state.outlineColor);
	}
}
