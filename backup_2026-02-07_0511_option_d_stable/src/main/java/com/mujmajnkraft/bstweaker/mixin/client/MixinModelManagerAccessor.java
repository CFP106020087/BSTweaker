package com.mujmajnkraft.bstweaker.mixin.client;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor for ModelManager to enable fast model reloading.
 * Exposes the internal modelRegistry (bakedRegistry) for direct updates
 * without triggering a full resource reload.
 */
@Mixin(ModelManager.class)
public interface MixinModelManagerAccessor {
    
    /**
     * Get the baked model registry for direct model updates.
     * This registry maps ModelResourceLocation to IBakedModel.
     */
    @Accessor("modelRegistry")
    IRegistry<ModelResourceLocation, IBakedModel> getModelRegistry();
}
