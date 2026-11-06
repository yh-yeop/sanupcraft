package io.github.yhyeop.sanupcraft.client;

import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.client.storage.StorageClient;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.transport.ModTransport;

public class SanupcraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMachines.MACHINE_MENU, MachineScreen::new);
		MenuScreens.register(ModLogistics.SORTER_MENU, FilterScreen::new);
		MenuScreens.register(ModLogistics.FILTER_HOPPER_MENU, FilterScreen::new);
		EntityRendererRegistry.register(ModBoss.RUSTY_GUARDIAN, RustyGuardianRenderer::new);
		EntityRendererRegistry.register(ModBoss.FURNACE_WARDEN, FurnaceWardenRenderer::new);
		EntityRendererRegistry.register(SteamGear.RIVET_ENTITY, ThrownItemRenderer::new);
		// M8 묶음 A: 바닐라 수레 모델에 보일러 · 통을 얹어 그린다 (엔티티의 getDefaultDisplayBlockState)
		EntityRendererRegistry.register(ModTransport.STEAM_LOCOMOTIVE, context -> new MinecartRenderer(context, ModelLayers.FURNACE_MINECART));
		EntityRendererRegistry.register(ModTransport.CARGO_WAGON, context -> new MinecartRenderer(context, ModelLayers.CHEST_MINECART));
		EntityRendererRegistry.register(io.github.yhyeop.sanupcraft.nether.ModNether.LAVA_STEAMBOAT_ENTITY, LavaSteamboatRenderer::new); // M5
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> addGuideTooltip(stack, lines));
		JetpackClient.init(); // 확장 M9a
		// M7 보관 · 제작
		StorageClient.init();
		io.github.yhyeop.sanupcraft.client.boss.BossModelLayers.init(); // 확장 M11a: 보스 전용 모델
		// M6 엔드 최종: 공허 거상 · 공허 수정 (전용 모델, X1)
		EntityRendererRegistry.register(io.github.yhyeop.sanupcraft.end.ModEnd.VOID_COLOSSUS, io.github.yhyeop.sanupcraft.client.end.VoidColossusRenderer::new);
		EntityRendererRegistry.register(io.github.yhyeop.sanupcraft.end.ModEnd.VOID_CRYSTAL, io.github.yhyeop.sanupcraft.client.end.VoidCrystalRenderer::new);
		io.github.yhyeop.sanupcraft.client.machine.MachineLookClient.init(); // 확장 M15 R4: 기계 움직이는 부품
		io.github.yhyeop.sanupcraft.client.bossgear.C4Client.init(); // 확장 3 C4 보스 2페이즈 · 전용 장비
	}

	/**
	 * 기계 · 장비 안내 문구. 아이템 이름 키 뒤에 .tooltip, .tooltip2, .tooltip3 이 언어 파일에 있으면 이름 바로 아래에 회색 줄로 붙인다.
	 * 문구는 전부 언어 파일에만 있어서, 키가 없는 아이템에는 아무것도 붙지 않는다.
	 */
	static void addGuideTooltip(ItemStack stack, List<Component> lines) {
		String base = stack.getItem().getDescriptionId();
		Language language = Language.getInstance();
		int at = Math.min(1, lines.size());
		for (String suffix : new String[] {".tooltip", ".tooltip2", ".tooltip3"}) {
			String key = base + suffix;
			if (language.has(key)) {
				lines.add(at++, Component.translatable(key).withStyle(ChatFormatting.GRAY));
			}
		}
	}
}
