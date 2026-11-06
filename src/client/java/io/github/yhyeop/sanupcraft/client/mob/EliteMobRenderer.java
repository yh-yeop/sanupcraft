package io.github.yhyeop.sanupcraft.client.mob;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import io.github.yhyeop.sanupcraft.client.boss.BossGlowLayer;
import io.github.yhyeop.sanupcraft.client.boss.BossModelLayers;
import io.github.yhyeop.sanupcraft.mob.EliteAbility;
import io.github.yhyeop.sanupcraft.mob.EliteMonster;

/**
 * 새 몹 셋의 렌더러 (확장 3 C2 · R7). 전용 모델 + 빛나는 레이어(보스와 같은 {@link BossGlowLayer}) + 정예 테두리({@link EliteAuraLayer}).
 * 정예는 빛이 늘 가득(맥박 없이 1.0) 켜지고 {@link #ELITE_SCALE}배 크다.
 */
public class EliteMobRenderer<T extends EliteMonster> extends MobRenderer<T, MobRenderState, EntityModel<MobRenderState>> {
	public static final float ELITE_SCALE = 1.12F;
	private final Identifier texture;
	private final float scale;
	private final boolean fullBright;

	public EliteMobRenderer(EntityRendererProvider.Context context, Function<ModelPart, EntityModel<MobRenderState>> model,
			ModelLayerLocation main, ModelLayerLocation aura, String name, float shadow, float scale, boolean fullBright,
			ToDoubleFunction<MobRenderState> glow) {
		super(context, model.apply(context.bakeLayer(main)), shadow * scale);
		this.texture = BossModelLayers.texture(name);
		this.scale = scale;
		this.fullBright = fullBright;
		addLayer(new BossGlowLayer<>(this, BossModelLayers.texture(name + "_glow"), state -> state.elite ? 1.0 : glow.applyAsDouble(state)));
		addLayer(new EliteAuraLayer(this, model.apply(context.bakeLayer(aura))));
	}

	@Override
	public Identifier getTextureLocation(MobRenderState state) {
		return texture;
	}

	@Override
	public MobRenderState createRenderState() {
		return new MobRenderState();
	}

	@Override
	public void extractRenderState(T entity, MobRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		EliteAbility ability = entity.eliteAbility();
		state.elite = ability != null;
		state.eliteColor = ability == null ? 0 : ability.rgb();
		state.aggressive = entity.isAggressive();
		state.swing = entity.getSwingAnimation(partialTicks);
	}

	@Override
	protected void scale(MobRenderState state, PoseStack poseStack) {
		float s = state.elite ? scale * ELITE_SCALE : scale;
		poseStack.scale(s, s, s);
	}

	@Override
	protected int getBlockLightLevel(T entity, BlockPos pos) {
		return fullBright ? 15 : super.getBlockLightLevel(entity, pos);
	}
}
