package io.github.yhyeop.sanupcraft.test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.mining.QuarryBlockEntity;
import io.github.yhyeop.sanupcraft.playtest.PlaytestCommand;
import io.github.yhyeop.sanupcraft.resource.ModResources;
import io.github.yhyeop.sanupcraft.resource.vein.ModVeins;
import io.github.yhyeop.sanupcraft.resource.vein.ProspectorItem;
import io.github.yhyeop.sanupcraft.resource.vein.Vein;
import io.github.yhyeop.sanupcraft.resource.vein.VeinData;
import io.github.yhyeop.sanupcraft.resource.vein.VeinType;
import io.github.yhyeop.sanupcraft.resource.vein.Veins;

/** 확장 3 D2 검사: 광맥 탐사 — 시드로 정한 광맥 · 광맥 탐사기 · 굴착기 덤 · 남은 양 저장. */
public class SanupcraftD2GameTest {
	/** 시험이 광맥을 심는 데 쓰는, 실제 지형과 상관없는 먼 청크 (이웃 시험과 겹치지 않게 시험마다 다른 곳). */
	private static ChunkPos farChunk(int n) {
		return new ChunkPos(700_000 + n * 17, -700_000 - n * 13);
	}

	private static int count(Container container, Item item) {
		int sum = 0;
		for (int i = 0; i < container.getContainerSize(); i++) {
			if (container.getItem(i).is(item)) {
				sum += container.getItem(i).getCount();
			}
		}
		return sum;
	}

	// ---- 광맥 만들기 ----

