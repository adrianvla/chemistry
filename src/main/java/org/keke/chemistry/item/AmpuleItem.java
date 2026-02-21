package org.keke.chemistry.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.block.AmpuleBlock;
import org.keke.chemistry.block.ModBlocks;
import org.keke.chemistry.entity.AmpuleBlockEntity;
import org.keke.chemistry.utils.ContainerType;

import java.util.List;
import java.util.Map;

/**
 * Ampule — a small (10 mL) sealed glass container.
 *
 * Sealed ampules:
 *   - Cannot be drunk from
 *   - Can be thrown (explodes/releases contents on impact)
 *   - Must be opened with Pliers (PliersItem) to access contents
 *
 * Once opened (sealed=false), behaves like other containers.
 */
public class AmpuleItem extends AbstractContainerItem {

    public AmpuleItem(Settings settings) {
        super(settings, ContainerType.AMPULE);
    }

    public static boolean isSealed(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return true; // ampules are sealed by default
        return nbt.getBoolean("sealed") || !nbt.contains("sealed");
    }

    public static void setSealed(ItemStack stack, boolean sealed) {
        stack.getOrCreateNbt().putBoolean("sealed", sealed);
    }

    @Override
    protected boolean canDrinkFrom(ItemStack stack) {
        return !isSealed(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        if (isSealed(stack)) {
            return Text.literal("Sealed ").append(baseName);
        }
        return baseName;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (isSealed(stack)) {
            tooltip.add(Text.literal("Sealed — use Pliers to open").formatted(Formatting.RED));
        } else {
            tooltip.add(Text.literal("Opened").formatted(Formatting.GREEN));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (player != null && player.isSneaking() && hasContents(stack) && !isSealed(stack)) {
            return ActionResult.PASS;
        }

        // Place ampule block
        BlockPos placePos = pos.offset(context.getSide());
        if (world.getBlockState(placePos).isReplaceable()) {
            if (!world.isClient) {
                BlockState state = ModBlocks.AMPULE.getDefaultState()
                        .with(AmpuleBlock.FACING, player != null
                                ? player.getHorizontalFacing().getOpposite()
                                : context.getHorizontalPlayerFacing().getOpposite());
                world.setBlockState(placePos, state);
                world.playSound(null, placePos, SoundEvents.BLOCK_GLASS_PLACE,
                        SoundCategory.BLOCKS, 1.0f, 1.5f);

                BlockEntity be = world.getBlockEntity(placePos);
                if (be instanceof AmpuleBlockEntity entity) {
                    entity.setMaxCapacityMl(containerType.getDefaultCapacityMl());
                    entity.setSealed(isSealed(stack));
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
