package com.mujmajnkraft.bstweaker.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mujmajnkraft.bstweaker.effects.EffectEventHandler;
import com.mujmajnkraft.bstweaker.effects.WeaponEvent;
import com.mujmajnkraft.bstweaker.validation.ConfigValidationErrors;
import com.mujmajnkraft.bstweaker.validation.WeaponAttributeValidator;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.Loader;

/** Weapon injector - creates weapons from JSON config for Mixin use. */
public class TweakerWeaponInjector {

    /** Item to weapon definition map (for model registration). */
    private static Map<Item, JsonObject> itemDefinitionMap = new HashMap<>();

    /** Material cache. */
    private static Map<String, ToolMaterial> materialCache = new HashMap<>();

    /** Weapon type to class mapping. */
    private static final Map<String, String> WEAPON_CLASSES = new HashMap<>();

    static {
        WEAPON_CLASSES.put("hammer", "com.mujmajnkraft.bettersurvival.items.ItemHammer");
        WEAPON_CLASSES.put("spear", "com.mujmajnkraft.bettersurvival.items.ItemSpear");
        WEAPON_CLASSES.put("dagger", "com.mujmajnkraft.bettersurvival.items.ItemDagger");
        WEAPON_CLASSES.put("battleaxe", "com.mujmajnkraft.bettersurvival.items.ItemBattleAxe");
        WEAPON_CLASSES.put("nunchaku", "com.mujmajnkraft.bettersurvival.items.ItemNunchaku");
    }

