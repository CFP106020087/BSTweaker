package com.mujmajnkraft.bstweaker.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

/** Tooltip event handler - adds weapon descriptions. */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        
        Item item = stack.getItem();
        JsonObject def = TweakerWeaponInjector.getDefinition(item);
        if (def == null) return;
        
        List<String> tooltip = event.getToolTip();
        
        // Replace item name with displayName (first line of tooltip)
        if (!tooltip.isEmpty()) {
            String displayName = null;

            if (def.has("displayName")) {
                displayName = def.get("displayName").getAsString();
            } else if (def.has("id")) {
                // Auto-generate from id: "emeralddagger" -> "Emeralddagger"
                String id = def.get("id").getAsString();
                displayName = id.substring(0, 1).toUpperCase() + id.substring(1);
            }

            if (displayName != null) {
                // Support translation keys: @key.name -> I18n.format("key.name")
                if (displayName.startsWith("@")) {
                    displayName = I18n.format(displayName.substring(1));
                }
                // Support color codes
                displayName = formatTooltipLine(displayName);
                // Replace first line (original item name)
                tooltip.set(0, displayName);
            }
        }

        // Add custom tooltip
        if (def.has("tooltip")) {
            JsonElement tooltipElem = def.get("tooltip");
            
            if (tooltipElem.isJsonArray()) {
                JsonArray lines = tooltipElem.getAsJsonArray();
                for (JsonElement line : lines) {
                    tooltip.add(formatTooltipLine(line.getAsString()));
                }
            } else {
                tooltip.add(formatTooltipLine(tooltipElem.getAsString()));
            }
        }
        
        // Auto-generate effect description from events
        if (def.has("events")) {
            JsonArray events = def.getAsJsonArray("events");
            for (JsonElement elem : events) {
                JsonObject eventDef = elem.getAsJsonObject();
                if (eventDef.has("_comment")) {
                    String comment = eventDef.get("_comment").getAsString();
                    // Support translation keys
                    if (comment.startsWith("@")) {
                        comment = I18n.format(comment.substring(1));
                    }
                    tooltip.add(TextFormatting.DARK_PURPLE + "- " + comment);
                }
            }
        }
    }
    
    /** Format tooltip line with color codes and translation keys. */
    private static String formatTooltipLine(String line) {
        // Translation key: @key.name -> I18n.format("key.name")
        if (line.startsWith("@")) {
            String key = line.substring(1);
            line = I18n.format(key);
        }

        // Support & color codes
        line = line.replace("&0", TextFormatting.BLACK.toString())
                   .replace("&1", TextFormatting.DARK_BLUE.toString())
                   .replace("&2", TextFormatting.DARK_GREEN.toString())
                   .replace("&3", TextFormatting.DARK_AQUA.toString())
                   .replace("&4", TextFormatting.DARK_RED.toString())
                   .replace("&5", TextFormatting.DARK_PURPLE.toString())
                   .replace("&6", TextFormatting.GOLD.toString())
                   .replace("&7", TextFormatting.GRAY.toString())
                   .replace("&8", TextFormatting.DARK_GRAY.toString())
                   .replace("&9", TextFormatting.BLUE.toString())
                   .replace("&a", TextFormatting.GREEN.toString())
                   .replace("&b", TextFormatting.AQUA.toString())
                   .replace("&c", TextFormatting.RED.toString())
                   .replace("&d", TextFormatting.LIGHT_PURPLE.toString())
                   .replace("&e", TextFormatting.YELLOW.toString())
                   .replace("&f", TextFormatting.WHITE.toString())
                   .replace("&l", TextFormatting.BOLD.toString())
                   .replace("&o", TextFormatting.ITALIC.toString())
                   .replace("&n", TextFormatting.UNDERLINE.toString())
                   .replace("&r", TextFormatting.RESET.toString());
        return line;
    }
}
