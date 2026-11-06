package io.github.yhyeop.sanupcraft.playtest;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import io.github.yhyeop.sanupcraft.boss.BossTier;
import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.fluid.ModFluid;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.ProductionGear;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.gear.backpack.ModBackpack;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.power.ModPower;
import io.github.yhyeop.sanupcraft.production.grinder.ModGrinder;
import io.github.yhyeop.sanupcraft.server.GuideBook;
import io.github.yhyeop.sanupcraft.server.ModServer;
import io.github.yhyeop.sanupcraft.production.ModProduction;
import io.github.yhyeop.sanupcraft.industry.ModIndustry;
import io.github.yhyeop.sanupcraft.transport.ModTransport;
import io.github.yhyeop.sanupcraft.storage.ModStorage;

/**
 * 싱글 실테스트용: {@code /sanupcraft playtest <단계>} (OP). 그 단계를 시험하는 데 필요한 장비 · 재료를 주고,
 * 네더 · 엔드 단계는 그 차원의 안전한 발판으로 보낸다. 체크리스트는 docs/PLAYTEST.md.
 */
public final class PlaytestCommand {
	/** 실테스트 단계. 순서가 곧 진행 순서. */
	public enum Stage {
		START("바닐라 초반 → 분쇄기 만들기", null, PlaytestCommand::kitStart),
		FACTORY("공장 1단계: 분쇄 · 합금 · 강철", null, PlaytestCommand::kitFactory),
		BOSS1("녹슨 수호자", null, PlaytestCommand::kitBoss1),
		STEAM("보일러 · 파이프 · 벨트 · 분류기 · 굴착기", null, PlaytestCommand::kitSteam),
		NETHER("내열 한 벌로 네더 탐험", Level.NETHER, PlaytestCommand::kitNether),
		BOSS2("네더 제단 · 용광로 파수꾼", Level.NETHER, PlaytestCommand::kitBoss2),
		FORGE("심장 제련로 · 고대 잔해", Level.NETHER, PlaytestCommand::kitForge),
		OUTPOST("네더 전초기지: 지열 분출구 · 내열 드릴 굴착기 · 엔더 닻", Level.NETHER, PlaytestCommand::kitOutpost),
		HOME("오버월드로 돌아오기", Level.OVERWORLD, PlaytestCommand::kitHome),
		END("엔드 · 엔더 운반기", Level.END, PlaytestCommand::kitEnd),
		SERVER("서버 기능: 팀 · 보스 기록판 · 공용 목표판 · 설명서", null, PlaytestCommand::kitServer),
		// 확장 M9a
		PRODUCTION("생산 · 장비: 작물 수확기 · 벌목기 · 증기 드릴 · 전기톱 · 제트팩", null, PlaytestCommand::kitProduction),
		// M9b 몹 분쇄기 · 경험치 탱크 · 배낭
		PRODUCTION2("생산 2: 몹 분쇄기 · 경험치 탱크 · 배낭", null, PlaytestCommand::kitProduction2),
		INDUSTRY("산업 규모: 대형 보일러 · 증기 기관차 · 화물 열차", null, PlaytestCommand::kitIndustry),
		STORAGE("보관 · 제작: 서랍 · 창고 네트워크 · 자동 제작", null, PlaytestCommand::kitStorage),
		// M5 네더 심화 2
		NETHER2("네더 심화: 영혼 화로 · 고압 증기 · 정밀 조립기 · 터빈 · 피글린 교역 · 잔해 탐지기 · 용암 증기선", Level.NETHER, PlaytestCommand::kitNether2),
		// C3 강화 소환석
		TIERS("강화 소환석: +1 ~ +3 소환석 · 재료 · 강철 · 내열 한 벌", null, PlaytestCommand::kitTiers),
		// M8 묶음 B
		INDUSTRY2("산업 규모 2: 용광로 탑 · 세척기 · 고압 정련기 · 정제 가루", null, PlaytestCommand::kitIndustry2),
		// M6 엔드 최종
		END2("엔드 최종: 공허 거상 · 공허 응축기 · 공허 장비 · 후렴 수확기 · 차원 창고 · 겉날개 가속 · 숨결 수집기", Level.END, PlaytestCommand::kitEnd2),
		// M8 I4
		WRENCH("렌치 · 면 설정: 돌리기 · 내용물째 집기 · 기계 면마다 넣기 / 빼기 / 막기", null, PlaytestCommand::kitWrench),
		// 확장 3 D1 새 광석
		ORES("새 광석: 크롬철석 · 보크사이트 · 원석 · 블록 · 가공 기계", null, PlaytestCommand::kitOres),
		// 확장 3 D3 · D5
		BLAST("증기 발파 · 위험 요소: 발파 장약 · 원격 기폭기 · 유독 가스 · 무른 바위", null, PlaytestCommand::kitBlast),
		// 확장 3 D2 광맥 탐사
		VEINS("광맥 탐사: 광맥 탐사기 · 굴착기 · 보일러 · 파이프", null, PlaytestCommand::kitVeins),
		// M13 전투 (C1 무기 · C5 협동)
		COMBAT("전투: 강철 창 · 증기 해머 · 화염 방사기 · 전투 신호기 · 쓰러짐 · 응급 증기 주사기", null, PlaytestCommand::kitCombat),
		// 확장 3 G1 · G2 합금 장비
		ALLOYS("합금 장비: 청동 · 크롬강 주괴 (합금로 · 제작대), 청동 · 크롬강 장비, 강철 장비 올리기", null, PlaytestCommand::kitAlloys),
		// 확장 3 C2 새 몹 · 정예
		MOBS("새 몹 · 정예: 녹슨 자동인형 · 잿불 정령 · 공허 그림자 (생성 알 · 정예 알 · 막는 장비)", null, PlaytestCommand::kitMobs);

