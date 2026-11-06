package io.github.yhyeop.sanupcraft.client.end;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;

import io.github.yhyeop.sanupcraft.boss.VoidColossus;

/**
 * 공허 거상 (M6, 임시): 철 골렘 모델을 1.6배로 키우고 짙은 보라로 물들인다. 텍스처는 게임의 철 골렘 것.
 * 전용 모델은 M11에서 바꾼다.
 */
public class VoidColossusRenderer extends MobRenderer<VoidColossus, IronGolemRenderState, IronGolemModel> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");
	private static final int VOID_TINT = 0xFF6A3FA0;
	private static final float SCALE = 1.6F;

	public VoidColossusRenderer(EntityRendererProvider.Context context) {
		super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F * SCALE);
	}

	@Override
	public Identifier getTextureLocation(IronGolemRenderState state) {
		return TEXTURE;
	}

	@Override
	public IronGolemRenderState createRenderState() {
		return new IronGolemRenderState();
	}

	@Override
	public void extractRenderState(VoidColossus entity, IronGolemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.attackTicksRemaining = entity.getAttackTick() > 0 ? entity.getAttackTick() - partialTicks : 0.0F;
		state.offerFlowerTick = 0;
		state.flowerBlock.clear();
		state.crackiness = Crackiness.GOLEM.byFraction(entity.getHealth() / entity.getMaxHealth());
	}

	@Override
	protected int getModelTint(IronGolemRenderState state) {
		return VOID_TINT;
	}

	@Override
	protected void scale(IronGolemRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
