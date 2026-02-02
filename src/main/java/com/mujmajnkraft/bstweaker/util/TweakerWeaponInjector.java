package com.mujmajnkraft.bstweaker.util;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mujmajnkraft.bstweaker.effects.EffectEventHandler;
import com.mujmajnkraft.bstweaker.effects.WeaponEvent;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.Loader;

/**
 * 武器注入器 - 从 JSON 配置创建武器并供 Mixin 使用
 */
public class TweakerWeaponInjector {

    /** 物品到武器定义的映射 (用于模型注册) */
    private static Map<Item, JsonObject> itemDefinitionMap = new HashMap<>();

    /** 材质缓存 */
    private static Map<String, ToolMaterial> materialCache = new HashMap<>();

    /** 武器类型到类的映射 */
    private static final Map<String, String> WEAPON_CLASSES = new HashMap<>();

    static {
        WEAPON_CLASSES.put("hammer", "com.mujmajnkraft.bettersurvival.items.ItemHammer");
        WEAPON_CLASSES.put("spear", "com.mujmajnkraft.bettersurvival.items.ItemSpear");
        WEAPON_CLASSES.put("dagger", "com.mujmajnkraft.bettersurvival.items.ItemDagger");
        WEAPON_CLASSES.put("battleaxe", "com.mujmajnkraft.bettersurvival.items.ItemBattleAxe");
        WEAPON_CLASSES.put("nunchaku", "com.mujmajnkraft.bettersurvival.items.ItemNunchaku");
    }

