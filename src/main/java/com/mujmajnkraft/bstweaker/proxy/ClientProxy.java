package com.mujmajnkraft.bstweaker.proxy;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;

/**
 * 客户端代理 - 处理模型和渲染注册
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        super.preInit();
        // 关键：在资源加载前注册动态资源包
        com.mujmajnkraft.bstweaker.client.ClientEventHandler.registerDynamicResourcePack();
    }

    @Override
    public void init() {
        super.init();
        registerItemRenders();
    }

    /**
     * 注册物品渲染模型
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
     * 注册单个物品的渲染模型
     */
    private void registerItemRender(Item item) {
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
