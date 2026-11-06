package io.github.yhyeop.sanupcraft.client.storage;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import io.github.yhyeop.sanupcraft.storage.AutocrafterBlockEntity;
import io.github.yhyeop.sanupcraft.storage.AutocrafterMenu;

/** 자동 제작대 화면: 견본 3x3 · 견본 결과 (어둡게), 재료 3x3 · 결과, 증기 막대. */
public class AutocrafterScreen extends AbstractContainerScreen<AutocrafterMenu> {
	public AutocrafterScreen(AutocrafterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		GuiPanels.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		GuiPanels.slots(graphics, menu, leftPos, topPos, AutocrafterMenu.OUTPUT);
		// 견본 칸은 푸르스름하게, 미리보기 칸은 어둡게
		for (int i = AutocrafterMenu.PATTERN_START; i < AutocrafterMenu.INPUT_START; i++) {
			var slot = menu.slots.get(i);
			graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF7A8C99);
		}
		var preview = menu.slots.get(AutocrafterMenu.PREVIEW);
		graphics.fill(leftPos + preview.x, topPos + preview.y, leftPos + preview.x + 16, topPos + preview.y + 16, 0xFF6B6B6B);
		// 증기 막대 (결과 칸 아래)
		int steam = menu.steam();
		int barX = leftPos + 148;
		int barY = topPos + 62;
		graphics.fill(barX, barY, barX + 24, barY + 5, 0xFF373737);
		int fill = Math.round(22.0F * Math.min(steam, AutocrafterBlockEntity.STEAM_CAPACITY) / AutocrafterBlockEntity.STEAM_CAPACITY);
		graphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 4, 0xFF6FD0F0);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.text(font, title, titleLabelX, titleLabelY, GuiPanels.TEXT, false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiPanels.TEXT, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (!menu.getCarried().isEmpty()) {
			return;
		}
		if (hoveredSlot != null && !hoveredSlot.hasItem()) {
			int index = menu.slots.indexOf(hoveredSlot);
			String key = index < AutocrafterMenu.INPUT_START ? "gui.sanupcraft.autocrafter.pattern"
					: index < AutocrafterMenu.OUTPUT ? "gui.sanupcraft.autocrafter.input"
					: index == AutocrafterMenu.OUTPUT ? "gui.sanupcraft.autocrafter.output"
					: index == AutocrafterMenu.PREVIEW ? "gui.sanupcraft.autocrafter.preview" : null;
			if (key != null) {
				graphics.setTooltipForNextFrame(font, Component.translatable(key), mouseX, mouseY);
			}
		} else if (hoveredSlot == null && isHovering(148, 60, 24, 9, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable("gui.sanupcraft.autocrafter.steam", menu.steam(), AutocrafterBlockEntity.STEAM_CAPACITY),
					mouseX, mouseY);
		}
	}
}
