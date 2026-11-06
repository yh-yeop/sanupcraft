package io.github.yhyeop.sanupcraft.hazard;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 유독 가스 (D5). 깊은 동굴 공기 칸에 드물게 생기는 보이지 않는 블록. 부딪히는 모양도 겉모양도 없고 (그냥 지나갈 수 있고), 다른 블록이 놓이면 밀려난다.
 * 안에 서 있는 생물은 독 I + 구역질을 5초 받는다. 불 · 용암 · 횃불이 옆에 닿으면 작은 폭발이 나고 (블록 피해 없음, 피해 4) 가스는 사라지며,
 * 이어진 가스로 번진다. 설정 {@code hazards.toxicGas}가 꺼져 있으면 아무 일도 하지 않는다.
 */
public class ToxicGasBlock extends Block {
	/** 가스 안에 있을 때 걸리는 독 · 구역질 시간 (틱, 5초). */
	public static final int EFFECT_TICKS = 100;
	/** 폭발이 주는 피해. */
	public static final float BURST_DAMAGE = 4.0F;
	/** 폭발이 닿는 거리. */
	public static final double BURST_RADIUS = 2.5;
	/** 이어진 가스가 번져 터지는 간격 (틱). */
	public static final int CHAIN_DELAY = 2;

	public ToxicGasBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (level.isClientSide() || !SanupcraftConfig.current().hazardToxicGas || !(entity instanceof LivingEntity living)) {
			return;
		}
		poison(living);
	}

	/** 독 I + 구역질을 5초 건다 (남은 시간이 넉넉하면 다시 걸지 않는다). */
	public static void poison(LivingEntity living) {
		refresh(living, new MobEffectInstance(MobEffects.POISON, EFFECT_TICKS, 0));
		refresh(living, new MobEffectInstance(MobEffects.NAUSEA, EFFECT_TICKS, 0));
	}

	private static void refresh(LivingEntity living, MobEffectInstance effect) {
		MobEffectInstance have = living.getEffect(effect.getEffect());
		if (have == null || have.getDuration() < EFFECT_TICKS / 2) {
			living.addEffect(effect);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
			ignitedCheck(serverLevel, pos);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel) {
			ignitedCheck(serverLevel, pos);
		}
	}

	/** 옆에 불씨가 있으면 터뜨린다. */
	private static void ignitedCheck(ServerLevel level, BlockPos pos) {
		if (!SanupcraftConfig.current().hazardToxicGas) {
			return;
		}
		for (Direction direction : Direction.values()) {
			if (isIgnitionSource(level.getBlockState(pos.relative(direction)))) {
				burst(level, pos);
				return;
			}
		}
	}

	/** 가스에 불을 붙이는 블록: 불 · 용암 · 횃불 · 켜진 모닥불. */
	public static boolean isIgnitionSource(BlockState state) {
		return state.is(BlockTags.FIRE)
				|| state.getFluidState().is(FluidTags.LAVA)
				|| state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
				|| (state.is(BlockTags.CAMPFIRES) && state.hasProperty(BlockStateProperties.LIT)
						&& state.getValue(BlockStateProperties.LIT));
	}

	/** 터진다: 가스가 사라지고, 둘레에 피해 4 (블록 피해 없음), 이어진 가스는 잠깐 뒤 따라 터진다. */
	public static void burst(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(ModHazard.TOXIC_GAS)) {
			return;
		}
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		level.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.3, 0.3, 0.3, 0.02);
		level.sendParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.3, 0.3, 0.3, 0.01);
		level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
		AABB area = new AABB(pos).inflate(BURST_RADIUS - 0.5);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			entity.hurtServer(level, level.damageSources().explosion(null, null), BURST_DAMAGE);
		}
		for (Direction direction : Direction.values()) {
			BlockPos next = pos.relative(direction);
			if (level.getBlockState(next).is(ModHazard.TOXIC_GAS)) {
				level.scheduleTick(next, ModHazard.TOXIC_GAS, CHAIN_DELAY);
			}
		}
	}

	/** 번져 오는 불꽃: 이어진 가스가 정해진 시간 뒤 터진다. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (SanupcraftConfig.current().hazardToxicGas) {
			burst(level, pos);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(24) == 0) {
			level.addParticle(new DustParticleOptions(0x8CC63F, 0.7F),
					pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
		}
	}
}
