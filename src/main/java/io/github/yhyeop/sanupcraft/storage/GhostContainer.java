package io.github.yhyeop.sanupcraft.storage;

import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 견본 칸 (자동 제작대 3x3 · 재고 유지기 1칸). 1개짜리 견본만 들어 있고, 놓아도 손의 것은 줄지 않는다.
 * 화면 쪽 클릭 처리는 {@link #click}이 맡는다 (필터 화면과 같은 규칙).
 */
public class GhostContainer extends SimpleContainer {
	private Runnable onChange = () -> {
	};

	public GhostContainer(int size) {
		super(size);
	}

	public void onChange(Runnable onChange) {
		this.onChange = onChange;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		onChange.run();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/** 견본을 놓는다 (빈 스택이면 비운다). */
	public void setSample(int slot, ItemStack stack) {
		setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
	}

	/** 견본 칸 클릭: 들고 누르면 놓기, 빈손이거나 Shift면 비우기, 숫자 키는 그 핫바 칸의 것. 처리했으면 true. */
	public boolean click(int slot, int button, ContainerInput input, Player player, ItemStack carried) {
		switch (input) {
			case PICKUP -> setSample(slot, carried);
			case QUICK_MOVE -> setSample(slot, ItemStack.EMPTY);
			case SWAP -> setSample(slot, button == 40 ? player.getOffhandItem()
					: button >= 0 && button < 9 ? player.getInventory().getItem(button) : ItemStack.EMPTY);
			case CLONE, THROW, PICKUP_ALL -> {
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	public void save(ValueOutput output, String key) {
		ContainerHelper.saveAllItems(output.child(key), getItems());
	}

	public void load(ValueInput input, String key) {
		for (int i = 0; i < getContainerSize(); i++) {
			getItems().set(i, ItemStack.EMPTY);
		}
		ContainerHelper.loadAllItems(input.childOrEmpty(key), getItems());
	}
}
