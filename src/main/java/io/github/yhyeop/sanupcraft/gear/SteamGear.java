package io.github.yhyeop.sanupcraft.gear;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 증기로 움직이는 장비 (확장 A1): 증기 통 · 리벳 · 리벳총 · 증기 부츠.
 * 증기 통은 충전대(power)에서 채우고, 리벳총은 인벤토리의 증기 통을, 부츠는 자기 몸에 담은 증기를 쓴다.
 * 증기를 담는 아이템의 공용 도우미는 {@link SteamCharge}.
 */
public final class SteamGear {
	private static final ResourceKey<EntityType<?>> RIVET_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sanupcraft.id("rivet"));

	/** 증기 0 ~ 1000. 아이템 막대에 증기량이 보인다. */
	public static final Item STEAM_CANISTER = ModRegistry.item("steam_canister",
			p -> new SteamChargedItem(p, SteamCharge.CANISTER_CAPACITY), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	/** 압착기: 철판 1 → 리벳 4. 리벳총의 탄약. */
	public static final Item RIVET = ModRegistry.item("rivet");
	public static final Item RIVET_GUN = ModRegistry.item("rivet_gun", RivetGunItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	/** 강철 부츠와 같은 방어 + 증기 500. 공중에서 웅크리면 돌진, 증기가 있으면 낙하 피해 절반. */
	public static final Item STEAM_BOOTS = ModRegistry.item("steam_boots", p -> new SteamChargedItem(p, SteamBoots.CAPACITY),
			new Item.Properties().humanoidArmor(SteelGear.STEEL_ARMOR, ArmorType.BOOTS).rarity(Rarity.UNCOMMON));

	public static final EntityType<RivetProjectile> RIVET_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, RIVET_KEY,
			EntityType.Builder.<RivetProjectile>of(RivetProjectile::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(2)
					.build(RIVET_KEY));

	private SteamGear() {
	}

	public static void init() {
		SteamCharge.init();
		SteamBoots.init();
	}
}
