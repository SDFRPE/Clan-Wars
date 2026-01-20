package sdfrpe.github.io.ptcclans;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import sdfrpe.github.io.ptcclans.Adapters.ClanWarAdapter;
import sdfrpe.github.io.ptcclans.Commands.ClanCommand;
import sdfrpe.github.io.ptcclans.Commands.ClanWarCommand;
import sdfrpe.github.io.ptcclans.Integration.CreditosIntegration;
import sdfrpe.github.io.ptcclans.Listeners.ChatListener;
import sdfrpe.github.io.ptcclans.Managers.ClanInvitationManager;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanRequestManager;
import sdfrpe.github.io.ptcclans.Managers.ClanWarManager;
import sdfrpe.github.io.ptcclans.Listeners.MenuListener;
import sdfrpe.github.io.ptcclans.Placeholders.PTCClansExpansion;
import sdfrpe.github.io.ptcclans.Tasks.ClanWarScheduler;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;
import sdfrpe.github.io.ptcclans.Utils.ClanSettings;
import sdfrpe.github.io.ptcclans.Commands.ClanWarTestCommand;

public class PTCClans extends JavaPlugin {
    private static PTCClans instance;
    private ClanSettings settings;
    private Gson gson;
    private ClanWarAdapter clanWarAdapter;
    private BukkitTask cleanupTask;
    private BukkitTask challengeCleanupTask;
    private ClanWarScheduler clanWarScheduler;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        ClanLogger.initialize(this);
        ClanLogSystem.logPluginStart();
        ClanLogSystem.info(LogCategory.CORE, "Versión: " + getDescription().getVersion());

        settings = new ClanSettings();
        settings.initialize(this);

        gson = new GsonBuilder().setPrettyPrinting().create();

        ClanManager.initialize(this);
        ClanWarManager.initialize(this);

        clanWarAdapter = new ClanWarAdapter(this);
        ClanLogSystem.debug(LogCategory.CORE, "ClanWarAdapter inicializado");

        if (settings.isLobbyMode()) {
            clanWarScheduler = new ClanWarScheduler(this);
            clanWarScheduler.start();
            ClanLogSystem.info(LogCategory.WAR, "ClanWarScheduler iniciado en modo LOBBY");
        }

        if (settings.isClanWarMode()) {
            clanWarScheduler = new ClanWarScheduler(this);
            clanWarScheduler.start();
        }

        ClanInvitationManager.getInstance();
        ClanLogSystem.debug(LogCategory.CORE, "ClanInvitationManager inicializado");

        ClanRequestManager.getInstance();
        ClanLogSystem.debug(LogCategory.CORE, "ClanRequestManager inicializado");

        CreditosIntegration.initialize();

        registerCommands();
        registerPlaceholders();
        startCleanupTasks();

        ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.CORE, "PTCClans habilitado correctamente");
        ClanLogSystem.info(LogCategory.CORE, "Sistema de menús: ACTIVO");
        ClanLogSystem.info(LogCategory.CORE, "Sistema de limpieza: ACTIVO");
        ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PTCClansExpansion(this).register();
            ClanLogSystem.info(LogCategory.CORE, "PlaceholderAPI hooked - Placeholders registrados");
        } else {
            ClanLogSystem.warn(LogCategory.CORE, "PlaceholderAPI no encontrado - Placeholders deshabilitados");
        }
    }

    private void startCleanupTasks() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    ClanInvitationManager.getInstance().cleanExpiredInvitations();
                    ClanRequestManager.getInstance().cleanExpiredRequests();
                } catch (Exception e) {
                    ClanLogSystem.error(LogCategory.CORE, "Error en cleanup task:", e.getMessage());
                }
            }
        }, 6000L, 6000L);
        ClanLogSystem.debug(LogCategory.CORE, "Tarea de limpieza iniciada (cada 5 minutos)");

        challengeCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    ClanWarManager.getInstance().cleanExpiredChallenges();
                } catch (Exception e) {
                    ClanLogSystem.error(LogCategory.WAR, "Error en challenge cleanup:", e.getMessage());
                }
            }
        }, 1200L, 1200L);
        ClanLogSystem.debug(LogCategory.WAR, "Tarea de limpieza de desafíos iniciada (cada 1 minuto)");
    }

    @Override
    public void onDisable() {
        ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.CORE, "PTCClans deshabilitando...");
        ClanLogSystem.info(LogCategory.CORE, "═══════════════════════════════════");

        if (cleanupTask != null) {
            cleanupTask.cancel();
            ClanLogSystem.debug(LogCategory.CORE, "Cleanup task cancelada");
        }

        if (challengeCleanupTask != null) {
            challengeCleanupTask.cancel();
            ClanLogSystem.debug(LogCategory.WAR, "Challenge cleanup task cancelada");
        }

        if (clanWarScheduler != null) {
            try {
                clanWarScheduler.cancel();
                ClanLogSystem.debug(LogCategory.WAR, "ClanWarScheduler cancelado");
            } catch (Exception e) {
                ClanLogSystem.error(LogCategory.WAR, "Error cancelando scheduler:", e.getMessage());
            }
        }

        ClanManager manager = ClanManager.getInstance();
        if (manager != null) {
            manager.saveAllClans();
        }

        ClanLogSystem.logPluginStop();
    }

    private void registerCommands() {
        getCommand("clan").setExecutor(new ClanCommand(this));
        ClanLogSystem.debug(LogCategory.CORE, "Comando /clan registrado");

        getCommand("clanwar").setExecutor(new ClanWarCommand(this));

        if (settings.canExecuteWarCommands()) {
            ClanLogSystem.debug(LogCategory.WAR, "Comando /clanwar registrado (HABILITADO)");
        } else {
            ClanLogSystem.debug(LogCategory.WAR, "Comando /clanwar registrado (DESHABILITADO - Solo mensaje informativo)");
        }

        boolean enableTestCommands = false;

        if (enableTestCommands) {
            getCommand("cwtest").setExecutor(new ClanWarTestCommand(this));
            ClanLogSystem.warn(LogCategory.CORE, "⚠ ═══════════════════════════════════");
            ClanLogSystem.warn(LogCategory.CORE, "⚠ COMANDO /cwtest HABILITADO");
            ClanLogSystem.warn(LogCategory.CORE, "⚠ ESTO ES SOLO PARA DESARROLLO");
            ClanLogSystem.warn(LogCategory.CORE, "⚠ DESHABILITAR EN PRODUCCIÓN");
            ClanLogSystem.warn(LogCategory.CORE, "⚠ ═══════════════════════════════════");
        } else {
            ClanLogSystem.info(LogCategory.CORE, "Comandos de testing deshabilitados (producción)");
        }

        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        ClanLogSystem.debug(LogCategory.MENU, "MenuListener registrado");

        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        ClanLogSystem.debug(LogCategory.CORE, "ChatListener registrado");
    }

    public static PTCClans getInstance() {
        return instance;
    }

    public ClanSettings getSettings() {
        return settings;
    }

    public Gson getGson() {
        return gson;
    }

    public ClanManager getClanManager() {
        return ClanManager.getInstance();
    }

    public ClanWarManager getClanWarManager() {
        return ClanWarManager.getInstance();
    }

    public ClanWarAdapter getClanWarAdapter() {
        return clanWarAdapter;
    }
}