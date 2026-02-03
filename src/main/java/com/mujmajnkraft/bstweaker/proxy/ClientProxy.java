package com.mujmajnkraft.bstweaker.proxy;

import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/** Client-side proxy - handles model and render registration. */
public class ClientProxy extends CommonProxy {

    private static final ResourceLocation ALWAYS_PROPERTY = new ResourceLocation("bstweaker", "always");

    @Override
    public void preInit() {
        super.preInit();
        // Register dynamic resource pack before resources load
        com.mujmajnkraft.bstweaker.client.ClientEventHandler.registerDynamicResourcePack();
    }

    @Override
    public void init() {
        super.init();
        registerItemRenders();
        registerAlwaysPredicate();
    }

    /** Register item render models. */
    private void registerItemRenders() {
        int count = 0;
        for (Item item : TweakerWeaponInjector.getItemDefinitionMap().keySet()) {
            registerItemRender(item);
            count++;
        }
        BSTweaker.LOG.info("Registered renders for " + count + " tweaked weapons.");
    }

    private void registerItemRender(Item item) {
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    /** Register bstweaker:always predicate (always returns 1.0) for hot-reload. */
    private void registerAlwaysPredicate() {
        int count = 0;
        for (Item item : TweakerWeaponInjector.getItemDefinitionMap().keySet()) {
            item.addPropertyOverride(ALWAYS_PROPERTY, (stack, world, entity) -> 1.0f);
            count++;
        }
        BSTweaker.LOG.info("Registered 'always' predicate for " + count + " weapons for hot-reload support.");
    }
}
