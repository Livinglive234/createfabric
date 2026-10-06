package com.simibubi.create.foundation.render;

import net.createmod.catnip.render.ShadedBlockSbbBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Accepts Fabric renderer vertex emission as well as vanilla bulk quads.
 * Direct emission does not carry Catnip's per-quad shade flag, so it uses
 * the default shaded section. Bulk quads retain the inherited shade handling.
 */
@SuppressWarnings("removal")
public class FabricShadedBlockSbbBuilder extends ShadedBlockSbbBuilder {
	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		unwrap(true).addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		bufferBuilder.setColor(red, green, blue, alpha);
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		bufferBuilder.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		bufferBuilder.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		bufferBuilder.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		bufferBuilder.setNormal(x, y, z);
		return this;
	}
}
