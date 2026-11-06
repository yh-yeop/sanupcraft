package io.github.yhyeop.sanupcraft.client.storage;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 화면 그리기 도우미 (판 · 칸 · 화살표 · 눈금). M7 화면들은 텍스처 없이 이것으로 그리고, 기계 화면은 같은 색 · 모양의 텍스처를 쓴다.
 * 확장 M15 R6: 모든 화면이 같은 모양 — 따뜻한 회색 판 + 모서리 놋쇠 리벳, 같은 칸 테, 놋쇠 테의 세로 눈금
 * (증기 = 푸른 흰색, 물 = 파랑), 놋쇠 진행 화살표. 색은 tools/make_textures.py의 A5_GUI와 같은 값이다.
 */
public final class GuiPanels {
	public static final int TEXT = 0xFF404040;
	public static final int OUTLINE = 0xFF1B1714;
	public static final int PANEL = 0xFFC9C2B4;
	public static final int PANEL_LIGHT = 0xFFF3EDE0;
	public static final int PANEL_DARK = 0xFF6B6253;
	public static final int SLOT = 0xFF8A8377;
	public static final int SLOT_DARK = 0xFF3A342C;
	public static final int SLOT_LIGHT = 0xFFF3EDE0;
	public static final int BRASS_DARK = 0xFF5E4318;
	public static final int BRASS = 0xFFB8893A;
	public static final int BRASS_LIGHT = 0xFFE8C46E;
	public static final int GLASS = 0xFF1F2429;
	/** 증기 눈금: 위 밝은 줄 · 몸통. */
	public static final int STEAM_TOP = 0xFFEEF7FF;
	public static final int STEAM_FILL = 0xFFA9CFF2;
	/** 물 눈금: 위 밝은 줄 · 몸통. */
	public static final int WATER_TOP = 0xFF7FA8F0;
	public static final int WATER_FILL = 0xFF3F76E4;
	/** 기계 화면 텍스처: 오른쪽 빈자리에 채운 화살표 (176, 0) · 빈 화살표 (176, 16) · 켜진 불꽃 (200, 0). */
	public static final Identifier MACHINE_TEXTURE = Sanupcraft.id("textures/gui/machine.png");
	public static final int ARROW_W = 24;
	public static final int ARROW_H = 16;

	private GuiPanels() {
	}

