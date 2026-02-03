package com.mujmajnkraft.bstweaker.client;

import com.google.common.collect.ImmutableSet;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic resource pack - loads textures, models, mcmeta, lang from config dir.
 */
public class DynamicResourcePack implements IResourcePack {

    private static final String BS_NAMESPACE = "mujmajnkraftsbettersurvival";

    private static final Map<String, String> dynamicModels = new HashMap<>();
    private static final Map<String, File> configTextures = new HashMap<>();
    private static final Map<String, File> configModels = new HashMap<>();
    private static final Map<String, File> configLangs = new HashMap<>();
    private static final Map<String, File> configMcmeta = new HashMap<>();

    private static File configDir;

    static {
        configDir = new File(Loader.instance().getConfigDir(), "bstweaker");
    }

    /** Rescan config resources for hot-reload. */
    public static void rescan() {
        dynamicModels.clear();
        configTextures.clear();
        configModels.clear();
        configLangs.clear();
        configMcmeta.clear();
        scanConfigResources();
    }

    /** Get texture locations for cache invalidation. */
    public static java.util.Set<net.minecraft.util.ResourceLocation> getTextureLocations() {
        java.util.Set<net.minecraft.util.ResourceLocation> locations = new java.util.HashSet<>();
        for (String name : configTextures.keySet()) {
            locations.add(new net.minecraft.util.ResourceLocation(Reference.MOD_ID, "textures/items/" + name + ".png"));
            if (!name.startsWith("itembstweaker_")) {
                locations.add(new net.minecraft.util.ResourceLocation(Reference.MOD_ID,
                        "textures/items/itembstweaker_" + name + ".png"));
            }
        }
        return locations;
    }

