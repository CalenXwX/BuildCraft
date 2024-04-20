/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import buildcraft.lib.block.BlockPropertiesCreater;
import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.transport.block.BlockFilteredBuffer;
import buildcraft.transport.block.BlockPipeHolder;
import buildcraft.transport.tile.TileFilteredBuffer;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;

public class BCTransportBlocks {
    private static final RegistrationHelper HELPER = new RegistrationHelper(BCTransport.MODID);
    public static RegistryObject<BlockFilteredBuffer> filteredBuffer;
    public static RegistryObject<BlockPipeHolder> pipeHolder;

    public static RegistryObject<BlockEntityType<TileFilteredBuffer>> filteredBufferTile;
    public static RegistryObject<BlockEntityType<TilePipeHolder>> pipeHolderTile;

    public static void preInit() {
        filteredBuffer = HELPER.addBlockAndItem("block.filtered_buffer", BlockPropertiesCreater.createDefaultProperties(Material.METAL), BlockFilteredBuffer::new);
        // TODO Calen isViewBlocking necessary?
        pipeHolder = HELPER.addBlock("block.pipe_holder", BlockPropertiesCreater.createDefaultProperties(Material.METAL).strength(0.25F, 3.0F).noOcclusion().isViewBlocking((state, world, pos) -> false).dynamicShape(), BlockPipeHolder::new);

        filteredBufferTile = HELPER.registerTile("tile.filtered_buffer", TileFilteredBuffer::new, filteredBuffer);
        pipeHolderTile = HELPER.registerTile("tile.pipe_holder", TilePipeHolder::new, pipeHolder);
    }
}
