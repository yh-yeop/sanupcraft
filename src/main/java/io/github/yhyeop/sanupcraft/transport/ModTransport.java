package io.github.yhyeop.sanupcraft.transport;

import java.util.Set;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 증기 기관차 · 화물 열차 (M8): 기관차 · 화물칸 · 연결 고리 · 적재 · 하역 역. */
public final class ModTransport {
	private static final ResourceKey<EntityType<?>> LOCOMOTIVE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("steam_locomotive"));
	private static final ResourceKey<EntityType<?>> CARGO_WAGON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("cargo_wagon"));

	/** 크기 · 추적 거리는 바닐라 화로 수레와 같다. */
	public static final EntityType<SteamLocomotive> STEAM_LOCOMOTIVE = Registry.register(BuiltInRegistries.ENTITY_TYPE, LOCOMOTIVE_KEY,
			EntityType.Builder.<SteamLocomotive>of(SteamLocomotive::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.98F, 0.7F)
					.passengerAttachments(0.1875F)
					.clientTrackingRange(8)
					.build(LOCOMOTIVE_KEY));

	public static final EntityType<CargoWagon> CARGO_WAGON = Registry.register(BuiltInRegistries.ENTITY_TYPE, CARGO_WAGON_KEY,
			EntityType.Builder.<CargoWagon>of(CargoWagon::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.98F, 0.7F)
					.passengerAttachments(0.1875F)
					.clientTrackingRange(8)
					.build(CARGO_WAGON_KEY));

	public static final Item STEAM_LOCOMOTIVE_ITEM = ModRegistry.item("steam_locomotive", p -> new MinecartItem(STEAM_LOCOMOTIVE, p), new Item.Properties().stacksTo(1));
	public static final Item CARGO_WAGON_ITEM = ModRegistry.item("cargo_wagon", p -> new MinecartItem(CARGO_WAGON, p), new Item.Properties().stacksTo(1));
	/** 연결 고리: 수레를 눌러 가까운 열차 꼬리에 잇거나 뗀다. 쓰지 않고 남는다. */
	public static final Item COUPLER = ModRegistry.item("coupler", new Item.Properties().stacksTo(16));

	public static final CargoStationBlock CARGO_STATION = ModRegistry.block("cargo_station", CargoStationBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	public static final BlockEntityType<CargoStationBlockEntity> CARGO_STATION_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("cargo_station"), new BlockEntityType<>(CargoStationBlockEntity::new, Set.of(CARGO_STATION)));

	private ModTransport() {
	}

	public static void init() {
		// 연결 고리 (아무 수레나), 웅크리고 빈손 (화물칸 · 기관차): 수레 자신의 우클릭보다 먼저
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (player.isSpectator() || !(entity instanceof AbstractMinecart car)) {
				return InteractionResult.PASS;
			}
			ItemStack held = player.getItemInHand(hand);
			boolean sneakEmpty = hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive() && held.isEmpty() && (car instanceof CargoWagon || car instanceof SteamLocomotive);
			if (!held.is(COUPLER) && !sneakEmpty) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel serverLevel) {
				TrainCoupling.toggle(serverLevel, player, car);
			}
			return InteractionResult.SUCCESS;
		});
	}
}
