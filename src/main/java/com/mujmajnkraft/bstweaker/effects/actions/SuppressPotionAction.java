package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

/** Suppress potion effect - caps amplifier to max level. */
public class SuppressPotionAction implements EventAction {
    
    private final String target;
    private final String potionId;
    private final int maxLevel;
    
    public SuppressPotionAction(String target, String[] args) {
        this.target = target;
        this.potionId = args.length > 0 ? args[0] : "";
        this.maxLevel = args.length > 1 ? Integer.parseInt(args[1]) : 0;
    }
    
    @Override
    public void execute(EventContext context) {
        EntityLivingBase entity = getTarget(context);
        if (entity == null) return;
        
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
        if (potion == null) return;
        
        PotionEffect effect = entity.getActivePotionEffect(potion);
        if (effect != null && effect.getAmplifier() > maxLevel) {
            // Remove old effect, add capped level
            entity.removePotionEffect(potion);
            entity.addPotionEffect(new PotionEffect(potion, effect.getDuration(), maxLevel, 
                effect.getIsAmbient(), effect.doesShowParticles()));
        }
    }
    
    private EntityLivingBase getTarget(EventContext context) {
        switch (target) {
            case "self": return context.self;
            case "victim": return context.victim;
            default: return context.self;
        }
    }
}
