/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.transport.pipe.ICustomPipeConnection;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileTank;
import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithTickableTE;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockTank extends BlockBCTile_Neptune<TileTank> implements ICustomPipeConnection, ITankBlockConnector, IWaterLoggable, IBlockWithTickableTE<TileTank> {
    public static final Property<Boolean> JOINED_BELOW = BuildCraftProperties.JOINED_BELOW;
    private static final Property<Boolean> WATERLOGGED = BlockStateProperties.WATERLOGGED;
    // private static final AxisAlignedBB BOUNDING_BOX = new AxisAlignedBB(2 / 16D, 0 / 16D, 2 / 16D, 14 / 16D, 16 / 16D, 14 / 16D);
    private static final VoxelShape BOUNDING_BOX = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public BlockTank(String idBC, AbstractBlock.Properties props) {
        super(idBC, props);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(JOINED_BELOW, false)
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    public TileBC_Neptune newBlockEntity(net.minecraft.world.IBlockReader world) {
        return BCFactoryBlocks.tankTile.get().create();
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.add(JOINED_BELOW);
        properties.add(BlockStateProperties.WATERLOGGED);
    }

    // 1.18.2: moved to BCFactory#clientSetup
//    @SideOnly(Side.CLIENT)
//    @Override
//    public BlockRenderLayer getBlockLayer() {
//        return BlockRenderLayer.CUTOUT;
//    }

//    @Override
//    public boolean isFullCube(IBlockState state) {
//        return false;
//    }

//    @Override
//    public boolean isOpaqueCube(IBlockState state) {
//        return false;
//    }

    @Override
    public boolean propagatesSkylightDown(BlockState p_49928_, IBlockReader p_49929_, BlockPos p_49930_) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, IBlockReader world, BlockPos pos) {
        return 1.0F;
    }

    @Override
//    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
    public VoxelShape getShape(BlockState state, IBlockReader getter, BlockPos pos, ISelectionContext context) {
        return BOUNDING_BOX;
    }

    @Override
    public BlockState getActualState(BlockState state, IWorld world, BlockPos pos, TileEntity tile) {
        boolean isTankBelow = world.getBlockState(pos.below()).getBlock() instanceof ITankBlockConnector;
        return state.setValue(JOINED_BELOW, isTankBelow);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = super.getStateForPlacement(context);
        boolean isInWater = oldState.getBlock() == Blocks.WATER && oldState.getFluidState().isSource(); // Calen: should be called before #getActualState, or the state will be changed from Water block to Tank block
        newState = getActualState(newState, world, pos, null);
        return newState.setValue(WATERLOGGED, isInWater);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        state = super.updateShape(state, facing, facingState, world, pos, facingPos);
        return getActualState(state, world, pos, null);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    // 1.18.2: ret OPPOSITE value to 1.12.2!
//    public boolean shouldSideBeRendered(IBlockAccess world, BlockPos pos, EnumFacing side)
    public boolean skipRendering(BlockState thisState, BlockState otherState, Direction side) {
        if (otherState.is(this)) {
            return !(side.getAxis() != Axis.Y || !(otherState.getBlock() instanceof ITankBlockConnector));
        }
        return super.skipRendering(thisState, otherState, side);
    }

    @Override
//    public boolean hasComparatorInputOverride(BlockState state)
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
//    public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos)
    public int getAnalogOutputSignal(BlockState state, World level, BlockPos pos) {
        TileEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileTank) {
            return ((TileTank) tile).getComparatorLevel();
        }
        return 0;
    }

    @Override
    public float getExtension(World world, BlockPos pos, Direction face, BlockState state) {
        return face.getAxis() == Axis.Y ? 0 : 2 / 16f;
    }

    // IWaterLoggable

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
