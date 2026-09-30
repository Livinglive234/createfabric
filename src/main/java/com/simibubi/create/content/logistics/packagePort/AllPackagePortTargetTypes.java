package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.TrainStationFrogportTarget;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import org.jetbrains.annotations.ApiStatus.Internal;
import com.simibubi.create.api.registry.CreateRegistries;

public class AllPackagePortTargetTypes {
	public static final Holder<PackagePortTargetType> CHAIN_CONVEYOR = Holder.direct(
		Registry.register(CreateBuiltInRegistries.PACKAGE_PORT_TARGET_TYPE, Create.asResource("chain_conveyor"),
			new ChainConveyorFrogportTarget.Type()));
	public static final Holder<PackagePortTargetType> TRAIN_STATION = Holder.direct(
		Registry.register(CreateBuiltInRegistries.PACKAGE_PORT_TARGET_TYPE, Create.asResource("train_station"),
			new TrainStationFrogportTarget.Type()));

	public static void register() {
	}
}
