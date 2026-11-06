package io.github.yhyeop.sanupcraft.factory;

import net.minecraft.world.item.Item;

/** 기계 업그레이드 아이템. 기계 화면의 업그레이드 칸에만 들어간다 (깔때기 · 벨트로는 못 넣고 못 뺀다). */
public class MachineUpgradeItem extends Item {
	private final MachineUpgrade upgrade;

	public MachineUpgradeItem(MachineUpgrade upgrade, Item.Properties properties) {
		super(properties);
		this.upgrade = upgrade;
	}

	public MachineUpgrade upgrade() {
		return upgrade;
	}
}