    /** Scan config directory for resources. */
    public static void scanConfigResources() {
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
                    "Place custom model .json files here (optional).\nIf not provided, a default handheld model will be generated.");
        }
        if (!langDir.exists()) {
            langDir.mkdirs();
            createReadme(langDir,
                    "Place language .lang files here (e.g., en_us.lang, zh_cn.lang).\nFormat: item.bstweaker.weapon_id.name=Display Name");
        }


        if (texturesDir.exists() && texturesDir.listFiles() != null) {
            for (File file : texturesDir.listFiles()) {
                if (file.getName().endsWith(".png")) {
                    String name = file.getName().replace(".png", "");
                    configTextures.put(name, file);
                } else if (file.getName().endsWith(".mcmeta")) {
                    String name = file.getName().replace(".png.mcmeta", "");
                    configMcmeta.put(name, file);
                }
            }
        }

        // Auto-convert BS model format if enabled
        if (com.mujmajnkraft.bstweaker.config.BSTweakerConfig.enableModelAutoConvert) {
            convertBSModels(modelsDir);
        }

        if (modelsDir.exists() && modelsDir.listFiles() != null) {
            for (File file : modelsDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    String name = file.getName().replace(".json", "");
                    configModels.put(name, file);
                }
            }
        }


        if (langDir.exists() && langDir.listFiles() != null) {
            for (File file : langDir.listFiles()) {
                if (file.getName().endsWith(".lang")) {
                    String name = file.getName().replace(".lang", "");
                    configLangs.put(name, file);
                }
            }
        }
    }
    
    /** Create README file explaining directory purpose. */
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
     * Convert BetterSurvival model format to BSTweaker format.
     * - Detect *_normal.json and *_spinning.json
     * - Replace mujmajnkraftsbettersurvival: -> bstweaker:
     * - Rename: xxx_normal.json -> xxx.json, xxx_spinning.json -> xxxspinning.json
     * - Backup originals to model backup directory
     */
    private static void convertBSModels(File modelsDir) {
        if (!modelsDir.exists())
            return;

        File backupDir = new File(modelsDir, "model backup");
        File[] files = modelsDir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            String name = file.getName();
            if (!name.endsWith(".json"))
                continue;

            // Detect BS format: xxx_normal.json or xxx_spinning.json
            String newName = null;
            if (name.endsWith("_normal.json")) {
                // xxx_normal.json -> xxx.json
                newName = name.replace("_normal.json", ".json");
            } else if (name.endsWith("_spinning.json")) {
                // xxx_spinning.json -> xxxspinning.json
                newName = name.replace("_spinning.json", "spinning.json");
            }

            if (newName == null)
                continue;

            try {
                // Read content
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

                // Check for BS path format
                if (!content.contains("mujmajnkraftsbettersurvival:")) {
                    continue; // Not BS format, skip
                }

                // Replace path
                String newContent = content.replace("mujmajnkraftsbettersurvival:", Reference.MOD_ID + ":");

                // Create backup directory
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                // Backup original file
                File backupFile = new File(backupDir, name);
                Files.copy(file.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Write new content to new filename
                File newFile = new File(modelsDir, newName);
                Files.write(newFile.toPath(), newContent.getBytes(StandardCharsets.UTF_8));

                // Delete original file
                file.delete();

            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to convert BS model " + name + ": " + e.getMessage());
            }
        }
    }

    /** Register dynamic model. */
    public static void registerModel(String textureName) {
        // Use custom model from config directory if exists
        if (configModels.containsKey(textureName)) {
            try {
                String content = new String(Files.readAllBytes(configModels.get(textureName).toPath()),
                        StandardCharsets.UTF_8);
                String modelPath = "assets/" + Reference.MOD_ID + "/models/item/" + textureName + ".json";
                dynamicModels.put(modelPath, content);
                return;
            } catch (IOException e) {
                BSTweaker.LOG.error("Failed to load config model: " + e.getMessage());
            }
        }

        // Otherwise generate default model
        String modelPath = "assets/" + Reference.MOD_ID + "/models/item/" + textureName + ".json";
        String modelContent = generateModelJson(textureName);
        dynamicModels.put(modelPath, modelContent);
    }
    
    /** Generate model JSON content. */
    private static String generateModelJson(String textureName) {
        return "{\n" +
               "  \"parent\": \"item/handheld\",\n" +
               "  \"textures\": {\n" +
               "    \"layer0\": \"" + Reference.MOD_ID + ":items/" + textureName + "\"\n" +
               "  }\n" +
               "}";
    }
    
    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        String namespace = location.getNamespace();
        String path = location.getPath();

        // Check if namespace is supported (bstweaker or mujmajnkraftsbettersurvival)
        if (!Reference.MOD_ID.equals(namespace) && !BS_NAMESPACE.equals(namespace)) {
            throw new FileNotFoundException("Resource not found: " + location);
        }

        // Check dynamically generated models
        String fullPath = "assets/" + namespace + "/" + path;
        String content = dynamicModels.get(fullPath);
        if (content != null) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        // Check config models - supports models/ and models/item/ formats
        if (path.endsWith(".json") && (path.startsWith("models/") || path.startsWith("models/item/"))) {
            String name;
            if (path.startsWith("models/item/")) {
                name = path.replace("models/item/", "").replace(".json", "");
            } else {
                name = path.replace("models/", "").replace(".json", "");
            }
            // Direct match
            if (configModels.containsKey(name)) {
                return new FileInputStream(configModels.get(name));
            }
            // Try removing itembstweaker_ prefix
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                if (configModels.containsKey(shortName)) {
                    return new FileInputStream(configModels.get(shortName));
                }
            }
        }

        // Check config textures
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");
            // Direct match
            if (configTextures.containsKey(name)) {
                return new FileInputStream(configTextures.get(name));
            }
            // Try removing itembstweaker_ prefix
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                if (configTextures.containsKey(shortName)) {
                    return new FileInputStream(configTextures.get(shortName));
                }
            }
            // Reverse match: request without prefix, file with prefix
            String prefixedName = "itembstweaker_" + name;
            if (configTextures.containsKey(prefixedName)) {
                return new FileInputStream(configTextures.get(prefixedName));
            }
        }

        // Check config mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            if (configMcmeta.containsKey(name)) {
                return new FileInputStream(configMcmeta.get(name));
            }
            // Reverse match
            String prefixedName = "itembstweaker_" + name;
            if (configMcmeta.containsKey(prefixedName)) {
                return new FileInputStream(configMcmeta.get(prefixedName));
            }
        }

        // Check config lang files
        if (path.startsWith("lang/") && path.endsWith(".lang")) {
            String name = path.replace("lang/", "").replace(".lang", "");
            if (configLangs.containsKey(name)) {
                return new FileInputStream(configLangs.get(name));
            }
        }

        throw new FileNotFoundException("Resource not found: " + location);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        String namespace = location.getNamespace();
        String path = location.getPath();

        // Check if namespace is supported
        if (!Reference.MOD_ID.equals(namespace) && !BS_NAMESPACE.equals(namespace)) {
            return false;
        }

        // Check dynamic models
        String fullPath = "assets/" + namespace + "/" + path;
        if (dynamicModels.containsKey(fullPath)) {
            return true;
        }

        // Check config models - supports models/xxx.json and models/item/xxx.json
        if (path.endsWith(".json") && (path.startsWith("models/") || path.startsWith("models/item/"))) {
            String name;
            if (path.startsWith("models/item/")) {
                name = path.replace("models/item/", "").replace(".json", "");
            } else {
                name = path.replace("models/", "").replace(".json", "");
            }

            if (configModels.containsKey(name)) {
                return true;
            }
            // Try removing itembstweaker_ prefix
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                if (configModels.containsKey(shortName)) {
                    return true;
                }
            }
        }

        // Check config textures
        if (path.startsWith("textures/items/") && path.endsWith(".png")) {
            String name = path.replace("textures/items/", "").replace(".png", "");

            // Direct match
            if (configTextures.containsKey(name)) {
                return true;
            }
            // Try removing itembstweaker_ prefix (request has prefix, file doesn't)
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                if (configTextures.containsKey(shortName)) {
                    return true;
                }
            }
            // Reverse match: request without prefix, file with prefix
            String prefixedName = "itembstweaker_" + name;
            if (configTextures.containsKey(prefixedName)) {
                return true;
            }
        }

        // Check config mcmeta
        if (path.startsWith("textures/items/") && path.endsWith(".png.mcmeta")) {
            String name = path.replace("textures/items/", "").replace(".png.mcmeta", "");
            if (configMcmeta.containsKey(name)) {
                return true;
            }
            // Try removing itembstweaker_ prefix
            if (name.startsWith("itembstweaker_")) {
                String shortName = name.substring(14);
                if (configMcmeta.containsKey(shortName)) {
                    return true;
                }
            }
            // Reverse match
            String prefixedName = "itembstweaker_" + name;
            if (configMcmeta.containsKey(prefixedName)) {
                return true;
            }
        }

        // Check config lang files
        if (path.startsWith("lang/") && path.endsWith(".lang")) {
            String name = path.replace("lang/", "").replace(".lang", "");
            return configLangs.containsKey(name);
        }

        return false;
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of(Reference.MOD_ID, BS_NAMESPACE);
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return null;
    }

    @Override
    public String getPackName() {
        return Reference.MOD_ID + "_dynamic";
    }
}
