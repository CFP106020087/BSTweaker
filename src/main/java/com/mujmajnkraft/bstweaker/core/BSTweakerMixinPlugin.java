package com.mujmajnkraft.bstweaker.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import fermiumbooter.FermiumRegistryAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "com.mujmajnkraft.bstweaker.core" })
@IFMLLoadingPlugin.Name("BSTweakerCore")
@IFMLLoadingPlugin.SortingIndex(1005)
public class BSTweakerMixinPlugin implements IFMLLoadingPlugin {

    private static final Logger LOGGER = LogManager.getLogger("BSTweaker");

    static {
        try {
            // 注册 BetterSurvival 注入 Mixin (late mixin，在 BS 加载后)
            FermiumRegistryAPI.enqueueMixin(true, "mixins.bstweaker.json");
            LOGGER.info("Mixin queued via FermiumBooter");
        } catch (Throwable e) {
            LOGGER.error("FermiumBooter registration failed: " + e);
        }
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
