package com.mujmajnkraft.bstweaker;

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

/** BetterSurvival Tweaker - JSON-based weapon extension mod. */
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, acceptedMinecraftVersions = Reference.MC_VERSION, dependencies = Reference.DEPENDENCIES)
public class BSTweaker {

    @Instance
    public static BSTweaker instance;

    public static Logger LOG = LogManager.getLogger(Reference.MOD_ID);

    @SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.SERVER_PROXY_CLASS)
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("BSTweaker PreInit");
        // Inject resources before model loading
        com.mujmajnkraft.bstweaker.util.ResourceInjector.injectResources();
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("BSTweaker Init");
        proxy.init();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        int weaponCount = TweakerWeaponInjector.getItemDefinitionMap().size();
        LOG.info("BSTweaker PostInit - Loaded " + weaponCount + " custom weapons");
    }

    @EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new com.mujmajnkraft.bstweaker.command.BSTweakerCommand());
        LOG.info("BSTweaker command registered");
    }
}
