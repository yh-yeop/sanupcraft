package io.github.yhyeop.sanupcraft.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.boss.BossParty;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.logistics.EnderAnchorBlockEntity;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;
import io.github.yhyeop.sanupcraft.power.GeothermalVentBlockEntity;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.power.SteamFlowMeter;
import io.github.yhyeop.sanupcraft.power.SteamGaugeBlock;
import io.github.yhyeop.sanupcraft.power.SteamGaugeBlockEntity;

/**
 * 묶음 1 검사: 서버 설정 파일 (A3), 증기 계기판 (B3).
 * 설정은 서버 전체가 함께 쓰는 값이라, 바꾸는 시험은 한 메서드 안에서 바꾸고 바로 되돌린다 (다른 시험과 끼어들지 않게).
 */
public class SanupcraftBundle1GameTest {
	// ---- 서버 설정 파일 (A3) ----

	/** 기본 설정 = 각 클래스의 상수. */
	@GameTest
	public void configDefaultsMatchConstants(GameTestHelper helper) {
		SanupcraftConfig defaults = SanupcraftConfig.defaults();
		helper.assertValueEqual(defaults.bossHealthPerExtraPlayer, BossParty.HEALTH_PER_EXTRA_PLAYER, "인원당 체력");
		helper.assertValueEqual(defaults.bossMaxHealthScale, BossParty.MAX_HEALTH_SCALE, "체력 배율 상한");
		helper.assertValueEqual((float) defaults.bossContributionShare, BossParty.CONTRIBUTION_SHARE, "기여 비율");
		helper.assertValueEqual(defaults.ventSteamPerLava, GeothermalVentBlockEntity.STEAM_PER_LAVA, "용암당 증기");
		helper.assertValueEqual(defaults.anchorTicksPerFuel, EnderAnchorBlockEntity.TICKS_PER_FUEL, "닻 연료 틱");
		helper.assertValueEqual(defaults.anchorRadius, EnderAnchorBlockEntity.RADIUS, "닻 반경");
		helper.assertValueEqual(defaults.quarryTicksPerBlock, QuarryBlockEntity.TICKS_PER_BLOCK, "굴착기 틱");
		// 기본값으로 쓴 JSON을 다시 읽어도 같고, 경고도 없고, 빠진 키도 없다
		SanupcraftConfig.Loaded again = SanupcraftConfig.parse(defaults.toJson());
		helper.assertTrue(again.warnings().isEmpty(), "기본 JSON에 경고: " + again.warnings());
		helper.assertFalse(again.missingKeys(), "기본 JSON에 빠진 키");
		helper.assertValueEqual(again.config().toJson(), defaults.toJson(), "다시 읽은 설정");
		helper.succeed();
	}

	/** 설정 값을 바꾸면 분출구 증기 · 보스 배율 · 닻 연료 시간 · 닻 반경이 따라 바뀐다. */
	@GameTest
	public void configChangesBehaviour(GameTestHelper helper) {
		BlockPos ventPos = new BlockPos(2, 2, 2);
		helper.setBlock(ventPos, ModPower.GEOTHERMAL_VENT);
		helper.setBlock(ventPos.north(), Blocks.LAVA);
		helper.setBlock(ventPos.south(), Blocks.LAVA);
		GeothermalVentBlockEntity vent = helper.getBlockEntity(ventPos, GeothermalVentBlockEntity.class);
		BlockPos abs = helper.absolutePos(ventPos);
		BlockPos anchorPos = new BlockPos(4, 2, 4);
		helper.setBlock(anchorPos, ModLogistics.ENDER_ANCHOR);
		EnderAnchorBlockEntity anchor = helper.getBlockEntity(anchorPos, EnderAnchorBlockEntity.class);
		try {
			SanupcraftConfig config = SanupcraftConfig.defaults();
			config.ventSteamPerLava = 5;
			config.bossHealthPerExtraPlayer = 1.0;
			config.bossMaxHealthScale = 2.5;
			config.anchorTicksPerFuel = 100;
			config.anchorRadius = 0;
			SanupcraftConfig.use(config);

			helper.assertValueEqual(vent.work(helper.getLevel(), abs, true), 10, "용암 둘 x 5");
			helper.assertValueEqual(BossParty.healthScale(2), 2.0, "2명 (인원당 1.0)");
			helper.assertValueEqual(BossParty.healthScale(9), 2.5, "9명 (상한 2.5)");
			anchor.insertFuel(new ItemStack(Items.POPPED_CHORUS_FRUIT, 1));
			anchor.tick(helper.getLevel(), helper.absolutePos(anchorPos));
			helper.assertValueEqual(anchor.remainingTicks(), 99L, "닻 남은 시간 (연료 하나 = 100틱)");
			helper.assertTrue(anchor.ownChunkCount() <= 1, "반경 0이면 자기 청크 하나만: " + anchor.ownChunkCount());
		} finally {
			SanupcraftConfig.resetToDefaults();
		}
		helper.assertValueEqual(BossParty.healthScale(2), 1.5, "되돌린 뒤 2명");
		helper.succeed();
	}

