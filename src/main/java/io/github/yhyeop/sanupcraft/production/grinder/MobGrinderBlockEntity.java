package io.github.yhyeop.sanupcraft.production.grinder;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.entity.FakePlayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.boss.PartyBoss;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.fluid.FluidTank;
import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 몹 분쇄기. 증기를 파이프로 받아 (기본) 1초마다 위 칸의 몹에게 피해를 준다. 피해는 가짜 플레이어가 준 것으로 치므로
 * "플레이어가 처치해야 나오는" 전리품과 경험치가 나온다 (아이템은 그냥 떨어진다 - 깔때기 · 벨트로 받는다).
 * 플레이어 · 길들인 동물 · 이름표 붙은 몹 · 주민 · 보스는 건드리지 않는다.
 * 둘레 2칸의 경험치 구슬은 옆의 경험치 탱크로 모으고, 탱크가 없으면 안의 작은 저장고(200점)에 모은다 (빈손 우클릭으로 꺼냄).
 */
public class MobGrinderBlockEntity extends BlockEntity implements SteamConsumer, SteamStorage {
	public static final int STEAM_CAPACITY = 200;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json. */
	public static final int STEAM_PER_HIT = 10;
	public static final float DAMAGE = 6.0F;
	public static final int TICKS_PER_HIT = 20;
	/** 구슬을 모으는 반경 (칸). */
	public static final double COLLECT_RANGE = 2.0;
	/** 구슬을 살펴보는 간격 (틱). */
	public static final int COLLECT_INTERVAL = 5;
	/** 안의 경험치 저장고 (점수). */
	public static final int BUFFER_POINTS = 200;

	/** 피해를 주는 가짜 플레이어의 이름표. */
	private static final GameProfile PROFILE = new GameProfile(UUID.fromString("5a6e0c1d-7b53-4e0a-9c2f-6d3b1a8f4e21"), "[분쇄기]");

	private final FluidTank buffer = new FluidTank(BUFFER_POINTS * FluidKind.MB_PER_XP, this::setChanged);
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private int steam;
	private int timer;

	public MobGrinderBlockEntity(BlockPos pos, BlockState state) {
		super(ModGrinder.MOB_GRINDER_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, MobGrinderBlockEntity grinder) {
		if (++grinder.timer >= SanupcraftConfig.current().grinderTicksPerHit) {
			grinder.timer = 0;
			if (grinder.grind(level, pos) > 0) {
				level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.1);
			}
		}
		if (level.getGameTime() % COLLECT_INTERVAL == 0) {
			grinder.collectExperience(level, pos);
		}
	}

	/** 베어도 되는 몹인가. */
	public static boolean canGrind(LivingEntity entity) {
		if (entity instanceof Player || entity instanceof AbstractVillager || entity instanceof PartyBoss || entity instanceof WitherBoss
				|| entity instanceof EnderDragon || entity instanceof Warden || entity instanceof ArmorStand) {
			return false;
		}
		if (entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) {
			return false;
		}
		return entity.isAlive() && !entity.isInvulnerable() && !entity.hasCustomName();
	}

