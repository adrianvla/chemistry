package org.keke.chemistry.block;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.keke.chemistry.entity.BeakerLiquidBlockEntity;
import org.keke.chemistry.screen.CompositionScreenHandler;

import java.util.stream.Stream;

/**
 * Creative-only composition block. Opens a GUI where the player can type any
 * chemical formula and amount (in moles or grams) and dispense it into a
 * beaker placed on top of this block.
 *
 * Not obtainable in survival — creative tab only.
 */
public class CompositionBlock extends Block {

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(0, 0, 0, 16, 2, 16),   // base plate
            Block.createCuboidShape(2, 2, 2, 14, 10, 14),   // machine body
            Block.createCuboidShape(4, 10, 4, 12, 12, 12)   // nozzle/top
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public CompositionBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            if (!player.isCreative()) {
                player.sendMessage(Text.literal("This device requires creative mode."), true);
                return ActionResult.FAIL;
            }

            player.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.literal("Chemical Composer");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
                    return new CompositionScreenHandler(syncId, inventory,
                            ScreenHandlerContext.create(world, pos), pos);
                }

                @Override
                public void writeScreenOpeningData(ServerPlayerEntity player1, PacketByteBuf buf) {
                    buf.writeBlockPos(pos);
                }
            });
        }
        return ActionResult.success(world.isClient);
    }

    /**
     * Find a BeakerLiquidBlockEntity directly above this block.
     */
    public static BeakerLiquidBlockEntity findBeakerAbove(World world, BlockPos pos) {
        BlockPos above = pos.up();
        BlockEntity be = world.getBlockEntity(above);
        if (be instanceof BeakerLiquidBlockEntity beaker) {
            return beaker;
        }
        return null;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
