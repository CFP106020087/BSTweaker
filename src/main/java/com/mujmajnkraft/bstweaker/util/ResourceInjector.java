package com.mujmajnkraft.bstweaker.util;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import net.minecraftforge.fml.common.Loader;

import java.io.File;

/**
 * Resource injector - delegates to DynamicResourcePack for hot-reload.
 */
public class ResourceInjector {

    private static File configDir;

    /** Initialize resource directory. */
    public static void init() {
        File mcDir = Loader.instance().getConfigDir().getParentFile();
        configDir = new File(mcDir, "config/" + Reference.MOD_ID);
        BSTweaker.LOG.info("Config dir: " + configDir.getAbsolutePath());
    }

    /** Refresh resources - rescan config directory. */
    public static void injectResources() {
        if (configDir == null) {
            init();
        }
        com.mujmajnkraft.bstweaker.client.DynamicResourcePack.rescan();
    }

    /** Translate namespace references in model content. */
    public static String translateModelContent(String content) {
        return content
                .replace(Reference.MOD_ID + ":items/", "mujmajnkraftsbettersurvival:items/itembstweaker_")
                .replace(Reference.MOD_ID + ":item/", "mujmajnkraftsbettersurvival:item/itembstweaker_");
    }
}