	/** 한 번 벤다: 위 칸의 몹마다 증기를 쓰고 피해를 준다. 벤 몹 수를 돌려준다. 시험이 직접 부른다. */
	public int grind(ServerLevel level, BlockPos pos) {
		int cost = SanupcraftConfig.current().grinderSteamPerHit;
		if (steam < cost) {
			return 0;
		}
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.above()).deflate(0.01), MobGrinderBlockEntity::canGrind);
		if (targets.isEmpty()) {
			return 0;
		}
		FakePlayer fake = FakePlayer.get(level, PROFILE);
		fake.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
		DamageSource source = level.damageSources().playerAttack(fake);
		float damage = (float) SanupcraftConfig.current().grinderDamage;
		int hit = 0;
		for (LivingEntity target : targets) {
			if (steam < cost) {
				break;
			}
			steam -= cost;
			hit++;
			Vec3 motion = target.getDeltaMovement();
			target.hurtServer(level, source, damage);
			// 밀려나지도, 가짜 플레이어를 쫓지도 않게 한다
			target.setLastHurtByMob(null);
			if (target instanceof Mob mob) {
				mob.setTarget(null);
			}
			if (target.isAlive()) {
				target.setDeltaMovement(motion);
				target.needsSync = false;
			}
		}
		setChanged();
		return hit;
	}

	/** 둘레 구슬을 모은다: 붙은 경험치 탱크 먼저, 없거나 가득이면 안의 저장고. 안에 쌓인 것도 탱크로 옮긴다. 모은 점수를 돌려준다. */
	public int collectExperience(ServerLevel level, BlockPos pos) {
		flushBuffer(level, pos);
		AABB box = new AABB(pos).inflate(COLLECT_RANGE);
		int collected = 0;
		for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
			if (orb.isRemoved()) {
				continue;
			}
			int value = orb.getValue();
			if (value > 0 && store(level, pos, value)) {
				orb.discard();
				collected += value;
			}
		}
		return collected;
	}

	/** 경험치 {@code points}점을 통째로 넣을 곳(탱크 → 저장고)에 넣는다. 못 넣으면 false. */
	private boolean store(ServerLevel level, BlockPos pos, int points) {
		int mb = points * FluidKind.MB_PER_XP;
		for (Direction direction : Direction.values()) {
			if (level.getBlockEntity(pos.relative(direction)) instanceof ExperienceTankBlockEntity tank && tank.fill(FluidKind.EXPERIENCE, mb, true) >= mb) {
				tank.fill(FluidKind.EXPERIENCE, mb, false);
				return true;
			}
		}
		if (buffer.fill(FluidKind.EXPERIENCE, mb, true) >= mb) {
			buffer.fill(FluidKind.EXPERIENCE, mb, false);
			return true;
		}
		return false;
	}

	private void flushBuffer(ServerLevel level, BlockPos pos) {
		if (buffer.fluidAmount() <= 0) {
			return;
		}
		for (Direction direction : Direction.values()) {
			if (level.getBlockEntity(pos.relative(direction)) instanceof ExperienceTankBlockEntity tank) {
				int moved = tank.fill(FluidKind.EXPERIENCE, buffer.fluidAmount(), false);
				buffer.drain(FluidKind.EXPERIENCE, moved, false);
				if (buffer.fluidAmount() <= 0) {
					return;
				}
			}
		}
	}

	/** 안의 저장고에 든 경험치 점수. */
	public int bufferPoints() {
		return buffer.fluidAmount() / FluidKind.MB_PER_XP;
	}

	/** 저장고에서 최대 {@code points}점 꺼내 돌려준다. */
	public int takeBufferPoints(int points) {
		return buffer.drain(FluidKind.EXPERIENCE, points * FluidKind.MB_PER_XP, false) / FluidKind.MB_PER_XP;
	}

	// ---- 증기망 ----

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.max(0, Math.min(amount, STEAM_CAPACITY - steam));
		steam += accepted;
		if (accepted > 0) {
			flow.add(level != null ? level.getGameTime() : 0L, accepted);
			setChanged();
		}
		return accepted;
	}

	@Override
	public int steamStored() {
		return steam;
	}

	@Override
	public int steamCapacity() {
		return STEAM_CAPACITY;
	}

	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(level != null ? level.getGameTime() : 0L);
	}

	/** 부서질 때 저장고의 경험치를 구슬로 떨군다. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel serverLevel && bufferPoints() > 0) {
			ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), takeBufferPoints(bufferPoints()));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		steam = Math.max(0, Math.min(STEAM_CAPACITY, input.getIntOr("steam", 0)));
		buffer.load(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("steam", steam);
		buffer.save(output);
	}
}