	/** 깨진 JSON / 이상한 값은 기본값 + 경고. 파일이 없으면 기본값으로 만든다. 새 키만 빠졌으면 채워서 다시 쓴다. */
	@GameTest
	public void configFallsBackOnBadInput(GameTestHelper helper) throws IOException {
		SanupcraftConfig defaults = SanupcraftConfig.defaults();

		SanupcraftConfig.Loaded broken = SanupcraftConfig.parse("{ not json");
		helper.assertFalse(broken.warnings().isEmpty(), "깨진 JSON에 경고가 없음");
		helper.assertValueEqual(broken.config().toJson(), defaults.toJson(), "깨진 JSON은 전부 기본값");
		helper.assertFalse(SanupcraftConfig.parse("[1, 2]").warnings().isEmpty(), "배열 최상위에 경고가 없음");

		SanupcraftConfig.Loaded mixed = SanupcraftConfig.parse("""
				{"boss": {"healthPerExtraPlayer": "많이", "maxHealthScale": 4.0, "contributionShare": 7},
				 "geothermalVent": {"steamPerLava": 2.5},
				 "enderAnchor": {"ticksPerFuel": 6000, "radius": 99, "mystery": 1},
				 "quarry": {"ticksPerBlock": 8},
				 "other": {}}
				""");
		SanupcraftConfig c = mixed.config();
		helper.assertValueEqual(c.bossHealthPerExtraPlayer, defaults.bossHealthPerExtraPlayer, "글자 값은 기본값");
		helper.assertValueEqual(c.bossMaxHealthScale, 4.0, "정상 값은 적용");
		helper.assertValueEqual(c.bossContributionShare, defaults.bossContributionShare, "범위 밖(1 초과)은 기본값");
		helper.assertValueEqual(c.ventSteamPerLava, defaults.ventSteamPerLava, "정수가 아닌 값은 기본값");
		helper.assertValueEqual(c.anchorTicksPerFuel, 6000, "정상 값 6000");
		helper.assertValueEqual(c.anchorRadius, defaults.anchorRadius, "반경 99는 범위 밖");
		helper.assertValueEqual(c.quarryTicksPerBlock, 8, "굴착기 8");
		// 경고: 글자 / 범위 / 정수 / 반경 / 모르는 키 / 모르는 구역
		helper.assertValueEqual(mixed.warnings().size(), 6, "경고 수: " + mixed.warnings());

		// 파일: 없으면 만들고, 값을 바꾼 파일은 그대로 읽고, 새 키만 빠졌으면 채운다, 깨진 파일은 건드리지 않는다
		Path dir = Files.createTempDirectory("sanupcraft-config-test");
		Path file = dir.resolve("sanupcraft.json");
		try {
			SanupcraftConfig.Loaded created = SanupcraftConfig.load(file);
			helper.assertTrue(created.wroteFile() && Files.isRegularFile(file), "설정 파일을 만들지 않음");
			helper.assertValueEqual(Files.readString(file, StandardCharsets.UTF_8), defaults.toJson(), "새 파일 내용 = 기본값");

			Files.writeString(file, "{\"quarry\": {\"ticksPerBlock\": 10}}", StandardCharsets.UTF_8);
			SanupcraftConfig.Loaded partial = SanupcraftConfig.load(file);
			helper.assertValueEqual(SanupcraftConfig.current().quarryTicksPerBlock, 10, "파일의 굴착기 값을 씀");
			helper.assertTrue(partial.missingKeys() && partial.wroteFile(), "빠진 키를 채워 다시 쓰지 않음");
			String rewritten = Files.readString(file, StandardCharsets.UTF_8);
			helper.assertTrue(rewritten.contains("\"ticksPerFuel\""), "채운 파일에 새 키가 없음");
			helper.assertValueEqual(SanupcraftConfig.parse(rewritten).config().quarryTicksPerBlock, 10, "다시 쓴 파일도 10 유지");

			Files.writeString(file, "{ broken", StandardCharsets.UTF_8);
			SanupcraftConfig.Loaded bad = SanupcraftConfig.load(file);
			helper.assertFalse(bad.warnings().isEmpty(), "깨진 파일에 경고가 없음");
			helper.assertFalse(bad.wroteFile(), "깨진 파일을 덮어씀");
			helper.assertValueEqual(Files.readString(file, StandardCharsets.UTF_8), "{ broken", "깨진 파일 내용 보존");
			helper.assertValueEqual(SanupcraftConfig.current().quarryTicksPerBlock, defaults.quarryTicksPerBlock, "깨진 파일은 기본값 사용");
		} finally {
			SanupcraftConfig.resetToDefaults();
			Files.deleteIfExists(file);
			Files.deleteIfExists(dir);
		}
		helper.succeed();
	}

