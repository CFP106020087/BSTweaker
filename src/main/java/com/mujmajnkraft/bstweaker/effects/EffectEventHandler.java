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
     * LivingHurtEvent - 处理 onHit（攻击别人）和 onHurt（自己被攻击）
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        Entity source = event.getSource().getTrueSource();

        // === onHit: 攻击者手持武器攻击别人 ===
        if (source instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source;
            ItemStack mainHand = attacker.getHeldItemMainhand();
            if (!mainHand.isEmpty()) {
                Item weapon = mainHand.getItem();
                ResourceLocation regName = weapon.getRegistryName();
                List<WeaponEvent> effects = weaponEffectsCache.get(regName);

                if (effects != null) {
                    for (WeaponEvent we : effects) {
                        if ("LivingHurtEvent".equals(we.eventType) || "onHit".equals(we.eventType)) {
                            EventContext ctx = new EventContext(attacker, victim, weapon, event);
                            executeScripts(we, ctx);
                        }
                    }
                }
            }
        }

        // === onHurt: 被攻击者手持武器被攻击 ===
        ItemStack victimMainHand = victim.getHeldItemMainhand();
        if (!victimMainHand.isEmpty()) {
            Item weapon = victimMainHand.getItem();
            ResourceLocation regName = weapon.getRegistryName();
            List<WeaponEvent> effects = weaponEffectsCache.get(regName);

            if (effects != null) {
                for (WeaponEvent we : effects) {
                    if ("onHurt".equals(we.eventType)) {
                        // self = 被攻击者, victim = 攻击者 (如果有)
                        EntityLivingBase attacker = (source instanceof EntityLivingBase) ? (EntityLivingBase) source
                                : null;
                        EventContext ctx = new EventContext(victim, attacker, weapon, event);
                        executeScripts(we, ctx);
                    }
                }
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

        // 性能优化：每 5 tick 才检查一次 (原来是10，压制效果需要更快响应)
        if (entity.ticksExisted % 5 != 0)
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
     * 执行脚本 - 合并所有 actions 为一个脚本执行
     */
    private static void executeScripts(WeaponEvent we, EventContext ctx) {
        // 合并所有 actions 为一个完整脚本
        StringBuilder sb = new StringBuilder();
        for (String action : we.actions) {
            sb.append(action).append("\n");
        }
        String fullScript = sb.toString();

        try {
            ScriptEngine_.execute(fullScript, ctx);
        } catch (Exception e) {
            BSTweaker.LOG.error("Script execution error: " + e.getMessage());
        }
    }
}
