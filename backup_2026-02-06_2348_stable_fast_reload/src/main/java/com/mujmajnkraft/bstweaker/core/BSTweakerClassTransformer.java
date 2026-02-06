package com.mujmajnkraft.bstweaker.core;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * ASM ClassTransformer to inject DynamicResourcePack into
 * Minecraft.refreshResources.
 * Based on ResourceLoader's implementation pattern.
 */
public class BSTweakerClassTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("BSTweaker");
    private static Map<String, String> methods;
    private static boolean inMcp = false;

    static {
        // Detect MCP environment
        try {
            Class<?> gradleClass = Class.forName("net.minecraftforge.gradle.GradleStartCommon");
            inMcp = true;

            Field dirField = gradleClass.getDeclaredField("CSV_DIR");
            dirField.setAccessible(true);
            File mappingDir = (File) dirField.get(null);
            methods = readMappings(new File(mappingDir, "methods.csv"));
            LOGGER.info("[ASM] Loaded MCP method mappings");
        } catch (Exception e) {
            inMcp = false;
            methods = null;
            LOGGER.info("[ASM] Running in obfuscated environment");
        }
    }

    private static String method(String srgName) {
        if (inMcp && methods != null && methods.containsKey(srgName)) {
            return methods.get(srgName);
        }
        return srgName;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readMappings(File file) {
        if (!file.isFile()) {
            return Maps.newHashMap();
        }
        try {
            return Files.readLines(file, Charsets.UTF_8, new LineProcessor<Map<String, String>>() {
                private final Splitter splitter = Splitter.on(',').trimResults();
                private final Map<String, String> map = Maps.newHashMap();
                private boolean foundFirst = false;

                @Override
                public boolean processLine(String line) {
                    if (!foundFirst) {
                        foundFirst = true;
                        return true;
                    }
                    try {
                        java.util.Iterator<String> it = splitter.split(line).iterator();
                        String srg = it.next();
                        String mcp = it.next();
                        map.put(srg, mcp);
                    } catch (Exception ignored) {
                    }
                    return true;
                }

                @Override
                public Map<String, String> getResult() {
                    return ImmutableMap.copyOf(map);
                }
            });
        } catch (Exception e) {
            return Maps.newHashMap();
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null)
            return null;

        if (transformedName.equals("net.minecraft.client.Minecraft")) {
            return patchMinecraft(basicClass);
        }
        return basicClass;
    }

    private byte[] patchMinecraft(byte[] basicClass) {
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            LOGGER.info("[ASM] Found Minecraft class: " + classNode.name);

            // Find refreshResources method (func_110436_a)
            String refreshResourcesName = method("func_110436_a");
            // Find reloadResources target (func_110541_a in IReloadableResourceManager)
            String reloadResourcesName = method("func_110541_a");

            MethodNode refreshResources = null;
            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals(refreshResourcesName)) {
                    refreshResources = mn;
                    break;
                }
            }

            if (refreshResources != null) {
                LOGGER.info("[ASM] Found refreshResources method: " + refreshResources.name);

                for (int i = 0; i < refreshResources.instructions.size(); i++) {
                    AbstractInsnNode ain = refreshResources.instructions.get(i);
                    if (ain instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) ain;

                        // Look for call to IReloadableResourceManager.reloadResources(List)
                        // func_110541_a or reloadResources
                        if (min.name.equals(reloadResourcesName) || min.name.equals("reloadResources")) {
                            LOGGER.info("[ASM] Found reloadResources call at index " + i + ", injecting...");

                            // Insert our pack injection BEFORE the reloadResources call
                            // The list is already on stack (from local var), we need to inject after ALOAD
                            // but before the method call
                            InsnList toInsert = new InsnList();
                            // Call DynamicResourcePack.insertPack(list) which modifies the list in-place
                            toInsert.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "com/mujmajnkraft/bstweaker/client/DynamicResourcePack",
                                    "insertPack",
                                    "(Ljava/util/List;)V",
                                    false));
                            // Reload the list variable (should be local var 1 like RL does)
                            toInsert.add(new VarInsnNode(Opcodes.ALOAD, 1));

                            refreshResources.instructions.insertBefore(min, toInsert);
                            LOGGER.info("[ASM] Successfully patched refreshResources!");
                            i += 2; // Skip past our inserted instructions
                        }
                    }
                }
            } else {
                LOGGER.warn("[ASM] Could not find refreshResources method!");
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();

        } catch (Exception e) {
            LOGGER.error("[ASM] Failed to patch Minecraft: " + e.getMessage());
            e.printStackTrace();
            return basicClass;
        }
    }
}
