package io.github.yhyeop.sanupcraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 부순 서랍 아이템에 남는 내용물 (데이터 컴포넌트 {@code sanupcraft:drawer_contents}). 다시 놓으면 그대로 돌아온다.
 * item은 1개짜리 견본 (비었으면 빈 스택), count는 실제 개수.
 */
public record DrawerContents(ItemStack item, int count, int upgrades, boolean voiding) {
	public static final DrawerContents EMPTY = new DrawerContents(ItemStack.EMPTY, 0, 0, false);

	public static final Codec<DrawerContents> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(DrawerContents::item),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("count", 0).forGetter(DrawerContents::count),
			Codec.intRange(0, DrawerBlockEntity.MAX_UPGRADES).optionalFieldOf("upgrades", 0).forGetter(DrawerContents::upgrades),
			Codec.BOOL.optionalFieldOf("voiding", false).forGetter(DrawerContents::voiding)
	).apply(i, DrawerContents::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, DrawerContents> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, DrawerContents::item,
			ByteBufCodecs.VAR_INT, DrawerContents::count,
			ByteBufCodecs.VAR_INT, DrawerContents::upgrades,
			ByteBufCodecs.BOOL, DrawerContents::voiding,
			DrawerContents::new);

	public boolean isEmpty() {
		return (item.isEmpty() || count <= 0) && upgrades == 0 && !voiding;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DrawerContents that && count == that.count && upgrades == that.upgrades && voiding == that.voiding
				&& ItemStack.matches(item, that.item);
	}

	@Override
	public int hashCode() {
		return ((ItemStack.hashItemAndComponents(item) * 31 + count) * 31 + upgrades) * 2 + (voiding ? 1 : 0);
	}
}
