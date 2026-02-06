package com.mujmajnkraft.bstweaker.effects;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.effects.actions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Action parser - parses strings into EventAction. */
public class ActionParser {
    
    // Match method call: target.method(args)
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(\\w+)\\.(\\w+)\\(([^)]*)\\)"
    );
    
    // Match assignment: target.field = value
    private static final Pattern ASSIGN_PATTERN = Pattern.compile(
        "(\\w+)\\.(\\w+)\\s*=\\s*(.+)"
    );
    
    // Match compound assignment: target.field *= value
    private static final Pattern OP_ASSIGN_PATTERN = Pattern.compile(
        "(\\w+)\\.(\\w+)\\s*([+\\-*/])=\\s*(.+)"
    );
    
    /** Parse action string. */
    public static EventAction parse(String actionStr) {
        actionStr = actionStr.trim();
        
        // Try compound assignment
        Matcher opAssign = OP_ASSIGN_PATTERN.matcher(actionStr);
        if (opAssign.matches()) {
            String target = opAssign.group(1);
            String field = opAssign.group(2);
            String op = opAssign.group(3);
            String value = opAssign.group(4).trim();
            return new FieldOperationAction(target, field, op, value);
        }
        
        // Try assignment
        Matcher assign = ASSIGN_PATTERN.matcher(actionStr);
        if (assign.matches()) {
            String target = assign.group(1);
            String field = assign.group(2);
            String value = assign.group(3).trim();
            return new FieldSetAction(target, field, value);
        }
        
        // Try method call
        Matcher method = METHOD_PATTERN.matcher(actionStr);
        if (method.matches()) {
            String target = method.group(1);
            String methodName = method.group(2);
            String args = method.group(3);
            return parseMethodCall(target, methodName, args);
        }
        
        BSTweaker.LOG.warn("Unknown action format: " + actionStr);
        return ctx -> {
        }; // No-op
    }
    
    /** Parse method call. */
    private static EventAction parseMethodCall(String target, String method, String args) {
        String[] argParts = args.isEmpty() ? new String[0] : args.split(",");
        for (int i = 0; i < argParts.length; i++) {
            argParts[i] = argParts[i].trim().replace("'", "").replace("\"", "");
        }
        
        switch (method) {
            case "suppressPotion":
                return new SuppressPotionAction(target, argParts);
            case "addPotion":
            case "applyPotion":
                return new ApplyPotionAction(target, argParts);
            case "removePotion":
                return new RemovePotionAction(target, argParts);
            case "heal":
                return new HealAction(target, argParts);
            case "damage":
                return new DamageAction(target, argParts);
            case "ignite":
                return new IgniteAction(target, argParts);
            case "cancel":
                return new CancelEventAction();
            default:
                BSTweaker.LOG.warn("Unknown method: " + method);
                return ctx -> {};
        }
    }
}
