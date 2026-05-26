package dev.playtimeplus.time;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerTimeRecord {
    private final UUID uuid;
    private String name;
    private long activeMillis;
    private long afkMillis;
    private long joins;
    private long firstSeenMillis;
    private long lastSeenMillis;
    private final Map<String, Long> rewardClaims = new HashMap<>();

    public PlayerTimeRecord(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public synchronized String name() {
        return name == null || name.isBlank() ? uuid.toString() : name;
    }

    public synchronized void name(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public synchronized void markJoin(String playerName, long nowMillis) {
        name(playerName);
        joins++;
        if (firstSeenMillis <= 0L) {
            firstSeenMillis = nowMillis;
        }
        lastSeenMillis = nowMillis;
    }

    public synchronized void markSeen(long nowMillis) {
        if (firstSeenMillis <= 0L) {
            firstSeenMillis = nowMillis;
        }
        lastSeenMillis = nowMillis;
    }

    public synchronized void addActive(long millis) {
        activeMillis = saturatedAdd(activeMillis, Math.max(0L, millis));
    }

    public synchronized void addAfk(long millis) {
        afkMillis = saturatedAdd(afkMillis, Math.max(0L, millis));
    }

    public synchronized void setMetric(TimeMetric metric, long millis) {
        long value = Math.max(0L, millis);
        switch (metric) {
            case ACTIVE -> activeMillis = value;
            case AFK -> afkMillis = value;
            case TOTAL -> {
                if (value >= afkMillis) {
                    activeMillis = value - afkMillis;
                } else {
                    activeMillis = 0L;
                    afkMillis = value;
                }
            }
        }
    }

    public synchronized void addMetric(TimeMetric metric, long millis) {
        long value = Math.max(0L, millis);
        switch (metric) {
            case ACTIVE, TOTAL -> addActive(value);
            case AFK -> addAfk(value);
        }
    }

    public synchronized void reset(String playerName, long nowMillis) {
        name(playerName);
        activeMillis = 0L;
        afkMillis = 0L;
        joins = 0L;
        firstSeenMillis = nowMillis;
        lastSeenMillis = nowMillis;
        rewardClaims.clear();
    }

    public synchronized PlayerTimeView snapshot(boolean online, boolean afk, long sessionMillis, String afkReason) {
        return new PlayerTimeView(
                uuid,
                name(),
                activeMillis,
                afkMillis,
                joins,
                firstSeenMillis,
                lastSeenMillis,
                online,
                afk,
                sessionMillis,
                afkReason == null ? "" : afkReason
        );
    }

    public synchronized long activeMillis() {
        return activeMillis;
    }

    public synchronized long afkMillis() {
        return afkMillis;
    }

    public synchronized long joins() {
        return joins;
    }

    public synchronized long firstSeenMillis() {
        return firstSeenMillis;
    }

    public synchronized long lastSeenMillis() {
        return lastSeenMillis;
    }

    public synchronized long rewardClaims(String rewardId) {
        return rewardClaims.getOrDefault(rewardId, 0L);
    }

    public synchronized void rewardClaims(String rewardId, long claims) {
        if (claims <= 0L) {
            rewardClaims.remove(rewardId);
            return;
        }
        rewardClaims.put(rewardId, claims);
    }

    public synchronized Map<String, Long> rewardClaims() {
        return Collections.unmodifiableMap(new HashMap<>(rewardClaims));
    }

    public synchronized void load(String playerName, long activeMillis, long afkMillis, long joins, long firstSeenMillis, long lastSeenMillis) {
        name(playerName);
        this.activeMillis = Math.max(0L, activeMillis);
        this.afkMillis = Math.max(0L, afkMillis);
        this.joins = Math.max(0L, joins);
        this.firstSeenMillis = Math.max(0L, firstSeenMillis);
        this.lastSeenMillis = Math.max(0L, lastSeenMillis);
    }

    public synchronized void loadRewardClaims(Map<String, Long> claims) {
        rewardClaims.clear();
        for (Map.Entry<String, Long> entry : claims.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null && entry.getValue() > 0L) {
                rewardClaims.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private long saturatedAdd(long left, long right) {
        long result = left + right;
        if (((left ^ result) & (right ^ result)) < 0L) {
            return Long.MAX_VALUE;
        }
        return result;
    }
}
