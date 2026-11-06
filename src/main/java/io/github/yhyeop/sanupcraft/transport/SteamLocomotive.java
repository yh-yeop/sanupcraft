package io.github.yhyeop.sanupcraft.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.power.BoilerBlock;
import io.github.yhyeop.sanupcraft.power.ModPower;

/**
 * 증기 기관차. 화로 광산 수레처럼 스스로 밀고 가지만 더 빠르고, 연결 고리로 이은 칸들을 끈다.
 * 연료 칸 하나 (석탄 · 숯 3600틱, 석탄 가루 1800틱, 한 묶음까지)를 먼저 태우고, 없으면 증기 통에서 받은 증기를 틱당 {@link #STEAM_PER_TICK} 쓴다.
 * 우클릭: 연료 · 증기 통이면 넣고 출발, 빈손이면 출발 / 멈춤 + 상태. 출발 방향은 화물칸 반대쪽 (칸이 없으면 누른 사람 반대쪽).
 * 꺼진 동력 레일(정거장) 위에서는 연료를 아끼며 기다렸다가, 레일에 신호가 들어오면 다시 간다.
 * 연결된 칸 목록(UUID)은 기관차가 들고, 틱마다 앞 칸과의 간격을 용수철처럼 맞춘다 ({@link TrainCoupling}).
 */
public class SteamLocomotive extends AbstractMinecart {
	public static final int TICKS_PER_COAL = 3600;
	public static final int TICKS_PER_COAL_DUST = 1800;
	public static final int STEAM_CAPACITY = 4000;
	public static final int STEAM_PER_TICK = 2;
	/** 최고 속도 (블록/틱). 화로 수레는 0.2, 보통 수레 한도는 0.4 (뒤 칸이 따라잡을 여유를 둔다). */
	public static final double MAX_SPEED = 0.34;
	/** 연결된 칸이 이 틱 동안 안 보이면 (부서짐) 그 칸부터 뒤를 떼어 낸다. */
	private static final int MISSING_GRACE_TICKS = 40;
	private static final double PUSH = 0.1;

	private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(SteamLocomotive.class, EntityDataSerializers.BOOLEAN);

	private ItemStack fuel = ItemStack.EMPTY;
	private int burnTicks;
	private int steam;
	private boolean running;
	/** 미는 방향 (수평 단위 벡터). */
	private Vec3 push = Vec3.ZERO;
	private final List<UUID> cars = new ArrayList<>();
	private int missingTicks;

