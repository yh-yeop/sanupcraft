package io.github.yhyeop.sanupcraft.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 지열 증기 분출구 (네더 단계). 맞닿은 용암 원천 하나마다 틱당 증기 2 (최대 4곳 = 8, 보일러 둘 몫).
 * 물도 연료도 안 쓴다. 대신 네더의 열기에서만 돈다 (오버월드 · 엔드에서는 "너무 차갑다").
 * 네더에는 물을 놓을 수 없어 보일러를 못 쓰므로, 네더 전초기지(굴착기 · 심장 제련로)의 동력이 된다.
 */
public class GeothermalVentBlockEntity extends BlockEntity implements SteamStorage {
	public static final int STEAM_CAPACITY = 1000;
	/** 기본값. 실제로 쓰는 값은 {@link #steamPerLava()} (config/sanupcraft.json). */
	public static final int STEAM_PER_LAVA = 2;
	public static final int MAX_LAVA = 4;

	/** 분출구 상태 (빈손 우클릭 표시용). */
	public enum Status {
		TOO_COLD, NO_LAVA, RUNNING
	}

	private int steam;
	private int lava;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private final SteamOutlet outlet = new SteamOutlet();

	public GeothermalVentBlockEntity(BlockPos pos, BlockState state) {
		super(ModPower.GEOTHERMAL_VENT_BLOCK_ENTITY, pos, state);
	}

	/** 용암 원천 하나당 틱당 증기 (설정 파일). */
	public static int steamPerLava() {
		return SanupcraftConfig.current().ventSteamPerLava;
	}

	/** 네더(물이 증발하는 차원)인가. 파수꾼이 깨어나는 곳과 같다. */
	public static boolean isHot(Level level) {
		return level.dimension() == Level.NETHER;
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GeothermalVentBlockEntity vent) {
		boolean running = vent.work(level, pos, isHot(level)) > 0;
		if (state.getValue(GeothermalVentBlock.LIT) != running) {
			level.setBlockAndUpdate(pos, state.setValue(GeothermalVentBlock.LIT, running));
		}
	}

	/** 한 틱 일한다. {@code hot}이 거짓이면 아무것도 만들지 않는다 (시험은 오버월드에서 hot = true로 부른다). 만든 증기를 돌려준다. */
	public int work(ServerLevel level, BlockPos pos, boolean hot) {
		int before = steam;
		lava = hot ? countLava(level, pos) : 0;
		int made = Math.min(STEAM_CAPACITY - steam, lava * steamPerLava());
		steam += made;
		flow.add(level.getGameTime(), made);
		steam = outlet.distribute(level, pos, steam);
		// 만들었든 내보냈든 저장값이 달라졌으면 표시한다 (용암이 없어도 남은 증기는 나간다)
		if (steam != before) {
			setChanged();
		}
		return made;
	}

	private static int countLava(Level level, BlockPos pos) {
		int count = 0;
		for (Direction direction : Direction.values()) {
			FluidState fluid = level.getFluidState(pos.relative(direction));
			if (fluid.is(FluidTags.LAVA) && fluid.isSource() && ++count >= MAX_LAVA) {
				break;
			}
		}
		return count;
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

	/** 분출구는 만든 양을 센다. */
	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(level != null ? level.getGameTime() : 0L);
	}

	/** 지난 틱에 센 용암 원천 수. */
	public int lava() {
		return lava;
	}

	public Status status() {
		if (level == null || !isHot(level)) {
			return Status.TOO_COLD;
		}
		return lava > 0 ? Status.RUNNING : Status.NO_LAVA;
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
