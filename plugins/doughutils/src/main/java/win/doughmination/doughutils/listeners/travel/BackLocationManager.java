/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.travel;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Session-only store for /back locations.
 * Data is never written to disk — entries are cleared when the player logs out.
 *
 * A back location is set:
 *   - Before any plugin-triggered teleport (tpa, spawn, rtp, base tp)
 *   - At the moment of player death (the location they died at)
 */
public class BackLocationManager {

    private final Map<UUID, Location> backLocations = new HashMap<>();

    /** Stores the player's current position as their /back destination. */
    public void set(UUID uuid, Location location) {
        backLocations.put(uuid, location.clone());
    }

    /** Returns the stored /back location, or null if none is set. */
    public Location get(UUID uuid) {
        return backLocations.get(uuid);
    }

    /** Returns true if a /back location exists for this player. */
    public boolean has(UUID uuid) {
        return backLocations.containsKey(uuid);
    }

    /** Clears the stored location — called on player quit. */
    public void clear(UUID uuid) {
        backLocations.remove(uuid);
    }
}
