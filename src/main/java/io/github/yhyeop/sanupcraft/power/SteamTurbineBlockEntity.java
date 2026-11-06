package io.github.yhyeop.sanupcraft.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.nether.ModNether;

/**
 * 증기 터빈 (M5): 고압 파이프로 받은 고압 증기 1을 보통 증기 3(설정 {@code turbine.multiplier})으로 바꿔 옆의 보통 증기망에 내보낸다.
 * 고압 쪽은 {@link HPSteamConsumer}, 보통 쪽은 보일러처럼 {@link SteamOutlet}을 쓰는 증기원이다 (보통 파이프가 터빈에 잇는다).
 * 틱당 고압 증기 {@link #HP_PER_TICK}까지 바꾼다. 계기판은 고압 쪽(받은 양)을 읽는다.
 */
public class SteamTurbineBlockEntity extends BlockEntity implements HPSteamConsumer, SteamStorage {
	public static final int HP_CAPACITY = 200;
	public static final int STEAM_CAPACITY = 600;
	/** 틱당 바꾸는 고압 증기 상한 (영혼 화로 둘 몫). */
	public static final int HP_PER_TICK = 8;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int MULTIPLIER = 3;
	private static final int LIT_HOLD_TICKS = 10;

	private int hp;
	private int steam;
	private int idleTicks = LIT_HOLD_TICKS;
	private final SteamFlowMeter flow = new SteamFlowMeter();
	private final SteamOutlet outlet = new SteamOutlet();

	public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(ModNether.STEAM_TURBINE_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SteamTurbineBlockEntity turbine) {
		turbine.work(level, pos);
		boolean lit = turbine.idleTicks < LIT_HOLD_TICKS;
		if (state.getValue(SteamTurbineBlock.LIT) != lit) {
			level.setBlockAndUpdate(pos, state.setValue(SteamTurbineBlock.LIT, lit));
		}
	}

	/** 한 틱: 고압 증기를 보통 증기로 바꾸고 보통 증기망에 나눈다. 바꾼 고압 증기 양을 돌려준다. */
	public int work(ServerLevel level, BlockPos pos) {
		int multiplier = SanupcraftConfig.current().turbineMultiplier;
		int before = hp + steam;
		int converted = Math.min(Math.min(hp, HP_PER_TICK), (STEAM_CAPACITY - steam) / multiplier);
		if (converted > 0) {
			hp -= converted;
			steam += converted * multiplier;
		}
		idleTicks = converted > 0 ? 0 : Math.min(idleTicks + 1, LIT_HOLD_TICKS);
		steam = outlet.distribute(level, pos, steam);
		if (hp + steam != before) {
			setChanged();
		}
		return converted;
	}

	@Override
	public int acceptHighPressureSteam(int amount) {
		int accepted = Math.min(amount, HP_CAPACITY - hp);
		if (accepted > 0) {
			hp += accepted;
			flow.add(level != null ? level.getGameTime() : 0L, accepted);
		}
		return accepted;
	}

	/** 모아 둔 고압 증기. */
	public int highPressure() {
		return hp;
	}

	/** 바꿔 두었지만 아직 못 내보낸 보통 증기. */
	public int steam() {
		return steam;
	}

	@Override
	public int steamStored() {
		return hp;
	}

	@Override
	public int steamCapacity() {
		return HP_CAPACITY;
	}

	@Override
	public int steamReceivedLastSecond() {
		return flow.lastSecond(level != null ? level.getGameTime() : 0L);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		hp = input.getIntOr("high_pressure", 0);
		steam = input.getIntOr("steam", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("high_pressure", hp);
		output.putInt("steam", steam);
	}
}
