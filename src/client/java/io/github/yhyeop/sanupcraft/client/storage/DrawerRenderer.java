package io.github.yhyeop.sanupcraft.client.storage;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.storage.DrawerBlock;
import io.github.yhyeop.sanupcraft.storage.DrawerBlockEntity;

/** 서랍 앞면: 담긴 아이템 그림과 그 아래 개수 (1.2k처럼 줄여서). 빛은 앞 칸의 밝기를 쓴다. */
public class DrawerRenderer implements BlockEntityRenderer<DrawerBlockEntity, DrawerRenderer.State> {
	private final ItemModelResolver itemModelResolver;
	private final Font font;

	public static class State extends BlockEntityRenderState {
		final ItemStackRenderState item = new ItemStackRenderState();
		boolean hasItem;
		Direction facing = Direction.NORTH;
		@Nullable FormattedCharSequence countText;
		int frontLight;
		int upgrades;
		boolean voiding;
	}

	public DrawerRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
		this.font = context.font();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DrawerBlockEntity drawer, State state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(drawer, state, partialTicks, cameraPosition, breakProgress);
		state.facing = drawer.getBlockState().getValue(DrawerBlock.FACING);
		state.frontLight = drawer.getLevel() != null ? LightCoordsUtil.getLightCoords(drawer.getLevel(), drawer.getBlockPos().relative(state.facing)) : state.lightCoords;
		ItemStack template = drawer.template();
		state.hasItem = !template.isEmpty();
		state.upgrades = drawer.upgrades();
		state.voiding = drawer.isVoiding();
		if (state.hasItem) {
			itemModelResolver.updateForTopItem(state.item, template, ItemDisplayContext.FIXED, drawer.getLevel(), null, (int) drawer.getBlockPos().asLong());
			state.countText = Component.literal(shortCount(drawer.count())).getVisualOrderText();
		} else {
			state.item.clear();
			state.countText = null;
		}
	}

	/** 1234 → 1.2k, 1234567 → 1.2M. */
	public static String shortCount(int count) {
		if (count < 1000) {
			return Integer.toString(count);
		}
		if (count < 1_000_000) {
			return count < 10_000 ? String.format("%.1fk", count / 1000.0) : (count / 1000) + "k";
		}
		return String.format("%.1fM", count / 1_000_000.0);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		// 앞면 가운데로 가서, -z가 바깥을 보게 돌린다 (액자와 같은 방향)
		poseStack.translate(0.5F + state.facing.getStepX() * 0.5F, 0.5F, 0.5F + state.facing.getStepZ() * 0.5F);
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());
		if (state.hasItem) {
			poseStack.pushPose();
			poseStack.translate(0.0F, 0.08F, -0.03F);
			poseStack.scale(0.5F, 0.5F, 0.08F);
			state.item.submit(poseStack, collector, state.frontLight, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
		if (state.countText != null) {
			poseStack.pushPose();
			poseStack.translate(0.0F, -0.3F, -0.006F);
			poseStack.rotateDegrees(Axis.YP, 180.0F);
			float scale = 1.0F / 80.0F;
			poseStack.scale(scale, -scale, scale);
			float x = -font.width(state.countText) / 2.0F;
			collector.submitText(poseStack, x, -4.0F, state.countText, false, Font.DisplayMode.POLYGON_OFFSET, state.frontLight, 0xFFFFFFFF, 0, 0xFF000000);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
