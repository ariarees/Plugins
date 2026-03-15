/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.wingsync.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import win.doughmination.doughutils.Main;
import win.doughmination.doughutils.wingsync.listeners.StorageUtil;

public class WsReloadCommand implements CommandExecutor {

    private final Main plugin;

    public WsReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wingsync.reload")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        plugin.reloadConfig();

        StorageUtil.StorageType currentType = plugin.getWingSyncManager().getStorageUtil().getStorageType();
        String newTypeStr = plugin.getConfig().getString("wingsync.storage.type", "json").toLowerCase();
        StorageUtil.StorageType newType = parseStorageType(newTypeStr);

        if (currentType != newType) {
            sender.sendMessage("§eSwitching WingSync storage to §b" + friendlyName(newType) + "§e...");
            plugin.getWingSyncManager().getStorageUtil().shutdown();
            plugin.getWingSyncManager().getStorageUtil().setup();
            sender.sendMessage("§aNow using §b" + friendlyName(newType) + "§a storage!");
        } else if (newType != StorageUtil.StorageType.FILE) {
            sender.sendMessage("§eRefreshing WingSync database connection...");
            plugin.getWingSyncManager().getStorageUtil().shutdown();
            plugin.getWingSyncManager().getStorageUtil().setup();
            sender.sendMessage("§aDatabase connection refreshed!");
        }

        sender.sendMessage("§eReconnecting Discord bot...");
        boolean connected = plugin.getWingSyncManager().connectDiscordBot();
        if (connected) sender.sendMessage("§aWingSync reloaded! Discord bot connected.");
        else sender.sendMessage("§cWingSync reloaded, but Discord bot failed to connect. Check console.");

        return true;
    }

    private StorageUtil.StorageType parseStorageType(String type) {
        return switch (type) {
            case "mysql"                   -> StorageUtil.StorageType.MYSQL;
            case "postgresql", "postgres"  -> StorageUtil.StorageType.POSTGRESQL;
            default                        -> StorageUtil.StorageType.FILE;
        };
    }

    private String friendlyName(StorageUtil.StorageType type) {
        return switch (type) {
            case MYSQL      -> "MySQL";
            case POSTGRESQL -> "PostgreSQL";
            default         -> "JSON (file)";
        };
    }
}
