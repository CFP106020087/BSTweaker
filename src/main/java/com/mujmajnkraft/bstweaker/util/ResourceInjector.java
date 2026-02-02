package com.mujmajnkraft.bstweaker.util;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.nio.file.*;

/**
 * 资源注入工具 - 将 config/bstweaker 中的资源复制到正确的 assets 目录
 * 使用 BetterSurvival 的命名空间: mujmajnkraftsbettersurvival
 */
public class ResourceInjector {

    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    private static File configDir;
    private static File assetsDir;

    /**
     * 初始化资源目录
     */
    public static void init() {
        configDir = new File(Loader.instance().getConfigDir(), "bstweaker");

        // 资源目标目录: run/assets/mujmajnkraftsbettersurvival/
        File runDir = new File(Loader.instance().getConfigDir().getParentFile(), "");
        assetsDir = new File(runDir, "assets/" + BS_NAMESPACE);

        BSTweaker.LOG.info("Config dir: " + configDir.getAbsolutePath());
        BSTweaker.LOG.info("Assets dir: " + assetsDir.getAbsolutePath());
    }

    /**
     * 复制所有资源到 BS 命名空间目录
     */
    public static void injectResources() {
        if (configDir == null) {
            init();
        }

        // 确保目标目录存在
        File modelsDir = new File(assetsDir, "models/item");
        File texturesDir = new File(assetsDir, "textures/items");
        File langDir = new File(assetsDir, "lang");

        modelsDir.mkdirs();
        texturesDir.mkdirs();
        langDir.mkdirs();

        // 复制模型文件
        copyResources(new File(configDir, "models"), modelsDir, ".json", "model");

        // 复制纹理文件
        copyResources(new File(configDir, "textures"), texturesDir, ".png", "texture");
        copyResources(new File(configDir, "textures"), texturesDir, ".png.mcmeta", "mcmeta");

        // 复制语言文件
        copyResources(new File(configDir, "lang"), langDir, ".lang", "lang");

        BSTweaker.LOG.info("Resource injection completed");
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
                // 翻译文件名: fieryingotnunchaku -> itemfieryingotnunchaku (添加 item 前缀)
                String targetName = translateFileName(file.getName(), type);
                File targetFile = new File(targetDir, targetName);

                if ("model".equals(type)) {
                    // 模型文件需要翻译内容中的命名空间
                    String content = new String(Files.readAllBytes(file.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                    content = translateModelContent(content);
                    Files.write(targetFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
     * 翻译文件名 - 添加 "item" 前缀以匹配 BS 的命名规范
     * fieryingotnunchaku.json -> itemfieryingotnunchaku.json
     * fieryingotnunchakuspinning.json -> itemfieryingotnunchakuspinning.json
     */
    private static String translateFileName(String fileName, String type) {
        // 语言文件不需要前缀
        if ("lang".equals(type)) {
            return fileName;
        }

        // 检查是否已经有 item 前缀
        if (fileName.startsWith("item")) {
            return fileName;
        }

        // 添加 item 前缀
        return "item" + fileName;
    }

    /**
     * 转换模型文件内容中的命名空间引用
     * bstweaker:items/ -> mujmajnkraftsbettersurvival:items/
     * bstweaker:item/ -> mujmajnkraftsbettersurvival:item/
     */
    public static String translateModelContent(String content) {
        return content
                .replace("bstweaker:items/", BS_NAMESPACE + ":items/item")
                .replace("bstweaker:item/", BS_NAMESPACE + ":item/item");
    }
}
