package sdfrpe.github.io.ptcclans.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Database.ClanAPI;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanWar;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;
import sdfrpe.github.io.ptcclans.Utils.EloCalculator;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClanWarManager {
    private static ClanWarManager instance;
    private final PTCClans plugin;
    private final Gson gson;
    private final ClanAPI clanAPI;
    private final Map<UUID, ClanWar> wars;
    private ClanWar activeWar;
    private static final long OLD_WAR_CLEANUP_THRESHOLD = 30L * 24 * 60 * 60 * 1000;

    private int acceptDeadlineMinutes = 10;
    private int warDurationMinutes = 60;
    private File settingsFile;
    private final SimpleDateFormat dateFormatter;

    private ClanWarManager(PTCClans plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.clanAPI = new ClanAPI();
        this.wars = new HashMap<UUID, ClanWar>();
        this.activeWar = null;
        this.settingsFile = new File(plugin.getDataFolder(), "war-settings.json");
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        this.dateFormatter.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        loadSettings();
    }

    public static ClanWarManager getInstance() {
        return instance;
    }

    public static void initialize(PTCClans plugin) {
        if (instance == null) {
            instance = new ClanWarManager(plugin);
            instance.loadAllWars();
            instance.cleanExpiredChallenges();
            instance.cleanExpiredActiveWars();
            ClanLogSystem.info(LogCategory.WAR, "Limpieza inicial de guerras expiradas completada");
        }
    }

    private void loadSettings() {
        if (settingsFile.exists()) {
            try (Reader reader = new FileReader(settingsFile)) {
                Map<String, Object> settings = gson.fromJson(reader, Map.class);
                if (settings != null) {
                    acceptDeadlineMinutes = ((Number) settings.getOrDefault("acceptDeadlineMinutes", 10)).intValue();
                    warDurationMinutes = ((Number) settings.getOrDefault("warDurationMinutes", 60)).intValue();

                    if (acceptDeadlineMinutes < 1) acceptDeadlineMinutes = 10;
                    if (warDurationMinutes < 1) warDurationMinutes = 60;

                    ClanLogSystem.info(LogCategory.WAR, "War settings cargados - Timeout:", acceptDeadlineMinutes + "min", "Duration:", warDurationMinutes + "min");
                }
            } catch (Exception e) {
                ClanLogSystem.error(LogCategory.WAR, "Error cargando war settings:", e.getMessage());
            }
        } else {
            saveSettings();
        }
    }

    private void saveSettings() {
        try (Writer writer = new FileWriter(settingsFile)) {
            Map<String, Integer> settings = new HashMap<>();
            settings.put("acceptDeadlineMinutes", acceptDeadlineMinutes);
            settings.put("warDurationMinutes", warDurationMinutes);
            gson.toJson(settings, writer);
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error guardando war settings:", e.getMessage());
        }
    }

    public int forceCleanAllExpiredWars() {
        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.WAR, "LIMPIEZA FORZADA INICIADA");
        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");

        refreshAllWarsFromAPI();

        List<ClanWar> toClean = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (war.isAccepted() && !war.isFinished() && war.hasExpiredByTime()) {
                toClean.add(war);
            }
        }

        int cleaned = 0;
        for (ClanWar war : toClean) {
            ClanLogSystem.info(LogCategory.WAR, "Finalizando guerra expirada:", war.getChallengerClanTag(), "vs", war.getChallengedClanTag(), "- ID:", war.getWarId().toString().substring(0, 8));

            war.setFinished(true);
            war.setWinnerClanTag(null);
            saveWar(war);
            cleaned++;
        }

        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.WAR, "LIMPIEZA COMPLETADA:", String.valueOf(cleaned), "guerras finalizadas");
        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");

        return cleaned;
    }

    public int getAcceptDeadlineMinutes() {
        return acceptDeadlineMinutes;
    }

    public int getWarDurationMinutes() {
        return warDurationMinutes;
    }

    public void setAcceptDeadlineMinutes(int minutes) {
        if (minutes < 1) throw new IllegalArgumentException("Accept deadline debe ser >= 1 minuto");
        this.acceptDeadlineMinutes = minutes;
        saveSettings();
    }

    public void setWarDurationMinutes(int minutes) {
        if (minutes < 1) throw new IllegalArgumentException("War duration debe ser >= 1 minuto");
        this.warDurationMinutes = minutes;
        saveSettings();
    }

    public void loadAllWars() {
        try {
            JsonObject response = clanAPI.getAllWars();

            if (response == null) {
                ClanLogSystem.warn(LogCategory.API, "No se pudo conectar a la API para cargar guerras");
                return;
            }

            if (!response.get("error").getAsBoolean()) {
                JsonArray dataArray = response.getAsJsonArray("data");
                int migratedCount = 0;
                int validCount = 0;

                for (int i = 0; i < dataArray.size(); i++) {
                    JsonObject warJson = dataArray.get(i).getAsJsonObject();

                    if (!warJson.has("creationTime") || !warJson.has("acceptDeadline")) {
                        String warId = warJson.get("warId").getAsString();
                        ClanLogSystem.warn(LogCategory.WAR, "Guerra antigua detectada:", warId, "- eliminando");

                        boolean deleted = clanAPI.deleteWar(warId);
                        if (deleted) {
                            migratedCount++;
                        }
                        continue;
                    }

                    ClanWar war = gson.fromJson(warJson.toString(), ClanWar.class);
                    wars.put(war.getWarId(), war);
                    validCount++;
                }

                if (migratedCount > 0) {
                    ClanLogSystem.info(LogCategory.WAR, "Migración - Antiguas eliminadas:", String.valueOf(migratedCount), "Válidas:", String.valueOf(validCount));
                } else {
                    ClanLogSystem.info(LogCategory.WAR, "Guerras cargadas desde API:", String.valueOf(validCount));
                }
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error cargando guerras desde API:", e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveWar(ClanWar war) {
        try {
            ClanLogSystem.debug(LogCategory.WAR, "═══════════════════════════════════");
            ClanLogSystem.debug(LogCategory.WAR, "saveWar() LLAMADO");
            ClanLogSystem.debug(LogCategory.WAR, "War ID:", war.getWarId().toString().substring(0, 8));
            ClanLogSystem.debug(LogCategory.WAR, "Desafiante:", war.getChallengerClanTag());
            ClanLogSystem.debug(LogCategory.WAR, "Desafiado:", war.getChallengedClanTag());
            ClanLogSystem.debug(LogCategory.WAR, "Estado ACCEPTED:", String.valueOf(war.isAccepted()));
            ClanLogSystem.debug(LogCategory.WAR, "Estado FINISHED:", String.valueOf(war.isFinished()));
            ClanLogSystem.debug(LogCategory.WAR, "Arena Key:", war.getArenaKey() != null ? war.getArenaKey() : "null");

            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            ClanLogSystem.debug(LogCategory.WAR, "LLAMADO DESDE:");
            for (int i = 2; i < Math.min(stackTrace.length, 5); i++) {
                ClanLogSystem.debug(LogCategory.WAR, "  " + stackTrace[i].toString());
            }
            ClanLogSystem.debug(LogCategory.WAR, "═══════════════════════════════════");

            String warJson = gson.toJson(war);
            JsonObject response = clanAPI.saveWar(war.getWarId().toString(), warJson);

            if (response != null && !response.get("error").getAsBoolean()) {
                wars.put(war.getWarId(), war);
            } else {
                ClanLogSystem.error(LogCategory.API, "Error guardando guerra en API:", war.getWarId().toString());
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.DATABASE, "Error guardando guerra:", e.getMessage());
        }
    }

    public void cleanupOldWars() {
        long cutoffTime = System.currentTimeMillis() - OLD_WAR_CLEANUP_THRESHOLD;
        int removed = 0;

        Iterator<Map.Entry<UUID, ClanWar>> iterator = wars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ClanWar> entry = iterator.next();
            if (entry.getValue().isFinished() && entry.getValue().getScheduledTime() < cutoffTime) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            ClanLogSystem.info(LogCategory.WAR, "Guerras antiguas limpiadas:", String.valueOf(removed));
        }
    }

    public boolean hasTimeConflict(String clanTag, long scheduledTime) {
        long warDurationMs = warDurationMinutes * 60 * 1000L;
        long newWarStart = scheduledTime;
        long newWarEnd = scheduledTime + warDurationMs;

        for (ClanWar war : wars.values()) {
            if (war.isFinished() || !war.isAccepted()) {
                continue;
            }

            if (!war.isParticipant(clanTag)) {
                continue;
            }

            long existingWarStart = war.getScheduledTime();
            long existingWarEnd = existingWarStart + warDurationMs;

            boolean overlaps = (newWarStart < existingWarEnd) && (newWarEnd > existingWarStart);

            if (overlaps) {
                ClanLogSystem.warn(LogCategory.WAR, "Conflicto de horario detectado para clan", clanTag);
                ClanLogSystem.warn(LogCategory.WAR, "Nueva guerra:", dateFormatter.format(new Date(newWarStart)), "a", dateFormatter.format(new Date(newWarEnd)));
                ClanLogSystem.warn(LogCategory.WAR, "Guerra existente:", dateFormatter.format(new Date(existingWarStart)), "a", dateFormatter.format(new Date(existingWarEnd)));
                return true;
            }
        }

        try {
            JsonObject response = clanAPI.getClanWars(clanTag);
            if (response != null && !response.get("error").getAsBoolean()) {
                JsonArray dataArray = response.getAsJsonArray("data");
                for (int i = 0; i < dataArray.size(); i++) {
                    ClanWar war = gson.fromJson(dataArray.get(i).toString(), ClanWar.class);

                    if (war.isFinished() || !war.isAccepted()) {
                        continue;
                    }

                    if (wars.containsKey(war.getWarId())) {
                        continue;
                    }

                    long existingWarStart = war.getScheduledTime();
                    long existingWarEnd = existingWarStart + warDurationMs;

                    boolean overlaps = (newWarStart < existingWarEnd) && (newWarEnd > existingWarStart);

                    if (overlaps) {
                        wars.put(war.getWarId(), war);
                        ClanLogSystem.warn(LogCategory.WAR, "Conflicto de horario detectado (desde API) para clan", clanTag);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error verificando conflictos desde API:", e.getMessage());
        }

        return false;
    }

    public ClanWar createChallenge(String challengerTag, String challengedTag, long scheduledTime) {
        cleanExpiredActiveWars();

        if (hasTimeConflict(challengerTag, scheduledTime)) {
            ClanLogSystem.warn(LogCategory.WAR, "Desafío bloqueado: Clan", challengerTag, "tiene conflicto de horario en", dateFormatter.format(new Date(scheduledTime)));
            return null;
        }

        if (hasTimeConflict(challengedTag, scheduledTime)) {
            ClanLogSystem.warn(LogCategory.WAR, "Desafío bloqueado: Clan", challengedTag, "tiene conflicto de horario en", dateFormatter.format(new Date(scheduledTime)));
            return null;
        }

        int pendingCount = getPendingWars().size();
        int maxWars = plugin.getSettings().getMaxSimultaneousWars();

        if (pendingCount >= maxWars) {
            ClanLogSystem.warn(LogCategory.WAR, "═══════════════════════════════════");
            ClanLogSystem.warn(LogCategory.WAR, "DESAFÍO BLOQUEADO - LÍMITE ALCANZADO");
            ClanLogSystem.warn(LogCategory.WAR, "Guerras pendientes:", String.valueOf(pendingCount));
            ClanLogSystem.warn(LogCategory.WAR, "Límite máximo:", String.valueOf(maxWars));
            ClanLogSystem.warn(LogCategory.WAR, "═══════════════════════════════════");
            return null;
        }

        try {
            ClanWar war = new ClanWar(challengerTag, challengedTag);
            war.setScheduledTime(scheduledTime);

            String warJson = gson.toJson(war);
            JsonObject response = clanAPI.saveWar(war.getWarId().toString(), warJson);

            if (response != null && !response.get("error").getAsBoolean()) {
                wars.put(war.getWarId(), war);
                ClanLogSystem.logWarCreated(challengerTag, challengedTag, "Pendiente");
                ClanLogSystem.info(LogCategory.WAR, "Timeout:", acceptDeadlineMinutes + "min");
                return war;
            }
            return null;
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error creando desafío:", e.getMessage());
            return null;
        }
    }

    public boolean forceAcceptChallenge(String challengerTag, String challengedTag) {
        ClanLogSystem.debug(LogCategory.WAR, "forceAcceptChallenge() LLAMADO [BY TAGS]");
        ClanLogSystem.debug(LogCategory.WAR, "Desafiante:", challengerTag, "Desafiado:", challengedTag);

        for (ClanWar war : wars.values()) {
            if (!war.isFinished() && !war.isAccepted() && war.getChallengerClanTag().equalsIgnoreCase(challengerTag) && war.getChallengedClanTag().equalsIgnoreCase(challengedTag)) {

                war.setAccepted(true);
                saveWar(war);
                ClanLogSystem.info(LogCategory.WAR, "Guerra forzada:", challengerTag, "vs", challengedTag);
                return true;
            }
        }
        return false;
    }

    public List<ClanWar> getAllWars() {
        return new ArrayList<ClanWar>(wars.values());
    }

    public boolean acceptChallenge(UUID warId) {
        ClanLogSystem.debug(LogCategory.WAR, "acceptChallenge() LLAMADO - War ID:", warId.toString());

        ClanWar war = wars.get(warId);
        if (war == null) war = getWarFromAPI(warId);
        if (war == null) return false;
        if (war.isExpired()) {
            cancelChallenge(warId);
            return false;
        }
        if (war.isAccepted()) return false;

        war.setAccepted(true);
        saveWar(war);

        ClanLogSystem.info(LogCategory.WAR, "Guerra aceptada:", war.getChallengerClanTag(), "vs", war.getChallengedClanTag());
        return true;
    }

    public boolean cancelChallenge(UUID warId) {
        try {
            if (clanAPI.deleteWar(warId.toString())) {
                wars.remove(warId);
                return true;
            }
            return false;
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error cancelando:", e.getMessage());
            return false;
        }
    }

    public ClanWar createTestChallenge(String challengerTag, String challengedTag, int minutes) {
        try {
            ClanWar war = new ClanWar(challengerTag, challengedTag);
            war.setScheduledTime(System.currentTimeMillis() + (minutes * 60 * 1000));
            war.setTestWar(true);

            String warJson = gson.toJson(war);
            JsonObject response = clanAPI.saveWar(war.getWarId().toString(), warJson);

            if (response != null && !response.get("error").getAsBoolean()) {
                wars.put(war.getWarId(), war);
                ClanLogSystem.info(LogCategory.WAR, "Guerra de prueba creada:", war.getWarId().toString(), "- Inicia en", minutes + "min");
                return war;
            }
            return null;
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error creando test war:", e.getMessage());
            return null;
        }
    }

    public boolean forceAcceptChallenge(UUID warId) {
        ClanLogSystem.debug(LogCategory.WAR, "forceAcceptChallenge() LLAMADO [BY UUID]");
        ClanLogSystem.debug(LogCategory.WAR, "War ID:", warId.toString());

        ClanWar war = wars.get(warId);
        if (war == null) war = getWarFromAPI(warId);
        if (war == null) return false;

        war.setAccepted(true);
        saveWar(war);
        ClanLogSystem.info(LogCategory.WAR, "Guerra forzada:", war.getWarId().toString());
        return true;
    }

    public boolean forceAcceptChallenge(String warIdStr) {
        try {
            return forceAcceptChallenge(UUID.fromString(warIdStr));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public List<ClanWar> getTestWars() {
        List<ClanWar> testWars = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (war.isTestWar()) testWars.add(war);
        }
        testWars.sort((w1, w2) -> Long.compare(w1.getScheduledTime(), w2.getScheduledTime()));
        return testWars;
    }

    public int clearTestWars() {
        List<ClanWar> testWars = getTestWars();
        int count = 0;

        for (ClanWar war : testWars) {
            if (clanAPI.deleteWar(war.getWarId().toString())) {
                wars.remove(war.getWarId());
                count++;
            }
        }
        ClanLogSystem.info(LogCategory.WAR, "Test wars eliminadas:", String.valueOf(count));
        return count;
    }

    public ClanWar getWarById(String warIdStr) {
        try {
            UUID id = UUID.fromString(warIdStr);
            ClanWar war = wars.get(id);
            return war != null ? war : getWarFromAPI(id);
        } catch (IllegalArgumentException e) {
            for (ClanWar war : wars.values()) {
                if (war.getWarId().toString().startsWith(warIdStr)) return war;
            }
            return null;
        }
    }

    public void cleanExpiredChallenges() {
        List<ClanWar> expired = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (!war.isAccepted() && !war.isFinished() && war.isExpired()) {
                expired.add(war);
            }
        }

        if (!expired.isEmpty()) {
            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            ClanLogSystem.info(LogCategory.WAR, "LIMPIANDO DESAFÍOS EXPIRADOS");
            ClanLogSystem.info(LogCategory.WAR, "Total a limpiar:", String.valueOf(expired.size()));

            for (ClanWar war : expired) {
                ClanLogSystem.info(LogCategory.WAR, "Eliminando:", war.getChallengerClanTag(), "vs", war.getChallengedClanTag(), "- ID:", war.getWarId().toString().substring(0, 8));
                cancelChallenge(war.getWarId());
            }

            ClanLogSystem.info(LogCategory.WAR, "LIMPIEZA COMPLETADA");
            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
        } else {
            ClanLogSystem.debug(LogCategory.WAR, "No hay desafíos expirados para limpiar");
        }
    }

    public void cleanExpiredActiveWars() {
        List<ClanWar> expired = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (war.isAccepted() && !war.isFinished() && war.hasExpiredByTime()) {
                expired.add(war);
            }
        }

        if (!expired.isEmpty()) {
            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            ClanLogSystem.info(LogCategory.WAR, "LIMPIANDO GUERRAS ACTIVAS EXPIRADAS");
            ClanLogSystem.info(LogCategory.WAR, "Total a finalizar:", String.valueOf(expired.size()));

            for (ClanWar war : expired) {
                ClanLogSystem.info(LogCategory.WAR, "Finalizando:", war.getChallengerClanTag(), "vs", war.getChallengedClanTag(), "- ID:", war.getWarId().toString().substring(0, 8));
                war.setFinished(true);
                war.setWinnerClanTag(null);
                saveWar(war);
            }

            ClanLogSystem.info(LogCategory.WAR, "LIMPIEZA COMPLETADA");
            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
        } else {
            ClanLogSystem.debug(LogCategory.WAR, "No hay guerras activas expiradas para limpiar");
        }
    }

    public List<ClanWar> getExpiredChallenges() {
        List<ClanWar> expired = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (!war.isAccepted() && !war.isFinished() && war.isExpired()) {
                expired.add(war);
            }
        }
        return expired;
    }

    public void onWarStarted() {
        if (activeWar != null) {
            ClanLogSystem.logWarStarted(activeWar.getChallengerClanTag(), activeWar.getChallengedClanTag(), activeWar.getArenaKey() != null ? activeWar.getArenaKey() : "Unknown");
        }
    }

    public void onWarEnded(String winnerClanTag) {
        if (activeWar != null) {
            activeWar.setFinished(true);
            activeWar.setWinnerClanTag(winnerClanTag);
            saveWar(activeWar);

            String loserTag = winnerClanTag != null ? activeWar.getOpponentTag(winnerClanTag) : null;
            ClanLogSystem.logWarEnded(winnerClanTag != null ? winnerClanTag : "EMPATE", loserTag != null ? loserTag : "N/A");

            if (winnerClanTag != null) {
                Clan winner = ClanManager.getInstance().forceLoadClan(winnerClanTag);
                if (winner != null) {
                    winner.addWin();
                    ClanManager.getInstance().saveClan(winner);
                }

                if (loserTag != null) {
                    Clan loser = ClanManager.getInstance().forceLoadClan(loserTag);
                    if (loser != null) {
                        loser.addLoss();
                        ClanManager.getInstance().saveClan(loser);
                    }
                }
            }

            activeWar = null;
        }
    }

    public void onPlayerKill(UUID killerUuid, UUID victimUuid) {
        if (activeWar != null) {
            activeWar.addKill(killerUuid);
            activeWar.addDeath(victimUuid);

            Clan killerClan = ClanManager.getInstance().getPlayerClan(killerUuid);
            Clan victimClan = ClanManager.getInstance().getPlayerClan(victimUuid);

            if (killerClan != null) {
                killerClan.addKill();
                ClanManager.getInstance().saveClan(killerClan);
            }
            if (victimClan != null) {
                victimClan.addDeath();
                ClanManager.getInstance().saveClan(victimClan);
            }

            saveWar(activeWar);
        }
    }

    public ClanWar getActiveClanWar(String clanTag) {
        for (ClanWar war : wars.values()) {
            if (!war.isFinished() && !war.hasExpiredByTime() && war.isParticipant(clanTag) && war.getArenaKey() != null) {
                return war;
            }
        }

        try {
            JsonObject response = clanAPI.getClanWars(clanTag);
            if (response != null && !response.get("error").getAsBoolean()) {
                JsonArray dataArray = response.getAsJsonArray("data");
                for (int i = 0; i < dataArray.size(); i++) {
                    ClanWar war = gson.fromJson(dataArray.get(i).toString(), ClanWar.class);
                    if (!war.isFinished() && !war.hasExpiredByTime() && war.getArenaKey() != null) {
                        wars.put(war.getWarId(), war);
                        return war;
                    }
                }
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error obteniendo guerra activa:", e.getMessage());
        }
        return null;
    }

    public boolean isPlayerClanInWar(UUID playerUuid) {
        Clan clan = ClanManager.getInstance().getPlayerClan(playerUuid);
        return clan != null && getActiveClanWar(clan.getTag()) != null;
    }

    public List<ClanWar> getPendingWars() {
        List<ClanWar> pending = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (!war.isFinished() && !war.isAccepted() && !war.isExpired()) {
                pending.add(war);
            }
        }
        return pending;
    }

    public List<ClanWar> getReadyWars() {
        List<ClanWar> ready = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (!war.isFinished() && war.isAccepted()) {
                ready.add(war);
            }
        }
        ready.sort((w1, w2) -> Long.compare(w1.getScheduledTime(), w2.getScheduledTime()));
        return ready;
    }

    public void setActiveWar(ClanWar war) {
        this.activeWar = war;
        if (war != null) {
            ClanManager.getInstance().ensureClansLoaded(war.getChallengerClanTag(), war.getChallengedClanTag());
        }
    }

    public ClanWar getActiveWar() {
        return activeWar;
    }

    public ClanWar getActiveWarWithArena() {
        if (activeWar != null && activeWar.getArenaKey() != null && !activeWar.isFinished()) {
            return activeWar;
        }

        List<ClanWar> candidateWars = new ArrayList<>();
        String thisServerKey = plugin.getSettings().getDefaultWarServer();

        for (ClanWar war : wars.values()) {
            if (war.getArenaKey() != null && !war.isFinished() && war.isAccepted() && (thisServerKey == null || thisServerKey.equals(war.getArenaKey()))) {
                candidateWars.add(war);
            }
        }

        if (candidateWars.isEmpty()) {
            return null;
        }

        candidateWars.sort((w1, w2) -> Long.compare(w1.getScheduledTime(), w2.getScheduledTime()));

        ClanWar selectedWar = candidateWars.get(0);

        this.activeWar = selectedWar;
        ClanManager.getInstance().ensureClansLoaded(selectedWar.getChallengerClanTag(), selectedWar.getChallengedClanTag());

        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.WAR, "Guerra activa seleccionada:");
        ClanLogSystem.info(LogCategory.WAR, "ID:", selectedWar.getWarId().toString());
        ClanLogSystem.info(LogCategory.WAR, "Clanes:", selectedWar.getChallengerClanTag(), "vs", selectedWar.getChallengedClanTag());
        ClanLogSystem.info(LogCategory.WAR, "Programada:", selectedWar.getFormattedScheduledTime());
        ClanLogSystem.info(LogCategory.WAR, "Servidor:", selectedWar.getArenaKey());
        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");

        return selectedWar;
    }

    private ClanWar getWarFromAPI(UUID warId) {
        try {
            JsonObject response = clanAPI.getWar(warId.toString());
            if (response != null && !response.get("error").getAsBoolean()) {
                ClanWar war = gson.fromJson(response.getAsJsonObject("data").toString(), ClanWar.class);
                wars.put(warId, war);
                return war;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error obteniendo guerra desde API:", e.getMessage());
        }
        return null;
    }

    public void refreshAllWarsFromAPI() {
        wars.clear();
        loadAllWars();
    }

    public void refreshReadyWarsFromAPI() {
        try {
            JsonObject response = clanAPI.getActiveWars();
            if (response == null || response.get("error").getAsBoolean()) return;

            JsonArray dataArray = response.getAsJsonArray("data");
            int newCount = 0, updatedCount = 0;

            for (int i = 0; i < dataArray.size(); i++) {
                ClanWar war = gson.fromJson(dataArray.get(i).toString(), ClanWar.class);
                if (war.isAccepted() && war.getArenaKey() == null && !war.isFinished()) {
                    UUID warId = war.getWarId();
                    if (!wars.containsKey(warId)) {
                        wars.put(warId, war);
                        ClanManager.getInstance().ensureClansLoaded(war.getChallengerClanTag(), war.getChallengedClanTag());
                        ClanLogSystem.debug(LogCategory.SYNC, "Nueva guerra:", warId.toString());
                        newCount++;
                    } else {
                        wars.put(warId, war);
                        updatedCount++;
                    }
                }
            }

            if (newCount > 0 || updatedCount > 0) {
                ClanLogSystem.debug(LogCategory.SYNC, "Refresh - Nuevas:", String.valueOf(newCount), "Actualizadas:", String.valueOf(updatedCount));
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.SYNC, "Error refreshing:", e.getMessage());
        }
    }

    public String formatTimeRemaining(long ms) {
        if (ms <= 0) return "&c0m 0s";
        long min = TimeUnit.MILLISECONDS.toMinutes(ms);
        long sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return (min > 5 ? "&a" : min > 2 ? "&e" : "&c") + min + "m " + sec + "s";
    }

    public int getWarsCountAtHour(long scheduledTime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        cal.setTimeInMillis(scheduledTime);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long start = cal.getTimeInMillis();
        long end = start + (60 * 60 * 1000);
        int count = 0;

        for (ClanWar war : wars.values()) {
            if (!war.isFinished()) {
                long t = war.getScheduledTime();
                if (t >= start && t < end) count++;
            }
        }
        return count;
    }

    public boolean clanHasWarAtHour(String clanTag, long scheduledTime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        cal.setTimeInMillis(scheduledTime);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long start = cal.getTimeInMillis();
        long end = start + (60 * 60 * 1000);

        for (ClanWar war : wars.values()) {
            if (!war.isFinished() && war.isParticipant(clanTag)) {
                long t = war.getScheduledTime();
                if (t >= start && t < end) return true;
            }
        }
        return false;
    }

    public boolean canScheduleWarAtHour(String challenger, String challenged, long scheduledTime) {
        return getWarsCountAtHour(scheduledTime) < 2 && !clanHasWarAtHour(challenger, scheduledTime) && !clanHasWarAtHour(challenged, scheduledTime);
    }

    public List<ClanWar> getClanScheduledWars(String clanTag) {
        List<ClanWar> clanWars = new ArrayList<>();
        for (ClanWar war : wars.values()) {
            if (!war.isFinished() && war.isParticipant(clanTag) && war.isAccepted()) {
                clanWars.add(war);
            }
        }
        clanWars.sort((w1, w2) -> Long.compare(w1.getScheduledTime(), w2.getScheduledTime()));
        return clanWars;
    }

    public Map<Integer, Integer> getHourlyWarDistribution() {
        Map<Integer, Integer> dist = new HashMap<>();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        for (ClanWar war : wars.values()) {
            if (!war.isFinished()) {
                cal.setTimeInMillis(war.getScheduledTime());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                dist.put(hour, dist.getOrDefault(hour, 0) + 1);
            }
        }
        return dist;
    }

    public void notifyTeamConnected(String clanTag) {
        if (activeWar == null) {
            ClanLogSystem.debug(LogCategory.WAR, "No hay guerra activa, ignorando notificación de conexión");
            return;
        }

        boolean updated = false;

        if (activeWar.getChallengerClanTag().equals(clanTag)) {
            if (!activeWar.isTeam1Connected()) {
                activeWar.setTeam1Connected(true);
                updated = true;
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                ClanLogSystem.info(LogCategory.WAR, "EQUIPO 1 CONECTADO");
                ClanLogSystem.info(LogCategory.WAR, "Clan:", clanTag);
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            }
        } else if (activeWar.getChallengedClanTag().equals(clanTag)) {
            if (!activeWar.isTeam2Connected()) {
                activeWar.setTeam2Connected(true);
                updated = true;
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                ClanLogSystem.info(LogCategory.WAR, "EQUIPO 2 CONECTADO");
                ClanLogSystem.info(LogCategory.WAR, "Clan:", clanTag);
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            }
        }

        if (updated) {
            saveWar(activeWar);

            if (activeWar.areBothTeamsConnected()) {
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                ClanLogSystem.info(LogCategory.WAR, "AMBOS EQUIPOS CONECTADOS");
                ClanLogSystem.info(LogCategory.WAR, "Guerra lista para iniciar");
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            }
        }
    }

    public void updateClanElo(String winnerTag, String loserTag, boolean wasForfeit) {
        try {
            Clan winner = ClanManager.getInstance().forceLoadClan(winnerTag);
            Clan loser = ClanManager.getInstance().forceLoadClan(loserTag);

            if (winner == null || loser == null) {
                ClanLogSystem.error(LogCategory.WAR, "No se pueden actualizar ELOs - Clanes no encontrados");
                return;
            }

            int winnerElo = winner.getElo();
            int loserElo = loser.getElo();

            EloCalculator.EloResult result = EloCalculator.calculateEloChanges(winnerElo, loserElo, wasForfeit);

            winner.setElo(result.getNewWinnerElo());
            loser.setElo(result.getNewLoserElo());

            ClanManager.getInstance().saveClan(winner);
            ClanManager.getInstance().saveClan(loser);

            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            ClanLogSystem.info(LogCategory.WAR, "ACTUALIZACIÓN ELO");
            ClanLogSystem.info(LogCategory.WAR, "Ganador:", winnerTag, winnerElo + " → " + result.getNewWinnerElo(), "(+" + result.getEloChange() + ")");
            ClanLogSystem.info(LogCategory.WAR, "Perdedor:", loserTag, loserElo + " → " + result.getNewLoserElo(), "(-" + result.getEloChange() + ")");
            ClanLogSystem.info(LogCategory.WAR, "Tipo:", wasForfeit ? "FORFEIT (K=16)" : "NORMAL (K=32)");
            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");

        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error actualizando ELO:", e.getMessage());
            e.printStackTrace();
        }
    }
}