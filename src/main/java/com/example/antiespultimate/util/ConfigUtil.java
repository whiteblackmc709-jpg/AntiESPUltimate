package com.example.antiespultimate.util;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Helpers for reading numeric config values with range validation, so a
 * typo'd or malicious config.yml can't push a module into a nonsensical or
 * dangerous state (e.g. view-distance = 0, negative check-interval).
 */
public final class ConfigUtil {

    private ConfigUtil() {}

    public static int getBoundedInt(JavaPlugin p, String path, int def, int min, int max) {
        int v = p.getConfig().getInt(path, def);
        if (v < min || v > max) {
            p.getLogger().warning("Config '" + path + "'=" + v + " ngoài [" + min + "," + max + "] — dùng " + def);
            return def;
        }
        return v;
    }

    public static double getBoundedDouble(JavaPlugin p, String path, double def, double min, double max) {
        double v = p.getConfig().getDouble(path, def);
        if (Double.isNaN(v) || Double.isInfinite(v) || v < min || v > max) {
            p.getLogger().warning("Config '" + path + "'=" + v + " ngoài [" + min + "," + max + "] — dùng " + def);
            return def;
        }
        return v;
    }

    public static long getBoundedLong(JavaPlugin p, String path, long def, long min, long max) {
        long v = p.getConfig().getLong(path, def);
        if (v < min || v > max) {
            p.getLogger().warning("Config '" + path + "'=" + v + " ngoài [" + min + "," + max + "] — dùng " + def);
            return def;
        }
        return v;
    }
}