	/** 화면 판: 둥근 검은 테 · 밝은 위왼쪽 · 어두운 아래오른쪽 · 모서리 리벳. */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x + 1, y, x + w - 1, y + h, OUTLINE);
		g.fill(x, y + 1, x + w, y + h - 1, OUTLINE);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_LIGHT);
		g.fill(x + 3, y + 3, x + w - 1, y + h - 1, PANEL_DARK);
		g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
		pixel(g, x + 1, y + 1, OUTLINE);
		pixel(g, x + w - 2, y + h - 2, OUTLINE);
		pixel(g, x + w - 3, y + 2, PANEL);
		pixel(g, x + 2, y + h - 3, PANEL);
		rivet(g, x + 4, y + 4);
		rivet(g, x + w - 7, y + 4);
		rivet(g, x + 4, y + h - 7);
		rivet(g, x + w - 7, y + h - 7);
	}

	/** 3x3 놋쇠 리벳 (x, y = 왼쪽 위). */
	public static void rivet(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x + 1, y, x + 2, y + 3, BRASS);
		g.fill(x, y + 1, x + 3, y + 2, BRASS);
		pixel(g, x + 1, y, BRASS_LIGHT);
		pixel(g, x, y + 1, BRASS_LIGHT);
		pixel(g, x + 2, y + 1, BRASS_DARK);
		pixel(g, x + 1, y + 2, BRASS_DARK);
	}

	/** 아이템 칸 하나 (x, y는 아이템이 그려지는 자리). */
	public static void slot(GuiGraphicsExtractor g, int x, int y) {
		frame(g, x - 1, y - 1, 18);
	}

	/** 결과 칸 (크게, 둘레 놋쇠 테). */
	public static void bigSlot(GuiGraphicsExtractor g, int x, int y) {
		frame(g, x - 5, y - 5, 26);
		g.fill(x - 7, y - 7, x + 23, y - 6, BRASS_DARK);
		g.fill(x - 7, y + 22, x + 23, y + 23, BRASS_DARK);
		g.fill(x - 7, y - 6, x - 6, y + 22, BRASS_DARK);
		g.fill(x + 22, y - 6, x + 23, y + 22, BRASS_DARK);
	}

	/** 칸 테: 위 · 왼쪽 어둡게, 아래 · 오른쪽 밝게 (x0, y0 = 바깥 왼쪽 위). */
	private static void frame(GuiGraphicsExtractor g, int x0, int y0, int size) {
		g.fill(x0, y0, x0 + size, y0 + size, SLOT);
		g.fill(x0, y0, x0 + size - 1, y0 + 1, SLOT_DARK);
		g.fill(x0, y0, x0 + 1, y0 + size - 1, SLOT_DARK);
		g.fill(x0 + 1, y0 + size - 1, x0 + size, y0 + size, SLOT_LIGHT);
		g.fill(x0 + size - 1, y0 + 1, x0 + size, y0 + size, SLOT_LIGHT);
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

	/** 오른쪽을 보는 진행 화살표 24x16 (빈 화살표 위에 progress 0~1만큼 놋쇠로 채움). */
	public static void arrow(GuiGraphicsExtractor g, int x, int y, float progress) {
		g.blit(RenderPipelines.GUI_TEXTURED, MACHINE_TEXTURE, x, y, 176.0F, 16.0F, ARROW_W, ARROW_H, 256, 256);
		arrowFill(g, x, y, progress);
	}

	/** 진행 화살표의 채운 부분만 (빈 화살표는 배경 텍스처에 있을 때). */
	public static void arrowFill(GuiGraphicsExtractor g, int x, int y, float progress) {
		int width = Mth.ceil(ARROW_W * Mth.clamp(progress, 0.0F, 1.0F));
		if (width > 0) {
			g.blit(RenderPipelines.GUI_TEXTURED, MACHINE_TEXTURE, x, y, 176.0F, 0.0F, width, ARROW_H, 256, 256);
		}
	}

	/** 눈금 틀: 놋쇠 테 + 어두운 유리 + 위아래 마개. (x, y, w, h) = 유리 안쪽 (채움이 그려지는 자리). */
	public static void gaugeFrame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x - 2, y - 2, x + w + 2, y + h + 2, BRASS_DARK);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BRASS);
		g.fill(x - 1, y - 1, x + w + 1, y, BRASS_LIGHT);
		g.fill(x - 1, y - 1, x, y + h + 1, BRASS_LIGHT);
		g.fill(x, y, x + w, y + h, GLASS);
		g.fill(x - 1, y - 3, x + w + 1, y - 2, BRASS_DARK);
		g.fill(x - 1, y + h + 2, x + w + 1, y + h + 3, BRASS_DARK);
	}

	/** 세로 증기 눈금 채움 (아래부터) + 눈금 줄. (x, y, w, h) = 유리 안쪽. */
	public static void steamFill(GuiGraphicsExtractor g, int x, int y, int w, int h, int value, int capacity) {
		verticalFill(g, x, y, w, h, value, capacity, STEAM_FILL, STEAM_TOP);
	}

	/** 세로 물 눈금 채움 (아래부터) + 눈금 줄. */
	public static void waterFill(GuiGraphicsExtractor g, int x, int y, int w, int h, int value, int capacity) {
		verticalFill(g, x, y, w, h, value, capacity, WATER_FILL, WATER_TOP);
	}

	private static void verticalFill(GuiGraphicsExtractor g, int x, int y, int w, int h, int value, int capacity, int fill, int top) {
		int level = capacity <= 0 ? 0 : Mth.clamp((int) ((long) value * h / capacity), 0, h);
		if (level > 0) {
			int y0 = y + h - level;
			g.fill(x, y0, x + w, y + h, fill);
			g.fill(x, y0, x + w, y0 + 1, top);
			// 왼쪽 빛 반사 줄
			g.fill(x, y0 + 1, x + 1, y + h, 0x40FFFFFF);
		}
		for (int ty = y + h - 10; ty > y; ty -= 10) {
			g.fill(x + w - 2, ty, x + w, ty + 1, 0x90000000);
		}
	}

	/** 가로 증기 막대 (자리가 좁은 화면용): 같은 놋쇠 테 · 같은 색, 왼쪽부터 채움. (x, y, w, h) = 유리 안쪽. */
	public static void steamBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int value, int capacity) {
		g.fill(x - 2, y - 2, x + w + 2, y + h + 2, BRASS_DARK);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BRASS);
		g.fill(x - 1, y - 1, x + w + 1, y, BRASS_LIGHT);
		g.fill(x, y, x + w, y + h, GLASS);
		int level = capacity <= 0 ? 0 : Mth.clamp((int) ((long) value * w / capacity), 0, w);
		if (level > 0) {
			g.fill(x, y, x + level, y + h, STEAM_FILL);
			g.fill(x, y, x + level, y + 1, STEAM_TOP);
		}
	}

	private static void pixel(GuiGraphicsExtractor g, int x, int y, int color) {
		g.fill(x, y, x + 1, y + 1, color);
	}
}
