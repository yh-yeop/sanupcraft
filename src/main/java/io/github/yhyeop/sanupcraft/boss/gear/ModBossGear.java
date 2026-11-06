package io.github.yhyeop.sanupcraft.boss.gear;

import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;
import io.github.yhyeop.sanupcraft.gear.HeatproofGear;
import io.github.yhyeop.sanupcraft.gear.SteelGear;

/**
 * 보스 전용 장비 (확장 3 C4). 제작 레시피는 없고, 보스를 잡은 기여자가 확률로 받는다 ({@link BossGearDrops}).
 * <ul>
 *   <li>녹슨 망치 (녹슨 수호자): 무거운 무기, 피해 9 · 매우 느림, 때리면 녹 II ({@link RustyHammerItem}).</li>
 *   <li>화구 방패 (용광로 파수꾼): 방패, 근접 공격을 막으면 때린 쪽이 과열 I ({@link FurnaceShieldItem}).</li>
 *   <li>공허 지팡이 (공허 거상): 우클릭으로 공허 탄 (피해 6 + 공허 침식 I), 쿨다운 30틱, 내구도를 쓴다 ({@link VoidStaffItem}).</li>
 * </ul>
 */
public final class ModBossGear {
	/**
	 * 보스 장비의 모듈 칸 수 (설계 C4: 셋 다 2).
	 * TODO(G3): 장비 모듈 시스템이 생기면 이 값을 모듈 칸으로 읽는다 ({@link #moduleSlots}). 지금은 칸만 정해 두고 끼우는 곳은 없다.
	 */
	public static final int MODULE_SLOTS = 2;

	/** 공허 장비 수리 재료 ({@code VoidGear.REPAIRS_VOID}와 같은 태그. 그 클래스를 일찍 불러 탭 순서가 바뀌지 않게 따로 만든다). */
	private static final TagKey<Item> REPAIRS_VOID = TagKey.create(Registries.ITEM, Sanupcraft.id("repairs_void"));
	private static final ResourceKey<EntityType<?>> VOID_BOLT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("void_bolt"));

	public static final Item RUSTY_HAMMER = ModRegistry.item("rusty_hammer", RustyHammerItem::new, new Item.Properties()
			.durability(RustyHammerItem.DURABILITY)
			.attributes(ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, RustyHammerItem.DAMAGE - 1.0, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, RustyHammerItem.ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.build())
			.component(DataComponents.WEAPON, new Weapon(1, RustyHammerItem.DISABLE_BLOCKING_SECONDS))
			.enchantable(12)
			.repairable(SteelGear.REPAIRS_STEEL)
			.rarity(Rarity.EPIC));

	public static final Item FURNACE_SHIELD = ModRegistry.item("furnace_shield", FurnaceShieldItem::new, new Item.Properties()
			.durability(FurnaceShieldItem.DURABILITY)
			.repairable(HeatproofGear.REPAIRS_HEATPROOF)
			.equippableUnswappable(EquipmentSlot.OFFHAND)
			.fireResistant()
			.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
					0.25F,
					1.0F,
					List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
					new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
					Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
					Optional.of(SoundEvents.SHIELD_BLOCK),
					Optional.of(SoundEvents.SHIELD_BREAK)))
			.component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
			.rarity(Rarity.EPIC));

	public static final Item VOID_STAFF = ModRegistry.item("void_staff", VoidStaffItem::new, new Item.Properties()
			.durability(VoidStaffItem.DURABILITY)
			.enchantable(15)
			.repairable(REPAIRS_VOID)
			.fireResistant()
			.rarity(Rarity.EPIC));

	/** 공허 탄의 그림 (탄 렌더러가 그리는 아이템). 크리에이티브 탭에는 넣지 않는다. */
	public static final Item VOID_BOLT = registerHidden("void_bolt");

	public static final EntityType<VoidBolt> VOID_BOLT_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, VOID_BOLT_KEY,
			EntityType.Builder.<VoidBolt>of(VoidBolt::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.3F, 0.3F)
					.clientTrackingRange(6)
					.updateInterval(2)
					.build(VOID_BOLT_KEY));

	private ModBossGear() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(FurnaceShieldItem::afterDamage);
	}

	/** 보스 장비인가. */
	public static boolean isBossGear(ItemStack stack) {
		return stack.is(RUSTY_HAMMER) || stack.is(FURNACE_SHIELD) || stack.is(VOID_STAFF);
	}

	/** 이 장비의 모듈 칸 (보스 장비 = {@link #MODULE_SLOTS}, 아니면 0). TODO(G3): 모듈 시스템이 다른 장비 칸과 함께 읽는다. */
	public static int moduleSlots(ItemStack stack) {
		return isBossGear(stack) ? MODULE_SLOTS : 0;
	}

	private static Item registerHidden(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Sanupcraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}
}
