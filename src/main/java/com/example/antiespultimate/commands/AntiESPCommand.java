package com.example.antiespultimate.commands;

import com.example.antiespultimate.AntiESPUltimate;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AntiESPCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "reload", "status", "modules", "counters", "help");

    private final AntiESPUltimate plugin;

    public AntiESPCommand(AntiESPUltimate plugin) {
        this.plugin = plugin;
    }

    // FIX 2.5: permission cấu hình được thay vì hardcode
    private String adminPermission() {
        return plugin.getConfig().getString("permissions.admin", "antiespu.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(adminPermission())) {
            // FIX P1-12: default value nếu config thiếu
            String noPerm = plugin.getConfig().getString("messages.no-permission",
                    "&cBạn không có quyền dùng lệnh này.");
            sender.sendMessage(color(noPerm));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                // FIX 2.4: log ai reload, khi nào
                plugin.getLogger().info("[AntiESP] " + sender.getName() + " đã chạy /antiespu reload.");
                plugin.reloadConfig();
                plugin.reloadModules();
                String reloaded = plugin.getConfig().getString("messages.reloaded",
                        "&aAntiESPUltimate config đã reload.");
                sender.sendMessage(color(reloaded));
                return true;

            case "status":
                sendStatus(sender);
                return true;

            case "modules":
                sendModules(sender);
                return true;

            case "counters":
                sendCounters(sender);
                return true;

            case "help":
                sendHelp(sender);
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Lệnh con không hợp lệ: " + args[0]);
                sendHelp(sender);
                return true;
        }
    }

    // FIX 1.3: status giờ chỉ tóm tắt 1 dòng/module; chi tiết chuyển sang
    // /antiespu counters để không tràn chat.
    private void sendStatus(CommandSender sender) {
        // FIX 1.4: build-marker giờ đọc từ config, không hardcode; rỗng thì ẩn luôn
        String marker = plugin.getConfig().getString("build-marker", "");
        sender.sendMessage(ChatColor.GOLD + "=== AntiESPUltimate ==="
                + (marker.isEmpty() ? "" : ChatColor.YELLOW + " [" + marker + "]"));
        sender.sendMessage("Debug: " + plugin.isDebug()
                + "  |  ProtocolLib sẵn sàng: " + plugin.isProtocolLibReady());
        sender.sendMessage("Block obfuscation: " + summarize("block-obfuscation.enabled", plugin.getBlockObfuscationModule() != null));
        sender.sendMessage("Player visibility: " + summarize("player-visibility.enabled", plugin.getPlayerVisibilityModule() != null));
        sender.sendMessage("View distance limit: " + summarize("view-distance-limit.enabled", plugin.getViewDistanceModule() != null));
        sender.sendMessage(ChatColor.GRAY + "Chi tiết: /antiespu modules, /antiespu counters");
        sender.sendMessage(ChatColor.GRAY + "Lưu ý: FreeCam không thể bị phát hiện/chặn từ phía server; "
                + "view-distance-limit chỉ giảm phạm vi dữ liệu nó có thể nhìn thấy.");
    }

    private String summarize(String configPath, boolean actuallyRunning) {
        boolean configEnabled = plugin.getConfig().getBoolean(configPath, true);
        if (!configEnabled) return ChatColor.GRAY + "TẮT (config)" + ChatColor.RESET;
        return actuallyRunning
                ? ChatColor.GREEN + "BẬT" + ChatColor.RESET
                : ChatColor.RED + "LỖI khởi tạo (xem console)" + ChatColor.RESET;
    }

    // FIX 1.3: sub riêng liệt kê rõ 3 module bật/tắt
    private void sendModules(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Modules ===");
        sender.sendMessage("block-obfuscation:    " + summarize("block-obfuscation.enabled", plugin.getBlockObfuscationModule() != null));
        sender.sendMessage("player-visibility:    " + summarize("player-visibility.enabled", plugin.getPlayerVisibilityModule() != null));
        sender.sendMessage("view-distance-limit:  " + summarize("view-distance-limit.enabled", plugin.getViewDistanceModule() != null)
                + "  (max " + plugin.getConfig().getInt("view-distance-limit.max-sent-view-distance") + ")");
    }

    // FIX 1.3: sub riêng cho counter chẩn đoán (nội dung debug cũ nằm ở đây)
    private void sendCounters(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Counters (block-obfuscation) ===");
        var mod = plugin.getBlockObfuscationModule();
        if (mod == null) {
            sender.sendMessage(ChatColor.RED + "Module không chạy (xem lỗi console lúc khởi động).");
            return;
        }
        sender.sendMessage("MAP_CHUNK packets processed: " + mod.getPacketsProcessed());
        sender.sendMessage("Block-entities scanned: " + mod.getBlockEntitiesScanned()
                + "  |  stripped: " + mod.getBlockEntitiesStripped()
                + "  |  NBT read failures: " + mod.getNbtReadFailures()
                + "  |  reflection field-access failures: " + mod.getReflectionFieldAccessFailures());
        sender.sendMessage(ChatColor.AQUA + "Last packet handle class: " + ChatColor.WHITE + mod.getLastHandleClassName());
        sender.sendMessage(ChatColor.AQUA + "Its top-level fields: " + ChatColor.WHITE + mod.getLastTopLevelFields());
        sender.sendMessage(ChatColor.AQUA + "coords-null: " + mod.getCoordsNullCount()
                + "  handle-null: " + mod.getHandleNullCount()
                + "  outer-exceptions: " + mod.getOuterExceptionCount());
        sender.sendMessage(ChatColor.AQUA + "Last outer error: " + ChatColor.WHITE + mod.getLastOuterError());
        sender.sendMessage(ChatColor.GRAY + "(packets=0 -> listener never fires. scanned=0 -> reflection couldn't"
                + " locate the BlockEntityInfo list on this packet (check console for 'could not locate a"
                + " BlockEntityInfo list' with debug:true). scanned>0 but stripped=0 -> the real block at that"
                + " position isn't in your obfuscated-blocks list, or coordinate math is off.)");
    }

    // FIX 1.5: subcommand help
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== /antiespu ===");
        sender.sendMessage(ChatColor.YELLOW + "/antiespu status" + ChatColor.GRAY + "   — trạng thái tổng quan");
        sender.sendMessage(ChatColor.YELLOW + "/antiespu modules" + ChatColor.GRAY + "  — 3 module bật/tắt");
        sender.sendMessage(ChatColor.YELLOW + "/antiespu counters" + ChatColor.GRAY + " — counter packet/NBT chi tiết");
        sender.sendMessage(ChatColor.YELLOW + "/antiespu reload" + ChatColor.GRAY + "   — reload config");
        sender.sendMessage(ChatColor.YELLOW + "/antiespu help" + ChatColor.GRAY + "     — hiện lại trợ giúp này");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // FIX P2-02: filter theo prefix
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) out.add(sub);
            }
            return out;
        }
        return Collections.emptyList();
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
