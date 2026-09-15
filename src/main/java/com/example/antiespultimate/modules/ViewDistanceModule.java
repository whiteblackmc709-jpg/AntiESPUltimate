package com.example.antiespultimate.modules;

import com.example.antiespultimate.AntiESPUltimate;
import com.example.antiespultimate.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Clamps the per-player sent view distance (Paper API). This does not detect
 * or block FreeCam -- nothing server-side can, since freecam sends no packets
 * of its own. It only limits how much world data a client is ever given in
 * the first place, which caps how far any client (freecam included) can look.
 *
 * If you're on plain Spigot (not Paper), Player#setSendViewDistance doesn't
 * exist; drop this module or replace it with lowering the global
 * server.properties view-distance/simulation-distance instead.
 */
public class ViewDistanceModule implements Listener {

    private final AntiESPUltimate plugin;
    private final int maxViewDistance;

    public ViewDistanceModule(AntiESPUltimate plugin) {
        this.plugin = plugin;
        // FIX 1.8 (thay P3-08 thủ công): dùng ConfigUtil để validate range
        this.maxViewDistance = ConfigUtil.getBoundedInt(plugin,
                "view-distance-limit.max-sent-view-distance", 6, 2, 32);
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    public void shutdown() {
        // FIX P1-04: unregister mọi event của listener này
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }

    // FIX 2.16: re-apply sau respawn — một số client reset view-distance
    // request khi hồi sinh (đổi dimension, v.v.)
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        apply(event.getPlayer());
    }

    // FIX P1-03: flag để không log lặp
    private boolean paperApiAvailable = true;

    private void apply(Player player) {
        if (!paperApiAvailable) return;
        try {
            // Paper-only API. Comment out / remove this call if building against
            // plain Spigot, and rely on server.properties view-distance instead.
            player.setSendViewDistance(maxViewDistance);
        } catch (NoSuchMethodError ex) {
            // FIX P1-03 + P2-08: log warning 1 lần, không nuốt lỗi bằng debug()
            paperApiAvailable = false;
            plugin.getLogger().warning("Paper API setSendViewDistance không khả dụng "
                    + "(có thể đang chạy Spigot). Tắt module view-distance-limit.");
        } catch (Exception ex) {
            plugin.getLogger().warning("Không thể setSendViewDistance cho "
                    + player.getName() + ": " + ex.getMessage());
        }
    }
}
