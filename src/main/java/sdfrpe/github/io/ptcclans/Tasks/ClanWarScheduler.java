package sdfrpe.github.io.ptcclans.Tasks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanWarManager;
import sdfrpe.github.io.ptcclans.Models.ClanWar;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClanWarScheduler extends BukkitRunnable {
    private final PTCClans plugin;
    private final SimpleDateFormat dateFormat;
    private final Set<UUID> processingWars;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ClanWarScheduler(PTCClans plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        this.processingWars = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void run() {
        if (cancelled.get()) {
            return;
        }

        try {
            if (plugin.getSettings().isLobbyMode()) {
                ClanWarManager.getInstance().refreshReadyWarsFromAPI();
                ClanWarManager.getInstance().cleanExpiredActiveWars();

                List<ClanWar> pendingWars = findPendingAcceptedWars();
                for (ClanWar war : pendingWars) {
                    if (cancelled.get()) {
                        break;
                    }

                    if (!processingWars.contains(war.getWarId())) {
                        assignServerToWar(war);
                    }
                }
                return;
            }

            if (plugin.getSettings().isClanWarMode()) {
                ClanWarManager.getInstance().refreshReadyWarsFromAPI();
                ClanWarManager.getInstance().cleanExpiredActiveWars();

                ClanWar activeWar = ClanWarManager.getInstance().getActiveWar();

                if (activeWar != null) {
                    checkActiveWarStatus(activeWar);
                } else {
                    List<ClanWar> pendingWars = findPendingAcceptedWars();
                    for (ClanWar war : pendingWars) {
                        if (cancelled.get()) {
                            break;
                        }

                        if (!processingWars.contains(war.getWarId())) {
                            assignServerToWar(war);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error crítico en ClanWarScheduler:", e.getMessage());
            e.printStackTrace();
        }
    }

    private List<ClanWar> findPendingAcceptedWars() {
        List<ClanWar> pending = new ArrayList<>();
        for (ClanWar war : ClanWarManager.getInstance().getReadyWars()) {
            if (war.isAccepted() && war.getArenaKey() == null && !war.isFinished()) {
                pending.add(war);
            }
        }
        pending.sort((w1, w2) -> Long.compare(w1.getScheduledTime(), w2.getScheduledTime()));
        return pending;
    }

    private void assignServerToWar(ClanWar war) {
        if (processingWars.contains(war.getWarId())) {
            return;
        }

        processingWars.add(war.getWarId());

        try {
            if (war.getWarDurationMinutes() == 0) {
                war.setWarDurationMinutes(plugin.getSettings().getWarDurationMinutes());
            }

            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
            ClanLogSystem.info(LogCategory.WAR, "ASIGNANDO SERVIDOR A GUERRA");
            ClanLogSystem.info(LogCategory.WAR, "Guerra ID:", war.getWarId().toString());
            ClanLogSystem.info(LogCategory.WAR, "Desafiante (AZUL):", war.getChallengerClanTag());
            ClanLogSystem.info(LogCategory.WAR, "Desafiado (ROJO):", war.getChallengedClanTag());
            ClanLogSystem.info(LogCategory.WAR, "Programada para:", dateFormat.format(new Date(war.getScheduledTime())));
            ClanLogSystem.info(LogCategory.WAR, "Duración:", war.getWarDurationMinutes() + " minutos");

            long timeUntil = war.getScheduledTime() - System.currentTimeMillis();
            ClanLogSystem.info(LogCategory.WAR, "Tiempo hasta inicio:", (timeUntil / 60000) + " minutos");
            ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");

            String selectedServer = selectAvailableServer();

            if (selectedServer != null) {
                ClanLogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");
                ClanLogSystem.info(LogCategory.NETWORK, "SERVIDOR SELECCIONADO:", selectedServer);
                ClanLogSystem.info(LogCategory.NETWORK, "Método de selección:", getSelectionMethod(selectedServer));
                ClanLogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");

                war.setArenaKey(selectedServer);
                ClanWarManager.getInstance().saveWar(war);
                notifyServerAssignment(selectedServer, war);
            } else {
                ClanLogSystem.warn(LogCategory.NETWORK, "No hay servidores disponibles - reintentará próximo ciclo");
                ClanLogSystem.warn(LogCategory.NETWORK, "Servidores ocupados:", getOccupiedServersInfo());
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.WAR, "Error asignando servidor:", e.getMessage());
            e.printStackTrace();
        } finally {
            processingWars.remove(war.getWarId());
        }
    }

    private String selectAvailableServer() {
        List<String> warServers = plugin.getSettings().getWarServers();

        if (warServers == null || warServers.isEmpty()) {
            ClanLogSystem.warn(LogCategory.NETWORK, "No hay servidores de guerra configurados en settings.json");
            return plugin.getSettings().getDefaultWarServer();
        }

        ClanLogSystem.debug(LogCategory.NETWORK, "Buscando servidor disponible entre:", warServers.toString());

        for (String server : warServers) {
            if (!hasActiveWar(server)) {
                ClanLogSystem.info(LogCategory.NETWORK, "Servidor libre encontrado:", server);
                return server;
            }
        }

        ClanLogSystem.info(LogCategory.NETWORK, "Todos los servidores ocupados, buscando el que termine primero...");
        return getServerWithEarliestEndTime(warServers);
    }

    private boolean hasActiveWar(String serverKey) {
        for (ClanWar war : ClanWarManager.getInstance().getReadyWars()) {
            if (serverKey.equals(war.getArenaKey()) && !war.isFinished()) {
                long timeRemaining = war.getScheduledEndTime() - System.currentTimeMillis();
                if (timeRemaining > 0) {
                    ClanLogSystem.debug(LogCategory.NETWORK, "Servidor " + serverKey + " ocupado por guerra " + war.getWarId() +
                            " (termina en " + (timeRemaining / 60000) + " min)");
                    return true;
                }
            }
        }
        return false;
    }

    private String getServerWithEarliestEndTime(List<String> warServers) {
        String earliestServer = null;
        long earliestEndTime = Long.MAX_VALUE;

        for (String server : warServers) {
            for (ClanWar war : ClanWarManager.getInstance().getReadyWars()) {
                if (server.equals(war.getArenaKey()) && !war.isFinished()) {
                    long endTime = war.getScheduledEndTime();
                    if (endTime < earliestEndTime) {
                        earliestEndTime = endTime;
                        earliestServer = server;
                    }
                }
            }
        }

        if (earliestServer != null) {
            long minutesUntilFree = (earliestEndTime - System.currentTimeMillis()) / 60000;
            ClanLogSystem.info(LogCategory.NETWORK, "Servidor que termina primero:", earliestServer);
            ClanLogSystem.info(LogCategory.NETWORK, "Estará libre en:", minutesUntilFree + " minutos");
            return earliestServer;
        }

        ClanLogSystem.warn(LogCategory.NETWORK, "No se encontró servidor con guerra activa, usando primero de la lista");
        return warServers.isEmpty() ? plugin.getSettings().getDefaultWarServer() : warServers.get(0);
    }

    private String getSelectionMethod(String selectedServer) {
        if (!hasActiveWar(selectedServer)) {
            return "Servidor libre";
        }

        for (ClanWar war : ClanWarManager.getInstance().getReadyWars()) {
            if (selectedServer.equals(war.getArenaKey()) && !war.isFinished()) {
                long timeRemaining = (war.getScheduledEndTime() - System.currentTimeMillis()) / 60000;
                return "Termina primero (en " + timeRemaining + " min)";
            }
        }

        return "Fallback";
    }

    private String getOccupiedServersInfo() {
        StringBuilder info = new StringBuilder();
        List<String> warServers = plugin.getSettings().getWarServers();

        for (String server : warServers) {
            for (ClanWar war : ClanWarManager.getInstance().getReadyWars()) {
                if (server.equals(war.getArenaKey()) && !war.isFinished()) {
                    long timeRemaining = (war.getScheduledEndTime() - System.currentTimeMillis()) / 60000;
                    info.append(server).append(" (libre en ").append(timeRemaining).append(" min), ");
                }
            }
        }

        return info.length() > 0 ? info.toString() : "Ninguno";
    }

    private void notifyServerAssignment(String serverKey, ClanWar war) {
        long timeUntil = war.getScheduledTime() - System.currentTimeMillis();
        long minutesUntil = timeUntil / 60000;

        ClanLogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.NETWORK, "SERVIDOR ASIGNADO:", serverKey);
        ClanLogSystem.info(LogCategory.WAR, "Guerra ID:", war.getWarId().toString());
        ClanLogSystem.info(LogCategory.WAR, "Duración:", war.getWarDurationMinutes() + " minutos");
        ClanLogSystem.info(LogCategory.WAR, "Finaliza:", dateFormat.format(new Date(war.getScheduledEndTime())));

        if (minutesUntil > 10) {
            ClanLogSystem.info(LogCategory.WAR, "Acceso disponible en:", (minutesUntil - 10) + " minutos");
        } else {
            ClanLogSystem.info(LogCategory.WAR, "Jugadores pueden unirse: AHORA");
        }
        ClanLogSystem.info(LogCategory.NETWORK, "═══════════════════════════════════");
    }

    private void checkActiveWarStatus(ClanWar war) {
        if (war.isFinished()) {
            ClanLogSystem.info(LogCategory.WAR, "Guerra finalizada, limpiando estado activo");
            ClanWarManager.getInstance().setActiveWar(null);
            processingWars.remove(war.getWarId());
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        cancelled.set(true);
        processingWars.clear();
        super.cancel();
        ClanLogSystem.info(LogCategory.CORE, "ClanWarScheduler cancelado");
    }

    public void start() {
        this.runTaskTimer(plugin, 200L, 1200L);
        ClanLogSystem.info(LogCategory.WAR, "ClanWarScheduler iniciado (cada 60 segundos)");
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cleanup() {
        processingWars.clear();
    }
}