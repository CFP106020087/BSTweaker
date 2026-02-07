package com.mujmajnkraft.bstweaker.core;

/**
 * BSTweaker 常量定义
 * BSTweaker Constants
 * 
 * 集中管理所有硬编码字符串
 * Centralized management of all hardcoded strings
 */
public final class BSTweakerConstants {

    private BSTweakerConstants() {
        // 防止实例化
        // Prevent instantiation
    }

    // ========== 命名空间 / Namespaces ==========
    
    /**
     * BetterSurvival 的 mod ID 和资源命名空间
     * BetterSurvival's mod ID and resource namespace
     */
    public static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    /**
     * BSTweaker 的 mod ID
     * BSTweaker's mod ID
     */
    public static final String BSTWEAKER_MODID = "bstweaker";

    // ========== 前缀 / Prefixes ==========

    /**
     * 自定义物品前缀 (不再使用前缀)
     * Custom item prefix (no longer using prefix)
     */
    public static final String ITEM_PREFIX = "";

    /**
     * 材质名称前缀（不再使用前缀）
     * Material name prefix (no longer using prefix)
     */
    public static final String MATERIAL_PREFIX = "";

    // ========== 目录 / Directories ==========

    /**
     * 配置目录名称
     * Config directory name
     */
    public static final String CONFIG_DIR = "bstweaker";

    /**
     * 模型子目录
     * Models subdirectory
     */
    public static final String MODELS_DIR = "models";

    /**
     * 纹理子目录
     * Textures subdirectory
     */
    public static final String TEXTURES_DIR = "textures";

    /**
     * 语言文件子目录
     * Lang files subdirectory
     */
    public static final String LANG_DIR = "lang";

    // ========== 配置文件 / Config Files ==========

    /**
     * 武器配置文件名
     * Weapons config filename
     */
    public static final String WEAPONS_CONFIG = "weapons.json";

    /**
     * 工具提示配置文件名
     * Tooltips config filename
     */
    public static final String TOOLTIPS_CONFIG = "tooltips.json";

    /**
     * 脚本配置文件名
     * Scripts config filename
     */
    public static final String SCRIPTS_CONFIG = "scripts.json";

    // ========== 资源路径 / Resource Paths ==========

    /**
     * JAR 内默认纹理路径
     * Default textures path in JAR
     */
    public static final String JAR_TEXTURES_PATH = "/assets/bstweaker/config/textures";

    /**
     * JAR 内默认配置路径
     * Default config path in JAR
     */
    public static final String JAR_CONFIG_PATH = "/assets/bstweaker/config/";

    // ========== 模型相关 / Model Related ==========

    /**
     * 默认父模型
     * Default parent model
     */
    public static final String DEFAULT_PARENT_MODEL = "item/handheld";

    /**
     * 占位纹理
     * Placeholder texture
     */
    public static final String PLACEHOLDER_TEXTURE = "minecraft:items/stick";

    /**
     * 自定义属性覆盖 predicate
     * Custom property override predicate
     */
    public static final String ALWAYS_PREDICATE = "bstweaker:always";
}
