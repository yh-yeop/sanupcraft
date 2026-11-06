package io.github.yhyeop.sanupcraft.client.boss;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.resources.Identifier;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 보스 전용 모델 레이어 (확장 M11 X1). 모양은 각 모델의 createBodyLayer()가 코드로 정한다.
 * 새 보스는 여기에 레이어 하나 + 등록 한 줄, 텍스처는 textures/entity/&lt;보스&gt;.png (빛나는 부분은 &lt;보스&gt;_glow.png).
 */
public final class BossModelLayers {
	public static final ModelLayerLocation RUSTY_GUARDIAN = layer("rusty_guardian");
	public static final ModelLayerLocation FURNACE_WARDEN = layer("furnace_warden");
	public static final ModelLayerLocation VOID_COLOSSUS = layer("void_colossus");
	/** 공허 거상 보호막 (수정이 살아 있을 때): 같은 모양을 조금 부풀린 것. */
	public static final ModelLayerLocation VOID_COLOSSUS_SHIELD = new ModelLayerLocation(Sanupcraft.id("void_colossus"), "shield");

	private BossModelLayers() {
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(RUSTY_GUARDIAN, RustyGuardianModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(FURNACE_WARDEN, FurnaceWardenModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(VOID_COLOSSUS, VoidColossusModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(VOID_COLOSSUS_SHIELD, () -> VoidColossusModel.createBodyLayer(new CubeDeformation(0.6F)));
	}

	/** 보스 텍스처 경로: textures/entity/&lt;이름&gt;.png */
	public static Identifier texture(String name) {
		return Sanupcraft.id("textures/entity/" + name + ".png");
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(Sanupcraft.id(name), "main");
	}
}
