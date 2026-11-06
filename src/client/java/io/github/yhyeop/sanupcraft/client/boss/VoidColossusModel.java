package io.github.yhyeop.sanupcraft.client.boss;

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
 * 공허 거상: 흑요석 같은 거대한 돌기둥 몸. 넓은 가슴 한가운데 구멍 속에서 공허 핵이 돌고, 어깨 위에는 몸에 붙지 않은 바위판이
 * 떠다닌다. 팔은 길어서 손이 땅에 닿을 듯하고, 머리 위로 돌 조각 셋이 맴돈다. 등에는 수정 가시.
 * 동작: 가만히 있어도 둥실 흔들림, 끌어당기기 예고 = 두 팔을 앞으로 벌려 듦 → 끌어당기는 중 = 팔을 몸 쪽으로 끌어옴,
 * 수정이 지키는 동안 바위가 떨린다 (일렁이는 막은 렌더러의 보호막 레이어). 내려치기는 두 팔을 머리 위로.
 * 텍스처 128x128 (tools/make_textures.py 의 main_fin 이 같은 자리에 칠한다 — texOffs를 바꾸면 거기도 같이).
 * 렌더러가 1.35배로 키워서 키 약 4.3칸 (머리 위 조각까지).
 */
public class VoidColossusModel extends EntityModel<VoidColossusRenderState> {
	private static final int CROWN_SHARDS = 3;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart crown;
	private final ModelPart core;
	private final ModelPart rightArm;
	private final ModelPart rightForearm;
	private final ModelPart leftArm;
	private final ModelPart leftForearm;
	private final ModelPart rightPlate;
	private final ModelPart leftPlate;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public VoidColossusModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		crown = head.getChild("crown");
		core = body.getChild("core");
		rightArm = body.getChild("right_arm");
		rightForearm = rightArm.getChild("right_forearm");
		leftArm = body.getChild("left_arm");
		leftForearm = leftArm.getChild("left_forearm");
		rightPlate = body.getChild("right_plate");
		leftPlate = body.getChild("left_plate");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		return createBodyLayer(CubeDeformation.NONE);
	}

	/** {@code deformation}으로 부풀린 모양은 보호막 레이어가 쓴다. */
	public static LayerDefinition createBodyLayer(CubeDeformation deformation) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// 허리를 축으로: 넓은 가슴 · 좁은 배 · 골반
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-10.0F, -22.0F, -6.0F, 20, 10, 12, deformation)
				.texOffs(0, 22).addBox(-7.0F, -12.0F, -4.5F, 14, 8, 9, deformation)
				.texOffs(46, 22).addBox(-6.0F, -4.0F, -4.0F, 12, 4, 8, deformation),
				PartPose.offset(0.0F, 8.0F, 0.0F));
		// 머리: 얼굴 없는 돌덩이에 눈 틈 둘, 가슴에 조금 묻힘. 위로 맴도는 돌 조각
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(64, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 9, 8, deformation),
				PartPose.offset(0.0F, -22.0F, -1.0F));
		PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create(), PartPose.offset(0.0F, -11.0F, 0.0F));
		for (int i = 0; i < CROWN_SHARDS; i++) {
			float angle = Mth.TWO_PI * i / CROWN_SHARDS;
			crown.addOrReplaceChild("shard_" + i, CubeListBuilder.create()
					.texOffs(92, 60).addBox(-1.5F, -2.5F, -1.5F, 3, 5, 3, deformation),
					PartPose.offsetAndRotation(Mth.cos(angle) * 5.5F, (i % 2) * 1.5F, Mth.sin(angle) * 5.5F, 0.35F * Mth.sin(angle), -angle, 0.35F * Mth.cos(angle)));
		}
		// 가슴 구멍 속 공허 핵 (반쯤 튀어나와 돈다)
		body.addOrReplaceChild("core", CubeListBuilder.create()
				.texOffs(76, 60).addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4, deformation),
				PartPose.offset(0.0F, -17.0F, -6.0F));
		arm(body, "right", -1.0F, false, deformation);
		arm(body, "left", 1.0F, true, deformation);
		// 어깨 위에 떠 있는 바위판 (몸에 붙지 않음)
		plate(body, "right_plate", -1.0F, false, deformation);
		plate(body, "left_plate", 1.0F, true, deformation);
		// 등의 수정 가시
		spike(body, "spike_0", -4.0F, -18.0F, 5.0F, -0.6F, -0.25F, deformation);
		spike(body, "spike_1", 4.5F, -19.0F, 5.0F, -0.45F, 0.3F, deformation);
		spike(body, "spike_2", 0.5F, -14.0F, 5.0F, -0.85F, 0.05F, deformation);
		root.addOrReplaceChild("right_leg", leg(false, deformation), PartPose.offset(-5.0F, 8.0F, 0.0F));
		root.addOrReplaceChild("left_leg", leg(true, deformation), PartPose.offset(5.0F, 8.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	/** 위팔, 그 아래 팔꿈치 축에 긴 아래팔 · 큰 손. side = -1 오른쪽, +1 왼쪽. */
	private static void arm(PartDefinition body, String name, float side, boolean mirror, CubeDeformation deformation) {
		PartDefinition arm = body.addOrReplaceChild(name + "_arm", CubeListBuilder.create().mirror(mirror)
				.texOffs(96, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 14, 6, deformation),
				PartPose.offset(side * 12.5F, -19.0F, 0.0F));
		arm.addOrReplaceChild(name + "_forearm", CubeListBuilder.create().mirror(mirror)
				.texOffs(86, 20).addBox(-3.5F, 0.0F, -3.5F, 7, 14, 7, deformation)
				.texOffs(0, 39).addBox(-4.0F, 13.0F, -4.0F, 8, 5, 8, deformation),
				PartPose.offset(0.0F, 11.0F, 0.0F));
	}

	/** 바위판 + 위에 비스듬히 솟은 덩어리. */
	private static void plate(PartDefinition body, String name, float side, boolean mirror, CubeDeformation deformation) {
		body.addOrReplaceChild(name, CubeListBuilder.create().mirror(mirror)
				.texOffs(0, 60).addBox(-6.0F, -1.0F, -6.0F, 12, 4, 12, deformation)
				.texOffs(48, 60).addBox(side < 0 ? -5.0F : -2.0F, -5.0F, -3.5F, 7, 4, 7, deformation),
				PartPose.offsetAndRotation(side * 14.5F, -26.0F, 0.0F, 0.0F, 0.0F, side * -0.12F));
	}

	private static void spike(PartDefinition body, String name, float x, float y, float z, float xRot, float zRot, CubeDeformation deformation) {
		body.addOrReplaceChild(name, CubeListBuilder.create()
				.texOffs(104, 60).addBox(-1.5F, -8.0F, -1.5F, 3, 8, 3, deformation),
				PartPose.offsetAndRotation(x, y, z, xRot, 0.0F, zRot));
	}

	private static CubeListBuilder leg(boolean mirror, CubeDeformation deformation) {
		return CubeListBuilder.create().mirror(mirror)
				.texOffs(32, 39).addBox(-3.5F, 0.0F, -3.5F, 7, 12, 7, deformation)
				.texOffs(60, 41).addBox(-4.5F, 12.0F, -4.5F, 9, 4, 9, deformation);
	}

	@Override
	public void setupAnim(VoidColossusRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float speed = state.walkAnimationSpeed;
		float swing = Mth.cos(state.walkAnimationPos * 0.45F);

		head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;

		// 공허에 뜬 바위처럼 천천히 오르내리고 좌우로 흔들린다. 살짝 앞으로 굽은 자세
		body.y += Mth.sin(age * 0.07F) * 0.7F;
		body.xRot = 0.06F;
		body.zRot = Mth.sin(age * 0.045F) * 0.035F + swing * speed * 0.04F;
		rightLeg.xRot = 0.8F * swing * speed;
		leftLeg.xRot = -0.8F * swing * speed;
		// 긴 팔은 늘어뜨리고 천천히 흔들린다 (걸으면 다리와 반대로)
		rightArm.xRot = -0.7F * swing * speed + Mth.sin(age * 0.07F + 0.5F) * 0.05F;
		leftArm.xRot = 0.7F * swing * speed + Mth.sin(age * 0.07F + 2.0F) * 0.05F;
		rightArm.zRot = 0.1F;
		leftArm.zRot = -0.1F;
		rightForearm.xRot = -0.2F;
		leftForearm.xRot = -0.2F;
		// 어깨 바위는 몸과 따로 둥실거리며 천천히 돈다
		rightPlate.y += Mth.sin(age * 0.1F) * 1.2F;
		leftPlate.y += Mth.sin(age * 0.1F + 2.1F) * 1.2F;
		rightPlate.yRot = age * 0.015F;
		leftPlate.yRot = -age * 0.015F;
		rightPlate.zRot += Mth.sin(age * 0.06F) * 0.05F;
		leftPlate.zRot -= Mth.sin(age * 0.06F + 1.0F) * 0.05F;
		// 핵은 가슴 구멍 안에서 돈다 (화나면 빨리)
		float spin = state.enraged ? 0.16F : 0.07F;
		core.yRot = age * spin;
		core.xRot = age * spin * 0.6F;
		crown.yRot = age * 0.04F;
		crown.y += Mth.sin(age * 0.09F) * 0.5F;

		float pull = state.pullAnimation;
		if (pull > 0.0F) {
			float p = Mth.sin(Math.min(pull, 1.0F) * Mth.HALF_PI);
			if (!state.pulling) {
				// 예고: 두 팔을 앞으로 벌려 들고 몸을 젖힌다
				rightArm.xRot = Mth.lerp(p, rightArm.xRot, -1.45F);
				leftArm.xRot = Mth.lerp(p, leftArm.xRot, -1.45F);
				rightArm.zRot += 0.55F * p;
				leftArm.zRot -= 0.55F * p;
				rightForearm.xRot = Mth.lerp(p, rightForearm.xRot, -0.05F);
				leftForearm.xRot = Mth.lerp(p, leftForearm.xRot, -0.05F);
				body.xRot -= 0.2F * p;
				head.xRot -= 0.3F * p;
			} else {
				// 끌어당기는 중: 두 팔을 몸 쪽으로 끌어오며 떤다
				float tug = Mth.sin(age * 0.8F) * 0.12F;
				rightArm.xRot = Mth.lerp(p, rightArm.xRot, -1.1F + tug);
				leftArm.xRot = Mth.lerp(p, leftArm.xRot, -1.1F - tug);
				rightArm.zRot += 0.15F * p;
				leftArm.zRot -= 0.15F * p;
				rightForearm.xRot = Mth.lerp(p, rightForearm.xRot, -1.25F);
				leftForearm.xRot = Mth.lerp(p, leftForearm.xRot, -1.25F);
				body.xRot -= 0.1F * p;
			}
			// 바위가 솟아 벌어지고 핵이 빨리 돈다
			rightPlate.y -= 3.0F * p;
			leftPlate.y -= 3.0F * p;
			rightPlate.x -= 2.0F * p;
			leftPlate.x += 2.0F * p;
			core.yRot += age * 0.2F * p;
		}

		float shield = state.shieldAnimation;
		if (shield > 0.0F) {
			// 수정이 지키는 동안: 바위가 잘게 떨리고 머리 위 조각이 빨리 돈다
			float jitter = Mth.sin(age * 1.7F) * 0.25F * shield;
			rightPlate.x += jitter;
			leftPlate.x -= jitter;
			crown.yRot += age * 0.08F * shield;
		}

		float t = state.attackTicksRemaining;
		if (t > 0.0F) {
			// 두 팔을 머리 위로 들었다가 (앞 40%) 앞으로 내리친다 (다음 30%)
			float p = 1.0F - t / 10.0F;
			float raise = p < 0.4F ? p / 0.4F : Math.max(0.0F, 1.0F - (p - 0.4F) / 0.3F);
			float arm = -0.4F - 2.4F * raise;
			rightArm.xRot = arm;
			leftArm.xRot = arm;
			rightForearm.xRot = -0.3F * raise;
			leftForearm.xRot = -0.3F * raise;
			body.xRot = p < 0.4F ? -0.08F * raise : 0.06F + 0.2F * (1.0F - raise);
		}
	}
}
