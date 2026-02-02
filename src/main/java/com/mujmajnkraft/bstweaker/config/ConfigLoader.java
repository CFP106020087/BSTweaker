package com.mujmajnkraft.bstweaker.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mujmajnkraft.bstweaker.BSTweaker;

/**
 * JSON 配置加载器
 */
public class ConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configDir;
    private static List<WeaponDefinition> loadedWeapons = new ArrayList<>();

    /**
     * 初始化配置目录
     */
    public static void init(File modConfigDir) {
        configDir = new File(modConfigDir, "bstweaker");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // 加载配置
        loadConfig();
    }

    /**
     * 加载武器配置
     */
    public static void loadConfig() {
        loadedWeapons.clear();

        File weaponsFile = new File(configDir, "weapons.json");

        // 如果配置文件不存在，创建示例
        if (!weaponsFile.exists()) {
            createExampleConfig(weaponsFile);
        }

        // 读取配置
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(weaponsFile),
                StandardCharsets.UTF_8)) {
            WeaponConfig config = GSON.fromJson(reader, WeaponConfig.class);
            if (config != null && config.weapons != null) {
                for (WeaponDefinition def : config.weapons) {
                    if (def.isValid()) {
                        loadedWeapons.add(def);
                        BSTweaker.LOG.info("Loaded weapon definition: " + def.id + " (type: " + def.type + ")");
                    } else {
                        BSTweaker.LOG
                                .warn("Invalid weapon definition skipped: " + (def.id != null ? def.id : "unknown"));
                    }
                }
            }
            BSTweaker.LOG.info("Loaded " + loadedWeapons.size() + " weapon definitions from config.");
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to load weapons.json", e);
        }
    }

    /**
     * 创建示例配置文件
     */
    private static void createExampleConfig(File file) {
        WeaponConfig example = new WeaponConfig();

        // 示例: 炙铁双截棍
        WeaponDefinition blazingNunchaku = new WeaponDefinition();
        blazingNunchaku.id = "blazing_iron_nunchaku";
        blazingNunchaku.type = "nunchaku";
        blazingNunchaku.displayName = "炙铁双截棍";
        blazingNunchaku.damageModifier = 0.6f;
        blazingNunchaku.speedModifier = 0.25f;
        blazingNunchaku.texture = "bstweaker:items/blazing_iron_nunchaku";

        MaterialDefinition blazingIron = new MaterialDefinition();
        blazingIron.name = "BlazingIron";
        blazingIron.harvestLevel = 2;
        blazingIron.durability = 300;
        blazingIron.efficiency = 6.5f;
        blazingIron.damage = 3.0f;
        blazingIron.enchantability = 16;
        blazingIron.repairItem = "minecraft:iron_ingot";
        blazingNunchaku.material = blazingIron;

        example.weapons.add(blazingNunchaku);

        // 示例: 钻石战锤
        WeaponDefinition diamondHammer = new WeaponDefinition();
        diamondHammer.id = "enhanced_diamond_hammer";
        diamondHammer.type = "hammer";
        diamondHammer.displayName = "强化钻石战锤";
        diamondHammer.damageModifier = 1.4f;
        diamondHammer.speedModifier = 1.3f;
        diamondHammer.texture = "bstweaker:items/enhanced_diamond_hammer";

        MaterialDefinition enhancedDiamond = new MaterialDefinition();
        enhancedDiamond.name = "EnhancedDiamond";
        enhancedDiamond.harvestLevel = 3;
        enhancedDiamond.durability = 2000;
        enhancedDiamond.efficiency = 9.0f;
        enhancedDiamond.damage = 4.0f;
        enhancedDiamond.enchantability = 12;
        enhancedDiamond.repairItem = "minecraft:diamond";
        diamondHammer.material = enhancedDiamond;

        example.weapons.add(diamondHammer);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(example, writer);
            BSTweaker.LOG.info("Created example weapons.json config file.");
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to create example config", e);
        }
    }

    /**
     * 获取已加载的武器定义
     */
    public static List<WeaponDefinition> getLoadedWeapons() {
        return loadedWeapons;
    }
}
