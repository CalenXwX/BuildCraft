package buildcraft.core.tile;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.core.BCCoreBlocks;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;

public class TilePowerConsumerTester extends TileBC_Neptune implements IMjReceiver, IDebuggable {

    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(this);
    private long lastReceived;
    private long totalReceived;

    public TilePowerConsumerTester() {
        super(BCCoreBlocks.powerTesterTile.get());
        caps.addProvider(mjCaps);
    }

    @Override
//    public void readFromNBT(CompoundNBT nbt)
    public void load(BlockState state, CompoundNBT nbt) {
//        super.readFromNBT(nbt);
        super.load(state, nbt);
        lastReceived = nbt.getLong("last");
        totalReceived = nbt.getLong("total");
    }

    @Override
//    public CompoundNBT writeToNBT(CompoundNBT nbt)
    public CompoundNBT save(CompoundNBT nbt) {
//        nbt = super.writeToNBT(nbt);
        super.save(nbt);
        nbt.putLong("last", lastReceived);
        nbt.putLong("total", totalReceived);
        return nbt;
    }

    // IMjReceiver

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return 100000 * MjAPI.MJ;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        if (!simulate) {
            lastReceived = microJoules;
            totalReceived += microJoules;
        }
        return 0;
    }

    // IDebuggable

    @Override
//    public void getDebugInfo(List<String> left, List<String> right, Direction side)
    public void getDebugInfo(List<ITextComponent> left, List<ITextComponent> right, Direction side) {
//        left.add("");
        left.add(new StringTextComponent(""));
//        left.add("Last received = " + LocaleUtil.localizeMj(lastReceived));
        left.add(new StringTextComponent("Last received = ").append(LocaleUtil.localizeMjComponent(lastReceived)));
//        left.add("Total received = " + LocaleUtil.localizeMj(totalReceived));
        left.add(new StringTextComponent("Total received = ").append(LocaleUtil.localizeMjComponent(totalReceived)));
    }
}
