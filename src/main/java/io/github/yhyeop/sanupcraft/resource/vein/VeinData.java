package io.github.yhyeop.sanupcraft.resource.vein;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 광맥의 남은 양 (확장 3 D2). 차원마다 하나 (오버월드 · 네더의 레벨 저장 데이터 {@code sanupcraft_veins.dat}).
 * 한 번이라도 캔 청크만 항목이 있다 - 항목이 없는 청크는 {@link Veins#generate}가 정한 처음 광맥 그대로다.
 */
public final class VeinData extends SavedData {
	private record Saved(int x, int z, String type, int total, int remaining) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("x").forGetter(Saved::x),
				Codec.INT.fieldOf("z").forGetter(Saved::z),
				Codec.STRING.fieldOf("type").forGetter(Saved::type),
				Codec.INT.fieldOf("total").forGetter(Saved::total),
				Codec.INT.fieldOf("remaining").forGetter(Saved::remaining)).apply(i, Saved::new));
	}

	public static final Codec<VeinData> CODEC = Saved.CODEC.listOf().xmap(VeinData::new, VeinData::snapshot);

	public static final SavedDataType<VeinData> TYPE = new SavedDataType<>(Sanupcraft.id("veins"), VeinData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<Long, Vein> veins = new HashMap<>();

	public VeinData() {
	}

	private VeinData(List<Saved> list) {
		for (Saved saved : list) {
			VeinType type = VeinType.byId(saved.type());
			if (type != null) {
				veins.put(ChunkPos.pack(saved.x(), saved.z()), new Vein(type, saved.total(), Math.max(0, saved.remaining())));
			}
		}
	}

	private List<Saved> snapshot() {
		return veins.entrySet().stream().map(e -> {
			long key = e.getKey();
			Vein vein = e.getValue();
			return new Saved(ChunkPos.getX(key), ChunkPos.getZ(key), vein.type().id(), vein.total(), vein.remaining());
		}).toList();
	}

	/** 이 차원의 광맥 데이터. */
	public static VeinData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/** 저장된 항목. 없으면 null (= 아직 안 캔 청크). */
	public @Nullable Vein get(ChunkPos chunk) {
		return veins.get(chunk.pack());
	}

	/** 항목을 정한다 (시험이 광맥을 심을 때도 쓴다). */
	public void put(ChunkPos chunk, Vein vein) {
		veins.put(chunk.pack(), vein);
		setDirty();
	}

	public int size() {
		return veins.size();
	}
}
