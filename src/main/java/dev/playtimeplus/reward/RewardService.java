package dev.playtimeplus.reward;

import dev.playtimeplus.api.event.PlaytimeRewardClaimEvent;
import dev.playtimeplus.config.PlaytimePlusConfig;
import dev.playtimeplus.time.DurationFormatter;
import dev.playtimeplus.time.PlayerTimeRecord;
import dev.playtimeplus.time.PlayerTimeView;
import dev.playtimeplus.time.TimeMetric;
import dev.playtimeplus.util.Text;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RewardService {
    private final JavaPlugin plugin;
    private PlaytimePlusConfig config;

    public RewardService(JavaPlugin plugin, PlaytimePlusConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void reload(PlaytimePlusConfig config) {
        this.config = config;
    }

    public int ruleCount() {
        return config.rewards().rules().size();
    }

    public List<RewardRule> rules() {
        return config.rewards().rules();
    }

    public void check(Player player, PlayerTimeRecord record, PlayerTimeView view) {
        RewardSettings settings = config.rewards();
        if (!settings.enabled() || settings.rules().isEmpty()) {
            return;
        }

        for (RewardRule rule : settings.rules()) {
            if (!eligible(player, rule, view)) {
                continue;
            }
            long earnedClaims = earnedClaims(rule, view.metricMillis(rule.metric()));
            long claimed = record.rewardClaims(rule.id());
            long claimable = Math.max(0L, earnedClaims - claimed);
            if (claimable <= 0L) {
                continue;
            }

            long claimsThisCheck = Math.min(claimable, rule.maxClaimsPerCheck());
            for (long offset = 1L; offset <= claimsThisCheck; offset++) {
                long claimNumber = claimed + offset;
                execute(player, view, rule, claimNumber);
            }
            record.rewardClaims(rule.id(), claimed + claimsThisCheck);
        }
    }

    public Optional<RewardProgress> nextProgress(PlayerTimeView view) {
        if (!config.rewards().enabled()) {
            return Optional.empty();
        }
        return config.rewards().rules().stream()
                .map(rule -> progress(rule, view))
                .filter(progress -> !progress.available())
                .min(Comparator.comparingLong(RewardProgress::remainingMillis));
    }

    public long totalClaimed(PlayerTimeRecord record) {
        return record.rewardClaims().values().stream().mapToLong(Long::longValue).sum();
    }

    private RewardProgress progress(RewardRule rule, PlayerTimeView view) {
        long value = view.metricMillis(rule.metric());
        long remaining = switch (rule.type()) {
            case INTERVAL -> {
                long remainder = value % rule.thresholdMillis();
                yield remainder == 0L && value > 0L ? 0L : rule.thresholdMillis() - remainder;
            }
            case MILESTONE -> Math.max(0L, rule.thresholdMillis() - value);
        };
        return new RewardProgress(rule.id(), rule.thresholdMillis(), remaining, remaining <= 0L);
    }

    private boolean eligible(Player player, RewardRule rule, PlayerTimeView view) {
        if (rule.requireOnline() && player == null) {
            return false;
        }
        if (rule.requireNotAfk() && view.afk()) {
            return false;
        }
        return rule.permission() == null || rule.permission().isBlank() || player == null || player.hasPermission(rule.permission());
    }

    private long earnedClaims(RewardRule rule, long metricMillis) {
        if (metricMillis < rule.thresholdMillis()) {
            return 0L;
        }
        return switch (rule.type()) {
            case INTERVAL -> metricMillis / rule.thresholdMillis();
            case MILESTONE -> 1L;
        };
    }

    private void execute(Player player, PlayerTimeView view, RewardRule rule, long claimNumber) {
        Map<String, String> placeholders = placeholders(player, view, rule, claimNumber);
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        for (String command : rule.commands()) {
            String rendered = Text.render(command, placeholders).trim();
            if (rendered.startsWith("/")) {
                rendered = rendered.substring(1);
            }
            if (!rendered.isBlank()) {
                Bukkit.dispatchCommand(console, rendered);
            }
        }

        if (!rule.message().isBlank() && player != null && player.isOnline()) {
            player.sendMessage(Text.color(config.prefix() + Text.render(rule.message(), placeholders)));
        }

        if (config.rewards().announceConsole()) {
            plugin.getLogger().info("Reward " + rule.id() + " claimed by " + view.name() + " (#" + claimNumber + ").");
        }

        Bukkit.getPluginManager().callEvent(new PlaytimeRewardClaimEvent(
                player,
                view.uuid(),
                player == null ? view.name() : player.getName(),
                rule.id(),
                rule.displayName(),
                rule.type().name().toLowerCase(),
                rule.metric(),
                claimNumber,
                rule.thresholdMillis(),
                view.totalMillis(),
                view.activeMillis(),
                view.afkMillis(),
                view.sessionMillis()
        ));
    }

    private Map<String, String> placeholders(Player player, PlayerTimeView view, RewardRule rule, long claimNumber) {
        Map<String, String> values = new HashMap<>();
        values.put("player", player == null ? view.name() : player.getName());
        values.put("uuid", view.uuid().toString());
        values.put("reward", rule.id());
        values.put("reward_name", rule.displayName());
        values.put("type", rule.type().name().toLowerCase());
        values.put("metric", rule.metric().key());
        values.put("claim", Long.toString(claimNumber));
        values.put("threshold", DurationFormatter.compact(rule.thresholdMillis()));
        values.put("threshold_short", DurationFormatter.compactShort(rule.thresholdMillis()));
        values.put("total", DurationFormatter.compact(view.totalMillis()));
        values.put("active", DurationFormatter.compact(view.activeMillis()));
        values.put("afk", DurationFormatter.compact(view.afkMillis()));
        values.put("session", DurationFormatter.compact(view.sessionMillis()));
        values.put("total_short", DurationFormatter.compactShort(view.totalMillis()));
        values.put("active_short", DurationFormatter.compactShort(view.activeMillis()));
        values.put("afk_short", DurationFormatter.compactShort(view.afkMillis()));
        values.put("session_short", DurationFormatter.compactShort(view.sessionMillis()));
        values.put("total_seconds", Long.toString(view.totalMillis() / 1000L));
        values.put("active_seconds", Long.toString(view.activeMillis() / 1000L));
        values.put("afk_seconds", Long.toString(view.afkMillis() / 1000L));
        values.put("session_seconds", Long.toString(view.sessionMillis() / 1000L));
        return values;
    }
}
