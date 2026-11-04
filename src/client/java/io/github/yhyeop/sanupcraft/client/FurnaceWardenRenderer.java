package io.github.yhyeop.sanupcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.ravager.RavagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.resources.Identifier;

import io.github.yhyeop.sanupcraft.boss.FurnaceWarden;

/**
 * 용광로 파수꾼: 파괴수 모델을 1.2배로 키우고 달군 쇠 색으로 물들인다. 증기 분출 전에는 포효 동작.
 * 텍스처는 게임에 들어 있는 파괴수 것을 쓴다 (레포에 복사하지 않음). 전용 도트는 나중에.
 */
public class FurnaceWardenRenderer extends MobRenderer<FurnaceWarden, RavagerRenderState, RavagerModel> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/illager/ravager.png");
	private static final int HEAT_TINT = 0xFFFF5A28;
	private static final float SCALE = 1.2F;

	public FurnaceWardenRenderer(EntityRendererProvider.Context context) {
		super(context, new RavagerModel(context.bakeLayer(ModelLayers.RAVAGER)), 1.1F * SCALE);
	}

	@Override
	public Identifier getTextureLocation(RavagerRenderState state) {
		return TEXTURE;
	}

	@Override
	public RavagerRenderState createRenderState() {
		return new RavagerRenderState();
	}

	@Override
	public void extractRenderState(FurnaceWarden entity, RavagerRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.stunnedTicksRemaining = 0.0F;
		state.attackTicksRemaining = entity.getAttackTick() > 0 ? entity.getAttackTick() - partialTicks : 0.0F;
		state.roarAnimation = entity.getRoarTick() > 0 ? (20 - entity.getRoarTick() + partialTicks) / 20.0F : 0.0F;
	}

	@Override
	protected int getModelTint(RavagerRenderState state) {
		return HEAT_TINT;
	}

	@Override
	protected void scale(RavagerRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
