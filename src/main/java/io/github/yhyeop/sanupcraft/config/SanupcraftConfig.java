package io.github.yhyeop.sanupcraft.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.BossParty;
import io.github.yhyeop.sanupcraft.factory.MachineUpgrade;
import io.github.yhyeop.sanupcraft.fluid.PumpBlockEntity;
import io.github.yhyeop.sanupcraft.gear.ChainsawItem;
import io.github.yhyeop.sanupcraft.gear.Jetpack;
import io.github.yhyeop.sanupcraft.gear.RivetGunItem;
import io.github.yhyeop.sanupcraft.gear.SteamBoots;
import io.github.yhyeop.sanupcraft.gear.SteamDrillItem;
import io.github.yhyeop.sanupcraft.logistics.BeltSorterBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.EnderAnchorBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.FilterHopperBlockEntity;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;
import io.github.yhyeop.sanupcraft.production.grinder.MobGrinderBlockEntity;
import io.github.yhyeop.sanupcraft.power.GeothermalVentBlockEntity;
import io.github.yhyeop.sanupcraft.power.SteamChargerBlockEntity;
import io.github.yhyeop.sanupcraft.production.CropHarvesterBlockEntity;
import io.github.yhyeop.sanupcraft.production.TreeFellerBlockEntity;
import io.github.yhyeop.sanupcraft.industry.BigBoilerControllerBlockEntity;
import io.github.yhyeop.sanupcraft.transport.CargoStationBlockEntity;
import io.github.yhyeop.sanupcraft.transport.SteamLocomotive;

/**
 * 서버 설정 파일 {@code config/sanupcraft.json}. 서버(싱글은 내장 서버)가 시작될 때 읽고, 파일이 없으면 기본값으로 만든다.
 * 값이 이상하면 (숫자가 아님 · 범위 밖 · JSON이 깨짐) 그 값만 (깨졌으면 전부) 기본값으로 쓰고 경고를 남긴다.
 * {@code /sanupcraft config reload}로 다시 읽는다. 기본값은 각 클래스의 상수이고, 여기서는 그 상수를 가리키기만 한다.
 *
 * <p>값을 더하려면: ① 아래에 필드 하나, ② {@link #OPTIONS}에 {@link Option} 한 줄 (구역 · 키 · 기본값 · 범위),
 * ③ 쓰는 곳에서 {@code SanupcraftConfig.current().필드}를 읽는다. 파일 읽기 · 쓰기 · 검증 · 경고는 표를 보고 알아서 한다.
 * 파일에 새 키가 빠져 있고 다른 문제가 없으면, 읽을 때 빠진 키를 기본값으로 채워 파일을 다시 쓴다.
 */
public final class SanupcraftConfig {
	/** 파일 이름 (config 폴더 안). */
	public static final String FILE_NAME = "sanupcraft.json";

