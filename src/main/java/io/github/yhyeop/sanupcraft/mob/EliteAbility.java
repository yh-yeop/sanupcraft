package io.github.yhyeop.sanupcraft.mob;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;

/**
 * 정예 몹의 능력 하나 (확장 3 C2). 이름표 색으로 어떤 능력인지 알 수 있다.
 * <ul>
 *   <li>가속: 이동 속도 +{@link #SPEED_BONUS}.</li>
 *   <li>반사: 때린 쪽에 받은 피해의 {@link #REFLECT_SHARE}를 가시처럼 돌려준다.</li>
 *   <li>재생: {@link #REGEN_INTERVAL}틱마다 체력 1.</li>
 * </ul>
 */
public enum EliteAbility {
	SPEED(ChatFormatting.AQUA, 0x55FFFF),
	REFLECT(ChatFormatting.GOLD, 0xFFAA00),
	REGEN(ChatFormatting.LIGHT_PURPLE, 0xFF55FF);

	public static final double SPEED_BONUS = 0.3;
	public static final float REFLECT_SHARE = 0.3F;
	public static final int REGEN_INTERVAL = 40;

	private final ChatFormatting color;
	private final int rgb;

	EliteAbility(ChatFormatting color, int rgb) {
		this.color = color;
		this.rgb = rgb;
	}

	/** 이름표와 같은 색 (RGB, 정예 테두리 빛). */
	public int rgb() {
		return rgb;
	}

	public ChatFormatting color() {
		return color;
	}

	/** 저장 이름 (speed · reflect · regen). */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static @Nullable EliteAbility byId(String id) {
		for (EliteAbility ability : values()) {
			if (ability.id().equals(id)) {
				return ability;
			}
		}
		return null;
	}

	public static EliteAbility random(RandomSource random) {
		return values()[random.nextInt(values().length)];
	}
}
