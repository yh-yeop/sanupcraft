package io.github.yhyeop.sanupcraft.client.mob;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 공허 그림자: 키 크고 마른 그림자. 뿔 둘 달린 머리에 보라 눈, 가는 몸통 (가슴에 보라 균열), 다리 대신 아래로 가늘어지며
 * 흩날리는 옷자락 (발이 땅에 안 닿음), 무릎까지 내려오는 긴 팔과 갈고리 손. 키 약 2.6칸.
 * 텍스처 64x32 (tools/make_textures.py 의 main_ex3_c2).
 */
public class VoidShadeModel extends EntityModel<MobRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart robe;
	private final ModelPart tatter;
	private final ModelPart leftStrip;
	private final ModelPart rightStrip;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightClaw;
	private final ModelPart leftClaw;

	public VoidShadeModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		body = root.getChild("body");
		robe = root.getChild("robe");
		tatter = robe.getChild("tatter");
		leftStrip = tatter.getChild("left_strip");
		rightStrip = tatter.getChild("right_strip");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
		rightClaw = rightArm.getChild("right_claw");
		leftClaw = leftArm.getChild("left_claw");
	}

	public static LayerDefinition createBodyLayer(CubeDeformation g) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0F, -7.0F, -3.0F, 6, 7, 6, g),
				PartPose.offset(0.0F, -10.0F, 0.0F));
		CubeListBuilder horn = CubeListBuilder.create().texOffs(40, 14).addBox(-0.5F, -3.0F, -0.5F, 1, 3, 1, g);
		head.addOrReplaceChild("right_horn", horn, PartPose.offsetAndRotation(-2.2F, -6.5F, 0.5F, -0.3F, 0.0F, -0.35F));
		head.addOrReplaceChild("left_horn", horn, PartPose.offsetAndRotation(2.2F, -6.5F, 0.5F, -0.3F, 0.0F, 0.35F));
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 13).addBox(-3.0F, 0.0F, -1.5F, 6, 12, 3, g),
				PartPose.offset(0.0F, -10.0F, 0.0F));
		PartDefinition robe = root.addOrReplaceChild("robe", CubeListBuilder.create()
				.texOffs(24, 0).addBox(-3.5F, 0.0F, -2.0F, 7, 10, 4, g),
				PartPose.offset(0.0F, 2.0F, 0.0F));
		PartDefinition tatter = robe.addOrReplaceChild("tatter", CubeListBuilder.create()
				.texOffs(24, 14).addBox(-2.5F, 0.0F, -1.5F, 5, 6, 3, g),
				PartPose.offset(0.0F, 10.0F, 0.0F));
		CubeListBuilder strip = CubeListBuilder.create().texOffs(44, 18).addBox(-0.5F, 0.0F, -0.5F, 1, 5, 1, g);
		tatter.addOrReplaceChild("right_strip", strip, PartPose.offset(-1.5F, 6.0F, 0.0F));
		tatter.addOrReplaceChild("left_strip", strip, PartPose.offset(1.5F, 6.0F, 0.5F));
		arm(root, "right", -4.0F, g, false);
		arm(root, "left", 4.0F, g, true);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void arm(PartDefinition root, String name, float x, CubeDeformation g, boolean mirror) {
		PartDefinition arm = root.addOrReplaceChild(name + "_arm", CubeListBuilder.create().mirror(mirror)
				.texOffs(48, 0).addBox(-1.0F, -1.0F, -1.0F, 2, 18, 2, g),
				PartPose.offset(x, -9.0F, 0.0F));
		arm.addOrReplaceChild(name + "_claw", CubeListBuilder.create().mirror(mirror)
				.texOffs(48, 20).addBox(-1.5F, 0.0F, -1.0F, 3, 4, 2, g),
				PartPose.offset(0.0F, 17.0F, 0.0F));
	}

	@Override
	public void setupAnim(MobRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		float walk = Mth.sin(state.walkAnimationPos * 0.6662F);
		// 떠 있는 듯 천천히 오르내린다
		float bob = Mth.sin(age * 0.07F) * 0.8F;
		head.y = -10.0F + bob;
		body.y = -10.0F + bob;
		robe.y = 2.0F + bob;
		rightArm.y = -9.0F + bob;
		leftArm.y = -9.0F + bob;

		head.yRot = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.zRot = 0.08F * Mth.sin(age * 0.045F);
		body.zRot = 0.04F * Mth.sin(age * 0.05F);

		// 옷자락은 걸음에 뒤로 날리고 끝으로 갈수록 늦게 흔들린다
		robe.xRot = 0.25F * speed + 0.05F * Mth.sin(age * 0.1F);
		tatter.xRot = 0.2F * speed + 0.12F * Mth.sin(age * 0.1F - 0.8F);
		rightStrip.xRot = 0.15F * Mth.sin(age * 0.13F - 1.4F) + 0.2F * speed;
		leftStrip.xRot = 0.15F * Mth.sin(age * 0.11F - 2.0F) + 0.2F * speed;
		rightStrip.zRot = 0.1F * Mth.sin(age * 0.09F);
		leftStrip.zRot = -0.1F * Mth.sin(age * 0.1F + 1.0F);

		// 팔: 늘어뜨리고 조금 벌림, 노릴 때는 앞으로 뻗어 갈고리를 편다
		float sway = 0.06F * Mth.sin(age * 0.06F);
		if (state.aggressive) {
			rightArm.xRot = -0.9F + sway;
			leftArm.xRot = -0.9F - sway;
		} else {
			rightArm.xRot = 0.35F * walk * speed + sway;
			leftArm.xRot = -0.35F * walk * speed - sway;
		}
		rightArm.zRot = 0.12F + sway;
		leftArm.zRot = -0.12F - sway;
		rightClaw.xRot = state.aggressive ? -0.5F : -0.15F;
		leftClaw.xRot = state.aggressive ? -0.5F : -0.15F;
		if (state.swing > 0.0F) {
			// 할퀴기: 오른팔을 높이 들었다가 비스듬히 내려긋는다
			float s = Mth.sin(state.swing * Mth.PI);
			rightArm.xRot = -2.2F * s + (1.0F - s) * rightArm.xRot;
			rightArm.yRot = 0.4F * s;
			leftArm.xRot -= 0.5F * s;
		} else {
			rightArm.yRot = 0.0F;
		}
	}
}
