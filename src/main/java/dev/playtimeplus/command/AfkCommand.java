package dev.playtimeplus.command;

import dev.playtimeplus.time.PlaytimeService;
import dev.playtimeplus.util.Text;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AfkCommand implements CommandExecutor, TabCompleter {
    private final PlaytimeService playtimeService;

    public AfkCommand(PlaytimeService playtimeService) {
        this.playtimeService = playtimeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playtimeplus.afk")) {
            message(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            message(sender, "players-only");
            return true;
        }
        playtimeService.toggleManualAfk(player, String.join(" ", args));
        return true;
    }

    private void message(CommandSender sender, String key) {
        sender.sendMessage(Text.color(playtimeService.config().prefix() + playtimeService.config().message(key)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
