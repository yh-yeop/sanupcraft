package io.github.yhyeop.sanupcraft.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 용암 증기선: 바닐라 참나무 배 모델(모델 레이어 {@code boat/oak})에 전용 텍스처만 입힌다.
 * 물 가림막(water patch)은 그리지 않는다 (용암 위에서는 쓸모가 없고, 물에서는 배 안에 물이 조금 비친다).
 */
public class LavaSteamboatRenderer extends AbstractBoatRenderer {
	private final EntityModel<BoatRenderState> model;

	public LavaSteamboatRenderer(EntityRendererProvider.Context context) {
		super(context, Sanupcraft.id("textures/entity/lava_steamboat.png"));
		this.model = new BoatModel(context.bakeLayer(ModelLayers.OAK_BOAT));
	}

	@Override
	protected EntityModel<BoatRenderState> model() {
		return model;
	}
}
