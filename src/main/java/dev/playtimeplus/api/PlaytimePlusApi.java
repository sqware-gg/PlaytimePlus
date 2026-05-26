package dev.playtimeplus.api;

import dev.playtimeplus.time.PlayerTimeView;
import dev.playtimeplus.time.PlaytimeService;
import dev.playtimeplus.time.TimeMetric;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class PlaytimePlusApi {
    private static PlaytimeService service;

    private PlaytimePlusApi() {
    }

    public static void register(PlaytimeService playtimeService) {
        service = playtimeService;
    }

    public static void unregister() {
        service = null;
    }

    public static Optional<PlayerTimeView> snapshot(UUID uuid) {
        return service == null ? Optional.empty() : service.snapshot(uuid);
    }

    public static Optional<PlayerTimeView> snapshot(Player player) {
        return player == null ? Optional.empty() : snapshot(player.getUniqueId());
    }

    public static Optional<PlayerTimeView> findByName(String playerName) {
        if (service == null) {
            return Optional.empty();
        }
        return service.findByName(playerName).flatMap(record -> service.snapshot(record.uuid()));
    }

    public static List<PlayerTimeView> top(TimeMetric metric, int limit) {
        if (service == null) {
            return List.of();
        }
        TimeMetric selectedMetric = metric == null ? TimeMetric.ACTIVE : metric;
        int cappedLimit = Math.max(1, limit);
        return service.top(selectedMetric).stream()
                .filter(view -> view.metricMillis(selectedMetric) > 0L)
                .limit(cappedLimit)
                .toList();
    }

    public static boolean isAfk(UUID uuid) {
        return service != null && service.isAfk(uuid);
    }

    public static int rank(UUID uuid, TimeMetric metric) {
        return service == null ? -1 : service.rank(uuid, metric);
    }
}
