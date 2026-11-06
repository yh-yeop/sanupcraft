package io.github.yhyeop.sanupcraft.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.client.storage.GuiPanels;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineMenu;

/**
 * 기계 화면: 재료(합금로는 둘) · 연료 · 결과 · 부산물 · 업그레이드 2칸. 배경은 tools/make_textures.py가 만든 전용 텍스처.
 * 확장 M15 R6: 단말 화면과 같은 모양 ({@link GuiPanels}) — 오른쪽 끝 세로 증기 눈금, 놋쇠 진행 화살표 · 불꽃은 같은 텍스처의 빈자리에서 잘라 쓴다.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
	private static final Identifier TEXTURE = Sanupcraft.id("textures/gui/machine.png");
	private static final Identifier ALLOY_TEXTURE = Sanupcraft.id("textures/gui/alloy_smelter.png");
	/** M8 세척기: 기본 화면 + 왼쪽에 물 눈금 틀. */
	private static final Identifier WASHER_TEXTURE = Sanupcraft.id("textures/gui/washer.png");

	/** 불꽃 그림 자리 (화면 안 좌표). */
	private static final int FLAME_X = 56;
	private static final int FLAME_Y = 36;
	private static final int FLAME_SIZE = 14;
	/** 세척기 물 눈금 안쪽 (화면 안 좌표, 텍스처의 틀과 맞춘다). */
	private static final int WATER_X = 35;
	private static final int WATER_Y = 17;
	private static final int WATER_W = 10;
	private static final int WATER_H = 52;
	/** 증기 눈금 안쪽 (화면 안 좌표, 텍스처의 틀과 맞춘다). */
	private static final int STEAM_X = 164;
	private static final int STEAM_Y = 17;
	private static final int STEAM_W = 5;
	private static final int STEAM_H = 52;
	/** 진행 화살표 자리. */
	private static final int ARROW_X = 79;
	private static final int ARROW_Y = 34;

	public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
	}

	/** 빈 칸에 마우스를 올리면 칸 설명, 불꽃 칸에는 모은 증기를 보여 준다. */
	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (!menu.getCarried().isEmpty()) {
			return;
		}
		if (hoveredSlot != null && !hoveredSlot.hasItem()) {
			String key = slotKey(hoveredSlot.index);
			if (key != null) {
				graphics.setTooltipForNextFrame(font, Component.translatable(key), mouseX, mouseY);
			}
		} else if (hoveredSlot == null && menu.kind().usesWater() && isHovering(WATER_X, WATER_Y, WATER_W, WATER_H, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable("gui.sanupcraft.machine.water", menu.water(), MachineBlockEntity.WATER_CAPACITY), mouseX, mouseY);
		} else if (hoveredSlot == null && (isHovering(FLAME_X, FLAME_Y, FLAME_SIZE, FLAME_SIZE, mouseX, mouseY)
				|| isHovering(STEAM_X - 2, STEAM_Y - 2, STEAM_W + 4, STEAM_H + 4, mouseX, mouseY))) {
			graphics.setTooltipForNextFrame(font, Component.translatable(menu.kind().highPressure() ? "gui.sanupcraft.machine.steam_hp" : "gui.sanupcraft.machine.steam", menu.steam(), MachineBlockEntity.STEAM_CAPACITY), mouseX, mouseY);
		}
	}

	private static String slotKey(int slotIndex) {
		return switch (slotIndex) {
			case MachineBlockEntity.SLOT_INPUT -> "gui.sanupcraft.machine.slot.input";
			case MachineBlockEntity.SLOT_INPUT2 -> "gui.sanupcraft.machine.slot.input2";
			case MachineBlockEntity.SLOT_FUEL -> "gui.sanupcraft.machine.slot.fuel";
			case MachineBlockEntity.SLOT_RESULT -> "gui.sanupcraft.machine.slot.result";
			case MachineBlockEntity.SLOT_BYPRODUCT -> "gui.sanupcraft.machine.slot.byproduct";
			case MachineBlockEntity.SLOT_UPGRADE, MachineBlockEntity.SLOT_UPGRADE + 1 -> "gui.sanupcraft.machine.slot.upgrade";
			default -> null;
		};
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = leftPos;
		int yo = topPos;
		Identifier texture = menu.twoInputs() ? ALLOY_TEXTURE : menu.kind().usesWater() ? WASHER_TEXTURE : TEXTURE;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, xo, yo, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
		if (menu.kind().usesWater()) {
			GuiPanels.waterFill(graphics, xo + WATER_X, yo + WATER_Y, WATER_W, WATER_H, menu.water(), MachineBlockEntity.WATER_CAPACITY);
		}
		GuiPanels.steamFill(graphics, xo + STEAM_X, yo + STEAM_Y, STEAM_W, STEAM_H, menu.steam(), MachineBlockEntity.STEAM_CAPACITY);
		if (menu.isLit()) {
			int litHeight = Mth.ceil(menu.litProgress() * 13.0F) + 1;
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture, xo + FLAME_X, yo + FLAME_Y + FLAME_SIZE - litHeight, 200.0F, FLAME_SIZE - litHeight,
					FLAME_SIZE, litHeight, 256, 256);
		}
		GuiPanels.arrowFill(graphics, xo + ARROW_X, yo + ARROW_Y, menu.progress());
	}
}