	// ---- 값 (읽기 전용으로 쓴다. 시험만 직접 바꾼다) ----
	/** 보스 소환 때 추가 인원 한 명당 체력 배율 증가. */
	public double bossHealthPerExtraPlayer = BossParty.HEALTH_PER_EXTRA_PLAYER;
	/** 보스 체력 배율 상한. */
	public double bossMaxHealthScale = BossParty.MAX_HEALTH_SCALE;
	/** 보상을 받으려고 깎아야 하는 최대 체력 비율. */
	public double bossContributionShare = asDouble(BossParty.CONTRIBUTION_SHARE);
	/** 지열 분출구: 용암 원천 하나당 틱당 증기. */
	public int ventSteamPerLava = GeothermalVentBlockEntity.STEAM_PER_LAVA;
	/** 엔더 닻: 튀긴 후렴 하나가 버티는 틱. */
	public int anchorTicksPerFuel = EnderAnchorBlockEntity.TICKS_PER_FUEL;
	/** 엔더 닻: 붙잡는 둘레 청크 수 (1 = 3x3). */
	public int anchorRadius = EnderAnchorBlockEntity.RADIUS;
	/** 증기 굴착기: 블록 하나를 캐는 데 걸리는 틱. */
	public int quarryTicksPerBlock = QuarryBlockEntity.TICKS_PER_BLOCK;
	/** 벨트 분류기: 한 덩어리를 내보내 보는 간격(틱). */
	public int sorterTicksPerMove = BeltSorterBlockEntity.TICKS_PER_MOVE;
	/** 필터 깔때기: 아이템 하나를 옮기는 간격(틱). 바닐라 깔때기는 8. */
	public int filterHopperTicksPerMove = FilterHopperBlockEntity.TICKS_PER_MOVE;
	/** 리벳총: 한 발에 드는 증기. */
	public int rivetGunSteam = RivetGunItem.STEAM_COST;
	/** 리벳총: 리벳이 주는 피해. */
	public double rivetGunDamage = asDouble(RivetGunItem.DAMAGE);
	/** 리벳총: 쏘고 나서 기다리는 틱. */
	public int rivetGunCooldown = RivetGunItem.COOLDOWN_TICKS;
	/** 증기 부츠: 돌진 한 번에 드는 증기. */
	public int bootsDashSteam = SteamBoots.DASH_COST;
	/** 증기 부츠: 돌진 뒤 기다리는 틱. */
	public int bootsDashCooldown = SteamBoots.DASH_COOLDOWN;
	/** 증기 충전대: 틱당 채우는 증기. */
	public int chargerSteamPerTick = SteamChargerBlockEntity.TRANSFER_PER_TICK;
	/** 기계 업그레이드: 속도 업그레이드 하나의 처리 배율 (틱당 증기 · 연료도 같은 배율). */
	public double upgradeSpeedMultiplier = MachineUpgrade.SPEED_MULTIPLIER;
	/** 기계 업그레이드: 효율 업그레이드 하나의 증기 · 연료 배율 (0.7 = 30% 절약). */
	public double upgradeEfficiencyMultiplier = MachineUpgrade.EFFICIENCY_MULTIPLIER;
	/** 기계 업그레이드: 두 배 업그레이드 하나가 결과를 하나 더 낼 확률. */
	public double upgradeDoubleChance = MachineUpgrade.DOUBLE_CHANCE;
	/** 펌프: 원천을 찾는 반경 (같은 높이 ~ 아래 이 칸 수, 가로 원). */
	public int pumpRadius = PumpBlockEntity.RADIUS;
	/** 펌프: 원천 하나(1양동이)에 드는 증기. */
	public int pumpSteamPerBucket = PumpBlockEntity.STEAM_PER_BUCKET;
	/** 펌프: 원천 하나를 빨아들이는 간격(틱). */
	public int pumpTicksPerBucket = PumpBlockEntity.TICKS_PER_BUCKET;
	// ---- M9 생산 · 장비 (확장 묶음 M9a) ----
	/** 작물 수확기: 앞 9x9의 한 칸을 훑는 간격(틱). */
	public int harvesterTicksPerScan = CropHarvesterBlockEntity.TICKS_PER_SCAN;
	/** 작물 수확기: 작물 하나를 거둘 때 드는 증기. */
	public int harvesterSteamPerCrop = CropHarvesterBlockEntity.STEAM_PER_CROP;
	/** 벌목기: 블록 하나를 베는 간격(틱). */
	public int fellerTicksPerBlock = TreeFellerBlockEntity.TICKS_PER_BLOCK;
	/** 벌목기: 원목 하나를 벨 때 드는 증기 (잎은 1/4). */
	public int fellerSteamPerBlock = TreeFellerBlockEntity.STEAM_PER_BLOCK;
	/** 증기 드릴: 블록 하나를 캘 때 드는 증기. */
	public int drillSteamPerBlock = SteamDrillItem.STEAM_PER_BLOCK;
	/** 전기톱: 원목 하나를 벨 때 드는 증기 (잎은 1/4). */
	public int chainsawSteamPerBlock = ChainsawItem.STEAM_PER_BLOCK;
	/** 증기 제트팩: 떠 있는 틱당 드는 증기. */
	public int jetpackSteamPerTick = Jetpack.STEAM_PER_TICK;
	// ---- M9b 몹 분쇄기 ----
	/** 몹 분쇄기: 한 번 벨 때 몹 하나가 받는 피해. */
	public double grinderDamage = asDouble(MobGrinderBlockEntity.DAMAGE);
	/** 몹 분쇄기: 몹 하나를 한 번 베는 데 드는 증기. */
	public int grinderSteamPerHit = MobGrinderBlockEntity.STEAM_PER_HIT;
	/** 몹 분쇄기: 베는 간격(틱). */
	public int grinderTicksPerHit = MobGrinderBlockEntity.TICKS_PER_HIT;
	// M8 묶음 A: 대형 보일러 · 증기 기관차
	/** 대형 보일러: 모양이 맞을 때 틱당 증기 (보일러는 4). */
	public int bigBoilerSteamPerTick = BigBoilerControllerBlockEntity.STEAM_PER_TICK;
	/** 증기 기관차: 최고 속도 (블록/틱, 화로 수레 0.2, 수레 한도 0.4). */
	public double locomotiveMaxSpeed = SteamLocomotive.MAX_SPEED;
	/** 적재 · 하역 역: 한 번(2틱)에 옮기는 아이템 수. */
	public int stationItemsPerTransfer = CargoStationBlockEntity.ITEMS_PER_TRANSFER;

