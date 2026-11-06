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
 * 녹슨 자동인형: 리벳 박은 네모난 몸통에 반쯤 파묻힌 머리 (금 간 외눈이 빛남), 안테나, 가슴 계기판,
 * 등에 작은 보일러 · 굴뚝, 어깨 갑옷 · 큰 주먹, 앞코가 나온 발. 키 2칸, 좀비보다 넓다.
 * 텍스처 64x64 (tools/make_textures.py 의 main_ex3_c2 가 같은 자리에 칠한다 — texOffs를 바꾸면 거기도 같이).
 * 노릴 때는 좀비처럼 두 팔을 앞으로 들고, 걸음은 뻣뻣하게 끊긴다.
 */
public class RustAutomatonModel extends EntityModel<MobRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public RustAutomatonModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		body = root.getChild("body");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer(CubeDeformation g) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, g)
				.texOffs(32, 0).addBox(-0.5F, -11.0F, -0.5F, 1, 3, 1, g),
				PartPose.offset(0.0F, 1.0F, -1.0F));
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-5.0F, 0.0F, -3.0F, 10, 12, 6, g)
				.texOffs(32, 16).addBox(-4.0F, 2.0F, -4.0F, 8, 5, 1, g)
				.texOffs(32, 22).addBox(-3.0F, 1.0F, 3.0F, 6, 7, 3, g)
				.texOffs(50, 16).addBox(0.5F, -3.0F, 4.0F, 2, 4, 2, g),
				PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(16, 34).addBox(-2.5F, -2.0F, -2.0F, 4, 12, 4, g)
				.texOffs(40, 0).addBox(-3.5F, -3.0F, -2.5F, 5, 4, 5, g)
				.texOffs(32, 42).addBox(-3.0F, 8.0F, -2.5F, 5, 4, 5, g),
				PartPose.offset(-7.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
				.texOffs(16, 34).addBox(-1.5F, -2.0F, -2.0F, 4, 12, 4, g)
				.texOffs(40, 0).addBox(-1.5F, -3.0F, -2.5F, 5, 4, 5, g)
				.texOffs(32, 42).addBox(-2.0F, 8.0F, -2.5F, 5, 4, 5, g),
				PartPose.offset(7.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", leg(g, false), PartPose.offset(-2.5F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", leg(g, true), PartPose.offset(2.5F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static CubeListBuilder leg(CubeDeformation g, boolean mirror) {
		return CubeListBuilder.create().mirror(mirror)
				.texOffs(0, 34).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, g)
				.texOffs(32, 34).addBox(-2.5F, 9.0F, -3.0F, 5, 3, 5, g);
	}

	@Override
	public void setupAnim(MobRenderState state) {
		super.setupAnim(state);
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		// 톱니처럼 끊기는 걸음: 사인파를 계단으로
		float raw = Mth.sin(state.walkAnimationPos * 0.55F);
		float swing = Math.round(raw * 4.0F) / 4.0F;
		float age = state.ageInTicks;

		head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// 가끔 고개가 덜컥 꺾인다
		head.zRot = Mth.sin(age * 0.05F) > 0.97F ? 0.25F : 0.0F;

		rightLeg.xRot = -0.9F * swing * speed;
		leftLeg.xRot = 0.9F * swing * speed;
		body.yRot = 0.06F * swing * speed;

		if (state.aggressive) {
			// 좀비처럼 두 팔을 앞으로
			float sway = Mth.sin(age * 0.12F) * 0.05F;
			rightArm.xRot = -Mth.HALF_PI + sway;
			leftArm.xRot = -Mth.HALF_PI - sway;
		} else {
			rightArm.xRot = 0.7F * swing * speed;
			leftArm.xRot = -0.7F * swing * speed;
		}
		float breathe = Mth.sin(age * 0.07F) * 0.03F;
		rightArm.zRot = 0.08F + breathe;
		leftArm.zRot = -0.08F - breathe;
		rightArm.yRot = 0.0F;
		leftArm.yRot = 0.0F;

		if (state.swing > 0.0F) {
			// 때리기: 두 주먹을 들었다가 내리찍는다
			float s = Mth.sin(state.swing * Mth.PI);
			rightArm.xRot -= 0.9F * s;
			leftArm.xRot -= 0.9F * s;
			body.xRot = 0.15F * s;
		} else {
			body.xRot = 0.0F;
		}
	}
}
