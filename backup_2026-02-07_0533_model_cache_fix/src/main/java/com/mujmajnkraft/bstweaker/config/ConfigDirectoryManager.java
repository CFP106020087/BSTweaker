package com.mujmajnkraft.bstweaker.config;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.core.BSTweakerConstants;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 配置目录管理器 - 管理 BSTweaker 的配置目录结构
 * Config Directory Manager - Manages BSTweaker's config directory structure
 * 
 * 从 ResourceInjector 提取的目录管理功能
 * Extracted directory management functionality from ResourceInjector
 */
public class ConfigDirectoryManager {

    private static File configDir;
    private static File assetsDir;
    private static boolean initialized = false;

    /**
     * 初始化配置目录
     * Initialize config directories
     */
    public static void init() {
        if (initialized) return;
        
        configDir = new File(Loader.instance().getConfigDir(), BSTweakerConstants.CONFIG_DIR);

        // 资源目标目录: run/assets/mujmajnkraftsbettersurvival/
        File runDir = new File(Loader.instance().getConfigDir().getParentFile(), "");
        assetsDir = new File(runDir, "assets/" + BSTweakerConstants.BS_NAMESPACE);

        BSTweaker.LOG.info("Config dir: " + configDir.getAbsolutePath());
        BSTweaker.LOG.info("Assets dir: " + assetsDir.getAbsolutePath());
        
        initialized = true;
    }

    /**
     * 确保所有配置目录存在，并创建 README 文件
     * Ensure all config directories exist and create README files
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
     * Create README file explaining directory purpose
     */
    public static void createReadme(File dir, String content) {
        try {
            File readme = new File(dir, "README.txt");
            if (!readme.exists()) {
                FileWriter writer = new FileWriter(readme);
                writer.write(content);
                writer.close();
            }
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to create README: " + e.getMessage());
        }
    }

    /**
     * 获取配置目录
     * Get config directory
     */
    public static File getConfigDir() {
        if (configDir == null) {
            init();
        }
        return configDir;
    }

    /**
     * 获取资源目标目录
     * Get assets target directory
     */
    public static File getAssetsDir() {
        if (assetsDir == null) {
            init();
        }
        return assetsDir;
    }

    /**
     * 获取模型目录
     * Get models directory
     */
    public static File getModelsDir() {
        return new File(getConfigDir(), "models");
    }

    /**
     * 获取纹理目录
     * Get textures directory
     */
    public static File getTexturesDir() {
        return new File(getConfigDir(), "textures");
    }

    /**
     * 获取语言文件目录
     * Get lang files directory
     */
    public static File getLangDir() {
        return new File(getConfigDir(), "lang");
    }

    /**
     * 获取 assets 下的模型目录
     * Get models directory under assets
     */
    public static File getAssetsModelsDir() {
        return new File(getAssetsDir(), "models/item");
    }

    /**
     * 获取 assets 下的纹理目录
     * Get textures directory under assets
     */
    public static File getAssetsTexturesDir() {
        return new File(getAssetsDir(), "textures/items");
    }

    /**
     * 获取 assets 下的语言文件目录
     * Get lang directory under assets
     */
    public static File getAssetsLangDir() {
        return new File(getAssetsDir(), "lang");
    }

    /**
     * 重置初始化状态（用于测试）
     * Reset initialization state (for testing)
     */
    public static void reset() {
        initialized = false;
        configDir = null;
        assetsDir = null;
    }
}