	/** 같은 시드 · 청크 · 차원이면 늘 같은 광맥. 비율 · 종류 · 매장량이 설계대로. */
	@GameTest
	public void veinGenerationIsDeterministic(GameTestHelper helper) {
		SanupcraftConfig config = SanupcraftConfig.current();
		long seed = 123456789L;
		int veins = 0;
		int total = 0;
		Set<VeinType> seen = EnumSet.noneOf(VeinType.class);
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				ChunkPos chunk = new ChunkPos(x, z);
				Optional<Vein> a = Veins.generate(seed, chunk, Level.OVERWORLD);
				Optional<Vein> b = Veins.generate(seed, chunk, Level.OVERWORLD);
				helper.assertValueEqual(a, b, "같은 입력인데 다른 광맥 " + chunk);
				total++;
				if (a.isPresent()) {
					veins++;
					Vein vein = a.get();
					seen.add(vein.type());
					helper.assertTrue(vein.type().overworldWeight() > 0, "오버월드에 안 나올 종류: " + vein.type());
					helper.assertTrue(vein.total() >= config.veinMinAmount && vein.total() <= config.veinMaxAmount, "매장량 범위 밖: " + vein.total());
					helper.assertValueEqual(vein.remaining(), vein.total(), "처음엔 가득");
				}
			}
		}
		helper.assertTrue(veins >= total * 12 / 100 && veins <= total * 40 / 100, "광맥 청크 비율이 이상함: " + veins + " / " + total);
		helper.assertTrue(seen.containsAll(List.of(VeinType.IRON, VeinType.COPPER, VeinType.COAL)), "흔한 광맥이 안 나옴: " + seen);
		// 시드가 다르면 지도가 다르다
		int differ = 0;
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				ChunkPos chunk = new ChunkPos(x, z);
				if (!Veins.generate(seed, chunk, Level.OVERWORLD).equals(Veins.generate(seed + 1, chunk, Level.OVERWORLD))) {
					differ++;
				}
			}
		}
		helper.assertTrue(differ > 50, "다른 시드인데 거의 같음: " + differ);
		helper.succeed();
	}

	/** 네더는 석영 · 금 · 고대 잔해 (잔해는 40~120), 엔드는 광맥이 없다. */
	@GameTest
	public void netherAndEndVeins(GameTestHelper helper) {
		long seed = 987654321L;
		Set<VeinType> seen = EnumSet.noneOf(VeinType.class);
		for (int x = -25; x <= 25; x++) {
			for (int z = -25; z <= 25; z++) {
				ChunkPos chunk = new ChunkPos(x, z);
				helper.assertTrue(Veins.generate(seed, chunk, Level.END).isEmpty(), "엔드에 광맥이 생김");
				Optional<Vein> vein = Veins.generate(seed, chunk, Level.NETHER);
				if (vein.isPresent()) {
					VeinType type = vein.get().type();
					seen.add(type);
					helper.assertTrue(type.netherWeight() > 0, "네더에 안 나올 종류: " + type);
					if (type == VeinType.ANCIENT_DEBRIS) {
						helper.assertTrue(vein.get().total() >= 40 && vein.get().total() <= 120, "잔해 매장량: " + vein.get().total());
					}
				}
			}
		}
		helper.assertTrue(seen.contains(VeinType.QUARTZ) && seen.contains(VeinType.GOLD), "네더 흔한 광맥이 안 나옴: " + seen);
		helper.assertTrue(!Veins.hasVeins(Level.END), "엔드는 광맥 차원이 아님");
		helper.assertTrue(Veins.hasVeins(Level.OVERWORLD) && Veins.hasVeins(Level.NETHER), "오버월드 · 네더는 광맥 차원");
		// 종류마다 덤 아이템이 있다
		helper.assertValueEqual(VeinType.CHROMIUM.item(), ModResources.RAW_CHROMIUM, "크롬 광맥 덤");
		helper.assertValueEqual(VeinType.ANCIENT_DEBRIS.item(), Items.ANCIENT_DEBRIS, "잔해 광맥 덤");
		helper.succeed();
	}

	// ---- 남은 양 저장 ----

	/** 캔 양이 레벨 저장 데이터에 남고, 코덱으로 쓰고 읽어도 그대로이고, 바닥나면 더 안 나온다. */
	@GameTest
	public void veinRemainingPersists(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChunkPos chunk = farChunk(1);
		VeinData data = VeinData.get(level);
		data.put(chunk, new Vein(VeinType.GOLD, 5000, 300));
		helper.assertTrue(data.isDirty(), "저장 표시를 안 함");
		helper.assertTrue(VeinData.get(level) == data, "같은 레벨인데 저장 데이터가 다름");
		helper.assertValueEqual(Veins.draw(level, chunk, 120), 120, "캔 양");
		helper.assertValueEqual(Veins.get(level, chunk).remaining(), 180, "남은 양");
		helper.assertValueEqual(Veins.get(level, chunk).total(), 5000, "처음 양은 그대로");

		// 저장 -> 읽기
		var encoded = VeinData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
		VeinData loaded = VeinData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
		Vein back = loaded.get(chunk);
		helper.assertTrue(back != null, "읽은 데이터에 항목이 없음");
		helper.assertValueEqual(back, new Vein(VeinType.GOLD, 5000, 180), "읽은 항목");

		// 바닥나면 남은 만큼만, 그 뒤로는 0
		helper.assertValueEqual(Veins.draw(level, chunk, 1000), 180, "남은 만큼만 캠");
		helper.assertValueEqual(Veins.draw(level, chunk, 1), 0, "바닥난 광맥에서 캐짐");
		helper.assertTrue(Veins.get(level, chunk).depleted(), "바닥났다고 안 나옴");
		helper.assertTrue(Veins.rollBonus(level, chunk, RandomSource.create(1L), 100).isEmpty(), "바닥난 광맥이 덤을 줌");
		// 광맥이 없는 차원은 늘 비어 있다
		helper.assertTrue(Veins.draw(level, farChunk(99), 0) == 0, "0을 캐면 0");
		data.put(chunk, new Vein(VeinType.GOLD, 5000, 0)); // 정리 (바닥난 채로 둔다)
		helper.succeed();
	}

	/** 덤 확률은 설정대로 (15%): 4000번 굴려 600 근처, 100%면 매번, 0%면 한 번도 없음. 광맥 종류의 아이템이 나온다. */
	@GameTest
	public void bonusRollFollowsChance(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChunkPos chunk = farChunk(2);
		VeinData.get(level).put(chunk, new Vein(VeinType.COPPER, 1_000_000, 1_000_000));
		RandomSource random = RandomSource.create(20260611L);
		int hits = 0;
		for (int i = 0; i < 4000; i++) {
			ItemStack bonus = Veins.rollBonus(level, chunk, random, Veins.BONUS_PERCENT);
			if (!bonus.isEmpty()) {
				hits++;
				helper.assertTrue(bonus.is(Items.RAW_COPPER) && bonus.getCount() == 1, "덤은 구리 원석 1개여야 함: " + bonus);
			}
		}
		helper.assertTrue(hits > 450 && hits < 750, "덤 횟수가 15%에서 멀다: " + hits + " / 4000");
		helper.assertValueEqual(Veins.get(level, chunk).remaining(), 1_000_000 - hits, "덤 횟수만큼 줄어야 함");
		int always = 0;
		int never = 0;
		for (int i = 0; i < 200; i++) {
			always += Veins.rollBonus(level, chunk, random, 100).isEmpty() ? 0 : 1;
			never += Veins.rollBonus(level, chunk, random, 0).isEmpty() ? 0 : 1;
		}
		helper.assertValueEqual(always, 200, "100%인데 빠짐");
		helper.assertValueEqual(never, 0, "0%인데 나옴");
		VeinData.get(level).put(chunk, new Vein(VeinType.COPPER, 1_000_000, 0));
		helper.succeed();
	}

	// ---- 광맥 탐사기 ----

	/** 우클릭하면 증기 50을 쓰고 5x5 지도 + 지금 청크 요약이 나온다. 쿨다운 중 · 증기 부족 · 창작 모드. */
	@GameTest
	public void prospectorShowsMapAndUsesSteam(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
		ServerLevel level = helper.getLevel();
		ChunkPos here = ChunkPos.containing(player.blockPosition());
		VeinData.get(level).put(here, new Vein(VeinType.IRON, 6000, 4500));

		ItemStack empty = new ItemStack(ModVeins.VEIN_PROSPECTOR);
		SteamCharge.set(empty, 40);
		helper.assertTrue(ProspectorItem.scan(level, player, empty) == ProspectorItem.Result.NO_STEAM, "증기 40으로 쓰임");
		helper.assertValueEqual(SteamCharge.get(empty), 40, "실패했는데 증기가 빠짐");
		helper.assertTrue(!player.getCooldowns().isOnCooldown(empty), "실패했는데 쿨다운이 걸림");

		ItemStack prospector = new ItemStack(ModVeins.VEIN_PROSPECTOR);
		SteamCharge.set(prospector, SteamCharge.capacity(prospector));
		helper.assertValueEqual(SteamCharge.capacity(prospector), 500, "탐사기 저장량");
		helper.assertTrue(ProspectorItem.scan(level, player, prospector) == ProspectorItem.Result.SCANNED, "탐사가 안 됨");
		helper.assertValueEqual(SteamCharge.get(prospector), 450, "쓴 증기");
		helper.assertTrue(ProspectorItem.scan(level, player, prospector) == ProspectorItem.Result.COOLDOWN, "쿨다운 중에 쓰임");
		helper.assertValueEqual(SteamCharge.get(prospector), 450, "쿨다운 중에 증기가 빠짐");

		// 지도: 머리글 + 5줄, 가운데 줄 가운데 칸이 지금 청크 (철 75%)
		List<Component> lines = ProspectorItem.mapLines(level, here);
		helper.assertValueEqual(lines.size(), 6, "지도 줄 수 (머리글 + 5)");
		String mid = lines.get(3).getString();
		String cell = Component.translatable(VeinType.IRON.shortKey()).getString() + "75";
		helper.assertTrue(mid.contains(cell), "가운데 칸에 '" + cell + "'가 없음: " + mid);
		// 이웃 칸은 Veins.get과 같은 내용 (동쪽 이웃)
		Vein east = Veins.get(level, new ChunkPos(here.x() + 1, here.z()));
		String eastCell = east == null ? "·" : Component.translatable(east.type().shortKey()).getString() + east.percent();
		helper.assertTrue(mid.contains(eastCell), "동쪽 이웃 칸 '" + eastCell + "'가 없음: " + mid);
		// 요약
		String summary = ProspectorItem.summary(level, here).getString();
		helper.assertTrue(summary.contains("4500") && summary.contains("6000") && summary.contains("75"), "요약에 남은 양이 없음: " + summary);
		VeinData.get(level).put(here, new Vein(VeinType.IRON, 6000, 0));
		helper.assertTrue(lines.size() == ProspectorItem.mapLines(level, here).size(), "바닥난 청크도 칸은 그대로");
		helper.assertTrue(ProspectorItem.mapLines(level, here).get(3).getString().contains(Component.translatable(VeinType.IRON.shortKey()).getString() + "0"),
				"바닥난 광맥은 0%로 보임");

		// 창작 모드는 증기가 안 든다
		Player creative = helper.makeMockPlayer(GameType.CREATIVE);
		creative.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
		creative.getAbilities().instabuild = true; // 모의 플레이어는 게임 모드만 창작이라 능력을 직접 켠다
		ItemStack free = new ItemStack(ModVeins.VEIN_PROSPECTOR);
		creative.getCooldowns().removeCooldown(creative.getCooldowns().getCooldownGroup(free)); // 같은 사람 · 같은 쿨다운 묶음일 수 있다
		helper.assertTrue(ProspectorItem.scan(level, creative, free) == ProspectorItem.Result.SCANNED, "창작 모드에서 안 됨");
		helper.assertValueEqual(SteamCharge.get(free), 0, "창작 모드 증기");
		helper.succeed();
	}

	// ---- 굴착기 덤 ----

	/**
	 * 광맥 청크의 굴착기가 캘 때마다 덤 광석을 상자로 올려 보내고 남은 양을 줄인다. 바닥나면 더 안 나온다.
	 * 확률 100%로 돌린다 (3x3 돌 9칸, 매장량 6 → 덤 정확히 6, 바닥난 뒤 캔 3칸은 덤 없음).
	 */
	@GameTest(environment = "sanupcraft-test:alone_quarry", maxTicks = 300)
	public void quarryGetsVeinBonusUntilDepleted(GameTestHelper helper) {
		int oldChance = SanupcraftConfig.current().veinBonusPercent;
		BlockPos quarryPos = new BlockPos(3, 3, 3);
		ServerLevel level = helper.getLevel();
		ChunkPos chunk = ChunkPos.containing(helper.absolutePos(quarryPos));
		VeinData.get(level).put(chunk, new Vein(VeinType.IRON, 6, 6));
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(quarryPos.offset(dx, -1, dz), Blocks.STONE);
			}
		}
		helper.setBlock(quarryPos, ModMining.STEAM_QUARRY);
		helper.setBlock(quarryPos.above(), Blocks.CHEST);
		QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
		quarry.acceptSteam(QuarryBlockEntity.STEAM_CAPACITY);
		ChestBlockEntity chest = helper.getBlockEntity(quarryPos.above(), ChestBlockEntity.class);
		helper.succeedWhen(() -> {
			SanupcraftConfig.current().veinBonusPercent = 100; // 설정 시험이 중간에 기본값으로 돌려도 매 틱 다시 켠다
			helper.assertTrue(quarry.mined() >= 9, "아직 다 안 캠: " + quarry.mined());
			helper.assertValueEqual(quarry.veinBonus(), 6, "덤은 매장량 6만큼 (그 뒤로 더 캔 돌은 덤이 없음)");
			Vein vein = Veins.get(level, chunk);
			helper.assertValueEqual(vein.remaining(), 0, "광맥 남은 양");
			helper.assertValueEqual(vein.total(), 6, "광맥 처음 양은 그대로");
			helper.assertValueEqual(count(chest, Items.RAW_IRON), 6, "상자의 철 원석 (돌은 버림)");
			String info = Veins.describe(level, chunk, quarry.veinBonus()).getString();
			helper.assertTrue(info.contains(Component.translatable(VeinType.IRON.nameKey()).getString()), "상태 메시지에 광맥 이름이 없음: " + info);
			SanupcraftConfig.current().veinBonusPercent = oldChance;
		});
	}

	// ---- 등록 · 설정 · 꾸러미 ----

	/** 설정 기본값, 아이템 등록, 실테스트 꾸러미(`veins`). */
	@GameTest
	public void configAndKit(GameTestHelper helper) {
		SanupcraftConfig defaults = new SanupcraftConfig();
		helper.assertValueEqual(defaults.veinChunkPercent, 25, "광맥 청크 비율");
		helper.assertValueEqual(defaults.veinMinAmount, 2000, "최소 매장량");
		helper.assertValueEqual(defaults.veinMaxAmount, 8000, "최대 매장량");
		helper.assertValueEqual(defaults.veinBonusPercent, 15, "덤 확률");
		helper.assertValueEqual(defaults.prospectorSteam, 50, "탐사기 증기");
		helper.assertTrue(defaults.toJson().contains("\"veins\""), "설정 파일에 veins 구역이 없음");
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Sanupcraft.id("vein_prospector")) != null, "발전 과제가 없음");

		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		PlaytestCommand.apply(player, PlaytestCommand.Stage.VEINS);
		boolean found = false;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ModVeins.VEIN_PROSPECTOR)) {
				found = true;
				helper.assertValueEqual(SteamCharge.get(stack), 500, "꾸러미 탐사기는 가득 차 있어야 함");
			}
		}
		helper.assertTrue(found, "꾸러미에 광맥 탐사기가 없음");
		helper.assertTrue(player.getInventory().countItem(ModMining.STEAM_QUARRY.asItem()) > 0, "꾸러미에 굴착기가 없음");
		helper.succeed();
	}
}
