package io.github.yhyeop.sanupcraft.logistics;

import java.util.Set;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** 운반: 컨베이어 벨트와 벨트 모터 (2단계), 벨트 분류기 · 필터 깔때기, 엔더 운반기 · 엔더 닻 (엔드 단계). */
public final class ModLogistics {
	public static final BeltBlock BELT = ModRegistry.block("conveyor_belt", BeltBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY)
			.strength(1.0F)
			.sound(SoundType.WOOL)
			.noOcclusion());

	public static final BeltMotorBlock BELT_MOTOR = ModRegistry.block("belt_motor", BeltMotorBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));

	/** 벨트 분류기: 필터에 맞는 것은 왼쪽, 나머지는 앞으로. 벨트 줄 가운데에 놓는다. */
	public static final BeltSorterBlock BELT_SORTER = ModRegistry.block("belt_sorter", BeltSorterBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
			.noOcclusion());

	/** 필터 깔때기: 바닐라 깔때기와 같되 필터 5칸에 맞는 것만 지나간다. */
	public static final FilterHopperBlock FILTER_HOPPER = ModRegistry.block("filter_hopper", FilterHopperBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.0F, 4.8F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
			.noOcclusion());

	/** 원석 표지: 필터 칸에 놓으면 원석 · 광석 태그(c:raw_materials, c:ores)의 아이템이 전부 맞는다. */
	public static final Item ORE_MARKER = ModRegistry.item("ore_marker");

	/** 엔더 운반기 (엔드 단계). 셜커 껍데기 + 엔더 진주로 만든다. */
	public static final EnderLinkBlock ENDER_SENDER = ModRegistry.block("ender_sender", p -> new EnderLinkBlock(true, p), enderLinkProperties());
	public static final EnderLinkBlock ENDER_RECEIVER = ModRegistry.block("ender_receiver", p -> new EnderLinkBlock(false, p), enderLinkProperties());

	/** 엔더 닻 (엔드 단계): 튀긴 후렴과로 주변 3x3 청크를 계속 불러 둔다. 용의 숨결이 들어간다. */
	public static final EnderAnchorBlock ENDER_ANCHOR = ModRegistry.block("ender_anchor", EnderAnchorBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE)
			.strength(5.0F, 1200.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
			.lightLevel(state -> state.getValue(EnderAnchorBlock.ACTIVE) ? 10 : 2));

	public static final BlockEntityType<BeltBlockEntity> BELT_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("conveyor_belt"), new BlockEntityType<>(BeltBlockEntity::new, Set.of(BELT)));

	public static final BlockEntityType<BeltMotorBlockEntity> BELT_MOTOR_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("belt_motor"), new BlockEntityType<>(BeltMotorBlockEntity::new, Set.of(BELT_MOTOR)));

	public static final BlockEntityType<BeltSorterBlockEntity> BELT_SORTER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("belt_sorter"), new BlockEntityType<>(BeltSorterBlockEntity::new, Set.of(BELT_SORTER)));

	public static final BlockEntityType<FilterHopperBlockEntity> FILTER_HOPPER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("filter_hopper"), new BlockEntityType<>(FilterHopperBlockEntity::new, Set.of(FILTER_HOPPER)));

	/** 필터 화면: 분류기(9칸, 발사기 모양)와 필터 깔때기(5칸, 깔때기 모양). */
	public static final MenuType<FilterMenu> SORTER_MENU = Registry.register(
			BuiltInRegistries.MENU, Sanupcraft.id("belt_sorter"), new MenuType<>(FilterMenu::sorter, FeatureFlags.VANILLA_SET));

	public static final MenuType<FilterMenu> FILTER_HOPPER_MENU = Registry.register(
			BuiltInRegistries.MENU, Sanupcraft.id("filter_hopper"), new MenuType<>(FilterMenu::hopper, FeatureFlags.VANILLA_SET));

	public static final BlockEntityType<EnderLinkBlockEntity> ENDER_LINK_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("ender_link"), new BlockEntityType<>(EnderLinkBlockEntity::new, Set.of(ENDER_SENDER, ENDER_RECEIVER)));

	public static final BlockEntityType<EnderAnchorBlockEntity> ENDER_ANCHOR_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("ender_anchor"), new BlockEntityType<>(EnderAnchorBlockEntity::new, Set.of(ENDER_ANCHOR)));

	private ModLogistics() {
	}

	public static void init() {
		// 정적 수신기 목록이 옛 서버의 월드를 붙들고 있지 않도록 서버가 시작 · 종료될 때 비운다
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			EnderLinkBlockEntity.clearReceivers();
			EnderAnchorBlockEntity.clearActive();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			EnderLinkBlockEntity.clearReceivers();
			EnderAnchorBlockEntity.clearActive();
		});
	}

	private static BlockBehaviour.Properties enderLinkProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE)
				.strength(3.5F, 1200.0F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
				.lightLevel(state -> 5);
	}
}
