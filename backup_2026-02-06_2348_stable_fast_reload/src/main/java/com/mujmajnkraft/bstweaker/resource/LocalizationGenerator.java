package com.mujmajnkraft.bstweaker.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.config.ConfigDirectoryManager;
import com.mujmajnkraft.bstweaker.core.BSTweakerConstants;
import com.mujmajnkraft.bstweaker.core.WeaponType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/**
 * 本地化生成器 - 自动生成 tooltips.json 和 lang 文件
 * Localization Generator - Auto-generates tooltips.json and lang files
 * 
 * 从 ResourceInjector 提取的本地化生成功能
 * Extracted localization generation functionality from ResourceInjector
 */
public class LocalizationGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 根据 weapons.json 自动生成 tooltips.json 和 lang 文件
     * Auto-generate tooltips.json and lang files based on weapons.json
     */
    public static void generateWeaponTooltipsAndLang(JsonArray weaponDefs) {
        File configDir = ConfigDirectoryManager.getConfigDir();
        File tooltipsFile = new File(configDir, "tooltips.json");
        File langDir = ConfigDirectoryManager.getLangDir();

        // 读取现有 tooltips
        Set<String> existingTooltipIds = new HashSet<>();
        JsonObject tooltipsRoot = new JsonObject();
        JsonArray tooltipsArray = new JsonArray();

        if (tooltipsFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(tooltipsFile), StandardCharsets.UTF_8)) {
                tooltipsRoot = GSON.fromJson(reader, JsonObject.class);
                if (tooltipsRoot.has("tooltips")) {
                    tooltipsArray = tooltipsRoot.getAsJsonArray("tooltips");
                    for (int i = 0; i < tooltipsArray.size(); i++) {
                        JsonObject t = tooltipsArray.get(i).getAsJsonObject();
                        if (t.has("id")) {
                            existingTooltipIds.add(t.get("id").getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                BSTweaker.LOG.error("Failed to read tooltips.json: " + e.getMessage());
            }
        }

        // 新增 tooltips 和 lang 条目
        StringBuilder enLang = new StringBuilder();
        StringBuilder zhLang = new StringBuilder();

        for (int i = 0; i < weaponDefs.size(); i++) {
            JsonObject weaponDef = weaponDefs.get(i).getAsJsonObject();
            if (!weaponDef.has("id")) continue;

            String id = weaponDef.get("id").getAsString();

            // 已存在则跳过
            if (existingTooltipIds.contains(id)) continue;

            String type = weaponDef.has("type") ? weaponDef.get("type").getAsString() : "dagger";
            WeaponType weaponType = WeaponType.fromId(type);
            String typeNameEn = weaponType != null ? weaponType.getNameEn() : capitalizeFirst(type);
            String typeNameZh = weaponType != null ? weaponType.getNameZh() : type;

            // 从 material.name 生成物品名（首字母大写）
            String materialName = "";
            if (weaponDef.has("material") && weaponDef.getAsJsonObject("material").has("name")) {
                materialName = weaponDef.getAsJsonObject("material").get("name").getAsString();
            }
            String displayNameEn = capitalizeFirst(materialName) + " " + typeNameEn;
            String displayNameZh = capitalizeFirst(materialName) + typeNameZh;

            // 创建 tooltip 条目
            JsonObject newTooltip = new JsonObject();
            newTooltip.addProperty("id", id);
            newTooltip.addProperty("displayName", displayNameEn);
            JsonArray tooltipLines = new JsonArray();
            // 默认空 tooltip
            newTooltip.add("tooltip", tooltipLines);
            tooltipsArray.add(newTooltip);

            // 添加 lang 条目
            String langKey = "item." + BSTweakerConstants.ITEM_PREFIX.replace("_", ".") + id + ".name";
            enLang.append(langKey).append("=").append(displayNameEn).append("\n");
            zhLang.append(langKey).append("=").append(displayNameZh).append("\n");

            BSTweaker.LOG.info("Auto-generated tooltip for: " + id);
        }

        // 保存 tooltips.json
        try {
            tooltipsRoot.add("tooltips", tooltipsArray);
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(tooltipsFile), StandardCharsets.UTF_8);
            GSON.toJson(tooltipsRoot, writer);
            writer.close();
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to write tooltips.json: " + e.getMessage());
        }

        // 追加 lang 文件
        appendLangFile(new File(langDir, "en_us.lang"), enLang.toString());
        appendLangFile(new File(langDir, "zh_cn.lang"), zhLang.toString());
    }

    /**
     * 追加内容到 lang 文件（避免重复）
     * Append content to lang file (avoiding duplicates)
     */
    public static void appendLangFile(File langFile, String content) {
        if (content.isEmpty()) return;

        try {
            String existing = "";
            if (langFile.exists()) {
                existing = new String(Files.readAllBytes(langFile.toPath()), StandardCharsets.UTF_8);
            }

            // 过滤已存在的行
            StringBuilder newContent = new StringBuilder();
            for (String line : content.split("\n")) {
                if (!line.isEmpty() && !existing.contains(line.split("=")[0])) {
                    newContent.append(line).append("\n");
                }
            }

            if (newContent.length() > 0) {
                java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(langFile, true), StandardCharsets.UTF_8);
                if (!existing.endsWith("\n") && !existing.isEmpty()) {
                    writer.write("\n");
                }
                writer.write(newContent.toString());
                writer.close();
                BSTweaker.LOG.info("Updated lang file: " + langFile.getName());
            }
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to append lang file: " + e.getMessage());
        }
    }

    /**
     * 首字母大写
     * Capitalize first letter
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
