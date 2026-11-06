package io.github.yhyeop.sanupcraft.client.machine;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import io.github.yhyeop.sanupcraft.factory.MachineBlock;
import io.github.yhyeop.sanupcraft.logistics.BeltMotorBlock;
import io.github.yhyeop.sanupcraft.mining.QuarryBlock;
import io.github.yhyeop.sanupcraft.power.SteamTurbineBlock;

/**
 * 기계의 움직이는 부품 (확장 M15 R4): 분쇄기 톱니 롤러 · 압착기 피스톤 · 터빈 날개 · 굴착기 드릴 · 벨트 모터 바퀴.
 * 블록 모델은 그대로 두고 그 위(밖)에 부품을 덧그린다. 작동 중인지는 이미 있는 블록 상태(LIT · POWERED)로 본다 —
 * 서버 데이터를 더 보내지 않는다. 돌 때만 움직이고, 멈추면 쉬는 자세. 32칸 안에서만 그린다.
 * 빛은 블록 안이 아니라 부품이 나온 쪽 칸의 밝기를 쓴다 (블록 안은 어둡다).
 */
public class MachinePartsRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, MachinePartsRenderer.State> {
	/** 그리는 부품 종류. */
	public enum Look {
		NONE, CRUSHER, PRESS, TURBINE, QUARRY, BELT_MOTOR
	}

	public static class State extends BlockEntityRenderState {
		Look look = Look.NONE;
		boolean running;
		Direction facing = Direction.NORTH;
		/** 게임 시각 (틱, 부분 틱 포함). 각도 · 높이는 이것으로 정한다. */
		float time;
		int partLight;
	}

	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(MachinePartLayers.TEXTURE);
	/** 32칸 밖에서는 부품을 그리지 않는다. */
	private static final int VIEW_DISTANCE = 32;

	private final ModelPart roller;
	private final ModelPart pressFrame;
	private final ModelPart pressHead;
	private final ModelPart rotor;
	private final ModelPart drill;
	private final ModelPart wheel;

	public MachinePartsRenderer(BlockEntityRendererProvider.Context context) {
		this.roller = context.bakeLayer(MachinePartLayers.CRUSHER_ROLLER);
		this.pressFrame = context.bakeLayer(MachinePartLayers.PRESS_FRAME);
		this.pressHead = context.bakeLayer(MachinePartLayers.PRESS_HEAD);
		this.rotor = context.bakeLayer(MachinePartLayers.TURBINE_ROTOR);
		this.drill = context.bakeLayer(MachinePartLayers.QUARRY_DRILL);
		this.wheel = context.bakeLayer(MachinePartLayers.BELT_WHEEL);
	}

	/** 블록 → 부품 종류. */
	public static Look lookOf(BlockState state) {
		if (state.getBlock() instanceof MachineBlock machine) {
			return switch (machine.kind()) {
				case CRUSHER -> Look.CRUSHER;
				case PRESS -> Look.PRESS;
				default -> Look.NONE;
			};
		}
		if (state.getBlock() instanceof SteamTurbineBlock) {
			return Look.TURBINE;
		}
		if (state.getBlock() instanceof QuarryBlock) {
			return Look.QUARRY;
		}
		if (state.getBlock() instanceof BeltMotorBlock) {
			return Look.BELT_MOTOR;
		}
		return Look.NONE;
	}

