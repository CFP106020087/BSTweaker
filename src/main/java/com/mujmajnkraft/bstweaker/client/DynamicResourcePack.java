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
     * 扫描 config 目录中的资源文件
     */
    public static void scanConfigResources() {
        // 扫描纹理
        File texturesDir = new File(configDir, "textures");
        if (texturesDir.exists()) {
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
        File modelsDir = new File(configDir, "models");
        if (modelsDir.exists()) {
            for (File file : modelsDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    String name = file.getName().replace(".json", "");
                    configModels.put(name, file);
                    BSTweaker.LOG.info("Found config model: " + name);
                }
            }
        }

        // 扫描语言文件
        File langDir = new File(configDir, "lang");
        if (langDir.exists()) {
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

        // 检查是否是 bstweaker 命名空间
        if (!Reference.MOD_ID.equals(namespace)) {
            throw new FileNotFoundException("Resource not found: " + location);
        }

        // 检查动态生成的模型
        String fullPath = "assets/" + namespace + "/" + path;
        String content = dynamicModels.get(fullPath);
        if (content != null) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        // 检查 config 目录中的纹理
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");
            if (configTextures.containsKey(name)) {
                return new FileInputStream(configTextures.get(name));
            }
        }

        // 检查 config 目录中的 mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            if (configMcmeta.containsKey(name)) {
                return new FileInputStream(configMcmeta.get(name));
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

        if (!Reference.MOD_ID.equals(namespace)) {
            return false;
        }

        // 检查动态模型
        String fullPath = "assets/" + namespace + "/" + path;
        if (dynamicModels.containsKey(fullPath)) {
            return true;
        }

        // 检查 config 纹理
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");
            return configTextures.containsKey(name);
        }

        // 检查 config mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            return configMcmeta.containsKey(name);
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
        return ImmutableSet.of(Reference.MOD_ID);
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
