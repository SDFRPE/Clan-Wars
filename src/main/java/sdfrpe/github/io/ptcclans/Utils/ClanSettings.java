package sdfrpe.github.io.ptcclans.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClanSettings {
    private String databaseURL = "http://127.0.0.1:25611/ptc/";
    private boolean lobbyMode = false;
    private boolean clanWarMode = false;
    private String defaultWarServer = "SV_PTC4";
    private List<String> warServers = Arrays.asList("WAR_SERVER_1", "WAR_SERVER_2");
    private int warDurationMinutes = 60;
    private boolean productionMode = false;
    private int clanCreationCost = 10000;
    private int clanDeletionCost = 2000;
    private int invitationCost = 3000;
    private int maxClanMembers = 10;
    private int maxSimultaneousWars = 2;
    private int minTagLength = 2;
    private int maxTagLength = 5;
    private int minNameLength = 3;
    private int maxNameLength = 16;
    private LoggingSettings logging = new LoggingSettings();

    private transient PTCClans plugin;
    private transient File settingsFile;
    private transient Gson gson;

    public void initialize(PTCClans plugin) {
        this.plugin = plugin;
        this.settingsFile = new File(plugin.getDataFolder(), "settings.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void load() {
        if (!settingsFile.exists()) {
            save();
            ClanLogSystem.info(LogCategory.CORE, "Archivo de configuración creado con valores por defecto");
            ClanLogSystem.info(LogCategory.CORE, "Database URL configurada:", databaseURL);
            ClanLogSystem.info(LogCategory.CORE, "Servidor de guerra por defecto:", defaultWarServer);
            ClanLogSystem.info(LogCategory.CORE, "Servidores de guerra disponibles:", warServers.toString());
            ClanLogSystem.info(LogCategory.CORE, "Duración de guerras:", warDurationMinutes + " minutos");
            ClanLogSystem.info(LogCategory.CORE, "Modo Lobby:", lobbyMode ? "ACTIVADO" : "DESACTIVADO");
            ClanLogSystem.info(LogCategory.CORE, "Modo Clan War:", clanWarMode ? "ACTIVADO" : "DESACTIVADO");
            ClanLogSystem.info(LogCategory.CORE, "Modo Producción:", productionMode ? "ACTIVADO" : "DESACTIVADO");
            return;
        }

        try (FileReader reader = new FileReader(settingsFile)) {
            ClanSettings loaded = gson.fromJson(reader, ClanSettings.class);
            this.databaseURL = loaded.databaseURL;
            this.lobbyMode = loaded.lobbyMode;
            this.clanWarMode = loaded.clanWarMode;
            this.defaultWarServer = loaded.defaultWarServer != null ? loaded.defaultWarServer : "SV_PTC4";
            this.warServers = loaded.warServers != null && !loaded.warServers.isEmpty()
                    ? loaded.warServers
                    : Arrays.asList("WAR_SERVER_1", "WAR_SERVER_2");
            this.warDurationMinutes = loaded.warDurationMinutes > 0 ? loaded.warDurationMinutes : 60;
            this.productionMode = loaded.productionMode;
            this.clanCreationCost = loaded.clanCreationCost;
            this.clanDeletionCost = loaded.clanDeletionCost;
            this.invitationCost = loaded.invitationCost != 0 ? loaded.invitationCost : 3000;
            this.maxClanMembers = loaded.maxClanMembers;
            this.maxSimultaneousWars = loaded.maxSimultaneousWars;
            this.minTagLength = loaded.minTagLength;
            this.maxTagLength = loaded.maxTagLength;
            this.minNameLength = loaded.minNameLength;
            this.maxNameLength = loaded.maxNameLength;
            this.logging = loaded.logging != null ? loaded.logging : new LoggingSettings();

            initializeLogging();

            ClanLogSystem.info(LogCategory.CORE, "Configuración cargada correctamente");
            ClanLogSystem.info(LogCategory.CORE, "Database URL:", databaseURL);
            ClanLogSystem.info(LogCategory.CORE, "Servidor de guerra por defecto:", defaultWarServer);
            ClanLogSystem.info(LogCategory.CORE, "Servidores de guerra disponibles:", warServers.toString());
            ClanLogSystem.info(LogCategory.CORE, "Duración de guerras:", warDurationMinutes + " minutos");
            ClanLogSystem.info(LogCategory.CORE, "Modo Lobby:", lobbyMode ? "ACTIVADO" : "DESACTIVADO");
            ClanLogSystem.info(LogCategory.CORE, "Modo Clan War:", clanWarMode ? "ACTIVADO" : "DESACTIVADO");
            ClanLogSystem.info(LogCategory.CORE, "Modo Producción:", productionMode ? "ACTIVADO (sin /cwtest)" : "DESACTIVADO (dev mode)");

            if (lobbyMode) {
                ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
                ClanLogSystem.info(LogCategory.CORE, "MODO LOBBY ACTIVO");
                ClanLogSystem.info(LogCategory.CORE, "- Gestión completa de clanes");
                ClanLogSystem.info(LogCategory.CORE, "- Programación de guerras: HABILITADA");
                ClanLogSystem.info(LogCategory.CORE, "- Servidores de guerra: " + warServers.size());
                for (int i = 0; i < warServers.size(); i++) {
                    ClanLogSystem.info(LogCategory.CORE, "  Arena " + (i + 1) + ": " + warServers.get(i));
                }
                ClanLogSystem.info(LogCategory.CORE, "- Duración de guerras: " + warDurationMinutes + " minutos");
                ClanLogSystem.info(LogCategory.CORE, "- Costo crear clan: " + clanCreationCost + " Coins");
                ClanLogSystem.info(LogCategory.CORE, "- Costo eliminar clan: " + clanDeletionCost + " Coins");
                ClanLogSystem.info(LogCategory.CORE, "- Costo invitar jugador: " + invitationCost + " Coins");
                ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
            }

            if (clanWarMode) {
                ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
                ClanLogSystem.info(LogCategory.CORE, "MODO CLAN WAR ACTIVO");
                ClanLogSystem.info(LogCategory.CORE, "- Arena dedicada a enfrentamientos");
                ClanLogSystem.info(LogCategory.CORE, "- Duración de guerras: " + warDurationMinutes + " minutos");
                ClanLogSystem.info(LogCategory.CORE, "- Equipos: AZUL vs ROJO");
                ClanLogSystem.info(LogCategory.CORE, "- Comandos de guerra: DESHABILITADOS");
                ClanLogSystem.info(LogCategory.CORE, "- Carga automática desde API");
                ClanLogSystem.info(LogCategory.CORE, "- Solo comandos de información disponibles");
                ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
            }

            if (!lobbyMode && !clanWarMode) {
                ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
                ClanLogSystem.info(LogCategory.CORE, "MODO NORMAL ACTIVO");
                ClanLogSystem.info(LogCategory.CORE, "- Solo comandos de información disponibles");
                ClanLogSystem.info(LogCategory.CORE, "- Gestión de clanes: DESHABILITADA");
                ClanLogSystem.info(LogCategory.CORE, "- Costo crear clan: " + clanCreationCost + " Coins");
                ClanLogSystem.info(LogCategory.CORE, "- Costo eliminar clan: " + clanDeletionCost + " Coins");
                ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
            }
        } catch (IOException e) {
            ClanLogSystem.error(LogCategory.CORE, "Error cargando configuración:", e.getMessage());
        }
    }

    public void initializeLogging() {
        ClanLogSystem.setDebugMode(this.logging.isDebugMode());

        for (String category : this.logging.getEnabledCategories()) {
            try {
                ClanLogSystem.LogCategory logCategory = ClanLogSystem.LogCategory.valueOf(category.toUpperCase());
                ClanLogSystem.enableDebugCategory(logCategory);
            } catch (IllegalArgumentException e) {
                ClanLogSystem.warn(LogCategory.CORE, "Categoría de log desconocida:", category);
            }
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(settingsFile)) {
            gson.toJson(this, writer);
            ClanLogSystem.debug(LogCategory.CORE, "Configuración guardada");
        } catch (IOException e) {
            ClanLogSystem.error(LogCategory.CORE, "Error guardando configuración:", e.getMessage());
        }
    }

    public static class LoggingSettings {
        private boolean debugMode = false;
        private List<String> enabledCategories = new ArrayList<>();

        public boolean isDebugMode() {
            return debugMode;
        }

        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }

        public List<String> getEnabledCategories() {
            if (this.enabledCategories == null) {
                this.enabledCategories = new ArrayList<>();
            }
            return enabledCategories;
        }

        public void setEnabledCategories(List<String> enabledCategories) {
            this.enabledCategories = enabledCategories;
        }
    }

    public String getDatabaseURL() {
        return databaseURL;
    }

    public void setDatabaseURL(String databaseURL) {
        this.databaseURL = databaseURL;
    }

    public boolean isLobbyMode() {
        return lobbyMode;
    }

    public void setLobbyMode(boolean lobbyMode) {
        this.lobbyMode = lobbyMode;
    }

    public boolean isClanWarMode() {
        return clanWarMode;
    }

    public void setClanWarMode(boolean clanWarMode) {
        this.clanWarMode = clanWarMode;
    }

    public String getDefaultWarServer() {
        return defaultWarServer;
    }

    public void setDefaultWarServer(String defaultWarServer) {
        this.defaultWarServer = defaultWarServer;
    }

    public List<String> getWarServers() {
        return warServers;
    }

    public void setWarServers(List<String> warServers) {
        this.warServers = warServers;
    }

    public int getWarDurationMinutes() {
        return warDurationMinutes;
    }

    public void setWarDurationMinutes(int warDurationMinutes) {
        this.warDurationMinutes = warDurationMinutes;
    }

    public boolean isProductionMode() {
        return productionMode;
    }

    public void setProductionMode(boolean productionMode) {
        this.productionMode = productionMode;
    }

    public boolean canExecuteWarCommands() {
        return lobbyMode && !clanWarMode;
    }

    public boolean canManageClans() {
        return lobbyMode;
    }

    public boolean isRequireCoins() {
        return true;
    }

    public int getClanCreationCost() {
        return clanCreationCost;
    }

    public int getClanDeletionCost() {
        return clanDeletionCost;
    }

    public int getInvitationCost() {
        return invitationCost;
    }

    public int getMaxClanMembers() {
        return maxClanMembers;
    }

    public int getMaxSimultaneousWars() {
        return maxSimultaneousWars;
    }

    public int getMinTagLength() {
        return minTagLength;
    }

    public int getMaxTagLength() {
        return maxTagLength;
    }

    public int getMinNameLength() {
        return minNameLength;
    }

    public int getMaxNameLength() {
        return maxNameLength;
    }

    public LoggingSettings getLogging() {
        if (this.logging == null) {
            this.logging = new LoggingSettings();
        }
        return logging;
    }

    public void setLogging(LoggingSettings logging) {
        this.logging = logging;
    }
}