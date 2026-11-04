package io.github.yhyeop.sanupcraft.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.power.SteamConsumer;

/** 벨트 모터: 증기를 받아 붙어 있는 벨트 줄(최대 32칸)을 돌린다. 줄 위에 올라탄 것이 없으면 증기를 쓰지 않는다. */
public class BeltMotorBlockEntity extends BlockEntity implements SteamConsumer {
	public static final int STEAM_CAPACITY = 200;
	public static final int MAX_BELTS = 32;
	/** 이만큼 틱마다 한 번 벨트를 켠다. 그때 STEAM_PER_PULSE를 쓴다 (평균 틱당 1). */
	private static final int PULSE_TICKS = 10;
	private static final int STEAM_PER_PULSE = 10;

	private int steam;
	private int belts;

	public BeltMotorBlockEntity(BlockPos pos, BlockState state) {
		super(ModLogistics.BELT_MOTOR_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BeltMotorBlockEntity motor) {
		if (level.getGameTime() % PULSE_TICKS != 0) {
			return;
		}
		boolean running = motor.steam >= STEAM_PER_PULSE;
		if (running) {
			List<BeltBlockEntity> line = motor.collectBelts(level, pos);
			motor.belts = line.size();
			// 줄 위에 올라탄 것이 하나도 없으면 증기를 쓰지 않고 벨트도 돌리지 않는다
			if (hasRider(level, line)) {
				motor.steam -= STEAM_PER_PULSE;
				long until = level.getGameTime() + PULSE_TICKS * 2L;
				for (BeltBlockEntity belt : line) {
					belt.power(until);
				}
			}
		} else {
			motor.belts = 0;
		}
		if (state.getValue(BeltMotorBlock.POWERED) != running) {
			level.setBlockAndUpdate(pos, state.setValue(BeltMotorBlock.POWERED, running));
		}
		setChanged(level, pos, state);
	}

	private static boolean hasRider(ServerLevel level, List<BeltBlockEntity> line) {
		for (BeltBlockEntity belt : line) {
			if (BeltBlockEntity.hasRider(level, belt.getBlockPos())) {
				return true;
			}
		}
		return false;
	}

	/** 모터에 닿은 벨트에서 시작해 옆으로 이어진 벨트(최대 {@link #MAX_BELTS}칸)를 모은다. */
	private List<BeltBlockEntity> collectBelts(ServerLevel level, BlockPos pos) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (Direction direction : Direction.values()) {
			queue.add(pos.relative(direction));
		}
		List<BeltBlockEntity> line = new ArrayList<>();
		while (!queue.isEmpty() && line.size() < MAX_BELTS) {
			BlockPos next = queue.poll();
			if (!seen.add(next) || !(level.getBlockEntity(next) instanceof BeltBlockEntity belt)) {
				continue;
			}
			line.add(belt);
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				queue.add(next.relative(direction));
			}
		}
		return line;
	}

	@Override
	public int acceptSteam(int amount) {
		int accepted = Math.min(amount, STEAM_CAPACITY - steam);
		steam += accepted;
		return accepted;
	}

	public int steam() {
		return steam;
	}

	/** 마지막 펄스 때 이어진 벨트 칸 수 (테스트 · 디버그용). 줄이 비어 안 돌렸어도 칸 수는 센다. */
	public int poweredBelts() {
		return belts;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		steam = input.getIntOr("steam", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("steam", steam);
	}
}