	/** float 상수를 0.05F -> 0.05처럼 보이는 그대로의 double로 (파일에 0.0500000007...이 안 찍히게). */
	private static double asDouble(float value) {
		return Double.parseDouble(Float.toString(value));
	}

	/** 설정 한 줄: 파일의 구역 · 키, 기본값, 허용 범위, 읽기 · 쓰기 함수. */
	private record Option(String section, String key, double def, double min, double max, boolean integer,
			ToDoubleFunction<SanupcraftConfig> get, ObjDoubleConsumer<SanupcraftConfig> set) {
	}

	private static final List<Option> OPTIONS = List.of(
			new Option("boss", "healthPerExtraPlayer", BossParty.HEALTH_PER_EXTRA_PLAYER, 0.0, 10.0, false,
					c -> c.bossHealthPerExtraPlayer, (c, v) -> c.bossHealthPerExtraPlayer = v),
			new Option("boss", "maxHealthScale", BossParty.MAX_HEALTH_SCALE, 1.0, 20.0, false,
					c -> c.bossMaxHealthScale, (c, v) -> c.bossMaxHealthScale = v),
			new Option("boss", "contributionShare", asDouble(BossParty.CONTRIBUTION_SHARE), 0.0, 1.0, false,
					c -> c.bossContributionShare, (c, v) -> c.bossContributionShare = v),
			new Option("geothermalVent", "steamPerLava", GeothermalVentBlockEntity.STEAM_PER_LAVA, 0, 100, true,
					c -> c.ventSteamPerLava, (c, v) -> c.ventSteamPerLava = (int) v),
			new Option("enderAnchor", "ticksPerFuel", EnderAnchorBlockEntity.TICKS_PER_FUEL, 20, 10_000_000, true,
					c -> c.anchorTicksPerFuel, (c, v) -> c.anchorTicksPerFuel = (int) v),
			new Option("enderAnchor", "radius", EnderAnchorBlockEntity.RADIUS, 0, 3, true,
					c -> c.anchorRadius, (c, v) -> c.anchorRadius = (int) v),
			new Option("quarry", "ticksPerBlock", QuarryBlockEntity.TICKS_PER_BLOCK, 1, 200, true,
					c -> c.quarryTicksPerBlock, (c, v) -> c.quarryTicksPerBlock = (int) v),
			new Option("beltSorter", "ticksPerMove", BeltSorterBlockEntity.TICKS_PER_MOVE, 1, 200, true,
					c -> c.sorterTicksPerMove, (c, v) -> c.sorterTicksPerMove = (int) v),
			new Option("filterHopper", "ticksPerMove", FilterHopperBlockEntity.TICKS_PER_MOVE, 1, 200, true,
					c -> c.filterHopperTicksPerMove, (c, v) -> c.filterHopperTicksPerMove = (int) v),
			new Option("rivetGun", "steamCost", RivetGunItem.STEAM_COST, 0, 1000, true,
					c -> c.rivetGunSteam, (c, v) -> c.rivetGunSteam = (int) v),
			new Option("rivetGun", "damage", asDouble(RivetGunItem.DAMAGE), 0.0, 100.0, false,
					c -> c.rivetGunDamage, (c, v) -> c.rivetGunDamage = v),
			new Option("rivetGun", "cooldownTicks", RivetGunItem.COOLDOWN_TICKS, 0, 200, true,
					c -> c.rivetGunCooldown, (c, v) -> c.rivetGunCooldown = (int) v),
			new Option("steamBoots", "dashSteamCost", SteamBoots.DASH_COST, 0, SteamBoots.CAPACITY, true,
					c -> c.bootsDashSteam, (c, v) -> c.bootsDashSteam = (int) v),
			new Option("steamBoots", "dashCooldownTicks", SteamBoots.DASH_COOLDOWN, 0, 600, true,
					c -> c.bootsDashCooldown, (c, v) -> c.bootsDashCooldown = (int) v),
			new Option("steamCharger", "steamPerTick", SteamChargerBlockEntity.TRANSFER_PER_TICK, 1, 1000, true,
					c -> c.chargerSteamPerTick, (c, v) -> c.chargerSteamPerTick = (int) v),
			new Option("machineUpgrade", "speedMultiplier", MachineUpgrade.SPEED_MULTIPLIER, 1.0, 10.0, false,
					c -> c.upgradeSpeedMultiplier, (c, v) -> c.upgradeSpeedMultiplier = v),
			new Option("machineUpgrade", "efficiencyMultiplier", MachineUpgrade.EFFICIENCY_MULTIPLIER, 0.05, 1.0, false,
					c -> c.upgradeEfficiencyMultiplier, (c, v) -> c.upgradeEfficiencyMultiplier = v),
			new Option("machineUpgrade", "doubleChance", MachineUpgrade.DOUBLE_CHANCE, 0.0, 1.0, false,
					c -> c.upgradeDoubleChance, (c, v) -> c.upgradeDoubleChance = v),
			new Option("pump", "radius", PumpBlockEntity.RADIUS, 0, 8, true,
					c -> c.pumpRadius, (c, v) -> c.pumpRadius = (int) v),
			new Option("pump", "steamPerBucket", PumpBlockEntity.STEAM_PER_BUCKET, 0, PumpBlockEntity.STEAM_CAPACITY, true,
					c -> c.pumpSteamPerBucket, (c, v) -> c.pumpSteamPerBucket = (int) v),
			new Option("pump", "ticksPerBucket", PumpBlockEntity.TICKS_PER_BUCKET, 1, 1200, true,
					c -> c.pumpTicksPerBucket, (c, v) -> c.pumpTicksPerBucket = (int) v),
			// M9 생산 · 장비 (확장 묶음 M9a)
			new Option("cropHarvester", "ticksPerScan", CropHarvesterBlockEntity.TICKS_PER_SCAN, 1, 200, true,
					c -> c.harvesterTicksPerScan, (c, v) -> c.harvesterTicksPerScan = (int) v),
			new Option("cropHarvester", "steamPerCrop", CropHarvesterBlockEntity.STEAM_PER_CROP, 0, 200, true,
					c -> c.harvesterSteamPerCrop, (c, v) -> c.harvesterSteamPerCrop = (int) v),
			new Option("treeFeller", "ticksPerBlock", TreeFellerBlockEntity.TICKS_PER_BLOCK, 1, 200, true,
					c -> c.fellerTicksPerBlock, (c, v) -> c.fellerTicksPerBlock = (int) v),
			new Option("treeFeller", "steamPerBlock", TreeFellerBlockEntity.STEAM_PER_BLOCK, 0, 200, true,
					c -> c.fellerSteamPerBlock, (c, v) -> c.fellerSteamPerBlock = (int) v),
			new Option("steamDrill", "steamPerBlock", SteamDrillItem.STEAM_PER_BLOCK, 0, SteamDrillItem.CAPACITY, true,
					c -> c.drillSteamPerBlock, (c, v) -> c.drillSteamPerBlock = (int) v),
			new Option("chainsaw", "steamPerBlock", ChainsawItem.STEAM_PER_BLOCK, 0, ChainsawItem.CAPACITY, true,
					c -> c.chainsawSteamPerBlock, (c, v) -> c.chainsawSteamPerBlock = (int) v),
			new Option("steamJetpack", "steamPerTick", Jetpack.STEAM_PER_TICK, 0, 100, true,
					c -> c.jetpackSteamPerTick, (c, v) -> c.jetpackSteamPerTick = (int) v),
			// M9b 몹 분쇄기
			new Option("mobGrinder", "damage", asDouble(MobGrinderBlockEntity.DAMAGE), 0.0, 1000.0, false,
					c -> c.grinderDamage, (c, v) -> c.grinderDamage = v),
			new Option("mobGrinder", "steamPerHit", MobGrinderBlockEntity.STEAM_PER_HIT, 0, MobGrinderBlockEntity.STEAM_CAPACITY, true,
					c -> c.grinderSteamPerHit, (c, v) -> c.grinderSteamPerHit = (int) v),
			new Option("mobGrinder", "ticksPerHit", MobGrinderBlockEntity.TICKS_PER_HIT, 1, 1200, true,
					c -> c.grinderTicksPerHit, (c, v) -> c.grinderTicksPerHit = (int) v),
			// M8 묶음 A
			new Option("bigBoiler", "steamPerTick", BigBoilerControllerBlockEntity.STEAM_PER_TICK, 1, 1000, true,
					c -> c.bigBoilerSteamPerTick, (c, v) -> c.bigBoilerSteamPerTick = (int) v),
			new Option("locomotive", "maxSpeed", SteamLocomotive.MAX_SPEED, 0.05, 0.4, false,
					c -> c.locomotiveMaxSpeed, (c, v) -> c.locomotiveMaxSpeed = v),
			new Option("cargoStation", "itemsPerTransfer", CargoStationBlockEntity.ITEMS_PER_TRANSFER, 1, 64, true,
					c -> c.stationItemsPerTransfer, (c, v) -> c.stationItemsPerTransfer = (int) v));

