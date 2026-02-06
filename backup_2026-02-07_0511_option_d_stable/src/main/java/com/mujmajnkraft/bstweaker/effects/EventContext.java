package com.mujmajnkraft.bstweaker.effects;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;

/** Event execution context - contains all available variables. */
public class EventContext {
    
    public final EntityLivingBase self; // Weapon holder
    public final EntityLivingBase victim; // Victim (may be null)
    public final Item weaponItem; // Trigger weapon
    public final Object forgeEvent; // Original Forge event
    
    public EventContext(EntityLivingBase self, EntityLivingBase victim, Item weaponItem, Object forgeEvent) {
        this.self = self;
        this.victim = victim;
        this.weaponItem = weaponItem;
        this.forgeEvent = forgeEvent;
    }
    
    /** Check if holding weapon in main hand. */
    public boolean isHoldingInMainHand() {
        if (self == null) return false;
        Item mainHand = self.getHeldItemMainhand().getItem();
        return mainHand == weaponItem;
    }
    
    /** Check if self is attacker. */
    public boolean isSelfAttacker() {
        return false;
    }
}
