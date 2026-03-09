/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 * WingSync
 */

package win.doughmination.wingsync.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import win.doughmination.api.events.PlayerBannedEvent;
import win.doughmination.wingsync.Main;


/**
 * General event listener for WingSync.
 *
 * Handles ban synchronisation from two sources:
 *  - DoughminationAPI's PlayerBannedEvent (fired by the /ban integration)
 *  - Vanilla Bukkit PlayerKickEvent (detects /ban via post-kick ban-list check)
 *
 * In both cases the flow is:
 *  1. Ban the player from the linked Discord server (while data still exists)
 *  2. Remove from the Minecraft whitelist
 *  3. Remove from WingSync storage
 */
public class GeneralListener implements Listener {

    private final Main plugin;

    public GeneralListener(Main plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // DoughminationAPI ban event
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onApiPlayerBanned(PlayerBannedEvent event) {
        String playerName = event.getPlayerName();
        plugin.getLogger().info("DoughminationAPI ban event received for " + playerName + " - syncing...");
        processBan(playerName, false);
    }

    // -------------------------------------------------------------------------
    // Vanilla /ban via PlayerKickEvent
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        String playerName = event.getPlayer().getName();

        // Wait 5 ticks (0.25 s) to ensure the ban entry is written to the ban list
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Guard against firing after plugin shutdown
            if (!plugin.isEnabled()) return;

            boolean isBanned = Bukkit.getBannedPlayers().stream()
                    .anyMatch(ban -> ban.getName() != null && ban.getName().equalsIgnoreCase(playerName));

            if (isBanned) {
                plugin.getLogger().info("Detected vanilla ban for " + playerName + " - syncing...");
                processBan(playerName, true);
            }
        }, 5L);
    }

    // -------------------------------------------------------------------------
    // Shared ban processing
    // -------------------------------------------------------------------------

    /**
     * Performs the full ban sync flow for a player.
     *
     * @param playerName      The Minecraft username of the banned player
     * @param runOnMainThread Whether whitelist removal must be dispatched on the main thread
     *                        (vanilla kick events fire on main; API events may not)
     */
    private void processBan(String playerName, boolean runOnMainThread) {
        // 1. Ban from Discord first (data must still exist for the lookup)
        try {
            plugin.banUserFromDiscord(playerName);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ban " + playerName + " from Discord: " + e.getMessage());
        }

        // 2. Remove from Minecraft whitelist
        Runnable whitelistRemove = () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + playerName);

        if (runOnMainThread) {
            Bukkit.getScheduler().runTask(plugin, whitelistRemove);
        } else {
            whitelistRemove.run();
        }

        // 3. Remove from WingSync storage
        try {
            plugin.getStorageUtil().removePlayerDataByName(playerName);
            plugin.getLogger().info("Removed " + playerName + " from WingSync storage.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove " + playerName + " from WingSync storage: " + e.getMessage());
        }
    }
}