	/** 읽은 결과: 설정, 경고 문구, 파일에 빠진 키가 있었는지, 파일을 새로 썼는지. */
	public record Loaded(SanupcraftConfig config, List<String> warnings, boolean missingKeys, boolean wroteFile) {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile SanupcraftConfig current = new SanupcraftConfig();

	/** 지금 쓰는 설정. 서버가 시작되기 전에는 기본값이다. */
	public static SanupcraftConfig current() {
		return current;
	}

	/** 기본값 설정 한 벌 (새 객체). */
	public static SanupcraftConfig defaults() {
		return new SanupcraftConfig();
	}

	/** 이 설정을 지금 쓰는 것으로 바꾼다 (시험 · 다시 읽기). */
	public static void use(SanupcraftConfig config) {
		current = config;
	}

	/** 기본값으로 되돌린다 (시험 뒤처리). */
	public static void resetToDefaults() {
		current = new SanupcraftConfig();
	}

	/** 모드 시작 때 한 번: 서버가 시작될 때 설정 파일을 읽도록 건다. */
	public static void init() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> load());
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** 설정 파일을 읽어 지금 쓰는 것으로 한다. 없으면 기본값으로 만든다. 경고는 로그에도 남긴다. */
	public static Loaded load() {
		return load(path());
	}

