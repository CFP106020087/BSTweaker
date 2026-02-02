package com.mujmajnkraft.bstweaker.effects;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.effects.actions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动作解析器 - 将字符串解析为 EventAction
 */
public class ActionParser {
    
    // 匹配方法调用: target.method(args) 或 self.method(args) 或 victim.method(args)
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(\\w+)\\.(\\w+)\\(([^)]*)\\)"
    );
    
    // 匹配赋值: target.field = value
    private static final Pattern ASSIGN_PATTERN = Pattern.compile(
        "(\\w+)\\.(\\w+)\\s*=\\s*(.+)"
    );
    
    // 匹配运算赋值: target.field *= value
    private static final Pattern OP_ASSIGN_PATTERN = Pattern.compile(
        "(\\w+)\\.(\\w+)\\s*([+\\-*/])=\\s*(.+)"
    );
    
    /**
     * 解析动作字符串
     */
    public static EventAction parse(String actionStr) {
        actionStr = actionStr.trim();
        
        // 尝试解析运算赋值
        Matcher opAssign = OP_ASSIGN_PATTERN.matcher(actionStr);
        if (opAssign.matches()) {
            String target = opAssign.group(1);
            String field = opAssign.group(2);
            String op = opAssign.group(3);
            String value = opAssign.group(4).trim();
            return new FieldOperationAction(target, field, op, value);
        }
        
        // 尝试解析赋值
        Matcher assign = ASSIGN_PATTERN.matcher(actionStr);
        if (assign.matches()) {
            String target = assign.group(1);
            String field = assign.group(2);
            String value = assign.group(3).trim();
            return new FieldSetAction(target, field, value);
        }
        
        // 尝试解析方法调用
        Matcher method = METHOD_PATTERN.matcher(actionStr);
        if (method.matches()) {
            String target = method.group(1);
            String methodName = method.group(2);
            String args = method.group(3);
            return parseMethodCall(target, methodName, args);
        }
        
        BSTweaker.LOG.warn("Unknown action format: " + actionStr);
        return ctx -> {}; // 空操作
    }
    
    /**
     * 解析方法调用
     */
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
