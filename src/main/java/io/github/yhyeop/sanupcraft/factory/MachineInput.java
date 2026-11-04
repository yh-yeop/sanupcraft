package io.github.yhyeop.sanupcraft.factory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** 기계 재료 칸 두 개 (두 번째는 합금로만 씀). */
public record MachineInput(ItemStack first, ItemStack second) implements RecipeInput {
	@Override
	public ItemStack getItem(int index) {
		return index == 0 ? first : second;
	}

	@Override
	public int size() {
		return 2;
	}
}
