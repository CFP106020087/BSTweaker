package com.mujmajnkraft.bstweaker.mixin.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.client.FastTextureReloader;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;

/**
 * Mixin into ModelLoader.setupModelRegistry() to skip expensive operations
 * when doing a BSTweaker fast reload:
 * 1. Skip texture atlas re-stitching (loadSprites)
 * 2. Skip non-weapon model baking (return old cached baked models)
 */
@Mixin(value = net.minecraftforge.client.model.ModelLoader.class, remap = false)
public class MixinModelLoaderFastReload {

    @Shadow
    protected Map<ModelResourceLocation, IModel> stateModels;

    /**
     * At HEAD: build weapon IModel set and cache old baked models for redirect.
     */
    @Inject(method = "setupModelRegistry", at = @At("HEAD"))
    private void bstPrepareWeaponSet(CallbackInfoReturnable<IRegistry<ModelResourceLocation, IBakedModel>> cir) {
        if (!FastTextureReloader.weaponOnlyReload)
            return;

        // Build set of weapon IModel instances
        Set<IModel> weaponModels = new HashSet<>();
        Set<Item> weapons = TweakerWeaponInjector.getItemDefinitionMap().keySet();
        for (Item weapon : weapons) {
            ResourceLocation regName = weapon.getRegistryName();
            if (regName != null) {
                ModelResourceLocation mrl = new ModelResourceLocation(regName, "inventory");
                IModel model = stateModels.get(mrl);
                if (model != null) {
                    weaponModels.add(model);
                }
            }
        }
        FastTextureReloader.weaponIModels = weaponModels;
        BSTweaker.LOG.info("[FastReload] Weapon IModels found: " + weaponModels.size());

        // Cache old baked models for non-weapon redirect
        Map<IModel, IBakedModel> oldCache = new HashMap<>();
        IRegistry<ModelResourceLocation, IBakedModel> oldRegistry = FastTextureReloader.oldModelRegistry;
        if (oldRegistry != null) {
            for (Map.Entry<ModelResourceLocation, IModel> entry : stateModels.entrySet()) {
                IModel imodel = entry.getValue();
                if (!weaponModels.contains(imodel)) {
                    IBakedModel old = oldRegistry.getObject(entry.getKey());
                    if (old != null && !oldCache.containsKey(imodel)) {
                        oldCache.put(imodel, old);
                    }
                }
            }
        }
        FastTextureReloader.oldBakedModelCache = oldCache;
        BSTweaker.LOG.info("[FastReload] Old cached models: " + oldCache.size());
    }

    /**
     * Skip non-weapon model baking — return old cached model instead.
     */
    @Redirect(method = "setupModelRegistry", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/IModel;bake(Lnet/minecraftforge/common/model/IModelState;Lnet/minecraft/client/renderer/vertex/VertexFormat;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/IBakedModel;", ordinal = 1))
    private IBakedModel bstRedirectBake(IModel model, IModelState state, VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> getter) {
        if (!FastTextureReloader.weaponOnlyReload) {
            return model.bake(state, format, getter);
        }

        // Weapon model: bake fresh
        if (FastTextureReloader.weaponIModels != null && FastTextureReloader.weaponIModels.contains(model)) {
            BSTweaker.LOG.info("[FastReload] Baking weapon model fresh");
            return model.bake(state, format, getter);
        }

        // Non-weapon: return old cached
        if (FastTextureReloader.oldBakedModelCache != null) {
            IBakedModel cached = FastTextureReloader.oldBakedModelCache.get(model);
            if (cached != null) {
                return cached;
            }
        }

        // Fallback: bake normally
        return model.bake(state, format, getter);
    }

    /**
     * Skip texture atlas re-stitching during fast reload.
     */
    @Redirect(method = "setupModelRegistry", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureMap;loadSprites(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;)V", remap = true))
    private void bstSkipLoadSprites(net.minecraft.client.renderer.texture.TextureMap texMap,
            IResourceManager rm, net.minecraft.client.renderer.texture.ITextureMapPopulator populator) {
        if (FastTextureReloader.weaponOnlyReload) {
            BSTweaker.LOG.info("[FastReload] Skipping loadSprites (texture atlas re-stitch)");
            return;
        }
        texMap.loadSprites(rm, populator);
    }
}
