package com.mujmajnkraft.bstweaker.effects;

import com.google.gson.JsonObject;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    // 武器 -> 事件列表 缓存
    private static final Map<Item, List<WeaponEvent>> weaponEffectsCache = new HashMap<>();

    /**
     * 注册武器效果
     */
    public static void registerWeaponEffects(Item weapon, List<WeaponEvent> events) {
        weaponEffectsCache.put(weapon, events);
        BSTweaker.LOG.info("Registered " + events.size() + " script events for: " + weapon.getRegistryName());
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
        List<WeaponEvent> effects = weaponEffectsCache.get(weapon);
        if (effects == null)
            return;

        EntityLivingBase victim = event.getEntityLiving();

        for (WeaponEvent we : effects) {
            if ("LivingHurtEvent".equals(we.eventType) || "onHit".equals(we.eventType)) {
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
        List<WeaponEvent> effects = weaponEffectsCache.get(weapon);
        if (effects == null)
            return;

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
        List<WeaponEvent> effects = weaponEffectsCache.get(weapon);
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
