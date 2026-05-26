package dev.playtimeplus.time;

import java.util.UUID;

public record PlayerTimeView(
        UUID uuid,
        String name,
        long activeMillis,
        long afkMillis,
        long joins,
        long firstSeenMillis,
        long lastSeenMillis,
        boolean online,
        boolean afk,
        long sessionMillis,
        String afkReason
) {
    public long totalMillis() {
        return activeMillis + afkMillis;
    }

    public long metricMillis(TimeMetric metric) {
        return switch (metric) {
            case ACTIVE -> activeMillis;
            case TOTAL -> totalMillis();
            case AFK -> afkMillis;
        };
    }
}
