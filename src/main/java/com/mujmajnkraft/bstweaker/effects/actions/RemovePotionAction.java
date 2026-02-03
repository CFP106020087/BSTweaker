package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/** Remove potion effect. */
public class RemovePotionAction implements EventAction {
    
    private final String target;
    private final String potionId;
    
    public RemovePotionAction(String target, String[] args) {
        this.target = target;
        this.potionId = args.length > 0 ? args[0] : "";
    }
    
    @Override
    public void execute(EventContext context) {
        EntityLivingBase entity = getTarget(context);
        if (entity == null) return;
        
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
        if (potion != null) {
            entity.removePotionEffect(potion);
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