    /** Create all custom weapons from config. */
    public static List<Item> createWeapons() {
        List<Item> weapons = new ArrayList<>();
        itemDefinitionMap.clear();

        // 清除旧的验证错误（支持热重载）
        ConfigValidationErrors.getInstance().clear();

        try {
            File configDir = new File(Loader.instance().getConfigDir(), "bstweaker");

            // Ensure config dir exists
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File weaponsFile = new File(configDir, "weapons.json");
            File tooltipsFile = new File(configDir, "tooltips.json");
            File scriptsFile = new File(configDir, "scripts.json");
            File apiFile = new File(configDir, "SCRIPT_API.md");

            // Copy default configs (only if not exist)
            copyDefaultConfig("weapons.json", weaponsFile);
            copyDefaultConfig("tooltips.json", tooltipsFile);
            copyDefaultConfig("scripts.json", scriptsFile);
            copyDefaultConfig("SCRIPT_API.md", apiFile);

            if (!weaponsFile.exists()) {
                System.out.println("[BSTweaker] No weapons.json found, skipping weapon injection");
                return weapons;
            }

            // === Phase 1: Parse weapons.json ===
            JsonParser parser = new JsonParser();
            JsonElement rootElement = parser.parse(new java.io.InputStreamReader(
                    new java.io.FileInputStream(weaponsFile), java.nio.charset.StandardCharsets.UTF_8));
            if (rootElement == null || !rootElement.isJsonObject()) {
                System.err.println("[BSTweaker] weapons.json is empty or invalid (parsed as: " + rootElement + ")");
                return weapons;
            }
            JsonObject root = rootElement.getAsJsonObject();

            if (!root.has("weapons")) {
                System.out.println("[BSTweaker] weapons.json has no 'weapons' array");
                return weapons;
            }
            JsonArray weaponDefs = root.getAsJsonArray("weapons");

            // === Phase 2: Load materials ===
            for (JsonElement elem : weaponDefs) {
                JsonObject weaponDef = elem.getAsJsonObject();
                if (weaponDef.has("material")) {
                    JsonObject matDef = weaponDef.getAsJsonObject("material");
                    loadMaterial(matDef);
                }
            }

            // === Phase 3: Auto-fill tooltips/scripts/textures ===
            // ResourceInjector reads weapons.json and fills missing entries
            try {
                ResourceInjector.injectResources();
            } catch (Exception e) {
                System.err.println("[BSTweaker] ResourceInjector failed (non-fatal): " + e.getMessage());
            }

            // === Phase 4: Reload tooltips/scripts (may have been auto-filled) ===
            Map<String, JsonObject> tooltipMap = new HashMap<>();
            if (tooltipsFile.exists()) {
                JsonElement tooltipsElement = parser.parse(new java.io.InputStreamReader(
                        new java.io.FileInputStream(tooltipsFile), java.nio.charset.StandardCharsets.UTF_8));
                if (tooltipsElement != null && tooltipsElement.isJsonObject()) {
                    JsonObject tooltipsRoot = tooltipsElement.getAsJsonObject();
                    if (tooltipsRoot.has("tooltips")) {
                        for (JsonElement elem : tooltipsRoot.getAsJsonArray("tooltips")) {
                            JsonObject t = elem.getAsJsonObject();
                            tooltipMap.put(t.get("id").getAsString(), t);
                        }
                    }
                } else {
                    System.out.println("[BSTweaker] tooltips.json is empty or invalid, skipping");
                }
            }

            Map<String, JsonArray> scriptMap = new HashMap<>();
            if (scriptsFile.exists()) {
                JsonElement scriptsElement = parser.parse(new java.io.InputStreamReader(
                        new java.io.FileInputStream(scriptsFile), java.nio.charset.StandardCharsets.UTF_8));
                if (scriptsElement != null && scriptsElement.isJsonObject()) {
                    JsonObject scriptsRoot = scriptsElement.getAsJsonObject();
                    if (scriptsRoot.has("scripts")) {
                        for (JsonElement elem : scriptsRoot.getAsJsonArray("scripts")) {
                            JsonObject s = elem.getAsJsonObject();
                            if (s.has("events")) {
                                scriptMap.put(s.get("id").getAsString(), s.getAsJsonArray("events"));
                            }
                        }
                    }
                } else {
                    System.out.println("[BSTweaker] scripts.json is empty or invalid, skipping");
                }
            }

            // === Phase 5: Create weapons with merged tooltip/script data ===
            for (JsonElement elem : weaponDefs) {
                JsonObject weaponDef = elem.getAsJsonObject();
                String id = weaponDef.has("id") ? weaponDef.get("id").getAsString() : "<unknown>";

                // === 验证武器配置 ===
                if (!WeaponAttributeValidator.validateWeapon(weaponDef)) {
                    System.err.println("[BSTweaker] Skipping weapon '" + id + "' due to validation errors");
                    continue; // 跳过注册
                }

                // Merge tooltip config
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

                    // Load events from scripts.json (matches id or material.name)
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
                        weaponDef.add("events", eventsArray);
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

            System.out.println("[BSTweaker] Created " + weapons.size() + " custom weapons");

        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to load weapons.json: " + e.getMessage());
            e.printStackTrace();
        }

        return weapons;
    }

    /** Load material from JSON definition and register it. */
    private static void loadMaterial(JsonObject matDef) {
        // Strip spaces from material name to prevent invalid ResourceLocations
        String name = matDef.has("name") ? matDef.get("name").getAsString().replaceAll("\\s+", "").toUpperCase()
                : "UNKNOWN";

        // Skip if already cached
        if (materialCache.containsKey(name)) {
            return;
        }

        int harvestLevel = matDef.has("harvestLevel") ? matDef.get("harvestLevel").getAsInt() : 2;
        int durability = matDef.has("durability") ? matDef.get("durability").getAsInt() : 500;
        float efficiency = matDef.has("efficiency") ? matDef.get("efficiency").getAsFloat() : 6.0f;
        float damage = matDef.has("damage") ? matDef.get("damage").getAsFloat() : 2.0f;
        int enchantability = matDef.has("enchantability") ? matDef.get("enchantability").getAsInt() : 14;

        ToolMaterial material = EnumHelper.addToolMaterial(name, harvestLevel, durability, efficiency, damage,
                enchantability);
        if (material != null) {
            materialCache.put(name, material);
            System.out.println("[BSTweaker] Registered material: " + name);
        }
    }

    /** Hot-reload configs (tooltips and scripts only, no new weapons). */
    public static void reloadConfigs() {
        System.out.println("[BSTweaker] Reloading configs...");

        try {
            File configDir = new File(Loader.instance().getConfigDir(), "bstweaker");
            File tooltipsFile = new File(configDir, "tooltips.json");
            File scriptsFile = new File(configDir, "scripts.json");
            JsonParser parser = new JsonParser();

            // Reload tooltips
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
                System.out.println("[BSTweaker] Reloaded " + tooltipMap.size() + " tooltip definitions");
            }

            // Reload scripts
            Map<String, JsonArray> scriptMap = new HashMap<>();
            if (scriptsFile.exists()) {
                JsonObject scriptsRoot = parser.parse(new java.io.InputStreamReader(
                        new java.io.FileInputStream(scriptsFile), java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject();
                if (scriptsRoot.has("scripts")) {
                    for (JsonElement elem : scriptsRoot.getAsJsonArray("scripts")) {
                        JsonObject s = elem.getAsJsonObject();
                        String scriptId = s.get("id").getAsString();
                        if (s.has("events")) {
                            scriptMap.put(scriptId, s.getAsJsonArray("events"));
                        }
                    }
                }
                System.out.println("[BSTweaker] Reloaded " + scriptMap.size() + " script definitions");
            }

            // Update existing weapons
            for (Map.Entry<Item, JsonObject> entry : itemDefinitionMap.entrySet()) {
                Item item = entry.getKey();
                JsonObject weaponDef = entry.getValue();
                String id = weaponDef.get("id").getAsString();

                // Update tooltip
                if (tooltipMap.containsKey(id)) {
                    JsonObject tooltip = tooltipMap.get(id);
                    if (tooltip.has("displayName"))
                        weaponDef.add("displayName", tooltip.get("displayName"));
                    if (tooltip.has("tooltip"))
                        weaponDef.add("tooltip", tooltip.get("tooltip"));
                }

                // Update scripts
                String materialName = weaponDef.has("material")
                        && weaponDef.getAsJsonObject("material").has("name")
                                ? weaponDef.getAsJsonObject("material").get("name").getAsString().toLowerCase()
                                : "";
                JsonArray eventsArray = null;

                if (scriptMap.containsKey(id)) {
                    eventsArray = scriptMap.get(id);
                } else if (!materialName.isEmpty() && scriptMap.containsKey(materialName)) {
                    eventsArray = scriptMap.get(materialName);
                }

                if (eventsArray != null) {
                    // Re-register events
                    weaponDef.add("events", eventsArray);
                    List<WeaponEvent> events = WeaponEvent.fromJsonArray(eventsArray);
                    if (!events.isEmpty()) {
                        EffectEventHandler.registerWeaponEffects(item, events);
                    }
                }
            }

            System.out.println("[BSTweaker] Config reload complete");

        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to reload configs: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /** Create single weapon. */
    private static Item createWeapon(JsonObject weaponDef) {
        try {
            String id = weaponDef.get("id").getAsString();
            String type = weaponDef.get("type").getAsString().toLowerCase();
            String rawMaterialName = weaponDef.get("material").getAsJsonObject().get("name").getAsString();
            String rawTexture = weaponDef.has("texture") ? weaponDef.get("texture").getAsString() : id;

            // Warn about spaces in name fields
            if (id.contains(" "))
                System.err.println(
                        "[BSTweaker] WARNING: id '" + id + "' contains spaces! Spaces break translation key matching.");
            if (rawTexture.contains(" "))
                System.err.println(
                        "[BSTweaker] WARNING: texture '" + rawTexture
                                + "' contains spaces! Spaces break registration.");
            if (rawMaterialName.contains(" "))
                System.err.println("[BSTweaker] WARNING: material.name '" + rawMaterialName
                        + "' contains spaces! Spaces break registration.");

            // Strip spaces from material name
            String materialName = rawMaterialName.replaceAll("\\s+", "").toUpperCase();

            // Get material
            ToolMaterial material = materialCache.get(materialName);
            if (material == null) {
                System.err.println("[BSTweaker] Unknown material: " + materialName + " for weapon " + id);
                return null;
            }

            // Get weapon class
            String className = WEAPON_CLASSES.get(type);
            if (className == null) {
                System.err.println("[BSTweaker] Unknown weapon type: " + type + " for weapon " + id);
                return null;
            }

            // Reflect create weapon - BS constructor auto-sets registry name
            // Format: mujmajnkraftsbettersurvival:item<material><type>
            // We keep this as-is since ResourceInjector uses the same formula.
            Class<?> weaponClass = Class.forName(className);
            Constructor<?> constructor = weaponClass.getConstructor(ToolMaterial.class);
            Item weapon = (Item) constructor.newInstance(material);

            // Override translation key to use weapon id
            // If id already ends with type (e.g. "emeraldexampledagger"), don't double it
            String translationKey = id.endsWith(type) ? id : id + type;
            weapon.setTranslationKey(translationKey);


            // Modify attack damage and speed
            if (weaponDef.has("damageModifier") || weaponDef.has("speedModifier")) {
                modifyWeaponStats(weapon, weaponDef);
            }

            System.out.println(
                    "[BSTweaker] Created weapon: " + weapon.getRegistryName() + " (type=" + type + ", material="
                            + materialName + ", texture=" + id + ", translationKey=" + translationKey + ")");
            return weapon;

        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to create weapon: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    /** Modify weapon attack attributes. */
    private static void modifyWeaponStats(Item weapon, JsonObject weaponDef) {
        try {
            // Get parent class ItemCustomWeapon fields
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

    /** Get weapon definition for item. */
    public static JsonObject getDefinition(Item item) {
        return itemDefinitionMap.get(item);
    }

    /** Get all created items. */
    public static Map<Item, JsonObject> getItemDefinitionMap() {
        return itemDefinitionMap;
    }

    /** Copy default config from JAR (UTF-8). */
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

            // UTF-8 encoding
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
