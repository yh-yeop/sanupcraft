package io.github.yhyeop.sanupcraft.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * 보스 2페이즈 (확장 3 C4 · R7): 금 간 갑옷. 쇠골렘 금 레이어처럼 같은 모델에 금 텍스처({@code <보스>_cracks})를 덧칠하고,
 * 금 속은 빛 텍스처({@code <보스>_cracks_glow}, 보스 빛 색)로 어둠 속에서도 맥박친다. 2페이즈가 아니면 아무것도 그리지 않는다.
 * 텍스처는 보스 텍스처와 같은 크기 · 같은 UV (tools/make_textures.py {@code main_c4}가 몸통 칸 위에만 금을 긋는다).
 */
public class BossCrackLayer<S extends BossRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	private final Identifier cracks;
	private final RenderType glow;

	public BossCrackLayer(RenderLayerParent<S, M> renderer, String bossName) {
		super(renderer);
		this.cracks = BossModelLayers.texture(bossName + "_cracks");
		this.glow = RenderTypes.eyes(BossModelLayers.texture(bossName + "_cracks_glow"));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
		if (!state.phaseTwo || state.isInvisible) {
			return;
		}
		renderColoredCutoutModel(getParentModel(), cracks, poseStack, collector, lightCoords, state, -1, 1);
		float pulse = state.deathTime > 0 ? 1.0F - state.deathTime / 20.0F : 0.75F + 0.25F * Mth.sin(state.ageInTicks * 0.3F);
		float alpha = Mth.clamp(pulse, 0.0F, 1.0F);
		if (alpha > 0.0F) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, glow, lightCoords, OverlayTexture.NO_OVERLAY,
					ARGB.white(alpha), null, state.outlineColor);
		}
	}
}
