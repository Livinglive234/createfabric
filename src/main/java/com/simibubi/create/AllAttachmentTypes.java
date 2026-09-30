package com.simibubi.create;

import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus.Internal;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
/**
 * fabric: the {@code MinecartController} attachment is handled directly via
 * {@code AbstractMinecartMixin} / {@code AbstractMinecartExtensions#create$getController()} instead
 * of NeoForge's generic data attachment API.
 */
public class AllAttachmentTypes {
	public static void register() {
	}
}
