package io.github.yhyeop.sanupcraft.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** 아이템(탱크)에 담긴 유체: 데이터 컴포넌트 {@code sanupcraft:fluid}. 탱크를 부숴도 내용이 아이템에 남는다. */
public record FluidContents(FluidKind kind, int amount) {
	public static final Codec<FluidContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			FluidKind.CODEC.fieldOf("fluid").forGetter(FluidContents::kind),
			Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidContents::amount)
	).apply(instance, FluidContents::new));

	public static final StreamCodec<ByteBuf, FluidContents> STREAM_CODEC = StreamCodec.composite(
			FluidKind.STREAM_CODEC, FluidContents::kind,
			ByteBufCodecs.VAR_INT, FluidContents::amount,
			FluidContents::new);
}
