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
	ASSEMBLER("assembling"),
	/** 심장 제련로 (3단계): 고대 잔해 → 네더라이트 파편 2, 블레이즈 막대 → 가루 4. */
	FORGE("forging"),
	/** 합금로: 재료 두 가지 → 합금 (철가루 2 + 석탄 가루 2 → 강철). */
	ALLOY("alloying");

	private final String recipeName;
	private RecipeType<MachineRecipe> recipeType;
	private RecipeSerializer<MachineRecipe> serializer;

	MachineKind(String recipeName) {
		this.recipeName = recipeName;
	}

	/** 레시피 이름 (`crushing` 등). 레시피 타입 id · 데이터 폴더 이름이다. */
	public String recipeName() {
		return recipeName;
	}

	/** 재료 칸이 두 개인 기계인가. */
	public boolean twoInputs() {
		return this == ALLOY;
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
