package io.github.yhyeop.sanupcraft.client.jei;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.factory.MachineKind;
import io.github.yhyeop.sanupcraft.factory.MachineRecipe;
import io.github.yhyeop.sanupcraft.factory.ModMachines;

/**
 * JEI 연동: 분쇄기 · 압착기 · 조립기 · 합금로 · 심장 제련로 레시피를 JEI에 띄운다.
 * JEI가 있을 때만 이 클래스를 불러온다 (fabric.mod.json의 {@code jei_mod_plugin}). 없으면 아무 일도 없다.
 * <p>
 * 레시피 출처: 싱글(통합 서버)이면 서버 레시피를 그대로 쓰고(데이터팩 반영), 멀티 서버에 붙었으면 클라이언트가 레시피를 받지 못하므로
 * 모드 jar 안의 레시피 JSON을 직접 읽는다 (서버가 데이터팩으로 바꾼 것은 반영되지 않는다).
 */
public class SanupcraftJeiPlugin implements IModPlugin {
	private static final Map<MachineKind, Block> MACHINES = Map.of(
			MachineKind.CRUSHER, ModMachines.CRUSHER,
			MachineKind.PRESS, ModMachines.PRESS,
			MachineKind.ASSEMBLER, ModMachines.ASSEMBLER,
			MachineKind.ALLOY, ModMachines.ALLOY_SMELTER,
			MachineKind.FORGE, ModMachines.HEART_FORGE,
			MachineKind.PRECISION, ModMachines.PRECISION_ASSEMBLER);
	/** JEI 목록에 나오는 순서 (진행 순서). */
	private static final MachineKind[] ORDER = {MachineKind.CRUSHER, MachineKind.PRESS, MachineKind.ALLOY, MachineKind.ASSEMBLER, MachineKind.FORGE, MachineKind.PRECISION};

	@Override
	public Identifier getPluginUid() {
		return Sanupcraft.id("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		for (MachineKind kind : ORDER) {
			registration.addRecipeCategories(new MachineRecipeCategory(gui, kind, MACHINES.get(kind)));
		}
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		int total = 0;
		for (MachineKind kind : ORDER) {
			List<MachineRecipe> list = recipes(kind);
			registration.addRecipes(MachineRecipeCategory.type(kind), list);
			total += list.size();
		}
		Sanupcraft.LOGGER.info("JEI: 기계 레시피 {}개를 올림", total);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		for (MachineKind kind : ORDER) {
			registration.addCraftingStation(MachineRecipeCategory.type(kind), MACHINES.get(kind));
		}
		// M7: 제작 단말 · 자동 제작대
		TerminalRecipeTransfer.addCatalysts(registration);
	}

	/** M7: JEI 제작 레시피 + 버튼 → 제작 단말. */
	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addRecipeTransferHandler(new TerminalRecipeTransfer(registration.getTransferHelper()), RecipeTypes.CRAFTING);
	}

	/** 이 기계의 레시피들. 이름순으로 정렬한다. */
	static List<MachineRecipe> recipes(MachineKind kind) {
		Minecraft minecraft = Minecraft.getInstance();
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server != null) {
			return server.getRecipeManager().getRecipes().stream()
					.filter(holder -> holder.value() instanceof MachineRecipe recipe && recipe.kind() == kind)
					.sorted(Comparator.comparing((RecipeHolder<?> holder) -> holder.id().identifier().toString()))
					.map(holder -> (MachineRecipe) holder.value())
					.toList();
		}
		if (minecraft.level == null) {
			return List.of();
		}
		return fromModJar(kind, minecraft.level.registryAccess());
	}

	/** 모드 jar의 {@code data/sanupcraft/recipe/<종류>/*.json}을 읽는다. 못 읽은 파일은 건너뛰고 기록만 남긴다. */
	static List<MachineRecipe> fromModJar(MachineKind kind, HolderLookup.Provider registries) {
		Optional<Path> folder = FabricLoader.getInstance().getModContainer(Sanupcraft.MOD_ID)
				.flatMap((ModContainer mod) -> mod.findPath("data/" + Sanupcraft.MOD_ID + "/recipe/" + kind.recipeName()));
		if (folder.isEmpty()) {
			return List.of();
		}
		RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
		List<MachineRecipe> result = new ArrayList<>();
		try (Stream<Path> files = Files.walk(folder.get())) {
			for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
				try (Reader reader = Files.newBufferedReader(file)) {
					JsonElement json = JsonParser.parseReader(reader);
					MachineRecipe.codec(kind).codec().parse(ops, json)
							.resultOrPartial(error -> Sanupcraft.LOGGER.warn("JEI: 레시피 {}를 읽지 못함: {}", file, error))
							.ifPresent(result::add);
				} catch (IOException | JsonParseException e) {
					// 파일 하나가 깨져도 그 파일만 건너뛰고 같은 폴더의 나머지는 계속 읽는다
					Sanupcraft.LOGGER.warn("JEI: 레시피 {}를 읽지 못함", file, e);
				}
			}
		} catch (IOException | UncheckedIOException e) {
			// Files.walk 시작 또는 순회 중 실패 (순회 실패는 UncheckedIOException으로 온다)
			Sanupcraft.LOGGER.warn("JEI: {} 레시피 폴더를 읽지 못함", kind.recipeName(), e);
		}
		return result;
	}
}
