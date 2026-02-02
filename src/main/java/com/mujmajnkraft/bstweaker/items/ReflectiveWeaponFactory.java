package com.mujmajnkraft.bstweaker.items;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.mujmajnkraft.bettersurvival.items.*;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.config.WeaponDefinition;

import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 反射武器工厂
 * 直接实例化 BetterSurvival 原武器类，通过反射修改属性
 * 这样可以保证附魔兼容性（精确类型检查不会失败）
 */
public class ReflectiveWeaponFactory {

    /** 武器类型到原类的映射 */
    private static final Map<String, Class<? extends Item>> WEAPON_TYPE_MAP = new HashMap<>();

    static {
        WEAPON_TYPE_MAP.put("nunchaku", ItemNunchaku.class);
        WEAPON_TYPE_MAP.put("hammer", ItemHammer.class);
        WEAPON_TYPE_MAP.put("dagger", ItemDagger.class);
        WEAPON_TYPE_MAP.put("battleaxe", ItemBattleAxe.class);
        WEAPON_TYPE_MAP.put("spear", ItemSpear.class);
    }

    /** ItemCustomWeapon 中的 attackDamage 字段 */
    private static Field attackDamageField;
    /** ItemCustomWeapon 中的 attackSpeed 字段 */
    private static Field attackSpeedField;
    /** IForgeRegistryEntry.Impl 中的 registryName 字段 */
    private static Field registryNameField;

    static {
        try {
            // 获取 ItemCustomWeapon 的私有字段
            attackDamageField = ItemCustomWeapon.class.getDeclaredField("attackDamage");
            attackDamageField.setAccessible(true);

            attackSpeedField = ItemCustomWeapon.class.getDeclaredField("attackSpeed");
            attackSpeedField.setAccessible(true);

            // 获取 registryName 字段用于清除
            registryNameField = IForgeRegistryEntry.Impl.class.getDeclaredField("registryName");
            registryNameField.setAccessible(true);

        } catch (NoSuchFieldException e) {
            BSTweaker.LOG.error("Failed to find required fields for reflection", e);
        }
    }

    /**
     * 使用反射创建武器实例
     * 
     * @param def      武器定义
     * @param material 工具材质
     * @return 创建的武器物品，失败返回 null
     */
    public static Item createWeapon(WeaponDefinition def, ToolMaterial material) {
        Class<? extends Item> weaponClass = WEAPON_TYPE_MAP.get(def.type.toLowerCase());
        if (weaponClass == null) {
            BSTweaker.LOG.warn("Unknown weapon type: " + def.type);
            return null;
        }

        try {
            // 1. 反射调用构造函数 (ToolMaterial)
            Constructor<? extends Item> constructor = weaponClass.getConstructor(ToolMaterial.class);
            Item weapon = constructor.newInstance(material);

            // 2. 清除原有的 registryName（父类构造函数可能已设置）
            if (registryNameField != null) {
                registryNameField.set(weapon, null);
            }

            // 3. 设置新的注册名和翻译键
            weapon.setRegistryName(Reference.MOD_ID, def.id);
            weapon.setTranslationKey(Reference.MOD_ID + "." + def.id);

            // 4. 反射修改 attackDamage 和 attackSpeed
            float customDamage = (3.0F + material.getAttackDamage()) * def.damageModifier;
            double customSpeed = -2.4 * def.speedModifier;

            if (attackDamageField != null) {
                attackDamageField.setFloat(weapon, customDamage);
            }
            if (attackSpeedField != null) {
                attackSpeedField.setDouble(weapon, customSpeed);
            }

            BSTweaker.LOG.info("Created weapon via reflection: " + def.id +
                    " (class=" + weaponClass.getSimpleName() +
                    ", damage=" + customDamage +
                    ", speed=" + customSpeed + ")");

            return weapon;

        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to create weapon via reflection: " + def.id, e);
            return null;
        }
    }

    /**
     * 检查是否支持指定的武器类型
     */
    public static boolean isSupportedType(String type) {
        return WEAPON_TYPE_MAP.containsKey(type.toLowerCase());
    }
}
