package com.example.antiespultimate.integrations;

import com.example.antiespultimate.AntiESPUltimate;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Optional PlaceholderAPI expansion. Only loaded/registered if PlaceholderAPI
 * is installed (see AntiESPUltimate#onEnable) -- this class itself is never
 * referenced unless that check passes, so it's safe even when the
 * PlaceholderAPI jar isn't present at runtime as long as it's a `provided`
 * (compile-only) Maven dependency.
 */
public class AntiESPPlaceholders extends PlaceholderExpansion {

    private final AntiESPUltimate plugin;

    public AntiESPPlaceholders(AntiESPUltimate plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "antiespu";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        switch (params.toLowerCase()) {
            case "enabled":
                return plugin.isEnabled() ? "1" : "0";
            case "obf":
                return plugin.getBlockObfuscationModule() != null ? "1" : "0";
            case "vis":
                return plugin.getPlayerVisibilityModule() != null ? "1" : "0";
            case "viewdistance":
                return plugin.getViewDistanceModule() != null ? "1" : "0";
            case "packets":
                return plugin.getBlockObfuscationModule() != null
                        ? String.valueOf(plugin.getBlockObfuscationModule().getPacketsProcessed())
                        : "0";
            case "stripped":
                return plugin.getBlockObfuscationModule() != null
                        ? String.valueOf(plugin.getBlockObfuscationModule().getBlockEntitiesStripped())
                        : "0";
            default:
                return "";
        }
    }
}
