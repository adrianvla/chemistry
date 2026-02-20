package org.keke.chemistry.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import org.keke.chemistry.block.ModBlocks;

/**
 * Screen handler for the Precise Extraction (Pipette) block.
 * Non-slot screen handler — GUI has text fields to specify which
 * compound and how many moles/grams to withdraw from the beaker above.
 */
public class PipetteScreenHandler extends ScreenHandler {

    private final ScreenHandlerContext context;
    private final BlockPos blockPos;

    /** Client-side constructor — receives block pos from ExtendedScreenHandlerType. */
    public PipetteScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        super(ModScreenHandlers.PIPETTE, syncId);
        this.context = ScreenHandlerContext.EMPTY;
        this.blockPos = pos;
    }

    /** Server-side constructor. */
    public PipetteScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, BlockPos pos) {
        super(ModScreenHandlers.PIPETTE, syncId);
        this.context = context;
        this.blockPos = pos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ModBlocks.PIPETTE_BLOCK);
    }
}
