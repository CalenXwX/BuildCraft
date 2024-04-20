/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.robotics.gui;

import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemPaintbrush_BC8;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.robotics.zone.*;
import buildcraft.robotics.zone.ZonePlannerMapChunk.MapColourData;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiZonePlanner extends GuiBC8<ContainerZonePlanner> {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcraftrobotics:textures/gui/zone_planner.png");
    private static final int SIZE_X = 256, SIZE_Y = 228;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS_INPUT = new GuiIcon(TEXTURE_BASE, 9, 228, 28, 9);
    private static final GuiIcon ICON_PROGRESS_OUTPUT = new GuiIcon(TEXTURE_BASE, 0, 228, 9, 28);
    private static final GuiRectangle RECT_PROGRESS_INPUT = new GuiRectangle(44, 128, 28, 9);
    private static final GuiRectangle RECT_PROGRESS_OUTPUT = new GuiRectangle(236, 45, 9, 28);
    private float startMouseX = 0;
    private float startMouseY = 0;
    private float startPositionX = 0;
    private float startPositionZ = 0;
    private float camY = 256;
    private float scaleSpeed = 0;
    private float positionX;
    private float positionZ;
    private boolean canDrag = false;
    private BlockPos lastSelected = null;
    private BlockPos selectionStartXZ = null;
    private ZonePlan bufferLayer = null;

    public GuiZonePlanner(ContainerZonePlanner container, Inventory inventory, Component component) {
        super(container, inventory, component);
//        xSize = SIZE_X;
        imageWidth = SIZE_X;
//        ySize = SIZE_Y;
        imageHeight = SIZE_Y;
//        BlockPos tilePos = container.tile.getPos();
        BlockPos tilePos = container.tile.getBlockPos();
        positionX = tilePos.getX();
        positionZ = tilePos.getZ();
    }

    private ItemStack getCurrentStack() {
//        return mc.player.inventory.getItemStack();
        return minecraft.player.containerMenu.getCarried();
    }

    private ItemStack getPaintbrush() {
        ItemStack currentStack = getCurrentStack();
        if (!currentStack.isEmpty() && currentStack.getItem() instanceof ItemPaintbrush_BC8) {
            return currentStack;
        }
        return null;
    }

    private ItemPaintbrush_BC8.Brush getPaintbrushBrush() {
        ItemStack paintbrush = getPaintbrush();
        if (paintbrush != null) {
            ItemPaintbrush_BC8.Brush brush = BCCoreItems.paintbrushClean.get().getBrushFromStack(paintbrush);
            if (brush.colour != null) {
                return brush;
            }
        }
        return null;
    }

//    @Override
//    public void handleMouseInput() throws IOException
//    {
//        int wheel = Mouse.getEventDWheel();
//        if (wheel != 0)
//        {
//            scaleSpeed -= wheel / 30F;
//        }
//        super.handleMouseInput();
//    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            // Calen: 1.12.2 Mouse.getEventDWheel = 1.18.2 mouseScrolled delta*120
//            scaleSpeed -= delta / 30F;
            scaleSpeed -= delta * 4.0F;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
//    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        canDrag = false;
        if (getPaintbrush() != null) {
            if (lastSelected != null) {
                selectionStartXZ = lastSelected;
            }
        } else if (getCurrentStack().isEmpty()) {
            startPositionX = positionX;
            startPositionZ = positionZ;
//            startMouseX = mouseX;
            startMouseX = (float) mouseX;
//            startMouseY = mouseY;
            startMouseY = (float) mouseY;
            canDrag = true;
        }
        return true;
    }

    @Override