    /**
     * 创建所有自定义武器
     */
    public static List<Item> createWeapons() {
        List<Item> weapons = new ArrayList<>();
        itemDefinitionMap.clear();

        try {
            File configDir = new File(Loader.instance().getConfigDir(), "bstweaker");

            // 确保配置目录存在
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File weaponsFile = new File(configDir, "weapons.json");
            File tooltipsFile = new File(configDir, "tooltips.json");
            File scriptsFile = new File(configDir, "scripts.json");
            File apiFile = new File(configDir, "SCRIPT_API.md");

            // 自动生成默认配置文件
            copyDefaultConfig("weapons.json", weaponsFile);
            copyDefaultConfig("tooltips.json", tooltipsFile);
            copyDefaultConfig("scripts.json", scriptsFile);
            copyDefaultConfig("SCRIPT_API.md", apiFile);

            if (!weaponsFile.exists()) {
                System.out.println("[BSTweaker] No weapons.json found, skipping weapon injection");
                return weapons;
            }

            JsonParser parser = new JsonParser();
            // 使用 UTF-8 编码读取
            JsonObject root = parser.parse(new java.io.InputStreamReader(
                    new java.io.FileInputStream(weaponsFile), java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();

            // 加载 tooltips 配置
            Map<String, JsonObject> tooltipMap = new HashMap<>();
            if (tooltipsFile.exists()) {
                JsonObject tooltipsRoot = parser.parse(new java.io.InputStreamReader(
                        new java.io.FileInputStream(tooltipsFile), java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject();
                if (tooltipsRoot.has("tooltips")) {
                    for (JsonElement elem : tooltipsRoot.getAsJsonArray("tooltips")) {
                        JsonObject t = elem.getAsJsonObject();
                        tooltipMap.put(t.get("id").getAsString(), t);
                    }
                }
            }

            // 加载 scripts 配置
            Map<String, JsonArray> scriptMap = new HashMap<>();
            if (scriptsFile.exists()) {
                JsonObject scriptsRoot = parser.parse(new java.io.InputStreamReader(
                        new java.io.FileInputStream(scriptsFile), java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject();
                if (scriptsRoot.has("scripts")) {
                    for (JsonElement elem : scriptsRoot.getAsJsonArray("scripts")) {
                        JsonObject s = elem.getAsJsonObject();
                        if (s.has("events")) {
                            scriptMap.put(s.get("id").getAsString(), s.getAsJsonArray("events"));
                        }
                    }
                }
            }

            // 加载武器定义
            if (root.has("weapons")) {
                JsonArray weaponDefs = root.getAsJsonArray("weapons");

                // 第一遍：从每个武器定义中加载材质
                for (JsonElement elem : weaponDefs) {
                    JsonObject weaponDef = elem.getAsJsonObject();
                    if (weaponDef.has("material")) {
                        JsonObject matDef = weaponDef.getAsJsonObject("material");
                        loadMaterial(matDef);
                    }
                }

                // 第二遍：创建武器
                for (JsonElement elem : weaponDefs) {
                    JsonObject weaponDef = elem.getAsJsonObject();
                    String id = weaponDef.get("id").getAsString();

                    // 合并 tooltip 配置
                    if (tooltipMap.containsKey(id)) {
                        JsonObject tooltip = tooltipMap.get(id);
                        if (tooltip.has("displayName"))
                            weaponDef.add("displayName", tooltip.get("displayName"));
                        if (tooltip.has("tooltip"))
                            weaponDef.add("tooltip", tooltip.get("tooltip"));
                    }

                    Item weapon = createWeapon(weaponDef);
                    if (weapon != null) {
                        weapons.add(weapon);
                        itemDefinitionMap.put(weapon, weaponDef);

                        // 从 scripts.json 加载事件 (同时匹配 id 和 material.name)
                        String materialName = weaponDef.has("material")
                                && weaponDef.getAsJsonObject("material").has("name")
                                        ? weaponDef.getAsJsonObject("material").get("name").getAsString().toLowerCase()
                                        : "";
                        JsonArray eventsArray = null;

                        if (scriptMap.containsKey(id)) {
                            eventsArray = scriptMap.get(id);
                            System.out.println("[BSTweaker] Found scripts by id: " + id);
                        } else if (!materialName.isEmpty() && scriptMap.containsKey(materialName)) {
                            eventsArray = scriptMap.get(materialName);
                            System.out.println("[BSTweaker] Found scripts by material: " + materialName);
                        }

                        if (eventsArray != null) {
                            System.out.println("[BSTweaker] Found " + eventsArray.size() + " events for weapon: " + id);
                            List<WeaponEvent> events = WeaponEvent.fromJsonArray(eventsArray);
                            if (!events.isEmpty()) {
                                EffectEventHandler.registerWeaponEffects(weapon, events);
                            }
                        } else {
                            System.out.println("[BSTweaker] No scripts found for weapon: " + id + " (material: "
                                    + materialName + ")");
                        }
                    }
                }
            }

            System.out.println("[BSTweaker] Created " + weapons.size() + " custom weapons");

        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to load weapons.json: " + e.getMessage());
            e.printStackTrace();
        }

        return weapons;
    }

    /**
     * 加载材质定义
     */
    private static void loadMaterial(JsonObject matDef) {
        try {
            String name = matDef.get("name").getAsString().toUpperCase();
            int harvestLevel = matDef.has("harvestLevel") ? matDef.get("harvestLevel").getAsInt() : 2;
            int durability = matDef.has("durability") ? matDef.get("durability").getAsInt() : 250;
            float efficiency = matDef.has("efficiency") ? matDef.get("efficiency").getAsFloat() : 6.0F;
            float damage = matDef.has("damage") ? matDef.get("damage").getAsFloat() : 2.0F;
            int enchantability = matDef.has("enchantability") ? matDef.get("enchantability").getAsInt() : 14;

            ToolMaterial material = EnumHelper.addToolMaterial(
                    "BSTWEAKER_" + name,
                    harvestLevel,
                    durability,
                    efficiency,
                    damage,
                    enchantability);

            // 设置修复物品
            if (matDef.has("repairItem")) {
                String repairItemId = matDef.get("repairItem").getAsString();
                Item repairItem = Item.getByNameOrId(repairItemId);
                if (repairItem != null) {
                    material.setRepairItem(new ItemStack(repairItem));
                }
            }

            materialCache.put(name, material);
            System.out.println("[BSTweaker] Loaded material: " + name);

        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to load material: " + e.getMessage());
        }
    }

    /**
     * 创建单个武器
     */
    private static Item createWeapon(JsonObject weaponDef) {
        try {
            String id = weaponDef.get("id").getAsString();
            String type = weaponDef.get("type").getAsString().toLowerCase();
            String materialName = weaponDef.get("material").getAsJsonObject().get("name").getAsString().toUpperCase();

            // 获取材质
            ToolMaterial material = materialCache.get(materialName);
            if (material == null) {
                System.err.println("[BSTweaker] Unknown material: " + materialName + " for weapon " + id);
                return null;
            }

            // 获取武器类
            String className = WEAPON_CLASSES.get(type);
            if (className == null) {
                System.err.println("[BSTweaker] Unknown weapon type: " + type + " for weapon " + id);
                return null;
            }

            // 反射创建武器实例 - BS 构造函数会自动设置注册名
            Class<?> weaponClass = Class.forName(className);
            Constructor<?> constructor = weaponClass.getConstructor(ToolMaterial.class);
            Item weapon = (Item) constructor.newInstance(material);

            // 不覆盖注册名，使用 BS 生成的原生注册名
            // 注册名格式: mujmajnkraftsbettersurvival:item<material><type>

            // 修改攻击伤害和速度
            if (weaponDef.has("damageModifier") || weaponDef.has("speedModifier")) {
                modifyWeaponStats(weapon, weaponDef);
            }

            System.out.println(
                    "[BSTweaker] Created weapon: " + weapon.getRegistryName() + " (type=" + type + ", material="
                            + materialName + ")");
            return weapon;

        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to create weapon: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 修改武器攻击属性
     */
    private static void modifyWeaponStats(Item weapon, JsonObject weaponDef) {
        try {
            // 尝试获取父类 ItemCustomWeapon 的字段
            Class<?> customWeaponClass = Class.forName("com.mujmajnkraft.bettersurvival.items.ItemCustomWeapon");

            if (weaponDef.has("damageModifier")) {
                float damageModifier = weaponDef.get("damageModifier").getAsFloat();
                Field attackDamageField = customWeaponClass.getDeclaredField("attackDamage");
                attackDamageField.setAccessible(true);
                float baseDamage = attackDamageField.getFloat(weapon);
                attackDamageField.setFloat(weapon, baseDamage + damageModifier);
            }

            if (weaponDef.has("speedModifier")) {
                double speedModifier = weaponDef.get("speedModifier").getAsDouble();
                Field attackSpeedField = customWeaponClass.getDeclaredField("attackSpeed");
                attackSpeedField.setAccessible(true);
                double baseSpeed = attackSpeedField.getDouble(weapon);
                attackSpeedField.setDouble(weapon, baseSpeed + speedModifier);
            }

        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to modify weapon stats: " + e.getMessage());
        }
    }

    /**
     * 获取物品对应的武器定义
     */
    public static JsonObject getDefinition(Item item) {
        return itemDefinitionMap.get(item);
    }

    /**
     * 获取所有已创建的物品
     */
    public static Map<Item, JsonObject> getItemDefinitionMap() {
        return itemDefinitionMap;
    }

    /**
     * 从 JAR 复制默认配置文件 (UTF-8 编码)
     */
    private static void copyDefaultConfig(String resourceName, File targetFile) {
        if (targetFile.exists())
            return;

        try {
            java.io.InputStream in = TweakerWeaponInjector.class.getResourceAsStream(
                    "/assets/bstweaker/config/" + resourceName);
            if (in == null) {
                System.out.println("[BSTweaker] Default config not found: " + resourceName);
                return;
            }

            // 使用 UTF-8 编码
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            java.io.BufferedWriter writer = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(
                            new java.io.FileOutputStream(targetFile), java.nio.charset.StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();

            System.out.println("[BSTweaker] Generated default config: " + targetFile.getName());
        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to copy default config: " + e.getMessage());
        }
    }
}
