package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.gametest.framework.GameTestHelper;

import io.github.yhyeop.sanupcraft.Sanupcraft;

public class SanupcraftGameTest {
	@GameTest
	public void modLoads(GameTestHelper helper) {
		helper.assertTrue(FabricLoader.getInstance().isModLoaded(Sanupcraft.MOD_ID), "sanupcraft가 로드되지 않음");
		helper.succeed();
	}
}
