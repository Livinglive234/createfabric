package com.simibubi.create.compat.trinkets;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class Trinkets {
	public static void init() {
		BacktankUtil.addBacktankSupplier(entity -> {
			List<ItemStack> stacks = new ArrayList<>();
			TrinketsApi.getTrinketComponent(entity).ifPresent(component ->
				component.getEquipped(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES::matches)
					.forEach(pair -> stacks.add(pair.getB())));
			return stacks;
		});

	}

	@Environment(EnvType.CLIENT)
	public static void clientInit() {
		GogglesItem.addIsWearingPredicate(player -> {
			Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
			if (optional.isPresent()) {
				TrinketComponent component = optional.get();
				if (component.isEquipped(AllItems.GOGGLES.get())) {
					return true;
				}
			}
			return false;
		});

		TrinketRendererRegistry.registerRenderer(AllItems.GOGGLES.get(), new GoggleTrinketRenderer());
	}
}
