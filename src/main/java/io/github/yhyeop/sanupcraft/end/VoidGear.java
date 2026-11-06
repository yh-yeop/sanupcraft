package io.github.yhyeop.sanupcraft.end;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.config.SanupcraftConfig;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 공허 장비 (M6, 최종 장비). 정밀 조립기에서 네더라이트 장비 + 공허 핵으로 만든다. 방어 · 공격은 네더라이트 + 1 (단단함 · 밀치기 저항은 같음).
 * 세트 효과: 네 부위를 다 입고 어느 차원에서든 최저 높이 − {@link #RESCUE_DEPTH} 아래로 떨어지면 마지막으로 서 있던 땅으로 올려 주고
 * 느린 낙하를 잠깐 건다. 쿨다운 5분 (설정 {@code voidGear.rescueCooldownTicks}). 서 있던 땅은 1초마다 기억한다 (메모리, 재시작하면 잊음 →
 * 그 차원의 원점 위 지면, 그것도 없으면 구하지 않는다).
 */
public final class VoidGear {
	public static final TagKey<Item> REPAIRS_VOID = TagKey.create(Registries.ITEM, Sanupcraft.id("repairs_void"));
	public static final ResourceKey<EquipmentAsset> VOID_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Sanupcraft.id("void"));

	/** 네더라이트(3 · 6 · 8 · 3, 단단함 3, 밀치기 저항 0.1)보다 부위마다 방어 + 1. */
	public static final ArmorMaterial VOID_ARMOR = new ArmorMaterial(
			40, defense(4, 7, 9, 4), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F, REPAIRS_VOID, VOID_ASSET);
	/** 네더라이트(공격 보너스 4)보다 + 1. */
	public static final ToolMaterial VOID_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2200, 9.0F, 5.0F, 15, REPAIRS_VOID);

	public static final Item VOID_HELMET = armor("void_helmet", ArmorType.HELMET);
	public static final Item VOID_CHESTPLATE = armor("void_chestplate", ArmorType.CHESTPLATE);
	public static final Item VOID_LEGGINGS = armor("void_leggings", ArmorType.LEGGINGS);
	public static final Item VOID_BOOTS = armor("void_boots", ArmorType.BOOTS);
	public static final Item VOID_SWORD = ModRegistry.item("void_sword",
			new Item.Properties().sword(VOID_TOOL, 3.0F, -2.4F).fireResistant().rarity(Rarity.EPIC));

	/** 최저 높이에서 이만큼 더 떨어지면 구한다. */
	public static final int RESCUE_DEPTH = 10;
	/** 기본값. 실제로 쓰는 값은 설정 파일. */
	public static final int RESCUE_COOLDOWN = 6000;
	public static final int SLOW_FALL_TICKS = 100;
	private static final int REMEMBER_INTERVAL = 20;

	private static final Map<UUID, GlobalPos> LAST_SAFE = new HashMap<>();
	private static final Map<UUID, Long> LAST_RESCUE = new HashMap<>();

	private VoidGear() {
	}

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			boolean remember = server.getTickCount() % REMEMBER_INTERVAL == 0;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (remember) {
					rememberSafe(player);
				}
				rescue(player, player.level().getGameTime());
			}
		});
	}

	/** 공허 갑옷 네 부위를 다 입었나. */
	public static boolean wearsFullSet(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(VOID_HELMET)
				&& entity.getItemBySlot(EquipmentSlot.CHEST).is(VOID_CHESTPLATE)
				&& entity.getItemBySlot(EquipmentSlot.LEGS).is(VOID_LEGGINGS)
				&& entity.getItemBySlot(EquipmentSlot.FEET).is(VOID_BOOTS);
	}

	/** 땅 위에 서 있으면 그 자리를 기억한다. 기억했으면 true. */
	public static boolean rememberSafe(ServerPlayer player) {
		if (!player.onGround() || player.isSpectator() || player.isPassenger()) {
			return false;
		}
		ServerLevel level = player.level();
		BlockPos feet = player.blockPosition();
		if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)) {
			return false;
		}
		LAST_SAFE.put(player.getUUID(), GlobalPos.of(level.dimension(), feet));
		return true;
	}

	/** 기억한 자리 (시험용). */
	public static GlobalPos lastSafe(ServerPlayer player) {
		return LAST_SAFE.get(player.getUUID());
	}

	/** 쿨다운을 지운다 (시험 뒤처리). */
	public static void forget(ServerPlayer player) {
		LAST_SAFE.remove(player.getUUID());
		LAST_RESCUE.remove(player.getUUID());
	}

	/** 한 벌을 입고 공허로 떨어졌으면 위로 올린다. 올렸으면 true. */
	public static boolean rescue(ServerPlayer player, long now) {
		ServerLevel level = player.level();
		if (player.getY() >= level.getMinY() - RESCUE_DEPTH || !player.isAlive() || player.isSpectator() || !wearsFullSet(player)) {
			return false;
		}
		Long last = LAST_RESCUE.get(player.getUUID());
		if (last != null && now - last < SanupcraftConfig.current().voidGearRescueCooldownTicks) {
			return false;
		}
		BlockPos target = safeSpot(level, player);
		if (target == null) {
			return false;
		}
		LAST_RESCUE.put(player.getUUID(), now);
		player.setDeltaMovement(Vec3.ZERO);
		player.teleportTo(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0));
		level.playSound(null, target, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.05);
		player.sendOverlayMessage(Component.translatable("item.sanupcraft.void_gear.rescued"));
		return true;
	}

	/** 올릴 자리: 이 차원에서 기억한 땅, 없으면 원점 위 지면. 둘 다 없으면 null. */
	private static BlockPos safeSpot(ServerLevel level, ServerPlayer player) {
		GlobalPos remembered = LAST_SAFE.get(player.getUUID());
		if (remembered != null && remembered.dimension() == level.dimension()) {
			return remembered.pos();
		}
		BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.ZERO);
		return top.getY() > level.getMinY() ? top : null;
	}

	private static Item armor(String name, ArmorType type) {
		return ModRegistry.item(name, new Item.Properties().humanoidArmor(VOID_ARMOR, type).fireResistant().rarity(Rarity.EPIC));
	}

	private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
		Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
		map.put(ArmorType.BOOTS, boots);
		map.put(ArmorType.LEGGINGS, legs);
		map.put(ArmorType.CHESTPLATE, chest);
		map.put(ArmorType.HELMET, helmet);
		map.put(ArmorType.BODY, 11);
		return map;
	}
}
