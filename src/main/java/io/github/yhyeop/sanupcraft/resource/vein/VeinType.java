package io.github.yhyeop.sanupcraft.resource.vein;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import io.github.yhyeop.sanupcraft.resource.ModResources;

/**
 * 광맥 종류 (확장 3 D2). 오버월드 · 네더에서 차원별 가중치로 뽑고, 굴착기 덤으로는 {@link #item()} 하나씩 나온다.
 * 이름 · 색 · 한 글자 표시는 lang {@code vein.sanupcraft.<id>} / {@code .short}.
 */
public enum VeinType {
	IRON("iron", 30, 0, 0xD8AF93, () -> Items.RAW_IRON),
	COPPER("copper", 25, 0, 0xE77C56, () -> Items.RAW_COPPER),
	GOLD("gold", 10, 30, 0xFDF55F, () -> Items.RAW_GOLD),
	CHROMIUM("chromium", 15, 0, 0x7F9DB5, () -> ModResources.RAW_CHROMIUM),
	ALUMINIUM("aluminium", 15, 0, 0xD9A77C, () -> ModResources.RAW_ALUMINIUM),
	COAL("coal", 20, 0, 0x8A8A8A, () -> Items.COAL),
	QUARTZ("quartz", 0, 40, 0xF4EFE8, () -> Items.QUARTZ),
	/** 고대 잔해는 아주 적다 (40 ~ 120). */
	ANCIENT_DEBRIS("ancient_debris", 0, 5, 0xA0522D, () -> Items.ANCIENT_DEBRIS);

	private final String id;
	private final int overworldWeight;
	private final int netherWeight;
	private final int color;
	private final Supplier<Item> item;

	VeinType(String id, int overworldWeight, int netherWeight, int color, Supplier<Item> item) {
		this.id = id;
		this.overworldWeight = overworldWeight;
		this.netherWeight = netherWeight;
		this.color = color;
		this.item = item;
	}

	public String id() {
		return id;
	}

	public int overworldWeight() {
		return overworldWeight;
	}

	public int netherWeight() {
		return netherWeight;
	}

	/** 표시 색 (RGB). */
	public int color() {
		return color;
	}

	/** 굴착기 덤으로 나오는 아이템. */
	public Item item() {
		return item.get();
	}

	public boolean isDebris() {
		return this == ANCIENT_DEBRIS;
	}

	public String nameKey() {
		return "vein.sanupcraft." + id;
	}

	public String shortKey() {
		return "vein.sanupcraft." + id + ".short";
	}

	/** id로 찾는다. 모르는 id면 null. */
	public static VeinType byId(String id) {
		for (VeinType type : values()) {
			if (type.id.equals(id)) {
				return type;
			}
		}
		return null;
	}
}
