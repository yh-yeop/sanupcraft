package io.github.yhyeop.sanupcraft.client.end;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.boss.VoidCrystal;

/**
 * 공허 수정 (M6, 임시): 바닐라 엔드 수정 모델을 절반 크기로, 바닥 받침 없이 그리고 거상까지 같은 빛줄기를 잇는다.
 * (바닐라 엔드 수정 렌더러는 엔드 수정 엔티티 전용이라 같은 그리기를 옮겨 왔다.)
 */
public class VoidCrystalRenderer extends EntityRenderer<VoidCrystal, EndCrystalRenderState> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
	private final EndCrystalModel model;

	public VoidCrystalRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.3F;
		this.model = new EndCrystalModel(context.bakeLayer(ModelLayers.END_CRYSTAL));
	}

	@Override
	public void submit(EndCrystalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.scale(1.0F, 1.0F, 1.0F);
		poseStack.translate(0.0F, -0.5F, 0.0F);
		collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		Vec3 beamOffset = state.beamOffset;
		if (beamOffset != null) {
			float crystalY = EndCrystalRenderer.getY(state.ageInTicks) * 0.5F;
			poseStack.pushPose();
			poseStack.translate(beamOffset);
			EnderDragonRenderer.submitCrystalBeams((float) -beamOffset.x, (float) -beamOffset.y + crystalY, (float) -beamOffset.z,
					state.ageInTicks, poseStack, collector, state.lightCoords);
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public EndCrystalRenderState createRenderState() {
		return new EndCrystalRenderState();
	}

	@Override
	public void extractRenderState(VoidCrystal entity, EndCrystalRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.ageInTicks = entity.time + partialTicks;
		state.showsBottom = false;
		BlockPos target = entity.getBeamTarget();
		state.beamOffset = target == null ? null : Vec3.atCenterOf(target).subtract(entity.getPosition(partialTicks));
	}

	@Override
	public boolean shouldRender(VoidCrystal entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
		return super.shouldRender(entity, culler, camX, camY, camZ, partialTicks) || entity.getBeamTarget() != null;
	}
}
