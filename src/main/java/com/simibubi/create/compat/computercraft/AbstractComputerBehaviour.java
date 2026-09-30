package com.simibubi.create.compat.computercraft;

import org.jetbrains.annotations.NotNull;


import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class AbstractComputerBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<AbstractComputerBehaviour> TYPE = new BehaviourType<>();

	boolean hasAttachedComputer;

	public AbstractComputerBehaviour(SmartBlockEntity te) {
		super(te);
		this.hasAttachedComputer = false;
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		hasAttachedComputer = nbt.getBoolean("HasAttachedComputer");
		super.read(nbt, registries, clientPacket);
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		nbt.putBoolean("HasAttachedComputer", hasAttachedComputer);
		super.write(nbt, registries, clientPacket);
	}

	@Nullable
	public <T> T getPeripheral() {
		return null;
	}

	public void setHasAttachedComputer(boolean hasAttachedComputer) {
		this.hasAttachedComputer = hasAttachedComputer;
	}

	public boolean hasAttachedComputer() {
		return hasAttachedComputer;
	}

	public void prepareComputerEvent(@NotNull ComputerEvent event) {}

	public void removePeripheral() {}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

}
