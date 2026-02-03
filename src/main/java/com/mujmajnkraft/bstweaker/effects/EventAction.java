package com.mujmajnkraft.bstweaker.effects;

/** Action interface - base for all event actions. */
public interface EventAction {
    
    /** Execute the action. */
    void execute(EventContext context);
    
    /** Parse action from string. */
    static EventAction parse(String actionStr) {
        return ActionParser.parse(actionStr);
    }
}
