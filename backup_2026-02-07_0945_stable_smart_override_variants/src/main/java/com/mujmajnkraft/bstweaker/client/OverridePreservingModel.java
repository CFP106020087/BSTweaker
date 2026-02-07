package com.mujmajnkraft.bstweaker.client;

import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * Wrapper that uses new quads/textures from ItemLayerModel bake,
 * with smart override interception and display transforms.
 */
public class OverridePreservingModel implements IBakedModel {
    private final IBakedModel delegate;
    private final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms;
    private final ItemOverrideList overrides;

    public OverridePreservingModel(IBakedModel delegate,
            ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms,
            ItemOverrideList overrides) {
        this.delegate = delegate;
        this.transforms = transforms;
        this.overrides = overrides;
    }

    /** Convenience: uses delegate's own perspective handling */
    public OverridePreservingModel(IBakedModel delegate, ItemOverrideList overrides) {
        this(delegate, ImmutableMap.of(), overrides);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return delegate.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return delegate.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return delegate.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return delegate.getParticleTexture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return delegate.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
            ItemCameraTransforms.TransformType cameraTransformType) {
        if (transforms.isEmpty()) {
            // Delegate to the underlying model's perspective handling
            return delegate.handlePerspective(cameraTransformType);
        }
        return PerspectiveMapWrapper.handlePerspective(this, transforms, cameraTransformType);
    }
}
