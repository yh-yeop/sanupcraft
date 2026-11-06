package io.github.yhyeop.sanupcraft.client.storage;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** 텍스처 없이 바닐라 화면처럼 그리는 도우미 (회색 판 · 칸 · 화살표). M7 화면들이 같이 쓴다. */
public final class GuiPanels {
	public static final int TEXT = 0xFF404040;
	private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");

	private GuiPanels() {
	}

	/** 둥근 모서리의 회색 판. */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
		g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
		g.fill(x + 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
		g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
	}

	/** 아이템 칸 하나 (x, y는 아이템이 그려지는 자리). */
	public static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
		g.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
		g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
	}

	/** 결과 칸 (크게). */
	public static void bigSlot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 5, y - 5, x + 21, y + 21, 0xFF373737);
		g.fill(x - 4, y - 4, x + 21, y + 21, 0xFFFFFFFF);
		g.fill(x - 4, y - 4, x + 20, y + 20, 0xFF8B8B8B);
	}

	/** 메뉴의 모든 칸 배경 (resultIndex번 칸은 크게, -1이면 없음). */
	public static void slots(GuiGraphicsExtractor g, AbstractContainerMenu menu, int left, int top, int resultIndex) {
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			if (i == resultIndex) {
				bigSlot(g, left + slot.x, top + slot.y);
			} else {
				slot(g, left + slot.x, top + slot.y);
			}
		}
	}

	/** 오른쪽을 보는 화살표 (progress 0~1만큼 채움, 1이면 다 채운 흰 화살표). */
	public static void arrow(GuiGraphicsExtractor g, int x, int y, float progress) {
		g.fill(x, y + 6, x + 16, y + 10, 0xFF8B8B8B);
		for (int i = 0; i < 8; i++) {
			g.fill(x + 16 + i, y + i, x + 17 + i, y + 16 - i, 0xFF8B8B8B);
		}
		int width = Math.round(24 * Math.max(0.0F, Math.min(1.0F, progress)));
		if (width > 0) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, x, y, width, 16);
		}
	}
}
