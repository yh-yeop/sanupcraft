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

import io.github.yhyeop.sanupcraft.boss.ModBoss;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteamCharge;
import io.github.yhyeop.sanupcraft.gear.SteamGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;
import io.github.yhyeop.sanupcraft.logistics.ModLogistics;
import io.github.yhyeop.sanupcraft.mining.ModMining;
import io.github.yhyeop.sanupcraft.power.ModPower;
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
		STORAGE("보관 · 제작: 서랍 · 창고 네트워크 · 자동 제작", null, PlaytestCommand::kitStorage);

		final String description;
		final ResourceKey<Level> dimension;
		final Consumer<ServerPlayer> kit;

		Stage(String description, ResourceKey<Level> dimension, Consumer<ServerPlayer> kit) {
			this.description = description;
			this.dimension = dimension;
			this.kit = kit;
		}

		public String id() {
			return name().toLowerCase();
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
		dispatcher.register(Commands.literal("sanupcraft")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(playtest));
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

	/** M7 보관 · 제작: 서랍 · 업그레이드 · 창고 네트워크 · 단말 · 자동 제작대 · 재고 유지기 · 휴대 단말. */
	private static void kitStorage(ServerPlayer p) {
		give(p, ModStorage.IRON_DRAWER.asItem(), 4);
		give(p, ModStorage.STEEL_DRAWER.asItem(), 2);
		give(p, ModStorage.DRAWER_UPGRADE, 4);
		give(p, ModStorage.DRAWER_VOID_UPGRADE, 1);
		give(p, ModStorage.STORAGE_CABLE.asItem(), 32);
		give(p, ModStorage.STORAGE_TERMINAL.asItem(), 1);
		give(p, ModStorage.CRAFTING_TERMINAL.asItem(), 1);
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
}
