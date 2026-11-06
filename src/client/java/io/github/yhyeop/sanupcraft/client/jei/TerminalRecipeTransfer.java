package io.github.yhyeop.sanupcraft.client.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import io.github.yhyeop.sanupcraft.storage.ModStorage;
import io.github.yhyeop.sanupcraft.storage.StoragePayloads;
import io.github.yhyeop.sanupcraft.storage.TerminalMenu;

/**
 * JEI 제작 레시피의 + 버튼 → 제작 단말 3x3 칸. 칸마다 쓸 수 있는 아이템 후보를 서버로 보내고,
 * 서버가 네트워크(없으면 인벤토리)에서 실제로 있는 것을 하나씩 놓는다 ({@link TerminalMenu#fillGrid}).
 */
public class TerminalRecipeTransfer implements IRecipeTransferHandler<TerminalMenu, RecipeHolder<CraftingRecipe>> {
	private final IRecipeTransferHandlerHelper helper;

	public TerminalRecipeTransfer(IRecipeTransferHandlerHelper helper) {
		this.helper = helper;
	}

	/** 제작 단말 · 자동 제작대를 JEI 제작 레시피의 작업대 목록에 넣는다. */
	public static void addCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(RecipeTypes.CRAFTING, ModStorage.CRAFTING_TERMINAL);
	}

	@Override
	public Class<? extends TerminalMenu> getContainerClass() {
		return TerminalMenu.class;
	}

	@Override
	public Optional<MenuType<TerminalMenu>> getMenuType() {
		return Optional.of(ModStorage.TERMINAL_MENU);
	}

	@Override
	public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
		return RecipeTypes.CRAFTING;
	}

	@Override
	public @Nullable IRecipeTransferError transferRecipe(TerminalMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots, Player player,
			boolean maxTransfer, boolean doTransfer) {
		if (!menu.isCrafting()) {
			return helper.createUserErrorWithTooltip(Component.translatable("gui.sanupcraft.terminal.no_grid"));
		}
		if (!doTransfer) {
			return null;
		}
		List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
		List<List<ItemStack>> choices = new ArrayList<>();
		for (int i = 0; i < TerminalMenu.GRID_SIZE; i++) {
			List<ItemStack> options = new ArrayList<>();
			if (i < inputs.size()) {
				inputs.get(i).getItemStacks().limit(StoragePayloads.MAX_CHOICES).forEach(stack -> options.add(stack.copyWithCount(1)));
			}
			choices.add(options);
		}
		ClientPlayNetworking.send(new StoragePayloads.Fill(menu.containerId, choices));
		return null;
	}
}
