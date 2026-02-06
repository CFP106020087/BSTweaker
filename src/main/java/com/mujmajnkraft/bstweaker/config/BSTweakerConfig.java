package com.mujmajnkraft.bstweaker.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.mujmajnkraft.bstweaker.Reference;

@Config(modid = Reference.MOD_ID, name = Reference.MOD_ID + "/config")
@Config.LangKey("bstweaker.config.title")
public class BSTweakerConfig {

    @Config.Comment({
            "Blacklist of weapon IDs to skip model auto-generation",
            "Weapons in this list will NOT have models auto-generated",
            "模型生成黑名单，列表中的武器ID不会自动生成模型",
            "Example: [\"my_custom_sword\", \"special_dagger\"]"
    })
    @Config.LangKey("bstweaker.config.modelBlacklist")
    public static String[] modelBlacklist = new String[] {};

    @Config.Comment({
            "Auto-generate tooltips.json entries for new weapons",
            "自动为新武器生成 tooltips.json 条目"
    })
    @Config.LangKey("bstweaker.config.autoGenerateTooltips")
    public static boolean autoGenerateTooltips = true;

    @Mod.EventBusSubscriber(modid = Reference.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Reference.MOD_ID)) {
                ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
