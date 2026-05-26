package dev.playtimeplus.time;

import dev.playtimeplus.api.event.PlaytimeAfkStateChangeEvent;
import dev.playtimeplus.config.PlaytimePlusConfig;
import dev.playtimeplus.reward.RewardService;
import dev.playtimeplus.util.Text;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PlaytimeService {
    private final JavaPlugin plugin;
    private final TimeStore store;
    private final RewardService rewardService;
    private final ConcurrentMap<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
    private PlaytimePlusConfig config;
    private BukkitTask checkTask;
    private BukkitTask saveTask;

    public PlaytimeService(JavaPlugin plugin, PlaytimePlusConfig config, TimeStore store, RewardService rewardService) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
        this.rewardService = rewardService;
    }

    public synchronized void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            startSession(player, false);
        }
        scheduleTasks();
    }

    public synchronized void stop() {
        cancelTasks();
        long now = now();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSession session = sessions.get(player.getUniqueId());
            if (session != null) {
                accrue(store.record(player.getUniqueId()), session, now);
                player.setSleepingIgnored(false);
            }
        }
        store.save();
    }

    public synchronized void reload(PlaytimePlusConfig config) {
        this.config = config;
        rewardService.reload(config);
        cancelTasks();
        scheduleTasks();
    }

    public synchronized void handleJoin(Player player) {
        startSession(player, true);
    }

    public synchronized void handleQuit(Player player) {
        long now = now();
        PlayerSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        PlayerTimeRecord record = store.record(player.getUniqueId());
        record.name(player.getName());
        accrue(record, session, now);
        player.setSleepingIgnored(false);
        store.save();
    }

    public synchronized void recordActivity(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            startSession(player, false);
            session = sessions.get(player.getUniqueId());
        }
        if (session == null) {
            return;
        }

        long now = now();
        if (session.afk()) {
            if (config.clearAfkOnActivity()) {
                leaveAfk(player, session, now, true);
            }
            return;
        }

        session.lastActivityMillis(now);
        session.warned(false);
        store.record(player.getUniqueId()).markSeen(now);
    }

    public synchronized void toggleManualAfk(Player player, String reason) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            startSession(player, false);
            session = sessions.get(player.getUniqueId());
        }
        if (session == null) {
            return;
        }

        long now = now();
        if (session.afk()) {
            leaveAfk(player, session, now, true);
            return;
        }

        PlayerTimeRecord record = store.record(player.getUniqueId());
        accrue(record, session, now);
        enterAfk(player, session, true, cleanReason(reason), now, true);
    }

    public synchronized boolean isAfk(UUID uuid) {
        PlayerSession session = sessions.get(uuid);
        return session != null && session.afk();
    }

    public synchronized String afkReason(UUID uuid) {
        PlayerSession session = sessions.get(uuid);
        return session == null ? "" : session.afkReason();
    }

    public synchronized Optional<PlayerTimeView> snapshot(UUID uuid) {
        long now = now();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            checkPlayer(player, now, false);
        }
        return store.existing(uuid).map(record -> snapshot(record, now));
    }

    public synchronized Optional<PlayerTimeRecord> findByName(String playerName) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return Optional.of(store.record(online.getUniqueId()));
        }
        return store.findByName(playerName);
    }

    public synchronized List<PlayerTimeView> top(TimeMetric metric) {
        long now = now();
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player, now, false);
        }
        return store.records().stream()
                .map(record -> snapshot(record, now))
                .sorted(Comparator.comparingLong((PlayerTimeView view) -> view.metricMillis(metric)).reversed()
                        .thenComparing(PlayerTimeView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized int rank(UUID uuid, TimeMetric metric) {
        List<PlayerTimeView> ranking = top(metric);
        for (int index = 0; index < ranking.size(); index++) {
            if (ranking.get(index).uuid().equals(uuid)) {
                return index + 1;
            }
        }
        return -1;
    }

    public synchronized long rewardClaims(UUID uuid) {
        return store.existing(uuid)
                .map(record -> rewardService.totalClaimed(record))
                .orElse(0L);
    }

    public synchronized void save() {
        long now = now();
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player, now, true);
        }
        store.save();
    }

    public synchronized void reset(UUID uuid, String name) {
        long now = now();
        PlayerTimeRecord record = store.record(uuid);
        record.reset(name, now);
        PlayerSession session = sessions.get(uuid);
        if (session != null) {
            session.resetJoin(now);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setSleepingIgnored(false);
            }
        }
        store.save();
    }

    public synchronized void setMetric(UUID uuid, String name, TimeMetric metric, long millis) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            checkPlayer(player, now(), false);
        }
        PlayerTimeRecord record = store.record(uuid);
        record.name(name);
        record.setMetric(metric, millis);
        record.markSeen(now());
        store.save();
    }

    public synchronized void addMetric(UUID uuid, String name, TimeMetric metric, long millis) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            checkPlayer(player, now(), false);
        }
        PlayerTimeRecord record = store.record(uuid);
        record.name(name);
        record.addMetric(metric, millis);
        record.markSeen(now());
        store.save();
    }

    public int recordCount() {
        return store.size();
    }

    public int onlineCount() {
        return sessions.size();
    }

    public long afkCount() {
        return sessions.values().stream().filter(PlayerSession::afk).count();
    }

    public PlaytimePlusConfig config() {
        return config;
    }

    public RewardService rewards() {
        return rewardService;
    }

    private void startSession(Player player, boolean countJoin) {
        long now = now();
        PlayerTimeRecord record = store.record(player.getUniqueId());
        if (countJoin) {
            record.markJoin(player.getName(), now);
        } else {
            record.name(player.getName());
            record.markSeen(now);
        }
        sessions.put(player.getUniqueId(), new PlayerSession(player.getUniqueId(), now));
        player.setSleepingIgnored(false);
    }

    private void scheduleTasks() {
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, config.checkIntervalTicks(), config.checkIntervalTicks());
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, config.saveIntervalTicks(), config.saveIntervalTicks());
    }

    private void cancelTasks() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    private synchronized void tick() {
        long now = now();
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player, now, true);
        }
    }

    private void checkPlayer(Player player, long now) {
        checkPlayer(player, now, true);
    }

    private void checkPlayer(Player player, long now, boolean processRewards) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            startSession(player, false);
            session = sessions.get(player.getUniqueId());
        }
        if (session == null) {
            return;
        }

        PlayerTimeRecord record = store.record(player.getUniqueId());
        record.name(player.getName());

        if (session.afk()) {
            accrue(record, session, now);
            checkRewards(player, record, now, processRewards);
            maybeKick(player, session, now);
            return;
        }

        if (!config.afkEnabled() || config.autoAfkMillis() <= 0L || hasPermission(player, config.autoAfkBypassPermission())) {
            accrue(record, session, now);
            checkRewards(player, record, now, processRewards);
            return;
        }

        long idleMillis = Math.max(0L, now - session.lastActivityMillis());
        warnIfNeeded(player, session, idleMillis);

        if (idleMillis >= config.autoAfkMillis()) {
            long thresholdAt = Math.min(now, session.lastActivityMillis() + config.autoAfkMillis());
            accrue(record, session, thresholdAt);
            enterAfk(player, session, false, "", thresholdAt, true);
            accrue(record, session, now);
            checkRewards(player, record, now, processRewards);
            return;
        }

        accrue(record, session, now);
        checkRewards(player, record, now, processRewards);
    }

    private void checkRewards(Player player, PlayerTimeRecord record, long now, boolean processRewards) {
        if (processRewards) {
            rewardService.check(player, record, snapshot(record, now));
        }
    }

    private void warnIfNeeded(Player player, PlayerSession session, long idleMillis) {
        long warningMillis = config.warningMillis();
        long autoMillis = config.autoAfkMillis();
        if (warningMillis <= 0L || warningMillis >= autoMillis || session.warned() || idleMillis < warningMillis) {
            return;
        }

        long remaining = Math.max(0L, autoMillis - idleMillis);
        send(player, "afk-warning", Map.of("time", DurationFormatter.compact(remaining)));
        session.warned(true);
    }

    private void enterAfk(Player player, PlayerSession session, boolean manual, String reason, long sinceMillis, boolean notify) {
        session.afk(true);
        session.afkSinceMillis(sinceMillis);
        session.manualAfk(manual);
        session.afkReason(reason);
        session.warned(false);
        if (config.ignoreSleep()) {
            player.setSleepingIgnored(true);
        }

        Map<String, String> placeholders = afkPlaceholders(player, session, now());
        if (notify) {
            send(player, "afk-on", placeholders);
            broadcast(player, "afk-broadcast", placeholders);
        }
        Bukkit.getPluginManager().callEvent(new PlaytimeAfkStateChangeEvent(player, true, manual, reason, 0L));
    }

    private void leaveAfk(Player player, PlayerSession session, long now, boolean notify) {
        long duration = Math.max(0L, now - session.afkSinceMillis());
        boolean manual = session.manualAfk();
        String reason = session.afkReason();
        PlayerTimeRecord record = store.record(player.getUniqueId());
        accrue(record, session, now);

        Map<String, String> placeholders = new HashMap<>(afkPlaceholders(player, session, now));
        placeholders.put("duration", DurationFormatter.compact(duration));

        session.afk(false);
        session.afkSinceMillis(0L);
        session.manualAfk(false);
        session.afkReason("");
        session.lastActivityMillis(now);
        session.warned(false);
        player.setSleepingIgnored(false);

        if (notify) {
            send(player, "afk-off", placeholders);
            broadcast(player, "afk-return-broadcast", placeholders);
        }
        Bukkit.getPluginManager().callEvent(new PlaytimeAfkStateChangeEvent(player, false, manual, reason, duration));
    }

    private void maybeKick(Player player, PlayerSession session, long now) {
        if (!config.kickEnabled() || config.kickAfterAfkMillis() <= 0L || hasPermission(player, config.kickBypassPermission())) {
            return;
        }
        long afkDuration = Math.max(0L, now - session.afkSinceMillis());
        if (afkDuration < config.kickAfterAfkMillis()) {
            return;
        }
        String message = Text.color(Text.render(config.message("auto-kick"), Map.of(
                "duration", DurationFormatter.compact(afkDuration),
                "player", player.getName()
        )));
        player.kickPlayer(message);
    }

    private void accrue(PlayerTimeRecord record, PlayerSession session, long untilMillis) {
        long elapsed = untilMillis - session.lastTickMillis();
        if (elapsed <= 0L) {
            return;
        }
        if (session.afk()) {
            record.addAfk(elapsed);
        } else {
            record.addActive(elapsed);
        }
        session.lastTickMillis(untilMillis);
        record.markSeen(untilMillis);
    }

    private PlayerTimeView snapshot(PlayerTimeRecord record, long now) {
        PlayerSession session = sessions.get(record.uuid());
        Player online = Bukkit.getPlayer(record.uuid());
        boolean onlineNow = online != null && session != null;
        return record.snapshot(
                onlineNow,
                onlineNow && session.afk(),
                onlineNow ? Math.max(0L, now - session.joinMillis()) : 0L,
                onlineNow ? session.afkReason() : ""
        );
    }

    private Map<String, String> afkPlaceholders(Player player, PlayerSession session, long now) {
        String reason = session.afkReason();
        return Map.of(
                "player", player.getName(),
                "reason", reason == null || reason.isBlank() ? "" : ": " + reason,
                "duration", session.afk() ? DurationFormatter.compact(now - session.afkSinceMillis()) : "0s"
        );
    }

    private void send(Player player, String messageKey, Map<String, String> placeholders) {
        player.sendMessage(Text.color(config.prefix() + Text.render(config.message(messageKey), placeholders)));
    }

    private void broadcast(Player source, String messageKey, Map<String, String> placeholders) {
        if (!config.broadcastAfk()) {
            return;
        }
        String message = Text.color(config.prefix() + Text.render(config.message(messageKey), placeholders));
        for (Player audience : Bukkit.getOnlinePlayers()) {
            if (!audience.equals(source) && audience.hasPermission("playtimeplus.notify")) {
                audience.sendMessage(message);
            }
        }
    }

    private boolean hasPermission(Player player, String permission) {
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private String cleanReason(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
