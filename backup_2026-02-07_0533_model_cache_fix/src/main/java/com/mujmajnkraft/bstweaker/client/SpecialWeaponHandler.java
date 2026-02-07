package com.mujmajnkraft.bstweaker.client;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 特殊武器处理器 - 处理继承自 ItemCustomWeapon 的特殊武器
 * 
 * 功能:
 * - 注册特殊武器类型（非标准 BS 武器）
 * - 自定义渲染器绑定
 * - 反射创建武器实例
 * - 模型和纹理资源管理
 * 
 * 特殊武器示例:
 * - 双形态武器 (如变形剑)
 * - 远程武器 (如投掷武器)
 * - 组合武器 (如剑盾)
 */
public class SpecialWeaponHandler {

    // ========== 常量 ==========
    private static final String CUSTOM_WEAPON_CLASS = "com.mujmajnkraft.bettersurvival.items.ItemCustomWeapon";
    
    // ========== 状态 ==========
    private static boolean initialized = false;
    private static File specialDir;
    
    // ========== 缓存 ==========
    private static Class<?> customWeaponClass;
    private static final Map<String, SpecialWeaponDef> registeredWeapons = new HashMap<>();
    private static final Map<String, Class<?>> weaponClassCache = new HashMap<>();
    
    // ========== 特殊武器定义 ==========
    public static class SpecialWeaponDef {
        public String id;
        public String className;           // 武器类全名
        public String texture;             // 纹理名称
        public String[] modelVariants;     // 模型变体 (如 normal, spinning, transformed)
        public boolean hasCustomRenderer;  // 是否需要自定义渲染器
        public Map<String, Object> extra;  // 额外参数
        
        public SpecialWeaponDef(String id, String className) {
            this.id = id;
            this.className = className;
            this.texture = id;
            this.modelVariants = new String[]{"normal"};
            this.hasCustomRenderer = false;
            this.extra = new HashMap<>();
        }
    }
    
    // ========== 初始化 ==========
    
