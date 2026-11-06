package io.github.yhyeop.sanupcraft.storage;

import java.util.Set;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/** M7 보관 · 제작 편의: 저장 서랍 (S1). */
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

	private ModStorage() {
	}

	public static void init() {
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

	private static BlockBehaviour.Properties drawerProperties(MapColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.strength(3.0F, 6.0F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL);
	}
}
