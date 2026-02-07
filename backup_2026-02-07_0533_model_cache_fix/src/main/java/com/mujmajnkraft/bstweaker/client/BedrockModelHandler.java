package com.mujmajnkraft.bstweaker.client;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
// 模型管理和烘焙
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
// 动画元数据
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationFrame;
// 资源加载
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
// 模型加载（display属性）
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
/**
 * Bedrock 模型处理器 - 使用反射实现 GeckoLib 软依赖
 * 
 * 功能:
 * - 检测 GeckoLib 是否存在
 * - 扫描 config/bstweaker/bedrock/ 目录
 * - 加载 .geo.json 和 .animation.json 文件
 * - 通过反射调用 GeckoLib API
 * 
 * 软依赖模式: 如果没有 GeckoLib，所有功能优雅降级
 */
public class BedrockModelHandler {

    // ========== 常量 ==========
    private static final String GECKOLIB_MOD_ID = "geckolib3";
    private static final String GECKOLIB_CLASS = "software.bernie.geckolib3.GeckoLib";
    
    // ========== 状态缓存 ==========
    private static boolean initialized = false;
    private static boolean geckolibAvailable = false;
    private static File bedrockDir;
    
    // ========== 反射缓存 ==========
    private static Class<?> geckolibClass;
    private static Class<?> animatedGeoModelClass;
    private static Class<?> geoItemRendererClass;
    private static Class<?> iAnimatableClass;
    private static Class<?> animationFactoryClass;
    
    // ========== 资源缓存 ==========
    private static final Map<String, File> geoModels = new HashMap<>();      // name -> .geo.json
    private static final Map<String, File> animations = new HashMap<>();     // name -> .animation.json
    private static final Map<String, File> textures = new HashMap<>();       // name -> .png
    
    // ========== 初始化 ==========
    
