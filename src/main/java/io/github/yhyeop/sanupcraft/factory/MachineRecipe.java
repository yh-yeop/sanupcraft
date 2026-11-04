package io.github.yhyeop.sanupcraft.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 기계 레시피. 재료 하나(합금로는 둘) → 결과 + 확률 부산물. 데이터팩 JSON 예:
 * {"type": "sanupcraft:crushing", "ingredient": "minecraft:raw_iron", "result": {"id": "sanupcraft:iron_dust", "count": 2},
 *  "byproduct": {"item": {"id": "sanupcraft:gold_dust"}, "chance": 0.1}, "time": 150}
 * 합금로: "ingredient2", "count2" (재료 칸 순서는 상관없음), 재료 여러 개는 "count".
 */
public class MachineRecipe implements Recipe<MachineInput> {
	public static final int DEFAULT_TIME = 200;

	/** 부산물: 하나 만들 때마다 chance(0~1) 확률로 부산물 칸에 나온다. */
	public record Byproduct(ItemStackTemplate item, float chance) {
		public static final Codec<Byproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
				ItemStackTemplate.CODEC.fieldOf("item").forGetter(Byproduct::item),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Byproduct::chance)
		).apply(i, Byproduct::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Byproduct> STREAM_CODEC = StreamCodec.composite(
				ItemStackTemplate.STREAM_CODEC, Byproduct::item, ByteBufCodecs.FLOAT, Byproduct::chance, Byproduct::new);
	}

	private final MachineKind kind;
	private final Recipe.CommonInfo commonInfo;
	private final Ingredient ingredient;
	private final int count;
	private final Optional<Ingredient> ingredient2;
	private final int count2;
	private final ItemStackTemplate result;
	private final Optional<Byproduct> byproduct;
	private final int time;
	private PlacementInfo placementInfo;

	public MachineRecipe(MachineKind kind, Recipe.CommonInfo commonInfo, Ingredient ingredient, int count, Optional<Ingredient> ingredient2, int count2,
			ItemStackTemplate result, Optional<Byproduct> byproduct, int time) {
		this.kind = kind;
		this.commonInfo = commonInfo;
		this.ingredient = ingredient;
		this.count = count;
		this.ingredient2 = ingredient2;
		this.count2 = count2;
		this.result = result;
		this.byproduct = byproduct;
		this.time = time;
	}

	/** 처리 시간 (틱). */
	public int time() {
		return time;
	}

	public ItemStackTemplate output() {
		return result;
	}

	public Optional<Byproduct> byproduct() {
		return byproduct;
	}

	public int count() {
		return count;
	}

	public boolean hasSecondIngredient() {
		return ingredient2.isPresent();
	}

	@Override
	public boolean matches(MachineInput input, Level level) {
		return orientation(input) != null;
	}

	/** 재료 칸이 레시피와 맞으면 [첫 재료 칸, 둘째 재료 칸] 순서(0 · 1 또는 1 · 0), 아니면 null. */
	public int[] orientation(MachineInput input) {
		if (ingredient2.isEmpty()) {
			return fits(input.first(), ingredient, count) ? new int[]{0, 1} : null;
		}
		Ingredient second = ingredient2.get();
		if (fits(input.first(), ingredient, count) && fits(input.second(), second, count2)) {
			return new int[]{0, 1};
		}
		if (fits(input.second(), ingredient, count) && fits(input.first(), second, count2)) {
			return new int[]{1, 0};
		}
		return null;
	}

	private static boolean fits(ItemStack stack, Ingredient ingredient, int count) {
		return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= count;
	}

	/** 만든 만큼 재료 칸에서 덜어 낸다. */
	public void consume(MachineInput input) {
		int[] order = orientation(input);
		if (order == null) {
			return;
		}
		input.getItem(order[0]).shrink(count);
		if (ingredient2.isPresent()) {
			input.getItem(order[1]).shrink(count2);
		}
	}

	@Override
	public ItemStack assemble(MachineInput input) {
		return result.create();
	}

	@Override
	public boolean showNotification() {
		return commonInfo.showNotification();
	}

	@Override
	public String group() {
		return "";
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
	public PlacementInfo placementInfo() {
		if (placementInfo == null) {
			List<Ingredient> ingredients = new ArrayList<>();
			ingredients.add(ingredient);
			ingredient2.ifPresent(ingredients::add);
			placementInfo = PlacementInfo.create(ingredients);
		}
		return placementInfo;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return ModMachines.RECIPE_BOOK_CATEGORY;
	}

	static MapCodec<MachineRecipe> codec(MachineKind kind) {
		return RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
				Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.ingredient),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(r -> r.count),
				Ingredient.CODEC.optionalFieldOf("ingredient2").forGetter(r -> r.ingredient2),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("count2", 1).forGetter(r -> r.count2),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
				Byproduct.CODEC.optionalFieldOf("byproduct").forGetter(r -> r.byproduct),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(r -> r.time)
		).apply(i, (commonInfo, ingredient, count, ingredient2, count2, result, byproduct, time) ->
				new MachineRecipe(kind, commonInfo, ingredient, count, ingredient2, count2, result, byproduct, time)));
	}

	static StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec(MachineKind kind) {
		return StreamCodec.of(
				(buf, r) -> {
					Recipe.CommonInfo.STREAM_CODEC.encode(buf, r.commonInfo);
					Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient);
					ByteBufCodecs.VAR_INT.encode(buf, r.count);
					ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).encode(buf, r.ingredient2);
					ByteBufCodecs.VAR_INT.encode(buf, r.count2);
					ItemStackTemplate.STREAM_CODEC.encode(buf, r.result);
					ByteBufCodecs.optional(Byproduct.STREAM_CODEC).encode(buf, r.byproduct);
					ByteBufCodecs.VAR_INT.encode(buf, r.time);
				},
				buf -> new MachineRecipe(kind,
						Recipe.CommonInfo.STREAM_CODEC.decode(buf),
						Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf),
						ItemStackTemplate.STREAM_CODEC.decode(buf),
						ByteBufCodecs.optional(Byproduct.STREAM_CODEC).decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf)));
	}
}
