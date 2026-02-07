package com.mujmajnkraft.bstweaker.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Weapon event config - parsed from JSON. */
public class WeaponEvent {
    
    public final String eventType; // Event type
    public final String condition; // Condition expression
    public final List<String> actions; // Action list
    public final String comment; // Comment
    
    public WeaponEvent(String eventType, String condition, List<String> actions, String comment) {
        this.eventType = eventType;
        this.condition = condition;
        this.actions = actions;
        this.comment = comment;
    }
    
    /** Parse event config from JSON. */
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
    
    /** Parse event list from JSON array. */
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
