/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import buildcraft.core.item.ItemList_BC8;
import net.minecraft.Util;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public enum BCCoreGuis {
    LIST;

    //    public void openGUI(Player player)
//    {
////        player.openGui(BuildCraft.INSTANCE, ordinal(), player.getLevel(), 0, 0, 0);
//    }
//
//    public void openGUI(Player player, BlockPos pos)
//    {
////        player.openGui(BuildCraft.INSTANCE, ordinal(), player.getLevel(), pos.getX(), pos.getY(), pos.getZ());
//    }
    public void openGUI(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
//            player.openMenu(state.getMenuProvider(player.level, pos));
            if (stack.getItem() instanceof ItemList_BC8 list) {
                NetworkHooks.openGui(serverPlayer, list, serverPlayer.blockPosition());
            } else {
                player.sendMessage(new TranslatableComponent("buildcraft.error.open_null_menu"), Util.NIL_UUID);
            }
        }
    }
}
