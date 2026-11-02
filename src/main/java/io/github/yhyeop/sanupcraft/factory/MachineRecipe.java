package io.github.yhyeop.sanupcraft.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * 재료 1개 → 결과물. 데이터팩 JSON 예:
 * {"type": "sanupcraft:crushing", "ingredient": "minecraft:raw_iron", "result": {"id": "sanupcraft:iron_dust", "count": 2}, "time": 150}
 */
public class MachineRecipe extends SingleItemRecipe {
	public static final int DEFAULT_TIME = 200;

	private final MachineKind kind;
	private final int time;

	public MachineRecipe(MachineKind kind, Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result, int time) {
		super(commonInfo, ingredient, result);
		this.kind = kind;
		this.time = time;
	}

	/** 처리 시간 (틱). */
	public int time() {
		return time;
	}

	public ItemStackTemplate output() {
		return result();
	}

	@Override
	public RecipeSerializer<MachineRecipe> getSerializer() {
		return kind.serializer();
	}

	@Override
	public RecipeType<MachineRecipe> getType() {
		return kind.recipeType();
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return ModMachines.RECIPE_BOOK_CATEGORY;
	}

	static MapCodec<MachineRecipe> codec(MachineKind kind) {
		return RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
				Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(MachineRecipe::output),
				Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(MachineRecipe::time)
		).apply(i, (commonInfo, ingredient, result, time) -> new MachineRecipe(kind, commonInfo, ingredient, result, time)));
	}

	static StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec(MachineKind kind) {
		return StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
				Ingredient.CONTENTS_STREAM_CODEC, SingleItemRecipe::input,
				ItemStackTemplate.STREAM_CODEC, MachineRecipe::output,
				ByteBufCodecs.VAR_INT, MachineRecipe::time,
				(commonInfo, ingredient, result, time) -> new MachineRecipe(kind, commonInfo, ingredient, result, time));
	}
}
