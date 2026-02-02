package com.mujmajnkraft.bstweaker;

import java.io.File;

import com.mujmajnkraft.bstweaker.config.ConfigLoader;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;
import com.mujmajnkraft.bstweaker.proxy.CommonProxy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * BetterSurvival Tweaker - JSON配置式武器扩展模组
 * 
 * 允许用户通过JSON配置文件添加自定义BetterSurvival武器。
 */
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, acceptedMinecraftVersions = Reference.MC_VERSION, dependencies = Reference.DEPENDENCIES)
public class BSTweaker {

    @Instance
    public static BSTweaker instance;

    public static Logger LOG = LogManager.getLogger(Reference.MOD_ID);

    public static File configDir;

    @SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.SERVER_PROXY_CLASS)
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("BSTweaker PreInit - Loading weapon configurations...");

        // 初始化配置目录
        configDir = new File(event.getModConfigurationDirectory(), Reference.MOD_ID);

        // 加载JSON配置 (Mixin 会在 BS ModItems.registerItems 时读取)
        ConfigLoader.init(event.getModConfigurationDirectory());

        // 注意: 物品注册现在通过 Mixin 注入到 BetterSurvival 的 ModItems.registerItems
        // 不再需要独立的 TweakerItems 注册

        proxy.preInit();

        LOG.info("BSTweaker PreInit complete.");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("BSTweaker Init...");

        proxy.init();

        LOG.info("BSTweaker Init complete.");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("BSTweaker PostInit...");

        proxy.postInit();

        LOG.info(
                "BSTweaker PostInit complete. Loaded " + TweakerWeaponInjector.getItemDefinitionMap().size()
                        + " custom weapons.");
    }
}
