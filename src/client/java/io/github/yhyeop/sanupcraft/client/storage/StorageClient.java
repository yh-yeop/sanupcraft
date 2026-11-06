package io.github.yhyeop.sanupcraft.client.storage;

import java.util.List;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.storage.DrawerContents;
import io.github.yhyeop.sanupcraft.storage.ModStorage;

/** M7 보관 · 제작의 클라이언트 쪽: 서랍 렌더러, 서랍 아이템 툴팁 (화면 · 패킷은 묶음마다 더한다). */
public final class StorageClient {
	private StorageClient() {
	}

	public static void init() {
		BlockEntityRenderers.register(ModStorage.DRAWER_BLOCK_ENTITY, DrawerRenderer::new);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> addDrawerTooltip(stack, lines));
	}

	/** 내용물이 든 서랍 아이템: 무엇이 몇 개 · 업그레이드. */
	static void addDrawerTooltip(ItemStack stack, List<Component> lines) {
		DrawerContents contents = stack.get(ModStorage.DRAWER_CONTENTS);
		if (contents == null) {
			return;
		}
		if (!contents.item().isEmpty() && contents.count() > 0) {
			lines.add(Component.translatable("item.sanupcraft.drawer.contents", contents.item().getHoverName(), String.format("%,d", contents.count()))
					.withStyle(ChatFormatting.GOLD));
		}
		if (contents.upgrades() > 0 || contents.voiding()) {
			lines.add(Component.translatable("item.sanupcraft.drawer.upgrades", contents.upgrades(), DrawerContentsText.voidText(contents))
					.withStyle(ChatFormatting.GRAY));
		}
	}

	private static final class DrawerContentsText {
		static Component voidText(DrawerContents contents) {
			return Component.translatable(contents.voiding() ? "item.sanupcraft.drawer.void_on" : "item.sanupcraft.drawer.void_off");
		}
	}
}
