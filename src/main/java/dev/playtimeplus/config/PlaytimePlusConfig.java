package dev.playtimeplus.config;

import dev.playtimeplus.reward.RewardConfigLoader;
import dev.playtimeplus.reward.RewardSettings;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaytimePlusConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private RewardSettings rewards;

    public PlaytimePlusConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        rewards = RewardConfigLoader.load(plugin, config);
    }

    public boolean afkEnabled() {
        return config.getBoolean("afk.enabled", true);
    }

    public long autoAfkMillis() {
        return seconds("afk.auto-threshold-seconds", 300) * 1000L;
    }

    public long warningMillis() {
        return seconds("afk.warning-seconds", 240) * 1000L;
    }

    public boolean clearAfkOnActivity() {
        return config.getBoolean("afk.clear-on-activity", true);
    }

    public boolean broadcastAfk() {
        return config.getBoolean("afk.broadcast", true);
    }

    public boolean ignoreSleep() {
        return config.getBoolean("afk.ignore-sleep", true);
    }

    public String autoAfkBypassPermission() {
        return config.getString("afk.auto-bypass-permission", "playtimeplus.afk.bypass");
    }

    public double movementDistanceThresholdSquared() {
        double threshold = config.getDouble("afk.movement.distance-threshold", 0.08D);
        return threshold * threshold;
    }

    public boolean countLookMovement() {
        return config.getBoolean("afk.movement.count-look", false);
    }

    public double lookThresholdDegrees() {
        return config.getDouble("afk.movement.look-threshold-degrees", 18.0D);
    }

    public boolean activityCounts(String key, boolean fallback) {
        return config.getBoolean("afk.activity." + key, fallback);
    }

    public boolean patternDetectionEnabled() {
        return config.getBoolean("afk.pattern-detection.enabled", true);
    }

    public long patternWindowMillis() {
        return seconds("afk.pattern-detection.window-seconds", 90) * 1000L;
    }

    public int patternMinEvents() {
        return Math.max(2, config.getInt("afk.pattern-detection.min-events", 14));
    }

    public int patternMaxUniqueSignatures() {
        return Math.max(1, config.getInt("afk.pattern-detection.max-unique-signatures", 3));
    }

    public Set<String> patternWatchedTypes() {
        Set<String> watched = new HashSet<>();
        for (String value : config.getStringList("afk.pattern-detection.watched-types")) {
            if (value != null && !value.isBlank()) {
                watched.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (watched.isEmpty()) {
            watched.add("movement");
            watched.add("interact");
            watched.add("commands");
            watched.add("inventory-click");
            watched.add("item-drop");
            watched.add("hotbar-change");
            watched.add("swap-hands");
            watched.add("teleport");
        }
        return watched;
    }

    public boolean kickEnabled() {
        return config.getBoolean("afk.kick.enabled", false);
    }

    public long kickAfterAfkMillis() {
        return seconds("afk.kick.after-seconds", 1800) * 1000L;
    }

    public String kickBypassPermission() {
        return config.getString("afk.kick.bypass-permission", "playtimeplus.kick.bypass");
    }

    public long checkIntervalTicks() {
        return Math.max(20L, seconds("tracking.check-interval-seconds", 10) * 20L);
    }

    public long saveIntervalTicks() {
        return Math.max(20L, seconds("tracking.save-interval-seconds", 300) * 20L);
    }

    public int leaderboardPageSize() {
        return Math.max(1, config.getInt("tracking.leaderboard-page-size", 10));
    }

    public boolean placeholdersEnabled() {
        return config.getBoolean("placeholders.enabled", true);
    }

    public RewardSettings rewards() {
        return rewards;
    }

    public String prefix() {
        return message("prefix");
    }

    public String message(String key) {
        return config.getString("messages." + key, "");
    }

    private long seconds(String path, long fallback) {
        return Math.max(0L, config.getLong(path, fallback));
    }
}
