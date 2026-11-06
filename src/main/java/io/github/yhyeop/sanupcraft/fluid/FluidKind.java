package io.github.yhyeop.sanupcraft.fluid;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;

/**
 * 탱크 · 펌프가 담는 유체 (물 · 용암 · 경험치). 양은 밀리버킷(mB) 정수, 양동이 하나 = {@link #BUCKET}.
 * 경험치(M9 P2)는 양동이가 없는 유체다: 경험치 탱크만 담고, 경험치 1점 = {@link #MB_PER_XP} mB.
 */
public enum FluidKind implements StringRepresentable {
	WATER("water", Items.WATER_BUCKET, SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY),
	LAVA("lava", Items.LAVA_BUCKET, SoundEvents.BUCKET_FILL_LAVA, SoundEvents.BUCKET_EMPTY_LAVA),
	/** M9 P2: 양동이 없음 ({@link #bucket()}이 null). 경험치 탱크 전용. */
	EXPERIENCE("experience", null, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundEvents.EXPERIENCE_ORB_PICKUP);

	public static final int BUCKET = 1000;
	/** 경험치 1점이 차지하는 mB (M9 P2에서 정함). */
	public static final int MB_PER_XP = 20;
	public static final Codec<FluidKind> CODEC = StringRepresentable.fromEnum(FluidKind::values);
	public static final StreamCodec<ByteBuf, FluidKind> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

	private final String name;
	private final @Nullable Item bucket;
	private final SoundEvent fillSound;
	private final SoundEvent emptySound;

	FluidKind(String name, @Nullable Item bucket, SoundEvent fillSound, SoundEvent emptySound) {
		this.name = name;
		this.bucket = bucket;
		this.fillSound = fillSound;
		this.emptySound = emptySound;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	/** 이 유체가 든 양동이. 양동이가 없는 유체(경험치)는 null. */
	public @Nullable Item bucket() {
		return bucket;
	}

	/** 양동이로 퍼 낼 때 소리. */
	public SoundEvent fillSound() {
		return fillSound;
	}

	/** 양동이를 부을 때 소리. */
	public SoundEvent emptySound() {
		return emptySound;
	}

	/** 언어 파일 키 (`fluid.sanupcraft.water`). */
	public String translationKey() {
		return "fluid.sanupcraft." + name;
	}

	/** 양동이 아이템이 담은 유체. 물 · 용암 양동이가 아니면 null. */
	public static @Nullable FluidKind ofBucket(ItemStack stack) {
		for (FluidKind kind : values()) {
			if (kind.bucket != null && stack.is(kind.bucket)) {
				return kind;
			}
		}
		return null;
	}

	/** 월드의 유체 상태가 무엇인지 (원천이든 흐르는 것이든). 물 · 용암이 아니면 null. */
	public static @Nullable FluidKind of(FluidState state) {
		if (state.is(FluidTags.WATER)) {
			return WATER;
		}
		if (state.is(FluidTags.LAVA)) {
			return LAVA;
		}
		return null;
	}

	public static @Nullable FluidKind byName(String name) {
		for (FluidKind kind : values()) {
			if (kind.name.equals(name)) {
				return kind;
			}
		}
		return null;
	}
}
