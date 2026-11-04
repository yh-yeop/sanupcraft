package io.github.yhyeop.sanupcraft.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 모드팩의 시체 모드(Corpse Graves)에서 시체가 갑옷을 입은 모습으로 보이게 한다.
 * <p>
 * Corpse Graves 1.1.0은 설정을 읽을 때마다 옛 설정 옮기기 코드가 돌아 {@code displayArmor}를 false로 되돌린다
 * (옛 키 {@code onlyOwnerCanLoot}가 없으면 실행되는데, 그 키를 스스로 쓰지 않음). 설정 파일로는 유지되지 않으므로
 * 서버에서 1초마다 다시 켠다. Corpse Graves가 없거나 구조가 바뀌면 경고 한 번 남기고 꺼진다.
 */
public final class CorpseGravesCompat {
	private static final String CONFIG_CLASS = "com.kador.corpsegraves.config.GraveConfig";

	private static Method getConfig;
	private static Field corpseField;
	private static Field displayArmorField;
	private static boolean broken;

	private CorpseGravesCompat() {
	}

	public static void init() {
		if (!FabricLoader.getInstance().isModLoaded("corpsegraves")) {
			return;
		}
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!broken && server.getTickCount() % 20 == 0) {
				showArmorOnCorpses();
			}
		});
	}

	private static void showArmorOnCorpses() {
		try {
			if (getConfig == null) {
				Class<?> configClass = Class.forName(CONFIG_CLASS);
				getConfig = configClass.getMethod("get");
				corpseField = configClass.getField("corpse");
				displayArmorField = corpseField.getType().getField("displayArmor");
			}
			Object corpse = corpseField.get(getConfig.invoke(null));
			if (!displayArmorField.getBoolean(corpse)) {
				displayArmorField.setBoolean(corpse, true);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			broken = true;
			Sanupcraft.LOGGER.warn("Corpse Graves 설정을 바꾸지 못함 (시체에 갑옷이 안 보일 수 있음)", e);
		}
	}
}