//    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double startX, double startY) {
//        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        super.mouseDragged(mouseX, mouseY, clickedMouseButton, startX, startY);
        if (!canDrag) {
            if (lastSelected != null && getPaintbrushBrush() != null) {
//                bufferLayer = new ZonePlan(container.tile.layers[getPaintbrushBrush().colour.getMetadata()]);
                bufferLayer = new ZonePlan(container.tile.layers[getPaintbrushBrush().colour.getId()]);
                if (selectionStartXZ != null && getPaintbrushBrush() != null && lastSelected != null) {
                    for (int x = Math.min(selectionStartXZ.getX(), lastSelected.getX());
                         x < Math.max(selectionStartXZ.getX(), lastSelected.getX());
                         x++) {
                        for (int z = Math.min(selectionStartXZ.getZ(), lastSelected.getZ());
                             z < Math.max(selectionStartXZ.getZ(), lastSelected.getZ());
                             z++) {
                            if (clickedMouseButton == 0) {
                                bufferLayer.set(
                                        x - container.tile.getBlockPos().getX(),
                                        z - container.tile.getBlockPos().getZ(),
                                        true
                                );
                            } else if (clickedMouseButton == 1) {
                                bufferLayer.set(
                                        x - container.tile.getBlockPos().getX(),
                                        z - container.tile.getBlockPos().getZ(),
                                        false
                                );
                            }
                        }
                    }
                }
            }
//            return;
            return true;
        }
        float deltaX = (float) (mouseX - startMouseX);
        float deltaY = (float) (mouseY - startMouseY);
        float s = 0.3F;
        positionX = startPositionX - deltaX * s;
        positionZ = startPositionZ - deltaY * s;

        return true;
    }

    @Override
//    protected void mouseReleased(int mouseX, int mouseY, int state)
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        selectionStartXZ = null;
        if (getPaintbrushBrush() != null && bufferLayer != null) {
//            container.tile.layers[getPaintbrushBrush().colour.getMetadata()] = bufferLayer;
            container.tile.layers[getPaintbrushBrush().colour.getId()] = bufferLayer;
//            container.tile.sendLayerToServer(getPaintbrushBrush().colour.getMetadata());
            container.tile.sendLayerToServer(getPaintbrushBrush().colour.getId());
        }
        bufferLayer = null;

        return true;
    }

    @Override
//    protected void drawBackgroundLayer(float partialTicks)
    protected void drawBackgroundLayer(float partialTicks, PoseStack poseStack) {
        // Calen debug
////        ICON_GUI.drawAt(mainGui.rootElement);
//        ICON_GUI.drawAt(mainGui.rootElement, poseStack);

        drawProgress(
                RECT_PROGRESS_INPUT,
                ICON_PROGRESS_INPUT,
                poseStack,
                container.tile.deltaProgressInput.getDynamic(partialTicks),
                1
        );
        drawProgress(
                RECT_PROGRESS_OUTPUT,
                ICON_PROGRESS_OUTPUT,
                poseStack,
                1,
                container.tile.deltaProgressOutput.getDynamic(partialTicks)
        );
    }

    //    private BlockPos rayTrace(int screenX, int screenY)
