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
import org.keke.chemistry.screen.PipetteScreenHandler;

import java.util.stream.Stream;

/**
 * Precise extraction block (Pipette). Allows removing exact amounts of
 * specific chemicals from a beaker placed on top.
 *
 * Accessible in creative mode. Craftable with netherite ingots.
 */
public class PipetteBlock extends Block {

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(0, 0, 0, 16, 2, 16),    // base plate
            Block.createCuboidShape(3, 2, 3, 13, 8, 13),    // body
            Block.createCuboidShape(6, 8, 6, 10, 14, 10)    // pipette tube
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public PipetteBlock(Settings settings) {
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
                    return Text.literal("Precision Pipette");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
                    return new PipetteScreenHandler(syncId, inventory,
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
