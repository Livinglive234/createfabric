package com.simibubi.create.compat.rei;

import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;

/* From JEI's Potion item subtype interpreter */
public class PotionFluidSubtypeInterpreter /*implements IIngredientSubtypeInterpreter<FluidStack>*/ {

//	@Override
	public String apply(FluidStack ingredient) {
		if (ingredient.getComponentsPatch().isEmpty())
			return "";

		PotionContents contents = ingredient.getVariant().getComponentMap().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		String potionTypeString = Potion.getName(contents.potion(), "");
		String bottleType = ingredient.getVariant().getComponentMap().getOrDefault(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, BottleType.REGULAR)
			.toString();

		StringBuilder stringBuilder = new StringBuilder(potionTypeString);
		List<MobEffectInstance> effects = contents.customEffects();

		stringBuilder.append(";")
			.append(bottleType);
		contents.potion().ifPresent(p -> {
			for (MobEffectInstance effect : p.value().getEffects())
				stringBuilder.append(";")
					.append(effect);
		});
		for (MobEffectInstance effect : effects)
			stringBuilder.append(";")
				.append(effect);
		return stringBuilder.toString();
	}

}
