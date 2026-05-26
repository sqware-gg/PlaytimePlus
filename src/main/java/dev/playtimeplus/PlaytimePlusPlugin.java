package dev.playtimeplus;

import dev.playtimeplus.api.PlaytimePlusApi;
import dev.playtimeplus.command.AfkCommand;
import dev.playtimeplus.command.PlaytimeCommand;
import dev.playtimeplus.command.PlaytimePlusCommand;
import dev.playtimeplus.config.ConfigReferenceWriter;
import dev.playtimeplus.config.PlaytimePlusConfig;
import dev.playtimeplus.listener.ActivityListener;
import dev.playtimeplus.reward.RewardService;
import dev.playtimeplus.time.PlaytimeService;
import dev.playtimeplus.time.TimeStore;
import java.lang.reflect.InvocationTargetException;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaytimePlusPlugin extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 31601;

    private PlaytimePlusConfig playtimeConfig;
    private TimeStore timeStore;
    private PlaytimeService playtimeService;
    private RewardService rewardService;
    private Object placeholderExpansion;

    @Override
    public void onEnable() {
        new Metrics(this, BSTATS_PLUGIN_ID);
        ConfigReferenceWriter.saveDefaultAndReferenceIfNeeded(this);

        playtimeConfig = new PlaytimePlusConfig(this);
        timeStore = new TimeStore(this);
        rewardService = new RewardService(this, playtimeConfig);
        playtimeService = new PlaytimeService(this, playtimeConfig, timeStore, rewardService);
        PlaytimePlusApi.register(playtimeService);

        getServer().getPluginManager().registerEvents(new ActivityListener(this, playtimeService), this);
        registerCommands();

        playtimeService.start();
        registerPlaceholderApiExpansion();
    }

    @Override
    public void onDisable() {
        unregisterPlaceholderApiExpansion();
        PlaytimePlusApi.unregister();
        if (playtimeService != null) {
            playtimeService.stop();
        }
    }

    public void reloadPlugin() {
        playtimeConfig.reload();
        playtimeService.reload(playtimeConfig);
        unregisterPlaceholderApiExpansion();
        registerPlaceholderApiExpansion();
    }

    private void registerCommands() {
        PlaytimeCommand playtimeCommand = new PlaytimeCommand(playtimeService);
        PluginCommand playtime = getCommand("playtime");
        if (playtime != null) {
            playtime.setExecutor(playtimeCommand);
            playtime.setTabCompleter(playtimeCommand);
        }

        AfkCommand afkCommand = new AfkCommand(playtimeService);
        PluginCommand afk = getCommand("afk");
        if (afk != null) {
            afk.setExecutor(afkCommand);
            afk.setTabCompleter(afkCommand);
        }

        PlaytimePlusCommand adminCommand = new PlaytimePlusCommand(this, playtimeService);
        PluginCommand admin = getCommand("playtimeplus");
        if (admin != null) {
            admin.setExecutor(adminCommand);
            admin.setTabCompleter(adminCommand);
        }
    }

    private void registerPlaceholderApiExpansion() {
        if (!playtimeConfig.placeholdersEnabled() || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            Class<?> expansionClass = Class.forName("dev.playtimeplus.hook.PlaceholderApiExpansion");
            Object expansion = expansionClass
                    .getConstructor(JavaPlugin.class, PlaytimeService.class)
                    .newInstance(this, playtimeService);
            expansionClass.getMethod("register").invoke(expansion);
            placeholderExpansion = expansion;
            getLogger().info("Registered PlaceholderAPI placeholders.");
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException e) {
            getLogger().warning("Could not register PlaceholderAPI placeholders: " + e.getMessage());
        }
    }

    private void unregisterPlaceholderApiExpansion() {
        if (placeholderExpansion == null) {
            return;
        }
        try {
            placeholderExpansion.getClass().getMethod("unregister").invoke(placeholderExpansion);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            getLogger().warning("Could not unregister PlaceholderAPI placeholders: " + e.getMessage());
        } finally {
            placeholderExpansion = null;
        }
    }
}
