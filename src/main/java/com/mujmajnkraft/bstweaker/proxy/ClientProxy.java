package com.mujmajnkraft.bstweaker.proxy;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.config.ConfigLoader;
import com.mujmajnkraft.bstweaker.config.WeaponDefinition;
import com.mujmajnkraft.bstweaker.init.TweakerItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;

/**
 * 客户端代理 - 处理模型和渲染注册
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        super.preInit();
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
        for (Item item : TweakerItems.getRegisteredItems()) {
            registerItemRender(item);
        }
        BSTweaker.LOG.info("Registered renders for " + TweakerItems.getRegisteredItems().size() + " tweaked weapons.");
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
     * 在 PreInit 阶段注册模型（用于自定义纹理路径）
     * 需要在 ModelRegistryEvent 中调用
     */
    public static void registerModels() {
        for (Item item : TweakerItems.getRegisteredItems()) {
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
