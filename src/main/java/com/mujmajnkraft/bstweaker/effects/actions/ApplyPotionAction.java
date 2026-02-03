package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

/** Apply potion effect. */
public class ApplyPotionAction implements EventAction {
    
    private final String target;
    private final String potionId;
    private final int duration;
    private final int amplifier;
    
    public ApplyPotionAction(String target, String[] args) {
        this.target = target;
        this.potionId = args.length > 0 ? args[0] : "";
        this.duration = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        this.amplifier = args.length > 2 ? Integer.parseInt(args[2]) : 0;
    }
    
    @Override
    public void execute(EventContext context) {
        EntityLivingBase entity = getTarget(context);
        if (entity == null) return;
        
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
        if (potion == null) return;
        
        entity.addPotionEffect(new PotionEffect(potion, duration, amplifier));
    }
    
    private EntityLivingBase getTarget(EventContext context) {
        switch (target) {
            case "self": return context.self;
            case "victim": return context.victim;
            default:
                return context.victim; // Default to victim
        }
    }
}
