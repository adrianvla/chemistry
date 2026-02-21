package org.keke.chemistry.recipe;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;

/**
 * Registers custom recipe serializers for the chemistry mod.
 */
public class ModRecipeSerializers {

    public static final RecipeSerializer<TntFromCompoundRecipe> TNT_FROM_COMPOUND =
            Registry.register(Registries.RECIPE_SERIALIZER,
                    new Identifier(Chemistry.MOD_ID, "tnt_from_compound"),
                    new TntFromCompoundRecipe.Serializer());

    public static void registerAll() {
        Chemistry.LOGGER.info("Registering recipe serializers for " + Chemistry.MOD_ID);
    }
}
