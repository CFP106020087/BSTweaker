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
import java.util.List;

/** /bstweaker reload command - hot-reload config files. */
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

        try {
            // 1. Reload tooltips and scripts
            TweakerWeaponInjector.reloadConfigs();
            sender.sendMessage(
                    new TextComponentString(TextFormatting.GREEN + "  ✓ Reloaded tooltips.json and scripts.json"));

            // 2. Rescan resource files (textures/models/lang in config dir)
            if (isClientSide()) {
                com.mujmajnkraft.bstweaker.client.DynamicResourcePack.rescan();
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "  ✓ Rescanned config resources"));
            }

            sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "[BSTweaker] Reload complete! Use F3+T if textures need refresh."));

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
}
