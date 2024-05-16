/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.client.render;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.lib.client.model.MutableVertex;
import buildcraft.lib.client.sprite.DynamicTextureBC;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.lib.misc.data.WorldPos;
import buildcraft.robotics.BCRoboticsBlocks;
import buildcraft.robotics.tile.TileZonePlanner;
import buildcraft.robotics.zone.ZonePlannerMapChunk;
import buildcraft.robotics.zone.ZonePlannerMapChunkKey;
import buildcraft.robotics.zone.ZonePlannerMapDataClient;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.matrix.MatrixStack.Entry;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
public class RenderZonePlanner extends TileEntityRenderer<TileZonePlanner> {
    private static final Cache<WorldPos, DynamicTextureBC> TEXTURES = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).removalListener(RenderZonePlanner::onRemove).build();
    private static final int TEXTURE_WIDTH = 10;
    private static final int TEXTURE_HEIGHT = 8;

    public RenderZonePlanner(TileEntityRendererDispatcher context) {
        super(context);
    }


    private static void onRemove(RemovalNotification<WorldPos, DynamicTextureBC> notification) {
        DynamicTextureBC texture = notification.getValue();
        if (texture != null) {
            texture.deleteGlTexture();
        }
    }

    @Override
//    public final void render(TileZonePlanner tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    public void render(TileZonePlanner tile, float partialTicks, MatrixStack poseStack, IRenderTypeBuffer bufferSource, int combinedLight, int combinedOverlay) {
        Minecraft.getInstance().getProfiler().push("bc");
        Minecraft.getInstance().getProfiler().push("zone");

        double offset = 0.001;
        double minX = 3 / 16D - offset;
        double maxX = 13 / 16D + offset;
        double minY = 5 / 16D - offset;
        double maxY = 13 / 16D + offset;
        double minZ = -offset;
        double maxZ = 1 + offset;

        BlockState state = tile.getLevel().getBlockState(tile.getBlockPos());
        if (state.getBlock() != BCRoboticsBlocks.zonePlanner.get()) {
            return;
        }
        Direction side = state.getValue(BuildCraftProperties.BLOCK_FACING).getOpposite();

        DynamicTextureBC texture = getTexture(tile, side);
        if (texture == null) {
            return;
        }
        poseStack.pushPose();
//        try (AutoTessellator tessellator = RenderUtil.getThreadLocalUnusedTessellator())
//        {
//            BufferBuilder buffer = tessellator.tessellator.getBuffer();
        IVertexBuilder buffer = bufferSource.getBuffer(RenderType.translucent());
        // TODO Calen: it seems that TileEntityRender only can use sprite in AtlasTexture.LOCATION_BLOCKS
        texture.updateTexture();
        texture.bindGlTexture();
//            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
//        RenderSystem.activeTexture(33985);
        // TODO Calen
//        GlStateManager.disableTexture2D();
//        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
//        RenderSystem.activeTexture(33984);
//            GlStateManager.disableBlend();
        RenderUtil.disableBlend();
        // TODO Calen
//         GlStateManager.disableCull();
////        if (Minecraft.isAmbientOcclusionEnabled())
//        if (Minecraft.useAmbientOcclusion()) {
////            GlStateManager.shadeModel(GL11.GL_SMOOTH);
//            RenderSystem.setShader(GameRenderer::getRendertypeEntitySmoothCutoutShader);
//        } else {
////            GlStateManager.shadeModel(GL11.GL_FLAT);
//            RenderSystem.setShader(GameRenderer::getBlockShader);
//        }

