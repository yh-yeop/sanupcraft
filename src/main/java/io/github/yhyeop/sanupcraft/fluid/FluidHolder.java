package io.github.yhyeop.sanupcraft.fluid;

import org.jspecify.annotations.Nullable;

/**
 * 유체를 담는 블록 엔티티 (탱크 · 펌프 · 보일러의 물). 한 번에 한 가지 유체만 담고, 양은 mB 정수.
 * Fabric Transfer API 대신 쓰는 작은 인터페이스다. 증기 쪽 {@code SteamStorage}와 따로 둔다.
 */
public interface FluidHolder {
	/** 지금 든 유체. 비었으면 null. */
	@Nullable FluidKind fluid();

	/** 지금 든 양 (mB). */
	int fluidAmount();

	/** 담을 수 있는 최대 양 (mB). */
	int fluidCapacity();

	/** {@code kind}를 최대 {@code amount}만큼 넣고 실제로 들어간 양을 돌려준다. {@code simulate}면 바꾸지 않고 셈만 한다. */
	int fill(FluidKind kind, int amount, boolean simulate);

	/** {@code kind}를 최대 {@code amount}만큼 빼고 실제로 빠진 양을 돌려준다. 빼 갈 수 없는 것(보일러)은 0. */
	int drain(FluidKind kind, int amount, boolean simulate);
}
