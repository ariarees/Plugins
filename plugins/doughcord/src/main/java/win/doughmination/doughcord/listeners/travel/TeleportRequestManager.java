/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.travel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import win.doughmination.doughcord.CordMain;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportRequestManager implements Listener {

    private final CordMain plugin;
    private final Map<UUID, TeleportRequest> teleportRequests = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public TeleportRequestManager(CordMain plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                teleportRequests.entrySet().removeIf(entry -> {
                    TeleportRequest req = entry.getValue();
                    if (req.isExpired()) return true;
                    Player requester = Bukkit.getPlayer(req.getRequesterUUID());
                    Player target = Bukkit.getPlayer(entry.getKey());
                    return requester == null || target == null;
                });
            }
        }.runTaskTimer(plugin, 20L, 20L * 10);
    }

    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        teleportRequests.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID quitter = event.getPlayer().getUniqueId();
        // Remove any request the quitter is the target of
        teleportRequests.remove(quitter);
        // Remove any request the quitter sent (they are the requester)
        teleportRequests.entrySet().removeIf(e -> e.getValue().getRequesterUUID().equals(quitter));
    }

    public void addRequest(UUID targetUUID, UUID requesterUUID) {
        teleportRequests.put(targetUUID, new TeleportRequest(requesterUUID));
    }

    public TeleportRequest getRequest(UUID targetUUID) {
        return teleportRequests.get(targetUUID);
    }

    public TeleportRequest removeRequest(UUID targetUUID) {
        return teleportRequests.remove(targetUUID);
    }

    public boolean hasRequest(UUID targetUUID) {
        return teleportRequests.containsKey(targetUUID);
    }

    // -----------------------------------------------------------------------
    // TeleportRequest inner class
    // -----------------------------------------------------------------------

    public static class TeleportRequest {
        private final UUID requesterUUID;
        private final long timestamp;

        private static final long EXPIRY_MS = 60_000; // 1 minute

        public TeleportRequest(UUID requesterUUID) {
            this.requesterUUID = requesterUUID;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getRequesterUUID() {
            return requesterUUID;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp >= EXPIRY_MS;
        }
    }
}
