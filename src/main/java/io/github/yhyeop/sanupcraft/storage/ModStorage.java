package io.github.yhyeop.sanupcraft.storage;

import java.util.Set;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * M7 보관 · 제작 편의: 저장 서랍 (S1), 창고 관 · 창고 단말 · 제작 단말 (S2 · S3),
 * 자동 제작대 · 재고 유지기 (S4 · S5). */
public final class ModStorage {
	/** 부순 서랍에 남는 내용물. */
	public static final DataComponentType<DrawerContents> DRAWER_CONTENTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Sanupcraft.id("drawer_contents"),
			DataComponentType.<DrawerContents>builder().persistent(DrawerContents.CODEC).networkSynchronized(DrawerContents.STREAM_CODEC).build());

	public static final DrawerBlock IRON_DRAWER = ModRegistry.block("iron_drawer", p -> new DrawerBlock(false, p), drawerProperties(MapColor.METAL));
	public static final DrawerBlock STEEL_DRAWER = ModRegistry.block("steel_drawer", p -> new DrawerBlock(true, p), drawerProperties(MapColor.COLOR_GRAY));
	/** 서랍 용량 업그레이드: 하나에 ×2, 서랍 하나에 최대 2개. */
	public static final Item DRAWER_UPGRADE = ModRegistry.item("drawer_upgrade");
	/** 넘침 버림 업그레이드: 가득 찬 서랍에 들어오는 같은 아이템을 버린다. */
	public static final Item DRAWER_VOID_UPGRADE = ModRegistry.item("drawer_void_upgrade");

	public static final BlockEntityType<DrawerBlockEntity> DRAWER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("drawer"), new BlockEntityType<>(DrawerBlockEntity::new, Set.of(IRON_DRAWER, STEEL_DRAWER)));

	// ---- S2 · S3 창고 네트워크 ----
	/** 휴대 단말이 연결된 단말 자리. */
	public static final DataComponentType<GlobalPos> TERMINAL_LINK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Sanupcraft.id("terminal_link"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

	public static final StorageCableBlock STORAGE_CABLE = ModRegistry.block("storage_cable", StorageCableBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GREEN)
			.strength(1.5F)
			.sound(SoundType.METAL)
			.noOcclusion());
	public static final TerminalBlock STORAGE_TERMINAL = ModRegistry.block("storage_terminal", p -> new TerminalBlock(false, p), machineProperties());
	public static final TerminalBlock CRAFTING_TERMINAL = ModRegistry.block("crafting_terminal", p -> new TerminalBlock(true, p), machineProperties());

	public static final BlockEntityType<TerminalBlockEntity> TERMINAL_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("storage_terminal"),
			new BlockEntityType<>(TerminalBlockEntity::new, Set.of(STORAGE_TERMINAL, CRAFTING_TERMINAL)));

	public static final ExtendedMenuType<TerminalMenu, TerminalMenu.OpenData> TERMINAL_MENU = Registry.register(
			BuiltInRegistries.MENU, Sanupcraft.id("storage_terminal"), new ExtendedMenuType<>(TerminalMenu::new, TerminalMenu.OpenData.STREAM_CODEC));

	// ---- S4 · S5 자동 제작 ----
	public static final StorageMachineBlock<AutocrafterBlockEntity> AUTOCRAFTER = ModRegistry.block("autocrafter",
			p -> new StorageMachineBlock<>(AutocrafterBlockEntity::new, () -> ModStorage.AUTOCRAFTER_BLOCK_ENTITY, AutocrafterBlockEntity::serverTick, p),
			machineProperties());
	public static final StorageMachineBlock<StockerBlockEntity> STOCKER = ModRegistry.block("stocker",
			p -> new StorageMachineBlock<>(StockerBlockEntity::new, () -> ModStorage.STOCKER_BLOCK_ENTITY, StockerBlockEntity::serverTick, p),
			machineProperties());

	public static final BlockEntityType<AutocrafterBlockEntity> AUTOCRAFTER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("autocrafter"), new BlockEntityType<>(AutocrafterBlockEntity::new, Set.of(AUTOCRAFTER)));
	public static final BlockEntityType<StockerBlockEntity> STOCKER_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("stocker"), new BlockEntityType<>(StockerBlockEntity::new, Set.of(STOCKER)));

	public static final MenuType<AutocrafterMenu> AUTOCRAFTER_MENU = Registry.register(
			BuiltInRegistries.MENU, Sanupcraft.id("autocrafter"), new MenuType<>(AutocrafterMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<StockerMenu> STOCKER_MENU = Registry.register(
			BuiltInRegistries.MENU, Sanupcraft.id("stocker"), new MenuType<>(StockerMenu::new, FeatureFlags.VANILLA_SET));

	private ModStorage() {
	}

	public static void init() {
		StoragePayloads.init();
		// 서랍 앞면 좌클릭: 부수지 않고 꺼낸다 (클라이언트도 SUCCESS를 돌려 캐기를 시작하지 않게 한다)
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof DrawerBlock) || direction != state.getValue(DrawerBlock.FACING) || player.isSpectator()) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
				DrawerBlock.giveExtracted(player, drawer, player.isSecondaryUseActive(), serverLevel.getGameTime());
			}
			return InteractionResult.SUCCESS;
		});
	}

	private static BlockBehaviour.Properties machineProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.METAL)
				.strength(3.5F, 6.0F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL);
	}

	private static BlockBehaviour.Properties drawerProperties(MapColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.strength(3.0F, 6.0F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL);
	}
}
