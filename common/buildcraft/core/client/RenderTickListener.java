/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.client;

import buildcraft.api.core.IBox;
import buildcraft.api.items.IMapLocation.MapLocationType;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemMapLocation;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.debug.ClientDebuggables;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.marker.MarkerSubCache;
import buildcraft.lib.misc.MatrixUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.Box;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenderTickListener {
    private static final Vec3[][][] MAP_LOCATION_POINT = new Vec3[6][][];
    private static final String DIFF_START, DIFF_HEADER_FORMATTING;

    private static final Box LAST_RENDERED_MAP_LOC = new Box();

    static {
        double[][][] upFace = { // Comments for formatting
                { { 0.5, 0.9, 0.5 }, { 0.5, 1.6, 0.5 } }, // Main line
                { { 0.5, 0.9, 0.5 }, { 0.8, 1.2, 0.5 } }, // First arrow part (+X)
                { { 0.5, 0.9, 0.5 }, { 0.2, 1.2, 0.5 } }, // Second arrow part (-X)
                { { 0.5, 0.9, 0.5 }, { 0.5, 1.2, 0.8 } }, // Third arrow part (+Z)
                { { 0.5, 0.9, 0.5 }, { 0.5, 1.2, 0.2 } }, // Forth arrow part (-Z)
        };

        for (Direction face : Direction.values()) {
            Matrix4f matrix = MatrixUtil.rotateTowardsFace(Direction.UP, face);
            Vec3[][] arr = new Vec3[5][2];
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 2; j++) {
                    double[] from = upFace[i][j];
                    Point3f point = new Point3f(new Point3d(from));
                    matrix.transform(point);
                    Vec3 to = new Vec3(point.x, point.y, point.z);
                    arr[i][j] = to;
                }
            }

            MAP_LOCATION_POINT[face.ordinal()] = arr;
        }
        DIFF_START = ChatFormatting.RED + "" + ChatFormatting.BOLD + "!" + ChatFormatting.RESET;
        DIFF_HEADER_FORMATTING = ChatFormatting.AQUA + "" + ChatFormatting.BOLD;
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getInstance();
        IDebuggable debuggable = ClientDebuggables.getDebuggableObject(mc.hitResult);
        if (debuggable != null) {
//            List<String> clientLeft = new ArrayList<>();
//            List<String> clientRight = new ArrayList<>();
            List<Component> clientLeft = new ArrayList<>();
            List<Component> clientRight = new ArrayList<>();
//            debuggable.getDebugInfo(clientLeft, clientRight, mc.hitResult.sideHit);
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                debuggable.getDebugInfo(clientLeft, clientRight, blockHitResult.getDirection());
            } else {
                debuggable.getDebugInfo(clientLeft, clientRight, null);
            }
            String headerFirst = DIFF_HEADER_FORMATTING + "SERVER:";
            String headerSecond = DIFF_HEADER_FORMATTING + "CLIENT:";
            appendDiff(event.getLeft(), ClientDebuggables.SERVER_LEFT, clientLeft, headerFirst, headerSecond);
            appendDiff(event.getRight(), ClientDebuggables.SERVER_RIGHT, clientRight, headerFirst, headerSecond);
