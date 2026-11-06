package io.github.yhyeop.sanupcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.core.ModItems;
import io.github.yhyeop.sanupcraft.factory.MachineBlockEntity;
import io.github.yhyeop.sanupcraft.fluid.FluidKind;
import io.github.yhyeop.sanupcraft.nether.ModNether;
import io.github.yhyeop.sanupcraft.power.BoilerBlockEntity;
import io.github.yhyeop.sanupcraft.sound.ModSounds;

/**
 * M11 X2 클라이언트 확인: sounds.json을 클라이언트가 읽어 모든 소리에 바닐라 소리 파일 · 자막이 붙는지 보고, 한 번씩 틀어 본다.
 * 그다음 돌고 있는 보일러(김) · 정밀 조립기(불꽃) · 세척기(물방울) · 분쇄기를 스크린샷으로 남긴다 (서버가 보낸 작동음도 이때 들림).
 * 로그에 "Missing sound for event" · "Unable to play unknown soundEvent" · "File ... does not exist"가 없어야 한다.
 * 스크린샷: build/run/clientGameTest/screenshots/m11b-*.png
 */
public class SanupcraftM11bClientGameTest implements FabricClientGameTest {
	private static final BlockPos BOILER = new BlockPos(20, -60, -4);
	private static final BlockPos PRECISION = new BlockPos(22, -60, -4);
	private static final BlockPos WASHER = new BlockPos(24, -60, -4);
	private static final BlockPos CRUSHER = new BlockPos(26, -60, -4);

	@Override
	public void runTest(ClientGameTestContext context) {
		for (SoundEvent event : ModSounds.all()) {
			WeighedSoundEvents loaded = context.computeOnClient(client -> client.getSoundManager().getSoundEvent(event.location()));
			if (loaded == null || loaded.getSubtitle() == null) {
				throw new AssertionError("클라이언트가 소리를 모름 (sounds.json): " + event.location());
			}
			context.runOnClient(client -> client.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F, 0.05F)));
		}
		Sanupcraft.LOGGER.info("M11b 클라이언트: 소리 {}개 확인", ModSounds.all().size());

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			server.runCommand("gamemode creative @a");
			server.runCommand("setblock 20 -60 -4 sanupcraft:boiler[facing=south]");
			server.runCommand("setblock 22 -60 -4 sanupcraft:precision_assembler[facing=south]");
			server.runCommand("setblock 24 -60 -4 sanupcraft:washer[facing=south]");
			server.runCommand("setblock 26 -60 -4 sanupcraft:crusher[facing=south]");
			server.runOnServer(s -> {
				BoilerBlockEntity boiler = (BoilerBlockEntity) s.overworld().getBlockEntity(BOILER);
				boiler.fill(FluidKind.WATER, BoilerBlockEntity.WATER_CAPACITY, false);
				boiler.setItem(0, new ItemStack(Items.COAL, 8));
				MachineBlockEntity precision = (MachineBlockEntity) s.overworld().getBlockEntity(PRECISION);
				precision.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(ModItems.STEEL_PLATE, 16));
				precision.setItem(MachineBlockEntity.SLOT_INPUT2, new ItemStack(ModNether.GILDED_QUARTZ, 16));
				MachineBlockEntity washer = (MachineBlockEntity) s.overworld().getBlockEntity(WASHER);
				washer.fill(FluidKind.WATER, MachineBlockEntity.WATER_CAPACITY, false);
				washer.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 16));
				washer.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
				MachineBlockEntity crusher = (MachineBlockEntity) s.overworld().getBlockEntity(CRUSHER);
				crusher.setItem(MachineBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 16));
				crusher.setItem(MachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
			});
			server.runCommand("tp @a 23.5 -58.5 0.5 180 25");
			// 정밀 조립기는 고압 증기로만 돈다: 파이프 대신 틱마다 채워 준다
			for (int i = 0; i < 60; i++) {
				server.runOnServer(s -> ((MachineBlockEntity) s.overworld().getBlockEntity(PRECISION)).acceptHighPressureSteam(MachineBlockEntity.STEAM_CAPACITY));
				context.waitTick();
			}
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("m11b-machines");
			String lit = server.computeOnServer(s -> {
				StringBuilder text = new StringBuilder();
				for (BlockPos pos : new BlockPos[] {BOILER, PRECISION, WASHER, CRUSHER}) {
					var state = s.overworld().getBlockState(pos);
					text.append(state.getBlock().getDescriptionId()).append('=').append(state.getValue(BlockStateProperties.LIT)).append(' ');
				}
				return text.toString();
			});
			Sanupcraft.LOGGER.info("M11b 클라이언트: 켜짐 {}", lit);
			int played = server.computeOnServer(s -> ModSounds.playedCount());
			Sanupcraft.LOGGER.info("M11b 클라이언트: 서버가 낸 작동음 {}번", played);
			if (played <= 0) {
				throw new AssertionError("기계 작동음이 한 번도 안 남");
			}
		}
	}
}
