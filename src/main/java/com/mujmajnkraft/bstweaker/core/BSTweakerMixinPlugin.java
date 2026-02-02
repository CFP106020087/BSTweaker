package com.mujmajnkraft.bstweaker.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import fermiumbooter.FermiumRegistryAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "com.mujmajnkraft.bstweaker.core" })
@IFMLLoadingPlugin.Name("BSTweakerCore")
@IFMLLoadingPlugin.SortingIndex(1005)
public class BSTweakerMixinPlugin implements IFMLLoadingPlugin {

    private static final Logger LOGGER = LogManager.getLogger("BSTweaker");

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
        // 在 Coremod 加载阶段注入资源（最早时机）
        try {
            // 获取 minecraft 目录
            java.io.File mcDir = (java.io.File) data.get("mcLocation");
            if (mcDir != null) {
                java.io.File configDir = new java.io.File(mcDir, "config/bstweaker");
                java.io.File assetsDir = new java.io.File(mcDir, "assets/mujmajnkraftsbettersurvival");

                injectResourcesEarly(configDir, assetsDir);
                LOGGER.info("Early resource injection completed (ASM phase)");
            }
        } catch (Throwable e) {
            LOGGER.error("Early resource injection failed: " + e.getMessage());
        }
    }

    /**
     * 早期资源注入 - 在 ASM 阶段执行
     */
    private void injectResourcesEarly(java.io.File configDir, java.io.File assetsDir) {
        if (!configDir.exists()) {
            return;
        }

        // 创建目标目录
        java.io.File modelsDir = new java.io.File(assetsDir, "models/item");
        java.io.File texturesDir = new java.io.File(assetsDir, "textures/items");
        modelsDir.mkdirs();
        texturesDir.mkdirs();

        // 复制模型
        copyResourceFiles(new java.io.File(configDir, "models"), modelsDir, ".json", true);
        // 复制纹理
        copyResourceFiles(new java.io.File(configDir, "textures"), texturesDir, ".png", false);
        copyResourceFiles(new java.io.File(configDir, "textures"), texturesDir, ".png.mcmeta", false);
    }

    private void copyResourceFiles(java.io.File srcDir, java.io.File destDir, String ext, boolean translateModel) {
        if (!srcDir.exists() || !srcDir.isDirectory())
            return;

        java.io.File[] files = srcDir.listFiles((d, n) -> n.endsWith(ext));
        if (files == null)
            return;

        for (java.io.File file : files) {
            try {
                String targetName = file.getName().startsWith("item") ? file.getName() : "item" + file.getName();
                java.io.File target = new java.io.File(destDir, targetName);

                if (translateModel && ext.equals(".json")) {
                    // 翻译模型内容 - 替换所有 bstweaker 命名空间
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                    // 纹理路径: bstweaker:items/xxx -> mujmajnkraftsbettersurvival:items/itemxxx
                    content = content.replace("bstweaker:items/", "mujmajnkraftsbettersurvival:items/item");
                    // 模型路径: bstweaker:item/xxx -> mujmajnkraftsbettersurvival:item/itemxxx
                    content = content.replace("bstweaker:item/", "mujmajnkraftsbettersurvival:item/item");
                    // 通用替换: 任何其他 bstweaker: 引用
                    content = content.replace("bstweaker:", "mujmajnkraftsbettersurvival:");
                    java.nio.file.Files.write(target.toPath(),
                            content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    java.nio.file.Files.copy(file.toPath(), target.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                LOGGER.info("Injected: " + file.getName() + " -> " + targetName);
            } catch (Exception e) {
                LOGGER.error("Failed to copy " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
