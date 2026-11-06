package io.github.yhyeop.sanupcraft.boss.phase;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.boss.BossCombat;
import io.github.yhyeop.sanupcraft.effect.ModEffects;

/**
 * 보스 2페이즈 패턴이 바닥에 남기는 위험 구역 (확장 3 C4). 모습은 입자뿐이고 (렌더러 없음), 서버가 판정한다.
 * <ul>
 *   <li>녹 장판 (녹슨 수호자 녹 폭풍): 반경 {@link #PUDDLE_RADIUS} 안에 서 있으면 {@link #APPLY_INTERVAL}틱마다 녹 II.</li>
 *   <li>공허 균열 (공허 거상): 둘레 {@link #RIFT_PULL_RANGE}칸 안 플레이어를 조금씩 끌어당기고, 닿으면({@link #RIFT_TOUCH_RADIUS}) 공허 침식 II.</li>
 * </ul>
 * 대상은 {@link BossCombat#targets} (관전 · 크리에이티브 제외). 막는 장비는 {@link ModEffects#inflict}가 본다.
 * 수명이 다하거나, 주인 보스가 없어지면 사라진다. 잠깐 있는 것이라 저장하지 않는다.
 */
public class BossHazard extends Entity {
	public enum Kind {
		RUST_PUDDLE, VOID_RIFT
	}

	public static final float PUDDLE_RADIUS = 2.5F;
	public static final int PUDDLE_TICKS = 160;
	public static final int PUDDLE_RUST_TICKS = 100;
	public static final int PUDDLE_RUST_LEVEL = 2;
	public static final float RIFT_TOUCH_RADIUS = 1.2F;
	public static final double RIFT_PULL_RANGE = 5.0;
	/** 균열이 틱마다 더하는 속도 (칸/틱). 거상 끌어당기기(0.16)보다 훨씬 약하다. */
	public static final double RIFT_PULL_STRENGTH = 0.04;
	public static final int RIFT_TICKS = 200;
	public static final int RIFT_EROSION_TICKS = 200;
	public static final int RIFT_EROSION_LEVEL = 2;
	public static final int APPLY_INTERVAL = 10;
	/** 주인이 없는지 이 간격으로 본다. */
	private static final int OWNER_CHECK = 20;

	private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(BossHazard.class, EntityDataSerializers.BYTE);

	private int age;
	private int lifetime = PUDDLE_TICKS;
	private @Nullable UUID owner;

	public BossHazard(EntityType<? extends BossHazard> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		setNoGravity(true);
	}

