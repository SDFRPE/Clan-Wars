package sdfrpe.github.io.ptcclans.Managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanInvitation;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

import java.util.*;

public class ClanInvitationManager {
    private static ClanInvitationManager instance;
    private final Map<UUID, List<ClanInvitation>> invitations;

    private ClanInvitationManager() {
        this.invitations = new HashMap<UUID, List<ClanInvitation>>();
    }

    public static ClanInvitationManager getInstance() {
        if (instance == null) {
            instance = new ClanInvitationManager();
        }
        return instance;
    }

    public void addInvitation(ClanInvitation invitation) {
        UUID invitedUuid = invitation.getInvitedUuid();
        if (!invitations.containsKey(invitedUuid)) {
            invitations.put(invitedUuid, new ArrayList<ClanInvitation>());
        }
        invitations.get(invitedUuid).add(invitation);
        ClanLogSystem.debug(LogCategory.CLAN, "Invitación agregada para:", invitedUuid.toString(), "- Clan:", invitation.getClanTag());
    }

    public List<ClanInvitation> getInvitations(UUID playerUuid) {
        List<ClanInvitation> playerInvitations = invitations.get(playerUuid);
        if (playerInvitations == null) {
            return new ArrayList<ClanInvitation>();
        }

        List<ClanInvitation> validInvitations = new ArrayList<ClanInvitation>();
        for (ClanInvitation inv : playerInvitations) {
            if (!inv.isExpired()) {
                validInvitations.add(inv);
            }
        }
        return validInvitations;
    }

    public ClanInvitation getInvitation(UUID playerUuid, String clanTag) {
        List<ClanInvitation> playerInvitations = getInvitations(playerUuid);
        for (ClanInvitation inv : playerInvitations) {
            if (inv.getClanTag().equalsIgnoreCase(clanTag)) {
                return inv;
            }
        }
        return null;
    }

    public void removeInvitation(UUID playerUuid, String clanTag) {
        List<ClanInvitation> playerInvitations = invitations.get(playerUuid);
        if (playerInvitations != null) {
            playerInvitations.removeIf(inv -> inv.getClanTag().equalsIgnoreCase(clanTag));
            ClanLogSystem.debug(LogCategory.CLAN, "Invitación removida para:", playerUuid.toString(), "- Clan:", clanTag);
        }
    }

    public void removeAllInvitations(UUID playerUuid) {
        invitations.remove(playerUuid);
        ClanLogSystem.debug(LogCategory.CLAN, "Todas las invitaciones removidas para:", playerUuid.toString());
    }

    public void removeAllInvitationsByClan(String clanTag) {
        int removed = 0;
        for (Map.Entry<UUID, List<ClanInvitation>> entry : invitations.entrySet()) {
            List<ClanInvitation> playerInvitations = entry.getValue();
            int sizeBefore = playerInvitations.size();
            playerInvitations.removeIf(inv -> inv.getClanTag().equalsIgnoreCase(clanTag));
            removed += (sizeBefore - playerInvitations.size());
        }
        if (removed > 0) {
            ClanLogSystem.debug(LogCategory.CLAN, "Invitaciones limpiadas por eliminación de clan:", clanTag, "- Total:", String.valueOf(removed));
        }
    }

    public boolean hasInvitation(UUID playerUuid, String clanTag) {
        return getInvitation(playerUuid, clanTag) != null;
    }

    public void cleanExpiredInvitations() {
        int removed = 0;
        for (Map.Entry<UUID, List<ClanInvitation>> entry : invitations.entrySet()) {
            List<ClanInvitation> playerInvitations = entry.getValue();
            int sizeBefore = playerInvitations.size();
            playerInvitations.removeIf(ClanInvitation::isExpired);
            removed += (sizeBefore - playerInvitations.size());
        }
        if (removed > 0) {
            ClanLogSystem.debug(LogCategory.CLAN, "Invitaciones expiradas limpiadas:", String.valueOf(removed));
        }
    }

    public void notifyPlayer(UUID playerUuid, String clanTag, String inviterName) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            Clan clan = ClanManager.getInstance().getClan(clanTag);
            if (clan != null) {
                player.sendMessage("§6═══════════════════════════════════");
                player.sendMessage("§e§lINVITACIÓN DE CLAN");
                player.sendMessage("§7Has sido invitado al clan " + clan.getFormattedTag());
                player.sendMessage("§7Por: §e" + inviterName);
                player.sendMessage("");
                player.sendMessage("§aEscribe §e/clan accept " + clanTag + " §apara aceptar");
                player.sendMessage("§cEscribe §e/clan deny " + clanTag + " §cpara rechazar");
                player.sendMessage("§7La invitación expira en §e5 minutos");
                player.sendMessage("§6═══════════════════════════════════");
            }
        }
    }
}