package io.github.yhyeop.sanupcraft.client.mob;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.util.Mth;

import io.github.yhyeop.sanupcraft.Sanupcraft;
import io.github.yhyeop.sanupcraft.mob.ModMobs;

/**
 * 새 몹 셋(확장 3 C2 · R7)의 전용 모델 레이어와 렌더러 등록. 보스 모델(client/boss)과 같은 방식: 모양은 각 모델의 createBodyLayer(),
 * 텍스처는 textures/entity/&lt;몹&gt;.png · &lt;몹&gt;_glow.png (tools/make_textures.py 의 main_ex3_c2).
 * 정예는 같은 모양을 조금 부풀린 aura 레이어에 능력 색 무늬가 흐른다 ({@link EliteAuraLayer}).
 */
public final class MobModelLayers {
	private static final CubeDeformation AURA = new CubeDeformation(0.45F);

	public static final ModelLayerLocation RUST_AUTOMATON = layer("rust_automaton", "main");
	public static final ModelLayerLocation RUST_AUTOMATON_AURA = layer("rust_automaton", "aura");
	public static final ModelLayerLocation EMBER_WISP = layer("ember_wisp", "main");
	public static final ModelLayerLocation EMBER_WISP_AURA = layer("ember_wisp", "aura");
	public static final ModelLayerLocation VOID_SHADE = layer("void_shade", "main");
	public static final ModelLayerLocation VOID_SHADE_AURA = layer("void_shade", "aura");

	private MobModelLayers() {
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(RUST_AUTOMATON, () -> RustAutomatonModel.createBodyLayer(CubeDeformation.NONE));
		ModelLayerRegistry.registerModelLayer(RUST_AUTOMATON_AURA, () -> RustAutomatonModel.createBodyLayer(AURA));
		ModelLayerRegistry.registerModelLayer(EMBER_WISP, () -> EmberWispModel.createBodyLayer(CubeDeformation.NONE));
		ModelLayerRegistry.registerModelLayer(EMBER_WISP_AURA, () -> EmberWispModel.createBodyLayer(AURA));
		ModelLayerRegistry.registerModelLayer(VOID_SHADE, () -> VoidShadeModel.createBodyLayer(CubeDeformation.NONE));
		ModelLayerRegistry.registerModelLayer(VOID_SHADE_AURA, () -> VoidShadeModel.createBodyLayer(AURA));

		// 자동인형: 외눈 · 가슴 계기 · 굴뚝 불씨가 천천히 깜박인다
		EntityRendererRegistry.register(ModMobs.RUST_AUTOMATON, context -> new EliteMobRenderer<>(context, RustAutomatonModel::new,
				RUST_AUTOMATON, RUST_AUTOMATON_AURA, "rust_automaton", 0.55F, 1.0F, false,
				state -> 0.55 + 0.25 * Mth.sin(state.ageInTicks * 0.15F) + (Mth.sin(state.ageInTicks * 1.7F) > 0.93F ? -0.4 : 0.0)));
		// 정령: 불꽃이라 늘 밝고 빠르게 일렁인다 (블레이즈처럼 어둠에서도 밝게 그린다)
		EntityRendererRegistry.register(ModMobs.EMBER_WISP, context -> new EliteMobRenderer<>(context, EmberWispModel::new,
				EMBER_WISP, EMBER_WISP_AURA, "ember_wisp", 0.35F, 1.0F, true,
				state -> 0.75 + 0.2 * Mth.sin(state.ageInTicks * 0.6F)));
		// 그림자: 눈 · 가슴 균열이 느리게 맥박친다
		EntityRendererRegistry.register(ModMobs.VOID_SHADE, context -> new EliteMobRenderer<>(context, VoidShadeModel::new,
				VOID_SHADE, VOID_SHADE_AURA, "void_shade", 0.4F, 1.0F, false,
				state -> 0.6 + 0.25 * Mth.sin(state.ageInTicks * 0.1F)));
		EntityRendererRegistry.register(ModMobs.EMBER_BOLT, context -> new ThrownItemRenderer<>(context, 0.6F, true));
	}

	private static ModelLayerLocation layer(String name, String part) {
		return new ModelLayerLocation(Sanupcraft.id(name), part);
	}
}
