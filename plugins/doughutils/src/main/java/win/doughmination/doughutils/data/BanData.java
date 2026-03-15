/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.data;

import java.util.UUID;

public class BanData {
    private final UUID playerUUID;
    private final String playerName;
    private final String reason;
    private final String bannedBy;
    private final UUID bannedByUUID;
    private final long bannedAt;

    public BanData(UUID playerUUID, String playerName, String reason, String bannedBy, UUID bannedByUUID, long bannedAt) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.reason = reason;
        this.bannedBy = bannedBy;
        this.bannedByUUID = bannedByUUID;
        this.bannedAt = bannedAt;
    }

    public UUID getPlayerUUID()  { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getReason()     { return reason; }
    public String getBannedBy()   { return bannedBy; }
    public UUID getBannedByUUID() { return bannedByUUID; }
    public long getBannedAt()     { return bannedAt; }
}
