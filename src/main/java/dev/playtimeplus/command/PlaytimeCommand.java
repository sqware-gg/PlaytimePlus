package dev.playtimeplus.command;

import dev.playtimeplus.time.DurationFormatter;
import dev.playtimeplus.time.PlayerTimeRecord;
import dev.playtimeplus.time.PlayerTimeView;
import dev.playtimeplus.time.PlaytimeService;
import dev.playtimeplus.time.TimeMetric;
import dev.playtimeplus.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class PlaytimeCommand implements CommandExecutor, TabCompleter {
    private final PlaytimeService playtimeService;

    public PlaytimeCommand(PlaytimeService playtimeService) {
        this.playtimeService = playtimeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playtimeplus.playtime")) {
            message(sender, "no-permission", Map.of());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            handleTop(sender, args);
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                message(sender, "usage-playtime", Map.of());
                return true;
            }
            sendStats(sender, player.getUniqueId(), true);
            return true;
        }

        Optional<PlayerTimeRecord> target = playtimeService.findByName(args[0]);
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return true;
        }

        if (sender instanceof Player player && !player.getUniqueId().equals(target.get().uuid())
                && !sender.hasPermission("playtimeplus.playtime.others")) {
            message(sender, "no-permission", Map.of());
            return true;
        }

        sendStats(sender, target.get().uuid(), false);
        return true;
    }

    private void sendStats(CommandSender sender, java.util.UUID uuid, boolean self) {
        Optional<PlayerTimeView> view = playtimeService.snapshot(uuid);
        if (view.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }

        PlayerTimeView stats = view.get();
        message(sender, self ? "playtime-self" : "playtime-other", Map.of(
                "player", stats.name(),
                "total", DurationFormatter.compact(stats.totalMillis()),
                "active", DurationFormatter.compact(stats.activeMillis()),
                "afk", DurationFormatter.compact(stats.afkMillis()),
                "session", DurationFormatter.compact(stats.sessionMillis()),
                "last_seen", DurationFormatter.timestamp(stats.lastSeenMillis()),
                "joins", Long.toString(stats.joins()),
                "status", stats.afk() ? "AFK" : stats.online() ? "online" : "offline"
        ));
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("playtimeplus.top")) {
            message(sender, "no-permission", Map.of());
            return;
        }

        TimeMetric metric = TimeMetric.ACTIVE;
        int page = 1;
        if (args.length >= 2) {
            Optional<TimeMetric> parsed = TimeMetric.from(args[1]);
            if (parsed.isPresent()) {
                metric = parsed.get();
            } else if (isInteger(args[1])) {
                page = Math.max(1, Integer.parseInt(args[1]));
            } else {
                message(sender, "invalid-metric", Map.of());
                return;
            }
        }
        if (args.length >= 3 && isInteger(args[2])) {
            page = Math.max(1, Integer.parseInt(args[2]));
        }

        TimeMetric selectedMetric = metric;
        List<PlayerTimeView> ranking = playtimeService.top(selectedMetric).stream()
                .filter(view -> view.metricMillis(selectedMetric) > 0L)
                .toList();
        if (ranking.isEmpty()) {
            sender.sendMessage(Text.color(playtimeService.config().prefix() + "&7No playtime records yet."));
            return;
        }

        int pageSize = playtimeService.config().leaderboardPageSize();
        int pages = Math.max(1, (int) Math.ceil(ranking.size() / (double) pageSize));
        page = Math.min(page, pages);
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, ranking.size());

        message(sender, "top-header", Map.of(
                "metric", metric.key(),
                "page", Integer.toString(page),
                "pages", Integer.toString(pages)
        ));
        for (int index = start; index < end; index++) {
            PlayerTimeView view = ranking.get(index);
            message(sender, "top-line", Map.of(
                    "rank", Integer.toString(index + 1),
                    "player", view.name(),
                    "time", DurationFormatter.compact(view.metricMillis(metric))
            ));
        }
    }

    private void message(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(Text.color(playtimeService.config().prefix()
                + Text.render(playtimeService.config().message(key), placeholders)));
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("top");
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return filter(List.of("active", "total", "afk"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }
}
