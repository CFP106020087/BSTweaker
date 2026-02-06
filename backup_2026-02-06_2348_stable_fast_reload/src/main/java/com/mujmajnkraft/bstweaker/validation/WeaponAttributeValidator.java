package com.mujmajnkraft.bstweaker.validation;

import com.google.gson.JsonObject;
import com.mujmajnkraft.bstweaker.validation.ConfigValidationErrors.Level;
import com.mujmajnkraft.bstweaker.validation.ConfigValidationErrors.Source;

import java.util.*;

/**
 * Weapon Attribute Validator
 * 
 * Features:
 * - Defines valid attributes for each weapon type
 * - Validates weapons.json attributes
 * - Marks required/optional attributes
 */
public class WeaponAttributeValidator {

    // ========== Attribute Definition ==========

    /**
     * Attribute definition
     */
    public static class AttributeDef {
        public final String name; // Attribute name
        public final Class<?> type; // Expected type (String, Number, Boolean, JsonObject)
        public final boolean required; // Is required
        public final Object defaultValue; // Default value
        public final String descKey; // Localization key for description

        public AttributeDef(String name, Class<?> type, boolean required,
                Object defaultValue, String descKey) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
            this.descKey = descKey;
        }
    }

    // ========== Common Attributes (shared by all weapons) ==========
    private static final List<AttributeDef> COMMON_ATTRIBUTES = Arrays.asList(
            new AttributeDef("id", String.class, true, null, "bstweaker.attr.id"),
            new AttributeDef("type", String.class, true, null, "bstweaker.attr.type"),
            new AttributeDef("material", JsonObject.class, true, null, "bstweaker.attr.material"),
            new AttributeDef("texture", String.class, false, null, "bstweaker.attr.texture"),
            new AttributeDef("damageModifier", Number.class, false, 0.0, "bstweaker.attr.damageModifier"),
            new AttributeDef("speedModifier", Number.class, false, 0.0, "bstweaker.attr.speedModifier"));

    // ========== Material Attributes ==========
    private static final List<AttributeDef> MATERIAL_ATTRIBUTES = Arrays.asList(
            new AttributeDef("name", String.class, true, null, "bstweaker.attr.name"),
            new AttributeDef("harvestLevel", Number.class, true, null, "bstweaker.attr.harvestLevel"),
            new AttributeDef("durability", Number.class, true, null, "bstweaker.attr.durability"),
            new AttributeDef("efficiency", Number.class, false, 6.0, "bstweaker.attr.efficiency"),
            new AttributeDef("damage", Number.class, true, null, "bstweaker.attr.damage"),
            new AttributeDef("enchantability", Number.class, false, 10, "bstweaker.attr.enchantability"));

    // ========== Sub Attributes (optional for all) ==========
    private static final List<AttributeDef> SUB_ATTRIBUTES = Arrays.asList(
            new AttributeDef("knockback", Number.class, false, 0.0, "bstweaker.attr.knockback"),
            new AttributeDef("critMultiplier", Number.class, false, 1.5, "bstweaker.attr.critMultiplier"),
            new AttributeDef("lifesteal", Number.class, false, 0.0, "bstweaker.attr.lifesteal"),
            new AttributeDef("armorPenetration", Number.class, false, 0.0, "bstweaker.attr.armorPenetration"),
            new AttributeDef("attackReach", Number.class, false, 0.0, "bstweaker.attr.attackReach"));

    // ========== Exclusive Attributes (by weapon type) ==========

    private static final Map<String, List<AttributeDef>> EXCLUSIVE_ATTRIBUTES = new HashMap<>();

    static {
        // Dagger
        EXCLUSIVE_ATTRIBUTES.put("dagger", Arrays.asList(
                new AttributeDef("backstabMultiplier", Number.class, false, 2.0, "bstweaker.attr.backstabMultiplier"),
                new AttributeDef("throwDamage", Number.class, false, 0.0, "bstweaker.attr.throwDamage"),
                new AttributeDef("throwSpeed", Number.class, false, 1.0, "bstweaker.attr.throwSpeed")));

        // Hammer
        EXCLUSIVE_ATTRIBUTES.put("hammer", Arrays.asList(
                new AttributeDef("groundPoundRadius", Number.class, false, 3.0, "bstweaker.attr.groundPoundRadius"),
                new AttributeDef("groundPoundDamage", Number.class, false, 0.0, "bstweaker.attr.groundPoundDamage"),
                new AttributeDef("stunDuration", Number.class, false, 20, "bstweaker.attr.stunDuration"),
                new AttributeDef("armorBreakChance", Number.class, false, 0.0, "bstweaker.attr.armorBreakChance")));

        // Spear
        EXCLUSIVE_ATTRIBUTES.put("spear", Arrays.asList(
                new AttributeDef("throwRange", Number.class, false, 30.0, "bstweaker.attr.throwRange"),
                new AttributeDef("throwDamage", Number.class, false, 0.0, "bstweaker.attr.throwDamage"),
                new AttributeDef("pierceCount", Number.class, false, 1, "bstweaker.attr.pierceCount"),
                new AttributeDef("reachBonus", Number.class, false, 1.0, "bstweaker.attr.reachBonus")));

        // Battleaxe
        EXCLUSIVE_ATTRIBUTES.put("battleaxe", Arrays.asList(
                new AttributeDef("disarmChance", Number.class, false, 0.0, "bstweaker.attr.disarmChance"),
                new AttributeDef("shieldBreakBonus", Number.class, false, 0.0, "bstweaker.attr.shieldBreakBonus"),
                new AttributeDef("armorDamageBonus", Number.class, false, 0.0, "bstweaker.attr.armorDamageBonus")));

        // Nunchaku
        EXCLUSIVE_ATTRIBUTES.put("nunchaku", Arrays.asList(
                new AttributeDef("spinSpeedBase", Number.class, false, 1.0, "bstweaker.attr.spinSpeedBase"),
                new AttributeDef("spinSpeedMax", Number.class, false, 3.0, "bstweaker.attr.spinSpeedMax"),
                new AttributeDef("comboDamageBonus", Number.class, false, 0.5, "bstweaker.attr.comboDamageBonus"),
                new AttributeDef("comboResetTime", Number.class, false, 40, "bstweaker.attr.comboResetTime")));
    }

    // ========== Valid Weapon Types ==========
    private static final Set<String> VALID_WEAPON_TYPES = new HashSet<>(Arrays.asList(
            "dagger", "hammer", "spear", "battleaxe", "nunchaku"));

    // ========== Validation Methods ==========

    /**
     * Validate a single weapon definition
     * 
     * @return true if valid (false = skip registration)
     */
    public static boolean validateWeapon(JsonObject weaponDef) {
        ConfigValidationErrors errors = ConfigValidationErrors.getInstance();
        String weaponId = weaponDef.has("id") ? weaponDef.get("id").getAsString() : "<unknown>";
        boolean valid = true;

        // 1. Validate common attributes
        for (AttributeDef attr : COMMON_ATTRIBUTES) {
            if (!validateAttribute(weaponDef, attr, weaponId, null)) {
                if (attr.required)
                    valid = false;
            }
        }

        // 2. Validate material attributes
        if (weaponDef.has("material") && weaponDef.get("material").isJsonObject()) {
            JsonObject material = weaponDef.getAsJsonObject("material");
            for (AttributeDef attr : MATERIAL_ATTRIBUTES) {
                if (!validateAttribute(material, attr, weaponId, "material")) {
                    if (attr.required)
                        valid = false;
                }
            }
        }

        // 3. Validate weapon type
        String type = weaponDef.has("type") ? weaponDef.get("type").getAsString().toLowerCase() : null;
        if (type != null && !VALID_WEAPON_TYPES.contains(type)) {
            errors.error(Source.WEAPONS, weaponId, "type",
                    "Unknown weapon type '" + type + "', valid types: " + VALID_WEAPON_TYPES);
            valid = false;
        }

        // 4. Validate sub attributes (optional, type check only)
        for (AttributeDef attr : SUB_ATTRIBUTES) {
            if (weaponDef.has(attr.name)) {
                validateAttribute(weaponDef, attr, weaponId, null);
            }
        }

        // 5. Validate exclusive attributes
        if (type != null && EXCLUSIVE_ATTRIBUTES.containsKey(type)) {
            List<AttributeDef> exclusiveAttrs = EXCLUSIVE_ATTRIBUTES.get(type);
            Set<String> validExclusiveNames = new HashSet<>();
            for (AttributeDef attr : exclusiveAttrs) {
                validExclusiveNames.add(attr.name);
                if (weaponDef.has(attr.name)) {
                    validateAttribute(weaponDef, attr, weaponId, null);
                }
            }

            // Check for invalid exclusive attributes
            checkInvalidExclusiveAttributes(weaponDef, type, validExclusiveNames, weaponId);
        }

        return valid;
    }

    /**
     * Validate a single attribute
     */
    private static boolean validateAttribute(JsonObject obj, AttributeDef attr,
            String weaponId, String parentField) {
        ConfigValidationErrors errors = ConfigValidationErrors.getInstance();
        String fieldPath = parentField != null ? parentField + "." + attr.name : attr.name;

        // Check if exists
        if (!obj.has(attr.name)) {
            if (attr.required) {
                errors.error(Source.WEAPONS, weaponId, fieldPath,
                        "Missing required field: " + attr.name);
                return false;
            }
            return true; // Optional attribute missing is OK
        }

        // Check type
        try {
            if (attr.type == String.class) {
                obj.get(attr.name).getAsString();
            } else if (attr.type == Number.class) {
                obj.get(attr.name).getAsNumber();
            } else if (attr.type == Boolean.class) {
                obj.get(attr.name).getAsBoolean();
            } else if (attr.type == JsonObject.class) {
                obj.get(attr.name).getAsJsonObject();
            }
        } catch (Exception e) {
            errors.error(Source.WEAPONS, weaponId, fieldPath,
                    "Type error, expected " + attr.type.getSimpleName());
            return false;
        }

        return true;
    }

    /**
     * Check for invalid exclusive attributes
     */
    private static void checkInvalidExclusiveAttributes(JsonObject weaponDef, String type,
            Set<String> validNames, String weaponId) {
        ConfigValidationErrors errors = ConfigValidationErrors.getInstance();

        // Collect exclusive attributes from other types (excluding shared ones)
        Set<String> invalidExclusiveNames = new HashSet<>();
        for (Map.Entry<String, List<AttributeDef>> entry : EXCLUSIVE_ATTRIBUTES.entrySet()) {
            if (!entry.getKey().equals(type)) {
                for (AttributeDef attr : entry.getValue()) {
                    // Only add if not valid for current type
                    if (!validNames.contains(attr.name)) {
                        invalidExclusiveNames.add(attr.name);
                    }
                }
            }
        }

        // Check for truly invalid exclusive attributes
        for (String name : invalidExclusiveNames) {
            if (weaponDef.has(name)) {
                errors.warning(Source.WEAPONS, weaponId, name,
                        "Attribute not valid for " + type + " type, ignored");
            }
        }
    }

    // ========== Public API ==========

    /**
     * Get all valid weapon types
     */
    public static Set<String> getValidWeaponTypes() {
        return Collections.unmodifiableSet(VALID_WEAPON_TYPES);
    }

    /**
     * Get exclusive attributes for a weapon type
     */
    public static List<AttributeDef> getExclusiveAttributes(String type) {
        return EXCLUSIVE_ATTRIBUTES.getOrDefault(type, Collections.emptyList());
    }

    /**
     * Get common attributes
     */
    public static List<AttributeDef> getCommonAttributes() {
        return Collections.unmodifiableList(COMMON_ATTRIBUTES);
    }

    /**
     * Get sub attributes
     */
    public static List<AttributeDef> getSubAttributes() {
        return Collections.unmodifiableList(SUB_ATTRIBUTES);
    }
}
