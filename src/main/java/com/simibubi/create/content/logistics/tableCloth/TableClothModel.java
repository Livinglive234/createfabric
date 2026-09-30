package com.simibubi.create.content.logistics.tableCloth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

// TODO fabric: this is unregistered/dead in the current fabric branch (see the commented-out
// `.onRegister(CreateRegistrate.blockModel(() -> TableClothModel::new))` in BuilderTransformers.java
// and `TableClothModel.reload()` call in ClientResourceReloadListener.java). NeoForge's ModelData
// system (used here to cache which sides are culled, so corner quads are skipped against a
// neighbouring tablecloth) has no fabric equivalent - fabric's BakedModel#getQuads doesn't carry a
// BlockPos/Level, only (BlockState, Direction, RandomSource), so there's no way to inspect neighbours
// at this point. Simplified to always render the corner quads (drops the seam-culling optimization).
public class TableClothModel implements BakedModel {

	private final BakedModel originalModel;

	private static final Map<TableClothBlock, List<List<BakedQuad>>> CORNERS = new HashMap<>();

	public TableClothModel(BakedModel originalModel) {
		this.originalModel = originalModel;
	}

	public static void reload() {
		CORNERS.clear();
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false;
	}

	@Override
	public boolean isGui3d() {
		return originalModel.isGui3d();
	}

	@Override
	public boolean usesBlockLight() {
		return originalModel.usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer() {
		return originalModel.isCustomRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return originalModel.getParticleIcon();
	}

	@Override
	public ItemTransforms getTransforms() {
		return originalModel.getTransforms();
	}

	@Override
	public ItemOverrides getOverrides() {
		return originalModel.getOverrides();
	}

	private List<BakedQuad> getCorner(TableClothBlock block, int corner, @NotNull RandomSource rand,
		@Nullable RenderType renderType) {
		if (!CORNERS.containsKey(block)) {
			TextureAtlasSprite targetSprite = getParticleIcon();
			List<List<BakedQuad>> list = new ArrayList<>();

			for (PartialModel pm : List.of(AllPartialModels.TABLE_CLOTH_SW, AllPartialModels.TABLE_CLOTH_NW,
				AllPartialModels.TABLE_CLOTH_NE, AllPartialModels.TABLE_CLOTH_SE))
				list.add(getCornerQuads(rand, renderType, targetSprite, pm));

			CORNERS.put(block, list);
		}

		return CORNERS.get(block)
			.get(corner);
	}

	private List<BakedQuad> getCornerQuads(RandomSource rand, RenderType renderType, TextureAtlasSprite targetSprite,
		PartialModel pm) {
		List<BakedQuad> quads = new ArrayList<>();

		for (BakedQuad quad : pm.get()
			.getQuads(null, null, rand)) {
			TextureAtlasSprite original = quad.getSprite();
			BakedQuad newQuad = BakedQuadHelper.clone(quad);
			int[] vertexData = newQuad.getVertices();
			for (int vertex = 0; vertex < 4; vertex++) {
				BakedQuadHelper.setU(vertexData, vertex, targetSprite
					.getU(SpriteShiftEntry.getUnInterpolatedU(original, BakedQuadHelper.getU(vertexData, vertex))));
				BakedQuadHelper.setV(vertexData, vertex, targetSprite
					.getV(SpriteShiftEntry.getUnInterpolatedV(original, BakedQuadHelper.getV(vertexData, vertex))));
			}
			quads.add(newQuad);
		}

		return quads;
	}

	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
											 @NotNull RandomSource rand) {
		List<BakedQuad> mainQuads = originalModel.getQuads(state, side, rand);
		if (side == null || side.getAxis() == Axis.Y)
			return mainQuads;
		if (state == null || !(state.getBlock() instanceof TableClothBlock dcb))
			return mainQuads;

		List<BakedQuad> copyOf = new ArrayList<>(mainQuads);
		copyOf.addAll(getCorner(dcb, side.get2DDataValue(), rand, null));
		return copyOf;
	}

}
