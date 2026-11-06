package io.github.yhyeop.sanupcraft.client.storage;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import io.github.yhyeop.sanupcraft.storage.StockerMenu;

/** 재고 유지기 화면: 견본 칸, 유지할 개수 · 지금 개수 · 상태, 개수 버튼 6개. */
public class StockerScreen extends AbstractContainerScreen<StockerMenu> {
	public StockerScreen(StockerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < StockerMenu.STEPS.length; i++) {
			int step = StockerMenu.STEPS[i];
			int id = i;
			addRenderableWidget(Button.builder(Component.literal(step > 0 ? "+" + step : Integer.toString(step)),
					button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id)).bounds(leftPos + 8 + i * 27, topPos + 54, 26, 14).build());
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		GuiPanels.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		GuiPanels.slots(graphics, menu, leftPos, topPos, 0);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.text(font, title, titleLabelX, titleLabelY, GuiPanels.TEXT, false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiPanels.TEXT, false);
		graphics.text(font, Component.translatable("gui.sanupcraft.stocker.target", menu.targetCount()), 52, 20, GuiPanels.TEXT, false);
		graphics.text(font, Component.translatable("gui.sanupcraft.stocker.have", menu.have()), 52, 31, GuiPanels.TEXT, false);
		graphics.text(font, Component.translatable("gui.sanupcraft.stocker.status." + menu.status().name().toLowerCase()), 52, 42, 0xFF2D6A3E, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.index == 0 && menu.slots.indexOf(hoveredSlot) == 0 && !hoveredSlot.hasItem()) {
			graphics.setTooltipForNextFrame(font, Component.translatable("gui.sanupcraft.stocker.slot"), mouseX, mouseY);
		}
	}
}
