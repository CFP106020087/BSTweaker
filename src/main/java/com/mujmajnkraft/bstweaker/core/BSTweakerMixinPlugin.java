package com.mujmajnkraft.bstweaker.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import fermiumbooter.FermiumRegistryAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "com.mujmajnkraft.bstweaker.core" })
@IFMLLoadingPlugin.Name("BSTweakerCore")
@IFMLLoadingPlugin.SortingIndex(1005)
public class BSTweakerMixinPlugin implements IFMLLoadingPlugin {

    private static final Logger LOGGER = LogManager.getLogger("BSTweaker");
    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    static {
        try {
            // 注册 BetterSurvival 注入 Mixin (late mixin，在 BS 加载后)
            FermiumRegistryAPI.enqueueMixin(true, "mixins.bstweaker.json");
            LOGGER.info("Mixin queued via FermiumBooter");
        } catch (Throwable e) {
            LOGGER.error("FermiumBooter registration failed: " + e);
        }
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // 在 Coremod 加载阶段创建资源包（最早时机）
        try {
            File mcDir = (File) data.get("mcLocation");
            if (mcDir != null) {
                File configDir = new File(mcDir, "config/bstweaker");
                File resourcepackDir = new File(mcDir, "resourcepacks/bstweaker");

                createResourcePack(configDir, resourcepackDir);
                LOGGER.info("Resource pack created at: " + resourcepackDir.getAbsolutePath());
            }
        } catch (Throwable e) {
            LOGGER.error("Resource pack creation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建资源包文件夹结构
     * resourcepacks/bstweaker/
     * pack.mcmeta
     * assets/mujmajnkraftsbettersurvival/
     * models/item/
     * textures/items/
     */
    private void createResourcePack(File configDir, File resourcepackDir) throws IOException {
        if (!configDir.exists()) {
            LOGGER.info("Config dir not found, skipping resource pack creation");
            return;
        }

        // 创建资源包目录结构
        File assetsDir = new File(resourcepackDir, "assets/" + BS_NAMESPACE);
        File modelsDir = new File(assetsDir, "models/item");
        File texturesDir = new File(assetsDir, "textures/items");

        modelsDir.mkdirs();
        texturesDir.mkdirs();

        // 创建 pack.mcmeta
        File packMcmeta = new File(resourcepackDir, "pack.mcmeta");
        String mcmetaContent = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": 3,\n" +
                "    \"description\": \"BSTweaker Custom Weapons Resources\"\n" +
                "  }\n" +
                "}";
        Files.write(packMcmeta.toPath(), mcmetaContent.getBytes(StandardCharsets.UTF_8));

        // 复制模型文件
        File srcModelsDir = new File(configDir, "models");
        if (srcModelsDir.exists()) {
            copyResourceFiles(srcModelsDir, modelsDir, ".json", true);
        }

        // 复制纹理文件
        File srcTexturesDir = new File(configDir, "textures");
        if (srcTexturesDir.exists()) {
            copyResourceFiles(srcTexturesDir, texturesDir, ".png", false);
            copyResourceFiles(srcTexturesDir, texturesDir, ".png.mcmeta", false);
        }
    }

    /**
     * 复制资源文件，翻译文件名和内容
     */
    private void copyResourceFiles(File srcDir, File destDir, String ext, boolean translateModel) {
        if (!srcDir.exists() || !srcDir.isDirectory())
            return;

        File[] files = srcDir.listFiles((d, n) -> n.endsWith(ext));
        if (files == null)
            return;

        for (File file : files) {
            try {
                // 翻译文件名: fieryingotnunchaku -> itembstweaker_fieryingotnunchaku
                String baseName = file.getName();
                String targetName = translateFileName(baseName);
                File target = new File(destDir, targetName);

                if (translateModel && ext.equals(".json")) {
                    // 翻译模型内容
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    content = translateModelContent(content);
                    Files.write(target.toPath(), content.getBytes(StandardCharsets.UTF_8));
                } else {
                    Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                LOGGER.info("Copied: " + file.getName() + " -> " + targetName);
            } catch (Exception e) {
                LOGGER.error("Failed to copy " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * 翻译文件名
     * fieryingotnunchaku.json -> itembstweaker_fieryingotnunchaku.json
     */
    private String translateFileName(String fileName) {
        // 获取文件名和扩展名
        int dotIndex = fileName.indexOf('.');
        String name = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        // 如果已经有正确前缀，不修改
        if (name.startsWith("itembstweaker_")) {
            return fileName;
        }

        // 如果有 item 前缀但没有 bstweaker_
        if (name.startsWith("item")) {
            return "itembstweaker_" + name.substring(4) + extension;
        }

        // 添加 itembstweaker_ 前缀
        return "itembstweaker_" + name + extension;
    }

    /**
     * 翻译模型内容中的命名空间引用
     * bstweaker:items/xxx -> mujmajnkraftsbettersurvival:items/itembstweaker_xxx
     */
    private String translateModelContent(String content) {
        return content
                .replace("bstweaker:items/", BS_NAMESPACE + ":items/itembstweaker_")
                .replace("bstweaker:item/", BS_NAMESPACE + ":item/itembstweaker_");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
