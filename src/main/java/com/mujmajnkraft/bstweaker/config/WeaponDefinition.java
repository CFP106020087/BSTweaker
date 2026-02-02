package com.mujmajnkraft.bstweaker.config;

/**
 * 武器定义，从 JSON 配置中解析
 */
public class WeaponDefinition {
    /** 武器唯一ID (用于注册名和翻译键) */
    public String id;

    /** 武器类型: nunchaku, hammer, dagger, battleaxe, spear */
    public String type;

    /** 显示名称 (用于语言文件) */
    public String displayName;

    /** 材质定义 */
    public MaterialDefinition material;

    /** 伤害倍率 (相对于同材质剑) */
    public float damageModifier = 1.0f;

    /** 攻速倍率 (相对于剑) */
    public float speedModifier = 1.0f;

    /** 纹理路径 (如 "bstweaker:items/blazing_iron_nunchaku") */
    public String texture;

    public WeaponDefinition() {
    }

    /**
     * 验证定义是否有效
     */
    public boolean isValid() {
        if (id == null || id.isEmpty())
            return false;
        if (type == null || type.isEmpty())
            return false;
        if (material == null || material.name == null)
            return false;

        // 验证武器类型
        switch (type.toLowerCase()) {
            case "nunchaku":
            case "hammer":
            case "dagger":
            case "battleaxe":
            case "spear":
                return true;
            default:
                return false;
        }
    }
}
