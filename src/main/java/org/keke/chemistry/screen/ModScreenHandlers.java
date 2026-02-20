package org.keke.chemistry.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;

/**
 * Registry for all screen handler types in the Chemistry mod.
 * Uses ExtendedScreenHandlerType to forward the block position to the client.
 */
public class ModScreenHandlers {

    public static final ScreenHandlerType<CompositionScreenHandler> COMPOSITION =
            Registry.register(Registries.SCREEN_HANDLER,
                    new Identifier(Chemistry.MOD_ID, "composition"),
                    new ExtendedScreenHandlerType<>((syncId, inv, buf) -> {
                        var pos = buf.readBlockPos();
                        return new CompositionScreenHandler(syncId, inv, pos);
                    }));

    public static final ScreenHandlerType<PipetteScreenHandler> PIPETTE =
            Registry.register(Registries.SCREEN_HANDLER,
                    new Identifier(Chemistry.MOD_ID, "pipette"),
                    new ExtendedScreenHandlerType<>((syncId, inv, buf) -> {
                        var pos = buf.readBlockPos();
                        return new PipetteScreenHandler(syncId, inv, pos);
                    }));

    public static final ScreenHandlerType<PouringScreenHandler> POURING =
            Registry.register(Registries.SCREEN_HANDLER,
                    new Identifier(Chemistry.MOD_ID, "pouring"),
                    new ExtendedScreenHandlerType<>((syncId, inv, buf) -> {
                        var pos = buf.readBlockPos();
                        return new PouringScreenHandler(syncId, inv, pos);
                    }));

    public static void registerAll() {
        Chemistry.LOGGER.info("Registering screen handlers for " + Chemistry.MOD_ID);
    }
}