//            debuggable.getClientDebugInfo(event.getLeft(), event.getRight(), mc.objectMouseOver.sideHit);
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                debuggable.getClientDebugInfo(event.getLeft(), event.getRight(), blockHitResult.getDirection());
            } else {
                debuggable.getClientDebugInfo(event.getLeft(), event.getRight(), null);
            }
        }
    }

    // private static void appendDiff(List<String> dest, List<String> first, List<String> second, String headerFirst, String headerSecond)
    private static void appendDiff(List<String> dest, List<Component> first, List<Component> second, String headerFirst, String headerSecond) {
        dest.add("");
        dest.add(headerFirst);
//        dest.addAll(first);
        dest.addAll(first.stream().map(Component::getString).toList());
        dest.add("");
        dest.add(headerSecond);
        if (first.size() != second.size()) {
            // no diffing
//            dest.addAll(second);
            dest.addAll(second.stream().map(Component::getString).toList());
        } else {
            for (int l = 0; l < first.size(); l++) {
//                String shownLine = first.get(l);
                String shownLine = first.get(l).getString();
//                String diffLine = second.get(l);
                String diffLine = second.get(l).getString();
                if (shownLine.equals(diffLine)) {
                    dest.add(diffLine);
                } else {
                    if (diffLine.startsWith(" ")) {
                        dest.add(DIFF_START + diffLine.substring(1));
                    } else {
                        dest.add(DIFF_START + diffLine);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderLast(RenderLevelStageEvent event) {
        // Calen: AFTER_SKY is the correct state for this render
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        float partialTicks = event.getPartialTick();
        renderHeldItemInWorld(partialTicks, event.getPoseStack(), event.getCamera());
    }

    private static void renderHeldItemInWorld(float partialTicks, PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack mainHand = StackUtil.asNonNull(player.getMainHandItem());
        ItemStack offHand = StackUtil.asNonNull(player.getOffhandItem());
        ClientLevel world = mc.level;

        mc.getProfiler().push("bc");
        mc.getProfiler().push("renderWorld");

        // Calen: push and translate by camera
        DetachedRenderer.fromWorldOriginPre(player, partialTicks, poseStack, camera);

        Item mainHandItem = mainHand.getItem();
        Item offHandItem = offHand.getItem();

        VertexConsumer bb = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(Sheets.solidBlockSheet());

        if (mainHandItem == BCCoreItems.mapLocation.get()) {
            renderMapLocation(mainHand, poseStack, bb);
        } else if (mainHandItem == BCCoreItems.markerConnector.get() || offHandItem == BCCoreItems.markerConnector.get()) {
            renderMarkerConnector(world, player, poseStack, bb);
        }

        // Calen: pop
        DetachedRenderer.fromWorldOriginPost(poseStack);

        mc.getProfiler().pop();
        mc.getProfiler().pop();
    }

    private static void renderMapLocation(@Nonnull ItemStack stack, PoseStack poseStack, VertexConsumer bb) {
        MapLocationType type = MapLocationType.getFromStack(stack);
        if (type == MapLocationType.SPOT) {
            Direction face = ItemMapLocation.getPointFace(stack);
            IBox box = ItemMapLocation.getPointBox(stack);
            if (box != null) {
                Vec3[][] vectors = MAP_LOCATION_POINT[face.ordinal()];
//                GL11.glTranslated(box.min().getX(), box.min().getY(), box.min().getZ());
                poseStack.pushPose();
                poseStack.translate(box.min().getX(), box.min().getY(), box.min().getZ());
                for (Vec3[] vec : vectors) {
                    LaserData_BC8 laser =
                            new LaserData_BC8(BuildCraftLaserManager.STRIPES_WRITE, vec[0], vec[1], 1 / 16.0);
//                    LaserRenderer_BC8.renderLaserStatic(laser);
                    LaserRenderer_BC8.renderLaserDynamic(laser, poseStack.last(), bb);
                }
                poseStack.popPose();
            }

        } else if (type == MapLocationType.AREA) {

            IBox box = ItemMapLocation.getAreaBox(stack);
            LAST_RENDERED_MAP_LOC.reset();
            LAST_RENDERED_MAP_LOC.initialize(box);
//            LaserBoxRenderer.renderLaserBoxStatic(LAST_RENDERED_MAP_LOC, BuildCraftLaserManager.STRIPES_WRITE, true);
            LaserBoxRenderer.renderLaserBoxDynamic(LAST_RENDERED_MAP_LOC, BuildCraftLaserManager.STRIPES_WRITE, poseStack.last(), bb, true);

        } else if (type == MapLocationType.PATH) {
            List<BlockPos> path = BCCoreItems.mapLocation.get().getPath(stack);
            if (path != null && path.size() > 1) {
                BlockPos last = null;
                for (BlockPos p : path) {
                    if (last == null) {
                        last = p;
                    }
                }
            }

            // TODO!
        } else if (type == MapLocationType.ZONE) {
            // TODO!
        }
    }

    private static void renderMarkerConnector(ClientLevel world, Player player, PoseStack poseStack, VertexConsumer bb) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        profiler.push("marker");
        for (MarkerCache<?> cache : MarkerCache.CACHES) {
            profiler.push(cache.name);
            renderMarkerCache(player, cache.getSubCache(world), poseStack, bb);
            profiler.pop();
        }
        profiler.pop();
    }

    private static void renderMarkerCache(Player player, MarkerSubCache<?> cache, PoseStack poseStack, VertexConsumer bb) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        profiler.push("compute");
        Set<LaserData_BC8> toRender = new HashSet<>();
        for (final BlockPos a : cache.getAllMarkers()) {
            for (final BlockPos b : cache.getValidConnections(a)) {
                if (a.asLong() > b.asLong()) {
                    // Only render each pair once
                    continue;
                }

                Vec3 start = VecUtil.convertCenter(a);
                Vec3 end = VecUtil.convertCenter(b);

                Vec3 startToEnd = end.subtract(start).normalize();
                Vec3 endToStart = start.subtract(end).normalize();
                start = start.add(VecUtil.scale(startToEnd, 0.125));
                end = end.add(VecUtil.scale(endToStart, 0.125));

                LaserType laserType = cache.getPossibleLaserType();
                if (laserType == null || isLookingAt(a, b, player)) {
                    laserType = BuildCraftLaserManager.MARKER_DEFAULT_POSSIBLE;
                }

                LaserData_BC8 data = new LaserData_BC8(laserType, start, end, 1 / 16.0);
                toRender.add(data);
            }
        }
        profiler.popPush("render");
        for (LaserData_BC8 laser : toRender) {
//            LaserRenderer_BC8.renderLaserStatic(laser);
            LaserRenderer_BC8.renderLaserDynamic(laser, poseStack.last(), bb);
        }
        profiler.pop();
    }

    private static boolean isLookingAt(BlockPos from, BlockPos to, Player player) {
        return ItemMarkerConnector.doesInteract(from, to, player);
    }
}
