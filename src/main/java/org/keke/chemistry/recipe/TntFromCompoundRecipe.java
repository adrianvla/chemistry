package org.keke.chemistry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.keke.chemistry.item.AbstractContainerItem;
import org.keke.chemistry.item.BeakerItem;
import org.keke.chemistry.utils.ExplosiveCompounds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Special crafting recipe: any chemistry container holding explosive compound(s)
 *   + string + paper → minecraft:tnt
 *
 * The explosive power of the container contents determines how many TNT blocks
 * are produced (1-4), calculated by ExplosiveCompounds.
 *
 * Layout (shapeless): container + string + paper anywhere in 3x3 grid.
 */
public class TntFromCompoundRecipe extends SpecialCraftingRecipe {

    public TntFromCompoundRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        boolean hasExplosiveContainer = false;
        boolean hasString = false;
        boolean hasPaper = false;
        int nonEmptySlots = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            nonEmptySlots++;

            if (isExplosiveContainer(stack)) {
                hasExplosiveContainer = true;
            } else if (stack.isOf(Items.STRING)) {
                hasString = true;
            } else if (stack.isOf(Items.PAPER)) {
                hasPaper = true;
            } else {
                return false; // unexpected item
            }
        }

        return hasExplosiveContainer && hasString && hasPaper && nonEmptySlots == 3;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && isExplosiveContainer(stack)) {
                Map<String, Double> contents = getContentsFromAny(stack);
                double power = ExplosiveCompounds.calculateTotalExplosionPower(contents);
                // 1 TNT per ~4.0 MC explosion power units, min 1, max 4
                int count = Math.max(1, Math.min(4, (int) (power / 4.0)));
                return new ItemStack(Items.TNT, count);
            }
        }
        return new ItemStack(Items.TNT, 1);
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.TNT_FROM_COMPOUND;
    }

    private boolean isExplosiveContainer(ItemStack stack) {
        Map<String, Double> contents = getContentsFromAny(stack);
        if (contents.isEmpty()) return false;
        double power = ExplosiveCompounds.calculateTotalExplosionPower(contents);
        return power > 0.5;
    }

    private Map<String, Double> getContentsFromAny(ItemStack stack) {
        if (stack.getItem() instanceof BeakerItem) {
            return BeakerItem.getContents(stack);
        }
        if (stack.getItem() instanceof AbstractContainerItem) {
            return AbstractContainerItem.getContents(stack);
        }
        // Fallback: raw NBT read
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("contents", NbtElement.COMPOUND_TYPE)) {
            Map<String, Double> map = new LinkedHashMap<>();
            NbtCompound contentsNbt = nbt.getCompound("contents");
            for (String key : contentsNbt.getKeys()) {
                double val = contentsNbt.getDouble(key);
                if (val > 0.001) map.put(key, val);
            }
            return map;
        }
        return Map.of();
    }

    public static class Serializer implements RecipeSerializer<TntFromCompoundRecipe> {
        @Override
        public TntFromCompoundRecipe read(Identifier id, JsonObject json) {
            CraftingRecipeCategory category = CraftingRecipeCategory.CODEC
                    .byId(json.has("category") ? json.get("category").getAsString() : "misc",
                            CraftingRecipeCategory.MISC);
            return new TntFromCompoundRecipe(id, category);
        }

        @Override
        public TntFromCompoundRecipe read(Identifier id, PacketByteBuf buf) {
            CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
            return new TntFromCompoundRecipe(id, category);
        }

        @Override
        public void write(PacketByteBuf buf, TntFromCompoundRecipe recipe) {
            buf.writeEnumConstant(recipe.getCategory());
        }
    }
}
