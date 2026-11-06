package io.github.yhyeop.sanupcraft.client.module;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import io.github.yhyeop.sanupcraft.client.storage.GuiPanels;
import io.github.yhyeop.sanupcraft.module.GearWorkbenchMenu;
import io.github.yhyeop.sanupcraft.module.ModuleSlots;

/** 장비 작업대 화면: 왼쪽 큰 장비 칸, 오른쪽 모듈 칸 셋 (장비의 칸 수만큼 열리고 나머지는 잠긴 칸), 아래 칸 수 안내. */
public class GearWorkbenchScreen extends AbstractContainerScreen<GearWorkbenchMenu> {
	private static final int LOCKED = 0xFF4A443B;
	private static final int LOCKED_MARK = 0xFF2A251F;

	public GearWorkbenchScreen(GearWorkbenchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		GuiPanels.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			int x = leftPos + slot.x;
			int y = topPos + slot.y;
			if (i == GearWorkbenchMenu.GEAR_SLOT) {
				GuiPanels.bigSlot(graphics, x, y);
			} else if (i < GearWorkbenchMenu.INVENTORY_START && !slot.isActive()) {
				// 잠긴 모듈 칸: 어두운 칸 + 가운데 X
				graphics.fill(x - 1, y - 1, x + 17, y + 17, LOCKED);
				for (int k = 3; k < 13; k++) {
					graphics.fill(x + k, y + k, x + k + 1, y + k + 1, LOCKED_MARK);
					graphics.fill(x + 15 - k, y + k, x + 16 - k, y + k + 1, LOCKED_MARK);
				}
			} else {
				GuiPanels.slot(graphics, x, y);
			}
		}
		// 장비 칸과 모듈 칸 사이 놋쇠 줄
		int lineY = topPos + GearWorkbenchMenu.MODULE_Y + 8;
		graphics.fill(leftPos + GearWorkbenchMenu.GEAR_X + 22, lineY, leftPos + GearWorkbenchMenu.MODULE_X - 2, lineY + 1, GuiPanels.BRASS);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.text(font, title, titleLabelX, titleLabelY, GuiPanels.TEXT, false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiPanels.TEXT, false);
		Component info = menu.gear().isEmpty() ? Component.translatable("gui.sanupcraft.gear_workbench.empty")
				: Component.translatable("gui.sanupcraft.gear_workbench.slots", menu.capacity() + "/" + ModuleSlots.MAX);
		graphics.text(font, info, GearWorkbenchMenu.MODULE_X - 1, GearWorkbenchMenu.MODULE_Y + 22, GuiPanels.TEXT, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (menu.getCarried().isEmpty() && hoveredSlot != null && menu.slots.indexOf(hoveredSlot) == GearWorkbenchMenu.GEAR_SLOT && !hoveredSlot.hasItem()) {
			graphics.setTooltipForNextFrame(font, Component.translatable("gui.sanupcraft.gear_workbench.gear_slot"), mouseX, mouseY);
		}
	}
}
