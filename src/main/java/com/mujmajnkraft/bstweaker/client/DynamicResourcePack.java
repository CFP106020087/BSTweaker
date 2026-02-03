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
 * 动态资源包 - 从 config 目录加载纹理、模型、mcmeta、lang
 * 
 * 目录结构:
 * config/bstweaker/
 * ├── textures/
 * │ ├── void_blade.png <- 纹理
 * │ └── void_blade.png.mcmeta <- 动画元数据
 * ├── models/
 * │ └── void_blade.json <- 自定义模型 (可选，不存在则自动生成)
 * └── lang/
 * ├── en_us.lang <- 英文
 * └── zh_cn.lang <- 中文
 */
public class DynamicResourcePack implements IResourcePack {

    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    private static final Map<String, String> dynamicModels = new HashMap<>();
    private static final Map<String, File> configTextures = new HashMap<>();
    private static final Map<String, File> configModels = new HashMap<>();
    private static final Map<String, File> configLangs = new HashMap<>();
    private static final Map<String, File> configMcmeta = new HashMap<>();

    private static File configDir;

    static {
        configDir = new File(Loader.instance().getConfigDir(), "bstweaker");
    }

    /**
     * 重新扫描资源 - 用于热重载
     */
    public static void rescan() {
        dynamicModels.clear();
        configTextures.clear();
        configModels.clear();
        configLangs.clear();
        configMcmeta.clear();
        scanConfigResources();
        BSTweaker.LOG.info("DynamicResourcePack rescanned");
    }

    /**
     * 获取所有配置纹理的 ResourceLocation - 用于热重载时清除纹理缓存
     */
    public static java.util.Set<net.minecraft.util.ResourceLocation> getTextureLocations() {
        java.util.Set<net.minecraft.util.ResourceLocation> locations = new java.util.HashSet<>();
        for (String name : configTextures.keySet()) {
            // 添加 bstweaker 命名空间的纹理路径
            locations.add(new net.minecraft.util.ResourceLocation(Reference.MOD_ID, "textures/items/" + name + ".png"));
            // 也添加带前缀的版本
            if (!name.startsWith("itembstweaker_")) {
                locations.add(new net.minecraft.util.ResourceLocation(Reference.MOD_ID,
                        "textures/items/itembstweaker_" + name + ".png"));
            }
        }
        BSTweaker.LOG.info("DynamicResourcePack: " + locations.size() + " texture locations for cache invalidation");
        return locations;
    }

    /**
     * 扫描 config 目录中的资源文件
     */
    public static void scanConfigResources() {
        // 确保目录存在
        File texturesDir = new File(configDir, "textures");
        File modelsDir = new File(configDir, "models");
        File langDir = new File(configDir, "lang");

        if (!texturesDir.exists()) {
            texturesDir.mkdirs();
            createReadme(texturesDir,
                    "Place your weapon texture .png files here.\nOptionally add .png.mcmeta files for animations.");
        }
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
            createReadme(modelsDir,
                    "Place custom model .json files here (optional).\nIf not provided, a default handheld model will be generated.");
        }
        if (!langDir.exists()) {
            langDir.mkdirs();
            createReadme(langDir,
                    "Place language .lang files here (e.g., en_us.lang, zh_cn.lang).\nFormat: item.bstweaker.weapon_id.name=Display Name");
        }

        // 扫描纹理
        if (texturesDir.exists() && texturesDir.listFiles() != null) {
            for (File file : texturesDir.listFiles()) {
                if (file.getName().endsWith(".png")) {
                    String name = file.getName().replace(".png", "");
                    configTextures.put(name, file);
                    BSTweaker.LOG.info("Found config texture: " + name);
                } else if (file.getName().endsWith(".mcmeta")) {
                    String name = file.getName().replace(".png.mcmeta", "");
                    configMcmeta.put(name, file);
                    BSTweaker.LOG.info("Found config mcmeta: " + name);
                }
            }
        }