	// ---- 증기 계기판 (B3) ----

	/** 신호 곡선: 비면 0, 조금이라도 있으면 1, 가득이면 15. */
	@GameTest
	public void gaugeSignalCurve(GameTestHelper helper) {
		helper.assertValueEqual(SteamGaugeBlockEntity.signalFor(0, 200), 0, "빔");
		helper.assertValueEqual(SteamGaugeBlockEntity.signalFor(1, 200), 1, "조금");
		helper.assertValueEqual(SteamGaugeBlockEntity.signalFor(100, 200), 8, "절반");
		helper.assertValueEqual(SteamGaugeBlockEntity.signalFor(199, 200), 14, "거의 가득");
		helper.assertValueEqual(SteamGaugeBlockEntity.signalFor(200, 200), 15, "가득");
		helper.assertValueEqual(SteamGaugeBlockEntity.signalFor(5, 0), 0, "용량 0");
		helper.succeed();
	}

	/** 최근 1초 계량기: 20틱 안에 들어온 것만 센다. */
	@GameTest
	public void flowMeterCountsLastSecond(GameTestHelper helper) {
		SteamFlowMeter meter = new SteamFlowMeter();
		helper.assertValueEqual(meter.lastSecond(100), 0, "처음");
		meter.add(100, 10);
		meter.add(105, 5);
		helper.assertValueEqual(meter.lastSecond(110), 15, "10틱 뒤");
		helper.assertValueEqual(meter.lastSecond(119), 15, "19틱 뒤 (아직 창 안)");
		helper.assertValueEqual(meter.lastSecond(120), 5, "20틱 뒤 첫 묶음이 빠짐");
		helper.assertValueEqual(meter.lastSecond(125), 0, "25틱 뒤");
		meter.add(300, 7);
		helper.assertValueEqual(meter.lastSecond(400), 0, "한참 뒤");
		helper.succeed();
	}

	/** 기계에 붙인 계기판: 저장률이 신호가 되고, 눈금판 반대쪽 블록만 읽는다. 읽을 게 없으면 0. */
	@GameTest
	public void gaugeReadsAttachedMachine(GameTestHelper helper) {
		BlockPos crusherPos = new BlockPos(2, 2, 2);
		BlockPos gaugePos = crusherPos.above();
		helper.setBlock(crusherPos, ModMachines.CRUSHER);
		helper.setBlock(gaugePos, ModPower.STEAM_GAUGE.defaultBlockState().setValue(SteamGaugeBlock.FACING, Direction.UP));
		MachineBlockEntity crusher = helper.getBlockEntity(crusherPos, MachineBlockEntity.class);
		SteamGaugeBlockEntity gauge = helper.getBlockEntity(gaugePos, SteamGaugeBlockEntity.class);

		helper.assertTrue(gauge.target() == crusher, "붙은 분쇄기를 못 읽음");
		helper.assertValueEqual(gauge.signal(), 0, "빈 기계");
		crusher.acceptSteam(MachineBlockEntity.STEAM_CAPACITY / 2);
		helper.assertValueEqual(gauge.signal(), 8, "절반 찬 기계");
		helper.assertValueEqual(gauge.target().steamReceivedLastSecond(), MachineBlockEntity.STEAM_CAPACITY / 2, "최근 1초 들어온 양");
		crusher.acceptSteam(MachineBlockEntity.STEAM_CAPACITY);
		helper.assertValueEqual(gauge.signal(), 15, "가득 찬 기계");
		helper.assertValueEqual(gauge.target().steamStored(), MachineBlockEntity.STEAM_CAPACITY, "저장량");

		// 방향을 돌리면 (눈금판이 아래, 읽는 쪽이 위) 위의 빈 칸이라 읽을 게 없다
		helper.setBlock(gaugePos, ModPower.STEAM_GAUGE.defaultBlockState().setValue(SteamGaugeBlock.FACING, Direction.DOWN));
		helper.assertValueEqual(helper.getBlockEntity(gaugePos, SteamGaugeBlockEntity.class).signal(), 0, "다른 쪽을 보는 계기판");
		helper.succeed();
	}

