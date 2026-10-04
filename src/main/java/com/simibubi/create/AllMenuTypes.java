package com.simibubi.create;

import com.simibubi.create.content.equipment.blueprint.BlueprintMenu;
import com.simibubi.create.content.equipment.blueprint.BlueprintScreen;
import com.simibubi.create.content.equipment.toolbox.ToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxScreen;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemMenu;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemScreen;
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.content.logistics.filter.FilterMenu;
import com.simibubi.create.content.logistics.filter.FilterScreen;
import com.simibubi.create.content.logistics.filter.PackageFilterMenu;
import com.simibubi.create.content.logistics.filter.PackageFilterScreen;
import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerMenu;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerScreen;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.content.schematics.table.SchematicTableMenu;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.content.trains.schedule.ScheduleMenu;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.tterrag.registrate.builders.MenuBuilder;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.fabricmc.api.EnvType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

// fabric: all of these are registered as plain vanilla MenuTypes via Registrate's generic() entry
// point, NOT its menu() builder. menu() was confirmed (by decompiling MenuBuilder - both its
// MenuFactory and ForgeMenuFactory overloads, even the one that looks like it should build a plain
// MenuType) to ALWAYS construct a Fabric ExtendedScreenHandlerType with a null packet codec
// internally, no matter which overload is used - there is no way to get a plain MenuType out of it.
// Confirmed broken by live testing either way: that codec doesn't round-trip, disconnecting the
// client. Any extra data these menus need is sent separately via MenuOpeningHelper/PendingMenuData.
// See PendingMenuData for the full explanation.
public class AllMenuTypes {

	public static final RegistryEntry<MenuType<?>, MenuType<SchematicTableMenu>> SCHEMATIC_TABLE =
		register("schematic_table", SchematicTableMenu::new, () -> SchematicTableScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<SchematicannonMenu>> SCHEMATICANNON =
		register("schematicannon", SchematicannonMenu::new, () -> SchematicannonScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<FilterMenu>> FILTER =
		register("filter", FilterMenu::new, () -> FilterScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<AttributeFilterMenu>> ATTRIBUTE_FILTER =
		register("attribute_filter", AttributeFilterMenu::new, () -> AttributeFilterScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<PackageFilterMenu>> PACKAGE_FILTER =
		register("package_filter", PackageFilterMenu::new, () -> PackageFilterScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<BlueprintMenu>> CRAFTING_BLUEPRINT =
		register("crafting_blueprint", BlueprintMenu::new, () -> BlueprintScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<LinkedControllerMenu>> LINKED_CONTROLLER =
		register("linked_controller", LinkedControllerMenu::new, () -> LinkedControllerScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<ToolboxMenu>> TOOLBOX =
		register("toolbox", ToolboxMenu::new, () -> ToolboxScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<ScheduleMenu>> SCHEDULE =
		register("schedule", ScheduleMenu::new, () -> ScheduleScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<StockKeeperCategoryMenu>> STOCK_KEEPER_CATEGORY =
		register("stock_keeper_category", StockKeeperCategoryMenu::new, () -> StockKeeperCategoryScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<StockKeeperRequestMenu>> STOCK_KEEPER_REQUEST =
		register("stock_keeper_request", StockKeeperRequestMenu::new, () -> StockKeeperRequestScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<PackagePortMenu>> PACKAGE_PORT =
		register("package_port", PackagePortMenu::new, () -> PackagePortScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<RedstoneRequesterMenu>> REDSTONE_REQUESTER =
		register("redstone_requester", RedstoneRequesterMenu::new, () -> RedstoneRequesterScreen::new);

	public static final RegistryEntry<MenuType<?>, MenuType<FactoryPanelSetItemMenu>> FACTORY_PANEL_SET_ITEM =
		register("factory_panel_set_item", FactoryPanelSetItemMenu::new, () -> FactoryPanelSetItemScreen::new);

	@SuppressWarnings("unchecked")
	private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> RegistryEntry<MenuType<?>, MenuType<C>> register(
			String name, MenuBuilder.MenuFactory<C> factory, NonNullSupplier<MenuScreens.ScreenConstructor<C, S>> screenFactory) {
		RegistryEntry<MenuType<?>, MenuType<C>> entry = Create.registrate()
			.generic(name, Registries.MENU, () -> {
				// fabric: self-referencing array trick - the MenuType needs to be passed into the
				// MenuSupplier lambda it's itself the argument of, which isn't possible without
				// something effectively-final to close over
				MenuType<C>[] self = new MenuType[1];
				self[0] = new MenuType<>((id, inv) -> factory.create(self[0], id, inv), FeatureFlags.DEFAULT_FLAGS);
				return self[0];
			})
			.register();
		EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> MenuScreens.register(entry.get(), screenFactory.get()));
		return entry;
	}

	public static void register() {
	}

}