        // 扫描模型
        if (modelsDir.exists() && modelsDir.listFiles() != null) {
            for (File file : modelsDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    String name = file.getName().replace(".json", "");
                    configModels.put(name, file);
                    BSTweaker.LOG.info("Found config model: " + name);
                }
            }
        }

        // 扫描语言文件
        if (langDir.exists() && langDir.listFiles() != null) {
            for (File file : langDir.listFiles()) {
                if (file.getName().endsWith(".lang")) {
                    String name = file.getName().replace(".lang", "");
                    configLangs.put(name, file);
                    BSTweaker.LOG.info("Found config lang: " + name);
                }
            }
        }

        BSTweaker.LOG.info("Scanned config resources: " + configTextures.size() + " textures, "
                + configModels.size() + " models, " + configLangs.size() + " langs");
    }
    
    /**
     * 创建 README 文件说明目录用途
     */
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

    /**
     * 注册动态模型
     */
    public static void registerModel(String textureName) {
        // 如果 config 目录有自定义模型，使用它
        if (configModels.containsKey(textureName)) {
            try {
                String content = new String(Files.readAllBytes(configModels.get(textureName).toPath()),
                        StandardCharsets.UTF_8);
                String modelPath = "assets/" + Reference.MOD_ID + "/models/item/" + textureName + ".json";
                dynamicModels.put(modelPath, content);
                BSTweaker.LOG.info("Using config model: " + textureName);
                return;
            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to load config model: " + e.getMessage());
            }
        }

        // 否则生成默认模型
        String modelPath = "assets/" + Reference.MOD_ID + "/models/item/" + textureName + ".json";
        String modelContent = generateModelJson(textureName);
        dynamicModels.put(modelPath, modelContent);
    }
    
    /**
     * 生成模型 JSON 内容
     */
    private static String generateModelJson(String textureName) {
        return "{\n" +
               "  \"parent\": \"item/handheld\",\n" +
               "  \"textures\": {\n" +
               "    \"layer0\": \"" + Reference.MOD_ID + ":items/" + textureName + "\"\n" +
               "  }\n" +
               "}";
    }
    
    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        String namespace = location.getNamespace();
        String path = location.getPath();

        // 检查是否是支持的命名空间 (bstweaker 或 mujmajnkraftsbettersurvival)
        if (!Reference.MOD_ID.equals(namespace) && !BS_NAMESPACE.equals(namespace)) {
            throw new FileNotFoundException("Resource not found: " + location);
        }

        // 检查动态生成的模型
        String fullPath = "assets/" + namespace + "/" + path;
        String content = dynamicModels.get(fullPath);
        if (content != null) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        // 检查 config 目录中的模型文件 - 支持 models/ 和 models/item/ 两种格式
        if (path.endsWith(".json") && (path.startsWith("models/") || path.startsWith("models/item/"))) {
            String name;
            if (path.startsWith("models/item/")) {
                name = path.replace("models/item/", "").replace(".json", "");
            } else {
                name = path.replace("models/", "").replace(".json", "");
            }
            BSTweaker.LOG.info("Looking for model: " + name);
            // 直接匹配
            if (configModels.containsKey(name)) {
                BSTweaker.LOG.info("Loading config model: " + name);
                return new FileInputStream(configModels.get(name));
            }
            // 尝试去掉 itembstweaker_ 前缀
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                BSTweaker.LOG.info("Trying short name for model: " + shortName);
                if (configModels.containsKey(shortName)) {
                    BSTweaker.LOG.info("Loading config model (stripped prefix): " + shortName);
                    return new FileInputStream(configModels.get(shortName));
                }
            }
        }

        // 检查 config 目录中的纹理
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");
            BSTweaker.LOG.info("Looking for texture: " + name);
            // 直接匹配
            if (configTextures.containsKey(name)) {
                BSTweaker.LOG.info("Serving texture: " + name);
                return new FileInputStream(configTextures.get(name));
            }
            // 尝试去掉 itembstweaker_ 前缀
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                if (configTextures.containsKey(shortName)) {
                    BSTweaker.LOG.info("Serving texture (stripped prefix): " + shortName);
                    return new FileInputStream(configTextures.get(shortName));
                }
            }
            // 反向匹配：请求不带前缀，但文件带前缀
            String prefixedName = "itembstweaker_" + name;
            if (configTextures.containsKey(prefixedName)) {
                BSTweaker.LOG.info("Serving texture (with prefix): " + prefixedName);
                return new FileInputStream(configTextures.get(prefixedName));
            }
        }

        // 检查 config 目录中的 mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            if (configMcmeta.containsKey(name)) {
                return new FileInputStream(configMcmeta.get(name));
            }
            // 反向匹配
            String prefixedName = "itembstweaker_" + name;
            if (configMcmeta.containsKey(prefixedName)) {
                return new FileInputStream(configMcmeta.get(prefixedName));
            }
        }

        // 检查 config 目录中的语言文件
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

        // 诊断日志 - 追踪所有资源请求
        if (path.contains("itembstweaker") || path.contains("fiery")) {
            BSTweaker.LOG.info("[DRP] resourceExists query: " + namespace + ":" + path);
        }

        // 检查是否是支持的命名空间
        if (!Reference.MOD_ID.equals(namespace) && !BS_NAMESPACE.equals(namespace)) {
            return false;
        }

        // 检查动态模型
        String fullPath = "assets/" + namespace + "/" + path;
        if (dynamicModels.containsKey(fullPath)) {
            return true;
        }

        // 检查 config 模型 - 支持 models/xxx.json 和 models/item/xxx.json 两种格式
        if (path.endsWith(".json") && (path.startsWith("models/") || path.startsWith("models/item/"))) {
            String name;
            if (path.startsWith("models/item/")) {
                name = path.replace("models/item/", "").replace(".json", "");
            } else {
                name = path.replace("models/", "").replace(".json", "");
            }

            BSTweaker.LOG.info("[DRP] resourceExists model check: " + name + ", configModels=" + configModels.keySet());
            if (configModels.containsKey(name)) {
                return true;
            }
            // 尝试去掉 itembstweaker_ 前缀
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                BSTweaker.LOG.info("[DRP] Trying short name: " + shortName);
                if (configModels.containsKey(shortName)) {
                    return true;
                }
            }
        }

        // 检查 config 纹理
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");
            BSTweaker.LOG.info(
                    "[DRP] resourceExists texture check: " + name + ", configTextures=" + configTextures.keySet());

            // 直接匹配
            if (configTextures.containsKey(name)) {
                BSTweaker.LOG.info("[DRP] Texture found directly: " + name);
                return true;
            }
            // 尝试去掉 itembstweaker_ 前缀（请求带前缀，文件不带）
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                BSTweaker.LOG.info("[DRP] Trying short texture name: " + shortName);
                if (configTextures.containsKey(shortName)) {
                    return true;
                }
            }
            // 反向匹配：请求不带前缀，但文件带前缀
            String prefixedName = "itembstweaker_" + name;
            if (configTextures.containsKey(prefixedName)) {
                BSTweaker.LOG.info("[DRP] Texture found with prefix: " + prefixedName);
                return true;
            }
        }

        // 检查 config mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            if (configMcmeta.containsKey(name)) {
                return true;
            }
            // 尝试去掉 itembstweaker_ 前缀
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                if (configMcmeta.containsKey(shortName)) {
                    return true;
                }
            }
            // 反向匹配
            String prefixedName = "itembstweaker_" + name;
            if (configMcmeta.containsKey(prefixedName)) {
                return true;
            }
        }

        // 检查 config 语言文件
        if (path.startsWith("lang/") && path.endsWith(".lang")) {
            String name = path.replace("lang/", "").replace(".lang", "");
            return configLangs.containsKey(name);
        }

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
