package dev.playtimeplus.time;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class PlayerSession {
    private record ActivitySample(String type, String signature, long millis) {
    }

    private final UUID uuid;
    private final Deque<ActivitySample> recentActivity = new ArrayDeque<>();
    private long joinMillis;
    private long lastTickMillis;
    private long lastActivityMillis;
    private boolean afk;
    private long afkSinceMillis;
    private boolean warned;
    private boolean manualAfk;
    private String afkReason = "";

    PlayerSession(UUID uuid, long nowMillis) {
        this.uuid = uuid;
        this.joinMillis = nowMillis;
        this.lastTickMillis = nowMillis;
        this.lastActivityMillis = nowMillis;
    }

    UUID uuid() {
        return uuid;
    }

    long joinMillis() {
        return joinMillis;
    }

    void resetJoin(long nowMillis) {
        joinMillis = nowMillis;
        lastTickMillis = nowMillis;
        lastActivityMillis = nowMillis;
        afk = false;
        afkSinceMillis = 0L;
        warned = false;
        manualAfk = false;
        afkReason = "";
        recentActivity.clear();
    }

    long lastTickMillis() {
        return lastTickMillis;
    }

    void lastTickMillis(long lastTickMillis) {
        this.lastTickMillis = lastTickMillis;
    }

    long lastActivityMillis() {
        return lastActivityMillis;
    }

    void lastActivityMillis(long lastActivityMillis) {
        this.lastActivityMillis = lastActivityMillis;
    }

    boolean afk() {
        return afk;
    }

    void afk(boolean afk) {
        this.afk = afk;
    }

    long afkSinceMillis() {
        return afkSinceMillis;
    }

    void afkSinceMillis(long afkSinceMillis) {
        this.afkSinceMillis = afkSinceMillis;
    }

    boolean warned() {
        return warned;
    }

    void warned(boolean warned) {
        this.warned = warned;
    }

    boolean manualAfk() {
        return manualAfk;
    }

    void manualAfk(boolean manualAfk) {
        this.manualAfk = manualAfk;
    }

    String afkReason() {
        return afkReason;
    }

    void afkReason(String afkReason) {
        this.afkReason = afkReason == null ? "" : afkReason;
    }

    boolean repetitiveActivity(String type, String signature, long nowMillis, long windowMillis,
                               int minEvents, int maxUniqueSignatures) {
        if (windowMillis <= 0L || minEvents <= 1 || maxUniqueSignatures <= 0) {
            return false;
        }
        String normalizedType = type == null || type.isBlank() ? "activity" : type;
        String normalizedSignature = signature == null || signature.isBlank() ? normalizedType : signature;
        recentActivity.addLast(new ActivitySample(normalizedType, normalizedSignature, nowMillis));
        long oldestAllowed = nowMillis - windowMillis;
        while (!recentActivity.isEmpty() && recentActivity.peekFirst().millis() < oldestAllowed) {
            recentActivity.removeFirst();
        }

        int watchedEvents = 0;
        Set<String> signatures = new HashSet<>();
        for (ActivitySample sample : recentActivity) {
            if (!sample.type().equals(normalizedType)) {
                continue;
            }
            watchedEvents++;
            signatures.add(sample.signature());
        }
        return watchedEvents >= minEvents && signatures.size() <= maxUniqueSignatures;
    }
}
