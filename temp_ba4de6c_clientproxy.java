package com.mujmajnkraft.bstweaker.proxy;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;

/**
 * 摰Ｘ蝡臭誨??- 憭?璅∪??葡?釣?? */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        super.preInit();
        // ?喲嚗韏??蝸?釣???皞?
        com.mujmajnkraft.bstweaker.client.ClientEventHandler.registerDynamicResourcePack();
    }

    @Override
    public void init() {
        super.init();
        registerItemRenders();
    }

    /**
     * 瘜典??拙?皜脫?璅∪?
     */
    private void registerItemRenders() {
        int count = 0;
        for (Item item : TweakerWeaponInjector.getItemDefinitionMap().keySet()) {
            registerItemRender(item);
            count++;
        }
        BSTweaker.LOG.info("Registered renders for " + count + " tweaked weapons.");
    }

    /**
     * 瘜典??葵?拙??葡?芋??     */
    private void registerItemRender(Item item) {
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
