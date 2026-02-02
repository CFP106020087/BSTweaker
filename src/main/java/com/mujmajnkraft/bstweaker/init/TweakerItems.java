package com.mujmajnkraft.bstweaker.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.config.ConfigLoader;
import com.mujmajnkraft.bstweaker.config.MaterialDefinition;
import com.mujmajnkraft.bstweaker.config.WeaponDefinition;
import com.mujmajnkraft.bstweaker.items.ReflectiveWeaponFactory;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 动态物品注册管理器
 */
public class TweakerItems {

    /** 已注册的物品列表 */
    private static List<Item> registeredItems = new ArrayList<>();

    /** 物品到武器定义的映射 (用于获取纹理路径等) */
    private static Map<Item, WeaponDefinition> itemDefinitionMap = new HashMap<>();

    /** 材质名称到 ToolMaterial 的映射 */
    private static Map<String, ToolMaterial> materialCache = new HashMap<>();

    /**
     * 从配置加载并创建物品
     */
    public static void loadFromConfig() {
        registeredItems.clear();
        itemDefinitionMap.clear();
        materialCache.clear();

        List<WeaponDefinition> definitions = ConfigLoader.getLoadedWeapons();

        for (WeaponDefinition def : definitions) {
            try {
                // 获取或创建材质
                ToolMaterial material = getOrCreateMaterial(def.material);

                // 创建武器物品
                Item item = createWeaponItem(def, material);
                if (item != null) {
                    registeredItems.add(item);
                    itemDefinitionMap.put(item, def);
                    BSTweaker.LOG.info("Created weapon item: " + def.id);
                }
            } catch (Exception e) {
                BSTweaker.LOG.error("Failed to create weapon: " + def.id, e);
            }
        }

        BSTweaker.LOG.info("Created " + registeredItems.size() + " tweaked weapons.");
    }

    /**
     * 获取或创建 ToolMaterial
     */
    private static ToolMaterial getOrCreateMaterial(MaterialDefinition matDef) {
        String key = matDef.name.toUpperCase();

        if (materialCache.containsKey(key)) {
            return materialCache.get(key);
        }

        // 使用 EnumHelper 创建新材质
        ToolMaterial material = EnumHelper.addToolMaterial(
                "BSTWEAKER_" + key,
                matDef.harvestLevel,
                matDef.durability,
                matDef.efficiency,
                matDef.damage,
                matDef.enchantability);

        // 设置修复物品
        if (matDef.repairItem != null && !matDef.repairItem.isEmpty()) {
            Item repairItem = Item.getByNameOrId(matDef.repairItem);
            if (repairItem != null) {
                material.setRepairItem(new ItemStack(repairItem));
            }
        }

        materialCache.put(key, material);
        return material;
    }

    /**
     * 根据类型创建武器物品 (使用反射实例化原武器类)
     */
    private static Item createWeaponItem(WeaponDefinition def, ToolMaterial material) {
        return ReflectiveWeaponFactory.createWeapon(def, material);
    }

    /**
     * Forge 物品注册事件
     */
    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        if (registeredItems.isEmpty()) {
            loadFromConfig();
        }

        event.getRegistry().registerAll(registeredItems.toArray(new Item[0]));
        BSTweaker.LOG.info("Registered " + registeredItems.size() + " tweaked weapons to Forge registry.");
    }

    /**
     * 获取已注册的物品列表
     */
    public static List<Item> getRegisteredItems() {
        return registeredItems;
    }

    /**
     * 获取物品对应的武器定义
     */
    public static WeaponDefinition getDefinition(Item item) {
        return itemDefinitionMap.get(item);
    }
}
