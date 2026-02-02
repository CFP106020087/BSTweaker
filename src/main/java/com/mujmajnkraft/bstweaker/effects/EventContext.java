package com.mujmajnkraft.bstweaker.effects;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;

/**
 * 事件执行上下文 - 包含事件中可用的所有变量
 */
public class EventContext {
    
    public final EntityLivingBase self;      // 持有武器的实体
    public final EntityLivingBase victim;    // 受害者 (可能为 null)
    public final Item weaponItem;            // 触发的武器
    public final Object forgeEvent;          // 原始 Forge 事件
    
    public EventContext(EntityLivingBase self, EntityLivingBase victim, Item weaponItem, Object forgeEvent) {
        this.self = self;
        this.victim = victim;
        this.weaponItem = weaponItem;
        this.forgeEvent = forgeEvent;
    }
    
    /**
     * 检查主手是否持有指定武器
     */
    public boolean isHoldingInMainHand() {
        if (self == null) return false;
        Item mainHand = self.getHeldItemMainhand().getItem();
        return mainHand == weaponItem;
    }
    
    /**
     * 检查是否为攻击来源
     */
    public boolean isSelfAttacker() {
        // 由具体事件处理器实现
        return false;
    }
}
