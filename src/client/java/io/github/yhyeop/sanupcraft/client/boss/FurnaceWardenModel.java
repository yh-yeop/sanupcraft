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
 * 용광로 파수꾼: 가슴에 화구 창살이 달린 네모난 용광로 몸통, 굴뚝을 쌓은 머리, 무거운 팔, 짧고 굵은 다리.
 * 몸통이 머리 · 팔을 거느려서 통째로 들썩인다. 다리는 따로 땅을 구른다.
 * 텍스처 128x64 (tools/make_textures.py 의 main_m11a 가 같은 자리에 칠한다 — texOffs를 바꾸면 거기도 같이).
 * 렌더러가 1.2배로 키워서 키 약 2.7칸.
 */
public class FurnaceWardenModel extends EntityModel<FurnaceWardenRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public FurnaceWardenModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		rightArm = body.getChild("right_arm");
		leftArm = body.getChild("left_arm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// 몸통 · 위 덮개판 · 앞으로 한 칸 나온 화구 창살
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 18).addBox(-10.0F, -14.0F, -7.0F, 20, 14, 14)
				.texOffs(0, 0).addBox(-11.0F, -16.0F, -8.0F, 22, 2, 16)
				.texOffs(38, 46).addBox(-6.0F, -11.0F, -8.0F, 12, 7, 1),
				PartPose.offset(0.0F, 16.0F, 0.0F));
		// 머리: 눈 틈이 있는 아래 통 · 굴뚝 · 굴뚝 테
		body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(68, 18).addBox(-4.0F, -6.0F, -4.0F, 8, 6, 8)
				.texOffs(100, 18).addBox(-3.0F, -11.0F, -3.0F, 6, 5, 6)
				.texOffs(38, 54).addBox(-3.5F, -12.0F, -3.5F, 7, 1, 7),
				PartPose.offset(0.0F, -16.0F, 0.0F));
		body.addOrReplaceChild("right_arm", arm(-1.0F, false), PartPose.offset(-12.0F, -12.0F, 0.0F));
		body.addOrReplaceChild("left_arm", arm(1.0F, true), PartPose.offset(12.0F, -12.0F, 0.0F));
		root.addOrReplaceChild("right_leg", leg(false), PartPose.offset(-5.5F, 16.0F, 0.0F));
		root.addOrReplaceChild("left_leg", leg(true), PartPose.offset(5.5F, 16.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** 어깨 · 굵은 팔 · 주먹. side = -1 오른쪽, +1 왼쪽. */
	private static CubeListBuilder arm(float side, boolean mirror) {
		boolean right = side < 0;
		return CubeListBuilder.create().mirror(mirror)
				.texOffs(68, 32).addBox(right ? -5.0F : -1.0F, -2.0F, -4.0F, 6, 6, 8)
				.texOffs(76, 0).addBox(right ? -5.0F : -2.0F, 4.0F, -4.0F, 7, 10, 8)
				.texOffs(66, 46).addBox(right ? -5.5F : -2.5F, 13.0F, -4.5F, 8, 5, 9);
	}

	private static CubeListBuilder leg(boolean mirror) {
		return CubeListBuilder.create().mirror(mirror)
				.texOffs(100, 29).addBox(-3.5F, 0.0F, -3.5F, 7, 8, 7)
				.texOffs(0, 46).addBox(-4.5F, 5.0F, -5.5F, 9, 3, 10);
	}

	@Override
	public void setupAnim(FurnaceWardenRenderState state) {
		super.setupAnim(state);
		float speed = state.walkAnimationSpeed;
		float swing = Mth.triangleWave(state.walkAnimationPos, 16.0F);
		float age = state.ageInTicks;

		head.yRot = Mth.clamp(state.yRot, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;

		// 가만히 있어도 불 때는 쇳덩이처럼 들썩이고, 걸으면 쿵쿵 구르며 좌우로 기우뚱한다
		body.y += Mth.sin(age * 0.1F) * 0.5F - Math.abs(swing) * speed * 1.2F;
		body.zRot = 0.06F * swing * speed;
		rightLeg.xRot = -0.9F * swing * speed;
		leftLeg.xRot = 0.9F * swing * speed;
		rightArm.xRot = 0.6F * swing * speed;
		leftArm.xRot = -0.6F * swing * speed;
		float sway = Mth.sin(age * 0.1F) * 0.04F;
		rightArm.zRot = 0.08F + sway;
		leftArm.zRot = -0.08F - sway;

		float roar = state.roarAnimation;
		if (roar > 0.0F) {
			// 증기 분출 예고: 팔을 벌리고 몸을 젖히며 떤다
			float r = Mth.sin(Math.min(roar, 1.0F) * Mth.PI * 0.5F);
			rightArm.zRot += 0.9F * r;
			leftArm.zRot -= 0.9F * r;
			rightArm.xRot -= 0.5F * r;
			leftArm.xRot -= 0.5F * r;
			head.xRot -= 0.35F * r;
			body.xRot -= 0.12F * r;
			body.zRot += Mth.sin(age * 2.5F) * 0.03F * r;
		}

		float t = state.attackTicksRemaining;
		if (t > 0.0F) {
			// 두 팔을 치켜들었다가 (앞 40%) 앞으로 내리친다 (다음 30%)
			float p = 1.0F - t / 10.0F;
			float raise = p < 0.4F ? p / 0.4F : Math.max(0.0F, 1.0F - (p - 0.4F) / 0.3F);
			float arm = -0.5F - 2.0F * raise;
			rightArm.xRot = arm;
			leftArm.xRot = arm;
			body.xRot = p < 0.4F ? -0.1F * raise : 0.18F * (1.0F - raise);
		}
	}
}
