package com.mujmajnkraft.bstweaker.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.mixin.client.MixinModelManagerAccessor;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraft.client.resources.IResourceManager;

/**
 * Fast texture hot-reload by directly updating sprite regions in the texture atlas.
 * This bypasses the slow full-atlas re-stitch process.
 */
public class FastTextureReloader {

    // Static fields for mixin communication (weapon-only fast reload)
    public static volatile boolean weaponOnlyReload = false;
    public static volatile IRegistry<ModelResourceLocation, IBakedModel> oldModelRegistry = null;
    public static volatile java.util.Set<IModel> weaponIModels = null;
    public static volatile java.util.Map<IModel, IBakedModel> oldBakedModelCache = null;

    /**
     * Reload textures for BSTweaker weapons only.
     * Uses TextureUtil.uploadTextureMipmap to update only changed regions.
     */
    public static void reloadWeaponTextures() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            TextureMap textureMap = mc.getTextureMapBlocks();
            
            // CRITICAL: Rescan config files to get latest file list before hot-reload
            DynamicResourcePack.rescan();
            BSTweaker.LOG.info("Rescanned config resources for hot-reload");
            
            Set<Item> weapons = TweakerWeaponInjector.getItemDefinitionMap().keySet();
            if (weapons.isEmpty()) {
                BSTweaker.LOG.info("No weapons to reload textures for");
                return;
            }

            // Debug: verify we have the correct texture atlas
            int atlasId = textureMap.getGlTextureId();
            net.minecraft.client.renderer.texture.ITextureObject boundTex = 
                mc.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
            int locBlocksId = boundTex != null ? boundTex.getGlTextureId() : -1;
            
            BSTweaker.LOG.info("TextureMap glId=" + atlasId + ", LOCATION_BLOCKS_TEXTURE glId=" + locBlocksId +
                ", same object=" + (textureMap == boundTex));
            
            // Bind the texture atlas before uploading
            net.minecraft.client.renderer.GlStateManager.bindTexture(atlasId);
            
            // Debug: list all sprites containing "nunchaku" 
            try {
                java.lang.reflect.Field mapField = TextureMap.class.getDeclaredField("mapUploadedSprites");
                mapField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<String, TextureAtlasSprite> uploadedSprites = (java.util.Map<String, TextureAtlasSprite>) mapField.get(textureMap);
                StringBuilder sb = new StringBuilder("Nunchaku sprites in atlas: ");
                for (String key : uploadedSprites.keySet()) {
                    if (key.toLowerCase().contains("nunchaku")) {
                        sb.append(key).append(", ");
                    }
                }
                BSTweaker.LOG.info(sb.toString());
            } catch (Exception ex) {
                BSTweaker.LOG.warn("Failed to enumerate sprites: " + ex.getMessage());
            }

            int reloaded = 0;
            for (Item item : weapons) {
                if (reloadSpriteForItem(mc, textureMap, item)) {
                    reloaded++;
                }
                // Also reload override textures (e.g., spinning nunchaku)
                reloaded += reloadOverrideSprites(mc, textureMap, item);
            }
            
            // Also reload ALL override textures from DynamicResourcePack (for textures with custom names)
            reloaded += reloadAllOverrideTextures(mc, textureMap);

