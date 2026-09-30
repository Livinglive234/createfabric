package com.simibubi.create.api.registry;

import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;

import net.minecraft.world.item.Item;

/**
 * fabric: NeoForge's generic {@code DataMapType} system isn't available on Fabric. These are populated by
 * {@link com.simibubi.create.impl.registry.CreateDataMapsImpl}, which loads the same
 * {@code data/<namespace>/data_maps/item/*.json} files NeoForge's data maps read, plus any values registered
 * directly in code via {@link #REGULAR_BLAZE_BURNER_FUELS} / {@link #SUPERHEATED_BLAZE_BURNER_FUELS}.
 */
public class CreateDataMaps {
	/**
	 * The {@linkplain Item} data map for regular blaze burner fuels.
	 * <p>
	 * The location of this data map is {@code create/data_maps/item/regular_blaze_burner_fuels.json}, and the values are objects with 1 field:
	 * <ul>
	 * <li>{@code burn_time}, a positive integer - how long the item will burn, in ticks</li>
	 * </ul>
	 * <p>
	 * The use of an integer as the value is also possible, though discouraged in case more options are added in the future.
	 */
	public static final Map<Item, BlazeBurnerFuel> REGULAR_BLAZE_BURNER_FUELS = new HashMap<>();

	/**
	 * The {@linkplain Item} data map for superheated blaze burner fuels.
	 * <p>
	 * The location of this data map is {@code create/data_maps/item/superheated_blaze_burner_fuels.json}, and the values are objects with 1 field:
	 * <ul>
	 * <li>{@code burn_time}, a positive integer - how long the item will burn, in ticks</li>
	 * </ul>
	 * <p>
	 * The use of an integer as the value is also possible, though discouraged in case more options are added in the future.
	 */
	public static final Map<Item, BlazeBurnerFuel> SUPERHEATED_BLAZE_BURNER_FUELS = new HashMap<>();

	private CreateDataMaps() {
		throw new AssertionError("This class should not be instantiated");
	}
}
