package sdfrpe.github.io.ptcclans.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.util.EnumSet;
import java.util.Set;

public class ClanLogSystem {

    private static final String PREFIX = "PTCClans";
    private static boolean DEBUG_ENABLED = false;
    private static final Set<LogCategory> ENABLED_DEBUG_CATEGORIES = EnumSet.noneOf(LogCategory.class);

    public enum LogLevel {
        ERROR("&4ERROR", 1),
        WARN("&6WARN", 2),
        INFO("&9INFO", 3),
        DEBUG("&eDEBUG", 4);

        private final String prefix;
        private final int priority;

        LogLevel(String prefix, int priority) {
            this.prefix = prefix;
            this.priority = priority;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getPriority() {
            return priority;
        }
    }

    public enum LogCategory {
        CORE("CORE"),
        CLAN("CLAN"),
        WAR("WAR"),
        DATABASE("DATABASE"),
        API("API"),
        NETWORK("NET"),
        MENU("MENU"),
        SYNC("SYNC");

        private final String tag;

        LogCategory(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }
    }

    public static void setDebugMode(boolean enabled) {
        DEBUG_ENABLED = enabled;
        if (enabled) {
            log(LogLevel.WARN, LogCategory.CORE, "DEBUG MODE ENABLED - Logs verbose activados");
        } else {
            log(LogLevel.INFO, LogCategory.CORE, "DEBUG MODE DISABLED - Solo logs importantes");
        }
    }

    public static void enableDebugCategory(LogCategory category) {
        ENABLED_DEBUG_CATEGORIES.add(category);
        log(LogLevel.INFO, LogCategory.CORE, "Debug habilitado para categoría: " + category.getTag());
    }

    public static void disableDebugCategory(LogCategory category) {
        ENABLED_DEBUG_CATEGORIES.remove(category);
        log(LogLevel.INFO, LogCategory.CORE, "Debug deshabilitado para categoría: " + category.getTag());
    }

    public static void log(LogLevel level, LogCategory category, String... messages) {
        if (level == LogLevel.DEBUG) {
            if (!DEBUG_ENABLED && !ENABLED_DEBUG_CATEGORIES.contains(category)) {
                return;
            }
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String msg : messages) {
            messageBuilder.append(msg).append(" ");
        }

        String finalMessage = messageBuilder.toString().trim();
        String formattedMessage = String.format("&8[&6%s&8] [%s&8] [&7%s&8]&f %s",
                PREFIX,
                level.getPrefix(),
                category.getTag(),
                finalMessage
        );

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', formattedMessage));
    }

    public static void log(LogLevel level, String... messages) {
        log(level, LogCategory.CORE, messages);
    }

    public static void error(LogCategory category, String... messages) {
        log(LogLevel.ERROR, category, messages);
    }

    public static void warn(LogCategory category, String... messages) {
        log(LogLevel.WARN, category, messages);
    }

    public static void info(LogCategory category, String... messages) {
        log(LogLevel.INFO, category, messages);
    }

    public static void debug(LogCategory category, String... messages) {
        log(LogLevel.DEBUG, category, messages);
    }

    public static void logPluginStart() {
        log(LogLevel.INFO, LogCategory.CORE, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.CORE, "PTCClans Plugin iniciando...");
        log(LogLevel.INFO, LogCategory.CORE, "═══════════════════════════════════");
    }

    public static void logPluginStop() {
        log(LogLevel.INFO, LogCategory.CORE, "PTCClans Plugin detenido correctamente");
    }

    public static void logClanCreated(String clanTag, String clanName, String creator) {
        log(LogLevel.INFO, LogCategory.CLAN, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.CLAN, "CLAN CREADO");
        log(LogLevel.INFO, LogCategory.CLAN, "Tag:", clanTag);
        log(LogLevel.INFO, LogCategory.CLAN, "Nombre:", clanName);
        log(LogLevel.INFO, LogCategory.CLAN, "Creador:", creator);
        log(LogLevel.INFO, LogCategory.CLAN, "═══════════════════════════════════");
    }

    public static void logClanDeleted(String clanTag, String deletedBy) {
        log(LogLevel.INFO, LogCategory.CLAN, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.CLAN, "CLAN ELIMINADO");
        log(LogLevel.INFO, LogCategory.CLAN, "Tag:", clanTag);
        log(LogLevel.INFO, LogCategory.CLAN, "Eliminado por:", deletedBy);
        log(LogLevel.INFO, LogCategory.CLAN, "═══════════════════════════════════");
    }

    public static void logWarCreated(String challengerTag, String challengedTag, String arenaKey) {
        log(LogLevel.INFO, LogCategory.WAR, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.WAR, "GUERRA PROGRAMADA");
        log(LogLevel.INFO, LogCategory.WAR, "Desafiante:", challengerTag);
        log(LogLevel.INFO, LogCategory.WAR, "Desafiado:", challengedTag);
        log(LogLevel.INFO, LogCategory.WAR, "Arena:", arenaKey);
        log(LogLevel.INFO, LogCategory.WAR, "═══════════════════════════════════");
    }

    public static void logWarStarted(String blueTag, String redTag, String server) {
        log(LogLevel.INFO, LogCategory.WAR, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.WAR, "GUERRA INICIADA");
        log(LogLevel.INFO, LogCategory.WAR, "Azul:", blueTag);
        log(LogLevel.INFO, LogCategory.WAR, "Rojo:", redTag);
        log(LogLevel.INFO, LogCategory.WAR, "Servidor:", server);
        log(LogLevel.INFO, LogCategory.WAR, "═══════════════════════════════════");
    }

    public static void logWarEnded(String winnerTag, String loserTag) {
        log(LogLevel.INFO, LogCategory.WAR, "═══════════════════════════════════");
        log(LogLevel.INFO, LogCategory.WAR, "GUERRA FINALIZADA");
        log(LogLevel.INFO, LogCategory.WAR, "Ganador:", winnerTag);
        log(LogLevel.INFO, LogCategory.WAR, "Perdedor:", loserTag);
        log(LogLevel.INFO, LogCategory.WAR, "═══════════════════════════════════");
    }

    public static void logDatabaseError(String operation, String errorMessage) {
        error(LogCategory.DATABASE, "Error en operación:", operation, "-", errorMessage);
    }

    public static void logDatabaseSuccess(String operation) {
        debug(LogCategory.DATABASE, "Operación exitosa:", operation);
    }

    public static void logAPICall(String endpoint, String method) {
        debug(LogCategory.API, "API Call:", method, endpoint);
    }

    public static void logAPIError(String endpoint, String error) {
        error(LogCategory.API, "API Error:", endpoint, "-", error);
    }

    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }
}