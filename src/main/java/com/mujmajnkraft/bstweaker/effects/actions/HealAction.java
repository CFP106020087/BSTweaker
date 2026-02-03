package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraft.entity.EntityLivingBase;

/** Heal action. */
public class HealAction implements EventAction {
    
    private final String target;
    private final float amount;
    
    public HealAction(String target, String[] args) {
        this.target = target;
        this.amount = args.length > 0 ? Float.parseFloat(args[0]) : 2.0f;
    }
    
    @Override
    public void execute(EventContext context) {
        EntityLivingBase entity = getTarget(context);
        if (entity != null) {
            entity.heal(amount);
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
