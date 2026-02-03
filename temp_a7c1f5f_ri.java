package com.mujmajnkraft.bstweaker.util;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.nio.file.*;

/**
 * 韏?瘜典撌亙 - 撠?config/bstweaker 銝剔?韏?憭?唳迤蝖桃? assets ?桀?
 * 雿輻 BetterSurvival ??征?? mujmajnkraftsbettersurvival
 */
public class ResourceInjector {

    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    private static File configDir;
    private static File assetsDir;

    /**
     * ????皞敶?     */
    public static void init() {
        configDir = new File(Loader.instance().getConfigDir(), "bstweaker");

        // 韏??格??桀?: run/assets/mujmajnkraftsbettersurvival/
        File runDir = new File(Loader.instance().getConfigDir().getParentFile(), "");
        assetsDir = new File(runDir, "assets/" + BS_NAMESPACE);

        BSTweaker.LOG.info("Config dir: " + configDir.getAbsolutePath());
        BSTweaker.LOG.info("Assets dir: " + assetsDir.getAbsolutePath());
    }

    /**
     * 憭???皞 BS ?賢?蝛粹?桀?
     */
    public static void injectResources() {
        if (configDir == null) {
            init();
        }

        // 蝖桐??格??桀?摮
        File modelsDir = new File(assetsDir, "models/item");
        File texturesDir = new File(assetsDir, "textures/items");
        File langDir = new File(assetsDir, "lang");

        modelsDir.mkdirs();
        texturesDir.mkdirs();
        langDir.mkdirs();

        // 憭璅∪??辣
        copyResources(new File(configDir, "models"), modelsDir, ".json", "model");

        // 憭蝥寧??辣
        copyResources(new File(configDir, "textures"), texturesDir, ".png", "texture");
        copyResources(new File(configDir, "textures"), texturesDir, ".png.mcmeta", "mcmeta");

        // 憭霂剛??辣
        copyResources(new File(configDir, "lang"), langDir, ".lang", "lang");

        BSTweaker.LOG.info("Resource injection completed");
    }

    /**
     * 憭??蝐餃???皞?隞?     */
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
                // 蝧餉??辣?? fieryingotnunchaku -> itemfieryingotnunchaku (瘛餃? item ??)
                String targetName = translateFileName(file.getName(), type);
                File targetFile = new File(targetDir, targetName);

                if ("model".equals(type)) {
                    // 璅∪??辣?閬蕃霂?摰嫣葉??征??                    String content = new String(Files.readAllBytes(file.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                    content = translateModelContent(content);
                    Files.write(targetFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    // ?嗡??辣?湔憭
                    Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                BSTweaker.LOG.info("Injected " + type + ": " + file.getName() + " -> " + targetName);

            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to copy " + type + " " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * 蝧餉??辣??- 瘛餃? "item" ??隞亙??BS ?????     * fieryingotnunchaku.json -> itemfieryingotnunchaku.json
     * fieryingotnunchakuspinning.json -> itemfieryingotnunchakuspinning.json
     */
    private static String translateFileName(String fileName, String type) {
        // 霂剛??辣銝?閬?蝻
        if ("lang".equals(type)) {
            return fileName;
        }

        // 璉?交?血歇蝏? item ??
        if (fileName.startsWith("item")) {
            return fileName;
        }

        // 瘛餃? item ??
        return "item" + fileName;
    }

    /**
     * 頧祆璅∪??辣?捆銝剔??賢?蝛粹撘
     * bstweaker:items/ -> mujmajnkraftsbettersurvival:items/
     * bstweaker:item/ -> mujmajnkraftsbettersurvival:item/
     */
    public static String translateModelContent(String content) {
        return content
                .replace("bstweaker:items/", BS_NAMESPACE + ":items/item")
                .replace("bstweaker:item/", BS_NAMESPACE + ":item/item");
    }
}