            BSTweaker.LOG.info("Fast texture reload: updated " + reloaded + " sprites");
        } catch (Exception e) {
            BSTweaker.LOG.error("Fast texture reload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reload override sprites for an item (e.g., spinning nunchaku texture).
     * Sprite lookup uses registryName path, file loading uses textureFileName.
     */
    private static int reloadOverrideSprites(Minecraft mc, TextureMap textureMap, Item item) {
        int reloaded = 0;
        
        // registryName path is what atlas uses (e.g. itemanunchaku)
        String regPath = item.getRegistryName().getPath();
        
        // textureFileName is what config files use (e.g. itema)
        String textureFileName = null;
        com.google.gson.JsonObject def = TweakerWeaponInjector.getDefinition(item);
        if (def != null && def.has("texture")) {
            textureFileName = "item" + def.get("texture").getAsString();
        }
        if (textureFileName == null) {
            textureFileName = regPath;
        }
        
        // Common override suffixes for weapons
        String[] overrideSuffixes = {"spinning", "blocking", "charged", "pulling"};
        
        for (String suffix : overrideSuffixes) {
            // Texture file name (e.g. itemaspinning) - now registered in bstweaker namespace
            String textureFileSuffix = textureFileName + suffix;
            
            BSTweaker.LOG.info("Searching for override texture: " + textureFileSuffix);
            // Now use textureFileName directly since we register spinning textures in bstweaker namespace
            if (reloadSpriteByName(mc, textureMap, textureFileSuffix)) {
                reloaded++;
                continue;
            }
            
            // Also try without "item" prefix for texture file if present
            if (textureFileName.startsWith("item")) {
                String withoutItemTexture = textureFileName.substring(4) + suffix;
                BSTweaker.LOG.info("Also trying texture without 'item' prefix: " + withoutItemTexture);
                if (reloadSpriteByName(mc, textureMap, withoutItemTexture)) {
                    reloaded++;
                }
            }
        }
        return reloaded;
    }
    
    /**
     * Reload all override textures from DynamicResourcePack directly.
     * This handles textures with custom names that don't match weapon registry names.
     * Supports all override suffixes: spinning, blocking, charged, pulling.
     */
    private static int reloadAllOverrideTextures(Minecraft mc, TextureMap textureMap) {
        int reloaded = 0;
        java.util.Set<String> textureNames = DynamicResourcePack.getTextureNames();
        
        // All supported override suffixes
        String[] overrideSuffixes = {"spinning", "blocking", "charged", "pulling"};
        
        for (String textureName : textureNames) {
            // Check if texture name ends with any override suffix
            boolean isOverride = false;
            String matchedSuffix = null;
            for (String suffix : overrideSuffixes) {
                if (textureName.endsWith(suffix)) {
                    isOverride = true;
                    matchedSuffix = suffix;
                    break;
                }
            }
            
            if (isOverride) {
                // Try to reload this override texture
                if (reloadSpriteByName(mc, textureMap, textureName)) {
                    BSTweaker.LOG.info("Reloaded " + matchedSuffix + " texture: " + textureName);
                    reloaded++;
                } else {
                    // Also try with "item" prefix if not present
                    if (!textureName.startsWith("item")) {
                        String withItem = "item" + textureName;
                        if (reloadSpriteByName(mc, textureMap, withItem)) {
                            BSTweaker.LOG.info("Reloaded " + matchedSuffix + " texture (with item prefix): " + withItem);
                            reloaded++;
                        }
                    }
                }
            }
        }
        
        return reloaded;
    }
    
    /**
     * Reload a sprite by name directly.
     */
    private static boolean reloadSpriteByName(Minecraft mc, TextureMap textureMap, String textureName) {
        String[] spriteNames = {
            "mujmajnkraftsbettersurvival:items/" + textureName,
            "bstweaker:items/" + textureName
        };
        
        TextureAtlasSprite sprite = null;
        String matchedName = null;
        
        for (String name : spriteNames) {
            TextureAtlasSprite candidate = textureMap.getAtlasSprite(name);
            if (candidate != null && candidate != textureMap.getMissingSprite()) {
                sprite = candidate;
                matchedName = name;
                break;
            }
        }
        
        if (sprite == null) {
            BSTweaker.LOG.info("Override sprite not found in atlas for: " + textureName + 
                ", tried: " + String.join(", ", spriteNames));
            return false;
        }
        
        // Check if we have a texture file for this
        java.io.File textureFile = DynamicResourcePack.getTextureFile(textureName);
        if (textureFile == null || !textureFile.exists()) {
            BSTweaker.LOG.info("Override texture file not found: " + textureName);
            return false;
        }
        
        BSTweaker.LOG.info("Found override sprite: " + matchedName + " at (" + 
            sprite.getOriginX() + "," + sprite.getOriginY() + ") " +
            sprite.getIconWidth() + "x" + sprite.getIconHeight());
        
        try {
            int width = sprite.getIconWidth();
            int height = sprite.getIconHeight();
            int originX = sprite.getOriginX();
            int originY = sprite.getOriginY();
            
            int[][] newTextureData = loadTextureData(mc, matchedName, width, height);
            if (newTextureData == null) {
                BSTweaker.LOG.warn("Failed to load texture data for override sprite: " + matchedName);
                return false;
            }
            
            // Use Forge's ObfuscationReflectionHelper for automatic SRG mapping
            int glTextureId = -1;
            try {
                java.lang.reflect.Method getGlTextureIdMethod = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findMethod(
                    net.minecraft.client.renderer.texture.AbstractTexture.class, 
                    "func_110552_b",  // SRG name - ObfuscationReflectionHelper maps to dev name automatically
                    int.class  // return type
                );
                glTextureId = (Integer) getGlTextureIdMethod.invoke(textureMap);
            } catch (Exception ex) {
                BSTweaker.LOG.error("Failed to get glTextureId: " + ex.getMessage());
                return false;
            }
            
            // Use GlStateManager.bindTexture directly - it's public
            net.minecraft.client.renderer.GlStateManager.bindTexture(glTextureId);
            
            BSTweaker.LOG.info("Loading override texture: " + textureName);
            
            // CRITICAL: Clear the entire allocated area first to prevent ghost pixels
            // This is needed when new texture is smaller than pre-allocated space (128x128)
            clearSpriteArea(width, height, originX, originY);

            // Upload texture
            TextureUtil.uploadTextureMipmap(newTextureData, width, height, originX, originY, false, false);
            
            // For animated sprites, we need to update framesTextureData
            // The uploaded data is only frame 0; the game calls updateAnimation() to switch frames
            BSTweaker.LOG.info("Checking animation metadata for: " + matchedName + ", hasMetadata=" + sprite.hasAnimationMetadata());
            if (sprite.hasAnimationMetadata()) {
                try {
                    // Use ObfuscationReflectionHelper for SRG name mapping
                    java.lang.reflect.Field framesField = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findField(
                        TextureAtlasSprite.class, "field_110976_a");  // framesTextureData
                    @SuppressWarnings("unchecked")
                    java.util.List<int[][]> framesTextureData = (java.util.List<int[][]>) framesField.get(sprite);
                    
                    int frameCount = framesTextureData.size();
                    BSTweaker.LOG.info("Sprite " + matchedName + " has " + frameCount + " frames in framesTextureData");
                    if (frameCount > 0) {
                        // Load all frames from the animation strip
                        java.io.File animTextureFile = DynamicResourcePack.getTextureFile(textureName);
                        BSTweaker.LOG.info("Animation texture file lookup for '" + textureName + "': " + 
                            (animTextureFile != null ? animTextureFile.getAbsolutePath() : "null"));
                        if (animTextureFile != null && animTextureFile.exists()) {
                            BufferedImage fullImage = ImageIO.read(animTextureFile);
                            
                            // TARGET dimensions: sprite's allocated size in atlas (may be pre-allocated to
                            // 128x128)
                            int targetFrameWidth = width;
                            int targetFrameHeight = height;
                            
                            // SOURCE dimensions: detect from source image (animation strip with square
                            // frames)
                            int srcFrameWidth = fullImage.getWidth();
                            int srcFrameHeight = fullImage.getWidth(); // Assume square frames in source
                            int actualFrameCount = fullImage.getHeight() / srcFrameHeight;

                            boolean needsScale = (srcFrameWidth != targetFrameWidth
                                    || srcFrameHeight != targetFrameHeight);
                            
                            BSTweaker.LOG.info("Loading " + actualFrameCount + " animation frames for: " + textureName + 
                                    " (source: " + srcFrameWidth + "x" + srcFrameHeight + " -> target: "
                                    + targetFrameWidth + "x" + targetFrameHeight +
                                    ", scale: " + needsScale + ")");
                            
                            // Load each frame, scaling if necessary
                            for (int i = 0; i < Math.min(frameCount, actualFrameCount); i++) {
                                // Extract frame from source strip
                                int srcY = i * srcFrameHeight;
                                
                                BufferedImage frameImage;
                                if (!needsScale) {
                                    // Perfect match - just extract
                                    frameImage = fullImage.getSubimage(0, srcY, srcFrameWidth, srcFrameHeight);
                                } else {
                                    // Need to scale from source size to target (atlas) size
                                    frameImage = new BufferedImage(targetFrameWidth, targetFrameHeight,
                                            BufferedImage.TYPE_INT_ARGB);
                                    java.awt.Graphics2D g = frameImage.createGraphics();
                                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                                    g.drawImage(fullImage.getSubimage(0, srcY, srcFrameWidth, srcFrameHeight),
                                            0, 0, targetFrameWidth, targetFrameHeight, null);
                                    g.dispose();
                                }
                                
                                int[] pixels = new int[targetFrameWidth * targetFrameHeight];
                                frameImage.getRGB(0, 0, targetFrameWidth, targetFrameHeight, pixels, 0,
                                        targetFrameWidth);
                                framesTextureData.set(i, new int[][] { pixels });
                            }
                            
                            // CRITICAL: Force GPU upload of updated frame data
                            // After updating framesTextureData, we need to:
                            // 1. Upload the first frame immediately to the atlas
                            // 2. Trigger animation system to use new frames
                            
                            // Upload frame 0 directly to GPU at sprite's atlas position
                            if (framesTextureData.size() > 0 && framesTextureData.get(0) != null) {
                                int[][] frame0 = framesTextureData.get(0);
                                TextureUtil.uploadTextureMipmap(frame0, width, height, originX, originY, false, false);
                                BSTweaker.LOG.info("Uploaded frame 0 to GPU for: " + matchedName);
                            }
                            
                            // Force animation to restart from frame 0
                            try {
                                java.lang.reflect.Field tickCounterField = TextureAtlasSprite.class.getDeclaredField("tickCounter");
                                tickCounterField.setAccessible(true);
                                tickCounterField.setInt(sprite, 0);
                                
                                java.lang.reflect.Field frameCounterField = TextureAtlasSprite.class.getDeclaredField("frameCounter");
                                frameCounterField.setAccessible(true);
                                frameCounterField.setInt(sprite, 0);
                            } catch (Exception resetEx) {
                                BSTweaker.LOG.debug("Could not reset animation counters: " + resetEx.getMessage());
                            }
                            
                            BSTweaker.LOG.info("Updated " + Math.min(frameCount, actualFrameCount) + " animation frames for override: " + matchedName);
                        } else {
                            BSTweaker.LOG.warn("Animation texture file not found or doesn't exist: " + textureName);
                        }
                    }
                } catch (Exception ex) {
                    BSTweaker.LOG.warn("Failed to update animation frames: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                BSTweaker.LOG.info("Sprite " + matchedName + " has NO animation metadata - cannot hot-reload animation");
            }
            
            org.lwjgl.opengl.GL11.glFinish();
            return true;
        } catch (Exception e) {
            BSTweaker.LOG.warn("Failed to reload override sprite " + textureName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Reload an override sprite with separate sprite name and texture file name.
     * @param spriteName The sprite name in atlas (e.g. itemanunchakuspinning)
     * @param textureFileName The texture file name (e.g. itemaspinning)
     */
    private static boolean reloadOverrideSpriteWithMapping(Minecraft mc, TextureMap textureMap, 
            String spriteName, String textureFileName) {
        // Try to find sprite in atlas using spriteName (registryName-based)
        String[] spriteNames = {
            "mujmajnkraftsbettersurvival:items/" + spriteName,
            "bstweaker:items/" + spriteName
        };
        
        TextureAtlasSprite sprite = null;
        String matchedSpriteName = null;
        
        for (String name : spriteNames) {
            TextureAtlasSprite candidate = textureMap.getAtlasSprite(name);
            if (candidate != null && candidate != textureMap.getMissingSprite()) {
                sprite = candidate;
                matchedSpriteName = name;
                break;
            }
        }
        
        if (sprite == null) {
            BSTweaker.LOG.info("Override sprite not found in atlas for: " + spriteName + 
                ", tried: " + String.join(", ", spriteNames));
            return false;
        }
        
        // Load texture file using textureFileName
        java.io.File textureFile = DynamicResourcePack.getTextureFile(textureFileName);
        if (textureFile == null || !textureFile.exists()) {
            BSTweaker.LOG.info("Override texture file not found: " + textureFileName + 
                " (sprite: " + matchedSpriteName + ")");
            return false;
        }
        
        BSTweaker.LOG.info("Found override sprite: " + matchedSpriteName + " -> loading file: " + textureFileName);
        
        // Delegate to the existing reloadSpriteByName logic but with the correct file
        // For simplicity, just call the underlying reload with textureFileName
        return reloadSpriteByName(mc, textureMap, textureFileName);
    }

    /**
     * Reload a single sprite for an item.
     * Sprite lookup uses registryName (what atlas uses).
     * Texture file loading uses texture field from weapon definition.
     */
    private static boolean reloadSpriteForItem(Minecraft mc, TextureMap textureMap, Item item) {
        try {
            // Get texture filename from weapon definition (for loading the file)
            String textureFileName = null;
            com.google.gson.JsonObject def = TweakerWeaponInjector.getDefinition(item);
            if (def != null && def.has("texture")) {
                textureFileName = "item" + def.get("texture").getAsString();
            }
            if (textureFileName == null) {
                textureFileName = item.getRegistryName().getPath();
            }
            
            // Sprite names in atlas use registryName path (e.g. itemanunchaku), not texture field
            String regPath = item.getRegistryName().getPath();
            String[] spriteNames = {
                // Primary: BS namespace with registryName path (what ModelBakery uses)
                "mujmajnkraftsbettersurvival:items/" + regPath,
                item.getRegistryName().getNamespace() + ":items/" + regPath,
                // Fallback: try with texture filename
                "mujmajnkraftsbettersurvival:items/" + textureFileName,
                "bstweaker:items/" + textureFileName
            };
            
            TextureAtlasSprite sprite = null;
            String matchedSpriteName = null;
            
            for (String name : spriteNames) {
                TextureAtlasSprite candidate = textureMap.getAtlasSprite(name);
                if (candidate != null && candidate != textureMap.getMissingSprite()) {
                    sprite = candidate;
                    matchedSpriteName = name;
                    break;
                }
            }
            
            if (sprite == null) {
                BSTweaker.LOG.debug("No sprite found for " + item.getRegistryName() + 
                    " (textureFile=" + textureFileName + "), tried: " + String.join(", ", spriteNames));
                return false;
            }
            
            BSTweaker.LOG.info("Found sprite: " + matchedSpriteName + " at (" + 
                sprite.getOriginX() + "," + sprite.getOriginY() + ") " +
                sprite.getIconWidth() + "x" + sprite.getIconHeight());

            // Get sprite location in the atlas
            int originX = sprite.getOriginX();
            int originY = sprite.getOriginY();
            int width = sprite.getIconWidth();
            int height = sprite.getIconHeight();

            // Upload the new texture data to the GPU at the sprite's position
            // Use textureFileName to load the file, not sprite name
            int[][] newTextureData = loadTextureData(mc, textureFileName, width, height);
            if (newTextureData == null) {
                BSTweaker.LOG.warn("Failed to load texture data for: " + textureFileName + " (sprite: " + matchedSpriteName + ")");
                return false;
            }
            
            // Use the EXACT same binding method as TextureMap.updateAnimations()
            // getGlTextureId() is protected, so we use reflection
            int glTextureId = -1;
            try {
                java.lang.reflect.Method getGlTextureIdMethod = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findMethod(
                    net.minecraft.client.renderer.texture.AbstractTexture.class, 
                    "func_110552_b",  // SRG name - ObfuscationReflectionHelper maps to dev name automatically
                    int.class  // return type
                );
                glTextureId = (Integer) getGlTextureIdMethod.invoke(textureMap);
            } catch (Exception ex) {
                BSTweaker.LOG.error("Failed to get glTextureId via reflection: " + ex.getMessage());
                return false;
            }
            // Use GlStateManager.bindTexture directly - it's public
            net.minecraft.client.renderer.GlStateManager.bindTexture(glTextureId);
            
            // Debug: print first pixel color
            int firstPixel = newTextureData[0][0];
            int a = (firstPixel >> 24) & 0xFF;
            int r = (firstPixel >> 16) & 0xFF;
            int g = (firstPixel >> 8) & 0xFF;
            int b = firstPixel & 0xFF;
            
            BSTweaker.LOG.info("Uploading to atlas at (" + originX + "," + originY + ") size " + width + "x" + height +
                " pixels: " + newTextureData[0].length + 
                " firstPixel ARGB=(" + a + "," + r + "," + g + "," + b + ")");

            // Use Minecraft's TextureUtil - it handles pixel format correctly
            TextureUtil.uploadTextureMipmap(
                newTextureData,
                width, height,
                originX, originY,
                false, false  // blur, clamp
            );
            
            // For animated sprites, update the framesTextureData via reflection
            // This ensures that animated textures continue to display the new texture
            if (sprite.hasAnimationMetadata()) {
                try {
                    java.lang.reflect.Field framesField = TextureAtlasSprite.class.getDeclaredField("framesTextureData");
                    framesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.List<int[][]> framesTextureData = (java.util.List<int[][]>) framesField.get(sprite);
                    
                    // Replace all animation frames with the new texture data
                    // For animated sprites, we need to load all frames from the sprite sheet
                    int frameCount = framesTextureData.size();
                    if (frameCount > 0) {
                        // For now, just update all frames with the new data
                        // A full implementation would parse the sprite sheet for each frame
                        for (int i = 0; i < frameCount; i++) {
                            framesTextureData.set(i, newTextureData);
                        }
                        BSTweaker.LOG.info("Updated " + frameCount + " animation frames for: " + matchedSpriteName);
                    }
                } catch (Exception ex) {
                    BSTweaker.LOG.warn("Failed to update animation frames: " + ex.getMessage());
                }
            }
            
            // Force GPU to complete all pending operations
            org.lwjgl.opengl.GL11.glFinish();
            
            // Check for GL errors
            int error = org.lwjgl.opengl.GL11.glGetError();
            if (error != 0) {
                BSTweaker.LOG.warn("GL Error after texture upload: " + error);
            }

            return true;
        } catch (Exception e) {
            BSTweaker.LOG.warn("Failed to reload sprite for " + item.getRegistryName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Load texture data from the resource pack.
     * @param spriteName Sprite name like "mujmajnkraftsbettersurvival:items/itememeralddagger"
     * Returns int[][] where [0] is the base mipmap level pixels.
     */
    private static int[][] loadTextureData(Minecraft mc, String spriteName, int expectedWidth, int expectedHeight) {
        try {
            // Extract texture name from sprite name
            // Sprite: "namespace:items/itemname" -> textureName: "itemname"
            // OR plain filename: "itemname" -> textureName: "itemname"
            int colonIdx = spriteName.indexOf(':');
            String textureName;
            if (colonIdx == -1) {
                // Plain filename format (e.g., "itememeraldnunchaku")
                textureName = spriteName;
            } else {
                // Sprite name format (e.g., "mujmajnkraftsbettersurvival:items/itemname")
                String path = spriteName.substring(colonIdx + 1);
                textureName = path;
                if (path.startsWith("items/")) {
                    textureName = path.substring(6); // Remove "items/" prefix
                }
            }
            
            // First: try to get texture file from DynamicResourcePack (config/bstweaker or resources dir)
            BSTweaker.LOG.info("Looking up texture: '" + textureName + "' in configTextures (size=" + 
                DynamicResourcePack.getTextureNames().size() + ")");
            java.io.File textureFile = DynamicResourcePack.getTextureFile(textureName);
            if (textureFile == null) {
                BSTweaker.LOG.info("getTextureFile returned null for: " + textureName);
            }
            
            InputStream stream = null;
            if (textureFile != null && textureFile.exists()) {
                // Log file modification time to debug caching issues
                long lastModified = textureFile.lastModified();
                long fileSize = textureFile.length();
                BSTweaker.LOG.info("Loading texture from file: " + textureFile.getAbsolutePath() + 
                    ", lastModified=" + lastModified + ", size=" + fileSize);
                stream = new java.io.FileInputStream(textureFile);

            } else if (colonIdx != -1) {
                // Fallback to ResourceManager (for mod's built-in textures)
                // Only works if we have a full sprite name with namespace
                String path = spriteName.substring(colonIdx + 1);
                if (path.startsWith("items/")) {
                    path = path.substring(6);
                }
                String namespace = spriteName.substring(0, colonIdx);
                ResourceLocation texLoc = new ResourceLocation(namespace, "textures/items/" + path + ".png");
                try {
                    stream = mc.getResourceManager().getResource(texLoc).getInputStream();
                    BSTweaker.LOG.info("Loading texture from ResourceManager: " + texLoc);
                } catch (Exception e) {
                    BSTweaker.LOG.debug("Texture not found for " + textureName);
                    return null;
                }
            } else {
                // Plain filename with no file found - give up
                BSTweaker.LOG.debug("No texture file found for plain filename: " + textureName);
                return null;
            }

            if (stream == null) {
                return null;
            }

            // Disable ImageIO caching to ensure fresh reads
            javax.imageio.ImageIO.setUseCache(false);
            
            BufferedImage image = ImageIO.read(stream);
            stream.close();

            if (image == null) {
                return null;
            }

            // Determine source texture dimensions and detect animation strips
            int srcWidth = image.getWidth();
            int srcHeight = image.getHeight();

            // Animation strip detection: height is multiple of width (square frames stacked
            // vertically)
            boolean isAnimationStrip = (srcWidth > 0 && srcHeight > srcWidth && srcHeight % srcWidth == 0);
            int srcFrameHeight = isAnimationStrip ? srcWidth : srcHeight;

            if (isAnimationStrip) {
                int frameCount = srcHeight / srcFrameHeight;
                BSTweaker.LOG.info("Detected animation strip for " + spriteName +
                        ": " + srcWidth + "x" + srcHeight + ", " + frameCount + " frames (frame size: " + srcWidth + "x"
                        + srcFrameHeight + ")");
            }

            // Need to scale if source dimensions don't match expected (pre-allocated)
            // dimensions
            boolean needsScale = (srcWidth != expectedWidth || srcFrameHeight != expectedHeight);

            if (needsScale) {
                BSTweaker.LOG.info("Scaling texture for " + spriteName +
                        " from " + srcWidth + "x" + srcFrameHeight + " to " + expectedWidth + "x" + expectedHeight +
                        (isAnimationStrip ? " (animation strip)" : ""));

                // Create scaled image - for animations, scale each frame
                int scaledTotalHeight = isAnimationStrip ? expectedHeight * (srcHeight / srcFrameHeight)
                        : expectedHeight;
                java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(
                        expectedWidth, scaledTotalHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

                if (isAnimationStrip) {
                    // Scale each frame individually
                    int frameCount = srcHeight / srcFrameHeight;
                    for (int i = 0; i < frameCount; i++) {
                        int srcY = i * srcFrameHeight;
                        int dstY = i * expectedHeight;
                        g.drawImage(image,
                                0, dstY, expectedWidth, dstY + expectedHeight, // dest
                                0, srcY, srcWidth, srcY + srcFrameHeight, // src
                                null);
                    }
                } else {
                    g.drawImage(image, 0, 0, expectedWidth, expectedHeight, null);
                }
                g.dispose();
                image = scaled;
            }

            // Convert to int[] pixel array (ARGB format) - just first frame
            int[] pixels = new int[expectedWidth * expectedHeight];
            image.getRGB(0, 0, expectedWidth, expectedHeight, pixels, 0, expectedWidth);

            // Return as mipmap array (only base level for now)
            int[][] mipmapData = new int[1][];
            mipmapData[0] = pixels;

            return mipmapData;
        } catch (Exception e) {
            BSTweaker.LOG.warn("Failed to load texture data for " + spriteName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Reload models for BSTweaker weapons only (fast path).
     * Calls ModelManager.onResourceManagerReload() but mixins intercept to:
     * 1. Skip baking non-weapon models (return old cached baked models)
     * 2. Skip texture atlas re-stitching (handled separately)
     * Result: only weapon models are re-baked through Forge's full pipeline.
     */
    public static void reloadModels() {
        try {
            long start = System.currentTimeMillis();
            Minecraft mc = Minecraft.getMinecraft();

            // Rescan config resources to pick up modified model JSON files
            DynamicResourcePack.rescan();

            // Set flag — MixinModelManagerFastReload will cancel full reload,
            // read weapon JSON fresh, create VanillaModelWrapper, bake, update registry
            weaponOnlyReload = true;
            try {
                ModelManager modelManager = mc.getRenderItem().getItemModelMesher().getModelManager();
                modelManager.onResourceManagerReload(mc.getResourceManager());
            } finally {
                weaponOnlyReload = false;
            }

            // Rebuild ItemModelMesher cache
            mc.getRenderItem().getItemModelMesher().rebuildCache();

            long elapsed = System.currentTimeMillis() - start;
            BSTweaker.LOG.info("Fast model reload (weapon-only bake): " + elapsed + "ms");
        } catch (Exception e) {
            BSTweaker.LOG.error("Fast model reload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Resolve the parent chain of a ModelBlock by reading parent JSONs
     * from the ResourceManager. Public so MixinModelManagerFastReload can use it.
     */
    public static void resolveParents(net.minecraft.client.renderer.block.model.ModelBlock model,
            IResourceManager resourceManager) {
        net.minecraft.client.renderer.block.model.ModelBlock current = model;
        java.util.Set<String> visited = new java.util.HashSet<>();

        while (current.getParentLocation() != null) {
            ResourceLocation parentLoc = current.getParentLocation();
            String parentStr = parentLoc.toString();

            if (visited.contains(parentStr))
                break;
            visited.add(parentStr);

            // builtin/generated: set parent to actual ModelBakery.MODEL_GENERATED instance
            // VanillaModelWrapper uses identity check (==) against this field
            if (parentLoc.getPath().equals("builtin/generated") || parentLoc.getPath().equals("item/generated")) {
                try {
                    java.lang.reflect.Field f = net.minecraft.client.renderer.block.model.ModelBakery.class
                            .getDeclaredField("MODEL_GENERATED");
                    f.setAccessible(true);
                    net.minecraft.client.renderer.block.model.ModelBlock modelGenerated = (net.minecraft.client.renderer.block.model.ModelBlock) f
                            .get(null);
                    current.parent = modelGenerated;
                } catch (Exception e) {
                    BSTweaker.LOG.warn("Failed to get MODEL_GENERATED: " + e.getMessage());
                }
                break;
            }

            // builtin/entity is terminal
            if (parentLoc.getPath().equals("builtin/entity")) {
                try {
                    java.lang.reflect.Field f = net.minecraft.client.renderer.block.model.ModelBakery.class
                            .getDeclaredField("MODEL_ENTITY");
                    f.setAccessible(true);
                    net.minecraft.client.renderer.block.model.ModelBlock modelEntity = (net.minecraft.client.renderer.block.model.ModelBlock) f
                            .get(null);
                    current.parent = modelEntity;
                } catch (Exception e) {
                    BSTweaker.LOG.warn("Failed to get MODEL_ENTITY: " + e.getMessage());
                }
                break;
            }

            ResourceLocation parentJsonLoc;
            String path = parentLoc.getPath();
            if (path.startsWith("models/")) {
                parentJsonLoc = new ResourceLocation(parentLoc.getNamespace(), path + ".json");
            } else {
                parentJsonLoc = new ResourceLocation(parentLoc.getNamespace(), "models/" + path + ".json");
            }

            try (java.io.InputStream is = resourceManager.getResource(parentJsonLoc).getInputStream();
                    java.io.InputStreamReader reader = new java.io.InputStreamReader(is,
                            java.nio.charset.StandardCharsets.UTF_8)) {
                net.minecraft.client.renderer.block.model.ModelBlock parent = net.minecraft.client.renderer.block.model.ModelBlock
                        .deserialize(reader);
                parent.name = parentJsonLoc.toString();
                current.parent = parent;
                current = parent;
            } catch (Exception e) {
                BSTweaker.LOG.warn("Failed to load parent model " + parentJsonLoc + ": " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Clear the sprite area with transparent pixels.
     * This prevents ghost pixels when new texture is smaller than allocated space.
     * 
     * @param width   Sprite width in atlas
     * @param height  Sprite height in atlas
     * @param originX X origin in atlas
     * @param originY Y origin in atlas
     */
    private static void clearSpriteArea(int width, int height, int originX, int originY) {
        // Create transparent pixel array
        int totalPixels = width * height;
        int[] clearPixels = new int[totalPixels];
        // All zeros = fully transparent black (ARGB = 0x00000000)

        // Wrap in mipmap format (just level 0)
        int[][] clearData = new int[1][];
        clearData[0] = clearPixels;

        // Upload transparent area to GPU
        TextureUtil.uploadTextureMipmap(clearData, width, height, originX, originY, false, false);
        BSTweaker.LOG.debug("Cleared sprite area {}x{} at ({},{})", width, height, originX, originY);
    }
}
