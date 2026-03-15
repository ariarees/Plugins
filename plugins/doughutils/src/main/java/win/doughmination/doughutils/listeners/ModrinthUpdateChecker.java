/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public final class ModrinthUpdateChecker implements Listener {

    private static final String API_URL = "https://api.modrinth.com/v2/project/%s/version";
    private static final String ALERT_PERMISSION = "dough.updatealerts";

    private final JavaPlugin plugin;
    private final String projectId;
    private final String projectSlug;
    private final String currentVersion;

    private volatile boolean updateAvailable = false;
    private volatile String latestVersion;

    private ModrinthUpdateChecker(JavaPlugin plugin, String projectId, String projectSlug, String currentVersion) {
        this.plugin = plugin;
        this.projectId = projectId;
        this.projectSlug = projectSlug;
        this.currentVersion = currentVersion;
    }

    /**
     * Initialises the update checker, registers the join listener, and fires the async check.
     *
     * @param plugin         The plugin instance
     * @param projectId      The Modrinth project ID (e.g. "rBAEI1nf")
     * @param projectSlug    The Modrinth slug used in the URL (e.g. "doughapi")
     * @param currentVersion The running version, read from config.yml
     */
    public static void check(JavaPlugin plugin, String projectId, String projectSlug, String currentVersion) {
        ModrinthUpdateChecker checker = new ModrinthUpdateChecker(plugin, projectId, projectSlug, currentVersion);
        plugin.getServer().getPluginManager().registerEvents(checker, plugin);
        checker.fetchLatestVersion();
    }

    private void fetchLatestVersion() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(String.format(API_URL, projectId));

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", plugin.getName() + "/" + currentVersion + " (update-checker)");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    plugin.getLogger().warning("[UpdateChecker] Modrinth returned HTTP "
                            + conn.getResponseCode() + " for project: " + projectId);
                    return;
                }

                JsonArray versions = JsonParser.parseReader(
                        new InputStreamReader(conn.getInputStream())
                ).getAsJsonArray();

                if (versions.isEmpty()) {
                    plugin.getLogger().warning("[UpdateChecker] No versions found on Modrinth for: " + projectId);
                    return;
                }

                latestVersion = versions.get(0)
                        .getAsJsonObject()
                        .get("version_number")
                        .getAsString();

                if (!currentVersion.equals(latestVersion)) {
                    updateAvailable = true;
                    plugin.getLogger().warning("[UpdateChecker] " + plugin.getName()
                            + " is out of date! Running " + currentVersion
                            + ", latest is " + latestVersion
                            + ". Update at: https://modrinth.com/plugin/" + projectSlug);
                } else {
                    plugin.getLogger().info("[UpdateChecker] " + plugin.getName()
                            + " is up to date (" + currentVersion + ").");
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[UpdateChecker] Failed to check for updates: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateAvailable) return;

        Player player = event.getPlayer();
        if (!player.isOp() && !player.hasPermission(ALERT_PERMISSION)) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            player.sendMessage(Component.text("[" + plugin.getName() + "] ", NamedTextColor.GOLD)
                    .append(Component.text("An update is available! ", NamedTextColor.YELLOW))
                    .append(Component.text(currentVersion, NamedTextColor.RED))
                    .append(Component.text(" → ", NamedTextColor.GRAY))
                    .append(Component.text(latestVersion, NamedTextColor.GREEN)));

            player.sendMessage(Component.text("  Download: ", NamedTextColor.GRAY)
                    .append(Component.text("https://modrinth.com/plugin/" + projectSlug, NamedTextColor.AQUA)));
        }, 40L);
    }
}