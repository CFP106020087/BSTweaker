package com.mujmajnkraft.bstweaker.resource;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.config.ConfigDirectoryManager;

import java.io.File;
import java.io.IOException;

/**
 * 纹理提取器 - 从 JAR 提取默认纹理到配置目录
 * Texture Extractor - Extracts default textures from JAR to config directory
 * 
 * 从 ResourceInjector 提取的纹理提取功能
 * Extracted texture extraction functionality from ResourceInjector
 */
public class TextureExtractor {

    /**
     * 从 JAR 解压默认纹理到 cfg/textures（仅在目录为空时）
     * Extract default textures from JAR to cfg/textures (only if directory is empty)
     */
    public static void extractDefaultTextures() {
        File targetDir = ConfigDirectoryManager.getTexturesDir();
        
        // 如果目录已有文件，跳过
        File[] existing = targetDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (existing != null && existing.length > 0) {
            return;
        }

        String resourcePath = "/assets/bstweaker/config/textures";
        try {
            java.net.URL url = TextureExtractor.class.getResource(resourcePath);
            if (url == null) {
                BSTweaker.LOG.info("No default textures in JAR");
                return;
            }

            java.net.URI uri = url.toURI();
            java.nio.file.Path path;

            if (uri.getScheme().equals("jar")) {
                java.nio.file.FileSystem fs;
                try {
                    fs = java.nio.file.FileSystems.getFileSystem(uri);
                } catch (java.nio.file.FileSystemNotFoundException e) {
                    fs = java.nio.file.FileSystems.newFileSystem(uri, java.util.Collections.emptyMap());
                }
                path = fs.getPath(resourcePath);
            } else {
                path = java.nio.file.Paths.get(uri);
            }

            java.nio.file.Files.walk(path, 1).forEach(source -> {
                if (java.nio.file.Files.isRegularFile(source)) {
                    try {
                        String fileName = source.getFileName().toString();
                        // 去掉 item 前缀以匹配 weapons.json 中的 texture 名称
                        // 例如: itememeralddagger.png -> emeralddagger.png
                        String targetName = fileName;
                        if (fileName.startsWith("item")) {
                            targetName = fileName.substring(4);
                        }
                        File targetFile = new File(targetDir, targetName);
                        if (!targetFile.exists()) {
                            java.nio.file.Files.copy(source, targetFile.toPath());
                            BSTweaker.LOG.info("Extracted texture: " + fileName + " -> " + targetName);
                        }
                    } catch (IOException e) {
                        BSTweaker.LOG.error("Failed to extract: " + source.getFileName());
                    }
                }
            });
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to extract default textures: " + e.getMessage());
        }
    }
}
