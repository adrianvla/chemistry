package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static org.keke.chemistry.Chemistry.sendMessage;

public class BeakerLiquidBlockEntity extends BlockEntity {
    private int number = 7;
    private int color = 0x3495eb;
    public BeakerLiquidBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEAKER_LIQUID, pos, state);
    }
    public static void tick(World world, BlockPos pos, BlockState state, BeakerLiquidBlockEntity be) {
        // Increase the number by one every tick
        be.number++;
//        be.color += 0x000001;
//        // If the number is greater than 10, reset it to 0
//        if (be.color > 0xffffff) {
//            be.color = 0x000000;
//        }
        if (be.number > 10) {
            be.number = 0;
        }
        // Mark the block entity for an update, so that the client knows it has to re-render
        be.markDirty();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        // Save the current value of the number to the nbt
        nbt.putInt("number", number);
        nbt.putInt("color",color);
        sendMessage("Write Color:"+String.format("0x%06X", color));

        super.writeNbt(nbt);
    }
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        number = nbt.getInt("number");
        color = nbt.getInt("color");
        sendMessage("Read Color:"+String.format("0x%06X", color));
    }
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public NbtCompound getNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("color",color);
        return nbt;
    }
}
