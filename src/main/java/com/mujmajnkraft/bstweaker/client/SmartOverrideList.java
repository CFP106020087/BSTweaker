package com.mujmajnkraft.bstweaker.client;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.mujmajnkraft.bstweaker.BSTweaker;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Custom override list built from scratch using the original JSON predicates
 * and freshly-baked variant models. Uses reflection to access the
 * package-private
 * ItemOverride.matchesItemStack() method.
 */
public class SmartOverrideList extends ItemOverrideList {

    private static Method matchesMethod;

    static {
        // Try deobfuscated name first, then search by signature
        try {
            matchesMethod = ItemOverride.class.getDeclaredMethod("matchesItemStack", ItemStack.class,
                    World.class, EntityLivingBase.class);
            matchesMethod.setAccessible(true);
        } catch (NoSuchMethodException e1) {
            // Might be obfuscated — find by signature: boolean(ItemStack, World,
            // EntityLivingBase)
            for (Method m : ItemOverride.class.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (m.getReturnType() == boolean.class && params.length == 3
                        && params[0] == ItemStack.class
                        && params[1] == World.class
                        && params[2] == EntityLivingBase.class) {
                    matchesMethod = m;
                    matchesMethod.setAccessible(true);
                    break;
                }
            }
            if (matchesMethod == null) {
                BSTweaker.LOG.error("[SmartOverrideList] Could not find matchesItemStack by signature");
            }
        } catch (Exception e2) {
            BSTweaker.LOG.error("[SmartOverrideList] Failed to reflect matchesItemStack", e2);
        }
    }

    private final List<ItemOverride> overridePredicates;
    private final List<IBakedModel> overrideModels;

    /**
     * @param overridePredicates The override entries from the base model JSON (in
     *                           JSON order)
     * @param overrideModels     Corresponding freshly-baked models (same order)
     */
    public SmartOverrideList(List<ItemOverride> overridePredicates, List<IBakedModel> overrideModels) {
        super(Collections.emptyList());
        this.overridePredicates = overridePredicates;
        this.overrideModels = overrideModels;
    }

    @Override
    public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack,
            @Nullable World world, @Nullable EntityLivingBase entity) {
        if (matchesMethod == null) {
            return originalModel;
        }

        // Check predicates in reverse order (last matching override wins, matching
        // vanilla)
        for (int i = overridePredicates.size() - 1; i >= 0; i--) {
            try {
                boolean matches = (boolean) matchesMethod.invoke(overridePredicates.get(i), stack, world, entity);
                if (matches) {
                    return overrideModels.get(i);
                }
            } catch (Exception e) {
                // Skip this override on error
            }
        }
        // No override matched — use the base model
        return originalModel;
    }
}
