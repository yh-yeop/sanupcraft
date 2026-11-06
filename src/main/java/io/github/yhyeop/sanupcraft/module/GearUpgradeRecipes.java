package io.github.yhyeop.sanupcraft.module;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/**
 * 계승 제작대 레시피 (G4). JSON은 바닐라 모양 · 무형 레시피와 똑같고 {@code type}만 {@code sanupcraft:gear_upgrade_shaped} ·
 * {@code sanupcraft:gear_upgrade_shapeless}. 결과를 만들 때 격자의 첫 장비(내구도가 있는 칸)에서 {@link GearInheritance#carry}로 옮긴다.
 * 바닐라 레시피를 이어받아서 레시피 책 · JEI에는 보통 레시피처럼 보인다.
 */
public final class GearUpgradeRecipes {
	public static final RecipeSerializer<ShapedRecipe> SHAPED = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			Sanupcraft.id("gear_upgrade_shaped"), new RecipeSerializer<>(Shaped.MAP_CODEC, Shaped.STREAM_CODEC));
	public static final RecipeSerializer<ShapelessRecipe> SHAPELESS = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			Sanupcraft.id("gear_upgrade_shapeless"), new RecipeSerializer<>(Shapeless.MAP_CODEC, Shapeless.STREAM_CODEC));

	private GearUpgradeRecipes() {
	}

	static void init() {
	}

	/** 격자의 첫 장비 (내구도가 있는 것). 확장 3 G5: 장비가 둘 이상이면(멀티툴 = 드릴 + 전기톱 + 괭이) 옮길 것(마법 부여 · 모듈 · 이름)이 있는 첫 것을 먼저 본다. */
	static ItemStack source(CraftingInput input) {
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (GearInheritance.isGear(stack) && (stack.isEnchanted() || stack.has(ModModules.MODULES)
					|| stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))) {
				return stack;
			}
		}
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (!stack.isEmpty() && stack.isDamageableItem()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** 모양 레시피 + 계승. */
	public static final class Shaped extends ShapedRecipe {
		static final MapCodec<ShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(o -> ((Shaped) o).common),
				CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> ((Shaped) o).book),
				ShapedRecipePattern.MAP_CODEC.forGetter(o -> ((Shaped) o).shape),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> ((Shaped) o).output)
		).apply(i, (common, book, shape, output) -> (ShapedRecipe) new Shaped(common, book, shape, output)));
		static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> STREAM_CODEC = StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, o -> ((Shaped) o).common,
				CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> ((Shaped) o).book,
				ShapedRecipePattern.STREAM_CODEC, o -> ((Shaped) o).shape,
				ItemStackTemplate.STREAM_CODEC, o -> ((Shaped) o).output,
				(common, book, shape, output) -> (ShapedRecipe) new Shaped(common, book, shape, output));

		private final Recipe.CommonInfo common;
		private final CraftingRecipe.CraftingBookInfo book;
		private final ShapedRecipePattern shape;
		private final ItemStackTemplate output;

		public Shaped(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ShapedRecipePattern shape, ItemStackTemplate output) {
			super(common, book, shape, output);
			this.common = common;
			this.book = book;
			this.shape = shape;
			this.output = output;
		}

		@Override
		public ItemStack assemble(CraftingInput input) {
			return GearInheritance.carry(source(input), super.assemble(input));
		}

		@Override
		public RecipeSerializer<ShapedRecipe> getSerializer() {
			return SHAPED;
		}
	}

	/** 무형 레시피 + 계승. */
	public static final class Shapeless extends ShapelessRecipe {
		static final MapCodec<ShapelessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(o -> ((Shapeless) o).common),
				CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> ((Shapeless) o).book),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> ((Shapeless) o).output),
				Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> ((Shapeless) o).parts)
		).apply(i, (common, book, output, parts) -> (ShapelessRecipe) new Shapeless(common, book, output, parts)));
		static final StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, o -> ((Shapeless) o).common,
				CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> ((Shapeless) o).book,
				ItemStackTemplate.STREAM_CODEC, o -> ((Shapeless) o).output,
				Ingredient.CONTENTS_STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()), o -> ((Shapeless) o).parts,
				(common, book, output, parts) -> (ShapelessRecipe) new Shapeless(common, book, output, parts));

		private final Recipe.CommonInfo common;
		private final CraftingRecipe.CraftingBookInfo book;
		private final ItemStackTemplate output;
		private final List<Ingredient> parts;

		public Shapeless(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book, ItemStackTemplate output, List<Ingredient> parts) {
			super(common, book, output, parts);
			this.common = common;
			this.book = book;
			this.output = output;
			this.parts = parts;
		}

		@Override
		public ItemStack assemble(CraftingInput input) {
			return GearInheritance.carry(source(input), super.assemble(input));
		}

		@Override
		public RecipeSerializer<ShapelessRecipe> getSerializer() {
			return SHAPELESS;
		}
	}
}
