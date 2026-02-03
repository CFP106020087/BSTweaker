package com.mujmajnkraft.bstweaker.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mujmajnkraft.bettersurvival.init.ModItems;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;

/** Inject into BetterSurvival ModItems.registerItems to add custom weapons. */
@Mixin(value = ModItems.class, remap = false)
public abstract class ModItems_InjectionMixin {

    @Shadow
    private static List<Item> items;

    /** Log at registerItems method start. */
    @Inject(method = "registerItems(Lnet/minecraftforge/event/RegistryEvent$Register;)V", at = @At(value = "HEAD"))
    private void bstweaker$injectCustomWeaponsAtHead(RegistryEvent.Register<Item> event, CallbackInfo ci) {
        System.out.println("[BSTweaker] Mixin triggered - registerItems HEAD");
    }

    /** Inject custom weapons before items.toArray call. */
    @Inject(method = "registerItems(Lnet/minecraftforge/event/RegistryEvent$Register;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;toArray([Ljava/lang/Object;)[Ljava/lang/Object;", ordinal = 0))
    private void bstweaker$injectCustomWeapons(RegistryEvent.Register<Item> event, CallbackInfo ci) {
        try {
            System.out.println("[BSTweaker] Attempting to inject custom weapons...");
            List<Item> customWeapons = com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector.createWeapons();
            if (customWeapons != null && !customWeapons.isEmpty()) {
                items.addAll(customWeapons);
                System.out.println("[BSTweaker] Injected " + customWeapons.size()
                        + " custom weapons into BetterSurvival items list");

                // Register models immediately
                com.mujmajnkraft.bstweaker.client.ClientEventHandler.registerModelsForItems(customWeapons);
            } else {
                System.out.println("[BSTweaker] No custom weapons to inject");
            }
        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to inject custom weapons: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
