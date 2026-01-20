package sdfrpe.github.io.ptcclans.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Integration.CreditosIntegration;
import sdfrpe.github.io.ptcclans.Managers.ClanInvitationManager;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanInvitation;
import sdfrpe.github.io.ptcclans.Models.ClanMember;
import sdfrpe.github.io.ptcclans.Models.ClanRole;
import sdfrpe.github.io.ptcclans.Utils.ChatInputManager;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;

import java.util.UUID;

public class ChatListener implements Listener {
    private final PTCClans plugin;

    public ChatListener(PTCClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!ChatInputManager.hasPendingAction(player)) {
            return;
        }

        event.setCancelled(true);

        ChatInputManager.PendingAction action = ChatInputManager.getPendingAction(player);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("cancelar")) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&lACCIÓN CANCELADA"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        switch (action.getType()) {
            case CREATE_CLAN_NAME:
                handleClanNameInput(player, message);
                break;
            case CREATE_CLAN_TAG:
                handleClanTagInput(player, message);
                break;
            case INVITE_MEMBER:
                handleInviteMember(player, message);
                break;
            case TRANSFER_LEADERSHIP:
                handleTransferLeadership(player, message);
                break;
            case EDIT_CLAN_NAME:
                handleEditClanName(player, message);
                break;
        }
    }

    private void handleClanNameInput(Player player, String input) {
        String cleanName = input.replaceAll("&[0-9a-fk-or]", "");

        int minLength = plugin.getSettings().getMinNameLength();
        int maxLength = plugin.getSettings().getMaxNameLength();

        if (cleanName.length() < minLength) {
            player.sendMessage(c("&c✖ El nombre es demasiado corto."));
            player.sendMessage(c("&7Mínimo: &e" + minLength + " &7caracteres (sin contar códigos)"));
            return;
        }

        if (cleanName.length() > maxLength) {
            player.sendMessage(c("&c✖ El nombre es demasiado largo."));
            player.sendMessage(c("&7Máximo: &e" + maxLength + " &7caracteres (sin contar códigos)"));
            return;
        }

        if (!cleanName.matches("^[a-zA-Z0-9 ]+$")) {
            player.sendMessage(c("&c✖ El nombre solo puede contener letras, números y espacios."));
            return;
        }

        if (input.contains("&k")) {
            player.sendMessage(c("&c✖ El código &k (obfuscado) no está permitido."));
            return;
        }

        ChatInputManager.setPendingAction(player, ChatInputManager.ActionType.CREATE_CLAN_TAG, input);

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&a&l✔ NOMBRE VÁLIDO"));
        player.sendMessage(c("&7Nombre: " + input.replace("&", "§")));
        player.sendMessage(c(""));
        player.sendMessage(c("&e&lAhora escribe el TAG del clan"));
        player.sendMessage(c("&7Ejemplo: &eRMC, PVP, SKY"));
        player.sendMessage(c("&7Rango: &e" + plugin.getSettings().getMinTagLength() + "-" + plugin.getSettings().getMaxTagLength() + " &7caracteres"));
        player.sendMessage(c("&7O escribe &ccancel &7para cancelar"));
        player.sendMessage(c("&6═══════════════════════════════════"));
    }

    private void handleEditClanName(Player player, String input) {
        String clanTag = (String) ChatInputManager.getPendingAction(player).getData();
        Clan clan = ClanManager.getInstance().getClan(clanTag);

        if (clan == null) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Clan no encontrado."));
            return;
        }

        if (!clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER)) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Solo Co-Líderes y Líderes pueden editar el nombre."));
            return;
        }

        String cleanName = input.replaceAll("&[0-9a-fk-or]", "");

        int minLength = plugin.getSettings().getMinNameLength();
        int maxLength = plugin.getSettings().getMaxNameLength();

        if (cleanName.length() < minLength) {
            player.sendMessage(c("&c✖ El nombre es demasiado corto."));
            player.sendMessage(c("&7Mínimo: &e" + minLength + " &7caracteres (sin contar códigos)"));
            return;
        }

        if (cleanName.length() > maxLength) {
            player.sendMessage(c("&c✖ El nombre es demasiado largo."));
            player.sendMessage(c("&7Máximo: &e" + maxLength + " &7caracteres (sin contar códigos)"));
            return;
        }

        if (!cleanName.matches("^[a-zA-Z0-9 ]+$")) {
            player.sendMessage(c("&c✖ El nombre solo puede contener letras, números y espacios."));
            return;
        }

        if (input.contains("&k")) {
            player.sendMessage(c("&c✖ El código &k (obfuscado) no está permitido."));
            return;
        }

        clan.setDisplayName(input);
        ClanManager.getInstance().saveClan(clan);
        ChatInputManager.clearPendingAction(player);

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&a&l✔ NOMBRE ACTUALIZADO"));
        player.sendMessage(c("&7Nuevo nombre: " + input.replace("&", "§")));
        player.sendMessage(c("&7TAG: " + clan.getFormattedTag()));
        player.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogger.info("Nombre de clan actualizado:", clanTag, "por", player.getName());
    }

    private void handleClanTagInput(Player player, String tag) {
        String clanName = (String) ChatInputManager.getPendingAction(player).getData();

        int minLength = plugin.getSettings().getMinTagLength();
        int maxLength = plugin.getSettings().getMaxTagLength();

        String upperTag = tag.toUpperCase();

        if (upperTag.length() < minLength) {
            player.sendMessage(c("&c✖ El TAG es demasiado corto."));
            player.sendMessage(c("&7Mínimo: &e" + minLength + " &7caracteres"));
            return;
        }

        if (upperTag.length() > maxLength) {
            player.sendMessage(c("&c✖ El TAG es demasiado largo."));
            player.sendMessage(c("&7Máximo: &e" + maxLength + " &7caracteres"));
            return;
        }

        if (!upperTag.matches("^[A-Z0-9]+$")) {
            player.sendMessage(c("&c✖ El TAG solo puede contener letras y números (sin espacios)."));
            return;
        }

        if (ClanManager.getInstance().clanExists(upperTag)) {
            player.sendMessage(c("&c✖ Ya existe un clan con ese TAG."));
            player.sendMessage(c("&7Elige otro TAG diferente."));
            return;
        }

        if (ClanManager.getInstance().getPlayerClan(player.getUniqueId()) != null) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Ya estás en un clan."));
            return;
        }

        int creationCost = plugin.getSettings().getClanCreationCost();

        if (!CreditosIntegration.hasCredits(player, creationCost)) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ CRÉDITOS INSUFICIENTES"));
            player.sendMessage(c("&7Necesitas: &e" + creationCost + " Créditos"));
            player.sendMessage(c("&7Tienes: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        if (!CreditosIntegration.removeCredits(player, creationCost, "Creación de clan [" + upperTag + "]")) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ ERROR AL PROCESAR PAGO"));
            player.sendMessage(c("&7No se pudo descontar los créditos"));
            player.sendMessage(c("&7El clan NO ha sido creado"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        if (ClanManager.getInstance().createClan(clanName, upperTag, player.getUniqueId())) {
            Clan newClan = ClanManager.getInstance().getClan(upperTag);
            if (newClan != null) {
                newClan.setDisplayName(clanName);
                ClanManager.getInstance().saveClan(newClan);
            }

            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&a&l✔ CLAN CREADO EXITOSAMENTE"));
            player.sendMessage(c("&7Nombre: " + (newClan != null ? newClan.getFormattedName() : clanName.replace("&", "§"))));
            player.sendMessage(c("&7TAG: &b[" + upperTag + "]"));
            player.sendMessage(c("&7Líder: &6" + player.getName()));
            player.sendMessage(c(""));
            player.sendMessage(c("&7Costo: &e-" + creationCost + " Créditos"));
            player.sendMessage(c("&7Saldo actual: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            ClanLogger.info("Clan creado:", upperTag, "por", player.getName());
        } else {
            CreditosIntegration.addCredits(player, creationCost, "Reembolso - Error creando clan [" + upperTag + "]");
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Error al crear el clan."));
            player.sendMessage(c("&7Los créditos han sido reembolsados."));
        }
    }

    private void handleInviteMember(Player player, String targetName) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Ya no estás en un clan."));
            return;
        }

        if (!clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER)) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Solo Co-Líderes y Líderes pueden invitar."));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ El jugador no está en línea."));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ No puedes invitarte a ti mismo."));
            return;
        }

        if (ClanManager.getInstance().getPlayerClan(target.getUniqueId()) != null) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ El jugador ya está en un clan."));
            return;
        }

        if (clan.getMemberCount() >= plugin.getSettings().getMaxClanMembers()) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ El clan está lleno."));
            return;
        }

        if (ClanInvitationManager.getInstance().hasInvitation(target.getUniqueId(), clan.getTag())) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Este jugador ya tiene una invitación pendiente."));
            return;
        }

        int invitationCost = plugin.getSettings().getInvitationCost();

        if (!CreditosIntegration.hasCredits(player, invitationCost)) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ CRÉDITOS INSUFICIENTES"));
            player.sendMessage(c("&7Necesitas: &e" + invitationCost + " Créditos"));
            player.sendMessage(c("&7Tienes: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        if (!CreditosIntegration.removeCredits(player, invitationCost, "Invitación a " + target.getName() + " al clan [" + clan.getTag() + "]")) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ ERROR AL PROCESAR PAGO"));
            player.sendMessage(c("&7No se pudo descontar los créditos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        try {
            ClanInvitation invitation = new ClanInvitation(clan.getTag(), player.getUniqueId(), target.getUniqueId());
            ClanInvitationManager.getInstance().addInvitation(invitation);
            ClanInvitationManager.getInstance().notifyPlayer(target.getUniqueId(), clan.getTag(), player.getName());

            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&a&l✔ INVITACIÓN ENVIADA"));
            player.sendMessage(c("&7Jugador: &e" + target.getName()));
            player.sendMessage(c("&7Costo: &e-" + invitationCost + " Créditos"));
            player.sendMessage(c("&7Saldo actual: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
            player.sendMessage(c("&7El jugador tiene &e5 minutos &7para aceptar"));
            player.sendMessage(c("&6═══════════════════════════════════"));

            ClanLogger.info("Invitación enviada:", clan.getTag(), "→", target.getName(), "por", player.getName());
        } catch (Exception e) {
            CreditosIntegration.addCredits(player, invitationCost, "Reembolso - Error enviando invitación");
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ ERROR AL ENVIAR INVITACIÓN"));
            player.sendMessage(c("&7Los créditos han sido reembolsados"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            ClanLogger.error("Error enviando invitación:", e.getMessage());
        }
    }

    private void handleTransferLeadership(Player player, String targetName) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Ya no estás en un clan."));
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Solo el líder puede transferir el liderazgo."));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ El jugador no está en línea."));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ No puedes transferirte el liderazgo a ti mismo."));
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ El jugador no es miembro del clan."));
            return;
        }

        UUID oldLeaderUuid = player.getUniqueId();
        UUID newLeaderUuid = target.getUniqueId();

        ClanMember oldLeader = clan.getMember(oldLeaderUuid);
        ClanMember newLeader = clan.getMember(newLeaderUuid);

        if (oldLeader == null || newLeader == null) {
            ChatInputManager.clearPendingAction(player);
            player.sendMessage(c("&c✖ Error al transferir liderazgo."));
            return;
        }

        clan.setLeaderUuid(newLeaderUuid);
        oldLeader.setRole(ClanRole.MEMBER);
        newLeader.setRole(ClanRole.LEADER);

        ClanManager.getInstance().saveClan(clan);
        ChatInputManager.clearPendingAction(player);

        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&e&l✔ LIDERAZGO TRANSFERIDO"));
        player.sendMessage(c("&7Nuevo líder: &6" + target.getName()));
        player.sendMessage(c("&7Tu nuevo rango: &fMiembro"));
        player.sendMessage(c("&6═══════════════════════════════════"));

        target.sendMessage(c("&6═══════════════════════════════════"));
        target.sendMessage(c("&6&l★ AHORA ERES EL LÍDER"));
        target.sendMessage(c("&7Clan: " + clan.getFormattedTag()));
        target.sendMessage(c("&7Líder anterior: &e" + player.getName()));
        target.sendMessage(c("&7¡Usa tu poder sabiamente!"));
        target.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogger.info("Liderazgo transferido en clan:", clan.getTag(), "-", player.getName(), "→", target.getName());
    }

    private String c(String text) {
        return text.replace("&", "§");
    }
}