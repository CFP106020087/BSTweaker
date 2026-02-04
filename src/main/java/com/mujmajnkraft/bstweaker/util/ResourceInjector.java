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
        File cfgModelsDir = new File(configDir, "models");
        File cfgTexturesDir = new File(configDir, "textures");

        modelsDir.mkdirs();
        texturesDir.mkdirs();
        langDir.mkdirs();
        cfgModelsDir.mkdirs();
        cfgTexturesDir.mkdirs();

        // 1. 从 JAR 解压默认纹理到 cfg/textures（首次运行）
        extractDefaultTextures(cfgTexturesDir);

        // 2. 根据 weapons.json 自动生成模型到 cfg/models
        generateWeaponModels(cfgModelsDir, cfgTexturesDir);

        // 3. 复制模型文件到 assets
        copyResources(cfgModelsDir, modelsDir, ".json", "model");

        // 4. 复制纹理文件到 assets
        copyResources(cfgTexturesDir, texturesDir, ".png", "texture");
        copyResources(cfgTexturesDir, texturesDir, ".png.mcmeta", "mcmeta");

        // 5. 复制语言文件到 assets
        copyResources(new File(configDir, "lang"), langDir, ".lang", "lang");

        BSTweaker.LOG.info("Resource injection completed");
    }

    /**
     * 从 JAR 解压默认纹理到 cfg/textures（仅在目录为空时）
     */
    private static void extractDefaultTextures(File targetDir) {
        // 如果目录已有文件，跳过
        File[] existing = targetDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (existing != null && existing.length > 0) {
            return;
        }

        String resourcePath = "/assets/bstweaker/config/textures";
        try {
            java.net.URL url = ResourceInjector.class.getResource(resourcePath);
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
                        File targetFile = new File(targetDir, fileName);
                        if (!targetFile.exists()) {
                            java.nio.file.Files.copy(source, targetFile.toPath());
                            BSTweaker.LOG.info("Extracted texture: " + fileName);
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

    /**
     * 根据 weapons.json 自动生成模型到 cfg/models
     * 生成: <texture>_normal.json, <texture>.json (base model),
     * <texture>spinning.json (如果有)
     */
    private static void generateWeaponModels(File modelsDir, File texturesDir) {
        File weaponsFile = new File(configDir, "weapons.json");
        if (!weaponsFile.exists())
            return;

        try {
            String content = new String(Files.readAllBytes(weaponsFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = new com.google.gson.JsonParser()
                    .parse(content).getAsJsonObject();

            if (!root.has("weapons"))
                return;

            com.google.gson.JsonArray weapons = root.getAsJsonArray("weapons");
            for (com.google.gson.JsonElement elem : weapons) {
                com.google.gson.JsonObject weapon = elem.getAsJsonObject();

                // 获取纹理名（优先使用 texture 字段，否则使用 id）
                String texture;
                if (weapon.has("texture")) {
                    texture = weapon.get("texture").getAsString();
                } else if (weapon.has("id")) {
                    texture = weapon.get("id").getAsString();
                } else {
                    continue;
                }

                // 获取武器类型（用于决定 parent）
                String type = weapon.has("type") ? weapon.get("type").getAsString().toLowerCase() : "dagger";

                // 1. 生成 _normal.json
                File normalModel = new File(modelsDir, texture + "_normal.json");
                if (!normalModel.exists()) {
                    String parent = "nunchaku".equals(type) ? "item/generated" : "item/handheld";
                    String normalJson = "{\n" +
                            "  \"parent\": \"" + parent + "\",\n" +
                            "  \"textures\": {\n" +
                            "    \"layer0\": \"bstweaker:items/" + texture + "\"\n" +
                            "  }\n" +
                            "}";
                    Files.write(normalModel.toPath(), normalJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    BSTweaker.LOG.info("Generated: " + normalModel.getName());
                }

                // 2. 检查是否有 spinning 纹理
                boolean hasSpinning = new File(texturesDir, texture + "spinning.png").exists();

                // 3. 生成 spinning.json（如果有 spinning 纹理）
                if (hasSpinning) {
                    File spinningModel = new File(modelsDir, texture + "spinning.json");
                    if (!spinningModel.exists()) {
                        String spinningJson = "{\n" +
                                "  \"parent\": \"item/generated\",\n" +
                                "  \"textures\": {\n" +
                                "    \"layer0\": \"bstweaker:items/" + texture + "spinning\"\n" +
                                "  }\n" +
                                "}";
                        Files.write(spinningModel.toPath(),
                                spinningJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        BSTweaker.LOG.info("Generated: " + spinningModel.getName());
                    }
                }

                // 4. 生成 base model（带 overrides）
                File baseModel = new File(modelsDir, texture + ".json");
                if (!baseModel.exists()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\n");
                    sb.append("  \"parent\": \"item/handheld\",\n");
                    sb.append("  \"textures\": {\n");
                    sb.append("    \"layer0\": \"minecraft:items/stick\"\n");
                    sb.append("  },\n");
                    sb.append("  \"overrides\": [\n");
                    sb.append("    {\n");
                    sb.append("      \"predicate\": {\"bstweaker:always\": 1},\n");
                    sb.append("      \"model\": \"bstweaker:item/" + texture + "_normal\"\n");
                    sb.append("    }");
                    if (hasSpinning) {
                        sb.append(",\n    {\n");
                        sb.append("      \"predicate\": {\"spinning\": 1},\n");
                        sb.append("      \"model\": \"bstweaker:item/" + texture + "spinning\"\n");
                        sb.append("    }");
                    }
                    sb.append("\n  ]\n");
                    sb.append("}");
                    Files.write(baseModel.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    BSTweaker.LOG.info("Generated base model: " + baseModel.getName());
                }
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to generate weapon models: " + e.getMessage());
        }
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
     * bstweaker:items/xxx -> mujmajnkraftsbettersurvival:items/itembstweaker_xxx
     * bstweaker:item/xxx -> mujmajnkraftsbettersurvival:item/itembstweaker_xxx
     */
    public static String translateModelContent(String content) {
        return content
                .replace("bstweaker:items/", BS_NAMESPACE + ":items/itembstweaker_")
                .replace("bstweaker:item/", BS_NAMESPACE + ":item/itembstweaker_");
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

