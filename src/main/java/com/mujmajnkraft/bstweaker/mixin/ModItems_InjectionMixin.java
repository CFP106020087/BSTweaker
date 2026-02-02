package com.mujmajnkraft.bstweaker.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mujmajnkraft.bettersurvival.init.ModItems;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * 注入 BetterSurvival 的 ModItems.registerItems 方法
 * 在原版武器注册前，将自定义武器添加到 items 列表中
 */
@Mixin(value = ModItems.class, remap = false)
public abstract class ModItems_InjectionMixin {

    @Shadow
    private static List<Item> items;

    /**
     * 在 registerItems 方法开始时，先让原方法填充 items 列表
     * 然后追加自定义武器到列表末尾
     * 注意：这里使用 RETURN 注入点，在方法返回前（registerAll 调用后）执行
     * 但我们实际需要在 registerAll 调用前执行，所以改用 INVOKE_ASSIGN
     */
    @Inject(method = "registerItems(Lnet/minecraftforge/event/RegistryEvent$Register;)V", at = @At(value = "HEAD"))
    private void bstweaker$injectCustomWeaponsAtHead(RegistryEvent.Register<Item> event, CallbackInfo ci) {
        System.out.println("[BSTweaker] Mixin triggered - registerItems HEAD");
    }

    /**
     * 使用字段 items 来追加武器 - 在方法末尾但 registerAll 之前
     * 由于 registerAll 是最后一个调用，我们在 for 循环后使用 INVOKE
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
            } else {
                System.out.println("[BSTweaker] No custom weapons to inject");
            }
        } catch (Exception e) {
            System.err.println("[BSTweaker] Failed to inject custom weapons: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
