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

/**
 * Tooltip 事件处理器 - 自动添加武器说明
 */
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
        
        // 添加自定义 tooltip
        if (def.has("tooltip")) {
            JsonElement tooltipElem = def.get("tooltip");
            
            if (tooltipElem.isJsonArray()) {
                // 多行 tooltip
                JsonArray lines = tooltipElem.getAsJsonArray();
                for (JsonElement line : lines) {
                    tooltip.add(formatTooltipLine(line.getAsString()));
                }
            } else {
                // 单行 tooltip
                tooltip.add(formatTooltipLine(tooltipElem.getAsString()));
            }
        }
        
        // 自动生成效果描述
        if (def.has("events")) {
            JsonArray events = def.getAsJsonArray("events");
            for (JsonElement elem : events) {
                JsonObject eventDef = elem.getAsJsonObject();
                if (eventDef.has("_comment")) {
                    String comment = eventDef.get("_comment").getAsString();
                    // 支持翻译键
                    if (comment.startsWith("@")) {
                        comment = I18n.format(comment.substring(1));
                    }
                    tooltip.add(TextFormatting.DARK_PURPLE + "◆ " + comment);
                }
            }
        }
    }
    
    /**
     * 格式化 tooltip 行，支持颜色代码和翻译键
     * 如果以 @ 开头，视为翻译键，使用 I18n 翻译
     */
    private static String formatTooltipLine(String line) {
        // 支持翻译键: @key.name -> I18n.format("key.name")
        if (line.startsWith("@")) {
            String key = line.substring(1);
            line = I18n.format(key);
        }

        // 支持 & 颜色代码
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
