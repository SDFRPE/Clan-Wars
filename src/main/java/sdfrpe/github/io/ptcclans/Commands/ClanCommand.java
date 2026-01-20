package sdfrpe.github.io.ptcclans.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanInvitationManager;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanRequestManager;
import sdfrpe.github.io.ptcclans.Menu.ClanMainMenu;
import sdfrpe.github.io.ptcclans.Menu.ClanListMenu;
import sdfrpe.github.io.ptcclans.Menu.ClanInfoMenu;
import sdfrpe.github.io.ptcclans.Menu.ClanRequestsMenu;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanInvitation;
import sdfrpe.github.io.ptcclans.Models.ClanMember;
import sdfrpe.github.io.ptcclans.Models.ClanRole;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

public class ClanCommand implements CommandExecutor {
    private final PTCClans plugin;

    public ClanCommand(PTCClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(c("&cEste comando solo puede ser ejecutado por jugadores."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            new ClanMainMenu(player, plugin).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
            case "lista":
                new ClanListMenu(player, plugin, 0).open();
                break;

            case "info":
                Clan playerClan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
                if (playerClan != null) {
                    new ClanInfoMenu(player, plugin, playerClan).open();
                } else {
                    player.sendMessage(c("&c✖ No estás en un clan."));
                }
                break;

            case "leave":
            case "salir":
                handleLeave(player);
                break;

            case "requests":
            case "solicitudes":
                handleRequests(player);
                break;

            case "accept":
            case "aceptar":
                if (args.length < 2) {
                    player.sendMessage(c("&cUso: /clan accept <TAG o nombre>"));
                    return true;
                }
                handleAccept(player, args[1]);
                break;

            case "deny":
            case "rechazar":
                if (args.length < 2) {
                    player.sendMessage(c("&cUso: /clan deny <TAG o nombre>"));
                    return true;
                }
                handleDeny(player, args[1]);
                break;

            default:
                new ClanMainMenu(player, plugin).open();
                break;
        }

        return true;
    }

    private void handleLeave(Player player) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage(c("&c✖ No estás en un clan."));
            return;
        }

        if (!plugin.getSettings().canManageClans()) {
            player.sendMessage(c("&c✖ Solo disponible en el lobby."));
            return;
        }

        if (clan.isLeader(player.getUniqueId())) {
            if (clan.getMemberCount() == 1) {
                ClanManager.getInstance().deleteClan(clan.getTag());
                player.sendMessage(c("&6═══════════════════════════════════"));
                player.sendMessage(c("&e&l✔ CLAN ELIMINADO"));
                player.sendMessage(c("&7Eras el único miembro"));
                player.sendMessage(c("&7El clan " + clan.getFormattedTag() + " &7ha sido eliminado"));
                player.sendMessage(c("&6═══════════════════════════════════"));
                ClanLogSystem.info(LogCategory.CLAN, "Clan auto-eliminado por líder solo:", clan.getTag(), "-", player.getName());
                return;
            }

            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ NO PUEDES SALIR"));
            player.sendMessage(c("&7Eres el líder del clan"));
            player.sendMessage(c(""));
            player.sendMessage(c("&7Opciones:"));
            player.sendMessage(c("&e1. &7Transfiere el liderazgo primero"));
            player.sendMessage(c("&e2. &7Elimina el clan desde /clan"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        clan.removeMember(player.getUniqueId());
        ClanManager.getInstance().clearPlayerClanMapping(player.getUniqueId());
        ClanManager.getInstance().saveClan(clan);
        ClanInvitationManager.getInstance().removeAllInvitations(player.getUniqueId());
        ClanRequestManager.getInstance().removeAllRequests(player.getUniqueId());

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&e&l← HAS SALIDO DEL CLAN"));
        player.sendMessage(c("&7Clan: " + clan.getFormattedTag()));
        player.sendMessage(c("&7Ya no eres miembro"));
        player.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogSystem.info(LogCategory.CLAN, "Jugador salió del clan:", player.getName(), "→", clan.getTag());
    }

    private void handleRequests(Player player) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            player.sendMessage(c("&c✖ No estás en un clan."));
            return;
        }

        if (!clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER)) {
            player.sendMessage(c("&c✖ Solo Co-Líderes y Líderes pueden ver solicitudes."));
            return;
        }

