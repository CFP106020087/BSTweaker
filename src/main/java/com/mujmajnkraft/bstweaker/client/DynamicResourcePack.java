package com.mujmajnkraft.bstweaker.client;

import com.google.common.collect.ImmutableSet;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic resource pack - loads textures, models, mcmeta, lang from config dir.
 */
public class DynamicResourcePack implements IResourcePack {

    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    private static final Map<String, String> dynamicModels = new HashMap<>();
    private static final Map<String, File> configTextures = new HashMap<>();
    private static final Map<String, File> configModels = new HashMap<>();
    private static final Map<String, File> configLangs = new HashMap<>();
    private static final Map<String, File> configMcmeta = new HashMap<>();

    private static File configDir;
    private static File resourcesDir;
    private static boolean initialized = false;

    // Singleton instance
    private static DynamicResourcePack INSTANCE;

    /** Get singleton instance */
    public static DynamicResourcePack getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DynamicResourcePack();
        }
        return INSTANCE;
    }

    /**
     * Called by ASM from Minecraft.refreshResources before reloadResources.
     * Injects DynamicResourcePack at index 0 for highest priority.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void insertPack(java.util.List resourcePacks) {
        // Ensure resources are scanned
        scanConfigResources();

        // Check if already present
        for (Object pack : resourcePacks) {
            if (pack instanceof DynamicResourcePack) {
                return; // Already present
            }
        }

        // Insert at index 0 for highest priority
        resourcePacks.add(0, getInstance());
        BSTweaker.LOG.info(
                "[ASM] Injected DynamicResourcePack into resource list (index 0, size=" + resourcePacks.size() + ")");
    }

    /** Lazy initialization to avoid early class loading issues */
    private static void ensureInit() {
        if (!initialized) {
            configDir = new File(net.minecraftforge.fml.common.Loader.instance().getConfigDir(), "bstweaker");
            resourcesDir = new File(net.minecraftforge.fml.common.Loader.instance().getConfigDir().getParentFile(),
                    "resources/" + BS_NAMESPACE);
            initialized = true;
        }
    }

    /** Rescan config resources for hot-reload. */
    public static void rescan() {
        dynamicModels.clear();
        configTextures.clear();
        configModels.clear();
        configLangs.clear();
        configMcmeta.clear();
        scanConfigResources();
    }

    /** Get texture locations for cache invalidation. */
    public static java.util.Set<net.minecraft.util.ResourceLocation> getTextureLocations() {
        java.util.Set<net.minecraft.util.ResourceLocation> locations = new java.util.HashSet<>();
        for (String name : configTextures.keySet()) {
            locations.add(new net.minecraft.util.ResourceLocation(Reference.MOD_ID, "textures/items/" + name + ".png"));
        }
        return locations;
    }

    /**
     * Get texture File by name (for hot reload - bypasses cache).
     * Supports lookup with or without 'item' prefix.
     */
    public static java.io.File getTextureFile(String textureName) {
        // Try exact match first
        File file = configTextures.get(textureName);
        if (file != null)
            return file;

        // Try with 'item' prefix if not present
        if (!textureName.startsWith("item")) {
            file = configTextures.get("item" + textureName);
            if (file != null)
                return file;
        }

        // Try without 'item' prefix if present
        if (textureName.startsWith("item")) {
            file = configTextures.get(textureName.substring(4));
            if (file != null)
                return file;
        }

        return null;
    }

    /** Get all texture names. */
    public static java.util.Set<String> getTextureNames() {
        return configTextures.keySet();
    }

    /** Scan config and resources directories for resources. */
    public static void scanConfigResources() {
        ensureInit(); // 延迟初始化
        // 扫描 config/bstweaker 目录（用户放置的源资源）
        File cfgTexturesDir = new File(configDir, "textures");
        File cfgModelsDir = new File(configDir, "models");
        File cfgLangDir = new File(configDir, "lang");

        // 扫描 resources/<namespace> 目录（生成的资源，热重载的核心）
        File resTexturesDir = new File(resourcesDir, "textures/items");
        File resModelsDir = new File(resourcesDir, "models/item");
        File resLangDir = new File(resourcesDir, "lang");

        // 创建 config 目录结构（首次运行）
        if (!cfgTexturesDir.exists()) {
            cfgTexturesDir.mkdirs();
            createReadme(cfgTexturesDir,
                    "Place your weapon texture .png files here.\nOptionally add .png.mcmeta files for animations.");
        }
        if (!cfgModelsDir.exists()) {
            cfgModelsDir.mkdirs();
            createReadme(cfgModelsDir,
                    "Place custom model .json files here (optional).\nIf not provided, a default handheld model will be generated.");
        }
        if (!cfgLangDir.exists()) {
            cfgLangDir.mkdirs();
            createReadme(cfgLangDir,
                    "Place language .lang files here (e.g., en_us.lang, zh_cn.lang).\nFormat: item.bstweaker.weapon_id.name=Display Name");
        }

        // 扫描 config textures
        scanDirectory(cfgTexturesDir, ".png", configTextures);
        scanMcmeta(cfgTexturesDir);

        // 扫描 resources textures（优先级更高）
        scanDirectory(resTexturesDir, ".png", configTextures);
        scanMcmeta(resTexturesDir);

        // 扫描 config models
        scanDirectory(cfgModelsDir, ".json", configModels);

        // 扫描 resources models（优先级更高）
        scanDirectory(resModelsDir, ".json", configModels);
        BSTweaker.LOG
                .info("Scanned " + configModels.size() + " models from config+resources: " + configModels.keySet());

        // 扫描 config lang
        scanDirectory(cfgLangDir, ".lang", configLangs);

        // 扫描 resources lang（优先级更高）
        scanDirectory(resLangDir, ".lang", configLangs);
    }

    /** Helper: scan directory and add files to map. */
    private static void scanDirectory(File dir, String extension, Map<String, File> map) {
        if (dir.exists() && dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(extension)) {
                    String name = file.getName().replace(extension, "");
                    map.put(name, file);
                }
            }
        }
    }

    /** Helper: scan mcmeta files. Supports both .png.mcmeta and .mcmeta naming. */
    private static void scanMcmeta(File dir) {
        if (dir.exists() && dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".mcmeta")) {
                    String fileName = file.getName();
                    String name;
                    if (fileName.endsWith(".png.mcmeta")) {
                        name = fileName.replace(".png.mcmeta", "");
                    } else {
                        name = fileName.replace(".mcmeta", "");
                    }
                    configMcmeta.put(name, file);

                }
            }
        }
    }
    
    /** Create README file explaining directory purpose. */
    private static void createReadme(File dir, String content) {
        try {
            File readme = new File(dir, "README.txt");
            if (!readme.exists()) {
                java.io.FileWriter writer = new java.io.FileWriter(readme);
                writer.write(content);
                writer.close();
            }
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to create README: " + e.getMessage());
        }
    }

    /** Register dynamic model. */
    public static void registerModel(String textureName) {
        // Use custom model from config directory if exists
        if (configModels.containsKey(textureName)) {
            try {
                String content = new String(Files.readAllBytes(configModels.get(textureName).toPath()),
                        StandardCharsets.UTF_8);
                String modelPath = "assets/" + BS_NAMESPACE + "/models/item/" + textureName + ".json";
                dynamicModels.put(modelPath, content);
                return;
            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to load config model: " + e.getMessage());
            }
        }

        // Otherwise generate default model
        String modelPath = "assets/" + BS_NAMESPACE + "/models/item/" + textureName + ".json";
        String modelContent = generateModelJson(textureName);
        dynamicModels.put(modelPath, modelContent);
    }
    
    /** Generate model JSON content. */
    private static String generateModelJson(String textureName) {
        return "{\n" +
               "  \"parent\": \"item/handheld\",\n" +
               "  \"textures\": {\n" +
                "    \"layer0\": \"" + BS_NAMESPACE + ":items/" + textureName + "\"\n" +
               "  }\n" +
               "}";
    }
    
    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        String namespace = location.getNamespace();
        String path = location.getPath();

        // Debug logging
        if (path.contains("model") || path.contains("texture")) {

        }

        // Check if namespace is supported (bstweaker or mujmajnkraftsbettersurvival)
        if (!Reference.MOD_ID.equals(namespace) && !BS_NAMESPACE.equals(namespace)) {
            throw new FileNotFoundException("Resource not found: " + location);
        }

        // Check dynamically generated models
        String fullPath = "assets/" + namespace + "/" + path;
        String content = dynamicModels.get(fullPath);
        if (content != null) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        // Check config models - supports models/ and models/item/ formats
        if (path.endsWith(".json") && (path.startsWith("models/") || path.startsWith("models/item/"))) {
            String name;
            if (path.startsWith("models/item/")) {
                name = path.replace("models/item/", "").replace(".json", "");
            } else {
                name = path.replace("models/", "").replace(".json", "");
            }
            // Direct match
            if (configModels.containsKey(name)) {
                return new FileInputStream(configModels.get(name));
            }
            // Fallback: strip "bstweaker_" prefix if present
            if (name.contains("bstweaker_")) {
                String stripped = name.replace("bstweaker_", "");
                if (configModels.containsKey(stripped)) {
                    return new FileInputStream(configModels.get(stripped));
                }
            }

            // Auto-generate model if we have a matching texture
            if (configTextures.containsKey(name)) {
                String modelJson = generateModelJson(name);
                BSTweaker.LOG.info("[DRP] Auto-generated model for: " + name);
                return new ByteArrayInputStream(modelJson.getBytes(StandardCharsets.UTF_8));
            }
        }

        // Check config textures
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");
            if (configTextures.containsKey(name)) {
                return new FileInputStream(configTextures.get(name));
            }
            // Fallback: check file system
            File textureFile = new File(configDir, "textures/" + name + ".png");
            if (textureFile.exists()) {
                configTextures.put(name, textureFile);
                return new FileInputStream(textureFile);
            }
        }

        // Check config mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            if (configMcmeta.containsKey(name)) {
                return new FileInputStream(configMcmeta.get(name));
            }
            // Fallback: strip bstweaker_ prefix (registry name -> config name)
            if (name.contains("bstweaker_")) {
                String stripped = name.replace("bstweaker_", "");
                if (configMcmeta.containsKey(stripped)) {
                    return new FileInputStream(configMcmeta.get(stripped));
                }
            }
            // Fallback: check file system
            File mcmetaFile = new File(configDir, "textures/" + name + ".png.mcmeta");
            if (mcmetaFile.exists()) {
                configMcmeta.put(name, mcmetaFile);
                return new FileInputStream(mcmetaFile);
            }
        }

        // Check config lang files
        if (path.startsWith("lang/") && path.endsWith(".lang")) {
            String name = path.replace("lang/", "").replace(".lang", "");
            if (configLangs.containsKey(name)) {
                return new FileInputStream(configLangs.get(name));
            }
        }

        throw new FileNotFoundException("Resource not found: " + location);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        String namespace = location.getNamespace();
        String path = location.getPath();

        // Check config lang files - serve from both namespaces
        if (path.startsWith("lang/") && path.endsWith(".lang")) {
            String name = path.replace("lang/", "").replace(".lang", "");
            if (configLangs.containsKey(name)) {
                return true;
            }
        }

        // Check if namespace is supported for other resources
        if (!Reference.MOD_ID.equals(namespace) && !BS_NAMESPACE.equals(namespace)) {
            return false;
        }

        // Check dynamic models
        String fullPath = "assets/" + namespace + "/" + path;
        if (dynamicModels.containsKey(fullPath)) {
            return true;
        }

        // Check config models - supports models/xxx.json and models/item/xxx.json
        if (path.endsWith(".json") && (path.startsWith("models/") || path.startsWith("models/item/"))) {
            String name;
            if (path.startsWith("models/item/")) {
                name = path.replace("models/item/", "").replace(".json", "");
            } else {
                name = path.replace("models/", "").replace(".json", "");
            }

            if (configModels.containsKey(name)) {
                return true;
            }
            // Fallback: strip "bstweaker_" prefix if present (supports itembstweaker_xxx ->
            // itemxxx mapping)
            if (name.contains("bstweaker_")) {
                String stripped = name.replace("bstweaker_", "");
                if (configModels.containsKey(stripped)) {
                    return true;
                }
            }

            // Auto-generate model if we have a matching texture
            if (configTextures.containsKey(name)) {
                return true;
            }
        }

        // Check config textures
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");

            // Direct match
            if (configTextures.containsKey(name)) {
                return true;
            }

            // Fallback: check file system directly
            File textureFile = new File(configDir, "textures/" + name + ".png");
            if (textureFile.exists()) {
                configTextures.put(name, textureFile);
                return true;
            }
        }

        // Check config mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            if (configMcmeta.containsKey(name)) {
                return true;
            }
            // Fallback: strip bstweaker_ prefix (registry name -> config name)
            if (name.contains("bstweaker_")) {
                String stripped = name.replace("bstweaker_", "");
                if (configMcmeta.containsKey(stripped)) {
                    return true;
                }
            }
            // Fallback: check file system directly
            File mcmetaFile = new File(configDir, "textures/" + name + ".png.mcmeta");
            if (mcmetaFile.exists()) {
                configMcmeta.put(name, mcmetaFile);
                return true;
            }
        }

        // Lang files already checked at the beginning of this method

        return false;
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of(Reference.MOD_ID, BS_NAMESPACE);
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return null;
    }

    @Override
    public String getPackName() {
        return Reference.MOD_ID + "_dynamic";
    }
}
