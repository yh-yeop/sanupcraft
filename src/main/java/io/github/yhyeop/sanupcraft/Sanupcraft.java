package io.github.yhyeop.sanupcraft;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.compat.CorpseGravesCompat;
import io.github.yhyeop.sanupcraft.config.ConfigCommand;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.core.ModTab;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.world.ModWorld;

public class Sanupcraft implements ModInitializer {
	public static final String MOD_ID = "sanupcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SanupcraftConfig.init();
		ConfigCommand.init();
		// 등록 순서 = 크리에이티브 탭 순서
		ModItems.init();
		ModMachines.init();
		ModPower.init();
		ModLogistics.init();
		ModMining.init();
		SteelGear.init();
		HeatproofGear.init();
		ModBoss.init();
		ModWorld.init();
		ModTab.init();
		CorpseGravesCompat.init();
		PlaytestCommand.init();
		LOGGER.info("sanupcraft 로드");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