        new ClanRequestsMenu(player, plugin, clan).open();
    }

    private void handleAccept(Player player, String identifier) {
        ClanInvitation invitation = ClanInvitationManager.getInstance().getInvitation(player.getUniqueId(), identifier.toUpperCase());

        if (invitation != null) {
            handleAcceptInvitation(player, identifier.toUpperCase());
            return;
        }

        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan != null && clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER)) {
            player.sendMessage(c("&eUsa el menú: /clan requests"));
            return;
        }

        player.sendMessage(c("&c✖ No tienes invitaciones ni solicitudes pendientes."));
    }

    private void handleDeny(Player player, String identifier) {
        ClanInvitation invitation = ClanInvitationManager.getInstance().getInvitation(player.getUniqueId(), identifier.toUpperCase());

        if (invitation != null) {
            handleDenyInvitation(player, identifier.toUpperCase());
            return;
        }

        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan != null && clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER)) {
            player.sendMessage(c("&eUsa el menú: /clan requests"));
            return;
        }

        player.sendMessage(c("&c✖ No tienes invitaciones ni solicitudes pendientes."));
    }

    private void handleAcceptInvitation(Player player, String clanTag) {
        if (ClanManager.getInstance().getPlayerClan(player.getUniqueId()) != null) {
            player.sendMessage(c("&c✖ Ya estás en un clan."));
            return;
        }

        ClanInvitation invitation = ClanInvitationManager.getInstance().getInvitation(player.getUniqueId(), clanTag);

        if (invitation == null) {
            player.sendMessage(c("&c✖ No tienes una invitación de este clan."));
            return;
        }

        if (invitation.isExpired()) {
            ClanInvitationManager.getInstance().removeInvitation(player.getUniqueId(), clanTag);
            player.sendMessage(c("&c✖ La invitación ha expirado."));
            return;
        }

        Clan clan = ClanManager.getInstance().getClan(clanTag);

        if (clan == null) {
            ClanInvitationManager.getInstance().removeInvitation(player.getUniqueId(), clanTag);
            player.sendMessage(c("&c✖ El clan ya no existe."));
            return;
        }

        if (clan.getMemberCount() >= plugin.getSettings().getMaxClanMembers()) {
            ClanInvitationManager.getInstance().removeInvitation(player.getUniqueId(), clanTag);
            player.sendMessage(c("&c✖ El clan está lleno."));
            return;
        }

        clan.addMember(new ClanMember(player.getUniqueId(), ClanRole.MEMBER));
        ClanManager.getInstance().saveClan(clan);

        if (!clan.isMember(player.getUniqueId())) {
            player.sendMessage(c("&c✖ Error al unirte al clan. Intenta de nuevo."));
            ClanLogSystem.error(LogCategory.CLAN, "Error agregando miembro al clan:", player.getName(), "→", clanTag);
            return;
        }

        ClanInvitationManager.getInstance().removeAllInvitations(player.getUniqueId());
        ClanRequestManager.getInstance().removeAllRequests(player.getUniqueId());

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&a&l✔ TE HAS UNIDO AL CLAN"));
        player.sendMessage(c("&7Clan: " + clan.getFormattedTag()));
        player.sendMessage(c("&7Nombre: &f" + clan.getName()));
        player.sendMessage(c("&7Miembros: &e" + clan.getMemberCount() + "&7/&e" + plugin.getSettings().getMaxClanMembers()));
        player.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogSystem.info(LogCategory.CLAN, "Jugador unido a clan:", player.getName(), "→", clanTag);
    }

    private void handleDenyInvitation(Player player, String clanTag) {
        ClanInvitation invitation = ClanInvitationManager.getInstance().getInvitation(player.getUniqueId(), clanTag);

        if (invitation == null) {
            player.sendMessage(c("&c✖ No tienes una invitación de este clan."));
            return;
        }

        ClanInvitationManager.getInstance().removeInvitation(player.getUniqueId(), clanTag);
        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&c&l✖ INVITACIÓN RECHAZADA"));
        player.sendMessage(c("&7Has rechazado la invitación de &e[" + clanTag + "]"));
        player.sendMessage(c("&6═══════════════════════════════════"));
    }

    private String c(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}