    /**
     * 初始化 Bedrock 模型处理器
     * 检测 GeckoLib 是否可用，缓存反射类
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        // 1. 检测 GeckoLib 是否加载
        geckolibAvailable = Loader.isModLoaded(GECKOLIB_MOD_ID);
        
        if (!geckolibAvailable) {
            BSTweaker.LOG.info("[BedrockModel] GeckoLib not found - Bedrock model support disabled");
            return;
        }
        
        BSTweaker.LOG.info("[BedrockModel] GeckoLib detected! Initializing Bedrock model support...");
        
        // 2. 缓存反射类
        if (!cacheReflectionClasses()) {
            geckolibAvailable = false;
            BSTweaker.LOG.error("[BedrockModel] Failed to cache GeckoLib classes - disabling support");
            return;
        }
        
        // 3. 初始化目录
        initDirectories();
        
        // 4. 扫描资源
        scanBedrockResources();
        
        BSTweaker.LOG.info("[BedrockModel] Initialization complete. Found " + geoModels.size() + " models.");
    }
    
    /**
     * 缓存 GeckoLib 反射类
     */
    private static boolean cacheReflectionClasses() {
        try {
            geckolibClass = Class.forName(GECKOLIB_CLASS);
            animatedGeoModelClass = Class.forName("software.bernie.geckolib3.model.AnimatedGeoModel");
            geoItemRendererClass = Class.forName("software.bernie.geckolib3.renderers.geo.GeoItemRenderer");
            iAnimatableClass = Class.forName("software.bernie.geckolib3.core.IAnimatable");
            animationFactoryClass = Class.forName("software.bernie.geckolib3.core.manager.AnimationFactory");
            
            BSTweaker.LOG.info("[BedrockModel] GeckoLib classes cached successfully");
            return true;
            
        } catch (ClassNotFoundException e) {
            BSTweaker.LOG.error("[BedrockModel] GeckoLib class not found: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 初始化 Bedrock 资源目录
     */
    private static void initDirectories() {
        File configDir = new File(Loader.instance().getConfigDir(), "bstweaker");
        bedrockDir = new File(configDir, "bedrock");
        
        // 创建子目录
        new File(bedrockDir, "geo").mkdirs();
        new File(bedrockDir, "animations").mkdirs();
        new File(bedrockDir, "textures").mkdirs();
        
        // 创建 README
        createReadme(bedrockDir, 
            "BSTweaker Bedrock Model Support\n\n" +
            "Directory Structure:\n" +
            "  geo/         - Place .geo.json model files here\n" +
            "  animations/  - Place .animation.json files here\n" +
            "  textures/    - Place .png texture files here\n\n" +
            "Naming Convention:\n" +
            "  geo/<weapon_id>.geo.json\n" +
            "  animations/<weapon_id>.animation.json\n" +
            "  textures/<weapon_id>.png\n\n" +
            "Requires GeckoLib mod to be installed!");
        
        BSTweaker.LOG.info("[BedrockModel] Bedrock directory: " + bedrockDir.getAbsolutePath());
    }
    
    /**
     * 扫描 Bedrock 资源文件
     */
    public static void scanBedrockResources() {
        if (!geckolibAvailable) return;
        
        geoModels.clear();
        animations.clear();
        textures.clear();
        
        // 扫描 geo 模型
        File geoDir = new File(bedrockDir, "geo");
        if (geoDir.exists()) {
            File[] files = geoDir.listFiles((dir, name) -> name.endsWith(".geo.json"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".geo.json", "");
                    geoModels.put(name, file);
                    BSTweaker.LOG.info("[BedrockModel] Found geo model: " + name);
                }
            }
        }
        
        // 扫描动画
        File animDir = new File(bedrockDir, "animations");
        if (animDir.exists()) {
            File[] files = animDir.listFiles((dir, name) -> name.endsWith(".animation.json"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".animation.json", "");
                    animations.put(name, file);
                    BSTweaker.LOG.info("[BedrockModel] Found animation: " + name);
                }
            }
        }
        
        // 扫描纹理
        File texDir = new File(bedrockDir, "textures");
        if (texDir.exists()) {
            File[] files = texDir.listFiles((dir, name) -> name.endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".png", "");
                    textures.put(name, file);
                    BSTweaker.LOG.info("[BedrockModel] Found texture: " + name);
                }
            }
        }
    }
    
    // ========== 公共 API ==========
    
    /**
     * 检查 GeckoLib 是否可用
     */
    public static boolean isGeckolibAvailable() {
        if (!initialized) init();
        return geckolibAvailable;
    }
    
    /**
     * 检查指定武器是否有 Bedrock 模型
     */
    public static boolean hasBedrockModel(String weaponId) {
        if (!geckolibAvailable) return false;
        return geoModels.containsKey(weaponId);
    }
    
    /**
     * 获取 geo 模型文件
     */
    public static File getGeoModel(String weaponId) {
        return geoModels.get(weaponId);
    }
    
    /**
     * 获取动画文件
     */
    public static File getAnimation(String weaponId) {
        return animations.get(weaponId);
    }
    
    /**
     * 获取纹理文件
     */
    public static File getTexture(String weaponId) {
        return textures.get(weaponId);
    }
    
    /**
     * 获取所有已加载的 Bedrock 模型 ID
     */
    public static java.util.Set<String> getLoadedModelIds() {
        return geoModels.keySet();
    }
    
    // ========== 反射工具 ==========
    
    /**
     * 获取缓存的 GeckoLib 类
     */
    public static Class<?> getGeckolibClass() { return geckolibClass; }
    public static Class<?> getAnimatedGeoModelClass() { return animatedGeoModelClass; }
    public static Class<?> getGeoItemRendererClass() { return geoItemRendererClass; }
    public static Class<?> getIAnimatableClass() { return iAnimatableClass; }
    public static Class<?> getAnimationFactoryClass() { return animationFactoryClass; }
    
    // ========== 工具方法 ==========
    
    private static void createReadme(File dir, String content) {
        try {
            File readme = new File(dir, "README.txt");
            if (!readme.exists()) {
                Files.write(readme.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("[BedrockModel] Failed to create README: " + e.getMessage());
        }
    }
    
    /**
     * 读取文件内容
     */
    public static String readFileContent(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            BSTweaker.LOG.error("[BedrockModel] Failed to read file: " + file.getName());
            return null;
        }
    }
}
