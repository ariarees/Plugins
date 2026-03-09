package win.dougmination.plural.listeners;

import org.bukkit.Bukkit;
import win.dougmination.plural.PluralMain;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerConnectionListener implements Listener {

    private static final long POLL_INTERVAL_TICKS = 20L * 30;

    // Store tasks so we can't cancel them lol
    private final Map<UUID, BukkitTask> pollingTasks = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (PluralMain.getApiClient() == null) return;

        fetchAndCache(player, uuid, true);

        // Capture only UUID - re-lookup Player inside the task to avoid retaining
        // a stale Player reference if the object is replaced between ticks
        BukkitTask task = PluralMain.getInstance().getServer()
                .getScheduler()
                .runTaskTimerAsynchronously(
                        PluralMain.getInstance(),
                        () -> {
                            if (!PluralMain.getInstance().isEnabled()) return;
                            Player online = PluralMain.getInstance().getServer().getPlayer(uuid);
                            if (online == null || !online.isOnline()) return;
                            fetchAndCache(online, uuid, false);
                        },
                        POLL_INTERVAL_TICKS,
                        POLL_INTERVAL_TICKS
                );
        pollingTasks.put(uuid, task);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Remove cached data
        PluralMain.systemCache.remove(uuid);

        // Cancel polling task to prevent memory or thread leaks
        BukkitTask task = pollingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    // Call this on onDisable() for safety
    public void shutdown() {
        pollingTasks.values().forEach(BukkitTask::cancel);
        pollingTasks.clear();
    }

    public void fetchAndCache(Player player, UUID uuid, boolean announceOnJoin) {
        PluralMain.getInstance().getServer().getScheduler()
                .runTaskAsynchronously(PluralMain.getInstance(), () -> {

                    PluralMain.PlayerSystemData data =
                            PluralMain.getApiClient().fetchPlayerData(uuid);

                    PluralMain.getInstance().getServer().getScheduler()
                            .runTask(PluralMain.getInstance(), () -> {

                                if (data != null) {
                                    PluralMain.PlayerSystemData prev =
                                            PluralMain.systemCache.get(uuid);

                                    PluralMain.systemCache.put(uuid, data);

                                    if (announceOnJoin && !data.activeFrontNames.isEmpty()) {
                                        player.sendMessage(ChatColor.GRAY + "[Plural] Fronting as: "
                                                + ChatColor.WHITE +
                                                String.join( " & ", data.activeFrontNames));
                                    }

                                    if (!announceOnJoin
                                            && prev != null
                                            && !prev.activeFrontNames.equals(data.activeFrontNames)
                                            && !data.activeFrontNames.isEmpty()) {
                                        player.sendMessage(ChatColor.GRAY + "[Plural] Front updated: "
                                                + ChatColor.WHITE +
                                                String.join(" & ", data.activeFrontNames));
                                    }
                                } else {
                                    PluralMain.systemCache.remove(uuid);
                                }
                            });
                });
    }

}