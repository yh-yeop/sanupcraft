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
	ALLOY("alloying"),
	/** 정밀 조립기 (M5): 고압 증기로만 돈다 (연료 칸 없음). 재료 두 가지 → 정밀 부품 등. */
	PRECISION("precision"),
	// M8 묶음 B: 광석 3 · 4배
	/** 세척기: 원석 → 정제 가루 3 (+희귀 가루). 증기 · 연료에 물도 한 번에 100 mB 쓴다. */
	WASHER("washing"),
	/** 고압 정련기: 원석 → 정제 가루 4 (+희귀 가루). 고압 증기 전용. */
	REFINER("refining");

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
		return this == ALLOY || this == PRECISION;
	}

	/** 고압 증기 전용 기계인가 (M5). 보통 증기 · 연료를 받지 않고, 고압 파이프로만 증기를 받는다. */
	public boolean highPressure() {
		return this == PRECISION || this == REFINER;
	}

	/** 물을 쓰는 기계인가 (M8 세척기). 물 탱크가 있고 펌프 · 탱크 · 물 양동이로 채운다. */
	public boolean usesWater() {
		return this == WASHER;
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
