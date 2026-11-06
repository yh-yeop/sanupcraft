package io.github.yhyeop.sanupcraft.factory;

import java.util.Arrays;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 기계 면 설정 (M8 I4). 면마다 기본 / 넣기 / 빼기 / 둘 다 / 막기. 면은 기계가 보는 쪽 기준(앞 · 뒤 · 왼쪽 · 오른쪽 · 위 · 아래)으로
 * 저장해서, 렌치로 기계를 돌리면 설정도 같이 돈다. 왼쪽 · 오른쪽은 앞에서 기계를 볼 때의 왼쪽 · 오른쪽.
 */
public final class MachineFaces {
	/** 면 하나의 모드. 기본 = 예전 규칙 (위 → 재료, 옆 → 연료, 아래 ← 결과). */
	public enum Mode {
		DEFAULT("default"),
		/** 넣기만: 재료 · 연료 칸. */
		IN("in"),
		/** 빼기만: 결과 · 부산물 칸 (+ 연료 칸의 빈 양동이). */
		OUT("out"),
		/** 넣기 + 빼기. */
		BOTH("both"),
		/** 막기: 아무것도 안 들고 나감. */
		NONE("none");

		private final String key;

		Mode(String key) {
			this.key = key;
		}

		public Mode next() {
			Mode[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public boolean canInsert() {
			return this == IN || this == BOTH;
		}

		public boolean canExtract() {
			return this == OUT || this == BOTH;
		}

		public Component displayName() {
			return Component.translatable("item.sanupcraft.wrench.face_mode." + key);
		}
	}

	/** 기계 기준 면. 순서 = 저장 순서. */
	public enum Side {
		FRONT("front"), BACK("back"), LEFT("left"), RIGHT("right"), UP("up"), DOWN("down");

		private final String key;

		Side(String key) {
			this.key = key;
		}

		public Component displayName() {
			return Component.translatable("item.sanupcraft.wrench.face_side." + key);
		}

		/** 기계가 {@code facing}을 볼 때 실제 방향 {@code direction}이 어느 면인가. */
		public static Side of(Direction facing, Direction direction) {
			if (direction == Direction.UP) {
				return UP;
			}
			if (direction == Direction.DOWN) {
				return DOWN;
			}
			if (direction == facing) {
				return FRONT;
			}
			if (direction == facing.getOpposite()) {
				return BACK;
			}
			// 앞에서 기계를 보는 사람의 왼쪽 = 기계가 보는 쪽에서 시계 방향
			return direction == facing.getClockWise() ? LEFT : RIGHT;
		}
	}

	private final Mode[] modes = new Mode[Side.values().length];

	public MachineFaces() {
		Arrays.fill(modes, Mode.DEFAULT);
	}

	public Mode get(Side side) {
		return modes[side.ordinal()];
	}

	public void set(Side side, Mode mode) {
		modes[side.ordinal()] = mode;
	}

	/** 실제 방향의 모드. 방향이 없으면 (손 · 방향 없는 운반) 기본. */
	public Mode get(Direction facing, Direction direction) {
		return get(Side.of(facing, direction));
	}

	public boolean allDefault() {
		for (Mode mode : modes) {
			if (mode != Mode.DEFAULT) {
				return false;
			}
		}
		return true;
	}

	/** "앞 기본 · 뒤 넣기 · ..." 한 줄. */
	public MutableComponent summary() {
		MutableComponent line = Component.empty();
		for (Side side : Side.values()) {
			if (side.ordinal() > 0) {
				line.append(" · ");
			}
			line.append(side.displayName()).append(" ").append(get(side).displayName());
		}
		return line;
	}

	/** 저장: 면 순서대로 모드 번호 6글자 ("000000" = 전부 기본). 전부 기본이면 저장하지 않는다. */
	public void save(ValueOutput output) {
		if (allDefault()) {
			return;
		}
		StringBuilder text = new StringBuilder();
		for (Mode mode : modes) {
			text.append((char) ('0' + mode.ordinal()));
		}
		output.putString("faces", text.toString());
	}

	public void load(ValueInput input) {
		Arrays.fill(modes, Mode.DEFAULT);
		String text = input.getStringOr("faces", "");
		Mode[] all = Mode.values();
		for (int i = 0; i < Math.min(text.length(), modes.length); i++) {
			int index = text.charAt(i) - '0';
			modes[i] = index >= 0 && index < all.length ? all[index] : Mode.DEFAULT;
		}
	}
}
