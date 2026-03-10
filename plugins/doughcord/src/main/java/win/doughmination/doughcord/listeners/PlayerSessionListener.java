/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners;

import win.doughmination.doughcord.CordMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listens for player session events and updates persistent data accordingly.
 * Handles:
 *   - On join: initialise settings file (first-timers), load all settings into memory
 *   - On death: record death location for /back
 *   - On quit: flush all settings to disk, clear session-only data
 */
public class PlayerSessionListener implements Listener {

    private final CordMain plugin;

    public PlayerSessionListener(CordMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Create settings file with defaults if this is their first time
        plugin.getPlayerDataManager().initPlayerFile(uuid);

        // Load veinminer toggles from file into memory
        plugin.getVeinMinerExecutor().loadForPlayer(uuid);

        // Load playtime into the in-memory map if not already present (handles restarts)
        plugin.getLoginTimestamps().put(uuid, System.currentTimeMillis());
        if (!plugin.getPlaytimeMap().containsKey(uuid)) {
            long saved = plugin.getPlayerDataManager().loadPlaytime(uuid);
            if (saved > 0) plugin.getPlaytimeMap().put(uuid, saved);
        }

        // Flight toggle is managed by BaseFlightMain — already loaded on startup from settings files,
        // but if this is a fresh file we ensure the default (false) is reflected
        if (!plugin.getBaseFlightMain().getFlightToggles().containsKey(uuid)) {
            boolean flight = plugin.getPlayerDataManager().loadFlightToggle(uuid);
            if (flight) plugin.getBaseFlightMain().getFlightToggles().put(uuid, true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Store where the player died so /back can return them there
        plugin.getBackLocationManager().set(
                event.getEntity().getUniqueId(),
                event.getEntity().getLocation()
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Flush playtime
        long sessionStart = plugin.getLoginTimestamps().remove(uuid);
        if (sessionStart > 0) {
            plugin.getPlaytimeMap().merge(uuid, System.currentTimeMillis() - sessionStart, Long::sum);
        }
        long total = plugin.getPlaytimeMap().getOrDefault(uuid, 0L);
        plugin.getPlayerDataManager().savePlaytime(uuid, total);

        // Flush veinminer toggles and unload from memory
        plugin.getVeinMinerExecutor().saveAndUnloadForPlayer(uuid);

        // Save last seen timestamp
        plugin.getPlayerDataManager().saveLastSeen(uuid, System.currentTimeMillis());

        // /back data is session-only — clear on logout
        plugin.getBackLocationManager().clear(uuid);
    }
}
