package com.mujmajnkraft.bstweaker.client;

import com.google.gson.JsonObject;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Client event handler - model registration with automatic texture redirection
 * 
 * 功能:
 * 1. 自动纹理重定向 - 模型指向 bstweaker 命名空间
 * 2. 动态模型生成 - 运行时生成模型 JSON，用户只需放纹理 PNG
 * 3. 支持 .mcmeta 动画 - 用户把 .mcmeta 放在纹理同目录即可
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public class ClientEventHandler {

    private static boolean resourcePackRegistered = false;

    /**
     * 注册动态资源包（需要在模型注册前调用）
     */
    public static void registerDynamicResourcePack() {
        if (resourcePackRegistered)
            return;

        try {
            // 先扫描 config 目录中的资源文件
            DynamicResourcePack.scanConfigResources();

            // 通过反射添加资源包到 Minecraft 的资源包列表
            Field field = Minecraft.class.getDeclaredField("defaultResourcePacks");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<IResourcePack> packs = (List<IResourcePack>) field.get(Minecraft.getMinecraft());
            packs.add(new DynamicResourcePack());
            resourcePackRegistered = true;
            BSTweaker.LOG.info("Dynamic resource pack registered.");
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to register dynamic resource pack: " + e.getMessage());
        }
    }

    /**
     * 从 Mixin 中调用 - 在物品注册后立即注册模型
     * 这个方法必须在正确的时机调用（物品注册事件中）
     */
    @SideOnly(Side.CLIENT)
    public static void registerModelsForItems(List<Item> items) {
        // 确保动态资源包已注册
        registerDynamicResourcePack();

        int count = 0;
        for (Item item : items) {
            JsonObject def = TweakerWeaponInjector.getDefinition(item);
            if (def == null)
                continue;

            // 获取纹理名称
            String textureName = getTextureName(def);

            // 注册动态模型
            DynamicResourcePack.registerModel(textureName);

            // 使用 bstweaker 命名空间注册模型
            ResourceLocation modelLocation = new ResourceLocation(Reference.MOD_ID, textureName);
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(modelLocation, "inventory"));

            BSTweaker.LOG.info("Registered model via Mixin: " + item.getRegistryName() + " -> " + modelLocation);
            count++;
        }

        BSTweaker.LOG.info("Registered " + count + " models for custom weapons.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onModelRegistry(ModelRegistryEvent event) {
        // 注册动态资源包
        registerDynamicResourcePack();

        Map<Item, JsonObject> itemMap = TweakerWeaponInjector.getItemDefinitionMap();
        BSTweaker.LOG.info("onModelRegistry: itemDefinitionMap size = " + itemMap.size());

        // 如果 map 不为空，也注册模型（作为备份）
        if (itemMap.isEmpty()) {
            BSTweaker.LOG.info("itemDefinitionMap is empty in onModelRegistry - models will be registered via Mixin");
            return;
        }

        int count = 0;

        for (Map.Entry<Item, JsonObject> entry : itemMap.entrySet()) {
            Item item = entry.getKey();
            JsonObject def = entry.getValue();

            // 获取纹理名称
            String textureName = getTextureName(def);

            // 注册动态模型（运行时生成）
            DynamicResourcePack.registerModel(textureName);

            // 使用 bstweaker 命名空间注册模型
            ResourceLocation modelLocation = new ResourceLocation(Reference.MOD_ID, textureName);
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(modelLocation, "inventory"));

            BSTweaker.LOG.info("Registered model via event: " + item.getRegistryName() + " -> " + modelLocation);
            count++;
        }

        BSTweaker.LOG.info("Registered " + count + " dynamic models (bstweaker namespace).");
    }

    /**
     * 从武器定义中获取纹理名称
     * 优先级: texture 字段 > id 字段
     */
    private static String getTextureName(JsonObject def) {
        // 优先使用 texture 字段
        if (def.has("texture")) {
            String texture = def.get("texture").getAsString();
            // 如果包含命名空间，提取纹理名
            if (texture.contains(":")) {
                texture = texture.substring(texture.indexOf(":") + 1);
            }
            // 如果包含路径，提取文件名
            if (texture.contains("/")) {
                texture = texture.substring(texture.lastIndexOf("/") + 1);
            }
            return texture;
        }

        // 否则使用 id 字段
        if (def.has("id")) {
            return def.get("id").getAsString();
        }

        return "missing";
    }
}