	/** 작동 중인가: 불 켜짐(LIT) 또는 가동(POWERED) 블록 상태. */
	public static boolean isRunning(BlockState state) {
		if (state.hasProperty(BlockStateProperties.LIT)) {
			return state.getValue(BlockStateProperties.LIT);
		}
		return state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T blockEntity, State state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		BlockState blockState = blockEntity.getBlockState();
		state.look = lookOf(blockState);
		if (state.look == Look.NONE) {
			return;
		}
		state.running = isRunning(blockState);
		state.facing = blockState.hasProperty(HorizontalDirectionalBlock.FACING) ? blockState.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
		Level level = blockEntity.getLevel();
		state.time = level == null ? 0.0F : (level.getGameTime() % 72000L) + partialTicks;
		Direction out = switch (state.look) {
			case QUARRY -> Direction.DOWN;
			case BELT_MOTOR -> state.facing;
			default -> Direction.UP;
		};
		state.partLight = level == null ? state.lightCoords : LightCoordsUtil.getLightCoords(level, blockEntity.getBlockPos().relative(out));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.look == Look.NONE) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.0F, 0.5F);
		// 앞이 -Z가 되게 돌린다 (서랍 렌더러와 같은 방향)
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());
		switch (state.look) {
			case CRUSHER -> crusher(state, poseStack, collector);
			case PRESS -> press(state, poseStack, collector);
			case TURBINE -> turbine(state, poseStack, collector);
			case QUARRY -> quarry(state, poseStack, collector);
			case BELT_MOTOR -> beltMotor(state, poseStack, collector);
			default -> {
			}
		}
		poseStack.popPose();
	}

	/** 각도 (라디안): 돌 때만 시간에 따라, 멈추면 0. */
	private static float spin(State state, float degreesPerTick) {
		return state.running ? (state.time * degreesPerTick % 360.0F) * Mth.DEG_TO_RAD : 0.0F;
	}

	/** 톱니 롤러 둘이 윗면 틈에 반쯤 묻혀 서로 반대로 돈다 (안쪽으로 물어들이는 방향). */
	private void crusher(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		float angle = spin(state, 12.0F);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(0.0F, 1.0F - 0.2F / 16.0F, side * 2.6F / 16.0F);
			poseStack.rotate(Axis.XP, side * angle + (side > 0 ? Mth.PI / 8.0F : 0.0F));
			part(roller, state, poseStack, collector);
			poseStack.popPose();
		}
	}

	/** 틀은 가만히, 머리는 30틱마다 빠르게 내려찍고 천천히 올라간다. */
	private void press(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 1.0F, 0.0F);
		part(pressFrame, state, poseStack, collector);
		float lift = 4.5F;
		if (state.running) {
			float t = state.time % 30.0F / 30.0F;
			// 0~0.15: 내려찍기, 0.15~0.35: 누르고 있기, 0.35~1: 올라가기
			lift = t < 0.15F ? 4.5F * (1.0F - t / 0.15F) : t < 0.35F ? 0.0F : 4.5F * Mth.sin((t - 0.35F) / 0.65F * Mth.HALF_PI);
		}
		poseStack.translate(0.0F, (0.3F + lift) / 16.0F, 0.0F);
		part(pressHead, state, poseStack, collector);
		poseStack.popPose();
	}

	/** 날개 6장이 윗면 위에서 돈다. */
	private void turbine(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 1.0F, 0.0F);
		poseStack.rotate(Axis.YP, spin(state, 24.0F));
		part(rotor, state, poseStack, collector);
		poseStack.popPose();
	}

	/** 드릴이 밑면 아래로 나와 돈다. */
	private void quarry(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		poseStack.pushPose();
		poseStack.rotate(Axis.YP, -spin(state, 30.0F));
		part(drill, state, poseStack, collector);
		poseStack.popPose();
	}

	/** 앞면에 붙은 바퀴가 돈다. */
	private void beltMotor(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.5F, -0.5F);
		poseStack.rotate(Axis.ZP, spin(state, 15.0F));
		part(wheel, state, poseStack, collector);
		poseStack.popPose();
	}

	private static void part(ModelPart part, State state, PoseStack poseStack, SubmitNodeCollector collector) {
		collector.submitModelPart(part, poseStack, RENDER_TYPE, state.partLight, OverlayTexture.NO_OVERLAY, null);
	}

	@Override
	public int getViewDistance() {
		return VIEW_DISTANCE;
	}
}
