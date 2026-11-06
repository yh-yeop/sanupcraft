package io.github.yhyeop.sanupcraft.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * 도구 모듈이 블록 드롭을 바꾼다 (G3, Fabric 전리품 이벤트 {@code MODIFY_DROPS}). 블록 전리품이고 도구가 모듈을 가졌을 때만.
 * <ul>
 * <li>섬세한 손길: 도구에 섬세한 손길 I을 붙인 사본으로 드롭을 다시 굴린다 (유리 · 광석 · 풀 등 바닐라 규칙 그대로)</li>
 * <li>행운: 도구의 행운을 +1 한 사본으로 다시 굴린다 (철광석이면 바닐라 행운 I처럼 33%로 2개)</li>
 * <li>자동 제련: 드롭마다 화로 레시피가 있으면 구운 것으로 (원석 철 → 철 주괴, 모래 → 유리)</li>
 * </ul>
 * 해머 · 범위 모듈로 같이 캔 칸도 같은 도구라 똑같이 적용된다.
 */
public final class ModuleDrops {
	/** 다시 굴리는 동안 (안쪽 굴림에서 또 바꾸지 않게). */
	private static final ThreadLocal<Boolean> REROLLING = ThreadLocal.withInitial(() -> false);

	private ModuleDrops() {
	}

	static void init() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> modify(context, drops));
	}

	private static void modify(LootContext context, List<ItemStack> drops) {
		if (REROLLING.get()) {
			return;
		}
		BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
		ItemInstance tool = context.getOptional(LootContextParams.TOOL);
		if (state == null || tool == null || !(tool instanceof ItemStack toolStack)) {
			return;
		}
		InstalledModules modules = GearModules.get(toolStack);
		if (modules.isEmpty()) {
			return;
		}
		ServerLevel level = context.getLevel();
		if (modules.has(GearModule.SILK_TOUCH) || modules.has(GearModule.LUCK)) {
			ItemStack copy = toolStack.copy();
			copy.remove(ModModules.MODULES);
			if (modules.has(GearModule.SILK_TOUCH)) {
				copy.enchant(enchantment(level, Enchantments.SILK_TOUCH), 1);
			} else {
				Holder<Enchantment> fortune = enchantment(level, Enchantments.FORTUNE);
				copy.enchant(fortune, EnchantmentHelper.getItemEnchantmentLevel(fortune, toolStack) + 1);
			}
			Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
			if (origin != null) {
				BlockEntity blockEntity = context.getOptional(LootContextParams.BLOCK_ENTITY);
				Entity breaker = context.getOptional(LootContextParams.THIS_ENTITY);
				List<ItemStack> rerolled;
				REROLLING.set(true);
				try {
					rerolled = Block.getDrops(state, level, BlockPos.containing(origin), blockEntity, breaker, copy);
				} finally {
					REROLLING.set(false);
				}
				drops.clear();
				drops.addAll(rerolled);
			}
		}
		if (modules.has(GearModule.AUTO_SMELT)) {
			List<ItemStack> smelted = new ArrayList<>(drops.size());
			for (ItemStack drop : drops) {
				smelted.add(smelt(level, drop));
			}
			drops.clear();
			drops.addAll(smelted);
		}
	}

	/** 화로에 구운 결과 (개수 그대로). 레시피가 없으면 그대로. */
	public static ItemStack smelt(ServerLevel level, ItemStack stack) {
		if (stack.isEmpty()) {
			return stack;
		}
		SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
		Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
		if (recipe.isEmpty()) {
			return stack;
		}
		ItemStack result = recipe.get().value().assemble(input);
		return result.isEmpty() ? stack : result.copyWithCount(result.getCount() * stack.getCount());
	}

	private static Holder<Enchantment> enchantment(ServerLevel level, ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}
}
