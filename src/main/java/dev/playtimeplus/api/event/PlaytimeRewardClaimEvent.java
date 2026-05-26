package dev.playtimeplus.api.event;

import dev.playtimeplus.time.TimeMetric;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlaytimeRewardClaimEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID playerUuid;
    private final String playerName;
    private final String rewardId;
    private final String rewardName;
    private final String rewardType;
    private final TimeMetric metric;
    private final long claimNumber;
    private final long thresholdMillis;
    private final long totalMillis;
    private final long activeMillis;
    private final long afkMillis;
    private final long sessionMillis;

    public PlaytimeRewardClaimEvent(Player player, UUID playerUuid, String playerName, String rewardId,
                                    String rewardName, String rewardType, TimeMetric metric, long claimNumber,
                                    long thresholdMillis, long totalMillis, long activeMillis, long afkMillis,
                                    long sessionMillis) {
        this.player = player;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.rewardId = rewardId;
        this.rewardName = rewardName;
        this.rewardType = rewardType;
        this.metric = metric;
        this.claimNumber = claimNumber;
        this.thresholdMillis = thresholdMillis;
        this.totalMillis = totalMillis;
        this.activeMillis = activeMillis;
        this.afkMillis = afkMillis;
        this.sessionMillis = sessionMillis;
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

    public String rewardId() {
        return rewardId;
    }

    public String rewardName() {
        return rewardName;
    }

    public String rewardType() {
        return rewardType;
    }

    public TimeMetric metric() {
        return metric;
    }

    public long claimNumber() {
        return claimNumber;
    }

    public long thresholdMillis() {
        return thresholdMillis;
    }

    public long totalMillis() {
        return totalMillis;
    }

    public long activeMillis() {
        return activeMillis;
    }

    public long afkMillis() {
        return afkMillis;
    }

    public long sessionMillis() {
        return sessionMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
