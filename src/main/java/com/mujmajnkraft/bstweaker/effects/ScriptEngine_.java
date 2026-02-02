package com.mujmajnkraft.bstweaker.effects;

import com.mujmajnkraft.bstweaker.BSTweaker;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Collection;

/**
 * JavaScript 脚本引擎 - 执行用户编写的脚本
 */
public class ScriptEngine_ {
    
    private static ScriptEngine engine;
    
    static {
        try {
            // 方法1: 尝试直接实例化 NashornScriptEngineFactory
            try {
                Class<?> factoryClass = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
                ScriptEngineFactory factory = (ScriptEngineFactory) factoryClass.newInstance();
                engine = factory.getScriptEngine();
                BSTweaker.LOG.info(
                        "Script engine initialized via NashornScriptEngineFactory: " + engine.getClass().getName());
            } catch (Exception e1) {
                BSTweaker.LOG.warn("NashornScriptEngineFactory not available: " + e1.getMessage());

                // 方法2: 尝试使用系统类加载器
                try {
                    ClassLoader systemCL = ClassLoader.getSystemClassLoader();
                    Class<?> factoryClass = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory", true,
                            systemCL);
                    ScriptEngineFactory factory = (ScriptEngineFactory) factoryClass.newInstance();
                    engine = factory.getScriptEngine();
                    BSTweaker.LOG
                            .info("Script engine initialized via SystemClassLoader: " + engine.getClass().getName());
                } catch (Exception e2) {
                    BSTweaker.LOG.warn("SystemClassLoader method failed: " + e2.getMessage());

                    // 方法3: 使用线程上下文类加载器
                    try {
                        ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
                        javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
                        engine = manager.getEngineByName("nashorn");
                        if (engine == null) {
                            engine = manager.getEngineByName("JavaScript");
                        }
                        Thread.currentThread().setContextClassLoader(contextCL);
                        if (engine != null) {
                            BSTweaker.LOG.info(
                                    "Script engine initialized via ContextClassLoader: " + engine.getClass().getName());
                        }
                    } catch (Exception e3) {
                        BSTweaker.LOG.error("All script engine initialization methods failed: " + e3.getMessage());
                    }
                }
            }

            if (engine == null) {
                BSTweaker.LOG.error("Script engine is NULL - JavaScript scripting will not work!");
                BSTweaker.LOG.error("This may be due to Minecraft's classloader blocking Nashorn.");
            }

        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to initialize script engine: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 执行脚本
     */
    public static void execute(String script, EventContext ctx) {
        if (engine == null) {
            // 只在第一次打印错误，避免刷屏
            return;
        }
        
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
        
        /**
         * 获取所有负面药水效果
         */
        public java.util.List<PotionEffectWrapper> getBadPotionEffects() {
            java.util.List<PotionEffectWrapper> badEffects = new java.util.ArrayList<>();
            for (PotionEffect effect : entity.getActivePotionEffects()) {
                if (effect.getPotion().isBadEffect()) {
                    badEffects.add(new PotionEffectWrapper(effect, entity));
                }
            }
            return badEffects;
        }

        /**
         * 压制所有负面效果到指定等级
         */
        public void suppressBadEffects(int maxLevel) {
            java.util.List<PotionEffect> toSuppress = new java.util.ArrayList<>();
            for (PotionEffect effect : entity.getActivePotionEffects()) {
                if (effect.getPotion().isBadEffect() && effect.getAmplifier() > maxLevel) {
                    toSuppress.add(effect);
                }
            }
            for (PotionEffect effect : toSuppress) {
                Potion potion = effect.getPotion();
                entity.removePotionEffect(potion);
                entity.addPotionEffect(new PotionEffect(potion, effect.getDuration(), maxLevel));
            }
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
