package io.github.yhyeop.sanupcraft;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.compat.CorpseGravesCompat;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.core.ModTab;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

public class Sanupcraft implements ModInitializer {
	public static final String MOD_ID = "sanupcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 등록 순서 = 크리에이티브 탭 순서
		ModItems.init();
		ModMachines.init();
		SteelGear.init();
		ModBoss.init();
		ModTab.init();
		CorpseGravesCompat.init();
		LOGGER.info("sanupcraft 로드");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
