package io.github.yhyeop.sanupcraft.nether;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 용암 증기선 (M5): 물에서는 보통 배, 용암 위에서도 뜬다. 엔티티 종류가 불 면역이라 용암 · 불에 타지 않는다.
 * <p>
 * 바닐라 배의 뜨기 계산(물 높이 찾기 · 부력)은 비공개 메서드라 믹스인 없이는 고칠 수 없다. 그래서 용암 위에서는 바닐라가
 * "공중"으로 보고 중력만 주고 움직인 뒤({@code super.tick()}), 여기서 배 바닥이 용암 윗면 {@link #DEPTH}칸 아래에 오도록 세로 속도를 고친다
 * (다음 틱 중력을 미리 상쇄 + 거리에 비례한 힘 + 감쇠). 물 위 배보다 얕게 떠서 배 안으로 용암이 비치지 않는다.
 * 바닐라 "공중" 상태의 가로 마찰(0.9)은 물 위와 같아서 노 젓기 속도도 같다.
 * <p>
 * 탄 사람 · 동물은 타는 동안 화염 저항(2초짜리를 1초마다 갱신, 입자 없음)을 받고 불이 꺼진다. 내리면 2초 뒤 풀린다.
 */
public class LavaSteamboat extends Boat {
	/** 용암에 잠기는 배 바닥 깊이 (배 높이 0.5625 중). */
	static final double DEPTH = 0.12;
	private static final double SPRING = 0.3;
	private static final double DAMPING = 0.5;
	private static final int PASSENGER_FIRE_RESIST_TICKS = 40;

	public LavaSteamboat(EntityType<? extends Boat> type, Level level, Supplier<Item> dropItem) {
		super(type, level, dropItem);
	}

	@Override
	public void tick() {
		super.tick();
		if (isLocalInstanceAuthoritative()) {
			floatOnLava();
		}
		if (!level().isClientSide() && tickCount % 20 == 0) {
			for (Entity passenger : getPassengers()) {
				if (passenger instanceof LivingEntity living) {
					living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, PASSENGER_FIRE_RESIST_TICKS, 0, true, false));
				}
				passenger.clearFire();
			}
		}
	}

	/** 배 바닥이 용암에 잠겨 있으면 윗면 {@link #DEPTH}칸 아래로 끌어 띄운다. 반환: 용암에 떠 있나. */
	boolean floatOnLava() {
		double surface = lavaSurface();
		if (surface == -Double.MAX_VALUE) {
			return false;
		}
		double error = surface - DEPTH - getY();
		Vec3 motion = getDeltaMovement();
		double lift = Mth.clamp(error * SPRING, -0.1, 0.2);
		setDeltaMovement(motion.x, motion.y * DAMPING + getGravity() + lift, motion.z);
		resetFallDistance();
		return true;
	}

	/** 배 바닥에 닿은 용암의 윗면 높이 (배 바닥보다 높을 때만). 없으면 -Double.MAX_VALUE. */
	double lavaSurface() {
		AABB box = getBoundingBox();
		int minX = Mth.floor(box.minX);
		int maxX = Mth.ceil(box.maxX);
		int minY = Mth.floor(box.minY);
		int maxY = Mth.ceil(box.maxY);
		int minZ = Mth.floor(box.minZ);
		int maxZ = Mth.ceil(box.maxZ);
		double surface = -Double.MAX_VALUE;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					pos.set(x, y, z);
					FluidState fluid = level().getFluidState(pos);
					if (fluid.is(FluidTags.LAVA)) {
						double height = y + fluid.getHeight(level(), pos);
						if (height > box.minY) {
							surface = Math.max(surface, height);
						}
					}
				}
			}
		}
		return surface;
	}
}
