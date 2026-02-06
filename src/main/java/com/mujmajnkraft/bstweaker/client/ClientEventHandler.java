package com.mujmajnkraft.bstweaker.client;

import com.google.gson.JsonObject;
import com.mujmajnkraft.bstweaker.BSTweaker;
import com.mujmajnkraft.bstweaker.Reference;
import com.mujmajnkraft.bstweaker.util.TweakerWeaponInjector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Client event handler - model registration with automatic texture redirection.
 * Features: auto texture redirect, dynamic model generation, .mcmeta animation
 * support.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public class ClientEventHandler {

    private static boolean resourcePackRegistered = false;
    private static boolean hasRefreshedThisSession = false;
    private static Field defaultResourcePacksField = null;

    /** Register dynamic resource pack (must call before model registration). */
    public static void registerDynamicResourcePack() {
        if (resourcePackRegistered)
            return;

        try {
            // Scan config resources first
            DynamicResourcePack.scanConfigResources();

            // Get or cache reflection field
            if (defaultResourcePacksField == null) {
                // SRG: field_110449_ao = defaultResourcePacks
                defaultResourcePacksField = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                        Minecraft.class, "defaultResourcePacks", "field_110449_ao");
            }

            @SuppressWarnings("unchecked")
            List<IResourcePack> packs = (List<IResourcePack>) defaultResourcePacksField.get(Minecraft.getMinecraft());

            // Add at position 0 for highest priority
            packs.add(0, new DynamicResourcePack());

            resourcePackRegistered = true;
            BSTweaker.LOG.info("Dynamic resource pack registered at position 0 (highest priority)");
        } catch (Exception e) {
            BSTweaker.LOG.error("Failed to register dynamic resource pack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Called from Mixin - register models immediately after item registration. */
    @SideOnly(Side.CLIENT)
    public static void registerModelsForItems(List<Item> items) {
        int count = 0;
        for (Item item : items) {
            JsonObject def = TweakerWeaponInjector.getDefinition(item);
            if (def == null)
                continue;

            // Use item registry name as model location
            ResourceLocation regName = item.getRegistryName();
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(regName, "inventory"));

            BSTweaker.LOG.info("Registered model: " + regName);
            count++;
        }

        BSTweaker.LOG.info("Registered " + count + " models for custom weapons.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onModelRegistry(ModelRegistryEvent event) {
        Map<Item, JsonObject> itemMap = TweakerWeaponInjector.getItemDefinitionMap();
        BSTweaker.LOG.info("onModelRegistry: itemDefinitionMap size = " + itemMap.size());

        if (itemMap.isEmpty()) {
            BSTweaker.LOG.info("itemDefinitionMap is empty - models will be registered via Mixin");
            return;
        }

        int count = 0;
        for (Map.Entry<Item, JsonObject> entry : itemMap.entrySet()) {
            Item item = entry.getKey();

            // Use item registry name as model location
            ResourceLocation regName = item.getRegistryName();
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    0,
                    new ModelResourceLocation(regName, "inventory"));

            BSTweaker.LOG.info("Registered model: " + regName);
            count++;
        }

        BSTweaker.LOG.info("Registered " + count + " models.");
    }

    /**
     * Get texture name from weapon definition. Priority: texture field > id field.
     */
    private static String getTextureName(JsonObject def) {
        if (def.has("texture")) {
            String texture = def.get("texture").getAsString();
            if (texture.contains(":")) {
                texture = texture.substring(texture.indexOf(":") + 1);
            }
            if (texture.contains("/")) {
                texture = texture.substring(texture.lastIndexOf("/") + 1);
            }
            return texture;
        }

        if (def.has("id")) {
            return def.get("id").getAsString();
        }

        return "missing";
    }

    /** Auto-refresh resources on first world join (fixes texture loading). */
    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (hasRefreshedThisSession)
            return;
        if (!(event.getEntity() instanceof EntityPlayer))
            return;
        if (!event.getWorld().isRemote)
            return; // Client only

        hasRefreshedThisSession = true;
        BSTweaker.LOG.info("First world join - using fast reload for weapon models...");

        Minecraft mc = Minecraft.getMinecraft();
        // Use fast reload instead of full mc.refreshResources()
        mc.addScheduledTask(() -> {
            BSTweaker.LOG.info("Executing fast texture reload...");
            FastTextureReloader.reloadWeaponTextures();

            BSTweaker.LOG.info("Executing fast model reload...");
            FastTextureReloader.reloadModels();

            BSTweaker.LOG.info("Fast reload complete - spinning should now work.");
        });
    }

    /**
     * Register ALL config textures to texture atlas at stitch time.
     * This enables hot-reload for all textures by pre-registering them in atlas.
     */
    @SubscribeEvent
    public static void onTextureStitchPre(TextureStitchEvent.Pre event) {
        // Only handle item texture map
        if (event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }

        // Ensure DynamicResourcePack has scanned resources
        DynamicResourcePack.scanConfigResources();

        // Register all config textures to the atlas
        java.util.Set<String> textureNames = DynamicResourcePack.getTextureNames();
        int registered = 0;

        for (String textureName : textureNames) {
            // Register in BS namespace (where models reference them)
            String bsPath = "mujmajnkraftsbettersurvival:items/" + textureName;
            event.getMap().registerSprite(new ResourceLocation(bsPath));

            // Also register in bstweaker namespace for fallback
            String bstPath = "bstweaker:items/" + textureName;
            event.getMap().registerSprite(new ResourceLocation(bstPath));

            registered++;
        }

        BSTweaker.LOG.info("Registered " + registered + " config textures to atlas (both BS and BST namespaces)");
    }
}
