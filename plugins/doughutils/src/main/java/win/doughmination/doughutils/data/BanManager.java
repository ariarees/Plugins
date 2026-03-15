/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import win.doughmination.doughutils.events.PlayerBannedEvent;
import win.doughmination.doughutils.events.PlayerUnbannedEvent;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the ban system — formerly part of LibMain.
 * Persists to plugins/DoughUtils/bans.json.
 */
public class BanManager {

    private final JavaPlugin plugin;
    private final Map<UUID, BanData> banDataMap = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File banDataFile;

    public BanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.banDataFile = new File(plugin.getDataFolder(), "bans.json");
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void load() {
        if (!banDataFile.exists()) return;
        try (FileReader reader = new FileReader(banDataFile)) {
            Type type = new TypeToken<Map<String, BanData>>(){}.getType();
            Map<String, BanData> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                for (Map.Entry<String, BanData> entry : loaded.entrySet()) {
                    banDataMap.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
            plugin.getLogger().info("Loaded " + banDataMap.size() + " ban records.");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load ban data: " + e.getMessage());
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(banDataFile)) {
            Map<String, BanData> saveData = new HashMap<>();
            for (Map.Entry<UUID, BanData> entry : banDataMap.entrySet()) {
                saveData.put(entry.getKey().toString(), entry.getValue());
            }
            gson.toJson(saveData, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save ban data: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------

    public void banPlayer(UUID playerUUID, String playerName, String reason, String bannedBy, UUID bannedByUUID) {
        BanData data = new BanData(playerUUID, playerName, reason, bannedBy, bannedByUUID, System.currentTimeMillis());
        banDataMap.put(playerUUID, data);
        save();

        Bukkit.getPluginManager().callEvent(
                new PlayerBannedEvent(playerUUID, playerName, reason, bannedBy, bannedByUUID));

        Player online = Bukkit.getPlayer(playerUUID);
        if (online != null && online.isOnline()) {
            online.kick(Component.text("You have been banned for: " + reason, NamedTextColor.RED));
        }
        plugin.getLogger().info(playerName + " has been banned by " + bannedBy + ". Reason: " + reason);
    }

    public void unbanPlayer(UUID playerUUID, String unbannedBy) {
        BanData data = banDataMap.remove(playerUUID);
        if (data != null) {
            save();
            Bukkit.getPluginManager().callEvent(
                    new PlayerUnbannedEvent(playerUUID, data.getPlayerName(), unbannedBy));
            plugin.getLogger().info(data.getPlayerName() + " has been unbanned by " + unbannedBy);
        }
    }

    public boolean isPlayerBanned(UUID playerUUID) {
        return banDataMap.containsKey(playerUUID);
    }

    public BanData getBanData(UUID playerUUID) {
        return banDataMap.get(playerUUID);
    }

    public Map<UUID, BanData> getAllBans() {
        return new HashMap<>(banDataMap);
    }
}
