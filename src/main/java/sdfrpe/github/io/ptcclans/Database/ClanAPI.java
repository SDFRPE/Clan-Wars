package sdfrpe.github.io.ptcclans.Database;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ClanAPI {
    private final String USER_AGENT = "Mozilla/5.0";
    private final String BASE_URL;

    public ClanAPI() {
        PTCClans plugin = PTCClans.getInstance();
        this.BASE_URL = plugin.getSettings().getDatabaseURL();
    }

    public JsonObject getClan(String tag) {
        try {
            URL obj = new URL(BASE_URL + "clan/" + tag.toUpperCase());
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para clan:", tag);
                return root.getAsJsonObject();
            } else if (responseCode == 404) {
                ClanLogSystem.debug(LogCategory.API, "Clan no encontrado:", tag);
                return null;
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para clan:", tag, "-", e.getMessage());
            return null;
        }
    }

    public JsonObject saveClan(String tag, String jsonObject) {
        try {
            URL obj = new URL(BASE_URL + "clan/" + tag.toUpperCase());
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);

            DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
            dataOutputStream.writeBytes(jsonObject);
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "POST exitoso para clan:", tag);
                return root.getAsJsonObject();
            } else {
                ClanLogSystem.debug(LogCategory.API, "POST falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en POST para clan:", tag, "-", e.getMessage());
            return null;
        }
    }

    public boolean deleteClan(String tag) {
        try {
            URL obj = new URL(BASE_URL + "clan/" + tag.toUpperCase());
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("DELETE");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int responseCode = con.getResponseCode();
            ClanLogSystem.debug(LogCategory.API, "DELETE clan:", tag, "- Código:", String.valueOf(responseCode));
            return responseCode == 200;
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en DELETE para clan:", tag, "-", e.getMessage());
            return false;
        }
    }

    public JsonObject getAllClans() {
        try {
            URL obj = new URL(BASE_URL + "clan/all");
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para todos los clanes");
                return root.getAsJsonObject();
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET all clans falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para todos los clanes:", e.getMessage());
            return null;
        }
    }

    public JsonObject getPlayerClan(String playerUuid) {
        try {
            URL obj = new URL(BASE_URL + "clan/player/" + playerUuid);
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para clan de jugador:", playerUuid);
                return root.getAsJsonObject();
            } else if (responseCode == 404) {
                ClanLogSystem.debug(LogCategory.API, "Jugador sin clan:", playerUuid);
                return null;
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET player clan falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para clan de jugador:", playerUuid, "-", e.getMessage());
            return null;
        }
    }

    public JsonObject getWar(String warId) {
        try {
            URL obj = new URL(BASE_URL + "war/" + warId);
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para guerra:", warId);
                return root.getAsJsonObject();
            } else if (responseCode == 404) {
                ClanLogSystem.debug(LogCategory.API, "Guerra no encontrada:", warId);
                return null;
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET war falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para guerra:", warId, "-", e.getMessage());
            return null;
        }
    }

    public JsonObject saveWar(String warId, String jsonObject) {
        try {
            URL obj = new URL(BASE_URL + "war/" + warId);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);

            DataOutputStream dataOutputStream = new DataOutputStream(con.getOutputStream());
            dataOutputStream.writeBytes(jsonObject);
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String jsonResponse = in.readLine();
                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "POST exitoso para guerra:", warId);
                return root.getAsJsonObject();
            } else {
                ClanLogSystem.debug(LogCategory.API, "POST war falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en POST para guerra:", warId, "-", e.getMessage());
            return null;
        }
    }

    public boolean deleteWar(String warId) {
        try {
            URL obj = new URL(BASE_URL + "war/" + warId);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("DELETE");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int responseCode = con.getResponseCode();
            ClanLogSystem.debug(LogCategory.API, "DELETE war:", warId, "- Código:", String.valueOf(responseCode));
            return responseCode == 200;
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en DELETE para guerra:", warId, "-", e.getMessage());
            return false;
        }
    }

    public JsonObject getAllWars() {
        try {
            URL obj = new URL(BASE_URL + "war/all");
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para todas las guerras");
                return root.getAsJsonObject();
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET all wars falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para todas las guerras:", e.getMessage());
            return null;
        }
    }

    public JsonObject getClanWars(String clanTag) {
        try {
            URL obj = new URL(BASE_URL + "war/clan/" + clanTag.toUpperCase());
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para guerras del clan:", clanTag);
                return root.getAsJsonObject();
            } else if (responseCode == 404) {
                ClanLogSystem.debug(LogCategory.API, "Clan sin guerras:", clanTag);
                return null;
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET clan wars falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para guerras del clan:", clanTag, "-", e.getMessage());
            return null;
        }
    }

    public JsonObject getActiveWars() {
        try {
            URL obj = new URL(BASE_URL + "war/active");
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para guerras activas (sin arena)");
                return root.getAsJsonObject();
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET active wars falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para guerras activas:", e.getMessage());
            return null;
        }
    }

    public JsonObject getAllActiveWars() {
        try {
            URL obj = new URL(BASE_URL + "war/active/all");
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
                JsonElement root = jsonParser.parse(jsonResponse);
                in.close();
                ClanLogSystem.debug(LogCategory.API, "GET exitoso para TODAS las guerras activas (con/sin arena)");
                return root.getAsJsonObject();
            } else {
                ClanLogSystem.debug(LogCategory.API, "GET all active wars falló con código:", String.valueOf(responseCode));
                return null;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error en GET para todas las guerras activas:", e.getMessage());
            return null;
        }
    }
}