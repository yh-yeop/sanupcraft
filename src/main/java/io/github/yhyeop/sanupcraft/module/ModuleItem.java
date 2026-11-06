package io.github.yhyeop.sanupcraft.module;

import net.minecraft.world.item.Item;

/** 모듈 아이템 (G3). 장비 작업대에서 장비에 끼운다. 설명 줄은 언어 파일의 {@code .tooltip}. */
public class ModuleItem extends Item {
	private final GearModule module;

	public ModuleItem(Item.Properties properties, GearModule module) {
		super(properties);
		this.module = module;
	}

	public GearModule module() {
		return module;
	}
}
