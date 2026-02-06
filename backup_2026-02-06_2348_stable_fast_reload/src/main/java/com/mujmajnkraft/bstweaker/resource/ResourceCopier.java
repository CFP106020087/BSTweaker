package com.mujmajnkraft.bstweaker.resource;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.config.ConfigDirectoryManager;
import com.mujmajnkraft.bstweaker.core.BSTweakerConstants;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 资源复制器 - 复制配置文件夹中的资源到 assets 目录
 * Resource Copier - Copies resources from config folder to assets directory
 * 
 * 从 ResourceInjector 提取的资源复制功能
 * Extracted resource copying functionality from ResourceInjector
 */
public class ResourceCopier {

    /**
     * 复制资源（模型、纹理、语言文件）- DISABLED
     * Copy resources (models, textures, lang files) - DISABLED
     * 
     * Now using DynamicResourcePack for runtime resource loading.
     * Copying to assets/ was interfering with hot-reload.
     */
    public static void copyAllResources() {
        // DISABLED: Now using DynamicResourcePack instead of copying to assets/
        // copyModels();
        // copyTextures();
        // copyLangFiles();
        BSTweaker.LOG.info("ResourceCopier.copyAllResources() - DISABLED (using DynamicResourcePack)");
    }

    /**
     * 复制模型文件
     * Copy model files
     */
    private static void copyModels() {
        File sourceDir = ConfigDirectoryManager.getModelsDir();
        File targetDir = ConfigDirectoryManager.getAssetsModelsDir();
        targetDir.mkdirs();

        File[] files = sourceDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                String translatedContent = translateModelContent(content);
                String targetName = translateFileName(file.getName(), false);

                File targetFile = new File(targetDir, targetName);
                Files.write(targetFile.toPath(), translatedContent.getBytes(StandardCharsets.UTF_8));
                BSTweaker.LOG.debug("Copied model: " + file.getName() + " -> " + targetName);
            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to copy model: " + file.getName());
            }
        }
    }

    /**
     * 复制纹理文件
     * Copy texture files
     */
    private static void copyTextures() {
        File sourceDir = ConfigDirectoryManager.getTexturesDir();
        File targetDir = ConfigDirectoryManager.getAssetsTexturesDir();
        targetDir.mkdirs();

        File[] files = sourceDir.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".mcmeta"));
        if (files == null) return;

        for (File file : files) {
            try {
                String targetName = translateFileName(file.getName(), false);
                File targetFile = new File(targetDir, targetName);
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                BSTweaker.LOG.debug("Copied texture: " + file.getName() + " -> " + targetName);
            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to copy texture: " + file.getName());
            }
        }
    }

    /**
     * 复制语言文件
     * Copy lang files
     */
    private static void copyLangFiles() {
        File sourceDir = ConfigDirectoryManager.getLangDir();
        File targetDir = ConfigDirectoryManager.getAssetsLangDir();
        targetDir.mkdirs();

        File[] files = sourceDir.listFiles((dir, name) -> name.endsWith(".lang"));
        if (files == null) return;

        for (File file : files) {
            try {
                File targetFile = new File(targetDir, file.getName());
                
                // 合并而不是覆盖
                if (targetFile.exists()) {
                    String existing = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
                    String newContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    
                    StringBuilder merged = new StringBuilder(existing);
                    for (String line : newContent.split("\n")) {
                        String key = line.split("=")[0];
                        if (!existing.contains(key + "=")) {
                            if (!merged.toString().endsWith("\n")) merged.append("\n");
                            merged.append(line);
                        }
                    }
                    Files.write(targetFile.toPath(), merged.toString().getBytes(StandardCharsets.UTF_8));
                } else {
                    Files.copy(file.toPath(), targetFile.toPath());
                }
                BSTweaker.LOG.debug("Copied lang file: " + file.getName());
            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to copy lang file: " + file.getName());
            }
        }
    }

    /**
     * 翻译文件名（不再使用前缀）
     * Translate filename (no longer using prefix)
     * 
     * @param fileName 原始文件名
     * @param isLangFile 是否为语言文件
     * @return 翻译后的文件名（现在直接返回原始名）
     */
    public static String translateFileName(String fileName, boolean isLangFile) {
        return fileName;
    }

    /**
     * 翻译模型内容中的命名空间引用
     * Translate namespace references in model content
     * 
     * bstweaker:xxx -> mujmajnkraftsbettersurvival:items/xxx
     * bstweaker:item/xxx -> mujmajnkraftsbettersurvival:item/xxx
     */
    public static String translateModelContent(String content) {
        String translated = content;
        
        // bstweaker:items/ -> mujmajnkraftsbettersurvival:items/
        translated = translated.replace(
                "bstweaker:items/",
                BSTweakerConstants.BS_NAMESPACE + ":items/");

        // bstweaker:item/ -> mujmajnkraftsbettersurvival:item/
        translated = translated.replace(
                "bstweaker:item/",
                BSTweakerConstants.BS_NAMESPACE + ":item/");

        return translated;
    }
}
