package io.github.yhyeop.sanupcraft.client.jei;

import java.util.List;
import java.util.Locale;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.factory.MachineKind;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;

/**
 * 기계 한 종류의 JEI 화면: 재료(합금로는 둘) → 화살표(처리 시간) → 결과 · 부산물(확률은 툴팁).
 * 아래 줄에 처리 시간과 "증기 또는 연료"를 적는다.
 */
public class MachineRecipeCategory extends AbstractRecipeCategory<MachineRecipe> {
	public static final int WIDTH = 120;
	public static final int HEIGHT = 36;

	private final MachineKind kind;

	public MachineRecipeCategory(IGuiHelper guiHelper, MachineKind kind, Block machine) {
		super(type(kind), machine.getName(), guiHelper.createDrawableItemLike(machine), WIDTH, HEIGHT);
		this.kind = kind;
	}

	/** 기계 종류마다 JEI 레시피 종류 하나 (`sanupcraft:crushing` 등). */
	public static IRecipeType<MachineRecipe> type(MachineKind kind) {
		return IRecipeType.create(Sanupcraft.id(kind.recipeName()), MachineRecipe.class);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
		int x = 0;
		builder.addInputSlot(x + 1, 5).setStandardSlotBackground().addItemStacks(stacks(recipe.ingredient(), recipe.count()));
		if (kind.twoInputs()) {
			x += 18;
			builder.addInputSlot(x + 1, 5).setStandardSlotBackground()
					.addItemStacks(recipe.ingredient2().map(second -> stacks(second, recipe.count2())).orElse(List.of()));
		}
		builder.addOutputSlot(66, 5).setOutputSlotBackground().add(recipe.output());
		recipe.byproduct().ifPresent(byproduct -> builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 5)
				.setStandardSlotBackground()
				.add(byproduct.item())
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.sanupcraft.chance",
						String.format(Locale.ROOT, "%.0f", byproduct.chance() * 100.0F)))));
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
		int arrowX = kind.twoInputs() ? 40 : 30;
		builder.addAnimatedRecipeArrow(recipe.time()).setPosition(arrowX, 5);
		String timeKey = kind.highPressure() ? "jei.sanupcraft.machine_time_hp" : kind.usesWater() ? "jei.sanupcraft.machine_time_water" : "jei.sanupcraft.machine_time";
		Component time = Component.translatable(timeKey,
				String.format(Locale.ROOT, "%.1f", recipe.time() / 20.0F));
		builder.addText(time, WIDTH, 10).setPosition(0, 26).setColor(0xFF808080);
	}

	/** 재료 하나의 후보들을 개수만큼 쌓아서. */
	private static List<ItemStack> stacks(Ingredient ingredient, int count) {
		return ingredient.items().map(item -> new ItemStack(item.value(), count)).toList();
	}
}
