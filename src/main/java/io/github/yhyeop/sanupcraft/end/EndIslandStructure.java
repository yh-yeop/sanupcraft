package io.github.yhyeop.sanupcraft.end;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * 엔드 섬 위 구조물 (구조물 종류 {@code sanupcraft:end_island}, 버려진 정거장이 쓴다).
 * 엔드는 섬 사이가 공허라 그냥 직소로 놓으면 공허 한가운데 떠 버린다. 엔드 도시처럼 놓을 자리와 네 귀퉁이(±{@link #FOOTPRINT})의
 * 지면 높이(WORLD_SURFACE_WG)를 보고, 가운데와 귀퉁이 셋 이상이 {@link #MIN_SURFACE} 위 땅이고 가운데와 ±{@link #MAX_STEP} 안일 때만 놓는다.
 * 틀의 y=0을 가운데 지면 + {@code hover} 높이에 맞춘다 (틀의 다리가 땅에 닿게 틀을 만든다).
 */
public class EndIslandStructure extends Structure {
	public static final MapCodec<EndIslandStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
			Codec.intRange(-16, 32).optionalFieldOf("hover", 0).forGetter(s -> s.hover)
	).apply(i, EndIslandStructure::new));

	static final int FOOTPRINT = 6;
	static final int MIN_SURFACE = 30;
	private static final int MAX_STEP = 6;

	private final Holder<StructureTemplatePool> startPool;
	private final int hover;

	public EndIslandStructure(Structure.StructureSettings settings, Holder<StructureTemplatePool> startPool, int hover) {
		super(settings);
		this.startPool = startPool;
		this.hover = hover;
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		int x = context.chunkPos().getMiddleBlockX();
		int z = context.chunkPos().getMiddleBlockZ();
		int center = surface(context, x, z);
		if (center < MIN_SURFACE) {
			return Optional.empty();
		}
		int similar = 0;
		for (int dx = -1; dx <= 1; dx += 2) {
			for (int dz = -1; dz <= 1; dz += 2) {
				int corner = surface(context, x + dx * FOOTPRINT, z + dz * FOOTPRINT);
				if (corner >= MIN_SURFACE && Math.abs(corner - center) <= MAX_STEP) {
					similar++;
				}
			}
		}
		if (similar < 3) {
			return Optional.empty();
		}
		// 깊이 1: 0이면 JigsawPlacement가 시작 조각도 넣지 않는다 (버려진 용광로와 같음)
		BlockPos start = new BlockPos(x, center + hover, z);
		return JigsawPlacement.addPieces(context, startPool, Optional.empty(), 1, start, false, Optional.empty(),
				new JigsawStructure.MaxDistance(16), PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
	}

	private static int surface(Structure.GenerationContext context, int x, int z) {
		return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
	}

	@Override
	public StructureType<?> type() {
		return ModEnd.END_ISLAND;
	}
}
