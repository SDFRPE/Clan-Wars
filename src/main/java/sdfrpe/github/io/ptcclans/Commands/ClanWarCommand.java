package sdfrpe.github.io.ptcclans.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanWarManager;
import sdfrpe.github.io.ptcclans.Menu.ClanWarListMenu;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanWar;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

import java.util.List;

public class ClanWarCommand implements CommandExecutor {
    private final PTCClans plugin;

    public ClanWarCommand(PTCClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser ejecutado por jugadores."));
            return true;
        }

        Player player = (Player) sender;
        Clan playerClan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        if (playerClan == null) {
            player.sendMessage(c("&c✖ Debes estar en un clan para usar este comando."));
            return true;
        }

        if (!plugin.getSettings().canExecuteWarCommands()) {
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&lGUERRAS DE CLANES"));
            player.sendMessage(c("&7Las guerras se programan desde el &elobby"));
            player.sendMessage(c("&7Conéctate al lobby para gestionar desafíos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return true;
        }

        if (args.length == 0) {
            new ClanWarListMenu(player, plugin, playerClan).open();
            return true;
        }

        if (!player.hasPermission("ptcclans.admin")) {
            new ClanWarListMenu(player, plugin, playerClan).open();
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "testwar":
                return handleTestWar(player, args);
            case "forceaccept":
                return handleForceAccept(player, args);
            case "list":
                return handleList(player);
            case "testwars":
                return handleTestWars(player);
            case "cleartests":
                return handleClearTests(player);
            default:
                new ClanWarListMenu(player, plugin, playerClan).open();
                return true;
        }
    }

    private boolean handleTestWar(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(c("&cUso: /cw testwar <clan1> <clan2> <minutos>"));
            return true;
        }

        String challengerTag = args[1].toUpperCase();
        String challengedTag = args[2].toUpperCase();
        int minutes;

        try {
            minutes = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(c("&cLos minutos deben ser un número válido."));
            return true;
        }

        if (minutes < 1 || minutes > 60) {
            player.sendMessage(c("&cLos minutos deben estar entre 1 y 60."));
            return true;
        }

        Clan challenger = ClanManager.getInstance().getClan(challengerTag);
        Clan challenged = ClanManager.getInstance().getClan(challengedTag);

        if (challenger == null) {
            player.sendMessage(c("&c✖ El clan " + challengerTag + " no existe."));
            return true;
        }

        if (challenged == null) {
            player.sendMessage(c("&c✖ El clan " + challengedTag + " no existe."));
            return true;
        }

        ClanWar testWar = ClanWarManager.getInstance().createTestChallenge(challengerTag, challengedTag, minutes);

        if (testWar == null) {
            player.sendMessage(c("&c✖ No se pudo crear la guerra de prueba."));
            player.sendMessage(c("&7Verifica que no haya guerras activas para estos clanes."));
            return true;
        }

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&a&l✓ GUERRA DE PRUEBA CREADA"));
        player.sendMessage(c(""));
        player.sendMessage(c("&eID: &f" + testWar.getWarId()));
        player.sendMessage(c("&e" + challengerTag + " &7vs &e" + challengedTag));
        player.sendMessage(c("&eInicia en: &f" + minutes + " minuto(s)"));
        player.sendMessage(c(""));
        player.sendMessage(c("&7Para aceptar automáticamente:"));
        player.sendMessage(c("&f/cw forceaccept " + challengerTag + " " + challengedTag));
        player.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogSystem.info(LogCategory.WAR, "[TEST] Guerra creada: " + challengerTag + " vs " + challengedTag + " - " + minutes + " min");

        return true;
    }

    private boolean handleForceAccept(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(c("&cUso: /cw forceaccept <clan1> <clan2>"));
            return true;
        }

        String challengerTag = args[1].toUpperCase();
        String challengedTag = args[2].toUpperCase();

        boolean success = ClanWarManager.getInstance().forceAcceptChallenge(challengerTag, challengedTag);

        if (success) {
            player.sendMessage(c("&a✓ Guerra aceptada forzadamente."));
            player.sendMessage(c("&7La guerra iniciará según el horario programado."));
            ClanLogSystem.info(LogCategory.WAR, "[TEST] Guerra aceptada forzadamente:", challengerTag, "vs", challengedTag);
        } else {
            player.sendMessage(c("&c✖ No se pudo aceptar la guerra."));
            player.sendMessage(c("&7Verifica que exista una guerra pendiente entre estos clanes."));
        }

        return true;
    }

    private boolean handleList(Player player) {
        List<ClanWar> allWars = ClanWarManager.getInstance().getAllWars();

        if (allWars.isEmpty()) {
            player.sendMessage(c("&7No hay guerras registradas."));
            return true;
        }

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&e&lGUERRAS ACTIVAS"));
        player.sendMessage(c(""));

        for (ClanWar war : allWars) {
            String status = war.isFinished() ? "&c✖ Finalizada" :
                    !war.isAccepted() ? "&e⏳ Pendiente" : "&a✓ Aceptada";

            player.sendMessage(c(status + " &f" + war.getChallengerClanTag() + " &7vs &f" + war.getChallengedClanTag()));

            if (war.isTestWar()) {
                player.sendMessage(c("  &7[TEST] ID: " + war.getWarId()));
            }
        }

        player.sendMessage(c("&6═══════════════════════════════════"));
        return true;
    }

    private boolean handleTestWars(Player player) {
        List<ClanWar> testWars = ClanWarManager.getInstance().getTestWars();

        if (testWars.isEmpty()) {
            player.sendMessage(c("&7No hay guerras de prueba activas."));
            return true;
        }

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&e&lGUERRAS DE PRUEBA"));
        player.sendMessage(c(""));

        for (ClanWar war : testWars) {
            String status = war.isAccepted() ? "&a✓ Aceptada" : "&e⏳ Pendiente";

            player.sendMessage(c(status + " &f" + war.getChallengerClanTag() + " &7vs &f" + war.getChallengedClanTag()));
            player.sendMessage(c("  &7ID: " + war.getWarId()));
            player.sendMessage(c("  &7Horario: " + war.getFormattedScheduledTime()));
        }

        player.sendMessage(c(""));
        player.sendMessage(c("&7Total: &f" + testWars.size() + " guerra(s)"));
        player.sendMessage(c("&6═══════════════════════════════════"));
        return true;
    }

    private boolean handleClearTests(Player player) {
        int cleared = ClanWarManager.getInstance().clearTestWars();

        if (cleared == 0) {
            player.sendMessage(c("&7No había guerras de prueba para limpiar."));
        } else {
            player.sendMessage(c("&a✓ Se eliminaron " + cleared + " guerra(s) de prueba."));
            ClanLogSystem.info(LogCategory.WAR, "[TEST] Limpiadas " + cleared + " guerras de prueba por " + player.getName());
        }

        return true;
    }

    private String c(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}