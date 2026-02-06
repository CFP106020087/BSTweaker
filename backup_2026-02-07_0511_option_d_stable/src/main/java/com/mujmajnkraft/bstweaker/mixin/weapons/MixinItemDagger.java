package com.mujmajnkraft.bstweaker.mixin.weapons;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dagger Attribute Mixin
 * Supports custom backstab multiplier from weapon NBT
 * 
 * Config field: backstabMultiplier (default 2.0)
 */
@Mixin(targets = "com.mujmajnkraft.bettersurvival.items.ItemDagger", remap = false)
public class MixinItemDagger {

    private static final String TAG_BSTWEAKER = "bstweaker";
    private static final String TAG_BACKSTAB_MULTIPLIER = "backstabMultiplier";

    /**
     * Inject into getBackstabMultiplier to override return value
     * If weapon has custom backstabMultiplier, use custom value
     */
    @Inject(method = "getBackstabMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetBackstabMultiplier(EntityLivingBase user, Entity target, boolean offhand,
            CallbackInfoReturnable<Float> cir) {
        // Get weapon
        ItemStack weapon = offhand ? user.getHeldItemOffhand() : user.getHeldItemMainhand();
        if (weapon.isEmpty())
            return;

        // Check bstweaker NBT
        NBTTagCompound tag = weapon.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_BSTWEAKER))
            return;

        NBTTagCompound bstweaker = tag.getCompoundTag(TAG_BSTWEAKER);
        if (!bstweaker.hasKey(TAG_BACKSTAB_MULTIPLIER))
            return;

        // Get original return value (to check if backstab)
        float original = cir.getReturnValue();

        // If original > 1, it's a backstab attack
        if (original > 1.0f) {
            float customMultiplier = bstweaker.getFloat(TAG_BACKSTAB_MULTIPLIER);

            // Keep assassinate enchant bonus (original - 2.0 = enchant bonus)
            float enchantBonus = original - 2.0f;
            float newValue = customMultiplier + enchantBonus;

            cir.setReturnValue(newValue);
        }
    }
}
