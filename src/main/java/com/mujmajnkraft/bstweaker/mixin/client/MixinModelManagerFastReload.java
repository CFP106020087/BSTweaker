package com.mujmajnkraft.bstweaker.mixin.client;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.client.FastTextureReloader;
import com.mujmajnkraft.bstweaker.client.OverridePreservingModel;
import com.mujmajnkraft.bstweaker.client.SmartOverrideList;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * Cancels ModelManager.onResourceManagerReload() during fast reload.
 * Uses ItemLayerModel to bake ALL weapon model variants (normal, spinning,
 * etc.)
 * with correct display transforms. Rebuilds ItemOverrideList from scratch using
 * JSON predicates paired with freshly-baked variant models.
 */
@Mixin(ModelManager.class)
public class MixinModelManagerFastReload {

    @Shadow(aliases = { "field_174958_a" })
    private IRegistry<ModelResourceLocation, IBakedModel> modelRegistry;

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"), cancellable = true)
    private void bstWeaponOnlyReload(IResourceManager resourceManager, CallbackInfo ci) {
        if (!FastTextureReloader.weaponOnlyReload)
            return;

        ci.cancel();

        long start = System.currentTimeMillis();
        Minecraft mc = Minecraft.getMinecraft();
        Function<ResourceLocation, TextureAtlasSprite> textureGetter = loc -> mc.getTextureMapBlocks()
                .getAtlasSprite(loc.toString());

        Set<Item> weapons = TweakerWeaponInjector.getItemDefinitionMap().keySet();
        int reloaded = 0;

        for (Item weapon : weapons) {
            ResourceLocation regName = weapon.getRegistryName();
            if (regName == null)
                continue;

            ModelResourceLocation mrl = new ModelResourceLocation(regName, "inventory");

            try {
                String basePath = regName.getPath();
                String ns = regName.getNamespace();

                // --- Step 1: Read BASE model JSON (has overrides) ---
                ModelBlock baseModelBlock = null;
                ResourceLocation baseJsonLoc = new ResourceLocation(ns,
                        "models/item/" + basePath + ".json");
                try (java.io.InputStream is = resourceManager.getResource(baseJsonLoc).getInputStream();
                        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    baseModelBlock = ModelBlock.deserialize(reader);
                    baseModelBlock.name = baseJsonLoc.toString();
                }

                if (baseModelBlock == null) {
                    BSTweaker.LOG.warn("[FastReload] No base model JSON for " + regName);
                    continue;
                }

                // --- Step 2: Bake the default model (fallback when no override matches) ---
                // Try _normal first, fall back to base
                IBakedModel bakedDefault = null;
                ResourceLocation normalJsonLoc = new ResourceLocation(ns,
                        "models/item/" + basePath + "_normal.json");
                try {
                    bakedDefault = bakeVariantModel(normalJsonLoc, resourceManager, textureGetter);
                } catch (Exception e) {
                    // No _normal, resolve base model itself
                    FastTextureReloader.resolveParents(baseModelBlock, resourceManager);
                    bakedDefault = bakeFromModelBlock(baseModelBlock, textureGetter);
                }

                if (bakedDefault == null) {
                    BSTweaker.LOG.warn("[FastReload] Failed to bake default for " + regName);
                    continue;
                }

                // --- Step 3: Bake ALL override variants and build override list ---
                List<ItemOverride> overrideList = baseModelBlock.getOverrides();
                int variantCount = 0;
                ItemOverrideList finalOverrides = ItemOverrideList.NONE;

                if (!overrideList.isEmpty()) {
                    List<ItemOverride> predicates = new ArrayList<>();
                    List<IBakedModel> models = new ArrayList<>();

                    for (ItemOverride override : overrideList) {
                        ResourceLocation variantLoc = override.getLocation();
                        // Convert: "ns:item/weaponspinning" -> "ns:models/item/weaponspinning.json"
                        String variantPath = variantLoc.getPath();
                        ResourceLocation variantJsonLoc = new ResourceLocation(
                                variantLoc.getNamespace(), "models/" + variantPath + ".json");

                        IBakedModel bakedVariant = null;
                        try {
                            bakedVariant = bakeVariantModel(variantJsonLoc, resourceManager, textureGetter);
                        } catch (Exception e) {
                            BSTweaker.LOG.debug("[FastReload] Could not bake variant " + variantLoc
                                    + ", using default: " + e.getMessage());
                        }

                        if (bakedVariant != null) {
                            predicates.add(override);
                            models.add(bakedVariant);
                            variantCount++;
                        } else {
                            // Use default model for this override
                            predicates.add(override);
                            models.add(bakedDefault);
                        }
                    }

                    finalOverrides = new SmartOverrideList(predicates, models);
                }

                // --- Step 4: Create final model with the default's quads + rebuilt overrides
                // ---
                IBakedModel finalModel = new OverridePreservingModel(bakedDefault, finalOverrides);

                this.modelRegistry.putObject(mrl, finalModel);

                BSTweaker.LOG.info("[FastReload] " + basePath + ": OK, variants=" + variantCount
                        + ", overrides=" + overrideList.size());

                reloaded++;
            } catch (Exception e) {
                BSTweaker.LOG.warn("Failed to reload model for " + regName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Rebuild mesher cache
        mc.getRenderItem().getItemModelMesher().rebuildCache();

        long elapsed = System.currentTimeMillis() - start;
        BSTweaker.LOG.info("Fast model reload: " + reloaded + " weapons in " + elapsed + "ms");
    }

    /**
     * Reads a variant JSON, resolves parents, bakes with ItemLayerModel.
     */
    private IBakedModel bakeVariantModel(ResourceLocation jsonLoc, IResourceManager resourceManager,
            Function<ResourceLocation, TextureAtlasSprite> textureGetter) throws Exception {
        ModelBlock modelBlock;
        try (java.io.InputStream is = resourceManager.getResource(jsonLoc).getInputStream();
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            modelBlock = ModelBlock.deserialize(reader);
            modelBlock.name = jsonLoc.toString();
        }

        FastTextureReloader.resolveParents(modelBlock, resourceManager);
        return bakeFromModelBlock(modelBlock, textureGetter);
    }

    /**
     * Bakes a resolved ModelBlock into IBakedModel with textures + transforms.
     */
    private IBakedModel bakeFromModelBlock(ModelBlock modelBlock,
            Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        // Extract texture layers
        ImmutableList.Builder<ResourceLocation> textureListBuilder = ImmutableList.builder();
        for (int i = 0; modelBlock.isTexturePresent("layer" + i); i++) {
            String texName = modelBlock.resolveTextureName("layer" + i);
            textureListBuilder.add(new ResourceLocation(texName));
        }
        ImmutableList<ResourceLocation> textureList = textureListBuilder.build();

        if (textureList.isEmpty()) {
            return null;
        }

        // Bake quads with ItemLayerModel
        ItemLayerModel itemModel = new ItemLayerModel(textureList);
        IBakedModel bakedBase = itemModel.bake(
                itemModel.getDefaultState(), DefaultVertexFormats.ITEM, textureGetter);

        // Get display transforms from resolved parent chain
        ItemCameraTransforms displayTransforms = modelBlock.getAllTransforms();
        ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transformMap = PerspectiveMapWrapper
                .getTransforms(displayTransforms);

        // Wrap with perspective transforms
        return new OverridePreservingModel(bakedBase, transformMap, ItemOverrideList.NONE);
    }
}