//    private BlockPos rayTrace(double screenX, double screenY)
    private BlockPos rayTrace(double minChunkX, double minChunkZ, double maxChunkX, double maxChunkZ,
                              double minScreenX, double minScreenY, double maxScreenX, double maxScreenY,
                              double screenX, double screenY) {
        BlockPos found = null;
//        FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
//        FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
//        IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);

//////        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
////        RenderSystem.projectionMatrix.store(projectionBuffer);
//        Minecraft.getInstance().gameRenderer.getProjectionMatrix(this.minecraft.options.fov).store(projectionBuffer);
//////        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
////        RenderSystem.modelViewMatrix.store(modelViewBuffer);
//        RenderSystem.getModelViewMatrix().store(modelViewBuffer);
//////        GlStateManager.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
////        GlStateManager.Viewport.INSTANCE.x(modelViewBuffer);
//        Minecraft.getInstance().gameRenderer.v

//        FloatBuffer positionNearBuffer = BufferUtils.createFloatBuffer(3);
//        FloatBuffer positionFarBuffer = BufferUtils.createFloatBuffer(3);

        // TODO Calen
//        GLU.gluUnProject(screenX, screenY, 0f, modelViewBuffer, projectionBuffer, viewportBuffer, positionNearBuffer);
//        GLU.gluUnProject(screenX, screenY, 1f, modelViewBuffer, projectionBuffer, viewportBuffer, positionFarBuffer);

//        Vector3d rayStart = new Vector3d(positionNearBuffer.get(0), positionNearBuffer.get(1), positionNearBuffer.get(2));
//        Vector3d rayPosition = new Vector3d(rayStart);
//        Vector3d rayDirection = new Vector3d(positionFarBuffer.get(0), positionFarBuffer.get(1), positionFarBuffer.get(2));
//        rayDirection.sub(rayStart);
//        rayDirection.normalize();

        int chunkX = (int) ((screenX - minScreenX) / (maxScreenX - minScreenX) * (maxChunkX - minChunkX) + minChunkX);
        int chunkZ = (int) ((screenY - minScreenY) / (maxScreenY - minScreenY) * (maxChunkZ - minChunkZ) + minChunkZ);

        int blockX = (int) (((screenX - minScreenX) / (maxScreenX - minScreenX) * (maxChunkX - minChunkX) + minChunkX) * 8);
        int blockY = 2;
        int blockZ = (int) (((screenY - minScreenY) / (maxScreenY - minScreenY) * (maxChunkZ - minChunkZ) + minChunkZ) * 8);

        for (int i = 0; i < 10000; i++) {
//            int chunkX = (int) Math.round(rayPosition.getX()) >> 4;
//            int chunkZ = (int) Math.round(rayPosition.getZ()) >> 4;
            ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
                    minecraft.level,
                    new ZonePlannerMapChunkKey(
                            new ChunkPos(chunkX, chunkZ),
//                            minecraft.level.provider.getDimension(),
                            minecraft.level.dimension(),
                            container.tile.getLevelBC()
                    )
            );
            if (zonePlannerMapChunk != null) {
                BlockPos pos = new BlockPos(
//                        Math.round(rayPosition.getX()) - (chunkX << 4),
//                        Math.round(rayPosition.getY()),
//                        Math.round(rayPosition.getZ()) - (chunkZ << 4)
                        blockX, blockY, blockZ
                );
                MapColourData data = zonePlannerMapChunk.getData(pos.getX(), pos.getZ());
                if (data != null && data.posY >= pos.getY()) {
                    found = new BlockPos(pos.getX() + (chunkX << 4), data.posY, pos.getZ() + (chunkZ << 4));
                    break;
                }
            } else {
                break;
            }
//            rayPosition.add(rayDirection);
        }
        return found;
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    @Override
//    protected void drawForegroundLayer()
    protected void drawForegroundLayer(PoseStack poseStack) {
        camY += scaleSpeed;
        scaleSpeed *= 0.7F;
        int posX = (int) positionX;
        int posZ = (int) positionZ;
//        int dimension = mc.world.provider.getDimension();
        ResourceKey<Level> dimension = minecraft.level.dimension();
        {
            ChunkPos chunkPos = new ChunkPos(posX >> 4, posZ >> 4);
            ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
//                    mc.world,
                    minecraft.level,
                    new ZonePlannerMapChunkKey(
                            chunkPos,
                            dimension,
                            container.tile.getLevelBC()
                    )
            );
            BlockPos pos = null;
            if (zonePlannerMapChunk != null) {
                MapColourData data = zonePlannerMapChunk.getData(posX, posZ);
                if (data != null) {
                    pos = new BlockPos(posX, data.posY, posZ);
                }
            }
            if (pos != null && pos.getY() + 10 > camY) {
                camY = Math.max(camY, pos.getY() + 10);
            }
        }
//        int x = guiLeft;
        int x = leftPos;
//        int y = guiTop;
        int y = topPos;
        if (lastSelected != null) {
            String text = "X: " + lastSelected.getX() + " Y: " + lastSelected.getY() + " Z: " + lastSelected.getZ();
//            fontRenderer.drawString(text, x + 130, y + 130, 0x404040);
            font.draw(poseStack, text, x + 130, y + 130, 0x404040);
        }
        int offsetX = 8;
        int offsetY = 9;
        int sizeX = 213;
        int sizeY = 100;
//        GlStateManager.pushMatrix();
        poseStack.pushPose();
        // TODO Calen
//        GlStateManager.matrixMode(GL11.GL_PROJECTION);
//        GlStateManager.pushMatrix();
        poseStack.pushPose();
