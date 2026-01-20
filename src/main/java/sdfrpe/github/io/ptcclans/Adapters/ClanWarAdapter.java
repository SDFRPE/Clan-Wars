package sdfrpe.github.io.ptcclans.Adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Database.ClanAPI;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanWar;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import sdfrpe.github.io.ptcclans.Utils.EloCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ClanWarAdapter {
    private final PTCClans plugin;
    private final ClanAPI clanAPI;
    private final Gson gson;
    private ClanWar currentWar;
    private Clan cachedBlueClan;
    private Clan cachedRedClan;
    private long lastAPICall = 0;
    private long lastClanCacheUpdate = 0;
    private static final long API_CALL_COOLDOWN = 5000;
    private static final long CLAN_CACHE_TTL = 30000;
    private String currentWarId = null;

    public ClanWarAdapter(PTCClans plugin) {
        this.plugin = plugin;
        this.clanAPI = new ClanAPI();
        this.gson = plugin.getGson();
        this.currentWar = null;
        this.cachedBlueClan = null;
        this.cachedRedClan = null;
    }

    public ClanWar loadNextWar() {
        long now = System.currentTimeMillis();
        if (now - lastAPICall < API_CALL_COOLDOWN) {
            ClanLogSystem.debug(LogCategory.API, "Cooldown API activo, usando datos cacheados");
            return currentWar;
        }
        lastAPICall = now;

        try {
            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            ClanLogSystem.info(LogCategory.WAR, "Buscando enfrentamiento en API...");

            String thisServerName = plugin.getSettings().getDefaultWarServer();
            ClanLogSystem.debug(LogCategory.WAR, "Servidor actual:", thisServerName);

            JsonObject response = null;
            try {
                response = Bukkit.getScheduler().callSyncMethod(plugin, () -> clanAPI.getAllActiveWars())
                        .get(10, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                ClanLogSystem.error(LogCategory.API, "TIMEOUT: API no respondió en 10 segundos");
                return currentWar;
            } catch (Exception e) {
                ClanLogSystem.error(LogCategory.API, "Error llamando API:", e.getMessage());
                return currentWar;
            }

            if (response == null || response.get("error").getAsBoolean()) {
                ClanLogSystem.warn(LogCategory.API, "No se pudo obtener guerras desde la API");
                return null;
            }

            JsonArray wars = response.getAsJsonArray("data");

            if (wars.size() == 0) {
                ClanLogSystem.info(LogCategory.WAR, "No hay enfrentamientos pendientes");
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                return null;
            }

            List<ClanWar> candidateWars = new ArrayList<>();

            for (int i = 0; i < wars.size(); i++) {
                JsonObject warJson = wars.get(i).getAsJsonObject();
                ClanWar war = gson.fromJson(warJson.toString(), ClanWar.class);

                if (war.isAccepted() && !war.isFinished() && !war.isExpired() &&
                        thisServerName.equals(war.getArenaKey())) {
                    candidateWars.add(war);
                }
            }

            if (candidateWars.isEmpty()) {
                ClanLogSystem.info(LogCategory.WAR, "No hay enfrentamientos asignados a este servidor");
                ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                return null;
            }

            candidateWars.sort((w1, w2) -> Long.compare(w1.getScheduledTime(), w2.getScheduledTime()));

            ClanWar selectedWar = candidateWars.get(0);

            this.currentWar = selectedWar;
            this.currentWarId = selectedWar.getWarId().toString();

            ClanManager.getInstance().ensureClansLoaded(
                    selectedWar.getChallengerClanTag(),
                    selectedWar.getChallengedClanTag()
            );

            this.cachedBlueClan = ClanManager.getInstance().forceLoadClan(selectedWar.getChallengerClanTag());
            this.cachedRedClan = ClanManager.getInstance().forceLoadClan(selectedWar.getChallengedClanTag());
            this.lastClanCacheUpdate = now;

            ClanLogSystem.info(LogCategory.WAR, "Enfrentamiento cargado:");
            ClanLogSystem.info(LogCategory.WAR, "- Clan 1 (AZUL):", selectedWar.getChallengerClanTag());
            ClanLogSystem.info(LogCategory.WAR, "- Clan 2 (ROJO):", selectedWar.getChallengedClanTag());
            ClanLogSystem.info(LogCategory.WAR, "- Servidor:", selectedWar.getArenaKey());
            ClanLogSystem.info(LogCategory.WAR, "- ID Guerra:", selectedWar.getWarId().toString());
            ClanLogSystem.info(LogCategory.WAR, "- Programada para:", selectedWar.getFormattedScheduledTime());

            if (candidateWars.size() > 1) {
                ClanLogSystem.info(LogCategory.WAR, "- Guerras filtradas:", String.valueOf(candidateWars.size()));
                ClanLogSystem.info(LogCategory.WAR, "- Seleccionada: Guerra más próxima");
            }

            if (cachedBlueClan != null) {
                ClanLogSystem.info(LogCategory.CLAN, "- Clan AZUL cargado:", cachedBlueClan.getFormattedName());
            }
            if (cachedRedClan != null) {
                ClanLogSystem.info(LogCategory.CLAN, "- Clan ROJO cargado:", cachedRedClan.getFormattedName());
            }

            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            return selectedWar;

        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error cargando guerra desde API:", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void syncWithManager() {
        try {
            ClanWar managedWar = PTCClans.getInstance().getClanWarManager().getActiveWarWithArena();

            if (managedWar != null) {
                String managedWarId = managedWar.getWarId().toString();

                if (currentWar == null || !managedWarId.equals(currentWarId)) {
                    this.currentWar = managedWar;
                    this.currentWarId = managedWarId;

                    ClanManager.getInstance().ensureClansLoaded(
                            managedWar.getChallengerClanTag(),
                            managedWar.getChallengedClanTag()
                    );

                    this.cachedBlueClan = ClanManager.getInstance().forceLoadClan(managedWar.getChallengerClanTag());
                    this.cachedRedClan = ClanManager.getInstance().forceLoadClan(managedWar.getChallengedClanTag());
                    this.lastClanCacheUpdate = System.currentTimeMillis();

                    ClanLogSystem.info(LogCategory.SYNC, "═══════════════════════════════════");
                    ClanLogSystem.info(LogCategory.SYNC, "ClanWarAdapter SINCRONIZADO");
                    ClanLogSystem.info(LogCategory.WAR, "Guerra ID:", managedWar.getWarId().toString());
                    ClanLogSystem.info(LogCategory.WAR, "Clanes:", managedWar.getChallengerClanTag(), "vs", managedWar.getChallengedClanTag());
                    ClanLogSystem.info(LogCategory.WAR, "Arena:", managedWar.getArenaKey());
                    ClanLogSystem.info(LogCategory.WAR, "Programada:", managedWar.getFormattedScheduledTime());

                    if (cachedBlueClan != null) {
                        ClanLogSystem.info(LogCategory.CLAN, "Clan AZUL sincronizado:", cachedBlueClan.getFormattedName());
                    }
                    if (cachedRedClan != null) {
                        ClanLogSystem.info(LogCategory.CLAN, "Clan ROJO sincronizado:", cachedRedClan.getFormattedName());
                    }

                    ClanLogSystem.info(LogCategory.SYNC, "═══════════════════════════════════");
                } else {
                    ClanLogSystem.debug(LogCategory.SYNC, "Guerra ya sincronizada:", currentWarId);
                }
            } else {
                if (currentWar != null) {
                    ClanLogSystem.info(LogCategory.SYNC, "No hay guerra activa en manager, limpiando adapter");
                    cleanup();
                }
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.SYNC, "Error sincronizando adapter con manager:", e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean assignArenaToWar(String arenaKey) {
        if (currentWar == null) {
            ClanLogSystem.warn(LogCategory.WAR, "No hay guerra actual para asignar arena");
            return false;
        }

        try {
            currentWar.setArenaKey(arenaKey);
            String warJson = gson.toJson(currentWar);
            JsonObject response = clanAPI.saveWar(currentWar.getWarId().toString(), warJson);

            if (response != null && !response.get("error").getAsBoolean()) {
                ClanLogSystem.info(LogCategory.WAR, "Arena " + arenaKey + " asignada a guerra " + currentWar.getWarId());
                return true;
            } else {
                ClanLogSystem.error(LogCategory.API, "Error asignando arena a guerra");
                return false;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error asignando arena:", e.getMessage());
            return false;
        }
    }

    public String getBlueClanTag() {
        return currentWar != null ? currentWar.getChallengerClanTag() : null;
    }

    public String getRedClanTag() {
        return currentWar != null ? currentWar.getChallengedClanTag() : null;
    }

    public Clan getBlueClan() {
        if (currentWar == null) return null;

        long now = System.currentTimeMillis();
        if (cachedBlueClan != null && (now - lastClanCacheUpdate) < CLAN_CACHE_TTL) {
            return cachedBlueClan;
        }

        cachedBlueClan = ClanManager.getInstance().forceLoadClan(currentWar.getChallengerClanTag());
        lastClanCacheUpdate = now;
        return cachedBlueClan;
    }

    public Clan getRedClan() {
        if (currentWar == null) return null;

        long now = System.currentTimeMillis();
        if (cachedRedClan != null && (now - lastClanCacheUpdate) < CLAN_CACHE_TTL) {
            return cachedRedClan;
        }

        cachedRedClan = ClanManager.getInstance().forceLoadClan(currentWar.getChallengedClanTag());
        lastClanCacheUpdate = now;
        return cachedRedClan;
    }

    public boolean isPlayerInBlueClan(UUID playerUuid) {
        Clan blueClan = getBlueClan();
        return blueClan != null && blueClan.isMember(playerUuid);
    }

    public boolean isPlayerInRedClan(UUID playerUuid) {
        Clan redClan = getRedClan();
        return redClan != null && redClan.isMember(playerUuid);
    }

    public void onWarFinished(String winnerClanTag) {
        if (currentWar == null) {
            ClanLogSystem.warn(LogCategory.WAR, "No hay guerra actual para finalizar");
            return;
        }

        try {
            currentWar.setFinished(true);
            currentWar.setWinnerClanTag(winnerClanTag);

            String warJson = gson.toJson(currentWar);
            JsonObject response = clanAPI.saveWar(currentWar.getWarId().toString(), warJson);

            if (response != null && !response.get("error").getAsBoolean()) {
                String loserTag = winnerClanTag != null ? currentWar.getOpponentTag(winnerClanTag) : null;
                ClanLogSystem.logWarEnded(
                        winnerClanTag != null ? winnerClanTag : "EMPATE",
                        loserTag != null ? loserTag : "N/A"
                );

                if (winnerClanTag != null && loserTag != null) {
                    Clan winnerClan = ClanManager.getInstance().forceLoadClan(winnerClanTag);
                    Clan loserClan = ClanManager.getInstance().forceLoadClan(loserTag);

                    if (winnerClan != null && loserClan != null) {
                        winnerClan.addWin();
                        loserClan.addLoss();

                        boolean wasForfeit = currentWar.wasForfeit();
                        int winnerElo = winnerClan.getElo();
                        int loserElo = loserClan.getElo();

                        EloCalculator.EloResult eloResult = EloCalculator.calculateEloChanges(
                                winnerElo, loserElo, wasForfeit
                        );

                        winnerClan.setElo(eloResult.getNewWinnerElo());
                        loserClan.setElo(eloResult.getNewLoserElo());

                        ClanManager.getInstance().saveClan(winnerClan);
                        ClanManager.getInstance().saveClan(loserClan);

                        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                        ClanLogSystem.info(LogCategory.WAR, "ELO ACTUALIZADO");
                        ClanLogSystem.info(LogCategory.WAR, winnerClanTag + ":",
                                winnerElo + " → " + eloResult.getNewWinnerElo() + " (+" + eloResult.getEloChange() + ")");
                        ClanLogSystem.info(LogCategory.WAR, loserTag + ":",
                                loserElo + " → " + eloResult.getNewLoserElo() + " (-" + eloResult.getEloChange() + ")");
                        ClanLogSystem.info(LogCategory.WAR, "Tipo:", wasForfeit ? "FORFEIT" : "NORMAL");
                        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                    }
                }

                cleanup();
            } else {
                ClanLogSystem.error(LogCategory.API, "Error finalizando guerra en API");
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error finalizando guerra:", e.getMessage());
            e.printStackTrace();
        }
    }

    public void onPlayerKill(UUID killerUuid, UUID victimUuid) {
        if (currentWar == null) return;

        try {
            currentWar.addKill(killerUuid);
            currentWar.addDeath(victimUuid);

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

            String warJson = gson.toJson(currentWar);
            clanAPI.saveWar(currentWar.getWarId().toString(), warJson);
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error registrando kill en guerra:", e.getMessage());
        }
    }

    public ClanWar getCurrentWar() {
        return currentWar;
    }

    public boolean hasActiveWar() {
        return currentWar != null;
    }

    public long getWarStartTime() {
        return currentWar != null ? currentWar.getScheduledTime() : 0;
    }

    public String getWarId() {
        return currentWarId;
    }

    public boolean isWarActive() {
        return currentWar != null && !currentWar.isFinished();
    }

    public String getBlueClanDisplayName() {
        Clan blueClan = getBlueClan();
        return blueClan != null ? blueClan.getFormattedName() : "AZUL";
    }

    public String getRedClanDisplayName() {
        Clan redClan = getRedClan();
        return redClan != null ? redClan.getFormattedName() : "ROJO";
    }

    public String getBlueClanColor() {
        Clan blueClan = getBlueClan();
        return blueClan != null ? blueClan.getColorCode() : "&9";
    }

    public String getRedClanColor() {
        Clan redClan = getRedClan();
        return redClan != null ? redClan.getColorCode() : "&c";
    }

    public void notifyTeamConnected(String clanTag) {
        if (currentWar == null) {
            ClanLogSystem.debug(LogCategory.WAR, "No hay guerra actual para notificar conexión");
            return;
        }

        boolean updated = false;

        if (currentWar.getChallengerClanTag().equals(clanTag)) {
            if (!currentWar.isTeam1Connected()) {
                currentWar.setTeam1Connected(true);
                updated = true;
                ClanLogSystem.info(LogCategory.NETWORK, "Equipo AZUL (" + clanTag + ") conectado");
            }
        } else if (currentWar.getChallengedClanTag().equals(clanTag)) {
            if (!currentWar.isTeam2Connected()) {
                currentWar.setTeam2Connected(true);
                updated = true;
                ClanLogSystem.info(LogCategory.NETWORK, "Equipo ROJO (" + clanTag + ") conectado");
            }
        }

        if (updated) {
            try {
                String warJson = gson.toJson(currentWar);
                clanAPI.saveWar(currentWar.getWarId().toString(), warJson);

                if (currentWar.areBothTeamsConnected()) {
                    ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                    ClanLogSystem.info(LogCategory.WAR, "AMBOS EQUIPOS CONECTADOS");
                    ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
                }
            } catch (Exception e) {
                ClanLogSystem.error(LogCategory.WAR, "Error guardando conexión de equipo:", e.getMessage());
            }
        }
    }

    public boolean areBothTeamsConnected() {
        return currentWar != null && currentWar.areBothTeamsConnected();
    }

    public boolean isTeam1Connected() {
        return currentWar != null && currentWar.isTeam1Connected();
    }

    public boolean isTeam2Connected() {
        return currentWar != null && currentWar.isTeam2Connected();
    }

    public void cleanup() {
        this.currentWar = null;
        this.currentWarId = null;
        this.cachedBlueClan = null;
        this.cachedRedClan = null;
        this.lastClanCacheUpdate = 0;
        ClanLogSystem.debug(LogCategory.SYNC, "ClanWarAdapter limpiado");
    }
}