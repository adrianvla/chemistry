package org.keke.chemistry.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Pliers — a tool used to open sealed ampules.
 *
 * Right-click a sealed ampule in the off-hand (or vice versa) to break the seal.
 * The pliers have durability and lose 1 point per use.
 */
public class PliersItem extends Item {

    public PliersItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack pliers = user.getStackInHand(hand);
        Hand otherHand = (hand == Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack other = user.getStackInHand(otherHand);

        if (other.getItem() instanceof AmpuleItem && AmpuleItem.isSealed(other)) {
            if (!world.isClient) {
                AmpuleItem.setSealed(other, false);
                pliers.damage(1, user, (entity) ->
                        entity.sendToolBreakStatus(hand));
                world.playSound(null, user.getBlockPos(),
                        SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS,
                        0.5f, 1.8f);
                user.sendMessage(Text.literal("Ampule opened").formatted(Formatting.GREEN), true);
            }
            return TypedActionResult.success(pliers, world.isClient);
        }

        return TypedActionResult.pass(pliers);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // Pliers can also open ampule blocks in the world
        // (handled via AmpuleBlock.onUse detecting pliers)
        return ActionResult.PASS;
    }
}
