package sdfrpe.github.io.ptcclans.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

@Deprecated
public class ClanLogger {
    private static final String PREFIX = "PTCClans";
    private static PTCClans plugin;

    public static void initialize(PTCClans pluginInstance) {
        plugin = pluginInstance;
        ClanLogSystem.info(LogCategory.CORE, "ClanLogger inicializado (legacy wrapper)");
    }

    public static void info(String... messages) {
        ClanLogSystem.info(LogCategory.CORE, messages);
    }

    public static void warn(String... messages) {
        ClanLogSystem.warn(LogCategory.CORE, messages);
    }

    public static void error(String... messages) {
        ClanLogSystem.error(LogCategory.CORE, messages);
    }

    public static void debug(String... messages) {
        ClanLogSystem.debug(LogCategory.CORE, messages);
    }

    public static void setDebugEnabled(boolean enabled) {
        ClanLogSystem.setDebugMode(enabled);
    }

    public static boolean isDebugEnabled() {
        return ClanLogSystem.isDebugEnabled();
    }
}