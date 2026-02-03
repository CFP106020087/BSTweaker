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

        // 自动生成带 overrides 的 base model（三层结构）
        generateBaseModels(new File(configDir, "models"), modelsDir);

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
     * 翻译文件名 - 添加 "itembstweaker_" 前缀
     * fieryingotnunchaku_normal.json ->
     * itembstweaker_fieryingotnunchaku_normal.json
     */
    private static String translateFileName(String fileName, String type) {
        // 语言文件不需要前缀
        if ("lang".equals(type)) {
            return fileName;
        }

        // 检查是否已经有 itembstweaker_ 前缀
        if (fileName.startsWith("itembstweaker_")) {
            return fileName;
        }

        // 添加 itembstweaker_ 前缀
        return "itembstweaker_" + fileName;
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

    /**
     * 自动生成带 overrides 的 base model（三层结构）
     * 检测 xxx_normal.json，自动生成 xxx.json (base model)
     * 
     * Base model 结构:
     * - layer0: minecraft:items/stick (占位符)
     * - override: bstweaker:always=1 -> xxx_normal
     * - override: spinning=1 -> xxxspinning
     */
    public static void generateBaseModels(File sourceDir, File targetDir) {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }

        File[] files = sourceDir.listFiles((dir, name) -> name.endsWith("_normal.json"));
        if (files == null || files.length == 0) {
            return;
        }

        for (File normalFile : files) {
            String normalName = normalFile.getName();
            // xxx_normal.json -> xxx
            String baseName = normalName.replace("_normal.json", "");

            // 检查是否有对应的 spinning 模型
            File spinningFile = new File(sourceDir, baseName + "spinning.json");
            boolean hasSpinning = spinningFile.exists();

            // 生成 base model JSON
            String baseModelJson = generateBaseModelJson(baseName, hasSpinning);

            // 写入目标目录 (使用 itembstweaker_ 前缀)
            String targetFileName = "itembstweaker_" + baseName + ".json";
            File targetFile = new File(targetDir, targetFileName);

            try {
                Files.write(targetFile.toPath(),
                        baseModelJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                BSTweaker.LOG.info("Generated base model with overrides: " + targetFileName);
            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to generate base model: " + e.getMessage());
            }
        }
    }

    /**
     * 生成 base model JSON 内容
     */
    private static String generateBaseModelJson(String baseName, boolean hasSpinning) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"parent\": \"item/handheld\",\n");
        sb.append("  \"textures\": {\n");
        sb.append("    \"layer0\": \"minecraft:items/stick\"\n");
        sb.append("  },\n");
        sb.append("  \"overrides\": [\n");

        // Always override -> normal model
        // 格式: mujmajnkraftsbettersurvival:item/itembstweaker_<baseName>_normal
        sb.append("    {\n");
        sb.append("      \"predicate\": {\"bstweaker:always\": 1},\n");
        sb.append("      \"model\": \"").append(BS_NAMESPACE).append(":item/itembstweaker_").append(baseName)
                .append("_normal\"\n");
        sb.append("    }");

        // Spinning override (if exists)
        // 格式: mujmajnkraftsbettersurvival:item/itembstweaker_<baseName>spinning
        if (hasSpinning) {
            sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"predicate\": {\"spinning\": 1},\n");
            sb.append("      \"model\": \"").append(BS_NAMESPACE).append(":item/itembstweaker_").append(baseName)
                    .append("spinning\"\n");
            sb.append("    }");
        }

        sb.append("\n  ]\n");
        sb.append("}");

        return sb.toString();
    }

    // ========== 目录管理方法 ==========

    /**
     * 确保所有配置目录存在，并创建 README 文件
     */
    public static void ensureDirectoriesExist() {
        if (configDir == null) {
            init();
        }

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
                    "Place custom model .json files here.\nFormat: <weapon>_normal.json and <weapon>spinning.json\nBase model with overrides will be auto-generated.");
        }
        if (!langDir.exists()) {
            langDir.mkdirs();
            createReadme(langDir,
                    "Place language .lang files here (e.g., en_us.lang, zh_cn.lang).\nFormat: item.bstweaker.weapon_id.name=Display Name");
        }

        BSTweaker.LOG.info("Config directories verified: " + configDir.getAbsolutePath());
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
     * 获取配置目录
     */
    public static File getConfigDir() {
        if (configDir == null) {
            init();
        }
        return configDir;
    }

    /**
     * 获取资源目标目录
     */
    public static File getAssetsDir() {
        if (assetsDir == null) {
            init();
        }
        return assetsDir;
    }

    // ========== 纹理和模型工具方法 ==========

    /**
     * 从武器定义中获取纹理名称
     * 优先级: texture 字段 > id 字段
     */
    public static String getTextureName(com.google.gson.JsonObject def) {
        if (def.has("texture")) {
            String texture = def.get("texture").getAsString();
            // 移除命名空间前缀
            if (texture.contains(":")) {
                texture = texture.substring(texture.indexOf(":") + 1);
            }
            // 移除路径前缀
            if (texture.contains("/")) {
                texture = texture.substring(texture.lastIndexOf("/") + 1);
            }
            return texture;
        }

        if (def.has("id")) {
            return def.get("id").getAsString();
        }

        return "missing";
    }

    /**
     * 刷新客户端资源（触发 F3+T 效果）
     */
    public static void refreshClientResources() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            mc.refreshResources();
            BSTweaker.LOG.info("Client resources refreshed");
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to refresh client resources: " + e.getMessage());
        }
    }

    /**
     * 完整的热重载流程
     * 1. 重新复制资源文件
     * 2. 生成 base model
     * 3. 刷新客户端资源
     */
    public static void performHotReload() {
        BSTweaker.LOG.info("Performing hot reload...");

        // 1. 复制所有资源
        injectResources();

        // 2. 刷新客户端
        refreshClientResources();

        BSTweaker.LOG.info("Hot reload completed");
    }
}

