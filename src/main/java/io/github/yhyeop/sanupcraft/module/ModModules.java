package io.github.yhyeop.sanupcraft.module;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModRegistry;

/**
 * 확장 3 G3 장비 모듈 · G4 수리 · 계승. 모듈 아이템 15종 + 모듈 틀, 장비 작업대(끼우고 빼기), 증기 수리대, 계승 레시피.
 * 모듈 칸 수는 {@link ModuleSlots}, 끼우기 규칙은 {@link GearModules}, 효과는 {@link ModuleEffects} · {@link ModuleDrops} ·
 * {@code effect.Resistances} · {@code gear.AreaMining} · {@code gear.RivetGunItem}, 계승은 {@link GearInheritance}.
 */
public final class ModModules {
	/** 장비에 끼운 모듈 (저장 · 동기화). */
	public static final DataComponentType<InstalledModules> MODULES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Sanupcraft.id("modules"), DataComponentType.<InstalledModules>builder()
					.persistent(InstalledModules.CODEC).networkSynchronized(InstalledModules.STREAM_CODEC).build());

	/** 모듈 틀: 강철판 · 레드스톤 · 기계 부품. 모든 모듈의 바탕. */
	public static final Item MODULE_FRAME = ModRegistry.item("module_frame");
	private static final Map<GearModule, Item> ITEMS = registerModules();

	public static final GearWorkbenchBlock GEAR_WORKBENCH = ModRegistry.block("gear_workbench", GearWorkbenchBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.5F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL));
	public static final RepairStationBlock STEAM_REPAIR_STATION = ModRegistry.block("steam_repair_station", RepairStationBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.0F)
					.requiresCorrectToolForDrops()
					.sound(SoundType.METAL));

	public static final BlockEntityType<RepairStationBlockEntity> REPAIR_STATION_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sanupcraft.id("steam_repair_station"),
			new BlockEntityType<>(RepairStationBlockEntity::new, Set.of(STEAM_REPAIR_STATION)));
	public static final MenuType<GearWorkbenchMenu> GEAR_WORKBENCH_MENU = Registry.register(
			BuiltInRegistries.MENU, Sanupcraft.id("gear_workbench"), new MenuType<>(GearWorkbenchMenu::new, FeatureFlags.VANILLA_SET));

	private ModModules() {
	}

	public static void init() {
		GearUpgradeRecipes.init();
		ModuleEffects.init();
		ModuleDrops.init();
	}

	/** 이 모듈의 아이템. */
	public static Item item(GearModule module) {
		return ITEMS.get(module);
	}

	private static Map<GearModule, Item> registerModules() {
		Map<GearModule, Item> map = new EnumMap<>(GearModule.class);
		for (GearModule module : GearModule.values()) {
			Item item = ModRegistry.item("module_" + module.id(), p -> new ModuleItem(p, module),
					new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
			module.setItem(item);
			map.put(module, item);
		}
		return map;
	}
}
