package dev.playtimeplus.reward;

import dev.playtimeplus.time.DurationParser;
import dev.playtimeplus.time.TimeMetric;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RewardConfigLoader {
    private RewardConfigLoader() {
    }

    public static RewardSettings load(JavaPlugin plugin, FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("rewards");
        if (section == null || !section.getBoolean("enabled", false)) {
            return RewardSettings.disabled();
        }

        boolean announceConsole = section.getBoolean("announce-console", true);
        int defaultMaxClaims = Math.max(1, section.getInt("max-claims-per-check", 3));
        ConfigurationSection rulesSection = section.getConfigurationSection("rules");
        if (rulesSection == null) {
            return new RewardSettings(true, announceConsole, defaultMaxClaims, List.of());
        }

        List<RewardRule> rules = new ArrayList<>();
        for (String id : rulesSection.getKeys(false)) {
            ConfigurationSection ruleSection = rulesSection.getConfigurationSection(id);
            if (ruleSection == null || !ruleSection.getBoolean("enabled", true)) {
                continue;
            }
            readRule(plugin, id, ruleSection, defaultMaxClaims).ifPresent(rules::add);
        }
        return new RewardSettings(true, announceConsole, defaultMaxClaims, List.copyOf(rules));
    }

    private static Optional<RewardRule> readRule(JavaPlugin plugin, String id, ConfigurationSection section, int defaultMaxClaims) {
        RewardType type = RewardType.from(section.getString("type", "milestone")).orElse(null);
        if (type == null) {
            plugin.getLogger().warning("Ignoring reward '" + id + "': unknown type.");
            return Optional.empty();
        }

        TimeMetric metric = TimeMetric.from(section.getString("metric", "active")).orElse(TimeMetric.ACTIVE);
        String durationValue = type == RewardType.INTERVAL
                ? section.getString("every", section.getString("at", ""))
                : section.getString("at", section.getString("every", ""));
        OptionalLong threshold = DurationParser.parseMillis(durationValue);
        if (threshold.isEmpty() || threshold.getAsLong() <= 0L) {
            plugin.getLogger().warning("Ignoring reward '" + id + "': missing or invalid duration.");
            return Optional.empty();
        }

        List<String> commands = section.getStringList("commands");
        String message = section.getString("message", "");
        if (commands.isEmpty() && message.isBlank()) {
            plugin.getLogger().warning("Ignoring reward '" + id + "': no commands or message configured.");
            return Optional.empty();
        }

        return Optional.of(new RewardRule(
                id,
                type,
                metric,
                threshold.getAsLong(),
                section.getString("permission", ""),
                section.getBoolean("require-online", true),
                section.getBoolean("require-not-afk", true),
                Math.max(1, section.getInt("max-claims-per-check", defaultMaxClaims)),
                List.copyOf(commands),
                message
        ));
    }
}
