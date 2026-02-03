package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Cancel event action. */
public class CancelEventAction implements EventAction {
    
    @Override
    public void execute(EventContext context) {
        if (context.forgeEvent instanceof Event) {
            Event event = (Event) context.forgeEvent;
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }
}
