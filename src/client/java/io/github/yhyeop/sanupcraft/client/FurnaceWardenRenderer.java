package io.github.yhyeop.sanupcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;
import io.github.yhyeop.sanupcraft.client.boss.BossGlowLayer;
import io.github.yhyeop.sanupcraft.client.boss.BossModelLayers;
import io.github.yhyeop.sanupcraft.client.boss.BossRenderState;
import io.github.yhyeop.sanupcraft.client.boss.FurnaceWardenModel;
import io.github.yhyeop.sanupcraft.client.boss.FurnaceWardenRenderState;

/**
 * 용광로 파수꾼: 전용 모델(client/boss/FurnaceWardenModel)을 1.2배로 키운다.
 * 화구 창살 · 눈 틈 · 굴뚝 속 불씨는 빛나는 레이어로 일렁이고, 증기 분출 예고 중에는 활활 타오른다.
 */
public class FurnaceWardenRenderer extends MobRenderer<FurnaceWarden, FurnaceWardenRenderState, FurnaceWardenModel> {
	private static final Identifier TEXTURE = BossModelLayers.texture("furnace_warden");
	private static final Identifier GLOW = BossModelLayers.texture("furnace_warden_glow");
	private static final float SCALE = 1.2F;
	/** FurnaceWarden.BLAST_WINDUP 과 같은 값 (예고 동작 길이). */
	private static final float ROAR_TICKS = 20.0F;

	public FurnaceWardenRenderer(EntityRendererProvider.Context context) {
		super(context, new FurnaceWardenModel(context.bakeLayer(BossModelLayers.FURNACE_WARDEN)), 1.1F * SCALE);
		addLayer(new BossGlowLayer<>(this, GLOW, FurnaceWardenRenderer::glow));
	}

	private static double glow(FurnaceWardenRenderState state) {
		if (state.roarAnimation > 0.0F) {
			return 0.85 + 0.15 * Mth.sin(state.ageInTicks * 1.3F);
		}
		float age = state.ageInTicks;
		double flicker = 0.08 * Mth.sin(age * 0.15F) + 0.05 * Mth.sin(age * 0.53F + 1.0F);
		return (state.enraged ? 0.85 : 0.72) + flicker * (state.enraged ? 1.8 : 1.0);
	}

	@Override
	public Identifier getTextureLocation(FurnaceWardenRenderState state) {
		return TEXTURE;
	}

	@Override
	public FurnaceWardenRenderState createRenderState() {
		return new FurnaceWardenRenderState();
	}

	@Override
	public void extractRenderState(FurnaceWarden entity, FurnaceWardenRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		BossRenderState.extractCommon(entity, state, partialTicks);
		state.roarAnimation = entity.getRoarTick() > 0 ? (ROAR_TICKS - entity.getRoarTick() + partialTicks) / ROAR_TICKS : 0.0F;
	}

	@Override
	protected void scale(FurnaceWardenRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
