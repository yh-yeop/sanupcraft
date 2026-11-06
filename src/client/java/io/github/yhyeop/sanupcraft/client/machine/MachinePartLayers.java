package io.github.yhyeop.sanupcraft.client.machine;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 기계의 움직이는 부품 모델 (확장 M15 R4). 단위는 픽셀(1/16칸), y가 위, 부품마다 원점이 회전 · 이동 중심이다.
 * 텍스처는 한 장 (textures/entity/machine_parts.png, 64x64), 자리는 tools/make_textures.py main_a5와 맞춘다.
 */
public final class MachinePartLayers {
	public static final Identifier TEXTURE = Sanupcraft.id("textures/entity/machine_parts.png");
	public static final ModelLayerLocation CRUSHER_ROLLER = layer("crusher_roller");
	public static final ModelLayerLocation PRESS_FRAME = layer("press_frame");
	public static final ModelLayerLocation PRESS_HEAD = layer("press_head");
	public static final ModelLayerLocation TURBINE_ROTOR = layer("turbine_rotor");
	public static final ModelLayerLocation QUARRY_DRILL = layer("quarry_drill");
	public static final ModelLayerLocation BELT_WHEEL = layer("belt_wheel");

	private static final float QUARTER = Mth.HALF_PI / 2.0F;

	private MachinePartLayers() {
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(CRUSHER_ROLLER, MachinePartLayers::crusherRoller);
		ModelLayerRegistry.registerModelLayer(PRESS_FRAME, MachinePartLayers::pressFrame);
		ModelLayerRegistry.registerModelLayer(PRESS_HEAD, MachinePartLayers::pressHead);
		ModelLayerRegistry.registerModelLayer(TURBINE_ROTOR, MachinePartLayers::turbineRotor);
		ModelLayerRegistry.registerModelLayer(QUARRY_DRILL, MachinePartLayers::quarryDrill);
		ModelLayerRegistry.registerModelLayer(BELT_WHEEL, MachinePartLayers::beltWheel);
	}

	/** 분쇄기 톱니 롤러: X축으로 누운 굴대 + 톱니 8개 (십자 판 둘을 45도 엇갈림). */
	static LayerDefinition crusherRoller() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -1.5F, -1.5F, 10.0F, 3.0F, 3.0F), PartPose.ZERO);
		CubeListBuilder teeth = CubeListBuilder.create()
				.texOffs(0, 6).addBox(-4.5F, -2.5F, -0.5F, 9.0F, 5.0F, 1.0F)
				.texOffs(0, 12).addBox(-4.5F, -0.5F, -2.5F, 9.0F, 1.0F, 5.0F);
		root.addOrReplaceChild("teeth", teeth, PartPose.ZERO);
		root.addOrReplaceChild("teeth_45", teeth, PartPose.rotation(QUARTER, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** 압착기 틀: 기둥 둘 + 위 가로대 (윗면 위에 선다, 원점 = 윗면 가운데). */
	static LayerDefinition pressFrame() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("posts", CubeListBuilder.create().texOffs(32, 0)
				.addBox(-6.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F)
				.addBox(4.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F), PartPose.ZERO);
		root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(0, 18).addBox(-6.0F, 9.0F, -2.0F, 12.0F, 2.0F, 4.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** 압착기 머리: 누르는 판 + 굴대 (원점 = 판 밑면). */
	static LayerDefinition pressHead() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 2.0F, 7.0F), PartPose.ZERO);
		root.addOrReplaceChild("rod", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** 터빈 회전자: 가운데 축 + 비스듬한 날개 6장 (Y축으로 돈다, 원점 = 윗면 가운데). */
	static LayerDefinition turbineRotor() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("hub", CubeListBuilder.create().texOffs(32, 12).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
		for (int i = 0; i < 6; i++) {
			PartDefinition arm = root.addOrReplaceChild("arm" + i, CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, i * Mth.PI / 3.0F, 0.0F));
			arm.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(32, 24).addBox(1.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F),
					PartPose.rotation(0.45F, 0.0F, 0.0F));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** 굴착기 드릴: 아래로 가늘어지는 마디 넷, 마디마다 45도 엇갈려 나선처럼 (원점 = 밑면 가운데). */
	static LayerDefinition quarryDrill() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("seg0", CubeListBuilder.create().texOffs(0, 34).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.ZERO);
		root.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(24, 34).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 3.0F, 4.0F),
				PartPose.rotation(0.0F, QUARTER, 0.0F));
		root.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(40, 34).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.ZERO);
		root.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(48, 34).addBox(-0.5F, -11.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.rotation(0.0F, QUARTER, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** 벨트 모터 바퀴: 원판 둘을 45도 엇갈려 팔각 + 가운데 축 (Z축으로 돈다, 원점 = 앞면 가운데, 앞이 -Z). */
	static LayerDefinition beltWheel() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("disc", CubeListBuilder.create().texOffs(0, 44).addBox(-4.5F, -4.5F, -2.0F, 9.0F, 9.0F, 1.0F), PartPose.ZERO);
		root.addOrReplaceChild("disc_45", CubeListBuilder.create().texOffs(20, 44).addBox(-4.5F, -4.5F, -1.8F, 9.0F, 9.0F, 1.0F),
				PartPose.rotation(0.0F, 0.0F, QUARTER));
		root.addOrReplaceChild("hub", CubeListBuilder.create().texOffs(40, 44).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(Sanupcraft.id("machine/" + name), "main");
	}
}