	public static Loaded load(Path file) {
		SanupcraftConfig result;
		List<String> warnings = new ArrayList<>();
		boolean missing = false;
		boolean wrote = false;
		if (Files.isRegularFile(file)) {
			String text = null;
			try {
				text = Files.readString(file, StandardCharsets.UTF_8);
			} catch (IOException e) {
				warnings.add("설정 파일을 읽지 못했다 (" + e.getMessage() + ") - 기본값을 쓴다");
			}
			if (text == null) {
				result = new SanupcraftConfig();
			} else {
				Loaded parsed = parse(text);
				result = parsed.config();
				warnings.addAll(parsed.warnings());
				missing = parsed.missingKeys();
				// 새 키만 빠졌고 다른 문제가 없으면 채워서 다시 쓴다 (고장 난 파일은 건드리지 않는다)
				if (missing && warnings.isEmpty()) {
					wrote = write(file, result, warnings);
				}
			}
		} else {
			result = new SanupcraftConfig();
			wrote = write(file, result, warnings);
		}
		for (String warning : warnings) {
			Sanupcraft.LOGGER.warn("{}: {}", FILE_NAME, warning);
		}
		use(result);
		return new Loaded(result, List.copyOf(warnings), missing, wrote);
	}

	/** JSON 글자를 설정으로 바꾼다. 절대 던지지 않는다: 이상한 값은 기본값 + 경고, JSON이 깨졌으면 전부 기본값. */
	public static Loaded parse(String json) {
		SanupcraftConfig config = new SanupcraftConfig();
		List<String> warnings = new ArrayList<>();
		JsonObject root;
		try {
			JsonElement element = JsonParser.parseString(json);
			if (!element.isJsonObject()) {
				warnings.add("최상위가 객체({})가 아니다 - 전부 기본값을 쓴다");
				return new Loaded(config, warnings, false, false);
			}
			root = element.getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			warnings.add("JSON이 깨졌다 (" + e.getMessage() + ") - 전부 기본값을 쓴다");
			return new Loaded(config, warnings, false, false);
		}
		boolean missing = false;
		for (Option option : OPTIONS) {
			JsonElement section = root.get(option.section());
			JsonElement value = section != null && section.isJsonObject() ? section.getAsJsonObject().get(option.key()) : null;
			String name = option.section() + "." + option.key();
			if (value == null) {
				missing = true;
				if (section != null && !section.isJsonObject()) {
					warnings.add(name + ": 구역 '" + option.section() + "'이 객체가 아니다 - 기본값 " + format(option, option.def()));
				}
				continue;
			}
			String problem = check(option, value);
			if (problem != null) {
				warnings.add(name + ": " + problem + " - 기본값 " + format(option, option.def()));
				continue;
			}
			option.set().accept(config, value.getAsDouble());
		}
		for (String sectionName : root.keySet()) {
			JsonElement section = root.get(sectionName);
			boolean known = OPTIONS.stream().anyMatch(o -> o.section().equals(sectionName));
			if (!known) {
				warnings.add("알 수 없는 설정 '" + sectionName + "' - 무시한다");
			} else if (section.isJsonObject()) {
				for (String key : section.getAsJsonObject().keySet()) {
					if (OPTIONS.stream().noneMatch(o -> o.section().equals(sectionName) && o.key().equals(key))) {
						warnings.add("알 수 없는 설정 '" + sectionName + "." + key + "' - 무시한다");
					}
				}
			}
		}
		return new Loaded(config, warnings, missing, false);
	}

