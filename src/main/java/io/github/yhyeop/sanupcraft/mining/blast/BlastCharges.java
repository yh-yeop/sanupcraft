package io.github.yhyeop.sanupcraft.mining.blast;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 발파 장약이 터지는 일. 장약 자리를 가운데로 5x5x5 정육면체를 다이아 곡괭이로 캔 것처럼 아이템으로 떨어뜨리고 지운다.
 * 건드리지 않는 것: 블록 엔티티가 있는 블록 (우리 기계 · 상자 · 화로 …), 부술 수 없는 블록 (기반암 · 강화 심층암 …), 액체.
 * 다른 장약은 부수지 않고 짧은 심지로 이어 터뜨린다. 바닐라 폭발은 쓰지 않는다 (블록 피해 없음, 입자와 소리만).
 */
public final class BlastCharges {
	/** 가운데에서 한쪽으로 뻗는 칸 수 (2 = 5칸). */
	public static final int HALF_SIZE = 2;
	/** 둘레 몹이 피해 · 밀려남을 받는 거리. */
	public static final double ENTITY_RADIUS = 5.0;
	/** 이어 터지는 장약의 심지 (틱). */
	public static final int CHAIN_FUSE = 5;

	private BlastCharges() {
	}

	/** 이 블록을 발파가 캘 수 있나. */
	public static boolean canMine(ServerLevel level, BlockPos pos, BlockState state) {
		return !state.isAir()
				&& !state.hasBlockEntity()
				&& !(state.getBlock() instanceof LiquidBlock)
				&& !state.is(BlockTags.WITHER_IMMUNE)
				&& state.getDestroySpeed(level, pos) >= 0.0F;
	}

	/** {@code center}의 장약을 터뜨린다. 캔 블록 수를 돌려준다. */
	public static int detonate(ServerLevel level, BlockPos center) {
		Vec3 middle = Vec3.atCenterOf(center);
		// 장약 자신을 먼저 지운다 (이 칸에서 다시 터지지 않게)
		level.setBlock(center, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		List<ItemStack> drops = new ArrayList<>();
		int mined = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-HALF_SIZE, -HALF_SIZE, -HALF_SIZE), center.offset(HALF_SIZE, HALF_SIZE, HALF_SIZE))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlast.BLAST_CHARGE)) {
				BlastChargeBlock.ignite(level, pos.immutable(), CHAIN_FUSE);
				continue;
			}
			if (!canMine(level, pos, state)) {
				continue;
			}
			for (ItemStack drop : Block.getDrops(state, level, pos, null, null, pickaxe)) {
				merge(drops, drop);
			}
			state.spawnAfterBreak(level, pos, pickaxe, true);
			level.levelEvent(2001, pos, Block.getId(state));
			// 물에 잠겨 있던 블록은 물이 남는다
			level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
			mined++;
		}
		for (ItemStack drop : drops) {
			Block.popResource(level, center, drop);
		}

		hurtAround(level, middle);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, middle.x, middle.y, middle.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.POOF, middle.x, middle.y, middle.z, 30, 1.5, 1.5, 1.5, 0.05);
		level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0F, 0.9F);
		ModBlast.awardNearby(level, middle);
		return mined;
	}

	/** 둘레 살아 있는 것에 약한 피해와 밀려남 (블록 피해는 없다). */
	private static void hurtAround(ServerLevel level, Vec3 middle) {
		float damage = (float) SanupcraftConfig.current().blastEntityDamage;
		AABB area = new AABB(middle, middle).inflate(ENTITY_RADIUS);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			double distance = entity.position().distanceTo(middle);
			if (distance > ENTITY_RADIUS) {
				continue;
			}
			if (damage > 0.0F) {
				entity.hurtServer(level, level.damageSources().explosion(null, null), damage);
			}
			Vec3 away = entity.getEyePosition().subtract(middle);
			Vec3 push = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : away.normalize();
			double strength = 1.0 - distance / ENTITY_RADIUS * 0.5;
			entity.push(push.x * strength, 0.3 + push.y * strength * 0.5, push.z * strength);
		}
	}

	/** 같은 아이템끼리 한 더미로 합쳐 담는다. */
	private static void merge(List<ItemStack> stacks, ItemStack add) {
		ItemStack rest = add.copy();
		for (ItemStack have : stacks) {
			if (rest.isEmpty()) {
				return;
			}
			if (ItemStack.isSameItemSameComponents(have, rest) && have.getCount() < have.getMaxStackSize()) {
				int move = Math.min(rest.getCount(), have.getMaxStackSize() - have.getCount());
				have.grow(move);
				rest.shrink(move);
			}
		}
		if (!rest.isEmpty()) {
			stacks.add(rest);
		}
	}
}
