package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraft.entity.EntityLivingBase;

/**
 * 点燃动作
 */
public class IgniteAction implements EventAction {
    
    private final String target;
    private final int ticks;
    
    public IgniteAction(String target, String[] args) {
        this.target = target;
        this.ticks = args.length > 0 ? Integer.parseInt(args[0]) : 100;
    }
    
    @Override
    public void execute(EventContext context) {
        EntityLivingBase entity = getTarget(context);
        if (entity != null) {
            entity.setFire(ticks / 20);
        }
    }
    
    private EntityLivingBase getTarget(EventContext context) {
        switch (target) {
            case "self": return context.self;
            case "victim": return context.victim;
            default: return context.victim;
        }
    }
}
