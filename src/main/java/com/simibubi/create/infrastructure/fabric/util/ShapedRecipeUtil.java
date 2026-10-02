package com.simibubi.create.infrastructure.fabric.util;

import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * fabric: vanilla's {@code ShapedRecipePattern.MAX_SIZE} caps shaped recipes at a 3x3 grid; NeoForge patches this
 * out entirely, so on fabric we widen the field via the access widener and raise it ourselves to fit the
 * mechanical crafter's largest supported grid.
 */
public class ShapedRecipeUtil {
	public static void setCraftingSize(int width, int height) {
		ShapedRecipePattern.MAX_SIZE = Math.max(ShapedRecipePattern.MAX_SIZE, Math.max(width, height));
	}
}
