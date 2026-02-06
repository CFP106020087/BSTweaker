package com.mujmajnkraft.bstweaker.client;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义翻译加载器 - 从 config/bstweaker/lang 目录加载翻译
 * Custom translation loader - loads translations from config/bstweaker/lang directory
 * 
 * Minecraft's I18n only loads lang files from JAR at startup.
 * This provides runtime translation for dynamic weapon tooltips.
 */
public class ConfigLangLoader {
    
    private static final Map<String, String> translations = new HashMap<>();
    private static boolean loaded = false;
    
    /**
     * Load translations from config lang files.
     * Should be called after ResourceInjector generates lang files.
     */
    public static void load() {
        translations.clear();
        
        // Hardcoded default translations - always available
        loadDefaults();
        
        File configDir = new File(Loader.instance().getConfigDir(), "bstweaker/lang");
        if (!configDir.exists()) {
            BSTweaker.LOG.info("No lang directory found, using defaults only");
            loaded = true;
            return;
        }
        
        // Get current language
        String currentLang = Minecraft.getMinecraft().getLanguageManager()
            .getCurrentLanguage().getLanguageCode().toLowerCase();
        
        // Try current language, then fallback to en_us
        String[] langOrder = { currentLang, "en_us" };
        
        for (String lang : langOrder) {
            File langFile = new File(configDir, lang + ".lang");
            if (langFile.exists()) {
                loadLangFile(langFile);
                break;
            }
        }
        
        loaded = true;
        BSTweaker.LOG.info("ConfigLangLoader: loaded " + translations.size() + " translations");
    }
    
    /**
     * Load hardcoded default translations
     */
    private static void loadDefaults() {
        // Preset weapon display names (English)
        translations.put("bstweaker.weapon.emeraldexampledagger.name", "Emerald Dagger");
        translations.put("bstweaker.weapon.emeraldexamplehammer.name", "Emerald Hammer");
        translations.put("bstweaker.weapon.emeraldexamplespear.name", "Emerald Spear");
        translations.put("bstweaker.weapon.emeraldexamplebattleaxe.name", "Emerald Battleaxe");
        translations.put("bstweaker.weapon.emeraldexamplenunchaku.name", "Emerald Nunchaku");
        
        // Item registry names (for Minecraft I18n fallback)
        translations.put("item.emeraldexampledaggerdagger.name", "Emerald Dagger");
        translations.put("item.emeraldexamplehammerhammer.name", "Emerald Hammer");
        translations.put("item.emeraldexamplespearspearnunchakuspear.name", "Emerald Spear");
        translations.put("item.emeraldexamplebattleaxebattleaxe.name", "Emerald Battleaxe");
        translations.put("item.emeraldexamplenunchakunchaku.name", "Emerald Nunchaku");
        
        // Common tooltip keys
        translations.put("bstweaker.tooltip.spinning", "Hold Left-Click to spin, continuing to land hits will increase damage");
    }
    
    private static void loadLangFile(File file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1);
                    translations.put(key, value);
                }
            }
        } catch (IOException e) {
            BSTweaker.LOG.error("Failed to load lang file: " + file.getAbsolutePath());
        }
    }
    
    /**
     * Get translation for a key.
     * @param key Translation key
     * @return Translated string, or the key itself if not found
     */
    public static String translate(String key) {
        if (!loaded) {
            load();
        }
        return translations.getOrDefault(key, key);
    }
    
    /**
     * Check if a translation exists for a key.
     */
    public static boolean hasTranslation(String key) {
        if (!loaded) {
            load();
        }
        return translations.containsKey(key);
    }
    
    /**
     * Reload translations (called during hot-reload).
     */
    public static void reload() {
        loaded = false;
        load();
    }
}
