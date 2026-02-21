package org.keke.chemistry.mixin;

import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.keke.chemistry.block.ModBlocks;
import org.keke.chemistry.entity.ChemistryCauldronBlockEntity;
import org.keke.chemistry.item.BeakerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Mixin on AbstractCauldronBlock to intercept right-click with a beaker item.
 * When a player right-clicks a vanilla cauldron with a filled beaker,
 * the cauldron is replaced with a Chemistry Cauldron block and the beaker
 * contents are transferred.
 */
@Mixin(AbstractCauldronBlock.class)
public class CauldronBeakerMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void chemistry$onBeakerUse(BlockState state, World world, BlockPos pos,
                                       PlayerEntity player, Hand hand, BlockHitResult hit,
                                       CallbackInfoReturnable<ActionResult> cir) {
        ItemStack heldItem = player.getStackInHand(hand);

        // Only intercept if player is holding a filled beaker
        if (!(heldItem.getItem() instanceof BeakerItem) || !BeakerItem.hasContents(heldItem)) {
            return;
        }

        if (!world.isClient) {
            // Replace vanilla cauldron with chemistry cauldron
            world.setBlockState(pos, ModBlocks.CHEMISTRY_CAULDRON.getDefaultState());

            // Get the new block entity and pour contents in
            var be = world.getBlockEntity(pos);
            if (be instanceof ChemistryCauldronBlockEntity cauldron) {
                Map<String, Double> beakerContents = BeakerItem.getContents(heldItem);
                double cauldronMolesBefore = cauldron.getTotalMoles();
                double cauldronTempK = cauldron.getTemperatureK();
                double sourceTempK = BeakerItem.getTemperatureK(heldItem);

                double totalPouredMoles = 0;
                Map<String, Double> remaining = new java.util.LinkedHashMap<>(beakerContents);
                for (var entry : beakerContents.entrySet()) {
                    double actualAdded = cauldron.addChemical(entry.getKey(), entry.getValue());
                    if (actualAdded > 0) {
                        totalPouredMoles += actualAdded;
                        double leftInSource = entry.getValue() - actualAdded;
                        if (leftInSource <= 0.001) {
                            remaining.remove(entry.getKey());
                        } else {
                            remaining.put(entry.getKey(), leftInSource);
                        }
                    }
                }

                // Energy conservation: mix temperatures
                if (totalPouredMoles > 0 && (cauldronMolesBefore + totalPouredMoles) > 0.001) {
                    double mixedT = (cauldronMolesBefore * cauldronTempK + totalPouredMoles * sourceTempK)
                            / (cauldronMolesBefore + totalPouredMoles);
                    cauldron.setTemperatureK(mixedT);
                }

                if (remaining.isEmpty()) {
                    BeakerItem.clearContents(heldItem);
                } else {
                    BeakerItem.setContents(heldItem, remaining);
                }
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY,
                        SoundCategory.BLOCKS, 1.0f, 0.9f);
            }
        }

        cir.setReturnValue(ActionResult.success(world.isClient));
    }
}
