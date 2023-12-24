package org.keke.chemistry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.ModItems;

public class Chemistry implements ModInitializer {
    /**
     * Runs the mod initializer.
     */
    public static final String MOD_ID = "chemistry";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static void sendMessage(String message){
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.player.sendMessage(Text.of(message),false);
    }
    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModBlockEntities.registerAllBlockEntities();
    }
}
