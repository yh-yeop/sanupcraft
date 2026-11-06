package io.github.yhyeop.sanupcraft.client.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.storage.StorageNetwork;
import io.github.yhyeop.sanupcraft.storage.StoragePayloads;
import io.github.yhyeop.sanupcraft.storage.TerminalMenu;

/**
 * 창고 단말 화면: 위쪽 목록(검색 · 정렬 · 굴리기), (제작 단말이면) 3x3 제작 칸, 아래 인벤토리.
 * 목록 클릭: 왼쪽 = 한 더미, 오른쪽 = 반 더미, Shift+왼쪽 = 인벤토리로. 아이템을 든 채 목록을 누르면 넣는다 (오른쪽 = 하나).
 * 검색은 이름 · 아이템 ID, "@모드"로 모드별.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
	private static final int WIDTH = 194;
	private static final int SCROLL_X = 174;

	private enum Sort {
		COUNT, NAME
	}

	private final int rows;
	private @Nullable EditBox search;
	private Sort sort = Sort.COUNT;
	private List<StorageNetwork.Entry> visible = List.of();
	private int seenVersion = -1;
	private String seenQuery = "";
	private int scrollRow;
	private boolean draggingScroll;

	public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, menu.imageHeight());
		this.rows = menu.isCrafting() ? TerminalMenu.LIST_ROWS_CRAFTING : TerminalMenu.LIST_ROWS;
		this.inventoryLabelY = menu.slots.get(menu.inventoryStart()).y - 11;
	}

	@Override
	protected void init() {
		super.init();
		search = new EditBox(font, leftPos + 98, topPos + 5, 72, 10, Component.translatable("gui.sanupcraft.terminal.search"));
		search.setMaxLength(50);
		search.setHint(Component.translatable("gui.sanupcraft.terminal.search").withStyle(ChatFormatting.DARK_GRAY));
		addRenderableWidget(search);
		addRenderableWidget(Button.builder(sortLabel(), button -> {
			sort = sort == Sort.COUNT ? Sort.NAME : Sort.COUNT;
			button.setMessage(sortLabel());
			seenVersion = -1;
		}).bounds(leftPos + 174, topPos + 4, 14, 12).build());
	}

	private Component sortLabel() {
		return Component.literal(sort == Sort.COUNT ? "#" : "A");
	}

	/** 목록을 거르고 정렬한다 (내용 · 검색어 · 정렬이 바뀌었을 때만). */
	private void refresh() {
		String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		if (seenVersion == menu.clientVersion() && query.equals(seenQuery)) {
			return;
		}
		seenVersion = menu.clientVersion();
		seenQuery = query;
		List<StorageNetwork.Entry> list = new ArrayList<>();
		for (StorageNetwork.Entry entry : menu.clientEntries()) {
			if (matches(entry.stack(), query)) {
				list.add(entry);
			}
		}
		if (sort == Sort.NAME) {
			list.sort(Comparator.comparing(e -> e.stack().getHoverName().getString()));
		} else {
			list.sort(Comparator.comparingLong(StorageNetwork.Entry::count).reversed().thenComparing(e -> e.stack().getHoverName().getString()));
		}
		visible = list;
		scrollRow = Math.min(scrollRow, maxScroll());
	}

	private static boolean matches(ItemStack stack, String query) {
		if (query.isEmpty()) {
			return true;
		}
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (query.startsWith("@")) {
			return id.getNamespace().startsWith(query.substring(1));
		}
		return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query) || id.getPath().contains(query);
	}

	private int maxScroll() {
		int totalRows = (visible.size() + TerminalMenu.LIST_COLUMNS - 1) / TerminalMenu.LIST_COLUMNS;
		return Math.max(0, totalRows - rows);
	}

	/** 마우스 아래 목록 칸 번호 (목록 밖이면 -1, 칸은 있지만 비었으면 visible 크기 이상). */
	private int cellAt(double mouseX, double mouseY) {
		int x = (int) Math.floor(mouseX) - leftPos - TerminalMenu.LIST_X + 1;
		int y = (int) Math.floor(mouseY) - topPos - TerminalMenu.LIST_Y + 1;
		if (x < 0 || y < 0 || x >= TerminalMenu.LIST_COLUMNS * 18 || y >= rows * 18) {
			return -1;
		}
		return (scrollRow + y / 18) * TerminalMenu.LIST_COLUMNS + x / 18;
	}

	private boolean onScrollbar(double mouseX, double mouseY) {
		int x = (int) mouseX - leftPos;
		int y = (int) mouseY - topPos;
		return x >= SCROLL_X && x < SCROLL_X + 14 && y >= TerminalMenu.LIST_Y - 1 && y < TerminalMenu.LIST_Y - 1 + rows * 18;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		refresh();
		GuiPanels.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		GuiPanels.slots(graphics, menu, leftPos, topPos, menu.isCrafting() ? 0 : -1);
		if (menu.isCrafting()) {
			var result = menu.slots.get(0);
			GuiPanels.arrow(graphics, leftPos + 90, topPos + result.y, 1.0F);
		}
		// 목록 칸 · 아이템
		int hovered = cellAt(mouseX, mouseY);
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < TerminalMenu.LIST_COLUMNS; col++) {
				int x = leftPos + TerminalMenu.LIST_X + col * 18;
				int y = topPos + TerminalMenu.LIST_Y + row * 18;
				GuiPanels.slot(graphics, x, y);
				int index = (scrollRow + row) * TerminalMenu.LIST_COLUMNS + col;
				if (index < visible.size()) {
					StorageNetwork.Entry entry = visible.get(index);
					graphics.item(entry.stack(), x, y);
					graphics.itemDecorations(font, entry.stack(), x, y, DrawerRenderer.shortCount((int) Math.min(Integer.MAX_VALUE, entry.count())));
				}
				if (index == hovered) {
					graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
				}
			}
		}
		// 굴림 막대
		int trackTop = topPos + TerminalMenu.LIST_Y - 1;
		int trackHeight = rows * 18;
		graphics.fill(leftPos + SCROLL_X, trackTop, leftPos + SCROLL_X + 12, trackTop + trackHeight, GuiPanels.SLOT_DARK);
		graphics.fill(leftPos + SCROLL_X + 1, trackTop + 1, leftPos + SCROLL_X + 12, trackTop + trackHeight, GuiPanels.SLOT);
		int max = maxScroll();
		int thumb = 15;
		int thumbY = trackTop + 1 + (max == 0 ? 0 : (trackHeight - 2 - thumb) * scrollRow / max);
		// 손잡이: 굴릴 수 있으면 놋쇠, 아니면 흐린 판 색 (확장 M15 R6)
		graphics.fill(leftPos + SCROLL_X + 1, thumbY, leftPos + SCROLL_X + 11, thumbY + thumb, max == 0 ? GuiPanels.PANEL_DARK : GuiPanels.BRASS_DARK);
		graphics.fill(leftPos + SCROLL_X + 1, thumbY, leftPos + SCROLL_X + 10, thumbY + thumb - 1, max == 0 ? GuiPanels.PANEL : GuiPanels.BRASS);
		graphics.fill(leftPos + SCROLL_X + 1, thumbY, leftPos + SCROLL_X + 10, thumbY + 1, max == 0 ? GuiPanels.PANEL_LIGHT : GuiPanels.BRASS_LIGHT);
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
		int index = cellAt(mouseX, mouseY);
		if (index >= 0 && index < visible.size()) {
			StorageNetwork.Entry entry = visible.get(index);
			List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(entry.stack()));
			lines.add(Component.translatable("gui.sanupcraft.terminal.count", String.format("%,d", entry.count())).withStyle(ChatFormatting.YELLOW));
			graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (onScrollbar(event.x(), event.y())) {
			draggingScroll = true;
			scrollTo(event.y());
			return true;
		}
		int index = cellAt(event.x(), event.y());
		if (index >= 0 && (event.button() == 0 || event.button() == 1)) {
			boolean right = event.button() == 1;
			int action;
			ItemStack template = ItemStack.EMPTY;
			if (!menu.getCarried().isEmpty()) {
				action = right ? TerminalMenu.ACTION_INSERT_ONE : TerminalMenu.ACTION_INSERT_CARRIED;
			} else if (index < visible.size()) {
				template = visible.get(index).stack();
				action = right ? TerminalMenu.ACTION_TAKE_HALF : event.hasShiftDown() ? TerminalMenu.ACTION_TAKE_TO_INVENTORY : TerminalMenu.ACTION_TAKE_STACK;
			} else {
				return true;
			}
			ClientPlayNetworking.send(new StoragePayloads.Action(menu.containerId, action, template.copyWithCount(1)));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingScroll) {
			scrollTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingScroll = false;
		return super.mouseReleased(event);
	}

	private void scrollTo(double mouseY) {
		int trackTop = topPos + TerminalMenu.LIST_Y;
		double fraction = (mouseY - trackTop - 7) / (rows * 18 - 17.0);
		scrollRow = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * maxScroll());
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (cellAt(x, y) >= 0 || onScrollbar(x, y)) {
			scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow - (int) Math.signum(scrollY)));
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (search != null && search.isFocused() && !event.isEscape()) {
			search.keyPressed(event);
			return true;
		}
		return super.keyPressed(event);
	}
}
