package com.mujmajnkraft.bstweaker.mixin.client;

import java.lang.reflect.Field;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.client.DynamicResourcePack;
import com.mujmajnkraft.bstweaker.client.HotReloadHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;

/**
 * Mixin to inject DynamicResourcePack and support fast hot-reload.
 */
@Mixin(Minecraft.class)
public class MixinMinecraftRefreshResources {

    private static Field defaultResourcePacksField = null;

    /**
     * Get defaultResourcePacks via reflection to avoid @Shadow aliasing issues.
     */
    @SuppressWarnings("unchecked")
    private List<IResourcePack> getDefaultResourcePacks() {
        try {
            if (defaultResourcePacksField == null) {
                // Try MCP name first, then SRG name
                try {
                    defaultResourcePacksField = Minecraft.class.getDeclaredField("defaultResourcePacks");
                } catch (NoSuchFieldException e) {
                    defaultResourcePacksField = Minecraft.class.getDeclaredField("field_110449_ao");
                }
                defaultResourcePacksField.setAccessible(true);
            }
            return (List<IResourcePack>) defaultResourcePacksField.get(Minecraft.getMinecraft());
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to get defaultResourcePacks: " + e.getMessage());
            return null;
        }
    }

    /**
     * Inject at TAIL of startGame to add DynamicResourcePack early.
     * This ensures our pack is present before the first resource reload.
     * Based on ResourceLoader ASM pattern: inject in startGame for early
     * registration.
     */
    @Inject(method = { "startGame", "func_71384_a" }, at = @At("TAIL"))
    private void bstweaker$onStartGame(CallbackInfo ci) {
        // Scan config resources first
        DynamicResourcePack.scanConfigResources();

        // Add DynamicResourcePack at position 0 (highest priority, like RL's
        // insertForcedPack)
        List<IResourcePack> packs = getDefaultResourcePacks();
        if (packs != null && !isDynamicPackPresent(packs)) {
            packs.add(0, DynamicResourcePack.getInstance());
            BSTweaker.LOG.info("[MIXIN] Injected DynamicResourcePack at startup (priority 0)");
        }
    }

    private boolean isDynamicPackPresent(List<IResourcePack> packs) {
        if (packs == null)
            return false;
        for (IResourcePack pack : packs) {
            if (pack instanceof DynamicResourcePack) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inject at HEAD of refreshResources.
     * If fast reload mode is enabled, cancel the full refresh and do fast reload.
     */
    @Inject(method = { "refreshResources", "func_110436_a" }, at = @At("HEAD"), cancellable = true)
    private void bstweaker$onRefreshResources(CallbackInfo ci) {
        // Ensure DynamicResourcePack is in the list
        ensureDynamicPackPresent();

        // Check if fast reload is enabled in config AND a fast reload was requested
        if (HotReloadHelper.consumeFastReload()) {
            doFastReload();
            ci.cancel(); // Skip the slow full refresh
        } else {
            // Normal F3+T refresh - just rescan before it runs
            // Also consume any pending fast reload flag so it doesn't affect next refresh
            HotReloadHelper.consumeFastReload();
            DynamicResourcePack.rescan();
        }
    }

    /**
     * Inject DynamicResourcePack into the resource pack list that gets passed to
     * reloadResourcesOnGameThread.
     * This matches ResourceLoader's ASM pattern: inject BEFORE func_110541_a is
     * called.
     * This ensures our pack has highest priority for model/texture loading.
     */
    @SuppressWarnings("unchecked")
    @ModifyArg(method = { "refreshResources",
            "func_110436_a" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/IReloadableResourceManager;reloadResources(Ljava/util/List;)V"), index = 0)
    private List<IResourcePack> bstweaker$injectDynamicPack(List<IResourcePack> resourcePacks) {
        // Rescan config resources before reload
        DynamicResourcePack.scanConfigResources();

        // Check if DynamicResourcePack is already in the list
        boolean hasOurPack = false;
        for (Object pack : resourcePacks) {
            if (pack instanceof DynamicResourcePack) {
                hasOurPack = true;
                break;
            }
        }

        // Add at index 0 for highest priority (like RL's insertForcedPack)
        if (!hasOurPack) {
            resourcePacks.add(0, DynamicResourcePack.getInstance());
            BSTweaker.LOG.info("[MIXIN] Injected DynamicResourcePack into reloadResources list (index 0)");
        }

        return resourcePacks;
    }

    // Keep legacy method for startGame injection
    private void ensureDynamicPackPresent() {
        List<IResourcePack> packs = getDefaultResourcePacks();
        if (packs != null && !isDynamicPackPresent(packs)) {
            packs.add(0, DynamicResourcePack.getInstance());
            BSTweaker.LOG.info("[MIXIN] Injected DynamicResourcePack in defaultResourcePacks");
        }
    }


    /**
     * Fast reload: only reload BSTweaker resources (language, models, textures).
     */
    private void doFastReload() {
        try {
            long start = System.currentTimeMillis();
            Minecraft mc = Minecraft.getMinecraft();

            // 1. Rescan config files
            DynamicResourcePack.rescan();

            // 2. Reload language
            mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());

            // 3. Fast reload textures - directly update sprite regions in atlas
            com.mujmajnkraft.bstweaker.client.FastTextureReloader.reloadWeaponTextures();

            // 4. Reload models for our items only
            reloadWeaponModels(mc);

            long elapsed = System.currentTimeMillis() - start;
            BSTweaker.LOG.info("Fast reload completed: " + elapsed + "ms");

        } catch (Exception e) {
            BSTweaker.LOG.error("Fast reload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reload models only for BSTweaker weapons.
     * Uses item.getRegistryName() to match BS namespace where models are generated.
     */
    private void reloadWeaponModels(Minecraft mc) {
        try {
            java.util.Map<net.minecraft.item.Item, com.google.gson.JsonObject> itemMap = com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector
                    .getItemDefinitionMap();

            if (itemMap.isEmpty())
                return;

            net.minecraft.client.renderer.ItemModelMesher mesher = mc.getRenderItem().getItemModelMesher();
            net.minecraft.client.renderer.block.model.ModelManager modelManager = mesher.getModelManager();

            for (java.util.Map.Entry<net.minecraft.item.Item, com.google.gson.JsonObject> entry : itemMap.entrySet()) {
                net.minecraft.item.Item item = entry.getKey();

                // Use item.getRegistryName() - this matches the BS namespace where models are
                // generated
                // e.g. mujmajnkraftsbettersurvival:itemanunchaku
                net.minecraft.util.ResourceLocation registryName = item.getRegistryName();
                if (registryName == null)
                    continue;

                net.minecraft.client.renderer.block.model.ModelResourceLocation loc = new net.minecraft.client.renderer.block.model.ModelResourceLocation(
                        registryName, "inventory");

                net.minecraft.client.renderer.block.model.IBakedModel model = modelManager.getModel(loc);
                if (model != null) {
                    mesher.register(item, 0, loc);
                    BSTweaker.LOG.debug("Re-registered model for " + registryName);
                }
            }
        } catch (Exception e) {
            BSTweaker.LOG.warn("Failed to reload weapon models: " + e.getMessage());
        }
    }

}
