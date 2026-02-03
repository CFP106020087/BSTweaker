package com.mujmajnkraft.bstweaker.effects.actions;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.effects.EventAction;
import com.mujmajnkraft.bstweaker.effects.EventContext;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Field operation action - compound assignments (+=, -=, *=, /=). */
public class FieldOperationAction implements EventAction {
    
    private final String target;
    private final String fieldName;
    private final String operator;
    private final String value;
    
    public FieldOperationAction(String target, String fieldName, String operator, String value) {
        this.target = target;
        this.fieldName = fieldName;
        this.operator = operator;
        this.value = value;
    }
    
    @Override
    public void execute(EventContext context) {
        // Special handling for LivingHurtEvent.amount
        if ("event".equals(target) && "amount".equals(fieldName) 
            && context.forgeEvent instanceof LivingHurtEvent) {
            
            LivingHurtEvent event = (LivingHurtEvent) context.forgeEvent;
            float current = event.getAmount();
            float operand = Float.parseFloat(value);
            float newValue = calculate(current, operand);
            event.setAmount(newValue);
            return;
        }
        
        BSTweaker.LOG.warn("Unsupported field operation: " + target + "." + fieldName);
    }
    
    private float calculate(float current, float operand) {
        switch (operator) {
            case "+": return current + operand;
            case "-": return current - operand;
            case "*": return current * operand;
            case "/": return operand != 0 ? current / operand : current;
            default: return current;
        }
    }
}
