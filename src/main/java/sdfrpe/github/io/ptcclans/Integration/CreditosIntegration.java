package sdfrpe.github.io.ptcclans.Integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;
import sdfrpe.github.io.creditos.api.CreditosAPI;

import java.util.UUID;

public class CreditosIntegration {
    private static CreditosAPI creditosAPI;
    private static boolean creditosAvailable = false;

    public static void initialize() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Creditos") != null) {
                creditosAPI = CreditosAPI.getInstance();
                if (creditosAPI != null) {
                    creditosAvailable = true;
                    ClanLogger.info("Integración con Creditos activada - Sistema de Créditos");
                } else {
                    ClanLogger.warn("Plugin Creditos detectado pero API no disponible");
                }
            } else {
                ClanLogger.warn("Plugin Creditos no detectado - Sistema de Créditos no disponible");
            }
        } catch (Exception e) {
            creditosAvailable = false;
            ClanLogger.error("Error inicializando integración con Creditos:", e.getMessage());
        }
    }

    public static boolean isCreditosAvailable() {
        return creditosAvailable;
    }

    public static long getPlayerCredits(Player player) {
        if (!creditosAvailable || creditosAPI == null) {
            ClanLogger.warn("Sistema de Créditos no disponible");
            return 0;
        }
        try {
            return creditosAPI.getBalance(player.getUniqueId());
        } catch (Exception e) {
            ClanLogger.error("Error obteniendo créditos de", player.getName() + ":", e.getMessage());
            return 0;
        }
    }

    public static boolean hasCredits(Player player, long amount) {
        if (!creditosAvailable || creditosAPI == null) {
            return false;
        }
        try {
            return creditosAPI.hasEnough(player.getUniqueId(), amount);
        } catch (Exception e) {
            ClanLogger.error("Error verificando créditos de", player.getName() + ":", e.getMessage());
            return false;
        }
    }

    public static boolean removeCredits(Player player, long amount, String reason) {
        if (!creditosAvailable || creditosAPI == null) {
            ClanLogger.warn("Sistema de Créditos no disponible");
            return false;
        }

        try {
            UUID playerUuid = player.getUniqueId();

            if (!creditosAPI.hasEnough(playerUuid, amount)) {
                ClanLogger.debug("Jugador no tiene suficientes créditos:", player.getName(), "- Requiere:", String.valueOf(amount), "- Tiene:", String.valueOf(creditosAPI.getBalance(playerUuid)));
                return false;
            }

            boolean result = creditosAPI.removeCredits(playerUuid, amount, "PTCClans", reason);

            if (result) {
                ClanLogger.info("Créditos descontados de", player.getName() + ":", String.valueOf(amount), "créditos - Razón:", reason);
            } else {
                ClanLogger.error("Error al descontar créditos de", player.getName());
            }

            return result;
        } catch (Exception e) {
            ClanLogger.error("Error removiendo créditos de", player.getName() + ":", e.getMessage());
            return false;
        }
    }

    public static boolean addCredits(Player player, long amount, String reason) {
        if (!creditosAvailable || creditosAPI == null) {
            ClanLogger.warn("Sistema de Créditos no disponible");
            return false;
        }

        try {
            boolean result = creditosAPI.addCredits(player.getUniqueId(), amount, "PTCClans", reason);

            if (result) {
                ClanLogger.info("Créditos agregados a", player.getName() + ":", String.valueOf(amount), "créditos - Razón:", reason);
            } else {
                ClanLogger.error("Error al agregar créditos a", player.getName());
            }

            return result;
        } catch (Exception e) {
            ClanLogger.error("Error agregando créditos a", player.getName() + ":", e.getMessage());
            return false;
        }
    }

    public static String formatCredits(long amount) {
        if (creditosAPI != null) {
            try {
                return creditosAPI.formatCredits(amount);
            } catch (Exception e) {
                return String.valueOf(amount);
            }
        }
        return String.valueOf(amount);
    }
}