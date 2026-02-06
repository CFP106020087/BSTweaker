package com.mujmajnkraft.bstweaker.core;

/**
 * 武器类型枚举
 * Weapon Type Enumeration
 * 
 * 定义所有支持的武器类型及其显示名称
 * Defines all supported weapon types and their display names
 */
public enum WeaponType {

    DAGGER("dagger", "匕首", "Dagger"),
    HAMMER("hammer", "战锤", "Hammer"),
    SPEAR("spear", "长矛", "Spear"),
    BATTLEAXE("battleaxe", "战斧", "Battle Axe"),
    NUNCHAKU("nunchaku", "双截棍", "Nunchaku");

    private final String id;
    private final String nameZh;
    private final String nameEn;

    WeaponType(String id, String nameZh, String nameEn) {
        this.id = id;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
    }

    public String getId() {
        return id;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    /**
     * 从 ID 获取武器类型
     * Get weapon type from ID
     */
    public static WeaponType fromId(String id) {
        if (id == null) return null;
        String lowerId = id.toLowerCase();
        for (WeaponType type : values()) {
            if (type.id.equals(lowerId)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 检查类型是否支持
     * Check if type is supported
     */
    public static boolean isSupported(String id) {
        return fromId(id) != null;
    }
}
