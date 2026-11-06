package io.github.yhyeop.sanupcraft.combat;

import java.util.Set;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/**
 * M13 전투 (확장 계획 3 C1 · C5): 강철 창 · 증기 해머 · 화염 방사기 (무기), 전투 신호기 · 쓰러짐 · 응급 증기 주사기 (협동).
 * 기절 · 쓰러짐은 이 패키지의 효과. 리벳총 모듈(연발 · 산탄)은 장비 모듈(G3)과 같이 나중에.
 */
public final class ModCombat {
	/** 강철 창: 찌르기 피해 7 (맨손 1 + 6), 초당 1번, 공격 사거리 +1.5. */
	public static final float SPEAR_DAMAGE = 7.0F;
	public static final float SPEAR_ATTACK_SPEED = 1.0F;
	public static final double SPEAR_REACH = 1.5;
	/** 증기 해머: 피해 10, 초당 0.6번. */
	public static final float HAMMER_DAMAGE = 10.0F;
	public static final float HAMMER_ATTACK_SPEED = 0.6F;

	// ---- 효과 ----
	/** 기절: 움직이지도 뛰지도 못하고, 근접 · 원거리 공격이 들어가지 않는다. 플레이어는 캐는 속도도 떨어진다. */
	public static final Holder<MobEffect> STUNNED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Sanupcraft.id("stunned"), new StunnedEffect());
	/** 쓰러짐: 기어 다닐 만큼만 움직이고 뛸 수 없다. 실제 상태는 {@link Downed}가 정하고, 이 효과는 모습(아이콘 · 느림)만. */
	public static final Holder<MobEffect> DOWNED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Sanupcraft.id("downed"),
			new CombatEffect(MobEffectCategory.HARMFUL, 0x8A1C1C)
					.addAttributeModifier(Attributes.MOVEMENT_SPEED, Sanupcraft.id("effect.downed.speed"), -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
					.addAttributeModifier(Attributes.JUMP_STRENGTH, Sanupcraft.id("effect.downed.jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	// ---- 무기 ----
	public static final Item STEEL_SPEAR = ModRegistry.item("steel_spear", new Item.Properties()
			.durability(SteelGear.STEEL_TOOL.durability())
			.repairable(SteelGear.REPAIRS_STEEL)
			.enchantable(SteelGear.STEEL_TOOL.enchantmentValue())
			.attributes(ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, SPEAR_DAMAGE - 1.0, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, SPEAR_ATTACK_SPEED - 4.0, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(Sanupcraft.id("steel_spear_reach"), SPEAR_REACH, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.build())
			.component(DataComponents.WEAPON, new Weapon(1)));
	/** 3x3으로 캐는 강철 해머와 다른 무기. 증기 600, 증기 30으로 강타하면 기절. */
	public static final Item STEAM_HAMMER = ModRegistry.item("steam_hammer", SteamHammerItem::new, new Item.Properties()
			.durability(SteelGear.STEEL_TOOL.durability())
			.repairable(SteelGear.REPAIRS_STEEL)
			.enchantable(SteelGear.STEEL_TOOL.enchantmentValue())
			.rarity(Rarity.UNCOMMON)
			.attributes(ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, HAMMER_DAMAGE - 1.0, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, HAMMER_ATTACK_SPEED - 4.0, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.build())
			// 무거운 망치: 방패를 3초 막는다
			.component(DataComponents.WEAPON, new Weapon(1, 3.0F)));
	public static final Item FLAMETHROWER = ModRegistry.item("flamethrower", FlamethrowerItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());

	// ---- 협동 ----
	public static final CombatBeaconBlock COMBAT_BEACON = ModRegistry.block("combat_beacon", CombatBeaconBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.strength(3.5F, 6.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
			.lightLevel(state -> 7));
	public static final Item STEAM_SYRINGE = ModRegistry.item("steam_syringe", SteamSyringeItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	public static final BlockEntityType<CombatBeaconBlockEntity> COMBAT_BEACON_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("combat_beacon"), new BlockEntityType<>(CombatBeaconBlockEntity::new, Set.of(COMBAT_BEACON)));

	private ModCombat() {
	}

	public static void init() {
		FlamethrowerItem.init();
		Downed.init();
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			// 기절한 몹 · 플레이어의 공격은 들어가지 않는다 (근접 · 그 몹이 쏜 화살 등)
			if (source.getEntity() instanceof LivingEntity attacker && attacker != entity && isStunned(attacker)) {
				return false;
			}
			return Downed.allowDamage(entity, source);
		});
	}

	public static boolean isStunned(LivingEntity entity) {
		return entity.hasEffect(STUNNED);
	}
}
