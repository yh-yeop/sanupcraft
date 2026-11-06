package io.github.yhyeop.sanupcraft.fluid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.SteamConsumer;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 펌프: 증기로 돈다. {@link #TICKS_PER_BUCKET}틱마다 증기 {@link #STEAM_PER_BUCKET}을 써서, 바로 아래부터 가까운 순으로
 * 반경 {@link #RADIUS} 안(같은 높이 ~ 아래 RADIUS칸)의 물 · 용암 원천 하나를 빨아(블록은 없어진다) 안에 1양동이를 채운다 (최대 8양동이).
 * 모은 유체는 틱마다 옆의 탱크 · 보일러(물)로 내보낸다. 무한 물(원천 둘 사이)은 바닐라가 다시 채우니 계속 퍼 올린다.
 */
public class PumpBlockEntity extends BlockEntity implements SteamConsumer, SteamStorage, FluidHolder {
	public static final int STEAM_CAPACITY = 200;
	public static final int CAPACITY = 8 * FluidKind.BUCKET;
	/** 기본값. 실제로 쓰는 값은 config/sanupcraft.json ({@code pump}). */
	public static final int RADIUS = 5;
	public static final int STEAM_PER_BUCKET = 40;
	public static final int TICKS_PER_BUCKET = 20;
	/** 틱마다 옆 블록 하나에 내보내는 최대 양. */
	public static final int PUSH_PER_TICK = 250;

	/** 펌프 상태 (빈손 우클릭 표시용). */
	public enum Status {
		FULL, NO_STEAM, NO_SOURCE, RUNNING
	}

	private static final Map<Integer, List<BlockPos>> OFFSETS = new ConcurrentHashMap<>();

	private final FluidTank tank = new FluidTank(CAPACITY, this::setChanged);
	private int steam;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private int cooldown;

	public PumpBlockEntity(BlockPos pos, BlockState state) {
		super(ModFluid.PUMP_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
		if (--pump.cooldown <= 0) {
			pump.cooldown = SanupcraftConfig.current().pumpTicksPerBucket;
			pump.pumpOnce(level, pos);
		}
		pump.push(level, pos);
	}

	public static int steamPerBucket() {
		return SanupcraftConfig.current().pumpSteamPerBucket;
	}

	/** 원천 하나를 빨아들인다. 됐으면 true. 시험이 직접 부른다. */
	public boolean pumpOnce(Level level, BlockPos pos) {
		int cost = steamPerBucket();
		if (tank.space() < FluidKind.BUCKET || steam < cost) {
			return false;
		}
		BlockPos source = findSource(level, pos);
		if (source == null) {
			return false;
		}
		BlockState state = level.getBlockState(source);
		if (!(state.getBlock() instanceof BucketPickup pickup)) {
			return false;
		}
		ItemStack bucket = pickup.pickupBlock(null, level, source, state);
		FluidKind kind = FluidKind.ofBucket(bucket);
		if (kind == null) {
			return false;
		}
		tank.fill(kind, FluidKind.BUCKET, false);
		steam -= cost;
		setChanged();
		return true;
	}

	/** 빨아들일 원천: 바로 아래부터 가까운 순. 안의 유체와 다른 것 · 양동이로 못 뜨는 것(다시마 등) · 안 불러온 곳은 건너뛴다. */
	public @Nullable BlockPos findSource(Level level, BlockPos pos) {
		for (BlockPos offset : offsets(SanupcraftConfig.current().pumpRadius)) {
			BlockPos at = pos.offset(offset);
			if (!level.isLoaded(at)) {
				continue;
			}
			FluidState fluid = level.getFluidState(at);
			if (!fluid.isSource()) {
				continue;
			}
			FluidKind kind = FluidKind.of(fluid);
			if (kind != null && tank.accepts(kind) && level.getBlockState(at).getBlock() instanceof BucketPickup) {
				return at;
			}
		}
		return null;
	}

	/** 같은 높이 ~ 아래 {@code radius}칸, 가로 반경 {@code radius} 원 안의 칸들을 바로 아래 칸에서 가까운 순으로. */
	public static List<BlockPos> offsets(int radius) {
		return OFFSETS.computeIfAbsent(radius, r -> {
			List<BlockPos> list = new ArrayList<>();
			for (int dy = 0; dy >= -r; dy--) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if ((dx != 0 || dy != 0 || dz != 0) && dx * dx + dz * dz <= r * r) {
							list.add(new BlockPos(dx, dy, dz));
						}
					}
				}
			}
			list.sort(Comparator.comparingInt((BlockPos o) -> o.getX() * o.getX() + o.getZ() * o.getZ() + (o.getY() + 1) * (o.getY() + 1))
					.thenComparingInt(BlockPos::getY));
			return List.copyOf(list);
		});
	}

	/** 안의 유체를 옆의 탱크 · 보일러로 내보낸다 (다른 펌프로는 안 보낸다). 내보낸 양을 돌려준다. */
	public int push(Level level, BlockPos pos) {
		int moved = 0;
		for (Direction direction : Direction.values()) {
			FluidKind kind = tank.fluid();
			if (kind == null) {
				break;
			}
			if (level.getBlockEntity(pos.relative(direction)) instanceof FluidHolder target && !(target instanceof PumpBlockEntity)) {
				int given = target.fill(kind, Math.min(PUSH_PER_TICK, tank.fluidAmount()), false);
				if (given > 0) {
					tank.drain(kind, given, false);
					moved += given;
				}
			}
		}
		return moved;
	}

	public Status status() {
		if (tank.space() < FluidKind.BUCKET) {
			return Status.FULL;
		}
		if (steam < steamPerBucket()) {
			return Status.NO_STEAM;
		}
		return level != null && findSource(level, worldPosition) != null ? Status.RUNNING : Status.NO_SOURCE;
	}

	// ---- 유체 (펌프 안: 양동이로 퍼 낼 수는 있지만 밖에서 넣지는 않는다) ----

	@Override
	public @Nullable FluidKind fluid() {
		return tank.fluid();
	}

	@Override
	public int fluidAmount() {
		return tank.fluidAmount();
	}

	@Override
	public int fluidCapacity() {
		return CAPACITY;
	}

	@Override
	public int fill(FluidKind kind, int amount, boolean simulate) {
		return 0;
	}

	@Override
	public int drain(FluidKind kind, int amount, boolean simulate) {
		return tank.drain(kind, amount, simulate);
	}

	/** 내용을 바로 정한다 (시험). */
	public void setFluid(@Nullable FluidKind kind, int amount) {
		tank.set(kind, amount);
		setChanged();
	}

	// ---- 증기 ----

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

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tank.load(input);
		steam = Math.max(0, Math.min(STEAM_CAPACITY, input.getIntOr("steam", 0)));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		tank.save(output);
		output.putInt("steam", steam);
	}
}
