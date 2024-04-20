/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide.parts;

import buildcraft.lib.client.guide.GuiGuide;
import buildcraft.lib.client.guide.PageLine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

import java.util.List;

public class GuideText extends GuidePart {
    public final PageLine text;

    //    public GuideText(GuiGuide gui, Component text)
    public GuideText(GuiGuide gui, String textKey, Component text) {
//        this(gui, new PageLine(0, text, false));
        this(gui, new PageLine(0, textKey, text, false));
    }

    public GuideText(GuiGuide gui, PageLine text) {
        super(gui);
        this.text = text;
    }

    @Override
    public PagePosition renderIntoArea(PoseStack poseStack, int x, int y, int width, int height, PagePosition current, int index) {
        PagePosition newPos = renderLine(poseStack, current, text, x, y, width, height, index);
        if (wasHovered && didRender) {
            List<Component> tooltip = text.getTooltip();
            if (tooltip != null && !tooltip.isEmpty()) {
                gui.tooltips.add(tooltip);
            }
        }
        return newPos;
    }

    @Override
    public PagePosition handleMouseClick(PoseStack poseStack, int x, int y, int width, int height, PagePosition current, int index,
                                         double mouseX, double mouseY) {
        return renderLine(poseStack, current, text, x, y, width, height, -1);
    }

    @Override
    public String toString() {
        return text.toString();
    }
}