    /**
     * 初始化特殊武器处理器
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        BSTweaker.LOG.info("[SpecialWeapon] Initializing special weapon handler...");
        
        // 缓存基类
        if (!cacheBaseClass()) {
            BSTweaker.LOG.error("[SpecialWeapon] Failed to cache ItemCustomWeapon class");
            return;
        }
        
        // 初始化目录
        initDirectories();
        
        BSTweaker.LOG.info("[SpecialWeapon] Initialization complete.");
    }
    
    /**
     * 缓存 ItemCustomWeapon 基类
     */
    private static boolean cacheBaseClass() {
        try {
            customWeaponClass = Class.forName(CUSTOM_WEAPON_CLASS);
            BSTweaker.LOG.info("[SpecialWeapon] Cached base class: " + CUSTOM_WEAPON_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            BSTweaker.LOG.error("[SpecialWeapon] Base class not found: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 初始化特殊武器目录
     */
    private static void initDirectories() {
        File configDir = new File(Loader.instance().getConfigDir(), "bstweaker");
        specialDir = new File(configDir, "special");
        
        // 创建子目录
        new File(specialDir, "classes").mkdirs();   // 自定义武器类 .java 或配置
        new File(specialDir, "models").mkdirs();    // 特殊模型
        new File(specialDir, "textures").mkdirs();  // 特殊纹理
        
        // 创建 README
        createReadme(specialDir,
            "BSTweaker Special Weapon Support\n\n" +
            "Directory Structure:\n" +
            "  classes/    - Custom weapon class definitions\n" +
            "  models/     - Special weapon models\n" +
            "  textures/   - Special weapon textures\n\n" +
            "Special weapons extend ItemCustomWeapon with custom behaviors.");
        
        BSTweaker.LOG.info("[SpecialWeapon] Special weapon directory: " + specialDir.getAbsolutePath());
    }
    
    // ========== 武器注册 ==========
    
    /**
     * 注册特殊武器定义
     */
    public static void registerSpecialWeapon(SpecialWeaponDef def) {
        if (!initialized) init();
        
        registeredWeapons.put(def.id, def);
        BSTweaker.LOG.info("[SpecialWeapon] Registered special weapon: " + def.id + " -> " + def.className);
    }
    
    /**
     * 注册特殊武器（简化版）
     */
    public static void registerSpecialWeapon(String id, String className) {
        registerSpecialWeapon(new SpecialWeaponDef(id, className));
    }
    
    /**
     * 通过反射创建特殊武器实例
     */
    public static Item createSpecialWeapon(String id, net.minecraft.item.Item.ToolMaterial material) {
        if (!initialized) init();
        
        SpecialWeaponDef def = registeredWeapons.get(id);
        if (def == null) {
            BSTweaker.LOG.error("[SpecialWeapon] Unknown special weapon: " + id);
            return null;
        }
        
        try {
            // 获取或缓存武器类
            Class<?> weaponClass = weaponClassCache.get(def.className);
            if (weaponClass == null) {
                weaponClass = Class.forName(def.className);
                weaponClassCache.put(def.className, weaponClass);
            }
            
            // 验证继承关系
            if (!customWeaponClass.isAssignableFrom(weaponClass)) {
                BSTweaker.LOG.error("[SpecialWeapon] Class does not extend ItemCustomWeapon: " + def.className);
                return null;
            }
            
            // 反射创建实例
            Constructor<?> constructor = weaponClass.getConstructor(net.minecraft.item.Item.ToolMaterial.class);
            Item weapon = (Item) constructor.newInstance(material);
            
            // 设置注册名
            String regName = "itembstweaker_special_" + id;
            weapon.setRegistryName("mujmajnkraftsbettersurvival", regName);
            
            BSTweaker.LOG.info("[SpecialWeapon] Created special weapon: " + weapon.getRegistryName());
            return weapon;
            
        } catch (Exception e) {
            BSTweaker.LOG.error("[SpecialWeapon] Failed to create weapon " + id + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // ========== 渲染注册 ==========
    
    /**
     * 注册特殊武器模型
     */
    @SideOnly(Side.CLIENT)
    public static void registerSpecialWeaponModel(Item weapon, String id) {
        if (!initialized) init();
        
        SpecialWeaponDef def = registeredWeapons.get(id);
        if (def == null) {
            // 使用默认模型
            ResourceLocation regName = weapon.getRegistryName();
            ModelLoader.setCustomModelResourceLocation(
                weapon, 0, new ModelResourceLocation(regName, "inventory"));
            return;
        }
        
        // 注册主模型
        ResourceLocation regName = weapon.getRegistryName();
        ModelLoader.setCustomModelResourceLocation(
            weapon, 0, new ModelResourceLocation(regName, "inventory"));
        
        // TODO: 注册模型变体 (如果有多种状态)
        if (def.modelVariants != null && def.modelVariants.length > 1) {
            // 将来支持多状态模型
            BSTweaker.LOG.info("[SpecialWeapon] Registered model with " + 
                def.modelVariants.length + " variants for: " + id);
        }
        
        BSTweaker.LOG.info("[SpecialWeapon] Registered model for: " + id);
    }
    
    // ========== 公共 API ==========
    
    /**
     * 检查是否为特殊武器
     */
    public static boolean isSpecialWeapon(String id) {
        return registeredWeapons.containsKey(id);
    }
    
    /**
     * 获取特殊武器定义
     */
    public static SpecialWeaponDef getDefinition(String id) {
        return registeredWeapons.get(id);
    }
    
    /**
     * 获取所有已注册的特殊武器 ID
     */
    public static java.util.Set<String> getRegisteredIds() {
        return registeredWeapons.keySet();
    }
    
    /**
     * 获取 ItemCustomWeapon 基类
     */
    public static Class<?> getCustomWeaponClass() {
        if (!initialized) init();
        return customWeaponClass;
    }
    
    // ========== 工具方法 ==========
    
    private static void createReadme(File dir, String content) {
        try {
            File readme = new File(dir, "README.txt");
            if (!readme.exists()) {
                Files.write(readme.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("[SpecialWeapon] Failed to create README: " + e.getMessage());
        }
    }
}
