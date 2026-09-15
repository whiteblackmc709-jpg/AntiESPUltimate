package com.example.antiespultimate.modules;

import com.comphenix.protocol.ProtocolManager;
import com.example.antiespultimate.AntiESPUltimate;
import com.example.antiespultimate.util.ConfigUtil;
import com.example.antiespultimate.util.RaycastUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Uses Bukkit's own Player#hidePlayer / showPlayer (backed by the server's
 * normal entity spawn/destroy packets) rather than raw NMS reflection, so it
 * keeps working across server version bumps without needing to be rewritten
 * against new mapped class names each time.
 *
 * Quy tắc ẩn/hiện (mục 1.2 — làm rõ ngữ nghĩa min/max-distance):
 *   - distance > maxDistance   -> LUÔN hiển thị (quá xa để cần che)
 *   - distance <= minDistance  -> LUÔN hiển thị (quá gần, tránh giật hình khi
 *                                  che/hiện liên tục lúc 2 người sát nhau)
 *   - ngược lại                -> che nếu KHÔNG có line-of-sight hợp lệ
 */
public class PlayerVisibilityModule implements org.bukkit.event.Listener {

    private final AntiESPUltimate plugin;
    private final ProtocolManager protocolManager; // kept for parity/future packet-level needs
    private final long checkIntervalTicks;
    private final double maxDistance;
    private final double minDistance;
    private final double blockingThreshold;
    private final boolean teamExemption;

    // FIX 1.1: cache team lookup thay vì gọi getScoreboard().getEntryTeam() mỗi pair
    private final Map<java.util.UUID, Team> teamCache = new ConcurrentHashMap<>();
    private volatile long lastTeamRefresh = 0;

    // FIX 2.3: rate-limit debug log "can see/can no longer see" khi nhiều người
    // đổi trạng thái cùng lúc (ví dụ: cả server bước ra khỏi 1 khu vực che khuất)
    private final Map<String, Long> debugLastLog = new ConcurrentHashMap<>();

    private void debugRateLimited(String key, String msg) {
        if (!plugin.isDebug()) return;
        long now = System.currentTimeMillis();
        Long last = debugLastLog.get(key);
        if (last != null && now - last < 3000) return;
        debugLastLog.put(key, now);
        plugin.debug(msg);
    }

    // FIX P0-05: dùng Map<UUID, Set<UUID>> thay vì Map<String, Boolean>
    private final Map<java.util.UUID, java.util.Set<java.util.UUID>> hiddenByViewer = new ConcurrentHashMap<>();

    // Legacy — giữ để tương thích method cũ
    // viewer -> set of targets currently hidden from them
    private final Map<String, Boolean> hiddenPairs = new ConcurrentHashMap<>();

    private BukkitTask task;

