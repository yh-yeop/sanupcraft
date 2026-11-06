package io.github.yhyeop.sanupcraft.server;

import java.util.List;

import net.minecraft.resources.Identifier;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/** 공용 목표판(T3)의 기본 목표 목록. 목표판은 하나를 채우면 이 목록의 다음 것으로 넘어가고, 끝이면 처음으로 돌아온다. */
public final class Goals {
	/** 아이템 id와 개수. */
	public record Goal(Identifier item, int count) {
	}

	public static final List<Goal> DEFAULTS = List.of(
			new Goal(Sanupcraft.id("steel_ingot"), 1000),
			new Goal(Sanupcraft.id("iron_plate"), 2000),
			new Goal(Sanupcraft.id("rivet"), 5000),
			new Goal(Sanupcraft.id("gear_part"), 500));

	private Goals() {
	}

	/** {@code index}번째 기본 목표 (목록 끝을 넘으면 처음부터 다시). */
	public static Goal defaultGoal(int index) {
		return DEFAULTS.get(Math.floorMod(index, DEFAULTS.size()));
	}
}
