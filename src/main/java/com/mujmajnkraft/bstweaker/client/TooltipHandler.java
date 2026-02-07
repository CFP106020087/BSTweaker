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
            String currentName = tooltip.get(0);
            String displayName = null;

            // Check if current name is an untranslated key (raw key shown)
            if (currentName.startsWith("bstweaker.") || currentName.startsWith("item.")) {
                // Try to translate the raw key using ConfigLangLoader
                if (ConfigLangLoader.hasTranslation(currentName)) {
                    displayName = ConfigLangLoader.translate(currentName);
                }
            }

            // If still no displayName, try from definition
            if (displayName == null && def.has("displayName")) {
                displayName = def.get("displayName").getAsString();
            } else if (displayName == null && def.has("id")) {
                // Auto-generate from id: "emeralddagger" -> "Emeralddagger"
                String id = def.get("id").getAsString();
                // Try ConfigLangLoader first with weapon key
                String weaponKey = "bstweaker.weapon." + id + ".name";
                if (ConfigLangLoader.hasTranslation(weaponKey)) {
                    displayName = ConfigLangLoader.translate(weaponKey);
                } else {
                    displayName = id.substring(0, 1).toUpperCase() + id.substring(1);
                }
            }

            if (displayName != null) {
                // Support translation keys: @key.name -> translate
                if (displayName.startsWith("@")) {
                    String key = displayName.substring(1);
                    // Use ConfigLangLoader for bstweaker keys
                    if (key.startsWith("bstweaker.") && ConfigLangLoader.hasTranslation(key)) {
                        displayName = ConfigLangLoader.translate(key);
                    } else {
                        displayName = I18n.format(key);
                    }
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
                        String key = comment.substring(1);
                        if (key.startsWith("bstweaker.") && ConfigLangLoader.hasTranslation(key)) {
                            comment = ConfigLangLoader.translate(key);
                        } else {
                            comment = I18n.format(key);
                        }
                    }
                    tooltip.add(TextFormatting.DARK_PURPLE + "- " + comment);
                }
            }
        }

        // Placeholder texture warning
        if (def.has("_placeholder") && def.get("_placeholder").getAsBoolean()) {
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "" + TextFormatting.ITALIC
                    + "\u26a0 \u5360\u4f4d\u7eb9\u7406 (Placeholder)");
            tooltip.add(TextFormatting.GRAY
                    + "\u5c06\u81ea\u5b9a\u4e49\u7eb9\u7406\u653e\u5165 config/bstweaker/textures/");
            tooltip.add(TextFormatting.GRAY + "\u7136\u540e\u4f7f\u7528 " + TextFormatting.AQUA + "/bstweaker reload"
                    + TextFormatting.GRAY + " \u91cd\u8f7d");
        }
    }
    
    /** Format tooltip line with color codes and translation keys. */
    private static String formatTooltipLine(String line) {
        // Translation key: @key.name -> translate
        if (line.startsWith("@")) {
            String key = line.substring(1);
            // Try ConfigLangLoader first for bstweaker keys
            if (key.startsWith("bstweaker.") && ConfigLangLoader.hasTranslation(key)) {
                line = ConfigLangLoader.translate(key);
            } else {
                line = I18n.format(key);
            }
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
