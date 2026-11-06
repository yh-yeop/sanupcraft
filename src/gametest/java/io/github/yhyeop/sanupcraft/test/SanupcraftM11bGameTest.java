package io.github.yhyeop.sanupcraft.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.factory.MachineKind;
import io.github.yhyeop.sanupcraft.factory.ModMachines;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/** M11 X2 검사: 효과음 등록 · sounds.json · 자막, 일하는 기계가 소리를 낸다. 소리가 실제로 들리는지는 클라이언트 확인. */
public class SanupcraftM11bGameTest {
	/** 등록한 소리마다 sounds.json 항목(바닐라 소리 이름 · 자막)이 있고, 자막은 두 언어에 다 있다. sounds.json에 남는 항목도 없다. */
	@GameTest
	public void soundsRegisteredAndDefined(GameTestHelper helper) {
		JsonObject sounds = readJson("/assets/sanupcraft/sounds.json");
		JsonObject ko = readJson("/assets/sanupcraft/lang/ko_kr.json");
		JsonObject en = readJson("/assets/sanupcraft/lang/en_us.json");
		Set<String> registered = new HashSet<>();
		// 확장 3 C2 새 몹 소리(MobSounds)도 같은 sounds.json에 있다
		for (SoundEvent event : java.util.stream.Stream.concat(ModSounds.all().stream(), io.github.yhyeop.sanupcraft.mob.MobSounds.all().stream()).toList()) {
			Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(event);
			helper.assertTrue(id != null && id.getNamespace().equals(Sanupcraft.MOD_ID), "등록 안 된 소리: " + event.location());
			helper.assertTrue(BuiltInRegistries.SOUND_EVENT.getValue(id) == event, "레지스트리에서 다른 소리: " + id);
			registered.add(id.getPath());
			JsonObject entry = sounds.getAsJsonObject(id.getPath());
			helper.assertTrue(entry != null, "sounds.json에 없음: " + id);
			JsonArray files = entry.getAsJsonArray("sounds");
			helper.assertTrue(files != null && !files.isEmpty(), "소리 파일이 없음: " + id);
			for (JsonElement file : files) {
				String name = file.isJsonObject() ? file.getAsJsonObject().get("name").getAsString() : file.getAsString();
				helper.assertTrue(name.startsWith("minecraft:"), "바닐라 소리가 아님: " + name);
			}
			String subtitle = entry.get("subtitle").getAsString();
			helper.assertTrue(ko.has(subtitle) && en.has(subtitle), "자막 번역이 없음: " + subtitle);
		}
		helper.assertValueEqual(sounds.keySet(), registered, "sounds.json 항목");
		for (MachineKind kind : new MachineKind[] {MachineKind.CRUSHER, MachineKind.PRESS, MachineKind.PRECISION}) {
			helper.assertTrue(ModSounds.forMachine(kind) != null, kind + " 작동음이 없음");
		}
		helper.succeed();
	}

	/** 연료를 넣은 분쇄기가 도는 동안 작동음을 한 번은 낸다 (다른 시험과 섞이지 않게 혼자 돈다). */
	@GameTest(environment = "sanupcraft-test:alone", maxTicks = 120)
	public void runningMachinePlaysSound(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModMachines.CRUSHER);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 16));
		crusher.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
		int before = ModSounds.playedCount();
		helper.succeedWhen(() -> helper.assertTrue(ModSounds.playedCount() > before, "작동음을 내지 않음"));
	}

	private static JsonObject readJson(String path) {
		try (InputStream in = Sanupcraft.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new AssertionError("자원이 없음: " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new AssertionError("읽기 실패: " + path, e);
		}
	}
}
