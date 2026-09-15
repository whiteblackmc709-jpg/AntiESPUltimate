package com.example.antiespultimate;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.example.antiespultimate.commands.AntiESPCommand;
import com.example.antiespultimate.integrations.AntiESPPlaceholders;
import com.example.antiespultimate.modules.BlockObfuscationModule;
import com.example.antiespultimate.modules.PlayerVisibilityModule;
import com.example.antiespultimate.modules.ViewDistanceModule;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiESPUltimate extends JavaPlugin {

    private ProtocolManager protocolManager;
    private BlockObfuscationModule blockObfuscationModule;
    private PlayerVisibilityModule playerVisibilityModule;
    private ViewDistanceModule viewDistanceModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // FIX P0-01: kiểm tra ProtocolLib tồn tại
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib không được cài đặt! AntiESPUltimate cần ProtocolLib.");
            getLogger().severe("Tải tại: https://www.spigotmc.org/resources/protocollib.1997/");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
        } catch (Throwable t) {
            getLogger().severe("Không thể lấy ProtocolManager: " + t.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        reloadModules();

        // FIX P0-02: null-check cho command
        org.bukkit.command.PluginCommand command = getCommand("antiespu");
        if (command == null) {
            getLogger().severe("Command /antiespu không có trong plugin.yml — kiểm tra lại!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        AntiESPCommand cmd = new AntiESPCommand(this);
        command.setExecutor(cmd);
        command.setTabCompleter(cmd);

        // FIX 3.1: hook PlaceholderAPI nếu có (soft-depend, không bắt buộc)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new AntiESPPlaceholders(this).register();
                getLogger().info("Đã hook PlaceholderAPI.");
            } catch (Throwable t) {
                getLogger().warning("Không thể hook PlaceholderAPI: " + t.getMessage());
            }
        }

        // FIX 3.2: bStats (chỉ bật nếu admin đã điền ID thật vào config.yml)
        int bstatsId = getConfig().getInt("bstats-plugin-id", -1);
        if (bstatsId > 0) {
            try {
                new org.bstats.bukkit.Metrics(this, bstatsId);
                getLogger().info("bStats metrics đã bật (id=" + bstatsId + ").");
            } catch (Throwable t) {
                getLogger().warning("bStats không khởi tạo được: " + t.getMessage());
            }
        }

        getLogger().info("AntiESPUltimate enabled.");
    }

    @Override
    public void onDisable() {
        if (blockObfuscationModule != null) blockObfuscationModule.shutdown();
        if (playerVisibilityModule != null) playerVisibilityModule.shutdown();
        if (viewDistanceModule != null) viewDistanceModule.shutdown();
        getLogger().info("AntiESPUltimate disabled.");
    }

    /** Tears down and re-registers all modules from the current config. Used by /antiespu reload. */
    public void reloadModules() {
        // FIX P1-09: log để dễ debug
        getLogger().info("[reload] Tearing down modules cũ...");

        if (blockObfuscationModule != null) { blockObfuscationModule.shutdown(); blockObfuscationModule = null; }
        if (playerVisibilityModule != null) { playerVisibilityModule.shutdown(); playerVisibilityModule = null; }
        if (viewDistanceModule != null) { viewDistanceModule.shutdown(); viewDistanceModule = null; }

        if (getConfig().getBoolean("block-obfuscation.enabled", true)) {
            try {
                blockObfuscationModule = new BlockObfuscationModule(this, protocolManager);
                blockObfuscationModule.start();
                getLogger().info("[reload] block-obfuscation: BẬT");
            } catch (Throwable t) {
                getLogger().severe("[reload] block-obfuscation khởi tạo lỗi: " + t.getMessage());
                blockObfuscationModule = null;
            }
        } else {
            getLogger().info("[reload] block-obfuscation: TẮT");
        }

        if (getConfig().getBoolean("player-visibility.enabled", true)) {
            try {
                playerVisibilityModule = new PlayerVisibilityModule(this, protocolManager);
                playerVisibilityModule.start();
                getLogger().info("[reload] player-visibility: BẬT");
            } catch (Throwable t) {
                getLogger().severe("[reload] player-visibility khởi tạo lỗi: " + t.getMessage());
                playerVisibilityModule = null;
            }
        } else {
            getLogger().info("[reload] player-visibility: TẮT");
        }

        if (getConfig().getBoolean("view-distance-limit.enabled", true)) {
            try {
                viewDistanceModule = new ViewDistanceModule(this);
                viewDistanceModule.start();
                getLogger().info("[reload] view-distance-limit: BẬT");
            } catch (Throwable t) {
                getLogger().severe("[reload] view-distance-limit khởi tạo lỗi: " + t.getMessage());
                viewDistanceModule = null;
            }
        } else {
            getLogger().info("[reload] view-distance-limit: TẮT");
        }
    }

    public boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    public void debug(String msg) {
        if (isDebug()) getLogger().info("[debug] " + msg);
    }

    public BlockObfuscationModule getBlockObfuscationModule() {
        return blockObfuscationModule;
    }

    // FIX 1.13: getter cho các module còn lại (trước chỉ có block-obfuscation)
    public PlayerVisibilityModule getPlayerVisibilityModule() {
        return playerVisibilityModule;
    }

    public ViewDistanceModule getViewDistanceModule() {
        return viewDistanceModule;
    }

    // FIX 2.20
    public boolean isProtocolLibReady() {
        return protocolManager != null;
    }
}
