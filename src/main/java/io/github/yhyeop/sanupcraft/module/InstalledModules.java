package io.github.yhyeop.sanupcraft.module;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** 장비에 끼운 모듈 목록 (끼운 순서). 데이터 컴포넌트 {@code sanupcraft:modules}의 값 (저장 · 동기화). 바꿀 수 없는 값이라 고칠 때는 새로 만든다. */
public record InstalledModules(List<GearModule> modules) {
	public static final InstalledModules EMPTY = new InstalledModules(List.of());
	public static final Codec<InstalledModules> CODEC = GearModule.CODEC.listOf().xmap(InstalledModules::new, InstalledModules::modules);
	public static final StreamCodec<ByteBuf, InstalledModules> STREAM_CODEC = GearModule.STREAM_CODEC.apply(ByteBufCodecs.list())
			.map(InstalledModules::new, InstalledModules::modules);

	public InstalledModules {
		modules = List.copyOf(modules);
	}

	public boolean has(GearModule module) {
		return modules.contains(module);
	}

	public int size() {
		return modules.size();
	}

	public boolean isEmpty() {
		return modules.isEmpty();
	}

	public InstalledModules with(GearModule module) {
		List<GearModule> list = new ArrayList<>(modules);
		list.add(module);
		return new InstalledModules(list);
	}

	public InstalledModules without(GearModule module) {
		List<GearModule> list = new ArrayList<>(modules);
		list.remove(module);
		return new InstalledModules(list);
	}
}
