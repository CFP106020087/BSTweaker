package com.mujmajnkraft.bstweaker.effects;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Collection;

/**
 * JavaScript 脚本引擎 - 执行用户编写的脚本
 */
public class ScriptEngine_ {
    
    private static ScriptEngine engine;
    
    static {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("nashorn");
            if (engine == null) {
                engine = manager.getEngineByName("JavaScript");
            }
            BSTweaker.LOG.info("Script engine initialized: " + (engine != null ? engine.getClass().getName() : "null"));
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to initialize script engine: " + e.getMessage());
        }
    }
    
    /**
     * 执行脚本
     */
    public static void execute(String script, EventContext ctx) {
        if (engine == null) return;
        
        try {
            Bindings bindings = engine.createBindings();
            
            // 绑定上下文变量
            bindings.put("self", new EntityWrapper(ctx.self));
            bindings.put("victim", ctx.victim != null ? new EntityWrapper(ctx.victim) : null);
            bindings.put("event", new EventWrapper(ctx.forgeEvent));
            bindings.put("Potion", new PotionHelper());
            bindings.put("log", new Logger());
            
            engine.eval(script, bindings);
            
        } catch (Exception e) {
            BSTweaker.LOG.error("Script error: " + e.getMessage());
        }
    }
    
    /**
     * 实体包装器 - 暴露给脚本的 API
     */
    public static class EntityWrapper {
        private final EntityLivingBase entity;
        
        public EntityWrapper(EntityLivingBase entity) {
            this.entity = entity;
        }
        
        // === 基础属性 ===
        public float getHealth() { return entity.getHealth(); }
        public void setHealth(float health) { entity.setHealth(health); }
        public float getMaxHealth() { return entity.getMaxHealth(); }
        public void heal(float amount) { entity.heal(amount); }
        
        // === 无敌帧 ===
        public int getHurtResistantTime() { return entity.hurtResistantTime; }
        public void setHurtResistantTime(int time) { entity.hurtResistantTime = time; }
        
        // === 药水效果 ===
        public PotionEffectWrapper getPotionEffect(String id) {
            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(id));
            if (potion != null) {
                PotionEffect effect = entity.getActivePotionEffect(potion);
                if (effect != null) {
                    return new PotionEffectWrapper(effect, entity);
                }
            }
            return null;
        }
        
        public void addPotionEffect(String id, int duration, int amplifier) {
            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(id));
            if (potion != null) {
                entity.addPotionEffect(new PotionEffect(potion, duration, amplifier));
            }
        }
        
        public void removePotionEffect(String id) {
            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(id));
            if (potion != null) {
                entity.removePotionEffect(potion);
            }
        }
        
        public boolean hasPotionEffect(String id) {
            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(id));
            return potion != null && entity.isPotionActive(potion);
        }
        
        public Collection<PotionEffect> getActivePotionEffects() {
            return entity.getActivePotionEffects();
        }
        
        // === 其他 ===
        public void setFire(int seconds) { entity.setFire(seconds); }
        public boolean isBurning() { return entity.isBurning(); }
        public boolean isInWater() { return entity.isInWater(); }
    }
    
    /**
     * 药水效果包装器
     */
    public static class PotionEffectWrapper {
        private final PotionEffect effect;
        private final EntityLivingBase entity;
        
        public PotionEffectWrapper(PotionEffect effect, EntityLivingBase entity) {
            this.effect = effect;
            this.entity = entity;
        }
        
        public int getAmplifier() { return effect.getAmplifier(); }
        public int getDuration() { return effect.getDuration(); }
        
        public void setAmplifier(int amplifier) {
            // 需要移除并重新添加
            Potion potion = effect.getPotion();
            entity.removePotionEffect(potion);
            entity.addPotionEffect(new PotionEffect(potion, effect.getDuration(), amplifier));
        }
    }
    
    /**
     * 事件包装器
     */
    public static class EventWrapper {
        private final Object event;
        
        public EventWrapper(Object event) {
            this.event = event;
        }
        
        public float getAmount() {
            if (event instanceof LivingHurtEvent) {
                return ((LivingHurtEvent) event).getAmount();
            }
            return 0;
        }
        
        public void setAmount(float amount) {
            if (event instanceof LivingHurtEvent) {
                ((LivingHurtEvent) event).setAmount(amount);
            }
        }
        
        public void cancel() {
            if (event instanceof net.minecraftforge.fml.common.eventhandler.Event) {
                ((net.minecraftforge.fml.common.eventhandler.Event) event).setCanceled(true);
            }
        }
    }
    
    /**
     * 药水辅助类
     */
    public static class PotionHelper {
        public Potion get(String id) {
            return Potion.REGISTRY.getObject(new ResourceLocation(id));
        }
    }
    
    /**
     * 日志辅助类
     */
    public static class Logger {
        public void info(Object msg) { BSTweaker.LOG.info(String.valueOf(msg)); }
        public void debug(Object msg) { BSTweaker.LOG.debug(String.valueOf(msg)); }
    }
}
