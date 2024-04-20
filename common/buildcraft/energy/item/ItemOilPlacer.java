package buildcraft.energy.item;

import buildcraft.energy.generation.structure.OilPlacer;
import buildcraft.energy.generation.structure.OilStructure;
import buildcraft.energy.generation.structure.OilStructureGenerator;
import buildcraft.energy.generation.structure.OilStructureGenerator.GenType;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.TimeUtil;
import buildcraft.lib.misc.data.Box;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class ItemOilPlacer extends ItemBC_Neptune {
    public ItemOilPlacer(String idBC, Item.Properties properties) {
        super(idBC, properties);
    }

    private static final String TAG_TYPE = "type";

    // Calen: 对方块用和对虚空用都会触发这个
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        final ItemStack itemStack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            final CompoundTag tag = itemStack.getOrCreateTag();

            if (tag.contains(TAG_TYPE)) {
                final byte mode = tag.getByte(TAG_TYPE);
                tag.putByte(TAG_TYPE, (byte) ((mode + 1) % GenType.values().length));
            } else {
                tag.putByte(TAG_TYPE, (byte) GenType.LARGE.ordinal());
            }

            GenType craterType = GenType.values()[tag.getByte(TAG_TYPE)];

            player.sendMessage(new TextComponent("TYPE = " + craterType.name()), Util.NIL_UUID);
        } else {

            this.place(serverLevel, serverPlayer, itemStack, player.blockPosition());
        }
        return InteractionResultHolder.success(itemStack);
    }

    private void place(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.sendMessage(new TextComponent(ChatFormatting.AQUA + ">>>>>>>>> " + TimeUtil.formatNow()), Util.NIL_UUID);
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_TYPE)) {
            tag.putByte(TAG_TYPE, (byte) GenType.LARGE.ordinal());
        }

        GenType craterType = GenType.values()[tag.getByte(TAG_TYPE)];

        player.sendMessage(new TextComponent(
                ChatFormatting.GOLD + "Spawned Type = " +
                        ChatFormatting.YELLOW + craterType
        ), Util.NIL_UUID);
        player.sendMessage(new TextComponent(
                ChatFormatting.GOLD + "BiomeCategory = " +
                        ChatFormatting.YELLOW + Biome.getBiomeCategory(level.getBiome(pos))
        ), Util.NIL_UUID);
        player.sendMessage(new TextComponent(
                ChatFormatting.GOLD + "BlockPos = " +
                        ChatFormatting.YELLOW + "[X = " + pos.getX() + " Y = " + pos.getY() + " Z = " + pos.getZ() + "]"
        ), Util.NIL_UUID);

        // localize some const values
        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();
        int radius = 16 * OilStructureGenerator.MAX_CHUNK_RADIUS;
        // StructurePiece 要求的 Box
        BlockPos min = new BlockPos(pos.getX() - radius, minBuildHeight, pos.getZ() - radius);
        Box box = new Box(min, min.offset(2 * radius, maxBuildHeight - minBuildHeight, 2 * radius));
        player.sendMessage(new TextComponent(
                ChatFormatting.GOLD + "Structure " +
                        ChatFormatting.YELLOW + "Creating " +
                        ChatFormatting.GOLD + TimeUtil.formatNow()
        ), Util.NIL_UUID);
        // Structure
        OilStructure structure = OilStructureGenerator.createTotalStructure(craterType, level.random, player.getBlockX(), player.getBlockZ(), minBuildHeight, maxBuildHeight, box);
        // Placer
        if (structure != null) {
            player.sendMessage(new TextComponent(
                    ChatFormatting.GOLD + "Structure " +
                            ChatFormatting.YELLOW + "Placing " +
                            ChatFormatting.GOLD + TimeUtil.formatNow()
            ), Util.NIL_UUID);
            final OilPlacer placer = new OilPlacer(level, structure.pieces, box.getBB());
            placer.place();
            player.sendMessage(new TextComponent(ChatFormatting.GREEN + ">>>>>>>>> " + TimeUtil.formatNow()), Util.NIL_UUID);
        } else {
            player.sendMessage(new TextComponent("None!"), Util.NIL_UUID);
        }
    }
}
