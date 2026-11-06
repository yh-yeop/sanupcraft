package io.github.yhyeop.sanupcraft.transport;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 열차 연결. 기관차가 뒤에 이은 칸 목록을 들고, 칸마다 바로 앞 칸과의 간격을 용수철처럼 맞춘다.
 * 칸은 아무 광산 수레나 된다 (화물칸 · 보통 수레 · 상자 수레). 기관차끼리는 잇지 않는다.
 * 잇기: 연결 고리를 들고 (화물칸은 웅크리고 빈손으로도) 칸을 누르면 {@link #COUPLE_RANGE} 안의 가장 가까운 열차 꼬리 뒤에 붙는다.
 * 이미 이어진 칸을 누르면 그 칸부터 뒤를 떼어 낸다.
 */
public final class TrainCoupling {
	/** 칸 사이 간격 (수레 중심 사이, 블록). 수레끼리 밀어내는 거리(약 1.2)보다 조금 멀다. */
	public static final double SPACING = 1.6;
	/** 간격이 벌어지거나 좁아진 만큼 속도를 더하는 비율. */
	public static final double SPRING = 0.4;
	/** 칸이 이보다 멀어지면 (탈선 · 막힘) 연결이 끊어진다. */
	public static final double BREAK_DISTANCE = 6.0;
	/** 누른 칸에서 이 거리 안의 열차 꼬리에 붙는다. */
	public static final double COUPLE_RANGE = 3.0;
	/** 한 열차의 최대 칸 수. */
	public static final int MAX_CARS = 16;
	/** 칸 속도 한도 (보통 수레 최고 속도). */
	private static final double CAR_MAX_SPEED = 0.4;
	/** 기관차를 찾는 범위 (칸 수 x 간격보다 넉넉히). */
	private static final double SEARCH_RANGE = MAX_CARS * SPACING + 8.0;

	private TrainCoupling() {
	}

	/** {@code car}를 앞 칸 {@code front}에 맞춰 움직인다: 앞 칸 속도 + 간격 차이 x 용수철. */
	public static void follow(AbstractMinecart car, Entity front) {
		double dx = front.getX() - car.getX();
		double dz = front.getZ() - car.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 1.0E-4) {
			return;
		}
		dx /= dist;
		dz /= dist;
		Vec3 frontVelocity = velocity(front);
		double along = frontVelocity.x * dx + frontVelocity.z * dz;
		double speed = Mth.clamp(along + SPRING * (dist - SPACING), -CAR_MAX_SPEED, CAR_MAX_SPEED);
		car.setDeltaMovement(dx * speed, car.getDeltaMovement().y, dz * speed);
	}

	/** 앞 칸의 실제 속도 (기관차는 최고 속도로 자른다: 속도 값은 그보다 클 수 있다). */
	private static Vec3 velocity(Entity entity) {
		Vec3 v = entity.getDeltaMovement();
		double limit = entity instanceof SteamLocomotive ? SanupcraftConfig.current().locomotiveMaxSpeed : CAR_MAX_SPEED;
		return new Vec3(Mth.clamp(v.x, -limit, limit), 0.0, Mth.clamp(v.z, -limit, limit));
	}

	/** {@code car}가 이어진 기관차. 없으면 null. */
	public static @Nullable SteamLocomotive trainOf(ServerLevel level, AbstractMinecart car) {
		if (car instanceof SteamLocomotive locomotive) {
			return locomotive;
		}
		List<SteamLocomotive> near = level.getEntitiesOfClass(SteamLocomotive.class, car.getBoundingBox().inflate(SEARCH_RANGE));
		for (SteamLocomotive locomotive : near) {
			if (locomotive.cars().contains(car.getUUID())) {
				return locomotive;
			}
		}
		return null;
	}

	/**
	 * 칸을 누름: 이어져 있으면 그 칸부터 뒤를 떼고, 아니면 가장 가까운 열차 꼬리 뒤에 붙인다. 결과 문구를 플레이어에게 보낸다.
	 * 붙였으면 그 기관차, 아니면 null.
	 */
	public static @Nullable SteamLocomotive toggle(ServerLevel level, Player player, AbstractMinecart car) {
		if (car instanceof SteamLocomotive locomotive) {
			player.sendOverlayMessage(Component.translatable("entity.sanupcraft.coupler.locomotive", locomotive.cars().size()));
			return null;
		}
		SteamLocomotive current = trainOf(level, car);
		if (current != null) {
			int removed = current.detachFrom(current.cars().indexOf(car.getUUID()));
			player.sendOverlayMessage(Component.translatable("entity.sanupcraft.coupler.detached", removed));
			return null;
		}
		SteamLocomotive target = couple(level, car);
		player.sendOverlayMessage(target != null
				? Component.translatable("entity.sanupcraft.coupler.attached", target.cars().size())
				: Component.translatable("entity.sanupcraft.coupler.none", (int) COUPLE_RANGE));
		return target;
	}

	/** 가장 가까운 열차 꼬리 ({@link #COUPLE_RANGE} 안) 뒤에 {@code car}를 붙인다. 붙인 기관차, 없으면 null. */
	public static @Nullable SteamLocomotive couple(ServerLevel level, AbstractMinecart car) {
		SteamLocomotive best = null;
		double bestDistance = COUPLE_RANGE;
		for (SteamLocomotive locomotive : level.getEntitiesOfClass(SteamLocomotive.class, car.getBoundingBox().inflate(SEARCH_RANGE))) {
			if (locomotive.cars().size() >= MAX_CARS) {
				continue;
			}
			Entity tail = locomotive.tail(level);
			if (tail == null || tail == car) {
				continue;
			}
			double distance = tail.distanceTo(car);
			if (distance <= bestDistance) {
				best = locomotive;
				bestDistance = distance;
			}
		}
		if (best != null) {
			best.attach(car);
		}
		return best;
	}
}
