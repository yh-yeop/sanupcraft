package io.github.yhyeop.sanupcraft.fluid;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 유체 칸 하나: 한 가지 유체를 용량까지. 탱크 · 펌프가 안에 하나씩 갖고 {@link FluidHolder}를 이것에 맡긴다.
 * 비면 유체 종류도 잊어서 다른 유체를 받을 수 있다. 바뀔 때마다 {@code onChange}를 부른다 (블록 엔티티의 setChanged).
 */
public final class FluidTank implements FluidHolder {
	private final int capacity;
	private final Runnable onChange;
	private @Nullable FluidKind kind;
	private int amount;

	public FluidTank(int capacity, Runnable onChange) {
		this.capacity = capacity;
		this.onChange = onChange;
	}

	@Override
	public @Nullable FluidKind fluid() {
		return amount > 0 ? kind : null;
	}

	@Override
	public int fluidAmount() {
		return amount;
	}

	@Override
	public int fluidCapacity() {
		return capacity;
	}

	public int space() {
		return capacity - amount;
	}

	/** {@code kind}를 지금 받을 수 있나 (비었거나 같은 유체). */
	public boolean accepts(FluidKind other) {
		return amount <= 0 || kind == other;
	}

	@Override
	public int fill(FluidKind other, int add, boolean simulate) {
		if (add <= 0 || !accepts(other)) {
			return 0;
		}
		int accepted = Math.min(add, space());
		if (accepted > 0 && !simulate) {
			kind = other;
			amount += accepted;
			onChange.run();
		}
		return accepted;
	}

	@Override
	public int drain(FluidKind other, int take, boolean simulate) {
		if (take <= 0 || amount <= 0 || kind != other) {
			return 0;
		}
		int taken = Math.min(take, amount);
		if (!simulate) {
			amount -= taken;
			if (amount == 0) {
				kind = null;
			}
			onChange.run();
		}
		return taken;
	}

	/** 내용을 바로 정한다 (불러오기 · 아이템 컴포넌트). 용량을 넘으면 자른다. */
	public void set(@Nullable FluidKind other, int value) {
		amount = other == null ? 0 : Math.max(0, Math.min(capacity, value));
		kind = amount > 0 ? other : null;
	}

	public void save(ValueOutput output) {
		if (amount > 0 && kind != null) {
			output.putString("fluid", kind.getSerializedName());
			output.putInt("amount", amount);
		}
	}

	public void load(ValueInput input) {
		set(FluidKind.byName(input.getStringOr("fluid", "")), input.getIntOr("amount", 0));
	}
}
