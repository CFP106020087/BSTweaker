package com.mujmajnkraft.bstweaker.validation;

import com.mujmajnkraft.bstweaker.Reference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 配置错误玩家提醒
 * 玩家进入游戏后显示红色错误消息（类似 CraftTweaker）
 * 
 * 修复线程安全问题：使用游戏主线程 tick 延迟发送
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ValidationErrorNotifier {

    private static boolean hasNotifiedThisSession = false;

    // 使用 WeakReference 避免内存泄漏
    private static WeakReference<EntityPlayer> pendingPlayer = null;
    private static int ticksRemaining = -1;

    /**
     * 玩家登录时检查并显示错误
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (hasNotifiedThisSession)
            return;

        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote)
            return;

        ConfigValidationErrors errors = ConfigValidationErrors.getInstance();

        if (errors.shouldShowToPlayer()) {
            // 使用主线程延迟发送（20 tick = 1 秒）
            pendingPlayer = new WeakReference<>(player);
            ticksRemaining = 20;
        }
    }

    /**
     * 服务器 tick 事件 - 安全地延迟发送消息
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (ticksRemaining < 0)
            return;

        ticksRemaining--;

        if (ticksRemaining == 0) {
            ticksRemaining = -1;

            EntityPlayer player = pendingPlayer != null ? pendingPlayer.get() : null;
            pendingPlayer = null;

            // 检查玩家是否仍在线
            if (player != null && !player.isDead && player.world != null) {
                ConfigValidationErrors errors = ConfigValidationErrors.getInstance();
                // 使用服务端安全的消息（英文）
                List<String> messages = errors.getChatMessagesServer();

                for (String msg : messages) {
                    player.sendMessage(new TextComponentString(msg));
                }

                errors.markAsShown();
                hasNotifiedThisSession = true;
            }
        }
    }

    /**
     * 手动触发提醒（用于 /bstweaker reload 后）
     */
    public static void notifyPlayer(EntityPlayer player) {
        if (player == null)
            return;

        ConfigValidationErrors errors = ConfigValidationErrors.getInstance();
        // 使用服务端安全的消息（英文）
        List<String> messages = errors.getChatMessagesServer();

        for (String msg : messages) {
            player.sendMessage(new TextComponentString(msg));
        }
    }

    /**
     * 重置会话状态（用于热重载后）
     */
    public static void resetSession() {
        hasNotifiedThisSession = false;
        pendingPlayer = null;
        ticksRemaining = -1;
    }
}
