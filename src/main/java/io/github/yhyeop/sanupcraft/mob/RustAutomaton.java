package io.github.yhyeop.sanupcraft.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.effect.ModEffects;

/**
 * 녹슨 자동인형 (확장 3 C2): 오버월드 밤 · 깊은 동굴 · 녹슨 사당의 근접 몹. 좀비보다 단단하고 느리다.
 * 때리면 녹 I ({@link #RUST_TICKS}틱, 설정 {@code mobs.automatonRustTicks}). 강철 한 벌이면 막는다 ({@code Resistances}).
 * 지표(Y {@link #DEEP_Y} 위)에서는 스폰 시도 중 {@link #SURFACE_CHANCE}만 통과해 드물고, 깊은 곳에서는 그대로 나온다.
 * 녹슨 사당 안에서는 사당 구조물의 몹 목록이 자동인형뿐이다 (구조물 JSON {@code spawn_overrides}).
 */
public class RustAutomaton extends EliteMonster {
	public static final int RUST_TICKS = 100;
	public static final int RUST_LEVEL = 1;
	public static final int DEEP_Y = 0;
	public static final float SURFACE_CHANCE = 0.35F;

	public RustAutomaton(EntityType<? extends RustAutomaton> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.21)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	/** 스폰 규칙: 어두운 곳 (보통 몬스터 규칙) + 지표는 {@link #SURFACE_CHANCE}만. 스폰 알 · 명령은 그대로. */
	public static boolean checkSpawnRules(EntityType<RustAutomaton> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		return reason != EntitySpawnReason.NATURAL || pos.getY() < DEEP_Y || random.nextFloat() < SURFACE_CHANCE;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		SanupcraftConfig config = SanupcraftConfig.current();
		return hurtWithEffect(level, target, ModEffects.RUST, config.mobAutomatonRustTicks, RUST_LEVEL);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// 등의 굴뚝에서 가끔 연기
		if (level().isClientSide() && random.nextInt(12) == 0) {
			level().addParticle(ParticleTypes.SMOKE, getX() - Math.sin(yBodyRot * Math.PI / 180.0) * -0.35, getY() + 1.7,
					getZ() + Math.cos(yBodyRot * Math.PI / 180.0) * -0.35, 0.0, 0.03, 0.0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return MobSounds.AUTOMATON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MobSounds.AUTOMATON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MobSounds.AUTOMATON_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(MobSounds.AUTOMATON_STEP, 0.4F, 1.0F);
	}
}