    public PlayerVisibilityModule(AntiESPUltimate plugin, ProtocolManager protocolManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        // FIX 1.8: dùng ConfigUtil để validate range
        this.checkIntervalTicks = ConfigUtil.getBoundedLong(plugin,
                "player-visibility.check-interval", 2, 1, 200);
        this.maxDistance = ConfigUtil.getBoundedDouble(plugin,
                "player-visibility.max-distance", 64.0, 1.0, 256.0);
        this.minDistance = ConfigUtil.getBoundedDouble(plugin,
                "player-visibility.min-distance", 5.0, 0.0, 256.0);
        this.blockingThreshold = ConfigUtil.getBoundedDouble(plugin,
                "player-visibility.blocking-threshold", 0.7, 0.0, 1.0);
        this.teamExemption = plugin.getConfig().getBoolean("player-visibility.team-exemption", true);
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, checkIntervalTicks);
        // FIX P0-06: đăng ký PlayerQuitEvent
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        // FIX P2-05: chỉ showPlayer nếu plugin còn enabled
        if (plugin.isEnabled()) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (viewer != target) viewer.showPlayer(plugin, target);
                }
            }
        }
        hiddenByViewer.clear();
        hiddenPairs.clear();
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        // FIX P0-05: dọn theo UUID
        hiddenByViewer.remove(uuid);
        for (java.util.Set<java.util.UUID> set : hiddenByViewer.values()) {
            set.remove(uuid);
        }
        // Legacy
        hiddenPairs.keySet().removeIf(k -> k.startsWith(uuid.toString()) || k.endsWith(uuid.toString()));
        teamCache.remove(uuid);
    }

    // FIX P1-01: stagger — 1 viewer mỗi tick
    private int viewerIndex = 0;

    private void tick() {
        java.util.Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;

        // FIX 1.1: refresh team cache tối đa 1 lần/2s, không phải mỗi pair
        refreshTeamCache();

        // FIX P1-02: bỏ toArray, dùng Collection
        java.util.List<Player> snapshot = new java.util.ArrayList<>(players);
        viewerIndex = (viewerIndex + 1) % snapshot.size();
        Player viewer = snapshot.get(viewerIndex);

        if (viewer.hasPermission("antiespu.bypass")) return;

        for (Player target : snapshot) {
            if (viewer == target) continue;
            if (target.hasPermission("antiespu.bypass")) continue;
            try {
                evaluatePair(viewer, target);
            } catch (Exception ex) {
                // One bad pair (e.g. an edge case we haven't seen) must never
                // stop the rest of this tick's pairs from being evaluated,
                // and must never spam the console every 2 ticks.
                plugin.debug("evaluatePair failed for " + viewer.getName() + "/" + target.getName() + ": " + ex);
            }
        }
    }

    private void evaluatePair(Player viewer, Player target) {
        String key = viewer.getUniqueId() + ":" + target.getUniqueId();

        if (teamExemption && sameTeam(viewer, target)) {
            setHidden(viewer, target, key, false);
            return;
        }

        Location viewerEye = viewer.getEyeLocation();
        Location targetEye = target.getEyeLocation();

        // Different worlds (e.g. one in the_nether, one in world) can never see
        // each other -- and Bukkit throws IllegalArgumentException if you try
        // to measure distance across worlds, so this check has to come first.
        if (!viewerEye.getWorld().equals(targetEye.getWorld())) {
            setHidden(viewer, target, key, false);
            return;
        }

        double distance = viewerEye.distance(targetEye);

        if (distance > maxDistance || distance <= minDistance) {
            setHidden(viewer, target, key, false);
            return;
        }

        boolean visible = RaycastUtils.hasLineOfSight(viewerEye, targetEye, blockingThreshold);
        setHidden(viewer, target, key, !visible);
    }

    private void setHidden(Player viewer, Player target, String key, boolean shouldBeHidden) {
        boolean currentlyHidden = hiddenPairs.getOrDefault(key, false);
        if (shouldBeHidden == currentlyHidden) return;

        if (shouldBeHidden) {
            viewer.hidePlayer(plugin, target);
            hiddenPairs.put(key, true);
            debugRateLimited("hide", viewer.getName() + " can no longer see " + target.getName());
        } else {
            viewer.showPlayer(plugin, target);
            hiddenPairs.put(key, false);
            debugRateLimited("show", viewer.getName() + " can see " + target.getName() + " again");
        }
    }

    // FIX 1.1: refresh 1 lần mỗi 2s thay vì gọi getScoreboard().getEntryTeam() mỗi pair mỗi tick
    private void refreshTeamCache() {
        long now = System.currentTimeMillis();
        if (now - lastTeamRefresh < 2000) return;
        lastTeamRefresh = now;
        teamCache.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team t = p.getScoreboard().getEntryTeam(p.getName());
            if (t != null) teamCache.put(p.getUniqueId(), t);
        }
    }

    private boolean sameTeam(Player a, Player b) {
        Team teamA = teamCache.get(a.getUniqueId());
        Team teamB = teamCache.get(b.getUniqueId());
        return teamA != null && teamA.equals(teamB);
    }
}
