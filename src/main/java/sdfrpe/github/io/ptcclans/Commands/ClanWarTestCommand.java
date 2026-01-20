package sdfrpe.github.io.ptcclans.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanWarManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanWar;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class ClanWarTestCommand implements CommandExecutor {
    private final PTCClans plugin;
    private final SimpleDateFormat dateFormat;

    public ClanWarTestCommand(PTCClans plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("America/Lima"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ptcclans.admin")) {
            sender.sendMessage(c("&c✖ Necesitas el permiso &6ptcclans.admin &cpara usar este comando."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "quick":
            case "q":
                return handleQuick(sender, args);

            case "instant":
            case "now":
                return handleInstant(sender, args);

            case "accept":
            case "a":
                return handleAccept(sender, args);

            case "list":
            case "l":
                return handleList(sender);

            case "clear":
            case "clean":
                return handleClear(sender);

            case "info":
            case "i":
                return handleInfo(sender, args);

            case "cleanup":
            case "limpiar":
                if (!sender.hasPermission("ptcclans.admin")) {
                    sender.sendMessage(c("&cNo tienes permiso."));
                    return true;
                }

                sender.sendMessage(c("&6Iniciando limpieza forzada de guerras expiradas..."));
                int cleaned = ClanWarManager.getInstance().forceCleanAllExpiredWars();

                if (cleaned > 0) {
                    sender.sendMessage(c("&a✓ Limpieza completada: &e" + cleaned + " &aguerras finalizadas"));
                } else {
                    sender.sendMessage(c("&7No se encontraron guerras expiradas para limpiar."));
                }
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleQuick(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(c("&c✖ Uso: /cwtest quick <clan1> <clan2> [minutos]"));
            sender.sendMessage(c("&7Ejemplo: /cwtest quick ABC DEF 2"));
            return true;
        }

        String clan1Tag = args[1].toUpperCase();
        String clan2Tag = args[2].toUpperCase();
        int minutes = 2;

        if (args.length >= 4) {
            try {
                minutes = Integer.parseInt(args[3]);
                if (minutes < 0 || minutes > 60) {
                    sender.sendMessage(c("&c✖ Los minutos deben estar entre 0 y 60"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(c("&c✖ Minutos inválidos: " + args[3]));
                return true;
            }
        }

        Clan clan1 = ClanManager.getInstance().getClan(clan1Tag);
        Clan clan2 = ClanManager.getInstance().getClan(clan2Tag);

        if (clan1 == null) {
            sender.sendMessage(c("&c✖ El clan &f" + clan1Tag + " &cno existe"));
            return true;
        }

        if (clan2 == null) {
            sender.sendMessage(c("&c✖ El clan &f" + clan2Tag + " &cno existe"));
            return true;
        }

        if (clan1Tag.equals(clan2Tag)) {
            sender.sendMessage(c("&c✖ Un clan no puede enfrentarse a sí mismo"));
            return true;
        }

        ClanWar war = ClanWarManager.getInstance().createTestChallenge(clan1Tag, clan2Tag, minutes);

        if (war == null) {
            sender.sendMessage(c("&c✖ Error al crear guerra de prueba"));
            sender.sendMessage(c("&7Verifica los logs del servidor"));
            return true;
        }

        Calendar scheduledCal = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        scheduledCal.setTimeInMillis(war.getScheduledTime());

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&a&l✔ GUERRA DE PRUEBA CREADA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7ID: &e" + war.getWarId().toString()));
        sender.sendMessage(c("&7Clan 1 (AZUL): " + clan1.getFormattedTag()));
        sender.sendMessage(c("&7Clan 2 (ROJO): " + clan2.getFormattedTag()));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Programada para:"));
        sender.sendMessage(c("&e" + dateFormat.format(scheduledCal.getTime())));
        sender.sendMessage(c("&7Inicia en: &a" + minutes + " minuto(s)"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Para auto-aceptar:"));
        sender.sendMessage(c("&e/cwtest accept " + war.getWarId().toString()));
        sender.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.WAR, "GUERRA DE PRUEBA CREADA");
        ClanLogSystem.info(LogCategory.WAR, "Creador:", sender.getName());
        ClanLogSystem.info(LogCategory.WAR, "ID:", war.getWarId().toString());
        ClanLogSystem.info(LogCategory.WAR, "Clanes:", clan1Tag, "vs", clan2Tag);
        ClanLogSystem.info(LogCategory.WAR, "Inicio en:", minutes + " minutos");
        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");

        return true;
    }

    private boolean handleInstant(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(c("&c✖ Uso: /cwtest instant <clan1> <clan2>"));
            sender.sendMessage(c("&7Ejemplo: /cwtest instant ABC DEF"));
            return true;
        }

        String clan1Tag = args[1].toUpperCase();
        String clan2Tag = args[2].toUpperCase();

        Clan clan1 = ClanManager.getInstance().getClan(clan1Tag);
        Clan clan2 = ClanManager.getInstance().getClan(clan2Tag);

        if (clan1 == null) {
            sender.sendMessage(c("&c✖ El clan &f" + clan1Tag + " &cno existe"));
            return true;
        }

        if (clan2 == null) {
            sender.sendMessage(c("&c✖ El clan &f" + clan2Tag + " &cno existe"));
            return true;
        }

        if (clan1Tag.equals(clan2Tag)) {
            sender.sendMessage(c("&c✖ Un clan no puede enfrentarse a sí mismo"));
            return true;
        }

        ClanWar war = ClanWarManager.getInstance().createTestChallenge(clan1Tag, clan2Tag, 0);

        if (war == null) {
            sender.sendMessage(c("&c✖ Error al crear guerra de prueba"));
            return true;
        }

        if (!ClanWarManager.getInstance().forceAcceptChallenge(war.getWarId())) {
            sender.sendMessage(c("&c✖ Error al auto-aceptar guerra"));
            return true;
        }

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&a&l✔ GUERRA INSTANTÁNEA CREADA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7ID: &e" + war.getWarId().toString()));
        sender.sendMessage(c("&7Clan 1 (AZUL): " + clan1.getFormattedTag()));
        sender.sendMessage(c("&7Clan 2 (ROJO): " + clan2.getFormattedTag()));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&a&l✔ ACEPTADA AUTOMÁTICAMENTE"));
        sender.sendMessage(c("&7La guerra se asignará al servidor en"));
        sender.sendMessage(c("&7el siguiente ciclo del scheduler"));
        sender.sendMessage(c("&7(máximo 60 segundos)"));
        sender.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");
        ClanLogSystem.info(LogCategory.WAR, "GUERRA INSTANTÁNEA CREADA");
        ClanLogSystem.info(LogCategory.WAR, "Creador:", sender.getName());
        ClanLogSystem.info(LogCategory.WAR, "ID:", war.getWarId().toString());
        ClanLogSystem.info(LogCategory.WAR, "Clanes:", clan1Tag, "vs", clan2Tag);
        ClanLogSystem.info(LogCategory.WAR, "Estado: ACEPTADA - Lista para asignar");
        ClanLogSystem.info(LogCategory.WAR, "═══════════════════════════════════");

        return true;
    }

    private boolean handleAccept(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(c("&c✖ Uso: /cwtest accept <warId>"));
            sender.sendMessage(c("&7Usa /cwtest list para ver guerras pendientes"));
            return true;
        }

        String warIdStr = args[1];

        if (!ClanWarManager.getInstance().forceAcceptChallenge(warIdStr)) {
            sender.sendMessage(c("&c✖ Error al aceptar guerra"));
            sender.sendMessage(c("&7Verifica que el ID sea correcto"));
            sender.sendMessage(c("&7Usa /cwtest list para ver guerras"));
            return true;
        }

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&a&l✔ GUERRA ACEPTADA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7ID: &e" + warIdStr));
        sender.sendMessage(c("&7Estado: &aACEPTADA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7La guerra se asignará al servidor"));
        sender.sendMessage(c("&7cuando llegue su hora programada"));
        sender.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogSystem.info(LogCategory.WAR, "Guerra de prueba aceptada manualmente:", warIdStr, "por", sender.getName());

        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<ClanWar> testWars = ClanWarManager.getInstance().getTestWars();

        if (testWars.isEmpty()) {
            sender.sendMessage(c("&6═══════════════════════════════════"));
            sender.sendMessage(c("&e&lGUERRAS DE PRUEBA"));
            sender.sendMessage(c(""));
            sender.sendMessage(c("&7No hay guerras de prueba activas"));
            sender.sendMessage(c(""));
            sender.sendMessage(c("&7Usa: &e/cwtest quick <clan1> <clan2>"));
            sender.sendMessage(c("&6═══════════════════════════════════"));
            return true;
        }

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&lGUERRAS DE PRUEBA ACTIVAS"));
        sender.sendMessage(c("&7Total: &e" + testWars.size()));
        sender.sendMessage(c(""));

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        int index = 1;

        for (ClanWar war : testWars) {
            Calendar warTime = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
            warTime.setTimeInMillis(war.getScheduledTime());

            long timeUntil = war.getScheduledTime() - now.getTimeInMillis();
            long minutesUntil = timeUntil / 60000;
            long secondsUntil = (timeUntil / 1000) % 60;

            String status;
            if (war.isFinished()) {
                status = "&c✖ FINALIZADA";
            } else if (war.isAccepted()) {
                if (war.getArenaKey() != null) {
                    status = "&a✔ ACTIVA (" + war.getArenaKey() + ")";
                } else if (timeUntil <= 0) {
                    status = "&e⚡ LISTA PARA ASIGNAR";
                } else {
                    status = "&a✔ ACEPTADA (" + minutesUntil + "m " + secondsUntil + "s)";
                }
            } else {
                status = "&e⏳ PENDIENTE (" + minutesUntil + "m " + secondsUntil + "s)";
            }

            sender.sendMessage(c("&6" + index + ". &f" + war.getChallengerClanTag() + " &7vs &f" + war.getChallengedClanTag()));
            sender.sendMessage(c("   &7ID: &e" + war.getWarId().toString().substring(0, 8) + "..."));
            sender.sendMessage(c("   &7Estado: " + status));
            sender.sendMessage(c("   &7Hora: &e" + dateFormat.format(warTime.getTime())));

            if (!war.isAccepted() && !war.isFinished()) {
                sender.sendMessage(c("   &7Auto-aceptar: &e/cwtest accept " + war.getWarId()));
            }

            sender.sendMessage(c(""));
            index++;
        }

        sender.sendMessage(c("&7Limpiar todas: &e/cwtest clear"));
        sender.sendMessage(c("&6═══════════════════════════════════"));

        return true;
    }

    private boolean handleClear(CommandSender sender) {
        int count = ClanWarManager.getInstance().clearTestWars();

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&lLIMPIEZA DE GUERRAS DE PRUEBA"));
        sender.sendMessage(c(""));

        if (count == 0) {
            sender.sendMessage(c("&7No había guerras de prueba para limpiar"));
        } else {
            sender.sendMessage(c("&a&l✔ LIMPIEZA COMPLETADA"));
            sender.sendMessage(c("&7Guerras eliminadas: &e" + count));
        }

        sender.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogSystem.info(LogCategory.WAR, "Guerras de prueba limpiadas:", String.valueOf(count), "- Por:", sender.getName());

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(c("&c✖ Uso: /cwtest info <warId>"));
            sender.sendMessage(c("&7Usa /cwtest list para ver IDs"));
            return true;
        }

        String warIdStr = args[1];
        ClanWar war = ClanWarManager.getInstance().getWarById(warIdStr);

        if (war == null) {
            sender.sendMessage(c("&c✖ Guerra no encontrada: " + warIdStr));
            return true;
        }

        Calendar warTime = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        warTime.setTimeInMillis(war.getScheduledTime());

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        long timeUntil = war.getScheduledTime() - now.getTimeInMillis();
        long minutesUntil = timeUntil / 60000;
        long secondsUntil = (timeUntil / 1000) % 60;

        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&lINFORMACIÓN DE GUERRA DE PRUEBA"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7ID Completo: &e" + war.getWarId().toString()));
        sender.sendMessage(c("&7Clan 1 (AZUL): &b" + war.getChallengerClanTag()));
        sender.sendMessage(c("&7Clan 2 (ROJO): &c" + war.getChallengedClanTag()));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Programada: &e" + dateFormat.format(warTime.getTime())));
        sender.sendMessage(c("&7Tiempo hasta inicio: &e" + minutesUntil + "m " + secondsUntil + "s"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Estado Aceptación: " + (war.isAccepted() ? "&a✔ ACEPTADA" : "&e⏳ PENDIENTE")));
        sender.sendMessage(c("&7Estado Final: " + (war.isFinished() ? "&c✖ FINALIZADA" : "&a✔ ACTIVA")));
        sender.sendMessage(c("&7Arena Asignada: " + (war.getArenaKey() != null ? "&a" + war.getArenaKey() : "&7Ninguna")));
        sender.sendMessage(c("&7Es Prueba: &a" + (war.isTestWar() ? "SÍ" : "NO")));
        sender.sendMessage(c(""));

        if (war.isFinished() && war.getWinnerClanTag() != null) {
            sender.sendMessage(c("&7Ganador: &6" + war.getWinnerClanTag()));
        }

        sender.sendMessage(c("&6═══════════════════════════════════"));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(c("&6═══════════════════════════════════"));
        sender.sendMessage(c("&e&lCOMANDOS DE PRUEBA - GUERRAS"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/cwtest quick <clan1> <clan2> [min]"));
        sender.sendMessage(c("&7  Crear guerra que inicia en X minutos"));
        sender.sendMessage(c("&7  Por defecto: 2 minutos"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/cwtest instant <clan1> <clan2>"));
        sender.sendMessage(c("&7  Crear y auto-aceptar guerra (0 min)"));
        sender.sendMessage(c("&7  Se asigna al servidor inmediatamente"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/cwtest accept <warId>"));
        sender.sendMessage(c("&7  Auto-aceptar una guerra pendiente"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/cwtest list"));
        sender.sendMessage(c("&7  Ver todas las guerras de prueba"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/cwtest info <warId>"));
        sender.sendMessage(c("&7  Ver información detallada de una guerra"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&e/cwtest clear"));
        sender.sendMessage(c("&7  Limpiar todas las guerras de prueba"));
        sender.sendMessage(c(""));
        sender.sendMessage(c("&7Ejemplos:"));
        sender.sendMessage(c("&6• &f/cwtest quick ABC DEF 2"));
        sender.sendMessage(c("&6• &f/cwtest instant ABC DEF"));
        sender.sendMessage(c("&6• &f/cwtest list"));
        sender.sendMessage(c("&6═══════════════════════════════════"));
    }

    private String c(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}