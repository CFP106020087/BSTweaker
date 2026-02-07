package com.mujmajnkraft.bstweaker.client;

import com.mujmajnkraft.bstweaker.BSTweaker;

/**
 * Helper class to control hot-reload behavior.
 * Used to communicate between command and Mixin.
 */
public class HotReloadHelper {
    
    /** Flag to enable fast reload mode (skip full resource refresh). */
    private static boolean fastReloadMode = false;

    /** Enable fast reload mode. Called from /bstweaker reload. */
    public static void enableFastReload() {
        fastReloadMode = true;
        BSTweaker.LOG.info("[HotReloadHelper] Fast reload ENABLED");
    }

    /** Check and consume the fast reload flag. */
    public static boolean consumeFastReload() {
        if (fastReloadMode) {
            fastReloadMode = false;
            BSTweaker.LOG.info("[HotReloadHelper] Fast reload CONSUMED - triggering fast path");
            return true;
        }
        BSTweaker.LOG.info("[HotReloadHelper] Fast reload NOT enabled - using slow path");
        return false;
    }
}
