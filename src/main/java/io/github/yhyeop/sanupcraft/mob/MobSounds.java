package io.github.yhyeop.sanupcraft.mob;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/** 새 몹 소리 (확장 3 C2). 소리 파일은 {@code sounds.json}에서 바닐라 .ogg를 높낮이만 바꿔 다시 쓴다 (자막은 언어 파일). */
public final class MobSounds {
	private static final List<SoundEvent> ALL = new ArrayList<>();

	public static final SoundEvent AUTOMATON_AMBIENT = register("mob.rust_automaton.ambient");
	public static final SoundEvent AUTOMATON_HURT = register("mob.rust_automaton.hurt");
	public static final SoundEvent AUTOMATON_DEATH = register("mob.rust_automaton.death");
	public static final SoundEvent AUTOMATON_STEP = register("mob.rust_automaton.step");
	public static final SoundEvent WISP_AMBIENT = register("mob.ember_wisp.ambient");
	public static final SoundEvent WISP_HURT = register("mob.ember_wisp.hurt");
	public static final SoundEvent WISP_DEATH = register("mob.ember_wisp.death");
	public static final SoundEvent WISP_SHOOT = register("mob.ember_wisp.shoot");
	public static final SoundEvent SHADE_AMBIENT = register("mob.void_shade.ambient");
	public static final SoundEvent SHADE_HURT = register("mob.void_shade.hurt");
	public static final SoundEvent SHADE_DEATH = register("mob.void_shade.death");
	public static final SoundEvent SHADE_TELEPORT = register("mob.void_shade.teleport");

	private MobSounds() {
	}

	private static SoundEvent register(String name) {
		SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, Sanupcraft.id(name), SoundEvent.createVariableRangeEvent(Sanupcraft.id(name)));
		ALL.add(event);
		return event;
	}

	/** 새 몹 소리 전부 (시험용). */
	public static List<SoundEvent> all() {
		return List.copyOf(ALL);
	}

	static void init() {
	}
}
