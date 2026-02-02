package com.mujmajnkraft.bstweaker.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import fermiumbooter.FermiumRegistryAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
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
        try {
            File mcDir = (File) data.get("mcLocation");
            if (mcDir != null) {
                File configDir = new File(mcDir, "config/bstweaker");
                File modsDir = new File(mcDir, "mods");

                // 方案1: 尝试注入到 BS JAR
                File bsJar = findBSJar(modsDir);
                if (bsJar != null) {
                    injectIntoJar(configDir, bsJar);
                    LOGGER.info("Resources injected into: " + bsJar.getName());
                } else {
                    // 方案2: 如果找不到 BS JAR，创建资源包
                    LOGGER.warn("BetterSurvival JAR not found, falling back to resource pack");
                    createResourcePack(configDir, new File(mcDir, "resourcepacks/bstweaker"));
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Resource injection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 查找 BetterSurvival JAR 文件
     */
    private File findBSJar(File modsDir) {
        if (!modsDir.exists())
            return null;

        File[] jars = modsDir.listFiles((d, n) -> n.toLowerCase().contains("bettersurvival") && n.endsWith(".jar"));

        if (jars != null && jars.length > 0) {
            return jars[0];
        }
        return null;
    }

    /**
     * 注入资源到 BetterSurvival JAR
     */
    private void injectIntoJar(File configDir, File jarFile) {
        if (!configDir.exists()) {
            LOGGER.info("Config dir not found, skipping injection");
            return;
        }

        Map<String, String> env = new HashMap<>();
        env.put("create", "false");

        URI jarUri = URI.create("jar:" + jarFile.toURI());

        try (FileSystem jarFs = FileSystems.newFileSystem(jarUri, env)) {
            // 注入模型
            injectModels(configDir, jarFs);
            // 注入纹理
            injectTextures(configDir, jarFs);

            LOGGER.info("JAR injection completed");
        } catch (Exception e) {
            LOGGER.error("Failed to inject into JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void injectModels(File configDir, FileSystem jarFs) throws IOException {
        File srcDir = new File(configDir, "models");
        if (!srcDir.exists())
            return;

        File[] files = srcDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null)
            return;

        for (File file : files) {
            String targetName = translateFileName(file.getName());
            Path targetPath = jarFs.getPath("assets/" + BS_NAMESPACE + "/models/item/" + targetName);

            // 确保目录存在
            Files.createDirectories(targetPath.getParent());

            // 读取并翻译模型内容
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            content = translateModelContent(content);

            // 写入 JAR
            Files.write(targetPath, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOGGER.info("Injected model: " + file.getName() + " -> " + targetName);
        }
    }

    private void injectTextures(File configDir, FileSystem jarFs) throws IOException {
        File srcDir = new File(configDir, "textures");
        if (!srcDir.exists())
            return;

        File[] files = srcDir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (!file.getName().endsWith(".png") && !file.getName().endsWith(".mcmeta"))
                continue;

            String targetName = translateFileName(file.getName());
            Path targetPath = jarFs.getPath("assets/" + BS_NAMESPACE + "/textures/items/" + targetName);

            // 确保目录存在
            Files.createDirectories(targetPath.getParent());

            // 复制文件到 JAR
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("Injected texture: " + file.getName() + " -> " + targetName);
        }
    }

    /**
     * 翻译文件名: fieryingotnunchaku.json -> itembstweaker_fieryingotnunchaku.json
     */
    private String translateFileName(String fileName) {
        int dotIndex = fileName.indexOf('.');
        String name = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        if (name.startsWith("itembstweaker_")) {
            return fileName;
        }

        if (name.startsWith("item")) {
            return "itembstweaker_" + name.substring(4) + extension;
        }

        return "itembstweaker_" + name + extension;
    }

    /**
     * 翻译模型内容中的命名空间引用
     */
    private String translateModelContent(String content) {
        return content
                .replace("bstweaker:items/", BS_NAMESPACE + ":items/itembstweaker_")
                .replace("bstweaker:item/", BS_NAMESPACE + ":item/itembstweaker_");
    }

    /**
     * 备用方案：创建资源包
     */
    private void createResourcePack(File configDir, File resourcepackDir) throws IOException {
        if (!configDir.exists())
            return;

        File assetsDir = new File(resourcepackDir, "assets/" + BS_NAMESPACE);
        File modelsDir = new File(assetsDir, "models/item");
        File texturesDir = new File(assetsDir, "textures/items");

        modelsDir.mkdirs();
        texturesDir.mkdirs();

        // pack.mcmeta
        File packMcmeta = new File(resourcepackDir, "pack.mcmeta");
        String mcmetaContent = "{\n  \"pack\": {\n    \"pack_format\": 3,\n    \"description\": \"BSTweaker Custom Weapons\"\n  }\n}";
        Files.write(packMcmeta.toPath(), mcmetaContent.getBytes(StandardCharsets.UTF_8));

        // 复制资源
        copyResourceFiles(new File(configDir, "models"), modelsDir, ".json", true);
        copyResourceFiles(new File(configDir, "textures"), texturesDir, ".png", false);
        copyResourceFiles(new File(configDir, "textures"), texturesDir, ".png.mcmeta", false);
    }

    private void copyResourceFiles(File srcDir, File destDir, String ext, boolean translateModel) {
        if (!srcDir.exists())
            return;
        File[] files = srcDir.listFiles((d, n) -> n.endsWith(ext));
        if (files == null)
            return;

        for (File file : files) {
            try {
                String targetName = translateFileName(file.getName());
                File target = new File(destDir, targetName);

                if (translateModel) {
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    content = translateModelContent(content);
                    Files.write(target.toPath(), content.getBytes(StandardCharsets.UTF_8));
                } else {
                    Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
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
