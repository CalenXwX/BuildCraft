/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;
import buildcraft.api.facades.*;
import buildcraft.api.imc.BcImcMessage;
import buildcraft.lib.BCLib;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.ItemStackKey;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.silicon.BCSiliconConfig;
import buildcraft.silicon.recipe.FacadeSwapRecipe;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.Unpooled;
import net.minecraft.block.*;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.EmptyBlockReader;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.InterModComms.IMCMessage;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public enum FacadeStateManager implements IFacadeRegistry {
    INSTANCE;

    public static final boolean DEBUG = BCDebugging.shouldDebugLog("silicon.facade");
    public static final SortedMap<BlockState, FacadeBlockStateInfo> validFacadeStates;
    public static final Map<ItemStackKey, List<FacadeBlockStateInfo>> stackFacades;
    public static FacadeBlockStateInfo defaultState, previewState;

    private static final Map<Block, String> disabledBlocks = new HashMap<>();
    private static final Map<BlockState, ItemStack> customBlocks = new HashMap<>();

    /** An array containing all mods that fail the {@link #doesPropertyConform(Property)} check, and any others.
     * <p>
     * Note: Mods should ONLY be added to this list AFTER it has been reported to them, and taken off the list once a
     * version has been released with the fix. */
    private static final List<String> KNOWN_INVALID_REPORTED_MODS = Arrays.asList(new String[] { //
    });

    static {
        validFacadeStates = new TreeMap<>(BlockUtil.blockStateComparator());
        stackFacades = new HashMap<>();
    }

    public static FacadeBlockStateInfo getInfoForBlock(Block block) {
        return getInfoForState(block.defaultBlockState());
    }

    private static FacadeBlockStateInfo getInfoForState(BlockState state) {
        return validFacadeStates.get(state);
    }

    // public static void receiveInterModComms(IMCMessage message)
    public static void receiveInterModComms(IMCMessage messageOuter, BcImcMessage messageInner) {
        String id = messageOuter.getMethod();
        if (FacadeAPI.IMC_FACADE_DISABLE.equals(id)) {
//            if (!message.isResourceLocationMessage()) {
//                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + message.getSender() + " - "
//                        + id + " should have a resourcelocation value, not a " + message);
//                return;
//            }
            ResourceLocation loc = messageInner.getResourceLocationValue();
//            Block block = Block.REGISTRY.getObject(loc);
            Block block = ForgeRegistries.BLOCKS.getValue(loc);
            if (block == Blocks.AIR || block == null) {
//                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + message.getSender() + " - "
                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + messageOuter.getSenderModId() + " - "
//                        + id + " should have a valid block target, not " + block + " (" + message + ")");
                        + id + " should have a valid block target [" + loc + "], not " + block + " (" + messageInner + ")");
                return;
            }
//            disabledBlocks.put(block, message.getSender());
            disabledBlocks.put(block, messageOuter.getSenderModId());
        } else if (FacadeAPI.IMC_FACADE_CUSTOM.equals(id)) {
            if (!messageInner.isNBTMessage()) {
//                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + message.getSender() + " - "
                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + messageOuter.getSenderModId() + " - "
//                        + id + " should have an nbt value, not a " + message);
                        + id + " should have an nbt value, not a " + messageInner);
                return;
            }
            CompoundNBT nbt = messageInner.getNBTValue();
            String regName = nbt.getString(FacadeAPI.NBT_CUSTOM_BLOCK_REG_KEY);
//            int meta = nbt.getInt(FacadeAPI.NBT_CUSTOM_BLOCK_META);
            BlockState state = NBTUtil.readBlockState(nbt.getCompound(FacadeAPI.NBT_CUSTOM_BLOCK_META));
//            ItemStack stack = new ItemStack(nbt.getCompound(FacadeAPI.NBT_CUSTOM_ITEM_STACK));
            ItemStack stack = ItemStack.of(nbt.getCompound(FacadeAPI.NBT_CUSTOM_ITEM_STACK));
            if (regName.isEmpty()) {
//                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + message.getSender() + " - "
                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + messageOuter.getSenderModId() + " - "
                        + id + " should have a registry name for the block, stored as "
                        + FacadeAPI.NBT_CUSTOM_BLOCK_REG_KEY);
                return;
            }
            if (stack.isEmpty()) {
//                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + message.getSender() + " - "
                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + messageOuter.getSenderModId() + " - "
                        + id + " should have a valid ItemStack stored in " + FacadeAPI.NBT_CUSTOM_ITEM_STACK);
                return;
            }
