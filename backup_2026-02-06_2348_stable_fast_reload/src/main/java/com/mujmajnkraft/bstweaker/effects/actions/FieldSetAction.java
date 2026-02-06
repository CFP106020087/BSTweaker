package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.lang.reflect.Field;

/** Field set action - sets entity or event field values. */
public class FieldSetAction implements EventAction {
    
    private final String target;
    private final String fieldName;
    private final String value;
    
    public FieldSetAction(String target, String fieldName, String value) {
        this.target = target;
        this.fieldName = fieldName;
        this.value = value;
    }
    
    @Override
    public void execute(EventContext context) {
        Object targetObj = getTargetObject(context);
        if (targetObj == null) return;
        
        try {
            // Handle common fields directly
            if (targetObj instanceof Entity && "hurtResistantTime".equals(fieldName)) {
                ((Entity) targetObj).hurtResistantTime = parseIntValue(value);
                return;
            }
            
            if (targetObj instanceof LivingHurtEvent && "amount".equals(fieldName)) {
                ((LivingHurtEvent) targetObj).setAmount(parseFloatValue(value));
                return;
            }
            
            // Generic reflection
            Field field = findField(targetObj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object val = parseValue(value, field.getType());
                field.set(targetObj, val);
            }
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to set field " + fieldName + ": " + e.getMessage());
        }
    }
    
    private Object getTargetObject(EventContext context) {
        switch (target) {
            case "self": return context.self;
            case "victim": return context.victim;
            case "event": return context.forgeEvent;
            default: return null;
        }
    }
    
    private Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    private Object parseValue(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) return parseIntValue(value);
        if (type == float.class || type == Float.class) return parseFloatValue(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        return value;
    }
    
    private int parseIntValue(String value) {
        return (int) Float.parseFloat(value);
    }
    
    private float parseFloatValue(String value) {
        return Float.parseFloat(value);
    }
}