//        GlStateManager.loadIdentity();
//        ScaledResolution scaledResolution = new ScaledResolution(mc);
        double scale = minecraft.getWindow().getGuiScale();
        double scaledHeight = minecraft.getWindow().getGuiScaledHeight();
//        int viewportX = (x + offsetX) * scaledResolution.getScaleFactor();
        int viewportX = (int) ((x + offsetX) * scale);
//        int viewportY = mc.displayHeight - (sizeY + y + offsetY) * scaledResolution.getScaleFactor();
        int viewportY = (int) (minecraft.getWindow().getGuiScaledHeight() - (sizeY + y + offsetY) * scale);
//        int viewportWidth = sizeX * scaledResolution.getScaleFactor();
        int viewportWidth = (int) (sizeX * scale);
//        int viewportHeight = sizeY * scaledResolution.getScaleFactor();
        int viewportHeight = (int) (sizeY * scale);
        // TODO Calen
////        GL11.glEnable(GL11.GL_SCISSOR_TEST);
//        GlStateManager._enableScissorTest();
////        GL11.glScissor(
//        GlStateManager._scissorBox(
//                viewportX,
//                viewportY,
//                viewportWidth,
//                viewportHeight
//        );
//        // TODO Calen
////        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
////        GL11.glDisable(GL11.GL_SCISSOR_TEST);
//        GlStateManager._disableScissorTest();
        // TODO Calen
////        GlStateManager.viewport(
//        GlStateManager._viewport(
//                viewportX,
//                viewportY,
//                viewportWidth,
//                viewportHeight
//        );
//        GlStateManager.scale(scaledResolution.getScaleFactor(), scaledResolution.getScaleFactor(), 1);
        poseStack.scale((float) scale, (float) scale, 1);
//        GLU.gluPerspective(70.0F, (float) sizeX / sizeY, 1F, 10000.0F);
        poseStack.last().pose().multiply(Matrix4f.perspective(70.0F, (float) sizeX / sizeY, 1F, 10000.0F));
////        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
//        RenderSystem.applyModelViewMatrix();
        // TODO Calen
//        GlStateManager.loadIdentity();
//        RenderHelper.enableStandardItemLighting();
//        GlStateManager.enableRescaleNormal();
        // TODO Calen ???
//        GlStateManager.rotate(90, 1, 0, 0); // look down
        poseStack.mulPose(Vector3f.XP.rotation(90));
//        poseStack.mulPose(Vector3f.ZP.rotation(1));
//        GlStateManager.pushMatrix();
        poseStack.pushPose();
        // TODO Calen test
////        GlStateManager.translate(-positionX, -camY, -positionZ);
//        poseStack.translate(-positionX, -camY, -positionZ);
//        GlStateManager.disableBlend();
        RenderUtil.disableBlend();
        // TODO Calen
//        GlStateManager.disableAlpha();
//        GlStateManager.disableTexture2D();
//        int minScreenX = (x + offsetX) * scaledResolution.getScaleFactor();
        int minScreenX = (int) ((x + offsetX) * scale);
//        int minScreenY = (scaledResolution.getScaledHeight() - (y + offsetY)) * scaledResolution.getScaleFactor();
        int minScreenY = (int) ((scaledHeight - (y + offsetY)) * scale);
//        int maxScreenX = (x + offsetX + sizeX) * scaledResolution.getScaleFactor();
        int maxScreenX = (int) ((x + offsetX + sizeX) * scale);
//        int maxScreenY = (scaledResolution.getScaledHeight() - (y + offsetY + sizeY)) * scaledResolution.getScaleFactor();
        int maxScreenY = (int) ((scaledHeight - (y + offsetY + sizeY)) * scale);
        int minChunkX = (posX >> 4) - 8;
        int minChunkZ = (posZ >> 4) - 8;
        int maxChunkX = (posX >> 4) + 8;
        int maxChunkZ = (posZ >> 4) + 8;
        // Calen
        int minChunkXTemp = minChunkX;
        int minChunkZTemp = minChunkZ;
        int maxChunkXTemp = maxChunkX;
        int maxChunkZTemp = maxChunkZ;
        // noinspection SuspiciousNameCombination
        List<ChunkPos> chunkPosBounds = Stream.of(
                        Pair.of(minScreenX, minScreenY),
                        Pair.of(minScreenX, maxScreenY),
                        Pair.of(maxScreenX, minScreenY),
                        Pair.of(maxScreenX, maxScreenY)
                )
