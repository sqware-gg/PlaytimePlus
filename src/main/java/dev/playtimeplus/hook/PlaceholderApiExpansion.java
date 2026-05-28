package dev.playtimeplus.hook;

import dev.playtimeplus.time.DurationFormatter;
import dev.playtimeplus.time.PlayerTimeView;
import dev.playtimeplus.time.PlaytimeService;
import dev.playtimeplus.time.TimeMetric;
import java.util.Locale;
import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaceholderApiExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final PlaytimeService playtimeService;

    public PlaceholderApiExpansion(JavaPlugin plugin, PlaytimeService playtimeService) {
        this.plugin = plugin;
        this.playtimeService = playtimeService;
    }

    @Override
    public String getAuthor() {
        return "SQWARE / Conflict";
    }

    @Override
    public String getIdentifier() {
        return "playtimeplus";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);
        Optional<PlayerTimeView> snapshot = playtimeService.snapshot(player.getUniqueId());
        PlayerTimeView view = snapshot.orElse(null);

        return switch (key) {
            case "total" -> format(view == null ? 0L : view.totalMillis());
            case "active" -> format(view == null ? 0L : view.activeMillis());
            case "afk" -> format(view == null ? 0L : view.afkMillis());
            case "session" -> format(view == null ? 0L : view.sessionMillis());
            case "total_short" -> formatShort(view == null ? 0L : view.totalMillis());
            case "active_short" -> formatShort(view == null ? 0L : view.activeMillis());
            case "afk_short" -> formatShort(view == null ? 0L : view.afkMillis());
            case "session_short" -> formatShort(view == null ? 0L : view.sessionMillis());
            case "total_seconds" -> seconds(view == null ? 0L : view.totalMillis());
            case "active_seconds" -> seconds(view == null ? 0L : view.activeMillis());
            case "afk_seconds" -> seconds(view == null ? 0L : view.afkMillis());
            case "session_seconds" -> seconds(view == null ? 0L : view.sessionMillis());
            case "is_afk" -> Boolean.toString(view != null && view.afk());
            case "afk_reason" -> view == null ? "" : view.afkReason();
            case "joins" -> Long.toString(view == null ? 0L : view.joins());
            case "last_seen" -> view == null ? "never" : DurationFormatter.timestamp(view.lastSeenMillis());
            case "rank_active" -> rank(player, TimeMetric.ACTIVE);
            case "rank_total" -> rank(player, TimeMetric.TOTAL);
            case "rank_afk" -> rank(player, TimeMetric.AFK);
            case "rewards_claimed" -> rewardsClaimed(view);
            case "next_reward" -> nextReward(view);
            case "next_reward_time" -> nextRewardTime(view);
            case "next_reward_time_short" -> nextRewardTimeShort(view);
            case "next_reward_seconds" -> nextRewardSeconds(view);
            default -> null;
        };
    }

    private String format(long millis) {
        return DurationFormatter.compact(millis);
    }

    private String formatShort(long millis) {
        return DurationFormatter.compactShort(millis);
    }

    private String seconds(long millis) {
        return Long.toString(Math.max(0L, millis / 1000L));
    }

    private String rank(OfflinePlayer player, TimeMetric metric) {
        int rank = playtimeService.rank(player.getUniqueId(), metric);
        return rank <= 0 ? "" : Integer.toString(rank);
    }

    private String rewardsClaimed(PlayerTimeView view) {
        if (view == null) {
            return "0";
        }
        return Long.toString(playtimeService.rewardClaims(view.uuid()));
    }

    private String nextReward(PlayerTimeView view) {
        if (view == null) {
            return "";
        }
        return playtimeService.rewards().nextProgress(view)
                .map(progress -> progress.ruleId())
                .orElse("");
    }

    private String nextRewardTime(PlayerTimeView view) {
        if (view == null) {
            return "";
        }
        return playtimeService.rewards().nextProgress(view)
                .map(progress -> DurationFormatter.compact(progress.remainingMillis()))
                .orElse("");
    }

    private String nextRewardTimeShort(PlayerTimeView view) {
        if (view == null) {
            return "";
        }
        return playtimeService.rewards().nextProgress(view)
                .map(progress -> DurationFormatter.compactShort(progress.remainingMillis()))
                .orElse("");
    }

    private String nextRewardSeconds(PlayerTimeView view) {
        if (view == null) {
            return "";
        }
        return playtimeService.rewards().nextProgress(view)
                .map(progress -> Long.toString(progress.remainingMillis() / 1000L))
                .orElse("");
    }
}
