package com.mujmajnkraft.bstweaker.util;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源注入工具 - 将 config/bstweaker 中的资源注入到 BetterSurvival JAR
 * 支持运行时热重载
 */
public class ResourceInjector {

    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    private static File configDir;
    private static File modsDir;

    /**
     * 初始化资源目录
     */
    public static void init() {
        File mcDir = Loader.instance().getConfigDir().getParentFile();
        configDir = new File(mcDir, "config/bstweaker");
        modsDir = new File(mcDir, "mods");

        BSTweaker.LOG.info("Config dir: " + configDir.getAbsolutePath());
        BSTweaker.LOG.info("Mods dir: " + modsDir.getAbsolutePath());
    }

    /**
     * 刷新资源 - 重新扫描 config 目录
     * 用于热重载 - 调用后需要 F3+T 或 refreshResources()
     */
    public static void injectResources() {
        if (configDir == null) {
            init();
        }

        // 使用 DynamicResourcePack 重新扫描 config 目录
        com.mujmajnkraft.bstweaker.client.DynamicResourcePack.rescan();
        BSTweaker.LOG.info("Config resources rescanned via DynamicResourcePack");
    }

    /**
     * 查找 BetterSurvival JAR 文件
     */
    private static File findBSJar() {
        if (!modsDir.exists())
            return null;

        // Match "bettersurvival" or "better_survival" (with or without underscore)
        File[] jars = modsDir.listFiles((d, n) -> {
            String lower = n.toLowerCase();
            return (lower.contains("bettersurvival") || lower.contains("better_survival")) && n.endsWith(".jar");
        });

        if (jars != null && jars.length > 0) {
            return jars[0];
        }
        return null;
    }

    /**
     * 注入资源到 JAR
     */
    private static void injectIntoJar(File jarFile) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "false");

        URI jarUri = URI.create("jar:" + jarFile.toURI());
        BSTweaker.LOG.info("Opening JAR: " + jarFile.getAbsolutePath());

        FileSystem jarFs = null;
        boolean needClose = false;

        try {
            // 先尝试获取已存在的 FileSystem (JAR 被 JVM 加载时会创建)
            jarFs = FileSystems.getFileSystem(jarUri);
            BSTweaker.LOG.info("Using existing JAR filesystem");
        } catch (FileSystemNotFoundException e) {
            // 不存在则创建新的
            jarFs = FileSystems.newFileSystem(jarUri, env);
            needClose = true;
            BSTweaker.LOG.info("Created new JAR filesystem");
        }

        try {
            // 注入模型
            injectModels(jarFs);
            // 注入纹理
            injectTextures(jarFs);
            BSTweaker.LOG.info("Resource injection completed");
        } finally {
            // 只关闭我们自己创建的 FileSystem
            if (needClose && jarFs != null) {
                jarFs.close();
            }
        }
    }

    private static void injectModels(FileSystem jarFs) throws IOException {
        File srcDir = new File(configDir, "models");
        if (!srcDir.exists())
            return;

        File[] files = srcDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null)
            return;

        for (File file : files) {
            String targetName = translateFileName(file.getName());
            Path targetPath = jarFs.getPath("assets/" + BS_NAMESPACE + "/models/item/" + targetName);

            Files.createDirectories(targetPath.getParent());

            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            content = translateModelContent(content);

            Files.write(targetPath, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            BSTweaker.LOG.info("Injected model: " + file.getName() + " -> " + targetName);
        }
    }

    private static void injectTextures(FileSystem jarFs) throws IOException {
        File srcDir = new File(configDir, "textures");
        if (!srcDir.exists())
            return;

        File[] files = srcDir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (!file.getName().endsWith(".png") && !file.getName().endsWith(".mcmeta"))
                continue;

            String targetName = translateFileName(file.getName());
            Path targetPath = jarFs.getPath("assets/" + BS_NAMESPACE + "/textures/items/" + targetName);

            Files.createDirectories(targetPath.getParent());
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            BSTweaker.LOG.info("Injected texture: " + file.getName() + " -> " + targetName);
        }
    }

    /**
     * 翻译文件名: fieryingotnunchaku.json -> itembstweaker_fieryingotnunchaku.json
     */
    private static String translateFileName(String fileName) {
        int dotIndex = fileName.indexOf('.');
        String name = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        if (name.startsWith("itembstweaker_")) {
            return fileName;
        }

        if (name.startsWith("item")) {
            return "itembstweaker_" + name.substring(4) + extension;
        }

        return "itembstweaker_" + name + extension;
    }

    /**
     * 转换模型内容中的命名空间引用
     */
    public static String translateModelContent(String content) {
        return content
                .replace("bstweaker:items/", BS_NAMESPACE + ":items/itembstweaker_")
                .replace("bstweaker:item/", BS_NAMESPACE + ":item/itembstweaker_");
    }
}
