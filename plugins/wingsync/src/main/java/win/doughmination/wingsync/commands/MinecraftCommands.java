/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 * WingSync
 */

package win.doughmination.wingsync.commands;

// Bukkit
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import win.doughmination.wingsync.Main;
import win.doughmination.wingsync.listeners.StorageUtil;

public class MinecraftCommands {

    private final Main plugin;

    public MinecraftCommands(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("wsreload")) {
            plugin.reloadConfig();

            // Determine old and new storage types
            StorageUtil.StorageType currentType = plugin.getStorageUtil().getStorageType();
            String newTypeStr = plugin.getConfig().getString("storage.type", "json").toLowerCase();
            StorageUtil.StorageType newType = parseStorageType(newTypeStr);

            if (currentType != newType) {
                // Switching backends — shut down old, init new
                sender.sendMessage("\u00a7eSwitching storage backend to \u00a7b" + friendlyName(newType) + "\u00a7e...");
                plugin.setupDatabase(); // shutdown + setup from updated config
                sender.sendMessage("\u00a7aNow using \u00a7b" + friendlyName(newType) + "\u00a7a storage!");
            } else if (newType != StorageUtil.StorageType.FILE) {
                // Same DB backend — refresh connection
                sender.sendMessage("\u00a7eRefreshing database connection...");
                plugin.closeDatabaseConnection();
                plugin.setupDatabase();
                sender.sendMessage("\u00a7aDatabase connection refreshed!");
            }
            // File storage auto-loads on access; no refresh needed

            // Attempt to reconnect the Discord bot
            sender.sendMessage("\u00a7eReconnecting Discord bot...");
            boolean connected = plugin.connectDiscordBot();
            if (connected) {
                sender.sendMessage("\u00a7aWingSync reloaded successfully! Discord bot connected.");
            } else {
                sender.sendMessage("\u00a7cWingSync reloaded, but Discord bot failed to connect. Check console for details.");
            }

            return true;
        }
        return false;
    }

    private StorageUtil.StorageType parseStorageType(String type) {
        switch (type) {
            case "mysql":      return StorageUtil.StorageType.MYSQL;
            case "postgresql":
            case "postgres":   return StorageUtil.StorageType.POSTGRESQL;
            default:           return StorageUtil.StorageType.FILE;
        }
    }

    private String friendlyName(StorageUtil.StorageType type) {
        switch (type) {
            case MYSQL:      return "MySQL";
            case POSTGRESQL: return "PostgreSQL";
            default:         return "JSON (file)";
        }
    }
}