//            Block block = Block.REGISTRY.getObject(new ResourceLocation(regName));
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(regName));
            if (block == Blocks.AIR || block == null) {
//                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + message.getSender() + " - "
                BCLog.logger.warn("[facade.imc] Received an invalid IMC message from " + messageOuter.getSenderModId() + " - "
//                        + id + " should have a valid block target, not " + block + " (" + message + ")");
                        + id + " should have a valid block target [" + regName + "], not " + block + " (" + messageInner + ")");
                return;
            }
//            BlockState state = block.getStateFromMeta(meta);
            customBlocks.put(state, stack);
        }
    }

    /** @return One of:
     *         <ul>
     *         <li>{@link ActionResultType#SUCCESS} if every state of the block is valid for a facade.
     *         <li>{@link ActionResultType#PASS} if every metadata needs to be checked by
     *         {@link #isValidFacadeState(BlockState)}</li>
     *         <li>{@link ActionResultType#FAIL} with string describing the problem with this block (if it is not valid
     *         for a facade)</li>
     *         </ul>
     */
    private static ActionResult<String> isValidFacadeBlock(Block block) {
        String disablingMod = disabledBlocks.get(block);
        if (disablingMod != null) {
            return new ActionResult<>(ActionResultType.FAIL, "it has been disabled by " + disablingMod);
        }
        if (block instanceof IFluidBlock || block instanceof FlowingFluidBlock) {
            return new ActionResult<>(ActionResultType.FAIL, "it is a fluid block");
        }
        // Calen
        if (block instanceof ChorusFlowerBlock) {
            return new ActionResult<>(ActionResultType.FAIL, "it is ChorusFlowerBlock");
        }
        // if (block instanceof BlockSlime) {
        // return "it is a slime block";
        // }
        if (block instanceof GlassBlock || block instanceof StainedGlassBlock) {
            return new ActionResult<>(ActionResultType.SUCCESS, "");
        }
        return new ActionResult<>(ActionResultType.PASS, "");
    }

    /** @return Any of:
     *         <ul>
     *         <li>{@link ActionResultType#SUCCESS} if this state is valid for a facade.
     *         <li>{@link ActionResultType#FAIL} with string describing the problem with this state (if it is not valid
     *         for a facade)</li>
     *         </ul>
     */
    private static ActionResult<String> isValidFacadeState(BlockState state) {
//        if (state.getBlock().hasTileEntity(state))
        if (state.getBlock().hasTileEntity(state)) {
            return new ActionResult<>(ActionResultType.FAIL, "it has a tile entity");
        }
//        if (state.getRenderType() != EnumBlockRenderType.MODEL)
        if (state.getRenderShape() != BlockRenderType.MODEL) {
            return new ActionResult<>(ActionResultType.FAIL, "it doesn't have a normal model");
        }
//        if (!state.isFullCube())
        VoxelShape shape = state.getCollisionShape(EmptyBlockReader.INSTANCE, BlockPos.ZERO);
//        if (shape.isEmpty() || !shape.bounds().equals(VoxelShapes.block().bounds()))
        if (shape.isEmpty() || shape != VoxelShapes.block()) {
            return new ActionResult<>(ActionResultType.FAIL, "it isn't a full cube");
        }
        return new ActionResult<>(ActionResultType.SUCCESS, "");
    }

    @Nonnull
    private static ItemStack getRequiredStack(BlockState state) {
        ItemStack stack = customBlocks.get(state);
        if (stack != null) {
            return stack;
        }
        Block block = state.getBlock();
//        Item item = Item.getItemFromBlock(block);
        ItemStack item = block.getCloneItemStack(EmptyBlockReader.INSTANCE, BlockPos.ZERO, state);
//        if (item == Items.AIR) {
//            item = block.getItemDropped(state, new Random(0), 0);
//        }
        return item;
    }

    public static void init() {
        defaultState = new FacadeBlockStateInfo(Blocks.AIR.defaultBlockState(), StackUtil.EMPTY, ImmutableSet.of());
        if (FacadeAPI.facadeItem == null) {
            previewState = defaultState;
            return;
        }

        Stopwatch watch = Stopwatch.createStarted();

        for (Block block : ForgeRegistries.BLOCKS) {
            scanBlock(block);
        }

        watch.stop();
        long time = watch.elapsed(TimeUnit.MICROSECONDS);
        if (DEBUG) {
            BCLog.logger.info("[silicon.facade] Scanned blocks for facade. (" + time / 1000 + "ms)");
        }

        previewState = validFacadeStates.get(Blocks.BRICKS.defaultBlockState());
        FacadeSwapRecipe.genRecipes();
    }

    private static void scanBlock(Block block) {
        try {
            if (!DEBUG && KNOWN_INVALID_REPORTED_MODS.contains(block.getRegistryName().getNamespace())) {
                if (BCLib.VERSION.startsWith("7.99")) {
                    BCLog.logger.warn(
                            "[silicon.facade] Skipping " + block + " as it has been added to the list of broken mods!");
                    return;
                }
            }

            // Check to make sure that all the properties work properly
            // Fixes a bug in extra utilities who doesn't serialise and deserialise properties properly

            boolean allPropertiesOk = true;
            for (Property<?> property : block.getStateDefinition().getProperties()) {
                allPropertiesOk &= doesPropertyConform(property);
            }
            if (!allPropertiesOk) {
                return;
            }

            ActionResult<String> result = isValidFacadeBlock(block);
            // These strings are hardcoded, so we can get away with not needing the .equals check
            if (result.getResult() != ActionResultType.PASS && result.getResult() != ActionResultType.SUCCESS) {
                if (DEBUG) {
                    BCLog.logger.info("[silicon.facade] Disallowed block " + block.getRegistryName() + " because "
                            + result.getResult());
                }
                return;
            } else if (DEBUG) {
                if (result.getResult() == ActionResultType.SUCCESS) {
                    BCLog.logger.info("[silicon.facade] Allowed block " + block.getRegistryName());
                }
            }
            Map<BlockState, ItemStack> usedStates = new HashMap<>();
            Map<ItemStackKey, Map<Property<?>, Comparable<?>>> varyingProperties = new HashMap<>();
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                // state = block.getStateFromMeta(block.getMetaFromState(state));
                // if (!checkedStates.add(state)) {
                // continue;
                // }
                if (result.getResult() != ActionResultType.SUCCESS) {
                    result = isValidFacadeState(state);
                    if (result.getResult() == ActionResultType.SUCCESS) {
                        if (DEBUG) {
                            BCLog.logger.info("[silicon.facade] Allowed state " + state);
                        }
                    } else {
                        if (DEBUG) {
                            BCLog.logger
                                    .info("[silicon.facade] Disallowed state " + state + " because " + result.getResult());
                        }
                        continue;
                    }
                }
                final ItemStack requiredStack;
                try {
                    requiredStack = getRequiredStack(state);
                } catch (RuntimeException e) {
                    BCLog.logger.warn(
                            "[silicon.facade] Disallowed state " + state
                                    + " after getRequiredStack(state) threw an exception!", e
                    );
                    continue;
                }
                if (requiredStack.isEmpty()) {
                    BCLog.logger.info(
                            "[silicon.facade] Disallowed state " + state
                                    + " because required item is empty"
                    );
                    continue;
                }
                usedStates.put(state, requiredStack);
                ItemStackKey stackKey = new ItemStackKey(requiredStack);
                Map<Property<?>, Comparable<?>> vars = varyingProperties.get(stackKey);
                if (vars == null) {
                    final Map<Property<?>, Comparable<?>> varsFinal = new HashMap<>();
                    state.getProperties().forEach((property -> varsFinal.put(property, state.getValue(property))));
                    vars = varsFinal;
                    varyingProperties.put(stackKey, vars);
                } else {
//                    for (Entry<Property<?>, Comparable<?>> entry : state.getProperties().entrySet())
                    for (Property<?> prop : state.getProperties()) {
//                        IProperty<?> prop = entry.getKey();
//                        Comparable<?> value = entry.getValue();
                        Comparable<?> value = state.getValue(prop);
                        if (vars.get(prop) != value) {
                            vars.put(prop, null);
                        }
                    }
                }

                // Calen
                if (block == Blocks.NOTE_BLOCK) {
                    if (!BCSiliconConfig.differStatesOfNoteBlockForFacade) {
                        break;
                    }
                }
            }
            PacketBufferBC testingBuffer = PacketBufferBC.asPacketBufferBc(Unpooled.buffer());
            varyingProperties.forEach((key, vars) ->
            {
                if (DEBUG) {
                    BCLog.logger.info("[silicon.facade]   pre-" + key + ":");
                    vars.keySet().forEach(p -> BCLog.logger.info("[silicon.facade]       " + p));
                }
                vars.values().removeIf(Objects::nonNull);
                if (DEBUG && !vars.isEmpty()) {
                    BCLog.logger.info("[silicon.facade]   " + key + ":");
                    vars.keySet().forEach(p -> BCLog.logger.info("[silicon.facade]       " + p));
                }
            });
            for (Entry<BlockState, ItemStack> entry : usedStates.entrySet()) {
                BlockState state = entry.getKey();
                ItemStack stack = entry.getValue();
                Map<Property<?>, Comparable<?>> vars = varyingProperties.get(new ItemStackKey(stack));
                try {
                    ImmutableSet<Property<?>> varSet = ImmutableSet.copyOf(vars.keySet());
                    FacadeBlockStateInfo info = new FacadeBlockStateInfo(state, stack, varSet);
                    validFacadeStates.put(state, info);
                    if (!info.requiredStack.isEmpty()) {
                        ItemStackKey stackKey = new ItemStackKey(info.requiredStack);
                        stackFacades.computeIfAbsent(stackKey, k -> new ArrayList<>()).add(info);
                    }

                    // Test to make sure that we can read + write it
                    FacadePhasedState phasedState = info.createPhased(null);
                    CompoundNBT nbt = phasedState.writeToNbt();
                    FacadePhasedState read = FacadePhasedState.readFromNbt(nbt);
                    if (read.stateInfo != info) {
                        throw new IllegalStateException("Read (from NBT) state was different! (\n\t" + read.stateInfo
                                + "\n !=\n\t" + info + "\n\tNBT = " + nbt + "\n)");
                    }
                    phasedState.writeToBuffer(testingBuffer);
                    read = FacadePhasedState.readFromBuffer(testingBuffer);
                    if (read.stateInfo != info) {
                        throw new IllegalStateException("Read (from buffer) state was different! (\n\t" + read.stateInfo
                                + "\n !=\n\t" + info + "\n)");
                    }
                    testingBuffer.clear();
                    if (DEBUG) {
                        BCLog.logger.info("[silicon.facade]   Added " + info);
                    }
                } catch (Throwable t) {
                    String msg = "Scanning facade states";
                    msg += "\n\tState = " + state;
                    msg += "\n\tBlock = " + safeToString(() -> state.getBlock().getRegistryName());
                    msg += "\n\tStack = " + stack;
                    msg += "\n\tvarying-properties: {";
                    for (Entry<Property<?>, Comparable<?>> varEntry : vars.entrySet()) {
                        msg += "\n\t\t" + varEntry.getKey() + " = " + varEntry.getValue();
                    }
                    msg += "\n\t}";
                    throw new IllegalStateException(msg.replace("\t", "    "), t);
                }
            }
        } catch (RuntimeException e) {
            if (e instanceof IllegalStateException) {
                // This one needs to exit properly
                throw e;
            }
            BCLog.logger.warn("[silicon.facade] Skipping " + block + " as something about it threw an exception! ", e);
        }
    }

    private static <V extends Comparable<V>> boolean doesPropertyConform(Property<V> property) {
        try {
            property.getValue("");
        } catch (AbstractMethodError error) {
            String message = "Invalid Property object detected!";
            message += "\n  Class = " + property.getClass();
            message += "\n  Method not overriden: Property.parseValue(String)";
            RuntimeException exception = new RuntimeException(message, error);
//            if (BCLib.DEV || !BCLib.MC_VERSION.equals("1.12.2"))
            if (BCLib.DEV || !BCLib.MC_VERSION.equals("1.18.2")) {
                throw exception;
            } else {
                BCLog.logger.error("[silicon.facade] Invalid property!", exception);
            }
            return false;
        }

        boolean allFine = true;
        for (V value : property.getPossibleValues()) {
            String name = property.getName(value);
            Optional<V> optional = property.getValue(name);
            V parsed = optional == null ? null : optional.orElse(null);
            if (!Objects.equals(value, parsed)) {
                allFine = false;
                // A property is *wrong*
                // this is a big problem
                String message = "Invalid property value detected!";
                message += "\n  Property class = " + property.getClass();
                message += "\n  Property = " + property;
                message += "\n  Possible Values = " + property.getPossibleValues();
                message += "\n  Value Name = " + name;
                message += "\n  Value (original) = " + value;
                message += "\n  Value (parsed) = " + parsed;
                message += "\n  Value class (original) = " + (value == null ? null : value.getClass());
                message += "\n  Value class (parsed) = " + (parsed == null ? null : parsed.getClass());
                if (optional == null) {
                    // Massive issue
//                    message += "\n  Property.parseValue() -> Null com.google.common.base.Optional!!";
                    message += "\n  Property.parseValue() -> Null java.util.Optional!!";
                }
                message += "\n";
                // This check *intentionally* crashes on a new MC version
                // or in a dev environment
                // as this really needs to be fixed
                RuntimeException exception = new RuntimeException(message);
//                if (BCLib.DEV || !BCLib.MC_VERSION.equals("1.12.2"))
                if (BCLib.DEV || !BCLib.MC_VERSION.equals("1.18.2")) {
                    throw exception;
                } else {
                    BCLog.logger.error("[silicon.facade] Invalid property!", exception);
                }
            }
        }
        return allFine;
    }

    private static String safeToString(Callable<Object> callable) {
        try {
            return Objects.toString(callable.call());
        } catch (Throwable t) {
            return "~~ERROR~~" + t.getMessage();
        }
    }

    // IFacadeRegistry

    @Override
    public Collection<? extends IFacadeState> getValidFacades() {
        return validFacadeStates.values();
    }

    @Override
    public IFacadePhasedState createPhasedState(IFacadeState state, DyeColor activeColor) {
        return new FacadePhasedState((FacadeBlockStateInfo) state, activeColor);
    }

    @Override
    public IFacade createPhasedFacade(IFacadePhasedState[] states, boolean isHollow) {
        FacadePhasedState[] realStates = new FacadePhasedState[states.length];
        for (int i = 0; i < states.length; i++) {
            realStates[i] = (FacadePhasedState) states[i];
        }
        return new FacadeInstance(realStates, isHollow);
    }
}
