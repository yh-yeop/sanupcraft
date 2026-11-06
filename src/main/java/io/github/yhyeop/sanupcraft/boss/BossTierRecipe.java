package io.github.yhyeop.sanupcraft.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 소환석 강화 레시피 ({@code sanupcraft:crafting_boss_tier}): 소환석 하나 + 재료 {@code count}개만 놓으면
 * 단계가 한 칸 오른 소환석 하나가 나온다. 이미 최대 단계({@link BossTier#MAX})면 맞지 않는다. 모양은 상관없다.
 */
public class BossTierRecipe extends CustomRecipe {
	public static final MapCodec<BossTierRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Item.CODEC.fieldOf("summoner").forGetter(r -> r.summoner),
			Item.CODEC.fieldOf("material").forGetter(r -> r.material),
			Codec.intRange(1, 8).fieldOf("count").forGetter(r -> r.count)).apply(i, BossTierRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, BossTierRecipe> STREAM_CODEC = StreamCodec.composite(
			Item.STREAM_CODEC, r -> r.summoner,
			Item.STREAM_CODEC, r -> r.material,
			ByteBufCodecs.VAR_INT, r -> r.count,
			BossTierRecipe::new);

	private final Holder<Item> summoner;
	private final Holder<Item> material;
	private final int count;

	public BossTierRecipe(Holder<Item> summoner, Holder<Item> material, int count) {
		this.summoner = summoner;
		this.material = material;
		this.count = count;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		int summoners = 0;
		int materials = 0;
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.is(summoner)) {
				if (BossTier.of(stack) >= BossTier.MAX) {
					return false;
				}
				summoners++;
			} else if (stack.is(material)) {
				materials++;
			} else {
				return false;
			}
		}
		return summoners == 1 && materials == count;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (stack.is(summoner)) {
				ItemStack result = stack.copyWithCount(1);
				BossTier.set(result, BossTier.of(stack) + 1);
				return result;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<BossTierRecipe> getSerializer() {
		return BossTier.RECIPE_SERIALIZER;
	}
}
