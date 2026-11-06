package io.github.yhyeop.sanupcraft.resource.vein;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;

/**
 * 광맥 (확장 3 D2): 청크마다 숨은 광맥이 있을 수 있다. 종류 · 처음 매장량은 월드 시드 + 청크 위치 + 차원으로 정해진다
 * (같은 시드면 항상 같다 - {@link #generate}). 캔 만큼은 {@link VeinData}에 남는다 (한 번이라도 캔 청크만).
 * 오버월드 · 네더에만 있고 엔드에는 없다. 증기 굴착기가 자기 청크의 광맥에서 덤을 캔다 ({@link #rollBonus}).
 */
public final class Veins {
	/** 기본값. 실제 값은 설정 파일 {@code veins.*}. */
	public static final int CHUNK_PERCENT = 25;
	public static final int MIN_AMOUNT = 2000;
	public static final int MAX_AMOUNT = 8000;
	/** 굴착기가 블록 하나를 캘 때 덤이 나올 확률(%). */
	public static final int BONUS_PERCENT = 15;
	/** 광맥 탐사기가 한 번에 쓰는 증기. */
	public static final int PROSPECTOR_STEAM = 50;
	/** 고대 잔해 광맥의 매장량 (아주 적다). */
	public static final int DEBRIS_MIN_AMOUNT = 40;
	public static final int DEBRIS_MAX_AMOUNT = 120;

	private static final long CHUNK_X_MIX = 341873128712L;
	private static final long CHUNK_Z_MIX = 132897987541L;
	private static final long OVERWORLD_SALT = 0x5A17C0DE0001L;
	private static final long NETHER_SALT = 0x5A17C0DE0002L;

	private Veins() {
	}

	/** 광맥이 생기는 차원인가 (오버월드 · 네더). */
	public static boolean hasVeins(ResourceKey<Level> dimension) {
		return dimension == Level.OVERWORLD || dimension == Level.NETHER;
	}

	/**
	 * 처음 광맥. 시드 · 청크 · 차원만으로 정해지는 결정적 함수다 (남은 양은 보지 않는다).
	 * 청크의 {@code veins.chunkPercent}% 쯤에 광맥이 있고, 종류는 차원별 가중치로 뽑는다.
	 */
	public static Optional<Vein> generate(long seed, ChunkPos chunk, ResourceKey<Level> dimension) {
		if (!hasVeins(dimension)) {
			return Optional.empty();
		}
		boolean nether = dimension == Level.NETHER;
		RandomSource random = RandomSource.create(seed ^ (chunk.x() * CHUNK_X_MIX) ^ (chunk.z() * CHUNK_Z_MIX) ^ (nether ? NETHER_SALT : OVERWORLD_SALT));
		random.nextInt(); // 첫 값은 버린다 (이웃 시드끼리 비슷하지 않게)
		SanupcraftConfig config = SanupcraftConfig.current();
		if (random.nextInt(100) >= config.veinChunkPercent) {
			return Optional.empty();
		}
		int sum = 0;
		for (VeinType type : VeinType.values()) {
			sum += nether ? type.netherWeight() : type.overworldWeight();
		}
		int roll = random.nextInt(sum);
		VeinType picked = VeinType.IRON;
		for (VeinType type : VeinType.values()) {
			int weight = nether ? type.netherWeight() : type.overworldWeight();
			if (roll < weight) {
				picked = type;
				break;
			}
			roll -= weight;
		}
		int amount;
		if (picked.isDebris()) {
			amount = DEBRIS_MIN_AMOUNT + random.nextInt(DEBRIS_MAX_AMOUNT - DEBRIS_MIN_AMOUNT + 1);
		} else {
			int min = Math.min(config.veinMinAmount, config.veinMaxAmount);
			int max = Math.max(config.veinMinAmount, config.veinMaxAmount);
			amount = min + random.nextInt(max - min + 1);
		}
		return Optional.of(new Vein(picked, amount, amount));
	}

	/** 그 청크의 광맥 (남은 양 포함). 없으면 null. 저장된 항목이 있으면 그것을, 없으면 처음 광맥을 돌려준다. */
	public static @Nullable Vein get(ServerLevel level, ChunkPos chunk) {
		if (!hasVeins(level.dimension())) {
			return null;
		}
		Vein saved = VeinData.get(level).get(chunk);
		if (saved != null) {
			return saved;
		}
		return generate(level.getSeed(), chunk, level.dimension()).orElse(null);
	}

	/** 그 청크의 광맥에서 최대 {@code amount}를 캔다. 실제로 캔 양 (광맥이 없거나 바닥났으면 0). */
	public static int draw(ServerLevel level, ChunkPos chunk, int amount) {
		Vein vein = get(level, chunk);
		if (vein == null || vein.depleted() || amount <= 0) {
			return 0;
		}
		int taken = Math.min(amount, vein.remaining());
		VeinData.get(level).put(chunk, vein.withRemaining(vein.remaining() - taken));
		return taken;
	}

	/**
	 * 굴착기가 블록 하나를 캘 때마다 부른다: 확률({@code veins.bonusPercent})에 맞으면 그 청크 광맥에서 1을 캐서 광맥 광석 1개를 돌려준다.
	 * 광맥이 없거나 바닥났으면 빈 스택.
	 */
	public static ItemStack rollBonus(ServerLevel level, ChunkPos chunk, RandomSource random) {
		return rollBonus(level, chunk, random, SanupcraftConfig.current().veinBonusPercent);
	}

	/** {@link #rollBonus(ServerLevel, ChunkPos, RandomSource)}에서 확률(%)을 직접 정한 것 (시험용). */
	public static ItemStack rollBonus(ServerLevel level, ChunkPos chunk, RandomSource random, int percent) {
		if (random.nextInt(100) >= percent) {
			return ItemStack.EMPTY;
		}
		Vein vein = get(level, chunk);
		if (vein == null || vein.depleted() || draw(level, chunk, 1) < 1) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(vein.type().item());
	}

	/** 굴착기 상태 메시지 끝에 붙일 광맥 정보. 광맥이 없으면 null. */
	public static @Nullable Component describe(ServerLevel level, ChunkPos chunk, int bonusMined) {
		Vein vein = get(level, chunk);
		if (vein == null) {
			return null;
		}
		Component name = Component.translatable(vein.type().nameKey());
		return vein.depleted()
				? Component.translatable("block.sanupcraft.steam_quarry.vein_empty", name)
				: Component.translatable("block.sanupcraft.steam_quarry.vein", name, vein.remaining(), vein.total(), bonusMined);
	}
}
