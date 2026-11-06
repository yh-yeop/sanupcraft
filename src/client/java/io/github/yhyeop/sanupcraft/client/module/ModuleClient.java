package io.github.yhyeop.sanupcraft.client.module;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.module.GearModule;
import io.github.yhyeop.sanupcraft.module.GearModules;
import io.github.yhyeop.sanupcraft.module.InstalledModules;
import io.github.yhyeop.sanupcraft.module.ModModules;
import io.github.yhyeop.sanupcraft.module.ModuleSlots;

/** 확장 3 G3 클라이언트: 장비 작업대 화면, 장비 툴팁의 "모듈 n/칸" + 끼운 모듈 이름. */
public final class ModuleClient {
	private ModuleClient() {
	}

	public static void init() {
		MenuScreens.register(ModModules.GEAR_WORKBENCH_MENU, GearWorkbenchScreen::new);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> addModuleTooltip(stack, lines));
	}

	/** 모듈 칸이 있는 장비: 속성 줄(빈 줄로 시작) 앞에 "모듈 1/2"와 끼운 모듈 이름. */
	static void addModuleTooltip(ItemStack stack, List<Component> lines) {
		int capacity = ModuleSlots.capacity(stack);
		InstalledModules modules = GearModules.get(stack);
		if (capacity <= 0 && modules.isEmpty()) {
			return;
		}
		List<Component> added = new ArrayList<>();
		added.add(Component.translatable("tooltip.sanupcraft.modules", modules.size(), capacity).withStyle(ChatFormatting.DARK_AQUA));
		for (GearModule module : modules.modules()) {
			added.add(Component.translatable("tooltip.sanupcraft.modules.entry", module.displayName()).withStyle(ChatFormatting.AQUA));
		}
		int at = lines.size();
		for (int i = 1; i < lines.size(); i++) {
			if (lines.get(i).getString().isEmpty()) {
				at = i;
				break;
			}
		}
		lines.addAll(at, added);
	}
}
