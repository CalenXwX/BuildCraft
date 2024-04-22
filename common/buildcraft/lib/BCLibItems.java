package buildcraft.lib;

import buildcraft.lib.item.ItemDebugger;
import buildcraft.lib.item.ItemGuide;
import buildcraft.lib.item.ItemGuideNote;
import buildcraft.lib.item.ItemPropertiesCreator;
import buildcraft.lib.registry.RegistrationHelper;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class BCLibItems {
    private static final RegistrationHelper HELPER = new RegistrationHelper(BCLib.MODID);

    public static RegistryObject<Item> guide;
    public static RegistryObject<Item> guideNote;
    public static RegistryObject<Item> debugger;

    private static boolean enableGuide, enableDebugger;

    public static void enableGuide() {
        enableGuide = true;
    }

    public static void enableDebugger() {
        enableDebugger = true;
    }

    public static boolean isGuideEnabled() {
        return enableGuide;
    }

    public static boolean isDebuggerEnabled() {
        return enableDebugger;
    }


    // Calen: should not static because guide/debugger enabling in <cinit> in Lib main class will call <cinit> here
    public static void fmlPreInit() {
        if (isGuideEnabled()) {
            guide = HELPER.addForcedItem("item.guide", ItemPropertiesCreator.common64(), ItemGuide::new);
            guideNote = HELPER.addForcedItem("item.guide.note", ItemPropertiesCreator.common64(), ItemGuideNote::new);
        }
        if (isDebuggerEnabled()) {
            debugger = HELPER.addForcedItem("item.debugger", ItemPropertiesCreator.common1(), ItemDebugger::new);
        }
    }
}
