package buildcraft.datagen.factory;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryItems;
import buildcraft.factory.loot.LootConditionSpreading;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

public class FactoryBlockLoot extends BlockLoot
{
    @Override
    protected void addTables()
    {
        dropSelf(BCFactoryBlocks.autoWorkbenchItems.get());
        dropSelf(BCFactoryBlocks.miningWell.get());
        dropSelf(BCFactoryBlocks.pump.get());
        dropSelf(BCFactoryBlocks.floodGate.get());
        dropSelf(BCFactoryBlocks.tank.get());
        dropSelf(BCFactoryBlocks.chute.get());
        dropSelf(BCFactoryBlocks.distiller.get());
        dropSelf(BCFactoryBlocks.heatExchange.get());
//        add(BCFactoryBlocks.TUBE.get(), LootTable.lootTable());
        add(BCFactoryBlocks.waterGel.get(),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1))
                                        .add(LootItem.lootTableItem(BCFactoryItems.gelledWater.get()))
                        )
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1))
                                        .add(LootItem.lootTableItem(BCFactoryItems.gelledWater.get())
                                                .when(LootConditionSpreading.builder())
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0, 1)))
                                        )
                        )
        );
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