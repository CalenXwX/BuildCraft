package buildcraft.datagen.builders;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.energy.BCEnergyBlocks;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Set;

public class BuildersBlockLoot extends BlockLoot
{
    @Override
    protected void addTables()
    {
        dropSelf(BCBuildersBlocks.filler.get());
        dropSelf(BCBuildersBlocks.builder.get());
        dropSelf(BCBuildersBlocks.architect.get());
        dropSelf(BCBuildersBlocks.library.get());
        dropSelf(BCBuildersBlocks.replacer.get());
        dropSelf(BCBuildersBlocks.quarry.get());
    }

    // without these: IllegalStateException: Missing loottable 'minecraft:blocks/stone' for 'minecraft:stone'
    private final Set<Block> knownBlocks = new HashSet<>();

    @Override
    protected void add(Block block, LootTable.Builder builder)
    {
        super.add(block, builder);
        knownBlocks.add(block);
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return knownBlocks;
    }
}
