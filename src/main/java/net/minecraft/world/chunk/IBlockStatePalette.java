package net.minecraft.world.chunk;

import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public interface IBlockStatePalette
{
    int idFor(IBlockState state);

    @Nullable
    IBlockState getBlockState(int indexKey);

    @SideOnly(Side.CLIENT)
    void read(PacketBuffer buf);

    void write(PacketBuffer buf);

    int getSerializedSize();
}