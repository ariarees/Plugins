/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.wingsync.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import win.doughmination.doughutils.Main;
import win.doughmination.doughutils.data.BanData;

import java.util.Map;
import java.util.UUID;

public class GeneralListener implements Listener {

    private final Main plugin;

    public GeneralListener(Main plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Internal ban event — fired when /doughban is used
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onApiPlayerBanned(win.doughmination.doughutils.events.PlayerBannedEvent event) {
        plugin.getLogger().info("Ban event received for " + event.getPlayerName() + " - syncing to Discord...");
        processBan(event.getPlayerName(), false);
    }

    // -------------------------------------------------------------------------
    // Vanilla /ban via PlayerKickEvent
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        String playerName = event.getPlayer().getName();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.isEnabled()) return;
            boolean isBanned = Bukkit.getBannedPlayers().stream()
                    .anyMatch(ban -> ban.getName() != null && ban.getName().equalsIgnoreCase(playerName));
            if (isBanned) {
                plugin.getLogger().info("Detected vanilla ban for " + playerName + " - syncing to Discord...");
                processBan(playerName, true);
            }
        }, 5L);
    }

    // -------------------------------------------------------------------------
    // Shared ban processing
    // -------------------------------------------------------------------------

    private void processBan(String playerName, boolean runOnMainThread) {
        try { plugin.getWingSyncManager().banUserFromDiscord(playerName); }
        catch (Exception e) { plugin.getLogger().warning("Failed to ban " + playerName + " from Discord: " + e.getMessage()); }

        Runnable whitelistRemove = () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + playerName);
        if (runOnMainThread) Bukkit.getScheduler().runTask(plugin, whitelistRemove);
        else whitelistRemove.run();

        try { plugin.getWingSyncManager().getStorageUtil().removePlayerDataByName(playerName); }
        catch (Exception e) { plugin.getLogger().warning("Failed to remove " + playerName + " from WingSync storage: " + e.getMessage()); }
    }
}
