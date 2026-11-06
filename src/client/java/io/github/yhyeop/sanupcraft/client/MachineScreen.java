package io.github.yhyeop.sanupcraft.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineMenu;

/**
 * 기계 화면: 재료(합금로는 둘) · 연료 · 결과 · 부산물 · 업그레이드 2칸. 배경은 tools/make_textures.py가 만든 전용 텍스처,
 * 불꽃 · 진행 화살표는 바닐라 화로 그림을 쓴다.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
	private static final Identifier TEXTURE = Sanupcraft.id("textures/gui/machine.png");
	private static final Identifier ALLOY_TEXTURE = Sanupcraft.id("textures/gui/alloy_smelter.png");
	private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

	/** 불꽃 그림 자리 (화면 안 좌표). */
	private static final int FLAME_X = 56;
	private static final int FLAME_Y = 36;
	private static final int FLAME_SIZE = 14;

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
		} else if (hoveredSlot == null && isHovering(FLAME_X, FLAME_Y, FLAME_SIZE, FLAME_SIZE, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable("gui.sanupcraft.machine.steam", menu.steam(), MachineBlockEntity.STEAM_CAPACITY), mouseX, mouseY);
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
		graphics.blit(RenderPipelines.GUI_TEXTURED, menu.twoInputs() ? ALLOY_TEXTURE : TEXTURE, xo, yo, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
		if (menu.isLit()) {
			int litHeight = Mth.ceil(menu.litProgress() * 13.0F) + 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - litHeight, xo + FLAME_X, yo + FLAME_Y + FLAME_SIZE - litHeight, 14, litHeight);
		}
		int burnWidth = Mth.ceil(menu.progress() * 24.0F);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, xo + 79, yo + 34, burnWidth, 16);
	}
}