		final String description;
		final ResourceKey<Level> dimension;
		final Consumer<ServerPlayer> kit;

		Stage(String description, ResourceKey<Level> dimension, Consumer<ServerPlayer> kit) {
			this.description = description;
			this.dimension = dimension;
			this.kit = kit;
		}

		public String id() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private PlaytestCommand() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> playtest = Commands.literal("playtest");
		for (Stage stage : Stage.values()) {
			playtest.then(Commands.literal(stage.id()).executes(c -> {
				ServerPlayer player = c.getSource().getPlayerOrException();
				apply(player, stage);
				c.getSource().sendSuccess(() -> Component.translatable("commands.sanupcraft.playtest.done", stage.id() + " · " + stage.description), false);
				return 1;
			}));
		}
		playtest.executes(c -> {
			String stages = Arrays.stream(Stage.values()).map(Stage::id).collect(Collectors.joining(", "));
			c.getSource().sendFailure(Component.translatable("commands.sanupcraft.playtest.unknown", stages));
			return 0;
		});
		// 뿌리(sanupcraft)에는 권한을 걸지 않고 하위 명령마다 건다 (M10: 팀 명령은 누구나 쓴다)
		dispatcher.register(Commands.literal("sanupcraft")
				.then(playtest.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))));
	}

	/** 단계 적용: 필요하면 차원 이동 → 장비 · 재료 지급. HOME은 이미 오버월드여도 월드 스폰으로 보낸다. */
	public static void apply(ServerPlayer player, Stage stage) {
		if (stage.dimension != null && (player.level().dimension() != stage.dimension || stage == Stage.HOME)) {
			ServerLevel target = player.level().getServer().getLevel(stage.dimension);
			if (target != null) {
				BlockPos spot = destination(target, player, stage);
				player.teleportTo(target, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.<Relative>of(), player.getYRot(), 0.0F, true);
			}
		}
		stage.kit.accept(player);
	}

	/** 그 단계에서 플레이어가 설 자리. 네더 · 엔드는 흑요석 발판을 깔고, 오버월드는 월드 스폰의 지표면이다. */
	private static BlockPos destination(ServerLevel target, ServerPlayer player, Stage stage) {
		if (stage.dimension == Level.END) {
			return safePlatform(target, new BlockPos(100, 64, 0));
		}
		if (stage.dimension == Level.OVERWORLD) {
			BlockPos spawn = target.getRespawnData().pos();
			target.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
			BlockPos surface = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
			// 물 · 용암 위면 그 자리에 발판을 깐다
			return target.getBlockState(surface.below()).getFluidState().isEmpty() ? surface : safePlatform(target, surface);
		}
		return safePlatform(target, new BlockPos(player.getBlockX() / 8, 64, player.getBlockZ() / 8));
	}

	/** 그 자리에 흑요석 5x5 발판을 깔고 위 4칸을 비운다. 플레이어가 설 자리를 돌려준다. */
	public static BlockPos safePlatform(ServerLevel level, BlockPos center) {
		level.getChunk(center.getX() >> 4, center.getZ() >> 4);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlockAndUpdate(center.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
				for (int dy = 0; dy < 4; dy++) {
					level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
		return center;
	}

	// ---- 꾸러미 ----

	private static void kitStart(ServerPlayer p) {
		give(p, Items.IRON_PICKAXE, 1);
		give(p, Items.RAW_IRON, 32);
		give(p, Items.IRON_INGOT, 16);
		give(p, Items.FLINT, 4);
		give(p, Items.COBBLESTONE, 64);
		give(p, Items.COAL, 32);
		give(p, Items.CRAFTING_TABLE, 1);
		give(p, Items.FURNACE, 2);
		give(p, Items.REDSTONE, 4);
		give(p, Items.OAK_PLANKS, 16);
		food(p);
	}

	private static void kitFactory(ServerPlayer p) {
		give(p, ModMachines.CRUSHER.asItem(), 1);
		give(p, ModMachines.ALLOY_SMELTER.asItem(), 1);
		give(p, ModMachines.PRESS.asItem(), 1);
		give(p, ModMachines.ASSEMBLER.asItem(), 1);
		give(p, Items.FURNACE, 2);
		give(p, Items.HOPPER, 10);
		give(p, Items.CHEST, 4);
		give(p, Items.RAW_IRON, 64);
		give(p, Items.RAW_COPPER, 32);
		give(p, Items.COAL, 64);
		give(p, Items.COAL, 64);
		give(p, Items.COBBLESTONE, 64);
		give(p, Items.GRAVEL, 32);
		// 기계 업그레이드: 속도 · 효율 · 두 배 각 2 (같은 것 둘이면 효과 두 번)
		give(p, ModMachines.SPEED_UPGRADE, 2);
		give(p, ModMachines.EFFICIENCY_UPGRADE, 2);
		give(p, ModMachines.DOUBLE_UPGRADE, 2);
		food(p);
	}

	private static void kitBoss1(ServerPlayer p) {
		equip(p, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		give(p, SteelGear.STEEL_SWORD, 1);
		give(p, SteelGear.STEEL_HAMMER, 1);
		give(p, Items.SHIELD, 1);
		give(p, ModBoss.GUARDIAN_ALTAR.asItem(), 1);
		give(p, ModBoss.GUARDIAN_SUMMONER, 4);
		// 증기 장비: 가득 찬 증기 통 · 부츠로 바로 써 볼 수 있게 (충전대는 steam 꾸러미)
		give(p, SteamGear.RIVET_GUN, 1);
		give(p, SteamGear.RIVET, 64);
		give(p, charged(SteamGear.STEAM_CANISTER));
		give(p, charged(SteamGear.STEAM_CANISTER));
		give(p, charged(SteamGear.STEAM_BOOTS));
		food(p);
	}

	private static void kitSteam(ServerPlayer p) {
		give(p, ModPower.BOILER.asItem(), 2);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, Items.WATER_BUCKET, 2);
		give(p, Items.COAL, 32);
		give(p, ModLogistics.BELT_MOTOR.asItem(), 1);
		give(p, ModLogistics.BELT.asItem(), 32);
		give(p, ModLogistics.BELT_SORTER.asItem(), 2);
		give(p, ModLogistics.FILTER_HOPPER.asItem(), 2);
		give(p, ModLogistics.ORE_MARKER, 1);
		give(p, ModPower.STEAM_CHARGER.asItem(), 1);
		give(p, SteamGear.STEAM_CANISTER, 2);
		give(p, SteamGear.STEAM_BOOTS, 1);
		give(p, ModMachines.CRUSHER.asItem(), 1);
		give(p, ModMachines.ASSEMBLER.asItem(), 1);
		give(p, ModMining.STEAM_QUARRY.asItem(), 1);
		give(p, Items.HOPPER, 8);
		give(p, Items.CHEST, 4);
		give(p, Items.RAW_IRON, 64);
		give(p, ModItems.GUARDIAN_CORE, 3);
		// 펌프 · 탱크: 물 위에 펌프, 옆에 탱크 · 보일러
		give(p, ModFluid.PUMP.asItem(), 1);
		give(p, ModFluid.TANK.asItem(), 2);
		give(p, Items.BUCKET, 1);
		give(p, ModMachines.SPEED_UPGRADE, 1);
		give(p, ModMachines.EFFICIENCY_UPGRADE, 1);
		food(p);
	}

	private static void kitNether(ServerPlayer p) {
		equip(p, HeatproofGear.HEATPROOF_HELMET, HeatproofGear.HEATPROOF_CHESTPLATE, HeatproofGear.HEATPROOF_LEGGINGS, HeatproofGear.HEATPROOF_BOOTS);
		give(p, HeatproofGear.HEATPROOF_SWORD, 1);
		give(p, Items.DIAMOND_PICKAXE, 1);
		give(p, Items.BOW, 1);
		give(p, Items.ARROW, 64);
		food(p);
	}

	private static void kitBoss2(ServerPlayer p) {
		kitNether(p);
		give(p, ModBoss.GUARDIAN_ALTAR.asItem(), 1);
		give(p, ModBoss.WARDEN_SUMMONER, 4);
	}

	private static void kitForge(ServerPlayer p) {
		give(p, ModMachines.HEART_FORGE.asItem(), 1);
		give(p, Items.ANCIENT_DEBRIS, 8);
		give(p, Items.BLAZE_ROD, 8);
		give(p, Items.COAL, 32);
		give(p, Items.HOPPER, 2);
		give(p, Items.CHEST, 1);
		give(p, ModItems.WARDEN_HEART, 1);
		food(p);
	}

	/** 네더 전초기지: 용암 옆 지열 분출구 → 파이프 → 내열 드릴 굴착기 · 심장 제련로, 송신기로 오버월드에 보내고 엔더 닻으로 붙잡아 둔다. */
	private static void kitOutpost(ServerPlayer p) {
		kitNether(p);
		give(p, ModPower.GEOTHERMAL_VENT.asItem(), 2);
		give(p, ModPower.STEAM_PIPE.asItem(), 32);
		give(p, ModMining.STEAM_QUARRY.asItem(), 1);
		give(p, ModMining.HEATPROOF_DRILL, 1);
		give(p, ModMachines.HEART_FORGE.asItem(), 1);
		give(p, ModLogistics.ENDER_SENDER.asItem(), 1);
		give(p, ModLogistics.ENDER_RECEIVER.asItem(), 1);
		give(p, Items.DYE.orange(), 2);
		give(p, ModLogistics.ENDER_ANCHOR.asItem(), 1);
		give(p, Items.POPPED_CHORUS_FRUIT, 16);
		give(p, Items.LAVA_BUCKET, 4);
		give(p, Items.CHEST, 2);
		give(p, Items.HOPPER, 4);
		give(p, Items.NETHER_QUARTZ_ORE, 8);
		give(p, Items.NETHER_GOLD_ORE, 8);
		give(p, ModMachines.CRUSHER.asItem(), 1);
		// 펌프로 용암을 탱크에 모아 분출구 옆에 두면 용암 원천으로 친다
		give(p, ModFluid.PUMP.asItem(), 1);
		give(p, ModFluid.TANK.asItem(), 2);
	}

	/** 네더에서 돌아온 뒤: 오버월드에서 파수꾼 소환석이 안 먹는 것(네더에서만 깨어난다)을 확인할 수 있게 제단과 소환석을 준다. */
	private static void kitHome(ServerPlayer p) {
		give(p, ModBoss.GUARDIAN_ALTAR.asItem(), 1);
		give(p, ModBoss.WARDEN_SUMMONER, 1);
		food(p);
	}

	private static void kitEnd(ServerPlayer p) {
		give(p, ModLogistics.ENDER_SENDER.asItem(), 2);
		give(p, ModLogistics.ENDER_RECEIVER.asItem(), 2);
		give(p, Items.DYE.red(), 4);
		give(p, Items.DYE.blue(), 4);
		give(p, Items.SHULKER_SHELL, 4);
		give(p, ModLogistics.ENDER_ANCHOR.asItem(), 1);
		give(p, Items.POPPED_CHORUS_FRUIT, 16);
		give(p, Items.DRAGON_BREATH, 1);
		give(p, Items.ENDER_PEARL, 16);
		give(p, Items.HOPPER, 4);
		give(p, Items.CHEST, 2);
		give(p, Items.COBBLESTONE, 64);
		give(p, Items.BOW, 1);
		give(p, Items.ARROW, 64);
		food(p);
	}

	/** M8 묶음 A: 외벽 33 + 제어기로 대형 보일러, 레일 위 기관차 + 화물칸 + 역 (꺼진 동력 레일 = 정거장). */
	private static void kitIndustry(ServerPlayer p) {
		give(p, ModIndustry.BIG_BOILER_CASING.asItem(), 40);
		give(p, ModIndustry.BIG_BOILER_CONTROLLER.asItem(), 1);
		give(p, Items.COAL, 64);
		give(p, Items.WATER_BUCKET, 2);
		give(p, ModFluid.PUMP.asItem(), 1);
		give(p, ModFluid.TANK.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, ModPower.STEAM_GAUGE.asItem(), 1);
		give(p, ModMachines.CRUSHER.asItem(), 1);
		give(p, Items.COMPARATOR, 1);
		give(p, Items.RAIL, 64);
		give(p, Items.RAIL, 64);
		give(p, Items.POWERED_RAIL, 8);
		give(p, Items.LEVER, 2);
		give(p, ModTransport.STEAM_LOCOMOTIVE_ITEM, 1);
		give(p, ModTransport.CARGO_WAGON_ITEM, 1);
		give(p, ModTransport.CARGO_WAGON_ITEM, 1);
		give(p, ModTransport.COUPLER, 4);
		give(p, ModTransport.CARGO_STATION.asItem(), 2);
		give(p, charged(SteamGear.STEAM_CANISTER));
		give(p, Items.HOPPER, 4);
		give(p, Items.CHEST, 2);
		give(p, Items.COBBLESTONE, 64);
		food(p);
	}

	private static void food(ServerPlayer p) {
		give(p, Items.COOKED_BEEF, 32);
	}

	/** 증기가 가득 찬 증기 통 · 부츠. */
	private static ItemStack charged(Item item) {
		ItemStack stack = new ItemStack(item);
		SteamCharge.set(stack, SteamCharge.capacity(stack));
		return stack;
	}

	private static void give(ServerPlayer p, Item item, int count) {
		give(p, new ItemStack(item, count));
	}

	private static void give(ServerPlayer p, ItemStack stack) {
		if (!p.getInventory().add(stack)) {
			Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), stack);
		}
	}

	private static void equip(ServerPlayer p, Item head, Item chest, Item legs, Item feet) {
		List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
		List<Item> items = List.of(head, chest, legs, feet);
		for (int i = 0; i < slots.size(); i++) {
			ItemStack old = p.getItemBySlot(slots.get(i));
			if (!old.isEmpty()) {
				give(p, old.copy());
			}
			p.setItemSlot(slots.get(i), new ItemStack(items.get(i)));
		}
	}

	/** M10 서버 기능: 기록판 · 목표판 · 설명서, 팀 잠금 시험용 엔더 수신기, 목표 재료, 기록용 보스 소환 꾸러미. */
	private static void kitServer(ServerPlayer p) {
		give(p, ModServer.BOSS_RECORD_BOARD.asItem(), 1);
		give(p, ModServer.GOAL_BOARD.asItem(), 2);
		give(p, GuideBook.create());
		give(p, ModLogistics.ENDER_RECEIVER.asItem(), 1);
		give(p, Items.HOPPER, 4);
		give(p, Items.CHEST, 2);
		give(p, ModItems.STEEL_INGOT, 64);
		give(p, ModItems.IRON_PLATE, 64);
		give(p, SteamGear.RIVET, 64);
		give(p, ModBoss.GUARDIAN_ALTAR.asItem(), 1);
		give(p, ModBoss.GUARDIAN_SUMMONER, 2);
		equip(p, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		give(p, SteelGear.STEEL_SWORD, 1);
		food(p);
	}

	/** 확장 M9a 생산 · 장비: 보일러 · 파이프로 수확기 · 벌목기를 돌리고, 가득 찬 드릴 · 전기톱 · 제트팩을 바로 써 본다. */
	private static void kitProduction(ServerPlayer p) {
		give(p, ModPower.BOILER.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, Items.WATER_BUCKET, 1);
		give(p, Items.COAL, 32);
		give(p, ModPower.STEAM_CHARGER.asItem(), 1);
		give(p, ModProduction.CROP_HARVESTER.asItem(), 1);
		give(p, ModProduction.TREE_FELLER.asItem(), 1);
		give(p, Items.HOPPER, 2);
		give(p, Items.CHEST, 2);
		give(p, Items.WHEAT_SEEDS, 16);
		give(p, Items.CARROT, 8);
		give(p, Items.BONE_MEAL, 32);
		give(p, Items.OAK_SAPLING, 8);
		give(p, Items.DIRT, 32);
		give(p, charged(ProductionGear.STEAM_DRILL));
		give(p, charged(ProductionGear.CHAINSAW));
		give(p, charged(ProductionGear.STEAM_JETPACK));
		give(p, SteamGear.STEAM_CANISTER, 2);
		food(p);
	}

	/** M9b 몹 분쇄기 · 경험치 탱크 · 배낭: 증기 설비, 몹 (소환 알), 받을 깔때기 · 상자, 병, 배낭 재료. */
	private static void kitProduction2(ServerPlayer p) {
		give(p, ModPower.BOILER.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, Items.COAL, 32);
		give(p, Items.WATER_BUCKET, 1);
		give(p, ModGrinder.MOB_GRINDER.asItem(), 2);
		give(p, ModGrinder.EXPERIENCE_TANK.asItem(), 2);
		give(p, Items.ZOMBIE_SPAWN_EGG, 8);
		give(p, Items.COW_SPAWN_EGG, 4);
		give(p, Items.HOPPER, 4);
		give(p, Items.CHEST, 4);
		give(p, Items.GLASS_BOTTLE, 16);
		give(p, ModBackpack.BACKPACK, 1);
		give(p, Items.LEATHER, 12);
		give(p, ModItems.IRON_PLATE, 8);
		give(p, ModItems.STEEL_PLATE, 8);
		give(p, Items.IRON_SWORD, 1);
		food(p);
	}

	/** M7 보관 · 제작: 서랍 · 업그레이드 · 창고 네트워크 · 단말 · 자동 제작대 · 재고 유지기 · 휴대 단말. */
	private static void kitStorage(ServerPlayer p) {
		give(p, ModStorage.IRON_DRAWER.asItem(), 4);
		give(p, ModStorage.STEEL_DRAWER.asItem(), 2);
		give(p, ModStorage.DRAWER_UPGRADE, 4);
		give(p, ModStorage.DRAWER_VOID_UPGRADE, 1);
		give(p, ModStorage.STORAGE_CABLE.asItem(), 32);
		give(p, ModStorage.STORAGE_TERMINAL.asItem(), 1);
		give(p, ModStorage.CRAFTING_TERMINAL.asItem(), 1);
		give(p, ModStorage.AUTOCRAFTER.asItem(), 2);
		give(p, ModStorage.STOCKER.asItem(), 1);
		give(p, ModStorage.PORTABLE_TERMINAL, 1);
		give(p, ModStorage.ENDER_PORTABLE_TERMINAL, 1);
		give(p, ModLogistics.ENDER_ANCHOR.asItem(), 1);
		give(p, Items.POPPED_CHORUS_FRUIT, 8);
		give(p, ModPower.BOILER.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, Items.WATER_BUCKET, 1);
		give(p, Items.COAL, 32);
		give(p, Items.OAK_LOG, 64);
		give(p, Items.COBBLESTONE, 256);
		give(p, Items.CHEST, 4);
		give(p, Items.HOPPER, 4);
		food(p);
	}

	/**
	 * M5 네더 심화 2: 영혼 흙 위 영혼 화로 → 고압 파이프 → 정밀 조립기 · 증기 터빈 → 보통 파이프 → 보통 기계,
	 * 금 분배기 · 교역 수집판(피글린 근처), 잔해 탐지기(가득 채움), 용암 증기선.
	 */
	private static void kitNether2(ServerPlayer p) {
		kitNether(p);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.SOUL_FURNACE.asItem(), 2);
		give(p, Items.SOUL_SOIL, 8);
		give(p, Items.BLAZE_POWDER, 32);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.HIGH_PRESSURE_PIPE.asItem(), 32);
		give(p, ModMachines.PRECISION_ASSEMBLER.asItem(), 1);
		give(p, ModItems.STEEL_PLATE, 8);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.GILDED_QUARTZ, 8);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.STEAM_TURBINE.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, ModMachines.CRUSHER.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.GOLD_DISPENSER.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.BARTER_COLLECTOR.asItem(), 1);
		give(p, Items.GOLD_INGOT, 32);
		give(p, Items.HOPPER, 2);
		give(p, Items.CHEST, 2);
		give(p, charged(io.github.yhyeop.sanupcraft.nether.ModNether.DEBRIS_SONAR));
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.LAVA_STEAMBOAT, 1);
		give(p, ModPower.STEAM_GAUGE.asItem(), 1);
	}

	/** C3 강화 소환석: +1 ~ +3 소환석과 재료를 받고, 강철 한 벌을 입고 (내열 한 벌은 가방에) 제단에서 단계마다 보스가 얼마나 세지는지 본다. */
	private static void kitTiers(ServerPlayer p) {
		equip(p, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		give(p, SteelGear.STEEL_SWORD, 1);
		give(p, SteelGear.STEEL_HAMMER, 1);
		give(p, Items.SHIELD, 1);
		give(p, HeatproofGear.HEATPROOF_HELMET, 1);
		give(p, HeatproofGear.HEATPROOF_CHESTPLATE, 1);
		give(p, HeatproofGear.HEATPROOF_LEGGINGS, 1);
		give(p, HeatproofGear.HEATPROOF_BOOTS, 1);
		give(p, HeatproofGear.HEATPROOF_SWORD, 1);
		give(p, ModBoss.GUARDIAN_ALTAR.asItem(), 2);
		for (int tier = 1; tier <= BossTier.MAX; tier++) {
			give(p, BossTier.stack(ModBoss.GUARDIAN_SUMMONER, tier));
		}
		give(p, BossTier.stack(ModBoss.WARDEN_SUMMONER, 1));
		give(p, BossTier.stack(ModBoss.WARDEN_SUMMONER, 3));
		// 작업대에서 직접 올려 볼 수 있게: 단계 0 소환석과 재료
		give(p, ModBoss.GUARDIAN_SUMMONER, 2);
		give(p, ModBoss.WARDEN_SUMMONER, 2);
		give(p, ModItems.CORE_SHARD, 12);
		give(p, ModItems.WARDEN_HEART, 3);
		give(p, Items.CRAFTING_TABLE, 1);
		// 공허 거상 강화 (엔드 제단): +2 하나 · 단계 0 하나 + 공허 핵 2
		give(p, BossTier.stack(io.github.yhyeop.sanupcraft.end.ModEnd.COLOSSUS_SUMMONER, 2));
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.COLOSSUS_SUMMONER, 1);
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.VOID_CORE, 2);
		food(p);
	}

	/** M8 묶음 B: 용광로 탑 한 벌 + 영혼 화로 · 고압 파이프, 세척기 + 펌프 · 탱크 · 물, 고압 정련기, 원석. */
	private static void kitIndustry2(ServerPlayer p) {
		give(p, io.github.yhyeop.sanupcraft.industry.ModFoundry.FURNACE_TOWER_CONTROLLER.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.industry.ModFoundry.HEATPROOF_BRICKS.asItem(), 23);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.SOUL_FURNACE.asItem(), 2);
		give(p, Items.SOUL_SOIL, 4);
		give(p, Items.BLAZE_POWDER, 64);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.HIGH_PRESSURE_PIPE.asItem(), 16);
		give(p, ModMachines.WASHER.asItem(), 1);
		give(p, ModMachines.HP_REFINER.asItem(), 1);
		give(p, ModFluid.PUMP.asItem(), 1);
		give(p, ModFluid.TANK.asItem(), 1);
		give(p, Items.WATER_BUCKET, 2);
		give(p, ModPower.BOILER.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 8);
		give(p, Items.COAL, 32);
		give(p, Items.RAW_IRON, 64);
		give(p, Items.RAW_COPPER, 32);
		give(p, Items.RAW_GOLD, 32);
		give(p, ModItems.IRON_DUST, 16);
		give(p, ModItems.COAL_DUST, 16);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.PRECISION_PART, 2);
		give(p, Items.HOPPER, 4);
		give(p, Items.CHEST, 2);
		give(p, Items.COMPARATOR, 1);
		give(p, ModPower.STEAM_GAUGE.asItem(), 1);
		food(p);
	}

	/**
	 * M6 엔드 최종: 원점에서 600칸 떨어진 엔드 공허 위 발판으로 옮긴다 (거상은 500칸 밖에서만 깨어남, 발판 옆이 공허라 응축기를 매단다).
	 * 제단 · 거상 소환석, 공허 응축기 · 고압 파이프 · 정밀 조립기 · 터빈, 네더라이트 한 벌 · 공허 핵(공허 장비 시험), 리벳총 · 리벳 · 증기 통,
	 * 겉날개, 후렴 수확기 · 엔드 돌 · 후렴 꽃, 차원 창고 · 염료, 숨결 수집기 · 유리병, 활 · 화살.
	 */
	private static void kitEnd2(ServerPlayer p) {
		ServerLevel end = p.level().getServer().getLevel(Level.END);
		if (end != null) {
			BlockPos spot = safePlatform(end, new BlockPos(600, 64, 0));
			p.teleportTo(end, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.<Relative>of(), p.getYRot(), 0.0F, true);
		}
		give(p, ModBoss.GUARDIAN_ALTAR.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.COLOSSUS_SUMMONER, 2);
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.VOID_CONDENSER.asItem(), 2);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.HIGH_PRESSURE_PIPE.asItem(), 16);
		give(p, ModMachines.PRECISION_ASSEMBLER.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.STEAM_TURBINE.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, ModPower.STEAM_GAUGE.asItem(), 1);
		for (Item netherite : List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.NETHERITE_SWORD)) {
			give(p, netherite, 1);
		}
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.VOID_CORE, 5);
		give(p, SteamGear.RIVET_GUN, 1);
		give(p, SteamGear.RIVET, 64);
		give(p, charged(SteamGear.STEAM_CANISTER));
		give(p, charged(SteamGear.STEAM_CANISTER));
		give(p, Items.ELYTRA, 1);
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.CHORUS_HARVESTER.asItem(), 1);
		give(p, Items.END_STONE, 16);
		give(p, Items.CHORUS_FLOWER, 4);
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.DIMENSIONAL_VAULT.asItem(), 2);
		give(p, Items.DYE.red(), 4);
		give(p, Items.DYE.blue(), 4);
		give(p, io.github.yhyeop.sanupcraft.end.ModEnd.BREATH_COLLECTOR.asItem(), 1);
		give(p, Items.GLASS_BOTTLE, 16);
		give(p, Items.HOPPER, 2);
		give(p, Items.CHEST, 2);
		give(p, Items.BOW, 1);
		give(p, Items.ARROW, 64);
		give(p, Items.COBBLESTONE, 64);
		food(p);
	}

	/** M8 I4: 렌치, 깔때기로 면 설정을 볼 기계 둘 · 재료 · 연료, 잠금을 볼 엔더 수신기. */
	private static void kitWrench(ServerPlayer p) {
		give(p, io.github.yhyeop.sanupcraft.wrench.ModWrench.WRENCH, 1);
		give(p, ModMachines.CRUSHER.asItem(), 2);
		give(p, ModMachines.PRESS.asItem(), 1);
		give(p, Items.HOPPER, 6);
		give(p, Items.CHEST, 4);
		give(p, Items.RAW_IRON, 64);
		give(p, Items.COAL, 32);
		give(p, io.github.yhyeop.sanupcraft.logistics.ModLogistics.ENDER_RECEIVER.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.logistics.ModLogistics.BELT.asItem(), 8);
		food(p);
	}

	/** 확장 3 D1: 새 광석 · 원석 · 블록, 가공 기계(분쇄기 · 세척기 · 고압 정련기)와 물 · 증기 설비, 곡괭이 둘. */
	private static void kitOres(ServerPlayer p) {
		give(p, Items.IRON_PICKAXE, 1);
		give(p, Items.STONE_PICKAXE, 1);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.DEEPSLATE_CHROMITE_ORE.asItem(), 8);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.BAUXITE_ORE.asItem(), 8);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.RAW_CHROMIUM, 32);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.RAW_ALUMINIUM, 32);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.RAW_CHROMIUM_BLOCK.asItem(), 2);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.RAW_ALUMINIUM_BLOCK.asItem(), 2);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.CHROMIUM_BLOCK.asItem(), 2);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.ALUMINIUM_BLOCK.asItem(), 2);
		give(p, ModMachines.CRUSHER.asItem(), 1);
		give(p, ModMachines.WASHER.asItem(), 1);
		give(p, ModMachines.HP_REFINER.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.HIGH_PRESSURE_PIPE.asItem(), 16);
		give(p, io.github.yhyeop.sanupcraft.nether.ModNether.SOUL_FURNACE.asItem(), 1);
		give(p, Items.SOUL_SOIL, 4);
		give(p, Items.BLAZE_POWDER, 32);
		give(p, ModFluid.PUMP.asItem(), 1);
		give(p, ModFluid.TANK.asItem(), 1);
		give(p, Items.WATER_BUCKET, 2);
		give(p, Items.FURNACE, 2);
		give(p, Items.BLAST_FURNACE, 1);
		give(p, Items.COAL, 64);
		give(p, io.github.yhyeop.sanupcraft.logistics.ModLogistics.ORE_MARKER, 1);
		give(p, Items.HOPPER, 4);
		give(p, Items.CHEST, 2);
		food(p);
	}

	/** 확장 3 D3 · D5: 발파 장약 · 원격 기폭기 · 레드스톤, 가스에 불을 붙여 볼 불씨, 무른 바위 · 곡괭이. 가스는 `/setblock ~ ~ ~ sanupcraft:toxic_gas`로 깐다. */
	private static void kitBlast(ServerPlayer p) {
		give(p, io.github.yhyeop.sanupcraft.mining.blast.ModBlast.BLAST_CHARGE.asItem(), 16);
		give(p, io.github.yhyeop.sanupcraft.mining.blast.ModBlast.REMOTE_DETONATOR, 1);
		give(p, Items.REDSTONE_BLOCK, 4);
		give(p, Items.LEVER, 2);
		give(p, Items.DIAMOND_PICKAXE, 1);
		give(p, Items.FLINT_AND_STEEL, 1);
		give(p, Items.TORCH, 16);
		give(p, Items.LAVA_BUCKET, 1);
		give(p, Items.CHEST, 2);
		give(p, io.github.yhyeop.sanupcraft.factory.ModMachines.CRUSHER.asItem(), 1);
		give(p, io.github.yhyeop.sanupcraft.hazard.ModHazard.LOOSE_ROCK.asItem(), 8);
		give(p, Items.MILK_BUCKET, 2);
		food(p);
	}

	/** 확장 3 D2: 가득 찬 광맥 탐사기로 지도를 보고, 광맥 청크에 굴착기(보일러 · 파이프 · 위 상자)를 놓아 덤 광석이 나오는지 본다. */
	private static void kitVeins(ServerPlayer p) {
		give(p, charged(io.github.yhyeop.sanupcraft.resource.vein.ModVeins.VEIN_PROSPECTOR));
		give(p, ModMining.STEAM_QUARRY.asItem(), 2);
		give(p, ModPower.BOILER.asItem(), 2);
		give(p, ModPower.STEAM_PIPE.asItem(), 16);
		give(p, Items.WATER_BUCKET, 2);
		give(p, Items.COAL, 64);
		give(p, ModPower.STEAM_CHARGER.asItem(), 1);
		give(p, SteamGear.STEAM_CANISTER, 2);
		give(p, Items.HOPPER, 4);
		give(p, Items.CHEST, 4);
		give(p, Items.IRON_PICKAXE, 1);
		give(p, Items.COMPASS, 1);
		food(p);
	}

	/** M13 전투: 무기 셋(증기 · 연료 가득), 보일러로 돌리는 전투 신호기, 응급 주사기 둘, 증기 통, 시험할 몹 (소환 알). 쓰러짐은 두 사람 이상이 필요. */
	private static void kitCombat(ServerPlayer p) {
		give(p, io.github.yhyeop.sanupcraft.combat.ModCombat.STEEL_SPEAR, 1);
		give(p, charged(io.github.yhyeop.sanupcraft.combat.ModCombat.STEAM_HAMMER));
		give(p, charged(io.github.yhyeop.sanupcraft.combat.ModCombat.FLAMETHROWER));
		give(p, Items.BLAZE_POWDER, 32);
		give(p, Items.MAGMA_CREAM, 16);
		give(p, io.github.yhyeop.sanupcraft.combat.ModCombat.COMBAT_BEACON.asItem(), 1);
		give(p, ModPower.BOILER.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 8);
		give(p, ModPower.STEAM_GAUGE.asItem(), 1);
		give(p, Items.WATER_BUCKET, 1);
		give(p, Items.COAL, 32);
		give(p, charged(io.github.yhyeop.sanupcraft.combat.ModCombat.STEAM_SYRINGE));
		give(p, charged(io.github.yhyeop.sanupcraft.combat.ModCombat.STEAM_SYRINGE));
		give(p, charged(SteamGear.STEAM_CANISTER));
		give(p, ModPower.STEAM_CHARGER.asItem(), 1);
		give(p, Items.ZOMBIE_SPAWN_EGG, 8);
		give(p, Items.SKELETON_SPAWN_EGG, 4);
		give(p, Items.SHIELD, 1);
		equip(p, SteelGear.STEEL_HELMET, SteelGear.STEEL_CHESTPLATE, SteelGear.STEEL_LEGGINGS, SteelGear.STEEL_BOOTS);
		food(p);
	}

	/** 확장 3 G1 · G2: 합금 재료 · 합금로와 증기 설비, 청동 · 크롬강 한 벌씩, 올려 볼 마법 부여 강철 검 · 이름 붙은 강철 투구. */
	private static void kitAlloys(ServerPlayer p) {
		give(p, Items.COPPER_INGOT, 64);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.ALUMINIUM_INGOT, 24);
		give(p, io.github.yhyeop.sanupcraft.resource.ModResources.CHROMIUM_INGOT, 64);
		give(p, ModItems.STEEL_INGOT, 32);
		give(p, ModMachines.ALLOY_SMELTER.asItem(), 1);
		give(p, ModPower.BOILER.asItem(), 1);
		give(p, ModPower.STEAM_PIPE.asItem(), 8);
		give(p, Items.WATER_BUCKET, 2);
		give(p, Items.COAL, 32);
		give(p, Items.CRAFTING_TABLE, 1);
		give(p, Items.STICK, 16);
		for (Item item : new Item[] {io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_HELMET, io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_CHESTPLATE,
				io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_LEGGINGS, io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_BOOTS,
				io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_SWORD, io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_PICKAXE,
				io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_AXE, io.github.yhyeop.sanupcraft.gear.BronzeGear.BRONZE_SHOVEL,
				io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_HELMET, io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_CHESTPLATE,
				io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_LEGGINGS, io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_BOOTS,
				io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_SWORD, io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_HAMMER,
				io.github.yhyeop.sanupcraft.gear.ChromeSteelGear.CHROME_STEEL_EXCAVATOR}) {
			give(p, item, 1);
		}
		// 올리기 시험용: 날카로움 III 강철 검, 이름 붙은 강철 투구
		ItemStack sword = new ItemStack(SteelGear.STEEL_SWORD);
		sword.enchant(p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
		give(p, sword);
		ItemStack helmet = new ItemStack(SteelGear.STEEL_HELMET);
		helmet.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("아끼는 투구"));
		give(p, helmet);
		give(p, SteelGear.STEEL_HAMMER, 1);
		give(p, Items.STONE, 64);
		food(p);
	}

	/** 확장 3 C2: 새 몹 생성 알 (보통 4개씩 + 능력별 정예 알), 무기 · 눈덩이 · 물, 막는 장비 세 벌 (강철 = 녹, 내열 = 과열, 공허 = 침식). */
	private static void kitMobs(ServerPlayer p) {
		give(p, io.github.yhyeop.sanupcraft.mob.ModMobs.RUST_AUTOMATON_SPAWN_EGG, 4);
		give(p, io.github.yhyeop.sanupcraft.mob.ModMobs.EMBER_WISP_SPAWN_EGG, 4);
		give(p, io.github.yhyeop.sanupcraft.mob.ModMobs.VOID_SHADE_SPAWN_EGG, 4);
		give(p, io.github.yhyeop.sanupcraft.mob.ModMobs.eliteEgg(io.github.yhyeop.sanupcraft.mob.ModMobs.RUST_AUTOMATON_SPAWN_EGG, io.github.yhyeop.sanupcraft.mob.EliteAbility.SPEED));
		give(p, io.github.yhyeop.sanupcraft.mob.ModMobs.eliteEgg(io.github.yhyeop.sanupcraft.mob.ModMobs.EMBER_WISP_SPAWN_EGG, io.github.yhyeop.sanupcraft.mob.EliteAbility.REGEN));
		give(p, io.github.yhyeop.sanupcraft.mob.ModMobs.eliteEgg(io.github.yhyeop.sanupcraft.mob.ModMobs.VOID_SHADE_SPAWN_EGG, io.github.yhyeop.sanupcraft.mob.EliteAbility.REFLECT));
		give(p, SteelGear.STEEL_SWORD, 1);
		give(p, Items.BOW, 1);
		give(p, Items.ARROW, 64);
		give(p, Items.SHIELD, 1);
		give(p, Items.SNOWBALL, 32);
		give(p, Items.WATER_BUCKET, 2);
		give(p, SteelGear.STEEL_HELMET, 1);
		give(p, SteelGear.STEEL_CHESTPLATE, 1);
		give(p, SteelGear.STEEL_LEGGINGS, 1);
		give(p, SteelGear.STEEL_BOOTS, 1);
		give(p, HeatproofGear.HEATPROOF_HELMET, 1);
		give(p, HeatproofGear.HEATPROOF_CHESTPLATE, 1);
		give(p, HeatproofGear.HEATPROOF_LEGGINGS, 1);
		give(p, HeatproofGear.HEATPROOF_BOOTS, 1);
		give(p, io.github.yhyeop.sanupcraft.end.VoidGear.VOID_HELMET, 1);
		give(p, io.github.yhyeop.sanupcraft.end.VoidGear.VOID_CHESTPLATE, 1);
		give(p, io.github.yhyeop.sanupcraft.end.VoidGear.VOID_LEGGINGS, 1);
		give(p, io.github.yhyeop.sanupcraft.end.VoidGear.VOID_BOOTS, 1);
		food(p);
	}
}
