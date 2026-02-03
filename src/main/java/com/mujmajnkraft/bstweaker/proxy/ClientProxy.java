package com.mujmajnkraft.bstweaker.proxy;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;

/**
 * 客户端代理 - 处理模型和渲染注册
 */
public class ClientProxy extends CommonProxy {

    private static final ResourceLocation ALWAYS_PROPERTY = new ResourceLocation("bstweaker", "always");

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
        registerAlwaysPredicate();
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

    /**
     * 注册 bstweaker:always predicate - 始终返回 1.0
     * 这允许普通纹理也通过 override 加载，实现热重载
     */
    private void registerAlwaysPredicate() {
        int count = 0;
        for (Item item : TweakerWeaponInjector.getItemDefinitionMap().keySet()) {
            item.addPropertyOverride(ALWAYS_PROPERTY, (stack, world, entity) -> 1.0f);
            count++;
        }
        BSTweaker.LOG.info("Registered 'always' predicate for " + count + " weapons for hot-reload support.");
    }
}
