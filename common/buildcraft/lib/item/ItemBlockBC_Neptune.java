/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.item;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.TagManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ItemBlockBC_Neptune extends BlockItem implements IItemBuildCraft {
    public final String id;
    private String unlocalizedName;


    public ItemBlockBC_Neptune(BlockBCBase_Neptune block, Item.Properties properties) {
//        super(block, properties);
        super(block, properties.tab(CreativeTabManager.getTab(TagManager.getTag("item." + block.id, TagManager.EnumTagType.CREATIVE_TAB))));
        this.id = "item." + block.id;
        init();
    }

    @Override
    public String getIdBC() {
        return id;
    }

    @Override
    public void setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName.replace("item.", "tile.");
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return this.unlocalizedName;
    }

    @Override
//    public void addInformation(ItemStack stack, Level world, List<String> tooltip, ITooltipFlag flags)
    public void appendHoverText(ItemStack stack, Level world, List<Component> strings, TooltipFlag flag) {
        super.appendHoverText(stack, world, strings, flag);
//        String tipId = getUnlocalizedName(stack) + ".tip";
        String tipId = getDescriptionId(stack).replace(".name", ".tip");
        if (LocaleUtil.canLocalize(tipId)) {
            strings.add(new TextComponent(ChatFormatting.GRAY + LocaleUtil.localize(tipId)));
        } else if (flag.isAdvanced()) {
            strings.add(new TextComponent(ChatFormatting.GRAY + tipId));
        }
    }
}
