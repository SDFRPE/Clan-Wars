package sdfrpe.github.io.ptcclans.Integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class PTCIntegration {
    private static boolean ptcAvailable = false;
    private static Object ptcInstance = null;
    private static Class<?> gamePlayerClass = null;
    private static Method getCoinsMethod = null;
    private static Method setCoinsMethod = null;
    private static final String USER_AGENT = "Mozilla/5.0";

    public static void initialize() {
        try {
            if (Bukkit.getPluginManager().getPlugin("PTC") != null) {
                Class<?> ptcClass = Class.forName("sdfrpe.github.io.ptc.PTC");
                Method getInstanceMethod = ptcClass.getMethod("getInstance");
                ptcInstance = getInstanceMethod.invoke(null);

                gamePlayerClass = Class.forName("sdfrpe.github.io.ptc.Player.GamePlayer");
                getCoinsMethod = gamePlayerClass.getMethod("getCoins");
                setCoinsMethod = gamePlayerClass.getMethod("setCoins", int.class);

                ptcAvailable = true;
                ClanLogger.info("Integración con PTC activada - Sistema de Coins (Reflection)");
            } else {
                ClanLogger.info("PTC no detectado - Sistema de Coins vía API REST");
            }
        } catch (Exception e) {
            ptcAvailable = false;
            ClanLogger.warn("No se pudo integrar con PTC via reflection:", e.getMessage());
            ClanLogger.info("Usando API REST para sistema de Coins");
        }
    }

    public static boolean isPTCAvailable() {
        return ptcAvailable;
    }

    public static int getPlayerCoins(Player player) {
        if (ptcAvailable) {
            int coins = getPlayerCoinsReflection(player);
            if (coins >= 0) {
                return coins;
            }
            ClanLogger.warn("Reflection falló, usando API fallback para GET coins");
        }
        return getPlayerCoinsAPI(player.getUniqueId());
    }

    public static boolean setPlayerCoins(Player player, int coins) {
        if (ptcAvailable) {
            if (setPlayerCoinsReflection(player, coins)) {
                return true;
            }
            ClanLogger.warn("Reflection falló, usando API fallback para SET coins");
        }
        return setPlayerCoinsAPI(player.getUniqueId(), coins);
    }

    public static boolean hasCoins(Player player, int amount) {
        return getPlayerCoins(player) >= amount;
    }

    public static boolean removeCoins(Player player, int amount) {
        int currentCoins = getPlayerCoins(player);
        if (currentCoins < amount) {
            ClanLogger.debug("Jugador no tiene suficientes coins:", player.getName(), "- Requiere:", String.valueOf(amount), "- Tiene:", String.valueOf(currentCoins));
            return false;
        }
        boolean result = setPlayerCoins(player, currentCoins - amount);
        if (result) {
            ClanLogger.info("Coins descontados de", player.getName() + ":", String.valueOf(amount), "coins - Saldo:", String.valueOf(currentCoins - amount));
        }
        return result;
    }

    public static boolean addCoins(Player player, int amount) {
        int currentCoins = getPlayerCoins(player);
        boolean result = setPlayerCoins(player, currentCoins + amount);
        if (result) {
            ClanLogger.info("Coins agregados a", player.getName() + ":", String.valueOf(amount), "coins - Saldo:", String.valueOf(currentCoins + amount));
        }
        return result;
    }

    public static String getClanDisplayName(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }

        Clan clan = ClanManager.getInstance().getPlayerClan(playerUuid);
        if (clan == null) {
            return null;
        }

        return clan.getFormattedName();
    }

    public static String getClanDisplayName(Player player) {
        if (player == null) {
            return null;
        }
        return getClanDisplayName(player.getUniqueId());
    }

    public static String getClanTag(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }

        Clan clan = ClanManager.getInstance().getPlayerClan(playerUuid);
        if (clan == null) {
            return null;
        }

        return clan.getFormattedTag();
    }

    public static String getClanTag(Player player) {
        if (player == null) {
            return null;
        }
        return getClanTag(player.getUniqueId());
    }

    public static boolean hasClan(UUID playerUuid) {
        return getClanDisplayName(playerUuid) != null;
    }

    public static boolean hasClan(Player player) {
        if (player == null) {
            return false;
        }
        return hasClan(player.getUniqueId());
    }

    private static int getPlayerCoinsReflection(Player player) {
        try {
            Object gameManager = ptcInstance.getClass().getMethod("getGameManager").invoke(ptcInstance);
            Object playerManager = gameManager.getClass().getMethod("getPlayerManager").invoke(gameManager);
            Object gamePlayer = playerManager.getClass().getMethod("getPlayer", UUID.class).invoke(playerManager, player.getUniqueId());

            if (gamePlayer == null) return -1;

            return (int) getCoinsMethod.invoke(gamePlayer);
        } catch (Exception e) {
            ClanLogger.error("Error obteniendo coins (reflection):", e.getMessage());
            return -1;
        }
    }

    private static boolean setPlayerCoinsReflection(Player player, int coins) {
        try {
            Object gameManager = ptcInstance.getClass().getMethod("getGameManager").invoke(ptcInstance);
            Object playerManager = gameManager.getClass().getMethod("getPlayerManager").invoke(gameManager);
            Object gamePlayer = playerManager.getClass().getMethod("getPlayer", UUID.class).invoke(playerManager, player.getUniqueId());

            if (gamePlayer == null) return false;

            setCoinsMethod.invoke(gamePlayer, coins);
            return true;
        } catch (Exception e) {
            ClanLogger.error("Error estableciendo coins (reflection):", e.getMessage());
            return false;
        }
    }

    private static int getPlayerCoinsAPI(UUID playerUuid) {
        try {
            String baseUrl = PTCClans.getInstance().getSettings().getDatabaseURL();
            URL obj = new URL(baseUrl + "player/" + playerUuid.toString() + "/coins");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonObject root = jsonParser.parse(jsonResponse).getAsJsonObject();
                in.close();

                if (!root.get("error").getAsBoolean()) {
                    JsonObject data = root.getAsJsonObject("data");
                    int coins = data.get("coins").getAsInt();
                    ClanLogger.debug("GET coins API exitoso:", playerUuid.toString(), "- Coins:", String.valueOf(coins));
                    return coins;
                }
            } else if (responseCode == 403) {
                ClanLogger.debug("Jugador sin registro:", playerUuid.toString(), "- Coins: 0");
                return 0;
            }

            ClanLogger.debug("GET coins API falló con código:", String.valueOf(responseCode));
            return 0;
        } catch (Exception e) {
            ClanLogger.error("Error obteniendo coins desde API:", e.getMessage());
            return 0;
        }
    }

    private static boolean setPlayerCoinsAPI(UUID playerUuid, int coins) {
        try {
            String baseUrl = PTCClans.getInstance().getSettings().getDatabaseURL();
            URL obj = new URL(baseUrl + "player/" + playerUuid.toString() + "/coins/update");
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);

            String jsonInput = "{\"coins\":" + coins + "}";
            DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
            dataOutputStream.writeBytes(jsonInput);
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonObject root = jsonParser.parse(jsonResponse).getAsJsonObject();
                in.close();

                if (!root.get("error").getAsBoolean()) {
                    ClanLogger.debug("POST coins API exitoso:", playerUuid.toString(), "- Nuevos coins:", String.valueOf(coins));
                    return true;
                }
            }

            ClanLogger.debug("POST coins API falló con código:", String.valueOf(responseCode));
            return false;
        } catch (Exception e) {
            ClanLogger.error("Error estableciendo coins en API:", e.getMessage());
            return false;
        }
    }
}