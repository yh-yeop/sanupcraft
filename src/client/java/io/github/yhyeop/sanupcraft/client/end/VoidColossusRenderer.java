package io.github.yhyeop.sanupcraft.client.end;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import io.github.yhyeop.sanupcraft.boss.VoidColossus;
import io.github.yhyeop.sanupcraft.client.boss.BossGlowLayer;
import io.github.yhyeop.sanupcraft.client.boss.BossModelLayers;
import io.github.yhyeop.sanupcraft.client.boss.BossRenderState;
import io.github.yhyeop.sanupcraft.client.boss.VoidColossusModel;
import io.github.yhyeop.sanupcraft.client.boss.VoidColossusRenderState;
import io.github.yhyeop.sanupcraft.client.boss.VoidShieldLayer;

/**
 * 공허 거상: 전용 모델(client/boss/VoidColossusModel)을 1.35배로 키운다 (키 약 4.3칸).
 * 가슴의 핵 · 눈 틈 · 팔의 금 · 바위판 틈은 빛나는 레이어(보라 · 자홍)로, 공허 수정이 지키는 동안은 보호막 레이어가 겹친다.
 */
public class VoidColossusRenderer extends MobRenderer<VoidColossus, VoidColossusRenderState, VoidColossusModel> {
	private static final Identifier TEXTURE = BossModelLayers.texture("void_colossus");
	private static final Identifier GLOW = BossModelLayers.texture("void_colossus_glow");
	private static final float SCALE = 1.35F;

	public VoidColossusRenderer(EntityRendererProvider.Context context) {
		super(context, new VoidColossusModel(context.bakeLayer(BossModelLayers.VOID_COLOSSUS)), 1.0F * SCALE);
		addLayer(new BossGlowLayer<>(this, GLOW, VoidColossusRenderer::glow));
		addLayer(new VoidShieldLayer(this, context.getModelSet()));
	}

	/** 천천히 맥박친다. 끌어당길 때는 활활, 보호막 중에는 빠르게 일렁이고, 화나면 맥박이 빨라진다. */
	private static double glow(VoidColossusRenderState state) {
		float age = state.ageInTicks;
		double base = 0.72 + 0.18 * Mth.sin(age * (state.enraged ? 0.35F : 0.12F));
		double pull = state.pullAnimation * (0.95 + 0.05 * Mth.sin(age * 1.1F));
		double shimmer = state.shieldAnimation * 0.12 * Mth.sin(age * 0.9F);
		return Math.max(base + shimmer, pull);
	}

	@Override
	public Identifier getTextureLocation(VoidColossusRenderState state) {
		return TEXTURE;
	}

	@Override
	public VoidColossusRenderState createRenderState() {
		return new VoidColossusRenderState();
	}

	@Override
	public void extractRenderState(VoidColossus entity, VoidColossusRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		BossRenderState.extractCommon(entity, state, partialTicks);
		state.pullAnimation = entity.pullAnimation(partialTicks);
		state.pulling = (entity.flags() & VoidColossus.FLAG_PULLING) != 0;
		state.shieldAnimation = entity.shieldAnimation(partialTicks);
	}

	@Override
	protected void scale(VoidColossusRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
