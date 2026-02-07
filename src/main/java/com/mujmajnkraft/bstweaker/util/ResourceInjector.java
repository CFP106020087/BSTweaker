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

    /** 记录使用了占位纹理的武器 ID（用于 tooltip 提示） */
    private static final java.util.Set<String> placeholderWeaponIds = new java.util.HashSet<>();

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

        // 1. 从 JAR 释放所有免费素材到 cfg/textures（逐个检查，不覆盖）
        extractAllBundledTextures(cfgTexturesDir);

        // 2. 按武器逐个检查纹理，缺失则从 JAR 释放 emerald 范本并重命名
        extractPerWeaponTextures(cfgTexturesDir);

        // 2. 自动填充 tooltips.json 和 scripts.json（不覆盖已有条目）
        if (com.mujmajnkraft.bstweaker.config.BSTweakerConfig.autoGenerateTooltips) {
            generateWeaponTooltipsAndLang();
        }
        autoFillScripts();

        // 3. 根据 weapons.json 自动生成模型到 cfg/models (黑名单内的 ID 跳过)
        generateWeaponModels(cfgModelsDir, cfgTexturesDir);

        // DISABLED: Direct asset copying - now using DynamicResourcePack for runtime
        // loading
        // These copies were interfering with hot-reload by putting files in assets/
        // which always overrides DynamicResourcePack

        // 4. 复制模型文件到 assets (DISABLED)
        // copyResources(cfgModelsDir, modelsDir, ".json", "model");

        // 5. 复制纹理文件到 assets (DISABLED)
        // copyResources(cfgTexturesDir, texturesDir, ".png", "texture");
        // copyResources(cfgTexturesDir, texturesDir, ".png.mcmeta", "mcmeta");

        // 6. 复制语言文件到 assets (DISABLED)
        // copyResources(new File(configDir, "lang"), langDir, ".lang", "lang");

        BSTweaker.LOG.info("Resource injection completed (using DynamicResourcePack only)");
    }

    /**
     * 从 JAR 释放所有免费素材到 cfg/textures（逐个检查，不存在则复制，不覆盖）。
     * 包括 emerald 范本、obsidian、scarlite、dragonsteel 等所有打包纹理。
     */
    private static void extractAllBundledTextures(File targetDir) {
        String resourcePath = "/assets/bstweaker/config/textures";
        try {
            java.net.URL url = ResourceInjector.class.getResource(resourcePath);
            if (url == null) {
                BSTweaker.LOG.info("No bundled textures in JAR");
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

            int[] count = { 0 };
            java.nio.file.Files.walk(path, 1).forEach(source -> {
                if (java.nio.file.Files.isRegularFile(source)) {
                    try {
                        String fileName = source.getFileName().toString();
                        File targetFile = new File(targetDir, fileName);
                        if (!targetFile.exists()) {
                            java.nio.file.Files.copy(source, targetFile.toPath());
                            count[0]++;
                        }
                    } catch (IOException e) {
                        BSTweaker.LOG.error("Failed to extract: " + source.getFileName());
                    }
                }
            });
            if (count[0] > 0) {
                BSTweaker.LOG.info("Extracted " + count[0] + " bundled textures to cfg/textures");
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to extract bundled textures: " + e.getMessage());
        }
    }

    /**
     * 按武器逐个检查纹理是否存在，不存在则从 JAR 释放 emerald 范本并重命名。
     * 同时释放范本自身的纹理（itememeraldexample*.png）。
     */
    private static void extractPerWeaponTextures(File targetDir) {
        File weaponsFile = new File(configDir, "weapons.json");
        if (!weaponsFile.exists())
            return;

        try {
            String content = new String(Files.readAllBytes(weaponsFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonElement rootElem = new com.google.gson.JsonParser().parse(content);
            if (rootElem == null || !rootElem.isJsonObject())
                return;
            com.google.gson.JsonObject root = rootElem.getAsJsonObject();
            if (!root.has("weapons"))
                return;

            // Emerald template type mapping
            String[] TYPES = { "dagger", "hammer", "spear", "battleaxe", "nunchaku" };
            // nunchaku has extra spinning variant
            String[] NUNCHAKU_EXTRAS = { "spinning" };

            for (com.google.gson.JsonElement elem : root.getAsJsonArray("weapons")) {
                com.google.gson.JsonObject weapon = elem.getAsJsonObject();
                String texture = getTextureName(weapon);
                String type = weapon.has("type") ? weapon.get("type").getAsString() : "";
                if (texture == null || texture.isEmpty() || type.isEmpty())
                    continue;

                // Check base texture: item<texture>.png
                String baseFileName = "item" + texture + ".png";
                File baseFile = new File(targetDir, baseFileName);
                if (!baseFile.exists()) {
                    // Copy from emerald template of same type
                    String templateName = "itememeraldexample" + type + ".png";
                    copyTemplateTexture(templateName, baseFile);
                    // 记录此武器使用了占位纹理
                    String wId = weapon.has("id") ? weapon.get("id").getAsString() : null;
                    if (wId != null)
                        placeholderWeaponIds.add(wId);
                }

                // Check variant textures (e.g. spinning for nunchaku)
                if ("nunchaku".equals(type)) {
                    for (String variant : NUNCHAKU_EXTRAS) {
                        String variantFileName = "item" + texture + variant + ".png";
                        File variantFile = new File(targetDir, variantFileName);
                        if (!variantFile.exists()) {
                            String templateName = "itememeraldexample" + type + variant + ".png";
                            copyTemplateTexture(templateName, variantFile);
                        }
                        // Also copy .mcmeta if exists
                        String mcmetaFileName = variantFileName + ".mcmeta";
                        File mcmetaFile = new File(targetDir, mcmetaFileName);
                        if (!mcmetaFile.exists()) {
                            String templateMcmeta = "itememeraldexample" + type + variant + ".png.mcmeta";
                            copyTemplateTexture(templateMcmeta, mcmetaFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to extract per-weapon textures: " + e.getMessage());
        }
    }

    /**
     * 从 JAR 复制范本纹理到目标文件。
     */
    private static void copyTemplateTexture(String templateName, File targetFile) {
        String resourcePath = "/assets/bstweaker/config/textures/" + templateName;
        try (java.io.InputStream in = ResourceInjector.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                BSTweaker.LOG.debug("Template texture not found in JAR: " + templateName);
                return;
            }
            Files.copy(in, targetFile.toPath());
            BSTweaker.LOG.info("Extracted template texture: " + templateName + " -> " + targetFile.getName());
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to copy template texture: " + e.getMessage());
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

                // 只为支持的武器类型生成模型
                if (!isSupportedWeaponType(type)) {
                    BSTweaker.LOG.warn("Skipping unsupported weapon type: " + type + " for " + texture);
                    continue;
                }

                // 检查黑名单 - 在配置中指定的 ID 不会自动生成模型
                // Check blacklist - IDs in config will NOT have models auto-generated
                String weaponId = weapon.has("id") ? weapon.get("id").getAsString() : texture;
                if (isInModelBlacklist(weaponId)) {
                    BSTweaker.LOG.info("Skipping blacklisted weapon ID: " + weaponId);
                    continue;
                }

                // Compute registryName to match BS constructor:
                // BS sets registryName = "item" + material.name().toLowerCase() + type
                String matName = "";
                if (weapon.has("material") && weapon.getAsJsonObject("material").has("name")) {
                    matName = weapon.getAsJsonObject("material").get("name").getAsString()
                            .replaceAll("\\s+", "").toLowerCase();
                }
                String registryName = "item" + matName + type;

                // 1. 生成 _normal.json (使用 registryName 作为文件名，匹配武器注册名)
                File normalModel = new File(modelsDir, registryName + "_normal.json");
                if (!normalModel.exists()) {
                    String parent = "nunchaku".equals(type) ? "item/generated" : "item/handheld";
                    StringBuilder normalSb = new StringBuilder();
                    normalSb.append("{\n");
                    normalSb.append("  \"parent\": \"").append(parent).append("\",\n");
                    normalSb.append("  \"textures\": {\n");
                    normalSb.append("    \"layer0\": \"mujmajnkraftsbettersurvival:items/item").append(texture)
                            .append("\"\n");
                    normalSb.append("  }");
                    // nunchaku 需要特殊的 display 属性
                    if ("nunchaku".equals(type)) {
                        normalSb.append(",\n  \"display\": {\n");
                        normalSb.append(
                                "    \"thirdperson_righthand\": { \"rotation\": [75, 90, 0], \"translation\": [0, 4, 2.5], \"scale\": [0.85, 0.85, 0.85] },\n");
                        normalSb.append(
                                "    \"thirdperson_lefthand\": { \"rotation\": [75, -90, 0], \"translation\": [0, 4, 2.5], \"scale\": [0.85, 0.85, 0.85] },\n");
                        normalSb.append(
                                "    \"firstperson_righthand\": { \"rotation\": [15, 90, 0], \"translation\": [-0.5, 2.85, 0.8], \"scale\": [0.68, 0.68, 0.68] },\n");
                        normalSb.append(
                                "    \"firstperson_lefthand\": { \"rotation\": [15, -90, 0], \"translation\": [-0.5, 2.85, 0.8], \"scale\": [0.68, 0.68, 0.68] }\n");
                        normalSb.append("  }");
                    }
                    normalSb.append("\n}");
                    Files.write(normalModel.toPath(),
                            normalSb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    BSTweaker.LOG.info("Generated: " + normalModel.getName());
                }

                // 2. 检查是否是 nunchaku 类型（nunchaku 需要 spinning 模型）
                boolean needsSpinning = "nunchaku".equals(type);

                // 3. 生成 spinning.json（nunchaku 类型自动生成）
                if (needsSpinning) {
                    File spinningModel = new File(modelsDir, registryName + "spinning.json");
                    if (!spinningModel.exists()) {
                        StringBuilder spinningSb = new StringBuilder();
                        spinningSb.append("{\n");
                        spinningSb.append("  \"parent\": \"item/generated\",\n");
                        spinningSb.append("  \"textures\": {\n");
                        spinningSb.append("    \"layer0\": \"mujmajnkraftsbettersurvival:items/item").append(texture)
                                .append("spinning\"\n");
                        spinningSb.append("  }");
                        // nunchaku spinning 需要特殊的 display 属性（更大的 scale）
                        if ("nunchaku".equals(type)) {
                            spinningSb.append(",\n  \"display\": {\n");
                            spinningSb.append(
                                    "    \"thirdperson_righthand\": { \"rotation\": [75, 90, 0], \"translation\": [0, 9.75, 8], \"scale\": [1.7, 1.7, 0.85] },\n");
                            spinningSb.append(
                                    "    \"thirdperson_lefthand\": { \"rotation\": [75, -90, 0], \"translation\": [0, 9.75, 8], \"scale\": [1.7, 1.7, 0.85] },\n");
                            spinningSb.append(
                                    "    \"firstperson_righthand\": { \"rotation\": [15, 90, 0], \"translation\": [0, 9.35, -2.2], \"scale\": [1.36, 1.36, 0.68] },\n");
                            spinningSb.append(
                                    "    \"firstperson_lefthand\": { \"rotation\": [15, -90, 0], \"translation\": [0, 9.35, -2.2], \"scale\": [1.36, 1.36, 0.68] }\n");
                            spinningSb.append("  }");
                        }
                        spinningSb.append("\n}");
                        Files.write(spinningModel.toPath(),
                                spinningSb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        BSTweaker.LOG.info("Generated: " + spinningModel.getName());
                    }
                }

                // 4. 生成 base model（带 overrides）
                File baseModel = new File(modelsDir, registryName + ".json");
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
                    sb.append("      \"model\": \"mujmajnkraftsbettersurvival:item/" + registryName + "_normal\"\n");
                    sb.append("    }");
                    if (needsSpinning) {
                        sb.append(",\n    {\n");
                        sb.append("      \"predicate\": {\"spinning\": 1},\n");
                        sb.append(
                                "      \"model\": \"mujmajnkraftsbettersurvival:item/" + registryName + "spinning\"\n");
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
     * 自动为 weapons.json 中的武器生成 tooltips.json 条目和 lang 文件
     * - 如果 tooltips.json 中没有该武器，自动添加
     * - 自动生成/更新 lang 文件中的翻译键
     */
    private static void generateWeaponTooltipsAndLang() {
        File weaponsFile = new File(configDir, "weapons.json");
        File tooltipsFile = new File(configDir, "tooltips.json");
        File langDir = new File(configDir, "lang");
        langDir.mkdirs();

        if (!weaponsFile.exists())
            return;

        try {
            // 读取 weapons.json
            String weaponsContent = new String(Files.readAllBytes(weaponsFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject weaponsRoot = new com.google.gson.JsonParser()
                    .parse(weaponsContent).getAsJsonObject();

            if (!weaponsRoot.has("weapons"))
                return;
            com.google.gson.JsonArray weapons = weaponsRoot.getAsJsonArray("weapons");

            // 读取或创建 tooltips.json
            com.google.gson.JsonObject tooltipsRoot;
            com.google.gson.JsonArray tooltipsArray;
            if (tooltipsFile.exists()) {
                String tooltipsContent = new String(Files.readAllBytes(tooltipsFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                com.google.gson.JsonElement tooltipsElem = new com.google.gson.JsonParser().parse(tooltipsContent);
                tooltipsRoot = (tooltipsElem != null && tooltipsElem.isJsonObject())
                        ? tooltipsElem.getAsJsonObject()
                        : new com.google.gson.JsonObject();
                tooltipsArray = tooltipsRoot.has("tooltips") ? tooltipsRoot.getAsJsonArray("tooltips")
                        : new com.google.gson.JsonArray();
            } else {
                tooltipsRoot = new com.google.gson.JsonObject();
                tooltipsArray = new com.google.gson.JsonArray();
            }

            // 收集已存在的 tooltip IDs
            java.util.Set<String> existingIds = new java.util.HashSet<>();
            for (com.google.gson.JsonElement elem : tooltipsArray) {
                if (elem.isJsonObject() && elem.getAsJsonObject().has("id")) {
                    existingIds.add(elem.getAsJsonObject().get("id").getAsString());
                }
            }

            // lang 文件内容
            StringBuilder zhLang = new StringBuilder();
            StringBuilder enLang = new StringBuilder();
            zhLang.append("# BSTweaker 武器本地化 - 自动生成\n\n");
            enLang.append("# BSTweaker Weapon Localization - Auto-generated\n\n");

            boolean modified = false;

            for (com.google.gson.JsonElement elem : weapons) {
                com.google.gson.JsonObject weapon = elem.getAsJsonObject();
                String id = weapon.has("id") ? weapon.get("id").getAsString() : null;
                if (id == null)
                    continue;

                String type = weapon.has("type") ? weapon.get("type").getAsString() : "weapon";

                // 只为支持的武器类型生成 tooltip 和 lang
                if (!isSupportedWeaponType(type)) {
                    continue;
                }

                String typeNameZh = getWeaponTypeNameZh(type);
                String typeNameEn = getWeaponTypeNameEn(type);

                // 生成 lang keys
                String nameKey = "bstweaker.weapon." + id + ".name";
                String tip1Key = "bstweaker.weapon." + id + ".tip1";
                String tip2Key = "bstweaker.weapon." + id + ".tip2";
                String tip3Key = "bstweaker.weapon." + id + ".tip3";
                String tip4Key = "bstweaker.weapon." + id + ".tip4";
                // 物品原始翻译 key (Minecraft 标准格式: item.<translationKey>.name)
                // If id already ends with type, don't double it
                String translationKey = id.endsWith(type) ? id : id + type;
                String itemLangKey = "item." + translationKey + ".name";

                // 添加到 lang 文件
                // 物品原始名称
                zhLang.append(itemLangKey).append("=").append(id).append("\n");
                enLang.append(itemLangKey).append("=").append(id).append("\n");
                // tooltip 相关
                zhLang.append(nameKey).append("=").append(id).append("\n");
                zhLang.append(tip1Key).append("=§7一把自定义的").append(typeNameZh).append("\n");
                zhLang.append(tip2Key).append("=§e由 BSTweaker 生成\n");
                zhLang.append(tip3Key).append("=§6可在 tooltips.json 中自定义\n");

                enLang.append(nameKey).append("=").append(id).append("\n");
                enLang.append(tip1Key).append("=§7A custom ").append(typeNameEn).append("\n");
                enLang.append(tip2Key).append("=§eGenerated by BSTweaker\n");
                enLang.append(tip3Key).append("=§6Customize in tooltips.json\n");

                // 占位纹理提示 (tip4)
                boolean isPlaceholder = placeholderWeaponIds.contains(id);
                if (isPlaceholder) {
                    zhLang.append(tip4Key).append("=§c\u26a0 占位纹理，请在 textures/ 中替换\n");
                    enLang.append(tip4Key).append("=§c\u26a0 Placeholder texture, replace in textures/\n");
                }
                zhLang.append("\n");
                enLang.append("\n");

                // 如果 tooltips 中不存在，添加
                if (!existingIds.contains(id)) {
                    com.google.gson.JsonObject tooltip = new com.google.gson.JsonObject();
                    tooltip.addProperty("id", id);
                    tooltip.addProperty("displayName", "@" + nameKey);
                    com.google.gson.JsonArray tips = new com.google.gson.JsonArray();
                    tips.add("@" + tip1Key);
                    tips.add("@" + tip2Key);
                    tips.add("@" + tip3Key);
                    if (isPlaceholder) {
                        tips.add("@" + tip4Key);
                    }
                    tooltip.add("tooltip", tips);
                    tooltipsArray.add(tooltip);
                    modified = true;
                    BSTweaker.LOG.info("Auto-generated tooltip for: " + id);
                }
            }

            // 写入 tooltips.json（如果有修改）
            if (modified) {
                tooltipsRoot.add("tooltips", tooltipsArray);
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                Files.write(tooltipsFile.toPath(),
                        gson.toJson(tooltipsRoot).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            // 写入 lang 文件 (追加模式：只添加新 key，不覆盖已有)
            File zhLangFile = new File(langDir, "zh_cn.lang");
            File enLangFile = new File(langDir, "en_us.lang");
            appendLangFile(zhLangFile, zhLang.toString());
            appendLangFile(enLangFile, enLang.toString());

        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to generate tooltips and lang: " + e.getMessage());
        }
    }

    /**
     * 自动为 weapons.json 中的武器填充 scripts.json 条目。
     * 如果 scripts.json 中不存在该武器的条目，自动添加带注释的空模板。
     * 不覆盖已有条目。
     */
    private static void autoFillScripts() {
        File weaponsFile = new File(configDir, "weapons.json");
        File scriptsFile = new File(configDir, "scripts.json");

        if (!weaponsFile.exists())
            return;

        try {
            // 读取 weapons.json
            String weaponsContent = new String(Files.readAllBytes(weaponsFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonElement weaponsElem = new com.google.gson.JsonParser().parse(weaponsContent);
            if (weaponsElem == null || !weaponsElem.isJsonObject())
                return;
            com.google.gson.JsonObject weaponsRoot = weaponsElem.getAsJsonObject();
            if (!weaponsRoot.has("weapons"))
                return;

            // 读取或创建 scripts.json
            com.google.gson.JsonObject scriptsRoot;
            com.google.gson.JsonArray scriptsArray;
            if (scriptsFile.exists()) {
                String scriptsContent = new String(Files.readAllBytes(scriptsFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                com.google.gson.JsonElement scriptsElem = new com.google.gson.JsonParser().parse(scriptsContent);
                scriptsRoot = (scriptsElem != null && scriptsElem.isJsonObject())
                        ? scriptsElem.getAsJsonObject()
                        : new com.google.gson.JsonObject();
                scriptsArray = scriptsRoot.has("scripts") ? scriptsRoot.getAsJsonArray("scripts")
                        : new com.google.gson.JsonArray();
            } else {
                scriptsRoot = new com.google.gson.JsonObject();
                scriptsArray = new com.google.gson.JsonArray();
            }

            // 收集已存在的 script IDs
            java.util.Set<String> existingIds = new java.util.HashSet<>();
            for (com.google.gson.JsonElement elem : scriptsArray) {
                if (elem.isJsonObject() && elem.getAsJsonObject().has("id")) {
                    existingIds.add(elem.getAsJsonObject().get("id").getAsString());
                }
            }

            boolean modified = false;
            for (com.google.gson.JsonElement elem : weaponsRoot.getAsJsonArray("weapons")) {
                com.google.gson.JsonObject weapon = elem.getAsJsonObject();
                String id = weapon.has("id") ? weapon.get("id").getAsString() : null;
                if (id == null)
                    continue;

                if (!existingIds.contains(id)) {
                    com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                    script.addProperty("id", id);
                    script.addProperty("_comment",
                            "在 events 中添加效果，示例: {\"event\":\"onHit\",\"actions\":[\"victim.setFire(3)\"]}");
                    script.add("events", new com.google.gson.JsonArray());
                    scriptsArray.add(script);
                    modified = true;
                    BSTweaker.LOG.info("Auto-generated script entry for: " + id);
                }
            }

            if (modified) {
                scriptsRoot.add("scripts", scriptsArray);
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                Files.write(scriptsFile.toPath(),
                        gson.toJson(scriptsRoot).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to auto-fill scripts: " + e.getMessage());
        }
    }

    /**
     * 追加模式写入 lang 文件：只添加新 key，不覆盖已有
     */
    private static void appendLangFile(File langFile, String newContent) {
        try {
            // 读取已有内容
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            StringBuilder existing = new StringBuilder();
            if (langFile.exists()) {
                for (String line : Files.readAllLines(langFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                    existing.append(line).append("\n");
                    if (line.contains("=") && !line.trim().startsWith("#")) {
                        existingKeys.add(line.split("=")[0].trim());
                    }
                }
            } else {
                existing.append("# BSTweaker Weapon Localization\n\n");
            }

            // 只添加新 key
            StringBuilder toAppend = new StringBuilder();
            for (String line : newContent.split("\n")) {
                if (line.contains("=") && !line.trim().startsWith("#")) {
                    String key = line.split("=")[0].trim();
                    if (!existingKeys.contains(key)) {
                        toAppend.append(line).append("\n");
                    }
                }
            }

            // 如果有新内容，追加到文件末尾
            if (toAppend.length() > 0) {
                existing.append("\n# Auto-generated\n");
                existing.append(toAppend);
                Files.write(langFile.toPath(), existing.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                BSTweaker.LOG.info("Appended new keys to: " + langFile.getName());
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to append lang file: " + e.getMessage());
        }
    }

    /**
     * 支持的武器类型列表 (BetterSurvival 原版5种)
     */
    private static final java.util.Set<String> SUPPORTED_TYPES = new java.util.HashSet<>(
            java.util.Arrays.asList("dagger", "hammer", "spear", "battleaxe", "nunchaku"));

    private static boolean isSupportedWeaponType(String type) {
        return SUPPORTED_TYPES.contains(type.toLowerCase());
    }

    /**
     * 检查武器 ID 是否在模型生成黑名单中
     * Check if weapon ID is in the model generation blacklist
     * 
     * @param weaponId 武器 ID / weapon ID
     * @return true 如果在黑名单中 / if in blacklist
     */
    private static boolean isInModelBlacklist(String weaponId) {
        String[] blacklist = com.mujmajnkraft.bstweaker.config.BSTweakerConfig.modelBlacklist;
        if (blacklist == null || blacklist.length == 0) {
            return false;
        }
        for (String id : blacklist) {
            if (id != null && id.equalsIgnoreCase(weaponId)) {
                return true;
            }
        }
        return false;
    }

    private static String getWeaponTypeNameZh(String type) {
        switch (type.toLowerCase()) {
            case "dagger":
                return "匕首";
            case "hammer":
                return "战锤";
            case "spear":
                return "长矛";
            case "battleaxe":
                return "战斧";
            case "nunchaku":
                return "双截棍";
            default:
                return "武器";
        }
    }

    private static String getWeaponTypeNameEn(String type) {
        switch (type.toLowerCase()) {
            case "dagger":
                return "Dagger";
            case "hammer":
                return "Hammer";
            case "spear":
                return "Spear";
            case "battleaxe":
                return "Battle Axe";
            case "nunchaku":
                return "Nunchaku";
            default:
                return "Weapon";
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

