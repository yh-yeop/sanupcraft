package io.github.yhyeop.sanupcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;

import io.github.yhyeop.sanupcraft.boss.RustyGuardian;

/**
 * 녹슨 수호자: 철 골렘 모델을 1.3배로 키우고 녹슨 적갈색으로 물들인다.
 * 텍스처는 게임에 들어 있는 철 골렘 것을 쓴다 (레포에 복사하지 않음). 전용 도트는 나중에.
 */
public class RustyGuardianRenderer extends MobRenderer<RustyGuardian, IronGolemRenderState, IronGolemModel> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");
	private static final int RUST_TINT = 0xFFC8743E;
	private static final float SCALE = 1.3F;

	public RustyGuardianRenderer(EntityRendererProvider.Context context) {
		super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F * SCALE);
		addLayer(new IronGolemCrackinessLayer(this));
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
	public void extractRenderState(RustyGuardian entity, IronGolemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.attackTicksRemaining = entity.getAttackAnimationTick() > 0 ? entity.getAttackAnimationTick() - partialTicks : 0.0F;
		state.offerFlowerTick = 0;
		state.flowerBlock.clear();
		state.crackiness = Crackiness.GOLEM.byFraction(entity.getHealth() / entity.getMaxHealth());
	}

	@Override
	protected int getModelTint(IronGolemRenderState state) {
		return RUST_TINT;
	}

	@Override
	protected void scale(IronGolemRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
