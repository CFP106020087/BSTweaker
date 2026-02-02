package com.mujmajnkraft.bstweaker.util;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * 资源包注入工具 - 将 config/bstweaker 中的资源复制到 resourcepacks/bstweaker 目录
 * 支持运行时热重载
 */
public class ResourceInjector {

    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    private static File configDir;
    private static File resourcepackDir;

    /**
     * 初始化资源目录
     */
    public static void init() {
        File mcDir = Loader.instance().getConfigDir().getParentFile();
        configDir = new File(mcDir, "config/bstweaker");
        resourcepackDir = new File(mcDir, "resourcepacks/bstweaker");

        BSTweaker.LOG.info("Config dir: " + configDir.getAbsolutePath());
        BSTweaker.LOG.info("Resourcepack dir: " + resourcepackDir.getAbsolutePath());
    }

    /**
     * 复制所有资源到 resourcepacks/bstweaker 目录
     * 支持热重载 - 调用后需要 refreshResources()
     */
    public static void injectResources() {
        if (configDir == null) {
            init();
        }

        if (!configDir.exists()) {
            BSTweaker.LOG.info("Config dir not found, skipping resource injection");
            return;
        }

        try {
            // 创建资源包目录结构
            File assetsDir = new File(resourcepackDir, "assets/" + BS_NAMESPACE);
            File modelsDir = new File(assetsDir, "models/item");
            File texturesDir = new File(assetsDir, "textures/items");
            File langDir = new File(assetsDir, "lang");

            modelsDir.mkdirs();
            texturesDir.mkdirs();
            langDir.mkdirs();

            // 创建/更新 pack.mcmeta
            createPackMcmeta();

            // 复制模型文件
            copyResources(new File(configDir, "models"), modelsDir, ".json", "model");

            // 复制纹理文件
            copyResources(new File(configDir, "textures"), texturesDir, ".png", "texture");
            copyResources(new File(configDir, "textures"), texturesDir, ".png.mcmeta", "mcmeta");

            // 复制语言文件 (不需要翻译文件名)
            copyResources(new File(configDir, "lang"), langDir, ".lang", "lang");

            BSTweaker.LOG.info("Resource pack updated at: " + resourcepackDir.getAbsolutePath());

        } catch (Exception e) {
            BSTweaker.LOG.error("Resource injection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建 pack.mcmeta 文件
     */
    private static void createPackMcmeta() throws IOException {
        File packMcmeta = new File(resourcepackDir, "pack.mcmeta");
        String content = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 3,\n" +
                "    \"description\": \"BSTweaker Custom Weapons Resources\\nEnable this to see custom textures!\"\n" +
                "  }\n" +
                "}";
        Files.write(packMcmeta.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 复制指定类型的资源文件
     */
    private static void copyResources(File sourceDir, File targetDir, String extension, String type) {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }

        File[] files = sourceDir.listFiles((dir, name) -> name.endsWith(extension));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                // 翻译文件名 (lang 文件不需要翻译)
                String targetName = "lang".equals(type) ? file.getName() : translateFileName(file.getName());
                File targetFile = new File(targetDir, targetName);

                if ("model".equals(type)) {
                    // 模型文件需要翻译内容中的命名空间
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    content = translateModelContent(content);
                    Files.write(targetFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
                } else {
                    // 其他文件直接复制
                    Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                BSTweaker.LOG.info("Injected " + type + ": " + file.getName() + " -> " + targetName);

            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to copy " + type + " " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * 翻译文件名 - 添加 "itembstweaker_" 前缀以匹配武器注册名
     * fieryingotnunchaku.json -> itembstweaker_fieryingotnunchaku.json
     */
    private static String translateFileName(String fileName) {
        // 获取文件名和扩展名
        int dotIndex = fileName.indexOf('.');
        String name = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        // 如果已经有正确的前缀
        if (name.startsWith("itembstweaker_")) {
            return fileName;
        }

        // 如果有 item 前缀但没有 bstweaker_
        if (name.startsWith("item")) {
            return "itembstweaker_" + name.substring(4) + extension;
        }

        // 添加 itembstweaker_ 前缀
        return "itembstweaker_" + name + extension;
    }

    /**
     * 转换模型文件内容中的命名空间引用
     * bstweaker:items/xxx -> mujmajnkraftsbettersurvival:items/itembstweaker_xxx
     */
    public static String translateModelContent(String content) {
        return content
                .replace("bstweaker:items/", BS_NAMESPACE + ":items/itembstweaker_")
                .replace("bstweaker:item/", BS_NAMESPACE + ":item/itembstweaker_");
    }
}
