package org.keke.chemistry.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.keke.chemistry.block.ModBlocks;
import org.keke.chemistry.block.TestTubeBlock;
import org.keke.chemistry.entity.TestTubeBlockEntity;
import org.keke.chemistry.utils.ContainerType;

import java.util.Map;

/**
 * Test Tube — a small (25 mL) throwable container for small-scale reactions.
 *
 * Interactions:
 *   - Right-click surface: place as block
 *   - Sneak + right-click: drink
 *   - Throwable (handled via dispenser-style throw — see ThrownContainerEntity)
 */
public class TestTubeItem extends AbstractContainerItem {

    public TestTubeItem(Settings settings) {
        super(settings, ContainerType.TEST_TUBE);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        // Sneaking? Pass through to drink handler
        if (player != null && player.isSneaking() && hasContents(stack)) {
            return ActionResult.PASS;
        }

        // Place test tube block
        BlockPos placePos = pos.offset(context.getSide());
        if (world.getBlockState(placePos).isReplaceable()) {
            if (!world.isClient) {
                BlockState state = ModBlocks.TEST_TUBE.getDefaultState()
                        .with(TestTubeBlock.FACING, player != null
                                ? player.getHorizontalFacing().getOpposite()
                                : context.getHorizontalPlayerFacing().getOpposite());
                world.setBlockState(placePos, state);
                world.playSound(null, placePos, SoundEvents.BLOCK_GLASS_PLACE,
                        SoundCategory.BLOCKS, 1.0f, 1.2f);

                BlockEntity be = world.getBlockEntity(placePos);
                if (be instanceof TestTubeBlockEntity entity) {
                    entity.setMaxCapacityMl(containerType.getDefaultCapacityMl());
                    Map<String, Double> contents = getContents(stack);
                    if (!contents.isEmpty()) {
                        entity.setContents(contents);
                    }
                    if (stack.getNbt() != null && stack.getNbt().contains("temperatureK")) {
                        entity.setTemperatureK(stack.getNbt().getDouble("temperatureK"));
                    }
                }

                if (player != null && !player.isCreative()) {
                    stack.decrement(1);
                }
            }
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }
}
