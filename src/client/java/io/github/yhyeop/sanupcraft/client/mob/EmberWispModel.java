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
 * 잿불 정령: 떠 있는 불꽃 핵 (네모난 숯덩이에 갈라진 틈 · 두 눈), 위로 일렁이는 불꽃 넷, 아래로 가늘어지는 꼬리,
 * 둘레를 도는 불씨 조각 넷 (큰 것 둘 · 작은 것 둘, 위아래로 출렁임). 키 약 1.2칸.
 * 텍스처 64x32 (tools/make_textures.py 의 main_ex3_c2).
 */
public class EmberWispModel extends EntityModel<MobRenderState> {
	private static final int EMBERS = 4;
	private static final float ORBIT_RADIUS = 6.5F;
	private final ModelPart core;
	private final ModelPart[] flames = new ModelPart[4];
	private final ModelPart tail;
	private final ModelPart tip;
	private final ModelPart orbit;
	private final ModelPart[] embers = new ModelPart[EMBERS];

	public EmberWispModel(ModelPart root) {
		super(root);
		core = root.getChild("core");
		flames[0] = core.getChild("flame_mid");
		flames[1] = core.getChild("flame_left");
		flames[2] = core.getChild("flame_right");
		flames[3] = core.getChild("flame_back");
		tail = core.getChild("tail");
		tip = tail.getChild("tip");
		orbit = root.getChild("orbit");
		for (int i = 0; i < EMBERS; i++) {
			embers[i] = orbit.getChild("ember" + i);
		}
	}

	public static LayerDefinition createBodyLayer(CubeDeformation g) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6, g),
				PartPose.offset(0.0F, 12.0F, 0.0F));
		CubeListBuilder flame = CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2, g);
		core.addOrReplaceChild("flame_mid", flame, PartPose.offset(0.0F, -3.0F, 0.0F));
		core.addOrReplaceChild("flame_left", flame, PartPose.offsetAndRotation(-2.0F, -2.5F, 0.0F, 0.0F, 0.0F, -0.5F));
		core.addOrReplaceChild("flame_right", flame, PartPose.offsetAndRotation(2.0F, -2.5F, 0.0F, 0.0F, 0.0F, 0.5F));
		core.addOrReplaceChild("flame_back", flame, PartPose.offsetAndRotation(0.0F, -2.5F, 2.0F, -0.5F, 0.0F, 0.0F));
		PartDefinition tail = core.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-1.5F, 0.0F, -1.5F, 3, 4, 3, g),
				PartPose.offset(0.0F, 3.0F, 0.0F));
		tail.addOrReplaceChild("tip", CubeListBuilder.create()
				.texOffs(44, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1, g),
				PartPose.offset(0.0F, 4.0F, 0.0F));
		PartDefinition orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
		for (int i = 0; i < EMBERS; i++) {
			float angle = i * Mth.TWO_PI / EMBERS;
			CubeListBuilder ember = i % 2 == 0
					? CubeListBuilder.create().texOffs(8, 12).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3, g)
					: CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2, g);
			orbit.addOrReplaceChild("ember" + i, ember,
					PartPose.offset(Mth.cos(angle) * ORBIT_RADIUS, 0.0F, Mth.sin(angle) * ORBIT_RADIUS));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(MobRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float bob = Mth.sin(age * 0.1F);
		core.y = 12.0F + bob;
		core.yRot = Mth.clamp(state.yRot, -70.0F, 70.0F) * Mth.DEG_TO_RAD;
		core.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
		// 불꽃이 일렁인다 (기본 기울기 + 흔들림)
		float[] baseZ = {0.0F, -0.5F, 0.5F, 0.0F};
		float[] baseX = {0.0F, 0.0F, 0.0F, -0.5F};
		for (int i = 0; i < flames.length; i++) {
			float flicker = Mth.sin(age * 0.55F + i * 1.9F);
			flames[i].zRot = baseZ[i] + 0.15F * flicker;
			flames[i].xRot = baseX[i] + 0.1F * Mth.cos(age * 0.45F + i);
			flames[i].yScale = 1.0F + 0.2F * flicker;
		}
		tail.xRot = 0.15F * Mth.sin(age * 0.2F);
		tail.zRot = 0.12F * Mth.cos(age * 0.17F);
		tip.xRot = 0.25F * Mth.sin(age * 0.2F - 0.6F);
		// 불씨가 핵 둘레를 돌며 위아래로 출렁이고 저마다 구른다. 쏠 때(휘두르기)는 바짝 붙는다
		float pull = 1.0F - 0.35F * Mth.sin(state.swing * Mth.PI);
		orbit.y = 12.0F + bob * 0.5F;
		orbit.yRot = age * 0.12F;
		for (int i = 0; i < EMBERS; i++) {
			float angle = i * Mth.TWO_PI / EMBERS;
			embers[i].x = Mth.cos(angle) * ORBIT_RADIUS * pull;
			embers[i].z = Mth.sin(angle) * ORBIT_RADIUS * pull;
			embers[i].y = Mth.sin(age * 0.2F + i * Mth.HALF_PI) * 2.0F;
			embers[i].xRot = age * 0.3F + i;
			embers[i].zRot = age * 0.21F + i * 0.7F;
		}
	}
}
