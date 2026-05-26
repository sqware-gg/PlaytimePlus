package dev.playtimeplus.time;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class TimeStore {
    private final JavaPlugin plugin;
    private final File file;
    private final ConcurrentMap<UUID, PlayerTimeRecord> records = new ConcurrentHashMap<>();

    public TimeStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        reload();
    }

    public void reload() {
        records.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = players.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                PlayerTimeRecord record = new PlayerTimeRecord(uuid);
                record.load(
                        section.getString("name", ""),
                        section.getLong("active-ms", 0L),
                        section.getLong("afk-ms", 0L),
                        section.getLong("joins", 0L),
                        section.getLong("first-seen", 0L),
                        section.getLong("last-seen", 0L)
                );
                record.loadRewardClaims(readRewardClaims(section));
                records.put(uuid, record);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().fine("Ignoring invalid UUID in players.yml: " + key);
            }
        }
    }

    public PlayerTimeRecord record(UUID uuid) {
        return records.computeIfAbsent(uuid, PlayerTimeRecord::new);
    }

    public Optional<PlayerTimeRecord> existing(UUID uuid) {
        return Optional.ofNullable(records.get(uuid));
    }

    public Optional<PlayerTimeRecord> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return records.values().stream()
                .filter(record -> record.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public Collection<PlayerTimeRecord> records() {
        return new ArrayList<>(records.values()).stream()
                .sorted(Comparator.comparing(PlayerTimeRecord::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public int size() {
        return records.size();
    }

    private Map<String, Long> readRewardClaims(ConfigurationSection playerSection) {
        Map<String, Long> claims = new HashMap<>();
        ConfigurationSection rewards = playerSection.getConfigurationSection("rewards");
        if (rewards == null) {
            return claims;
        }
        for (String key : rewards.getKeys(false)) {
            long count = rewards.getLong(key + ".claims", rewards.getLong(key, 0L));
            if (count > 0L) {
                claims.put(key, count);
            }
        }
        return claims;
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerTimeRecord record : records.values()) {
            String path = "players." + record.uuid();
            yaml.set(path + ".name", record.name());
            yaml.set(path + ".active-ms", record.activeMillis());
            yaml.set(path + ".afk-ms", record.afkMillis());
            yaml.set(path + ".joins", record.joins());
            yaml.set(path + ".first-seen", record.firstSeenMillis());
            yaml.set(path + ".last-seen", record.lastSeenMillis());
            for (Map.Entry<String, Long> reward : record.rewardClaims().entrySet()) {
                yaml.set(path + ".rewards." + reward.getKey() + ".claims", reward.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save players.yml: " + e.getMessage());
        }
    }
}
