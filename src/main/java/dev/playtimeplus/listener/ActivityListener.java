package dev.playtimeplus.listener;

import dev.playtimeplus.time.PlaytimeService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ActivityListener implements Listener {
    private final JavaPlugin plugin;
    private final PlaytimeService playtimeService;

    public ActivityListener(JavaPlugin plugin, PlaytimeService playtimeService) {
        this.plugin = plugin;
        this.playtimeService = playtimeService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        playtimeService.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playtimeService.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!activityCounts("movement", true)) {
            return;
        }
        if (movementCounts(event.getFrom(), event.getTo())) {
            playtimeService.recordActivity(event.getPlayer(), "movement", locationSignature(event.getTo()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!activityCounts("commands", false)) {
            return;
        }
        String command = event.getMessage().split(" ", 2)[0].replaceFirst("^/", "");
        if (!command.equalsIgnoreCase("afk")) {
            playtimeService.recordActivity(event.getPlayer(), "commands", command.toLowerCase(java.util.Locale.ROOT));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!activityCounts("chat", true)) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                playtimeService.recordActivity(player, "chat", "chat");
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!activityCounts("interact", true)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "interact", interactSignature(event.getAction(), event.getClickedBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!activityCounts("block-break", true)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "block-break", blockSignature(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!activityCounts("block-place", true)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "block-place", blockSignature(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!activityCounts("inventory-click", false)) {
            return;
        }
        if (event.getWhoClicked() instanceof Player player) {
            playtimeService.recordActivity(player, "inventory-click", String.valueOf(event.getSlot()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!activityCounts("item-drop", false)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "item-drop", event.getItemDrop().getItemStack().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!activityCounts("item-consume", true)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "item-consume", event.getItem().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItem(PlayerItemHeldEvent event) {
        if (!activityCounts("hotbar-change", false)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "hotbar-change", event.getPreviousSlot() + ">" + event.getNewSlot());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!activityCounts("swap-hands", false)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "swap-hands", "swap");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!activityCounts("teleport", false)) {
            return;
        }
        playtimeService.recordActivity(event.getPlayer(), "teleport", locationSignature(event.getTo()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!activityCounts("combat", true)) {
            return;
        }
        if (event.getDamager() instanceof Player player) {
            playtimeService.recordActivity(player, "combat", event.getEntityType().name());
        }
    }

    private boolean activityCounts(String key, boolean fallback) {
        return playtimeService.config().activityCounts(key, fallback);
    }

    private boolean movementCounts(Location from, Location to) {
        if (to == null || from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return true;
        }

        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared >= playtimeService.config().movementDistanceThresholdSquared()) {
            return true;
        }

        if (!playtimeService.config().countLookMovement()) {
            return false;
        }
        return angleDelta(from.getYaw(), to.getYaw()) >= playtimeService.config().lookThresholdDegrees()
                || angleDelta(from.getPitch(), to.getPitch()) >= playtimeService.config().lookThresholdDegrees();
    }

    private double angleDelta(float left, float right) {
        double delta = Math.abs(left - right) % 360.0D;
        return delta > 180.0D ? 360.0D - delta : delta;
    }

    private String interactSignature(Action action, Block block) {
        Material material = block == null ? Material.AIR : block.getType();
        String location = block == null ? "air" : blockSignature(block);
        return action.name() + ":" + material.name() + ":" + location;
    }

    private String blockSignature(Block block) {
        return block.getWorld().getName() + ":" + block.getType().name() + ":"
                + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private String locationSignature(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return location.getWorld().getName() + ":"
                + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
