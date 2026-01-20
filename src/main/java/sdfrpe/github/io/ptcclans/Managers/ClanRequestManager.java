package sdfrpe.github.io.ptcclans.Managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanRequest;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;

import java.util.*;

public class ClanRequestManager {
    private static ClanRequestManager instance;
    private final Map<String, List<ClanRequest>> requests;

    private ClanRequestManager() {
        this.requests = new HashMap<String, List<ClanRequest>>();
    }

    public static ClanRequestManager getInstance() {
        if (instance == null) {
            instance = new ClanRequestManager();
        }
        return instance;
    }

    public void addRequest(ClanRequest request) {
        String clanTag = request.getClanTag();
        if (!requests.containsKey(clanTag)) {
            requests.put(clanTag, new ArrayList<ClanRequest>());
        }
        requests.get(clanTag).add(request);
        ClanLogSystem.debug(LogCategory.CLAN, "Solicitud agregada para clan:", clanTag, "- Jugador:", request.getRequesterUuid().toString());
    }

    public List<ClanRequest> getRequests(String clanTag) {
        List<ClanRequest> clanRequests = requests.get(clanTag);
        if (clanRequests == null) {
            return new ArrayList<ClanRequest>();
        }

        List<ClanRequest> validRequests = new ArrayList<ClanRequest>();
        for (ClanRequest req : clanRequests) {
            if (!req.isExpired()) {
                validRequests.add(req);
            }
        }
        return validRequests;
    }

    public ClanRequest getRequest(String clanTag, UUID requesterUuid) {
        List<ClanRequest> clanRequests = getRequests(clanTag);
        for (ClanRequest req : clanRequests) {
            if (req.getRequesterUuid().equals(requesterUuid)) {
                return req;
            }
        }
        return null;
    }

    public void removeRequest(String clanTag, UUID requesterUuid) {
        List<ClanRequest> clanRequests = requests.get(clanTag);
        if (clanRequests != null) {
            clanRequests.removeIf(req -> req.getRequesterUuid().equals(requesterUuid));
            ClanLogSystem.debug(LogCategory.CLAN, "Solicitud removida de clan:", clanTag, "- Jugador:", requesterUuid.toString());
        }
    }

    public void removeAllRequests(UUID requesterUuid) {
        for (List<ClanRequest> clanRequests : requests.values()) {
            clanRequests.removeIf(req -> req.getRequesterUuid().equals(requesterUuid));
        }
        ClanLogSystem.debug(LogCategory.CLAN, "Todas las solicitudes removidas para:", requesterUuid.toString());
    }

    public void removeAllRequestsByClan(String clanTag) {
        List<ClanRequest> removed = requests.remove(clanTag);
        if (removed != null && !removed.isEmpty()) {
            ClanLogSystem.debug(LogCategory.CLAN, "Solicitudes limpiadas por eliminación de clan:", clanTag, "- Total:", String.valueOf(removed.size()));
        }
    }

    public boolean hasRequest(String clanTag, UUID requesterUuid) {
        return getRequest(clanTag, requesterUuid) != null;
    }

    public void cleanExpiredRequests() {
        int removed = 0;
        for (Map.Entry<String, List<ClanRequest>> entry : requests.entrySet()) {
            List<ClanRequest> clanRequests = entry.getValue();
            int sizeBefore = clanRequests.size();
            clanRequests.removeIf(ClanRequest::isExpired);
            removed += (sizeBefore - clanRequests.size());
        }
        if (removed > 0) {
            ClanLogSystem.debug(LogCategory.CLAN, "Solicitudes expiradas limpiadas:", String.valueOf(removed));
        }
    }

    public void notifyLeaders(String clanTag, String requesterName) {
        Clan clan = ClanManager.getInstance().getClan(clanTag);
        if (clan != null) {
            Player leader = Bukkit.getPlayer(clan.getLeaderUuid());
            if (leader != null && leader.isOnline()) {
                leader.sendMessage("§6═══════════════════════════════════");
                leader.sendMessage("§e§lSOLICITUD DE CLAN");
                leader.sendMessage("§e" + requesterName + " §7quiere unirse a " + clan.getFormattedTag());
                leader.sendMessage("");
                leader.sendMessage("§aEscribe §e/clan requests §apara ver solicitudes");
                leader.sendMessage("§7La solicitud expira en §e5 minutos");
                leader.sendMessage("§6═══════════════════════════════════");
            }
        }
    }
}