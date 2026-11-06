package io.github.yhyeop.sanupcraft.module;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * 확장 3 G3 장비 모듈 종류. 모듈 아이템(`module_<이름>`)마다 하나. 장비에는 이 이름 목록이 {@link InstalledModules}로 저장된다.
 * 종류(갑옷 · 도구 · 무기 · 리벳총)가 맞는 장비에만 끼운다 ({@link ModuleSlots#kinds}).
 */
public enum GearModule implements StringRepresentable {
	// 갑옷
	/** 6칸 안 떨어진 아이템을 끌어당긴다. */
	MAGNET("magnet", Kind.ARMOR),
	NIGHT_VISION("night_vision", Kind.ARMOR),
	/** 유독 가스(D5)에 걸리지 않는다. */
	GAS_FILTER("gas_filter", Kind.ARMOR),
	/** 녹 면역 ({@code Resistances.rustImmune}). */
	RUST_PROOF("rust_proof", Kind.ARMOR),
	/** 과열 면역 ({@code Resistances.heatResistant}). */
	HEAT_PROOF("heat_proof", Kind.ARMOR),
	/** 공허 침식 면역 ({@code Resistances.voidResistant}). */
	VOID_PROOF("void_proof", Kind.ARMOR),
	/** 점프 강화 I (점프 힘 +0.1 · 안전 낙하 +1). */
	STEAM_JUMP("steam_jump", Kind.ARMOR),
	// 도구
	/** 3x3 캐기 ({@code AreaMining}), 대신 캐는 속도가 해머처럼 60%. 해머 · 굴삭 삽 · 증기 공구에는 못 끼운다. */
	AREA("area", Kind.TOOL),
	/** 캐는 속도 +30% (속성). */
	EFFICIENCY("efficiency", Kind.TOOL),
	/** 드롭을 화로에 구운 것으로. */
	AUTO_SMELT("auto_smelt", Kind.TOOL),
	/** 행운 마법 부여 +1로 드롭을 다시 굴린다. */
	LUCK("luck", Kind.TOOL),
	/** 섬세한 손길로 드롭을 다시 굴린다 (블록 그대로). */
	SILK_TOUCH("silk_touch", Kind.TOOL),
	// 무기
	/** 밀치기 +1 (속성). */
	KNOCKBACK("knockback", Kind.WEAPON),
	// 리벳총 (C1)
	/** 쿨다운 반. */
	RAPID_FIRE("rapid_fire", Kind.RIVET_GUN),
	/** 리벳 1개로 3발 부채꼴 (한 발 피해 50%). */
	SCATTER("scatter", Kind.RIVET_GUN);

	/** 모듈을 끼우는 장비 종류. */
	public enum Kind {
		ARMOR, TOOL, WEAPON, RIVET_GUN
	}

	public static final Codec<GearModule> CODEC = StringRepresentable.fromEnum(GearModule::values);
	public static final StreamCodec<ByteBuf, GearModule> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], GearModule::ordinal);

	private final String id;
	private final Kind kind;
	private Item item;

	GearModule(String id, Kind kind) {
		this.id = id;
		this.kind = kind;
	}

	public String id() {
		return id;
	}

	public Kind kind() {
		return kind;
	}

	/** 이 모듈의 아이템 ({@link ModModules}가 등록하며 정한다). */
	public Item item() {
		return item;
	}

	void setItem(Item item) {
		this.item = item;
	}

	/** 같이 끼울 수 없는가 (섬세한 손길은 행운 · 자동 제련과 함께 못 쓴다). */
	public boolean conflictsWith(GearModule other) {
		return this == SILK_TOUCH && (other == LUCK || other == AUTO_SMELT)
				|| other == SILK_TOUCH && (this == LUCK || this == AUTO_SMELT);
	}

	public Component displayName() {
		return Component.translatable("item.sanupcraft.module_" + id);
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
