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
            "Enable auto-conversion of BetterSurvival models to BSTweaker format.",
            "Converts *_normal.json and *_spinning.json files, backs up originals."
    })
    @Config.LangKey("bstweaker.config.enableModelAutoConvert")
    public static boolean enableModelAutoConvert = true;

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
