package io.github.yhyeop.sanupcraft;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.yhyeop.sanupcraft.boss.BossTier;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.compat.CorpseGravesCompat;
import io.github.yhyeop.sanupcraft.config.ConfigCommand;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.core.ModTab;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.fluid.ModFluid;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.ProductionGear;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.gear.backpack.ModBackpack;
import io.github.yhyeop.sanupcraft.industry.ModIndustry;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.production.grinder.ModGrinder;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.server.ModServer;
import io.github.yhyeop.sanupcraft.production.ModProduction;
import io.github.yhyeop.sanupcraft.transport.ModTransport;
import io.github.yhyeop.sanupcraft.storage.ModStorage;
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
		ModFluid.init();
		ModLogistics.init();
		ModMining.init();
		SteelGear.init();
		HeatproofGear.init();
		SteamGear.init();
		ModBoss.init();
		ModWorld.init();
		io.github.yhyeop.sanupcraft.wrench.ModWrench.init(); // M8 I4 렌치 · 면 설정
		io.github.yhyeop.sanupcraft.sound.ModSounds.init(); // M11 X2 효과음
		BossTier.init(); // C3 강화 소환석
		io.github.yhyeop.sanupcraft.industry.ModFoundry.init(); // M8 묶음 B 용광로 탑 · 정제 가루
		io.github.yhyeop.sanupcraft.end.ModEnd.init(); // M6 엔드 최종
		ModGrinder.init(); // M9b 몹 분쇄기 · 경험치 탱크
		ModBackpack.init(); // M9b 배낭
		ModServer.init(); // M10 서버 운영 (팀 · 기록판 · 목표판 · 설명서)
		ModProduction.init(); // 확장 M9a
		ProductionGear.init(); // 확장 M9a
		// M8 묶음 A: 대형 보일러 · 증기 기관차
		ModIndustry.init();
		ModTransport.init();
		// M7 보관 · 제작
		ModStorage.init();
		io.github.yhyeop.sanupcraft.nether.ModNether.init(); // M5 네더 심화 2
		ModTab.init();
		CorpseGravesCompat.init();
		PlaytestCommand.init();
		LOGGER.info("sanupcraft 로드");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
