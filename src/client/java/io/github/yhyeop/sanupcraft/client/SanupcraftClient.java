package io.github.yhyeop.sanupcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.gui.screens.MenuScreens;

import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.factory.ModMachines;

public class SanupcraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMachines.MACHINE_MENU, MachineScreen::new);
		EntityRendererRegistry.register(ModBoss.RUSTY_GUARDIAN, RustyGuardianRenderer::new);
	}
}
