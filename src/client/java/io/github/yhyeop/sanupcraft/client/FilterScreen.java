package io.github.yhyeop.sanupcraft.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import io.github.yhyeop.sanupcraft.logistics.FilterMenu;

/**
 * 필터 화면 (벨트 분류기 · 필터 깔때기). 그림은 바닐라 발사기(3x3) · 깔때기(1x5) 화면을 그대로 쓴다.
 * 칸 위치는 {@link FilterMenu}가 그 그림에 맞춰 놓는다.
 */
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
	private static final Identifier DISPENSER = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");
	private static final Identifier HOPPER = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");

	private final boolean grid;

	public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, menu.filterSize() == FilterMenu.SORTER_SIZE ? 166 : 133);
		this.grid = menu.filterSize() == FilterMenu.SORTER_SIZE;
		if (!grid) {
			inventoryLabelY = imageHeight - 94;
		}
	}

	@Override
	public void init() {
		super.init();
		if (grid) {
			titleLabelX = (imageWidth - font.width(title)) / 2;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, grid ? DISPENSER : HOPPER, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
	}
}
