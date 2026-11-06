package io.github.yhyeop.sanupcraft.module;

import java.util.List;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import io.github.yhyeop.sanupcraft.gear.AreaMining;
import io.github.yhyeop.sanupcraft.gear.SteamToolItem;

/** 장비의 모듈 읽기 · 끼우기 · 빼기 (G3). 규칙은 여기 한 곳: 칸 수, 종류, 같은 모듈 두 개 금지, 같이 못 쓰는 모듈. */
public final class GearModules {
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** 끼울 수 있나 본 결과. */
	public enum Check {
		OK, NO_SLOTS, FULL, WRONG_KIND, DUPLICATE, CONFLICT
	}

	private GearModules() {
	}

	public static InstalledModules get(DataComponentGetter stack) {
		InstalledModules modules = stack.get(ModModules.MODULES);
		return modules == null ? InstalledModules.EMPTY : modules;
	}

	public static boolean has(DataComponentGetter stack, GearModule module) {
		InstalledModules modules = stack.get(ModModules.MODULES);
		return modules != null && modules.has(module);
	}

	/** 입은 갑옷(머리 · 가슴 · 다리 · 발) 중 하나라도 이 모듈이 있나. */
	public static boolean wornHas(LivingEntity entity, GearModule module) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (has(entity.getItemBySlot(slot), module)) {
				return true;
			}
		}
		return false;
	}

	/** 주손에 든 장비에 이 모듈이 있나. */
	public static boolean heldHas(LivingEntity entity, GearModule module) {
		return has(entity.getMainHandItem(), module);
	}

	/** 이 장비에 이 모듈을 하나 더 끼울 수 있나. */
	public static Check check(ItemStack gear, GearModule module) {
		int capacity = ModuleSlots.capacity(gear);
		if (capacity <= 0) {
			return Check.NO_SLOTS;
		}
		if (!fits(gear, module)) {
			return Check.WRONG_KIND;
		}
		InstalledModules modules = get(gear);
		if (modules.has(module)) {
			return Check.DUPLICATE;
		}
		for (GearModule other : modules.modules()) {
			if (module.conflictsWith(other)) {
				return Check.CONFLICT;
			}
		}
		return modules.size() >= capacity ? Check.FULL : Check.OK;
	}

	/** 장비 종류가 맞나. 범위 모듈은 원래 3x3이 아닌 곡괭이 · 삽 · 도끼에만. */
	public static boolean fits(ItemStack gear, GearModule module) {
		if (!ModuleSlots.kinds(gear).contains(module.kind())) {
			return false;
		}
		if (module == GearModule.AREA) {
			return !AreaMining.isAreaToolItem(gear) && !(gear.getItem() instanceof SteamToolItem)
					&& (gear.is(net.minecraft.tags.ItemTags.PICKAXES) || gear.is(net.minecraft.tags.ItemTags.SHOVELS)
							|| gear.is(net.minecraft.tags.ItemTags.AXES));
		}
		return true;
	}

	/** 끼운다. 끼웠으면 true. */
	public static boolean install(ItemStack gear, GearModule module) {
		if (check(gear, module) != Check.OK) {
			return false;
		}
		set(gear, get(gear).with(module).modules());
		return true;
	}

	/** 뺀다. 있었으면 true. */
	public static boolean remove(ItemStack gear, GearModule module) {
		InstalledModules modules = get(gear);
		if (!modules.has(module)) {
			return false;
		}
		set(gear, modules.without(module).modules());
		return true;
	}

	/** 목록을 통째로 정한다 (비면 컴포넌트를 지워 새 장비와 같이 쌓이게). */
	public static void set(ItemStack gear, List<GearModule> modules) {
		if (modules.isEmpty()) {
			gear.remove(ModModules.MODULES);
		} else {
			gear.set(ModModules.MODULES, new InstalledModules(modules));
		}
	}
}
