package com.mujmajnkraft.bstweaker.effects;

/**
 * 动作接口 - 所有事件动作的基类
 */
public interface EventAction {
    
    /**
     * 执行动作
     * @param context 事件上下文
     */
    void execute(EventContext context);
    
    /**
     * 从字符串解析动作
     */
    static EventAction parse(String actionStr) {
        return ActionParser.parse(actionStr);
    }
}