	/** 분출구에 붙은 계기판은 만든 증기를 센다. 빈손 우클릭은 성공한다 (붙은 게 있든 없든). */
	@GameTest
	public void gaugeReadsVentAndAnswersClick(GameTestHelper helper) {
		BlockPos ventPos = new BlockPos(2, 2, 2);
		helper.setBlock(ventPos, ModPower.GEOTHERMAL_VENT);
		helper.setBlock(ventPos.north(), Blocks.LAVA);
		helper.setBlock(ventPos.south(), Blocks.LAVA);
		BlockPos gaugePos = ventPos.east();
		helper.setBlock(gaugePos, ModPower.STEAM_GAUGE.defaultBlockState().setValue(SteamGaugeBlock.FACING, Direction.EAST));
		GeothermalVentBlockEntity vent = helper.getBlockEntity(ventPos, GeothermalVentBlockEntity.class);
		SteamGaugeBlockEntity gauge = helper.getBlockEntity(gaugePos, SteamGaugeBlockEntity.class);
		vent.work(helper.getLevel(), helper.absolutePos(ventPos), true);

		helper.assertTrue(gauge.target() == vent, "붙은 분출구를 못 읽음");
		helper.assertValueEqual(gauge.target().steamReceivedLastSecond(), 2 * GeothermalVentBlockEntity.STEAM_PER_LAVA, "분출구가 만든 양");
		helper.assertValueEqual(gauge.signal(), 1, "조금 찬 분출구");

		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockState state = helper.getBlockState(gaugePos);
		BlockPos absGauge = helper.absolutePos(gaugePos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absGauge), Direction.EAST, absGauge, false);
		helper.assertValueEqual(state.useWithoutItem(helper.getLevel(), player, hit), InteractionResult.SUCCESS, "붙은 것이 있을 때 우클릭");
		helper.setBlock(ventPos, Blocks.AIR);
		helper.assertTrue(gauge.target() == null, "사라진 블록을 계속 읽음");
		helper.assertValueEqual(state.useWithoutItem(helper.getLevel(), player, hit), InteractionResult.SUCCESS, "붙은 것이 없을 때 우클릭");
		helper.succeed();
	}

	/** 진짜 비교기가 계기판 신호를 받는다: 증기가 차면 비교기 출력이 따라 오른다. */
	@GameTest(maxTicks = 100)
	public void gaugeDrivesComparator(GameTestHelper helper) {
		BlockPos crusherPos = new BlockPos(2, 2, 2);
		BlockPos gaugePos = crusherPos.above();
		BlockPos comparatorPos = gaugePos.south();
		helper.setBlock(crusherPos, ModMachines.CRUSHER);
		helper.setBlock(gaugePos, ModPower.STEAM_GAUGE.defaultBlockState().setValue(SteamGaugeBlock.FACING, Direction.UP));
		helper.setBlock(comparatorPos.below(), Blocks.STONE);
		// 비교기는 바라보는 쪽(북)의 블록 = 계기판에서 신호를 읽는다
		helper.setBlock(comparatorPos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
		MachineBlockEntity crusher = helper.getBlockEntity(crusherPos, MachineBlockEntity.class);

		helper.runAfterDelay(10, () -> {
			helper.assertValueEqual(helper.getBlockEntity(comparatorPos, ComparatorBlockEntity.class).getOutputSignal(), 0, "빈 기계의 비교기 출력");
			crusher.acceptSteam(MachineBlockEntity.STEAM_CAPACITY);
			helper.runAfterDelay(12, () -> {
				helper.assertValueEqual(helper.getBlockEntity(comparatorPos, ComparatorBlockEntity.class).getOutputSignal(), 15, "가득 찬 기계의 비교기 출력");
				helper.succeed();
			});
		});
	}

	/** 레시피가 읽힌다. */
	@GameTest
	public void gaugeRecipeExists(GameTestHelper helper) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Sanupcraft.id("steam_gauge"));
		helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(key).isPresent(), "계기판 레시피가 없음");
		helper.succeed();
	}
}