//                .map(p -> rayTrace(p.getLeft(), p.getRight()))
                .map(p -> rayTrace(minChunkXTemp, minChunkZTemp, maxChunkXTemp, maxChunkZTemp, minScreenX, minScreenY, maxScreenX, maxScreenY, p.getLeft(), p.getRight()))
                .filter(Objects::nonNull)
                .map(ChunkPos::new)
                .collect(Collectors.toList());
        for (ChunkPos chunkPos : chunkPosBounds) {
            if (chunkPos.x < minChunkX) {
                minChunkX = chunkPos.x;
            }
            if (chunkPos.z < minChunkZ) {
                minChunkZ = chunkPos.z;
            }
            if (chunkPos.x > maxChunkX) {
                maxChunkX = chunkPos.x;
            }
            if (chunkPos.z > maxChunkZ) {
                maxChunkZ = chunkPos.z;
            }
        }
        minChunkX--;
        minChunkZ--;
        maxChunkX++;
        maxChunkZ++;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ZonePlannerMapRenderer.INSTANCE.getChunkGlList(
                        new ZonePlannerMapChunkKey(
                                new ChunkPos(chunkX, chunkZ),
                                dimension,
                                container.tile.getLevelBC()
                        ),
                        poseStack
                );
//                        .ifPresent(GlStateManager::callList);
            }
        }

        BlockPos found = null;
        int foundColor = 0;
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
//        if (Mouse.getX() >= minScreenX &&
//                Mouse.getY() <= minScreenY &&
//                Mouse.getX() <= maxScreenX &&
//                Mouse.getY() >= maxScreenY)
        if (mouse.xpos() >= minScreenX &&
                (scaledHeight * scale - mouse.ypos()) <= minScreenY &&
                mouse.xpos() <= maxScreenX &&
                (scaledHeight * scale - mouse.ypos()) >= maxScreenY)
        {
//            found = rayTrace(Mouse.getX(), Mouse.getY());
//            found = rayTrace(mouse.xpos(), (scaledHeight * scale - mouse.ypos()));
            found = rayTrace(minChunkX, minChunkZ, maxChunkX, maxChunkZ, minScreenX, minScreenY, maxScreenX, maxScreenY, mouse.xpos(), (scaledHeight * scale - mouse.ypos()));
        }
        if (found != null) {
            ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
                    minecraft.level,
                    new ZonePlannerMapChunkKey(
                            new ChunkPos(found),
//                            mc.world.provider.getDimension(),
                            minecraft.level.dimension(),
                            container.tile.getLevelBC()
                    )
            );
            if (zonePlannerMapChunk != null) {
                MapColourData data = zonePlannerMapChunk.getData(found.getX(), found.getZ());
                if (data != null) {
                    foundColor = data.colour;
                }
            }
        }

        if (found != null) {
//            GlStateManager.disableDepth();
            RenderUtil.disableDepth();
//            GlStateManager.enableBlend();
            RenderUtil.enableBlend();
            // TODO Calen
//            GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
//            GlStateManager.glLineWidth(2);
            int r = (int) (((foundColor >> 16) & 0xFF) * 0.7);
            int g = (int) (((foundColor >> 8) & 0xFF) * 0.7);
            int b = (int) (((foundColor >> 0) & 0xFF) * 0.7);
            int a = 0x77;
            ZonePlannerMapRenderer.INSTANCE.setColor(r << 16 | g << 8 | b << 0 | a << 24);
//            BufferBuilder builder = Tessellator.getInstance().getBuffer();
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
//            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
//            builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//            ZonePlannerMapRenderer.INSTANCE.drawBlockCuboid(builder, found.getX(), found.getY(), found.getZ());
            ZonePlannerMapRenderer.INSTANCE.drawBlockCuboid(builder, poseStack, found.getX(), found.getY(), found.getZ());
//            Tessellator.getInstance().draw();
            Tesselator.getInstance().end();
            // TODO Calen
//            GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
//            GlStateManager.disableBlend();
            RenderUtil.disableBlend();
//            GlStateManager.enableDepth();
            RenderUtil.enableDepth();
        }

        // TODO Calen
