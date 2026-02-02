package com.mujmajnkraft.bstweaker.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 武器事件配置 - 从 JSON 解析
 */
public class WeaponEvent {
    
    public final String eventType;      // Forge 事件类型
    public final String condition;       // 条件表达式
    public final List<String> actions;   // 动作列表
    public final String comment;         // 注释
    
    public WeaponEvent(String eventType, String condition, List<String> actions, String comment) {
        this.eventType = eventType;
        this.condition = condition;
        this.actions = actions;
        this.comment = comment;
    }
    
    /**
     * 从 JSON 解析事件配置
     */
    public static WeaponEvent fromJson(JsonObject json) {
        String eventType = json.has("event") ? json.get("event").getAsString() : "";
        String condition = json.has("when") ? json.get("when").getAsString() : "";
        String comment = json.has("_comment") ? json.get("_comment").getAsString() : "";
        
        List<String> actions = new ArrayList<>();
        if (json.has("actions")) {
            JsonArray arr = json.getAsJsonArray("actions");
            for (JsonElement elem : arr) {
                actions.add(elem.getAsString());
            }
        }
        
        return new WeaponEvent(eventType, condition, actions, comment);
    }
    
    /**
     * 从 JSON 数组解析事件列表
     */
    public static List<WeaponEvent> fromJsonArray(JsonArray array) {
        List<WeaponEvent> events = new ArrayList<>();
        for (JsonElement elem : array) {
            if (elem.isJsonObject()) {
                events.add(fromJson(elem.getAsJsonObject()));
            }
        }
        return events;
    }
}
