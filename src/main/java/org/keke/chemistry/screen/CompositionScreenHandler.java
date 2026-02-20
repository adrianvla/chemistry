package org.keke.chemistry.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import org.keke.chemistry.block.ModBlocks;

/**
 * Screen handler for the Chemical Composer block.
 * This is a non-slot screen handler — the GUI uses text fields
 * and buttons, with compound addition handled via custom packets.
 */
public class CompositionScreenHandler extends ScreenHandler {

    private final ScreenHandlerContext context;
    private final BlockPos blockPos;

    /** Client-side constructor — receives block pos from ExtendedScreenHandlerType. */
    public CompositionScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        super(ModScreenHandlers.COMPOSITION, syncId);
        this.context = ScreenHandlerContext.EMPTY;
        this.blockPos = pos;
    }

    /** Server-side constructor. */
    public CompositionScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, BlockPos pos) {
        super(ModScreenHandlers.COMPOSITION, syncId);
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
        return canUse(context, player, ModBlocks.COMPOSITION_BLOCK);
    }
}