//        GlStateManager.disableLighting();
//        GlStateManager.enableBlend();
        RenderUtil.enableBlend();

        for (int i = 0; i < container.tile.layers.length; i++) {
//            if (getPaintbrushBrush() != null && getPaintbrushBrush().colour.getMetadata() != i)
            if (getPaintbrushBrush() != null && getPaintbrushBrush().colour.getId() != i) {
                continue;
            }
            ZonePlan layer = container.tile.layers[i];
//            if (getPaintbrushBrush() != null && getPaintbrushBrush().colour.getMetadata() == i && bufferLayer != null)
            if (getPaintbrushBrush() != null && getPaintbrushBrush().colour.getId() == i && bufferLayer != null) {
                layer = bufferLayer;
            }
            if (!layer.getChunkPoses().isEmpty()) {
//                Tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                Tesselator.getInstance().getBuilder().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
//                        for (int blockX = chunkPos.getXStart(); blockX <= chunkPos.getXEnd(); blockX++)
                        for (int blockX = chunkPos.getMinBlockX(); blockX <= chunkPos.getMaxBlockX(); blockX++) {
//                            for (int blockZ = chunkPos.getZStart(); blockZ <= chunkPos.getZEnd(); blockZ++)
                            for (int blockZ = chunkPos.getMinBlockZ(); blockZ <= chunkPos.getMaxBlockZ(); blockZ++) {
                                if (!layer.get(
                                        blockX - container.tile.getBlockPos().getX(),
                                        blockZ - container.tile.getBlockPos().getZ()
                                ))
                                {
                                    continue;
                                }
                                int height;
                                ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
                                        minecraft.level,
                                        new ZonePlannerMapChunkKey(
                                                chunkPos,
                                                dimension,
                                                container.tile.getLevelBC()
                                        )
                                );
                                if (zonePlannerMapChunk != null) {
                                    MapColourData data = zonePlannerMapChunk.getData(blockX, blockZ);
                                    if (data != null) {
                                        height = data.posY;
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
//                                int color = DyeColor.byMetadata(i).getColorValue();
                                float[] color = DyeColor.byId(i).getTextureDiffuseColors();
//                                int r = (color >> 16) & 0xFF;
                                int r = (int) (color[0] * 255.0F);
//                                int g = (color >> 8) & 0xFF;
                                int g = (int) (color[1] * 255.0F);
//                                int b = (color >> 0) & 0xFF;
                                int b = (int) (color[2] * 255.0F);
                                int a = 0x55;
                                ZonePlannerMapRenderer.INSTANCE.setColor(r << 16 | g << 8 | b << 0 | a << 24);
                                ZonePlannerMapRenderer.INSTANCE.drawBlockCuboid(
//                                        Tessellator.getInstance().getBuffer(),
                                        Tesselator.getInstance().getBuilder(),
                                        poseStack,
                                        blockX,
                                        height + 0.1,
                                        blockZ,
                                        height,
                                        0.6
                                );
                            }
                        }
                    }
                }
//                Tessellator.getInstance().draw();
                Tesselator.getInstance().end();
            }
        }
//        GlStateManager.disableBlend();
        RenderUtil.disableBlend();
        // TODO Calen
//        GlStateManager.disableLighting();
//        GlStateManager.enableTexture2D();

        lastSelected = found;
//        GlStateManager.popMatrix();
        poseStack.popPose();
        // TODO Calen
//        GlStateManager.disableRescaleNormal();
//        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        // TODO Calen
////        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
//        RenderSystem.viewport(0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
//        GlStateManager.popMatrix();
        poseStack.popPose();
//        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
//        GlStateManager.popMatrix();
        poseStack.popPose();
        // TODO Calen
//        RenderHelper.disableStandardItemLighting();
//        GlStateManager.disableBlend();
        RenderUtil.disableBlend();
    }
}
