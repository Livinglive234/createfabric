package com.simibubi.create;

import com.simibubi.create.content.equipment.blueprint.BlueprintScreen;
import com.simibubi.create.content.equipment.toolbox.ToolboxScreen;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemScreen;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.content.logistics.filter.FilterScreen;
import com.simibubi.create.content.logistics.filter.PackageFilterScreen;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerScreen;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import net.minecraft.client.gui.screens.Screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.screens.MenuScreens;

/** Client-only bindings for the shared menu types. */
@Environment(EnvType.CLIENT)
public class AllMenuScreens {
	public static void register() {
		MenuScreens.register(AllMenuTypes.SCHEMATIC_TABLE.get(), SchematicTableScreen::new);
		MenuScreens.register(AllMenuTypes.SCHEMATICANNON.get(), SchematicannonScreen::new);
		MenuScreens.register(AllMenuTypes.FILTER.get(), FilterScreen::new);
		MenuScreens.register(AllMenuTypes.ATTRIBUTE_FILTER.get(), AttributeFilterScreen::new);
		MenuScreens.register(AllMenuTypes.PACKAGE_FILTER.get(), PackageFilterScreen::new);
		MenuScreens.register(AllMenuTypes.CRAFTING_BLUEPRINT.get(), BlueprintScreen::new);
		MenuScreens.register(AllMenuTypes.LINKED_CONTROLLER.get(), LinkedControllerScreen::new);
		MenuScreens.register(AllMenuTypes.TOOLBOX.get(), ToolboxScreen::new);
		MenuScreens.register(AllMenuTypes.SCHEDULE.get(), ScheduleScreen::new);
		MenuScreens.register(AllMenuTypes.STOCK_KEEPER_CATEGORY.get(), StockKeeperCategoryScreen::new);
		MenuScreens.register(AllMenuTypes.STOCK_KEEPER_REQUEST.get(), StockKeeperRequestScreen::new);
		MenuScreens.register(AllMenuTypes.PACKAGE_PORT.get(), PackagePortScreen::new);
		MenuScreens.register(AllMenuTypes.REDSTONE_REQUESTER.get(), RedstoneRequesterScreen::new);
		MenuScreens.register(AllMenuTypes.FACTORY_PANEL_SET_ITEM.get(), FactoryPanelSetItemScreen::new);
	}
}
