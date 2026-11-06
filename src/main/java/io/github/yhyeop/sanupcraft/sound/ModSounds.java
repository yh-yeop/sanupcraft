package io.github.yhyeop.sanupcraft.sound;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.factory.MachineKind;

/**
 * 효과음 (M11 X2). 소리 파일은 새로 넣지 않고 {@code assets/sanupcraft/sounds.json}에서 바닐라 .ogg를 다시 쓴다 (자막은 언어 파일).
 * 기계 소리는 서버가 {@link #working}으로 일하는 동안 몇 초에 한 번, 작게 낸다 (근처 모두가 듣는다).
 * 크기는 설정 {@code sounds.machineVolume}으로 줄이거나 끈다 (보스 소리는 그대로).
 */
public final class ModSounds {
	private static final List<SoundEvent> ALL = new ArrayList<>();

	// 기계
	public static final SoundEvent CRUSHER_GRIND = register("machine.crusher");
	public static final SoundEvent PRESS_THUMP = register("machine.press");
	public static final SoundEvent ASSEMBLER_WORK = register("machine.assembler");
	public static final SoundEvent PRECISION_SPARK = register("machine.precision");
	public static final SoundEvent WASHER_SPLASH = register("machine.washer");
	/** 보일러 · 대형 보일러 · 터빈 · 고압 정련기의 증기 새는 소리. */
	public static final SoundEvent STEAM_HISS = register("machine.steam");
	public static final SoundEvent QUARRY_DIG = register("machine.quarry");
	// 보스
	/** 녹슨 수호자: 돌진 방향을 잠글 때 (녹슨 관절이 갈리는 소리). */
	public static final SoundEvent GUARDIAN_CHARGE = register("boss.guardian.charge");
	/** 용광로 파수꾼: 증기 분출 방향을 잠글 때 (숨을 들이켜는 소리). */
	public static final SoundEvent WARDEN_BURST = register("boss.warden.burst");
	/** 보스 체력이 절반 아래로 (화남). */
	public static final SoundEvent BOSS_PHASE = register("boss.phase");
	public static final SoundEvent GUARDIAN_DEATH = register("boss.guardian.death");
	public static final SoundEvent WARDEN_DEATH = register("boss.warden.death");
	/** 공허 거상: 끌어당기기 예고 (공허가 차오르는 소리). */
	public static final SoundEvent COLOSSUS_PULL = register("boss.colossus.pull");
	/** 공허 거상: 공허 수정을 띄울 때. */
	public static final SoundEvent COLOSSUS_CRYSTALS = register("boss.colossus.crystals");
	/** 공허 거상: 체력이 절반 아래로 (화남, 다른 보스의 {@link #BOSS_PHASE} 대신). */
	public static final SoundEvent COLOSSUS_RAGE = register("boss.colossus.rage");
	public static final SoundEvent COLOSSUS_DEATH = register("boss.colossus.death");

	/** 서버에서 낸 기계 소리 수 (시험용). */
	private static int playedCount;

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, Sanupcraft.id(name), SoundEvent.createVariableRangeEvent(Sanupcraft.id(name)));
		ALL.add(event);
		return event;
	}

	/** 이 모드의 소리 전부 (시험용). */
	public static List<SoundEvent> all() {
		return List.copyOf(ALL);
	}

	public static int playedCount() {
		return playedCount;
	}

	/** 기계 종류의 작동음. 없으면 null (합금로 · 심장 제련로는 화로처럼 불 소리만). */
	public static @Nullable SoundEvent forMachine(MachineKind kind) {
		return switch (kind) {
			case CRUSHER -> CRUSHER_GRIND;
			case PRESS -> PRESS_THUMP;
			case ASSEMBLER -> ASSEMBLER_WORK;
			case PRECISION -> PRECISION_SPARK;
			case WASHER -> WASHER_SPLASH;
			case REFINER -> STEAM_HISS;
			default -> null;
		};
	}

	/** 일하는 기계가 매 틱 부른다. 종류에 맞는 소리를 40틱에 한 번쯤. */
	public static void machine(ServerLevel level, BlockPos pos, MachineKind kind) {
		working(level, pos, forMachine(kind), kind == MachineKind.CRUSHER ? 30 : 40, 0.35F);
	}

	/**
	 * 일하는 동안 매 틱 부르면 {@code interval} 틱에 한 번 소리를 낸다. 자리마다 때를 어긋나게 해서 기계가 여럿이어도 한꺼번에 울리지 않는다.
	 * 높이는 0.9 ~ 1.1로 조금씩 흔든다.
	 */
	public static void working(ServerLevel level, BlockPos pos, @Nullable SoundEvent sound, int interval, float volume) {
		if (sound == null) {
			return;
		}
		float scaled = volume * (float) SanupcraftConfig.current().machineSoundVolume;
		if (scaled <= 0.0F || (level.getGameTime() + Math.floorMod(pos.hashCode(), interval)) % interval != 0) {
			return;
		}
		level.playSound(null, pos, sound, SoundSource.BLOCKS, scaled, 0.9F + level.getRandom().nextFloat() * 0.2F);
		playedCount++;
	}

	/** 보스가 화날 때 (체력 절반 아래): 으르렁 + 연기 · 보스 색 입자를 한 번. */
	public static void bossPhase(ServerLevel level, Entity boss, ParticleOptions particle) {
		bossPhase(level, boss, BOSS_PHASE, particle);
	}

	/** 화남 소리를 보스가 따로 고른다 (공허 거상). */
	public static void bossPhase(ServerLevel level, Entity boss, SoundEvent sound, ParticleOptions particle) {
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), sound, SoundSource.HOSTILE, 2.0F, 0.8F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + boss.getBbHeight() * 0.6, boss.getZ(), 30, 0.8, 0.8, 0.8, 0.03);
		level.sendParticles(particle, boss.getX(), boss.getY() + boss.getBbHeight() * 0.5, boss.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
	}

	public static void init() {
		// 정적 필드를 불러 등록만 한다
	}
}
