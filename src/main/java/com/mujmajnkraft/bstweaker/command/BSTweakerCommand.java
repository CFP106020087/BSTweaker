package com.mujmajnkraft.bstweaker.command;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * /bstweaker reload 命令 - 热重载配置文件
 */
public class BSTweakerCommand extends CommandBase {

    @Override
    public String getName() {
        return "bstweaker";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bstweaker reload - Reload tooltips, scripts, lang, and resources";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP level
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "reload");
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Usage: /bstweaker reload"));
            return;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            executeReload(sender);
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown subcommand: " + args[0]));
        }
    }

    private void executeReload(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "[BSTweaker] Reloading configs..."));

        int reloaded = 0;

        try {
            // 1. 重新加载 tooltips 和 scripts
            TweakerWeaponInjector.reloadConfigs();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "  ✓ Reloaded tooltips.json and scripts.json"));
            reloaded++;

            // 2. 重新复制资源文件 (models, textures, lang)
            com.mujmajnkraft.bstweaker.util.ResourceInjector.injectResources();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "  ✓ Re-injected resource files"));
            reloaded++;

            // 3. 触发资源重载 (客户端)
            if (sender.getEntityWorld().isRemote || isClientSide()) {
                refreshClientResources();
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "  ✓ Refreshed client resources"));
                reloaded++;
            }

            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[BSTweaker] Reload complete! (" + reloaded + " tasks)"));
            BSTweaker.LOG.info("Hot reload completed successfully");

        } catch (Exception e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "[BSTweaker] Reload failed: " + e.getMessage()));
            BSTweaker.LOG.error("Hot reload failed", e);
        }
    }

    private boolean isClientSide() {
        try {
            return net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide() == Side.CLIENT;
        } catch (Exception e) {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    private void refreshClientResources() {
        try {
            Minecraft.getMinecraft().refreshResources();
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to refresh client resources", e);
        }
    }
}
