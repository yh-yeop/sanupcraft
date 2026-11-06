package io.github.yhyeop.sanupcraft.client.boss;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 녹슨 수호자: 리벳 박은 넓은 몸통, 외눈 틈 하나 있는 작은 머리, 팔꿈치에 피스톤이 달린 큰 팔, 짧은 다리.
 * 텍스처 128x64 (tools/make_textures.py 의 main_m11a 가 같은 자리에 칠한다 — texOffs를 바꾸면 거기도 같이).
 * 렌더러가 1.25배로 키워서 키 3.5칸.
 */
public class RustyGuardianModel extends EntityModel<RustyGuardianRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart rightForearm;
	private final ModelPart leftArm;
	private final ModelPart leftForearm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public RustyGuardianModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		body = root.getChild("body");
		rightArm = root.getChild("right_arm");
		rightForearm = rightArm.getChild("right_forearm");
		leftArm = root.getChild("left_arm");
		leftForearm = leftArm.getChild("left_forearm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(64, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
				PartPose.offset(0.0F, -13.0F, -1.0F));
		// 허리를 축으로: 몸통 · 가슴판 · 배기통 둘 · 골반
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-10.0F, -18.0F, -6.0F, 20, 18, 12)
				.texOffs(0, 30).addBox(-7.0F, -15.0F, -7.0F, 14, 8, 1)
				.texOffs(110, 44).addBox(-8.0F, -22.0F, 3.0F, 3, 7, 3)
				.texOffs(110, 44).mirror().addBox(5.0F, -22.0F, 3.0F, 3, 7, 3).mirror(false)
				.texOffs(30, 34).addBox(-6.0F, 0.0F, -4.0F, 12, 4, 8),
				PartPose.offset(0.0F, 4.0F, 0.0F));
		arm(root, "right", -1.0F, false);
		arm(root, "left", 1.0F, true);
		root.addOrReplaceChild("right_leg", leg(false), PartPose.offset(-5.0F, 8.0F, 0.0F));
		root.addOrReplaceChild("left_leg", leg(true), PartPose.offset(5.0F, 8.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** 어깨 갑옷 · 위팔 · 피스톤 통, 그 아래 팔꿈치 축에 아래팔 · 주먹 · 피스톤 막대. side = -1 오른쪽, +1 왼쪽. */
	private static void arm(PartDefinition root, String name, float side, boolean mirror) {
		PartDefinition arm = root.addOrReplaceChild(name + "_arm", CubeListBuilder.create().mirror(mirror)
				.texOffs(96, 0).addBox(side < 0 ? -5.0F : -2.0F, -4.0F, -4.5F, 7, 7, 9)
				.texOffs(96, 30).addBox(side < 0 ? -4.0F : -1.0F, 3.0F, -3.0F, 5, 8, 6)
				.texOffs(96, 44).addBox(side < 0 ? -3.0F : 0.0F, 4.0F, 3.0F, 3, 6, 2),
				PartPose.offset(side * 12.0F, -11.0F, 0.0F));
		arm.addOrReplaceChild(name + "_forearm", CubeListBuilder.create().mirror(mirror)
				.texOffs(64, 16).addBox(-3.5F, 0.0F, -3.5F, 7, 10, 7)
				.texOffs(96, 16).addBox(-4.0F, 9.0F, -4.0F, 8, 5, 8)
				.texOffs(106, 44).addBox(-0.5F, -5.0F, 3.5F, 1, 6, 1),
				PartPose.offset(side * 1.5F, 11.0F, 0.0F));
	}

	private static CubeListBuilder leg(boolean mirror) {
		return CubeListBuilder.create().mirror(mirror)
				.texOffs(70, 34).addBox(-3.0F, 0.0F, -3.0F, 6, 10, 6)
				.texOffs(0, 46).addBox(-4.0F, 10.0F, -5.0F, 8, 6, 9);
	}

	@Override
	public void setupAnim(RustyGuardianRenderState state) {
		super.setupAnim(state);
		float speed = state.walkAnimationSpeed;
		float swing = Mth.triangleWave(state.walkAnimationPos, 13.0F);
		float age = state.ageInTicks;

		head.yRot = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// 걸음: 다리는 크게, 팔은 반대로, 몸은 살짝 앞으로 숙이고 좌우로 흔들린다
		rightLeg.xRot = -1.1F * swing * speed;
		leftLeg.xRot = 1.1F * swing * speed;
		body.xRot = 0.08F * speed;
		body.zRot = 0.05F * swing * speed;
		rightArm.xRot = 0.8F * swing * speed;
		leftArm.xRot = -0.8F * swing * speed;
		rightForearm.xRot = -0.25F - 0.35F * Math.max(0.0F, -swing) * speed;
		leftForearm.xRot = -0.25F - 0.35F * Math.max(0.0F, swing) * speed;
		// 서 있을 때도 팔을 살짝 벌리고 숨 쉬듯 움직인다
		float breathe = Mth.sin(age * 0.08F) * 0.03F;
		rightArm.zRot = 0.1F + breathe;
		leftArm.zRot = -0.1F - breathe;

		float t = state.attackTicksRemaining;
		if (t > 0.0F) {
			// 두 팔을 머리 위로 들었다가 (앞 40%) 내리찍는다 (다음 30%)
			float p = 1.0F - t / 10.0F;
			float raise = p < 0.4F ? p / 0.4F : Math.max(0.0F, 1.0F - (p - 0.4F) / 0.3F);
			float arm = -0.3F - 2.4F * raise;
			rightArm.xRot = arm;
			leftArm.xRot = arm;
			rightForearm.xRot = -0.2F - 0.8F * raise;
			leftForearm.xRot = -0.2F - 0.8F * raise;
			body.xRot = p < 0.4F ? -0.12F * raise : 0.2F * (1.0F - raise);
		}
	}
}
