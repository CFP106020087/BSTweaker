package com.mujmajnkraft.bstweaker.mixin.client;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Pre-allocate extra atlas space for BSTweaker weapon sprites.
 * 
 * This Mixin hooks loadSpriteFrames (AFTER frame data is loaded) and:
 * 1. Pads each frame's pixel data from original size to MAX_SIZE
 * 2. Updates width/height so Stitcher allocates larger space
 * 
 * Only applies to weapons registered in weapons.json.
 */
@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSpritePreAlloc {

    private static final int MAX_SPRITE_SIZE = 64;

    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Shadow
    protected List<int[][]> framesTextureData;

    @Shadow
    public abstract String getIconName();

    /**
     * After loadSpriteFrames loads frame data, pad frames and enlarge dimensions.
     * ONLY applies to animation strips (multiple frames), NOT static textures.
     */
    @Inject(method = "loadSpriteFrames", at = @At("RETURN"))
    private void onLoadSpriteFramesReturn(IResource resource, int mipmapLevels, CallbackInfo ci) {
        String iconName = this.getIconName();

        // Only apply to STATIC textures (single frame) from weapons.json
        // Animation strips (multiple frames) keep original size - MC handles animation
        // natively
        // Pre-allocating animated sprites causes rendering issues (texture appears tiny
        // because actual pixels occupy only a small corner of the padded 128x128 area)
        boolean isStaticTexture = framesTextureData != null && framesTextureData.size() == 1;
        if (iconName != null && isStaticTexture && isBSTweakerWeaponSprite(iconName)) {
            int originalWidth = this.width;
            int originalHeight = this.height;

            // Skip if already at or above MAX_SIZE
            if (originalWidth >= MAX_SPRITE_SIZE && originalHeight >= MAX_SPRITE_SIZE) {
                return;
            }

            int newWidth = Math.max(originalWidth, MAX_SPRITE_SIZE);
            int newHeight = Math.max(originalHeight, MAX_SPRITE_SIZE);

            try {
                // Pad each frame's pixel data
                for (int frameIdx = 0; frameIdx < framesTextureData.size(); frameIdx++) {
                    int[][] frameData = framesTextureData.get(frameIdx);
                    if (frameData == null)
                        continue;

                    int[][] paddedFrameData = new int[frameData.length][];

                    for (int mipLevel = 0; mipLevel < frameData.length; mipLevel++) {
                        int[] originalPixels = frameData[mipLevel];
                        if (originalPixels == null)
                            continue;

                        // Calculate mipmap dimensions
                        int mipOrigWidth = originalWidth >> mipLevel;
                        int mipOrigHeight = originalHeight >> mipLevel;
                        int mipNewWidth = newWidth >> mipLevel;
                        int mipNewHeight = newHeight >> mipLevel;

                        if (mipOrigWidth <= 0)
                            mipOrigWidth = 1;
                        if (mipOrigHeight <= 0)
                            mipOrigHeight = 1;
                        if (mipNewWidth <= 0)
                            mipNewWidth = 1;
                        if (mipNewHeight <= 0)
                            mipNewHeight = 1;

                        // Create padded pixel array (transparent by default)
                        int[] paddedPixels = new int[mipNewWidth * mipNewHeight];

                        // Copy original pixels to top-left corner
                        for (int y = 0; y < mipOrigHeight && y < mipNewHeight; y++) {
                            for (int x = 0; x < mipOrigWidth && x < mipNewWidth; x++) {
                                int srcIdx = y * mipOrigWidth + x;
                                int dstIdx = y * mipNewWidth + x;
                                if (srcIdx < originalPixels.length && dstIdx < paddedPixels.length) {
                                    paddedPixels[dstIdx] = originalPixels[srcIdx];
                                }
                            }
                        }

                        paddedFrameData[mipLevel] = paddedPixels;
                    }

                    framesTextureData.set(frameIdx, paddedFrameData);
                }

                // Update dimensions for Stitcher
                this.width = newWidth;
                this.height = newHeight;

                BSTweaker.LOG.info("Pre-allocated sprite {} from {}x{} to {}x{} ({} frames)",
                        iconName, originalWidth, originalHeight, newWidth, newHeight, framesTextureData.size());

            } catch (Exception e) {
                BSTweaker.LOG.warn("Failed to pre-allocate sprite {}: {}", iconName, e.getMessage());
                // Restore original dimensions on failure
                this.width = originalWidth;
                this.height = originalHeight;
            }
        }
    }

    /**
     * Check if this sprite is a BSTweaker weapon from weapons.json.
     */
    private boolean isBSTweakerWeaponSprite(String iconName) {
        // Match bstweaker namespace items
        if (iconName.startsWith("bstweaker:items/")) {
            return isRegisteredWeaponTexture(iconName);
        }

        // Match BS namespace items
        if (iconName.startsWith("mujmajnkraftsbettersurvival:items/")) {
            return isRegisteredWeaponTexture(iconName);
        }

        return false;
    }

    /**
     * Check if texture name matches a registered weapon from weapons.json.
     */
    private boolean isRegisteredWeaponTexture(String iconName) {
        try {
            // Extract texture name from path
            String textureName = iconName;
            if (textureName.contains("/")) {
                textureName = textureName.substring(textureName.lastIndexOf('/') + 1);
            }

            java.util.Set<net.minecraft.item.Item> weapons = com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector
                    .getItemDefinitionMap().keySet();

            for (net.minecraft.item.Item weapon : weapons) {
                net.minecraft.util.ResourceLocation regName = weapon.getRegistryName();
                if (regName != null) {
                    String baseName = regName.getPath();
                    String expectedTexture = "item" + baseName;
                    String expectedSpinning = expectedTexture + "spinning";

                    if (textureName.equals(expectedTexture) ||
                            textureName.equals(expectedSpinning) ||
                            textureName.equals(baseName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Weapon injector not ready yet, or error during check
        }

        return false;
    }
}
