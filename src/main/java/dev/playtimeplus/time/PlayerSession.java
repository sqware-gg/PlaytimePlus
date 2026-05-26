package dev.playtimeplus.time;

import java.util.UUID;

final class PlayerSession {
    private final UUID uuid;
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
}
