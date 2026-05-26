package dev.playtimeplus.command;

import dev.playtimeplus.PlaytimePlusPlugin;
import dev.playtimeplus.reward.RewardRule;
import dev.playtimeplus.time.DurationFormatter;
import dev.playtimeplus.time.DurationParser;
import dev.playtimeplus.time.PlayerTimeRecord;
import dev.playtimeplus.time.PlaytimeService;
import dev.playtimeplus.time.TimeMetric;
import dev.playtimeplus.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class PlaytimePlusCommand implements CommandExecutor, TabCompleter {
    private final PlaytimePlusPlugin plugin;
    private final PlaytimeService playtimeService;

    public PlaytimePlusCommand(PlaytimePlusPlugin plugin, PlaytimeService playtimeService) {
        this.plugin = plugin;
        this.playtimeService = playtimeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playtimeplus.admin")) {
            message(sender, "no-permission", Map.of());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            message(sender, "status", Map.of(
                    "online", Integer.toString(playtimeService.onlineCount()),
                    "records", Integer.toString(playtimeService.recordCount()),
                    "afk", Long.toString(playtimeService.afkCount()),
                    "rewards", Integer.toString(playtimeService.rewards().ruleCount())
            ));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadPlugin();
                message(sender, "reloaded", Map.of());
            }
            case "save" -> {
                playtimeService.save();
                message(sender, "saved", Map.of());
            }
            case "rewards" -> rewards(sender);
            case "reset" -> reset(sender, args);
            case "set" -> mutate(sender, args, true);
            case "add" -> mutate(sender, args, false);
            default -> message(sender, "usage-admin", Map.of());
        }
        return true;
    }

    private void reset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.color(playtimeService.config().prefix() + "&7Usage: &#2b98fd/playtimeplus reset <player>"));
            return;
        }
        Optional<PlayerTimeRecord> target = playtimeService.findByName(args[1]);
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        PlayerTimeRecord record = target.get();
        playtimeService.reset(record.uuid(), record.name());
        message(sender, "reset", Map.of("player", record.name()));
    }

    private void rewards(CommandSender sender) {
        List<RewardRule> rules = playtimeService.rewards().rules();
        if (rules.isEmpty()) {
            sender.sendMessage(Text.color(playtimeService.config().prefix() + "&7No reward rules are enabled."));
            return;
        }
        sender.sendMessage(Text.color(playtimeService.config().prefix() + "&7Reward rules: &#2b98fd" + rules.size()));
        for (RewardRule rule : rules) {
            sender.sendMessage(Text.color("&8- &f" + rule.id()
                    + " &7" + rule.type().name().toLowerCase(Locale.ROOT)
                    + " &#2b98fd" + rule.metric().key()
                    + " &f" + DurationFormatter.compact(rule.thresholdMillis())));
        }
    }

    private void mutate(CommandSender sender, String[] args, boolean set) {
        if (args.length < 4) {
            sender.sendMessage(Text.color(playtimeService.config().prefix()
                    + (set ? "&7Usage: &#2b98fd/playtimeplus set <player> <active|total|afk> <duration>"
                    : "&7Usage: &#2b98fd/playtimeplus add <player> <active|total|afk> <duration>")));
            return;
        }

        Optional<PlayerTimeRecord> target = playtimeService.findByName(args[1]);
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        Optional<TimeMetric> metric = TimeMetric.from(args[2]);
        if (metric.isEmpty()) {
            message(sender, "invalid-metric", Map.of());
            return;
        }
        OptionalLong duration = DurationParser.parseMillis(args[3]);
        if (duration.isEmpty()) {
            message(sender, "invalid-duration", Map.of());
            return;
        }

        PlayerTimeRecord record = target.get();
        if (set) {
            playtimeService.setMetric(record.uuid(), record.name(), metric.get(), duration.getAsLong());
            message(sender, "set", Map.of(
                    "player", record.name(),
                    "metric", metric.get().key(),
                    "time", DurationFormatter.compact(duration.getAsLong())
            ));
        } else {
            playtimeService.addMetric(record.uuid(), record.name(), metric.get(), duration.getAsLong());
            message(sender, "add", Map.of(
                    "player", record.name(),
                    "metric", metric.get().key(),
                    "time", DurationFormatter.compact(duration.getAsLong())
            ));
        }
    }

    private void message(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(Text.color(playtimeService.config().prefix()
                + Text.render(playtimeService.config().message(key), placeholders)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("playtimeplus.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("status", "reload", "save", "rewards", "reset", "set", "add"), args[0]);
        }
        if (args.length == 2 && List.of("reset", "set", "add").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[1]);
        }
        if (args.length == 3 && List.of("set", "add").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(List.of("active", "total", "afk"), args[2]);
        }
        if (args.length == 4 && List.of("set", "add").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(List.of("30m", "1h", "6h", "1d"), args[3]);
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
