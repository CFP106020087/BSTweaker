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

/**
 * 注入 BetterSurvival 的 ModItems.registerItems 方法
 * 在原版武器注册前，将自定义武器添加到 items 列表中
 */
@Mixin(value = ModItems.class, remap = false)
public abstract class ModItems_InjectionMixin {

    @Shadow
    private static List<Item> items;

    /**
     * 在 registerItems 方法开始时打印日志
     */
    @Inject(method = "registerItems(Lnet/minecraftforge/event/RegistryEvent$Register;)V", at = @At(value = "HEAD"))
    private void bstweaker$injectCustomWeaponsAtHead(RegistryEvent.Register<Item> event, CallbackInfo ci) {
        System.out.println("[BSTweaker] Mixin triggered - registerItems HEAD");
    }

    /**
     * 在 items.toArray 调用前注入自定义武器
     */
    @Inject(method = "registerItems(Lnet/minecraftforge/event/RegistryEvent$Register;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;toArray([Ljava/lang/Object;)[Ljava/lang/Object;", ordinal = 0))
    private void bstweaker$injectCustomWeapons(RegistryEvent.Register<Item> event, CallbackInfo ci) {
        try {
            System.out.println("[BSTweaker] Attempting to inject custom weapons...");
            List<Item> customWeapons = com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector.createWeapons();
            if (customWeapons != null && !customWeapons.isEmpty()) {
                items.addAll(customWeapons);
                System.out.println("[BSTweaker] Injected " + customWeapons.size()
                        + " custom weapons into BetterSurvival items list");

                // ** 注册模型 - 在物品注册后立即执行 **
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
