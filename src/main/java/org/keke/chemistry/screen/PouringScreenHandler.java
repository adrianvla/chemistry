package org.keke.chemistry.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import org.keke.chemistry.block.ModBlocks;

/**
 * Screen handler for the beaker pouring GUI.
 * Non-slot handler — the GUI uses text fields and buttons,
 * with pouring handled via custom network packets.
 *
 * The source beaker is the item in the player's hand;
 * the target beaker is the placed block at {@code blockPos}.
 */
public class PouringScreenHandler extends ScreenHandler {

    private final ScreenHandlerContext context;
    private final BlockPos blockPos;

    /** Client-side constructor — receives block pos from ExtendedScreenHandlerType. */
    public PouringScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        super(ModScreenHandlers.POURING, syncId);
        this.context = ScreenHandlerContext.EMPTY;
        this.blockPos = pos;
    }

    /** Server-side constructor. */
    public PouringScreenHandler(int syncId, PlayerInventory playerInventory,
                                ScreenHandlerContext context, BlockPos pos) {
        super(ModScreenHandlers.POURING, syncId);
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
        return canUse(context, player, ModBlocks.BEAKER_BASE);
    }
}
