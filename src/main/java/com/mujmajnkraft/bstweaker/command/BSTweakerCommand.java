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

            // 3. 触发资源重载 (客户端) - 必须在客户端主线程执行
            if (isClientSide()) {
                scheduleClientRefresh();
                sender.sendMessage(
                        new TextComponentString(TextFormatting.GREEN + "  ✓ Scheduled client resource refresh"));
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
    private void scheduleClientRefresh() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            try {
                BSTweaker.LOG.info("Starting targeted texture hot-reload...");

                // 1. 重新扫描 DynamicResourcePack 的资源
                com.mujmajnkraft.bstweaker.client.DynamicResourcePack.rescan();

                // 2. 删除 GPU 缓存的纹理
                net.minecraft.client.renderer.texture.TextureManager texManager = mc.getTextureManager();
                for (net.minecraft.util.ResourceLocation loc : com.mujmajnkraft.bstweaker.client.DynamicResourcePack
                        .getTextureLocations()) {
                    texManager.deleteTexture(loc);
                }

                // 3. 只重建物品纹理图集 (比 refreshResources 快很多!)
                net.minecraft.client.renderer.texture.TextureMap texMap = mc.getTextureMapBlocks();
                texMap.loadTextureAtlas(mc.getResourceManager());
                BSTweaker.LOG.info("Rebuilt texture atlas");

                // 4. 重建模型缓存
                mc.getRenderItem().getItemModelMesher().rebuildCache();

                BSTweaker.LOG.info("Targeted texture hot-reload complete!");
            } catch (Exception e) {
                BSTweaker.LOG.error("Texture hot-reload failed: " + e.getMessage());
                // 如果快速方法失败，回退到完整刷新
                BSTweaker.LOG.info("Falling back to full resource refresh...");
                mc.refreshResources();
            }
        });
    }
}
