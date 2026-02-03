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
    // 缓存反射字段，避免重复查找
    private static Field defaultResourcePacksField = null;

    /**
     * 注册动态资源包（需要在模型注册前调用）
     */
    public static void registerDynamicResourcePack() {
        if (resourcePackRegistered)
            return;

        try {
            // 先扫描 config 目录中的资源文件
            DynamicResourcePack.scanConfigResources();

            // 获取或缓存反射字段
            if (defaultResourcePacksField == null) {
                // SRG 名: field_110449_ao = defaultResourcePacks
                defaultResourcePacksField = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                        Minecraft.class, "defaultResourcePacks", "field_110449_ao");
            }

            @SuppressWarnings("unchecked")
            List<IResourcePack> packs = (List<IResourcePack>) defaultResourcePacksField.get(Minecraft.getMinecraft());

            // 添加到列表开头获得最高优先级
            packs.add(0, new DynamicResourcePack());

            resourcePackRegistered = true;
            BSTweaker.LOG.info("Dynamic resource pack registered at position 0 (highest priority)");
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to register dynamic resource pack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从 Mixin 中调用 - 在物品注册后立即注册模型
     * 模型位置使用武器的注册名（mujmajnkraftsbettersurvival:itembstweaker_xxx）
     */
    @SideOnly(Side.CLIENT)
    public static void registerModelsForItems(List<Item> items) {
        int count = 0;
        for (Item item : items) {
            JsonObject def = TweakerWeaponInjector.getDefinition(item);
            if (def == null)
                continue;

            // 使用物品的注册名作为模型位置
            // 物品注册名: mujmajnkraftsbettersurvival:itembstweaker_fieryingotnunchaku
            // 模型位置: mujmajnkraftsbettersurvival:itembstweaker_fieryingotnunchaku
            ResourceLocation regName = item.getRegistryName();
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(regName, "inventory"));

            BSTweaker.LOG.info("Registered model: " + regName);
            count++;
        }

        BSTweaker.LOG.info("Registered " + count + " models for custom weapons.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onModelRegistry(ModelRegistryEvent event) {
        Map<Item, JsonObject> itemMap = TweakerWeaponInjector.getItemDefinitionMap();
        BSTweaker.LOG.info("onModelRegistry: itemDefinitionMap size = " + itemMap.size());

        if (itemMap.isEmpty()) {
            BSTweaker.LOG.info("itemDefinitionMap is empty - models will be registered via Mixin");
            return;
        }

        int count = 0;
        for (Map.Entry<Item, JsonObject> entry : itemMap.entrySet()) {
            Item item = entry.getKey();

            // 使用物品的注册名作为模型位置
            ResourceLocation regName = item.getRegistryName();
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(regName, "inventory"));

            BSTweaker.LOG.info("Registered model: " + regName);
            count++;
        }

        BSTweaker.LOG.info("Registered " + count + " models.");
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
