/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired when a player is banned via /doughban.
 * WingSyncManager listens to this to sync bans to Discord.
 */
public class PlayerBannedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUUID;
    private final String playerName;
    private final String reason;
    private final String bannedBy;
    private final UUID bannedByUUID;

    public PlayerBannedEvent(UUID playerUUID, String playerName, String reason, String bannedBy, UUID bannedByUUID) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.reason = reason;
        this.bannedBy = bannedBy;
        this.bannedByUUID = bannedByUUID;
    }

    public UUID getPlayerUUID()   { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getReason()     { return reason; }
    public String getBannedBy()   { return bannedBy; }
    public UUID getBannedByUUID() { return bannedByUUID; }

    @Override public HandlerList getHandlers()            { return HANDLERS; }
    public static HandlerList getHandlerList()            { return HANDLERS; }
}