	/** 값이 쓸 만하면 null, 아니면 이유. */
	private static String check(Option option, JsonElement value) {
		if (!value.isJsonPrimitive() || !((JsonPrimitive) value).isNumber()) {
			return "숫자가 아니다 (" + value + ")";
		}
		double number = value.getAsDouble();
		if (Double.isNaN(number) || Double.isInfinite(number)) {
			return "유한한 숫자가 아니다";
		}
		if (option.integer() && number != Math.rint(number)) {
			return "정수여야 한다 (" + value + ")";
		}
		if (number < option.min() || number > option.max()) {
			return "범위 " + format(option, option.min()) + " ~ " + format(option, option.max()) + " 밖이다 (" + value + ")";
		}
		return null;
	}

	private static String format(Option option, double value) {
		return option.integer() ? Long.toString((long) value) : Double.toString(value);
	}

	/** 이 설정을 JSON 글자로 (구역별로 묶고, 표의 순서를 따른다). */
	public String toJson() {
		JsonObject root = new JsonObject();
		for (Option option : OPTIONS) {
			JsonObject section = root.has(option.section()) ? root.getAsJsonObject(option.section()) : new JsonObject();
			double value = option.get().applyAsDouble(this);
			if (option.integer()) {
				section.addProperty(option.key(), (long) value);
			} else {
				section.addProperty(option.key(), value);
			}
			root.add(option.section(), section);
		}
		return GSON.toJson(root) + "\n";
	}

	private static boolean write(Path file, SanupcraftConfig config, List<String> warnings) {
		try {
			Path parent = file.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.writeString(file, config.toJson(), StandardCharsets.UTF_8);
			return true;
		} catch (IOException e) {
			warnings.add("설정 파일을 쓰지 못했다 (" + e.getMessage() + ")");
			return false;
		}
	}
}