	public SteamLocomotive(EntityType<? extends SteamLocomotive> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_LIT, false);
	}

	/** 다른 수레와 부딪칠 때 화로 수레처럼 민다. */
	@Override
	public boolean isFurnace() {
		return true;
	}

	@Override
	public void tick() {
		if (!level().isClientSide()) {
			entityData.set(DATA_LIT, running && !waitingAtStation() && usePower());
		}
		super.tick();
		if (level() instanceof ServerLevel serverLevel && !isRemoved()) {
			pullCars(serverLevel);
		}
		if (level().isClientSide() && isLit() && random.nextInt(3) == 0) {
			level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.1, getZ(), 0.0, 0.05, 0.0);
			level().addParticle(ParticleTypes.CLOUD, getX(), getY() + 1.2, getZ(), 0.0, 0.08, 0.0);
		}
	}

	/** 이번 틱 동력을 쓴다: 타는 연료 → 새 연료 한 개 → 증기. 못 쓰면 false. */
	private boolean usePower() {
		if (burnTicks <= 0 && !fuel.isEmpty()) {
			burnTicks += fuelTicks(fuel);
			fuel.shrink(1);
			if (fuel.isEmpty()) {
				fuel = ItemStack.EMPTY;
			}
		}
		if (burnTicks > 0) {
			burnTicks--;
			return true;
		}
		if (steam >= STEAM_PER_TICK) {
			steam -= STEAM_PER_TICK;
			return true;
		}
		return false;
	}

	/** 기관차 연료 한 개가 타는 틱. 연료가 아니면 0. */
	public static int fuelTicks(ItemStack stack) {
		if (stack.is(ModItems.COAL_DUST)) {
			return TICKS_PER_COAL_DUST;
		}
		return stack.is(ItemTags.FURNACE_MINECART_FUEL) ? TICKS_PER_COAL : 0;
	}

	/** 꺼진 동력 레일 위: 정거장에서 기다린다. */
	public boolean waitingAtStation() {
		BlockState rail = level().getBlockState(getCurrentBlockPosOrRailBelow());
		return rail.is(Blocks.POWERED_RAIL) && !rail.getValue(PoweredRailBlock.POWERED);
	}

	@Override
	protected double getMaxSpeed(ServerLevel level) {
		return Math.min(super.getMaxSpeed(level), SanupcraftConfig.current().locomotiveMaxSpeed);
	}

	@Override
	protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
		Vec3 next;
		if (isLit() && push.lengthSqr() > 1.0E-7) {
			if (deltaMovement.horizontalDistanceSqr() > 0.001) {
				// 화로 수레처럼: 미는 방향을 지금 가는 방향(레일)에 맞춘다
				Vec3 along = push.projectedOn(deltaMovement);
				if (along.lengthSqr() > 1.0E-7) {
					push = along.normalize();
				}
			}
			next = deltaMovement.multiply(0.8, 0.0, 0.8).add(push.scale(PUSH));
			if (isInWater()) {
				next = next.scale(0.1);
			}
		} else if (!running) {
			// 멈춤: 브레이크를 건다
			next = deltaMovement.multiply(0.7, 0.0, 0.7);
		} else {
			next = deltaMovement.multiply(0.98, 0.0, 0.98);
		}
		return super.applyNaturalSlowdown(next);
	}

	// ---- 연결된 칸 ----

	private void pullCars(ServerLevel level) {
		Entity front = this;
		for (int i = 0; i < cars.size(); i++) {
			Entity found = level.getEntity(cars.get(i));
			if (found == null) {
				// 막 불러온 청크라 아직 없을 수도 있다: 잠깐 기다렸다가 떼어 낸다
				if (++missingTicks > MISSING_GRACE_TICKS) {
					detachFrom(i);
				}
				return;
			}
			if (!(found instanceof AbstractMinecart car) || !car.isAlive() || car.distanceTo(front) > TrainCoupling.BREAK_DISTANCE) {
				detachFrom(i);
				return;
			}
			TrainCoupling.follow(car, front);
			front = car;
		}
		missingTicks = 0;
	}

	/** {@code index}번째 칸부터 뒤를 모두 떼어 낸다. 떼어 낸 칸 수를 돌려준다. */
	public int detachFrom(int index) {
		int removed = 0;
		while (cars.size() > index) {
			cars.remove(cars.size() - 1);
			removed++;
		}
		missingTicks = 0;
		return removed;
	}

	/** 연결된 칸 (앞에서부터, UUID). */
	public List<UUID> cars() {
		return cars;
	}

	/** 맨 뒤에 칸을 붙인다. */
	public void attach(AbstractMinecart car) {
		cars.add(car.getUUID());
	}

	/** 맨 뒤 칸 (없으면 기관차 자신). 찾지 못하면 null. */
	public @Nullable Entity tail(ServerLevel level) {
		return cars.isEmpty() ? this : level.getEntity(cars.get(cars.size() - 1));
	}

	// ---- 운전 ----

	/** {@code direction} 쪽으로 출발한다 (수평 성분만 쓴다). */
	public void start(Vec3 direction) {
		Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
		if (flat.lengthSqr() < 1.0E-6) {
			flat = Vec3.atLowerCornerOf(getMotionDirection().getUnitVec3i());
		}
		push = flat.normalize();
		running = true;
	}

	public void stop() {
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	/** 지금 동력을 써서 가는 중인가 (클라이언트도 안다: 연기 · 불 켜진 모습). */
	public boolean isLit() {
		return entityData.get(DATA_LIT);
	}

	/** 출발 방향: 첫 화물칸 반대쪽, 칸이 없거나 안 보이면 {@code from} 반대쪽. */
	private Vec3 startDirection(ServerLevel level, Vec3 from) {
		Entity first = cars.isEmpty() ? null : level.getEntity(cars.get(0));
		return position().subtract(first != null ? first.position() : from);
	}

	/** 연료를 넣는다 (같은 것끼리 한 묶음까지). 넣은 개수를 돌려준다. */
	public int addFuel(ItemStack stack) {
		if (fuelTicks(stack) <= 0) {
			return 0;
		}
		if (fuel.isEmpty()) {
			fuel = stack.split(stack.getCount());
			return fuel.getCount();
		}
		if (!ItemStack.isSameItemSameComponents(fuel, stack)) {
			return 0;
		}
		int move = Math.min(stack.getCount(), fuel.getMaxStackSize() - fuel.getCount());
		fuel.grow(move);
		stack.shrink(move);
		return move;
	}

	/** 증기를 받는다. 받은 양을 돌려준다. */
	public int addSteam(int amount) {
		int accepted = Mth.clamp(amount, 0, STEAM_CAPACITY - steam);
		steam += accepted;
		return accepted;
	}

	public int steam() {
		return steam;
	}

	public int fuelCount() {
		return fuel.getCount();
	}

	public int burnTicks() {
		return burnTicks;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (!(level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (fuelTicks(held) > 0) {
			if (addFuel(held) <= 0 && fuel.getCount() < fuel.getMaxStackSize()) {
				player.sendOverlayMessage(Component.translatable("entity.sanupcraft.steam_locomotive.other_fuel"));
				return InteractionResult.FAIL;
			}
			if (!running) {
				start(startDirection(level, player.position()));
			}
		} else if (SteamCharge.holdsSteam(held) && SteamCharge.get(held) > 0) {
			SteamCharge.take(held, addSteam(SteamCharge.get(held)));
			if (!running) {
				start(startDirection(level, player.position()));
			}
		} else if (running) {
			stop();
		} else {
			start(startDirection(level, player.position()));
		}
		player.sendOverlayMessage(status());
		return InteractionResult.SUCCESS;
	}

	/** 상태 한 줄: 운행 · 연료 · 증기 · 칸 수. */
	public Component status() {
		String state = !running ? "stopped" : waitingAtStation() ? "waiting" : hasPower() ? "running" : "no_power";
		return Component.translatable("entity.sanupcraft.steam_locomotive.status",
				Component.translatable("entity.sanupcraft.steam_locomotive.state." + state),
				fuel.getCount(), burnTicks / 20, steam, STEAM_CAPACITY, cars.size());
	}

	private boolean hasPower() {
		return burnTicks > 0 || !fuel.isEmpty() || steam >= STEAM_PER_TICK;
	}

	@Override
	protected Item getDropItem() {
		return ModTransport.STEAM_LOCOMOTIVE_ITEM;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(ModTransport.STEAM_LOCOMOTIVE_ITEM);
	}

	@Override
	protected void destroy(ServerLevel level, DamageSource source) {
		super.destroy(level, source);
		if (!fuel.isEmpty()) {
			spawnAtLocation(level, fuel.copy());
			fuel = ItemStack.EMPTY;
		}
	}

	/** 수레 안에 보일러가 앉아 있다 (가는 중이면 불 켜진 모습). */
	@Override
	public BlockState getDefaultDisplayBlockState() {
		return ModPower.BOILER.defaultBlockState().setValue(BoilerBlock.FACING, Direction.NORTH).setValue(BoilerBlock.LIT, isLit());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("fuel", ItemStack.CODEC, fuel.isEmpty() ? null : fuel);
		output.putInt("burn_ticks", burnTicks);
		output.putInt("steam", steam);
		output.putBoolean("running", running);
		output.putDouble("push_x", push.x);
		output.putDouble("push_z", push.z);
		output.store("cars", UUIDUtil.CODEC.listOf(), List.copyOf(cars));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		fuel = input.read("fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		burnTicks = input.getIntOr("burn_ticks", 0);
		steam = input.getIntOr("steam", 0);
		running = input.getBooleanOr("running", false);
		push = new Vec3(input.getDoubleOr("push_x", 0.0), 0.0, input.getDoubleOr("push_z", 0.0));
		cars.clear();
		input.read("cars", UUIDUtil.CODEC.listOf()).ifPresent(cars::addAll);
	}
}
