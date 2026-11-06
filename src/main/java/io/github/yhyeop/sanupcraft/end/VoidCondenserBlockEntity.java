package io.github.yhyeop.sanupcraft.end;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamOutlet;
import io.github.yhyeop.sanupcraft.power.SteamStorage;

/**
 * 공허 응축기 (M6). 엔드에서, 바로 아래부터 월드 최저 높이까지 전부 공기(공허)일 때만 고압 증기를 틱당 8 (설정 {@code voidCondenser.steamPerTick}).
 * 연료 · 물이 필요 없다. 고압 파이프로 고압 기계(정밀 조립기 · 터빈)에 나간다. 아래가 비었는지는 {@link #CHECK_INTERVAL}틱마다 다시 본다.
 */
public class VoidCondenserBlockEntity extends BlockEntity implements SteamStorage {
	public static final int STEAM_CAPACITY = 2000;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int STEAM_PER_TICK = 8;
	private static final int CHECK_INTERVAL = 20;

	public enum Status {
		NOT_END, NOT_VOID, RUNNING
	}

	private int steam;
	private boolean voidBelow;
	private int checkIn;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private final SteamOutlet outlet = new SteamOutlet(true);

	public VoidCondenserBlockEntity(BlockPos pos, BlockState state) {
		super(ModEnd.VOID_CONDENSER_BLOCK_ENTITY, pos, state);
	}

	public static int steamPerTick() {
		return SanupcraftConfig.current().voidCondenserSteamPerTick;
	}

	public static boolean isEnd(Level level) {
		return level.dimension() == Level.END;
	}

	/** 바로 아래부터 최저 높이까지 전부 공기인가. */
	public static boolean isVoidBelow(Level level, BlockPos pos) {
		BlockPos.MutableBlockPos cursor = pos.mutable();
		for (int y = pos.getY() - 1; y >= level.getMinY(); y--) {
			if (!level.getBlockState(cursor.setY(y)).isAir()) {
				return false;
			}
		}
		return true;
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, VoidCondenserBlockEntity condenser) {
		boolean running = condenser.work(level, pos, isEnd(level)) > 0;
		if (state.getValue(VoidCondenserBlock.LIT) != running) {
			level.setBlockAndUpdate(pos, state.setValue(VoidCondenserBlock.LIT, running));
		}
	}

	/** 한 틱 일한다. {@code inEnd}가 거짓이면 만들지 않는다 (시험은 오버월드에서 참으로 부른다). 만든 고압 증기를 돌려준다. */
	public int work(ServerLevel level, BlockPos pos, boolean inEnd) {
		int before = steam;
		if (--checkIn <= 0) {
			voidBelow = isVoidBelow(level, pos);
			checkIn = CHECK_INTERVAL;
		}
		int made = inEnd && voidBelow ? Math.min(STEAM_CAPACITY - steam, steamPerTick()) : 0;
		if (made > 0) {
			steam += made;
			flow.add(level.getGameTime(), made);
		}
		steam = outlet.distribute(level, pos, steam);
		if (steam != before) {
			setChanged();
		}
		return made;
	}

	/** 아래를 바로 다시 보게 한다 (시험용: 블록을 놓은 뒤). */
	public void recheck() {
		checkIn = 0;
	}

	public Status status(Level level, BlockPos pos) {
		if (!isEnd(level)) {
			return Status.NOT_END;
		}
		return isVoidBelow(level, pos) ? Status.RUNNING : Status.NOT_VOID;
	}

	public int steam() {
		return steam;
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
		steam = Math.max(0, Math.min(STEAM_CAPACITY, input.getIntOr("steam", 0)));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("steam", steam);
	}
}
