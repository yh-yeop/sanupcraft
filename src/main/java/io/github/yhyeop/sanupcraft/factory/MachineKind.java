package io.github.yhyeop.sanupcraft.factory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import io.github.yhyeop.sanupcraft.Sanupcraft;

/** 기계 종류. 종류마다 레시피 타입(`sanupcraft:crushing` 등)이 따로 있다. */
public enum MachineKind {
	CRUSHER("crushing"),
	PRESS("pressing"),
	ASSEMBLER("assembling");

	private final String recipeName;
	private RecipeType<MachineRecipe> recipeType;
	private RecipeSerializer<MachineRecipe> serializer;

	MachineKind(String recipeName) {
		this.recipeName = recipeName;
	}

	public RecipeType<MachineRecipe> recipeType() {
		return recipeType;
	}

	public RecipeSerializer<MachineRecipe> serializer() {
		return serializer;
	}

	static void register() {
		for (MachineKind kind : values()) {
			String name = kind.recipeName;
			kind.recipeType = Registry.register(BuiltInRegistries.RECIPE_TYPE, Sanupcraft.id(name), new RecipeType<MachineRecipe>() {
				@Override
				public String toString() {
					return name;
				}
			});
			kind.serializer = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Sanupcraft.id(name),
					new RecipeSerializer<>(MachineRecipe.codec(kind), MachineRecipe.streamCodec(kind)));
		}
	}
}
