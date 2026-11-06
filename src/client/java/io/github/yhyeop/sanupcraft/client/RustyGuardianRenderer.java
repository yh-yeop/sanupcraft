package io.github.yhyeop.sanupcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import io.github.yhyeop.sanupcraft.boss.RustyGuardian;
import io.github.yhyeop.sanupcraft.client.boss.BossCrackLayer;
import io.github.yhyeop.sanupcraft.client.boss.BossGlowLayer;
import io.github.yhyeop.sanupcraft.client.boss.BossModelLayers;
import io.github.yhyeop.sanupcraft.client.boss.BossRenderState;
import io.github.yhyeop.sanupcraft.client.boss.RustyGuardianModel;
import io.github.yhyeop.sanupcraft.client.boss.RustyGuardianRenderState;

/**
 * 녹슨 수호자: 전용 모델(client/boss/RustyGuardianModel)을 1.25배로 키운다 (키 3.5칸 = 엔티티 높이).
 * 외눈 틈과 가슴의 핵은 따로 빛나는 레이어로 그린다.
 */
public class RustyGuardianRenderer extends MobRenderer<RustyGuardian, RustyGuardianRenderState, RustyGuardianModel> {
	private static final Identifier TEXTURE = BossModelLayers.texture("rusty_guardian");
	private static final Identifier GLOW = BossModelLayers.texture("rusty_guardian_glow");
	private static final float SCALE = 1.25F;

	public RustyGuardianRenderer(EntityRendererProvider.Context context) {
		super(context, new RustyGuardianModel(context.bakeLayer(BossModelLayers.RUSTY_GUARDIAN)), 0.9F * SCALE);
		addLayer(new BossGlowLayer<>(this, GLOW, RustyGuardianRenderer::glow));
		addLayer(new BossCrackLayer<>(this, "rusty_guardian")); // 확장 3 C4 · R7 2페이즈 금 간 갑옷
	}

	/** 천천히 숨 쉬듯 밝아졌다 어두워지고, 화나면 빠르게 깜박인다. */
	private static double glow(RustyGuardianRenderState state) {
		float speed = state.enraged ? 0.45F : 0.1F;
		return 0.8 + 0.2 * Mth.sin(state.ageInTicks * speed);
	}

	@Override
	public Identifier getTextureLocation(RustyGuardianRenderState state) {
		return TEXTURE;
	}

	@Override
	public RustyGuardianRenderState createRenderState() {
		return new RustyGuardianRenderState();
	}

	@Override
	public void extractRenderState(RustyGuardian entity, RustyGuardianRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		BossRenderState.extractCommon(entity, state, partialTicks);
	}

	@Override
	protected void scale(RustyGuardianRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
