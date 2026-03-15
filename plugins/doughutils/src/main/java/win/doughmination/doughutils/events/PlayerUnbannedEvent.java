/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlayerUnbannedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUUID;
    private final String playerName;
    private final String unbannedBy;

    public PlayerUnbannedEvent(UUID playerUUID, String playerName, String unbannedBy) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.unbannedBy = unbannedBy;
    }

    public UUID getPlayerUUID()   { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getUnbannedBy() { return unbannedBy; }

    @Override public HandlerList getHandlers()  { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