//            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
//            buffer.setTranslation(x, y, z);

        Vector3d min;
        Vector3d max;

        float minU = 0;
        float maxU = texture.getMaxU();
        float minV = 0;
        float maxV = texture.getMaxV();

        switch (side) {
            case NORTH:
                min = new Vector3d(minX, minY, maxZ);
                max = new Vector3d(maxX, maxY, maxZ);
                break;
            case EAST:
                min = new Vector3d(minZ, minY, minX);
                max = new Vector3d(minZ, maxY, maxX);
                break;
            case SOUTH:
                min = new Vector3d(minX, minY, minZ);
                max = new Vector3d(maxX, maxY, minZ);
                break;
            case WEST:
            default:
                min = new Vector3d(maxZ, minY, minX);
                max = new Vector3d(maxZ, maxY, maxX);
                break;
        }

        MutableVertex vertex = new MutableVertex();

        vertex.colouri(-1);
        vertex.lighti((byte) 0xF, (byte) 0xF);

        // Calen
        Entry pose = poseStack.last();

        vertex.positiond(min.x, min.y, min.z).texf(minU, minV).render(pose, buffer);
        vertex.positiond(max.x, min.y, max.z).texf(maxU, minV).render(pose, buffer);
        vertex.positiond(max.x, max.y, max.z).texf(maxU, maxV).render(pose, buffer);
        vertex.positiond(min.x, max.y, min.z).texf(minU, maxV).render(pose, buffer);

//            buffer.setTranslation(0, 0, 0);
//            tessellator.tessellator.draw();
//        }
        // TODO Calen
//        RenderHelper.enableStandardItemLighting();
        poseStack.popPose();

        Minecraft.getInstance().getProfiler().pop();
        Minecraft.getInstance().getProfiler().pop();
    }

    private static DynamicTextureBC getTexture(TileZonePlanner tile, Direction side) {
        if (TEXTURES.getIfPresent(new WorldPos(tile)) == null) {
            DynamicTextureBC texture = createTexture(tile, side);
            if (texture != null) {
                TEXTURES.put(new WorldPos(tile), texture);
            }
        }
        return TEXTURES.getIfPresent(new WorldPos(tile));
    }

    private static DynamicTextureBC createTexture(TileZonePlanner tile, Direction side) {
        DynamicTextureBC texture = new DynamicTextureBC(TEXTURE_WIDTH, TEXTURE_HEIGHT);
        for (int textureX = 0; textureX < TEXTURE_WIDTH; textureX++) {
            for (int textureY = 0; textureY < TEXTURE_HEIGHT; textureY++) {
                int posX;
                int posZ;
                int scale = 4;
                int offset1 = (textureX - TEXTURE_WIDTH / 2) * scale;
                int offset2 = (textureY - TEXTURE_HEIGHT / 2) * scale;
                switch (side) {
                    case NORTH:
                        posX = tile.getBlockPos().getX() + offset1;
                        posZ = tile.getBlockPos().getZ() - offset2;
                        break;
                    case EAST:
                        posX = tile.getBlockPos().getX() + offset2;
                        posZ = tile.getBlockPos().getZ() + offset1;
                        break;
                    case SOUTH:
                        posX = tile.getBlockPos().getX() + offset1;
                        posZ = tile.getBlockPos().getZ() + offset2;
                        break;
                    case WEST:
                    default:
                        posX = tile.getBlockPos().getX() - offset2;
                        posZ = tile.getBlockPos().getZ() + offset1;
                        break;
                }
                ChunkPos chunkPos = new ChunkPos(posX >> 4, posZ >> 4);
                texture.setColor(textureX, textureY, -1);
//                ZonePlannerMapChunkKey key = new ZonePlannerMapChunkKey(chunkPos, tile.getWorld().provider.getDimension(), tile.getLevel());
                ZonePlannerMapChunkKey key = new ZonePlannerMapChunkKey(chunkPos, tile.getLevel().dimension(), tile.getLevelBC());
                ZonePlannerMapChunk zonePlannerMapChunk =
                        ZonePlannerMapDataClient.INSTANCE.getChunk(tile.getLevel(), key);
                if (zonePlannerMapChunk != null) {
                    texture.setColor(textureX, textureY, zonePlannerMapChunk.getColour(posX, posZ) | 0xFF_00_00_00);
                } else {
                    return null;
                }
            }
        }
        return texture;
    }
}
