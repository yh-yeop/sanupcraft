package io.github.yhyeop.sanupcraft.nether;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * 네더 바닥 구조물 (구조물 종류 {@code sanupcraft:nether_floor}, 버려진 용광로가 쓴다).
 * 네더는 천장이 있어 높이 지도로 지면을 못 찾으므로, 네더 화석처럼 기본 지형 기둥을 위에서 아래로 훑어 "위는 비고 아래는 단단한" 바닥을 찾는다.
 * 틀 한 장(템플릿 풀의 시작 조각)을 그 바닥에 놓는다: 틀의 y=0이 바닥 블록 자리, y=1부터 빈 공간.
 * 틀이 놓일 네 귀퉁이 쪽(±{@link #FOOTPRINT}) 중 셋 이상이 비슷한 높이(±3)의 바닥이어야 놓는다 (낭떠러지 · 용암 호수 위는 건너뜀).
 * 남는 틈은 구조물 설정의 terrain_adaptation(beard_thin)이 메운다.
 */
public class NetherFloorStructure extends Structure {
	public static final MapCodec<NetherFloorStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
			HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight)
	).apply(i, NetherFloorStructure::new));

	/** 틀 가운데까지 거리 (11칸 틀의 절반쯤). */
	static final int FOOTPRINT = 5;
	/** 바닥 위로 비어 있어야 하는 칸 수. */
	static final int HEADROOM = 5;
	private static final int MAX_STEP = 3;

	private final Holder<StructureTemplatePool> startPool;
	private final HeightProvider startHeight;

	public NetherFloorStructure(Structure.StructureSettings settings, Holder<StructureTemplatePool> startPool, HeightProvider startHeight) {
		super(settings);
		this.startPool = startPool;
		this.startHeight = startHeight;
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		int x = context.chunkPos().getMinBlockX() + context.random().nextInt(16);
		int z = context.chunkPos().getMinBlockZ() + context.random().nextInt(16);
		int top = startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
		int minFloor = context.chunkGenerator().getSeaLevel() + 1;
		int floor = floorBelow(context, x, z, top, minFloor);
		if (floor == Integer.MIN_VALUE) {
			return Optional.empty();
		}
		int similar = 0;
		for (int dx = -1; dx <= 1; dx += 2) {
			for (int dz = -1; dz <= 1; dz += 2) {
				int corner = floorBelow(context, x + dx * FOOTPRINT, z + dz * FOOTPRINT, floor + MAX_STEP + HEADROOM, minFloor);
				if (corner != Integer.MIN_VALUE && Math.abs(corner - floor) <= MAX_STEP) {
					similar++;
				}
			}
		}
		if (similar < 3) {
			return Optional.empty();
		}
		// JigsawPlacement는 틀의 y=1을 이 높이에 맞춘다 → y=0이 바닥 블록 자리.
		// 깊이는 1이어야 한다: 0이면 JigsawPlacement가 시작 조각조차 넣지 않아 구조물이 비어 버린다 (틀에 직소 블록이 없어 더 붙지는 않음)
		BlockPos start = new BlockPos(x, floor + 1, z);
		return JigsawPlacement.addPieces(context, startPool, Optional.empty(), 1, start, false, Optional.empty(),
				new JigsawStructure.MaxDistance(16), PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
	}

	/** {@code top}에서 아래로 훑어 처음 나오는 바닥 블록의 y (위 {@link #HEADROOM}칸이 빔). 없으면 Integer.MIN_VALUE. */
	private static int floorBelow(Structure.GenerationContext context, int x, int z, int top, int minFloor) {
		NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		int air = 0;
		for (int y = top; y > minFloor; y--) {
			BlockState state = column.getBlock(y);
			if (state.isAir()) {
				air++;
				continue;
			}
			if (air >= HEADROOM && state.getFluidState().isEmpty() && state.isFaceSturdy(EmptyBlockGetter.INSTANCE, pos.setY(y), Direction.UP)) {
				return y;
			}
			air = 0;
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public StructureType<?> type() {
		return ModNether.NETHER_FLOOR;
	}
}
