package com.mujmajnkraft.bstweaker.client;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.config.WeaponDefinition;
import com.mujmajnkraft.bstweaker.init.TweakerItems;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Client event handler - model registration
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        BSTweaker.LOG.info("Registering models for tweaked weapons...");

        for (Item item : TweakerItems.getRegisteredItems()) {
            ResourceLocation registryName = item.getRegistryName();

            // Get definition from mapping
            WeaponDefinition def = TweakerItems.getDefinition(item);
            String texturePath = (def != null) ? def.texture : null;

            // Register model
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(registryName, "inventory"));

            BSTweaker.LOG.debug("Registered model for: " + registryName);
        }

        BSTweaker.LOG.info("Model registration complete for " + TweakerItems.getRegisteredItems().size() + " items.");
    }
}
