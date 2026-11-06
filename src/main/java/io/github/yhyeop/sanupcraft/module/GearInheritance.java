package io.github.yhyeop.sanupcraft.module;

import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * 계승 (G4): 장비를 윗단계로 올릴 때 마법 부여 · 모듈 · 이름 · 닳은 정도 · 수리 비용 · 장식(갑옷 다듬기)을 결과로 옮긴다.
 * 길은 셋:
 * <ul>
 * <li>제작대 {@code minecraft:crafting_transmute} (강철 → 크롬강): 바닐라가 컴포넌트를 통째로 옮긴다</li>
 * <li>제작대 {@code sanupcraft:gear_upgrade_shaped} · {@code _shapeless} (강철 · 크롬강 → 내열, 강철 → 증기 부츠 · 제트팩):
 * 보통 모양 · 무형 레시피와 같고 결과에 이 클래스로 옮긴다 ({@link GearUpgradeRecipes})</li>
 * <li>기계 레시피 (정밀 조립기: 네더라이트 → 공허): 기계가 결과를 만들 때 재료 칸의 장비에서 옮긴다 ({@link #fromInputs})</li>
 * </ul>
 * 다이아 → 네더라이트는 바닐라 대장장이 작업대가 이미 옮긴다.
 */
public final class GearInheritance {
	private static final List<DataComponentType<?>> CARRIED = List.of(
			DataComponents.ENCHANTMENTS, DataComponents.CUSTOM_NAME, DataComponents.REPAIR_COST, DataComponents.TRIM);

	private GearInheritance() {
	}

	/** 옮길 것이 있는 장비인가 (내구도가 있거나, 마법 부여 · 모듈 · 이름이 있음). */
	public static boolean isGear(ItemStack stack) {
		return !stack.isEmpty() && (stack.isDamageableItem() || stack.isEnchanted() || stack.has(ModModules.MODULES)
				|| ModuleSlots.capacity(stack) > 0);
	}

	/** from의 마법 부여 · 모듈 · 이름 · 닳은 정도를 to에 옮겨 to를 돌려준다. 모듈은 결과의 칸 · 종류에 맞는 것만. */
	public static ItemStack carry(ItemStack from, ItemStack to) {
		if (from.isEmpty() || to.isEmpty()) {
			return to;
		}
		for (DataComponentType<?> type : CARRIED) {
			copy(type, from, to);
		}
		if (from.isDamageableItem() && to.isDamageableItem() && from.getDamageValue() > 0) {
			to.setDamageValue(Math.min(from.getDamageValue(), to.getMaxDamage() - 1));
		}
		InstalledModules modules = GearModules.get(from);
		if (!modules.isEmpty()) {
			GearModules.set(to, List.of());
			for (GearModule module : modules.modules()) {
				GearModules.install(to, module);
			}
		}
		return to;
	}

	/** 기계 결과: 재료 칸 중 장비가 있으면 거기서 옮긴다 (결과가 장비일 때만). 시험이 직접 부른다. */
	public static ItemStack fromInputs(ItemStack result, ItemStack first, ItemStack second) {
		if (!result.isDamageableItem() && ModuleSlots.capacity(result) <= 0) {
			return result;
		}
		if (isGear(first)) {
			return carry(first, result);
		}
		if (isGear(second)) {
			return carry(second, result);
		}
		return result;
	}

	private static <T> void copy(DataComponentType<T> type, ItemStack from, ItemStack to) {
		T value = from.get(type);
		if (value != null) {
			to.set(type, value);
		}
	}
}
