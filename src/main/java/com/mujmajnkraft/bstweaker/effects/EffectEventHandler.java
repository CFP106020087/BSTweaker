package com.mujmajnkraft.bstweaker.effects;

import com.google.gson.JsonObject;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 效果事件处理器 - 监听 Forge 事件并执行 JavaScript 脚本
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class EffectEventHandler {

    // 武器注册名 -> 事件列表 (使用注册名作为 key，而不是 Item 对象)
    private static final Map<ResourceLocation, List<WeaponEvent>> weaponEffectsCache = new HashMap<>();

    /**
     * 注册武器效果
     */
    public static void registerWeaponEffects(Item weapon, List<WeaponEvent> events) {
        ResourceLocation regName = weapon.getRegistryName();
        weaponEffectsCache.put(regName, events);
        BSTweaker.LOG.info("Registered " + events.size() + " script events for: " + regName);
        BSTweaker.LOG.info("Cache now contains " + weaponEffectsCache.size() + " weapons");
    }

    /**
     * LivingHurtEvent - 伤害事件
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityLivingBase))
            return;

        EntityLivingBase attacker = (EntityLivingBase) source;
        ItemStack mainHand = attacker.getHeldItemMainhand();
        if (mainHand.isEmpty())
            return;

        Item weapon = mainHand.getItem();
        ResourceLocation regName = weapon.getRegistryName();
        List<WeaponEvent> effects = weaponEffectsCache.get(regName);

        // 调试日志
        if (weaponEffectsCache.size() > 0) {
            BSTweaker.LOG.debug("onLivingHurt: weapon=" + regName + ", hasEffects=" + (effects != null));
        }

        if (effects == null)
            return;

        BSTweaker.LOG.info("Executing " + effects.size() + " effects for LivingHurtEvent with weapon: " + regName);
        EntityLivingBase victim = event.getEntityLiving();

        for (WeaponEvent we : effects) {
            if ("LivingHurtEvent".equals(we.eventType) || "onHit".equals(we.eventType)) {
                BSTweaker.LOG.info("Executing onHit script: "
                        + we.actions.get(0).substring(0, Math.min(30, we.actions.get(0).length())));
                EventContext ctx = new EventContext(attacker, victim, weapon, event);
                executeScripts(we, ctx);
            }
        }
    }

    /**
     * LivingDeathEvent - 死亡事件 (击杀)
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityLivingBase))
            return;

        EntityLivingBase attacker = (EntityLivingBase) source;
        ItemStack mainHand = attacker.getHeldItemMainhand();
        if (mainHand.isEmpty())
            return;

        Item weapon = mainHand.getItem();
        ResourceLocation regName = weapon.getRegistryName();
        List<WeaponEvent> effects = weaponEffectsCache.get(regName);
        if (effects == null)
            return;

        BSTweaker.LOG.info("Executing " + effects.size() + " effects for LivingDeathEvent with weapon: " + regName);
        EntityLivingBase victim = event.getEntityLiving();

        for (WeaponEvent we : effects) {
            if ("LivingDeathEvent".equals(we.eventType) || "onKill".equals(we.eventType)) {
                EventContext ctx = new EventContext(attacker, victim, weapon, event);
                executeScripts(we, ctx);
            }
        }
    }

    /**
     * LivingUpdateEvent - 实体更新 (tick)
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();

        // 性能优化：每 10 tick 才检查一次
        if (entity.ticksExisted % 10 != 0)
            return;

        ItemStack mainHand = entity.getHeldItemMainhand();
        if (mainHand.isEmpty())
            return;

        Item weapon = mainHand.getItem();
        ResourceLocation regName = weapon.getRegistryName();
        List<WeaponEvent> effects = weaponEffectsCache.get(regName);
        if (effects == null)
            return;

        for (WeaponEvent we : effects) {
            if ("LivingUpdateEvent".equals(we.eventType) || "whenHeld".equals(we.eventType)) {
                EventContext ctx = new EventContext(entity, null, weapon, event);
                executeScripts(we, ctx);
            }
        }
    }

    /**
     * 执行脚本
     */
    private static void executeScripts(WeaponEvent we, EventContext ctx) {
        for (String script : we.actions) {
            try {
                ScriptEngine_.execute(script, ctx);
            } catch (Exception e) {
                BSTweaker.LOG.error("Script execution error: " + e.getMessage());
            }
        }
    }
}
