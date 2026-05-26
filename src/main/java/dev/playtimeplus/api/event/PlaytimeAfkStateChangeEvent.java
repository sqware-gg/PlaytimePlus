package dev.playtimeplus.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlaytimeAfkStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID playerUuid;
    private final String playerName;
    private final boolean afk;
    private final boolean manual;
    private final String reason;
    private final long durationMillis;

    public PlaytimeAfkStateChangeEvent(Player player, boolean afk, boolean manual, String reason, long durationMillis) {
        this.player = player;
        this.playerUuid = player.getUniqueId();
        this.playerName = player.getName();
        this.afk = afk;
        this.manual = manual;
        this.reason = reason == null ? "" : reason;
        this.durationMillis = Math.max(0L, durationMillis);
    }

    public Player player() {
        return player;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String playerName() {
        return playerName;
    }

    public boolean afk() {
        return afk;
    }

    public boolean manual() {
        return manual;
    }

    public String reason() {
        return reason;
    }

    public long durationMillis() {
        return durationMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
