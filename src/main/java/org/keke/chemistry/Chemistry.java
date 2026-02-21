package org.keke.chemistry;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.entity.ModEntityTypes;
import org.keke.chemistry.item.ModItems;
import org.keke.chemistry.network.ModNetworking;
import org.keke.chemistry.recipe.ModRecipeSerializers;
import org.keke.chemistry.screen.ModScreenHandlers;

public class Chemistry implements ModInitializer {
    public static final String MOD_ID = "chemistry";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModBlockEntities.registerAllBlockEntities();
        ModEntityTypes.registerAll();
        ModRecipeSerializers.registerAll();
        ModScreenHandlers.registerAll();
        ModNetworking.registerServerReceivers();
    }
}