	/** 위험 구역 하나를 {@code pos}(바닥 높이)에 놓는다. */
	public static BossHazard spawn(ServerLevel level, Kind kind, Vec3 pos, int lifetime, @Nullable Entity owner) {
		BossHazard hazard = new BossHazard(ModBossPhase.BOSS_HAZARD, level);
		hazard.entityData.set(DATA_KIND, (byte) kind.ordinal());
		hazard.lifetime = lifetime;
		hazard.owner = owner != null ? owner.getUUID() : null;
		hazard.setPos(pos.x, pos.y, pos.z);
		level.addFreshEntity(hazard);
		return hazard;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_KIND, (byte) 0);
	}

	public Kind kind() {
		int id = entityData.get(DATA_KIND);
		return id >= 0 && id < Kind.values().length ? Kind.values()[id] : Kind.RUST_PUDDLE;
	}

	/** 판정 반경 (장판 = 장판 크기, 균열 = 닿는 거리). */
	public float radius() {
		return kind() == Kind.RUST_PUDDLE ? PUDDLE_RADIUS : RIFT_TOUCH_RADIUS;
	}

	@Override
	public void tick() {
		super.tick();
		age++;
		if (level() instanceof ServerLevel level) {
			if (age >= lifetime || age % OWNER_CHECK == 0 && !ownerAlive(level)) {
				discard();
				return;
			}
			if (kind() == Kind.VOID_RIFT) {
				pull(level);
			}
			if (age % APPLY_INTERVAL == 0) {
				affect(level);
			}
		} else {
			clientParticles();
		}
	}

	private boolean ownerAlive(ServerLevel level) {
		if (owner == null) {
			return true;
		}
		Entity entity = level.getEntity(owner);
		return entity != null && entity.isAlive();
	}

	/** 구역 안 대상에게 효과를 건다 (장판 = 녹 II, 균열 = 공허 침식 II). 건 수를 돌려준다. 시험이 직접 부른다. */
	public int affect(ServerLevel level) {
		int count = 0;
		Entity source = owner != null ? level.getEntity(owner) : null;
		for (Player player : inside(level, radius())) {
			boolean hit = kind() == Kind.RUST_PUDDLE
					? ModEffects.inflict(player, ModEffects.RUST, PUDDLE_RUST_TICKS, PUDDLE_RUST_LEVEL, source)
					: ModEffects.inflict(player, ModEffects.VOID_EROSION, RIFT_EROSION_TICKS, RIFT_EROSION_LEVEL, source);
			if (hit) {
				count++;
			}
		}
		return count;
	}

	/** 균열: 둘레 플레이어를 가운데로 조금 끌어당긴다. 당긴 수를 돌려준다. */
	public int pull(ServerLevel level) {
		int pulled = 0;
		for (Player player : inside(level, RIFT_PULL_RANGE)) {
			Vec3 to = position().subtract(player.position());
			Vec3 flat = new Vec3(to.x, 0.0, to.z);
			if (flat.lengthSqr() < 0.25) {
				continue;
			}
			Vec3 push = flat.normalize().scale(RIFT_PULL_STRENGTH);
			player.push(push.x, 0.0, push.z);
			pulled++;
		}
		return pulled;
	}

	/** 수평 거리 {@code range} 안, 높이 −1 ~ +2.5칸 안 대상. */
	private List<Player> inside(ServerLevel level, double range) {
		AABB box = new AABB(getX() - range, getY() - 1.0, getZ() - range, getX() + range, getY() + 2.5, getZ() + range);
		return BossCombat.targets(level, box, p -> p.isAlive() && BossCombat.horizontalDistance(p.position(), position()) <= range);
	}

	private void clientParticles() {
		Level level = level();
		if (kind() == Kind.RUST_PUDDLE) {
			for (int i = 0; i < 3; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				double r = Math.sqrt(random.nextDouble()) * PUDDLE_RADIUS;
				level.addParticle(new DustParticleOptions(random.nextBoolean() ? 0xA85628 : 0x6E3219, 1.4F),
						getX() + Math.cos(a) * r, getY() + 0.05, getZ() + Math.sin(a) * r, 0.0, 0.0, 0.0);
			}
			if (age % 4 == 0) {
				for (int i = 0; i < 12; i++) {
					double a = i * Math.PI * 2.0 / 12.0 + age * 0.05;
					level.addParticle(new DustParticleOptions(0xDE8442, 1.0F),
							getX() + Math.cos(a) * PUDDLE_RADIUS, getY() + 0.1, getZ() + Math.sin(a) * PUDDLE_RADIUS, 0.0, 0.0, 0.0);
				}
			}
		} else {
			// 세로로 찢어진 틈: 가운데 기둥 + 둘레로 빨려 드는 입자
			for (int i = 0; i < 4; i++) {
				level.addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + random.nextDouble() * 2.6,
						getZ() + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.0, 0.0);
			}
			level.addParticle(new DustParticleOptions(0x2A0F3F, 1.8F), getX() + (random.nextDouble() - 0.5) * 0.4, getY() + random.nextDouble() * 2.4,
					getZ() + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
			double a = random.nextDouble() * Math.PI * 2.0;
			double r = RIFT_PULL_RANGE * (0.4 + random.nextDouble() * 0.4);
			// 엔더 입자는 (자리 + 속도)에서 나와 자리로 모인다 → 둘레에서 가운데로 빨려 든다
			level.addParticle(ParticleTypes.PORTAL, getX(), getY() + 1.2, getZ(), Math.cos(a) * r, random.nextDouble() - 0.5, Math.sin(a) * r);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	/** 잠깐 있는 구역이라 저장하지 않는다 (청크가 내려가면 사라진다). */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
