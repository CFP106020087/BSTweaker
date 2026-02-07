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

/** Effect event handler - listens to Forge events and executes scripts. */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class EffectEventHandler {

    // Weapon registry name -> event list
    private static final Map<ResourceLocation, List<WeaponEvent>> weaponEffectsCache = new HashMap<>();

    /** Register weapon effects. */
    public static void registerWeaponEffects(Item weapon, List<WeaponEvent> events) {
        ResourceLocation regName = weapon.getRegistryName();
        weaponEffectsCache.put(regName, events);
    }

    /** LivingHurtEvent - handles onHit (attack) and onHurt (being attacked). */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        Entity source = event.getSource().getTrueSource();

        // onHit: attacker holds weapon
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

        // onHurt: victim holds weapon
        ItemStack victimMainHand = victim.getHeldItemMainhand();
        if (!victimMainHand.isEmpty()) {
            Item weapon = victimMainHand.getItem();
            ResourceLocation regName = weapon.getRegistryName();
            List<WeaponEvent> effects = weaponEffectsCache.get(regName);

            if (effects != null) {
                for (WeaponEvent we : effects) {
                    if ("onHurt".equals(we.eventType)) {
                        // self = victim, victim = attacker (if exists)
                        EntityLivingBase attacker = (source instanceof EntityLivingBase) ? (EntityLivingBase) source
                                : null;
                        EventContext ctx = new EventContext(victim, attacker, weapon, event);
                        executeScripts(we, ctx);
                    }
                }
            }
        }
    }

    /** LivingDeathEvent - death event (kill). */
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

        EntityLivingBase victim = event.getEntityLiving();

        for (WeaponEvent we : effects) {
            if ("LivingDeathEvent".equals(we.eventType) || "onKill".equals(we.eventType)) {
                EventContext ctx = new EventContext(attacker, victim, weapon, event);
                executeScripts(we, ctx);
            }
        }
    }

    /** LivingUpdateEvent - entity update (tick). */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();

        // Throttle: check every 5 ticks
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

    /** Execute scripts - merge all actions into one script. */
    private static void executeScripts(WeaponEvent we, EventContext ctx) {
        // Merge all actions into full script
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
