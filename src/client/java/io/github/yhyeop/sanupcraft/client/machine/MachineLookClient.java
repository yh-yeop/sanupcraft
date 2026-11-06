package io.github.yhyeop.sanupcraft.client.machine;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.nether.ModNether;

/** 확장 M15 R4: 움직이는 기계 부품 모델 · 렌더러 등록. */
public final class MachineLookClient {
	private MachineLookClient() {
	}

	public static void init() {
		MachinePartLayers.init();
		BlockEntityRenderers.register(ModMachines.MACHINE_BLOCK_ENTITY, MachinePartsRenderer::new);
		BlockEntityRenderers.register(ModNether.STEAM_TURBINE_BLOCK_ENTITY, MachinePartsRenderer::new);
		BlockEntityRenderers.register(ModMining.QUARRY_BLOCK_ENTITY, MachinePartsRenderer::new);
		BlockEntityRenderers.register(ModLogistics.BELT_MOTOR_BLOCK_ENTITY, MachinePartsRenderer::new);
	}
}
