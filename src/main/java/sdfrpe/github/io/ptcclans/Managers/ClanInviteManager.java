package sdfrpe.github.io.ptcclans.Managers;

import sdfrpe.github.io.ptcclans.Utils.ClanLogger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClanInviteManager {
    private final Map<UUID, String> invites;
    private final Map<UUID, Long> inviteTimestamps;
    private final long INVITE_TIMEOUT = 5 * 60 * 1000;

    public ClanInviteManager() {
        this.invites = new ConcurrentHashMap<>();
        this.inviteTimestamps = new ConcurrentHashMap<>();
    }

    public void createInvite(UUID playerUuid, String clanTag) {
        invites.put(playerUuid, clanTag);
        inviteTimestamps.put(playerUuid, System.currentTimeMillis());
        ClanLogger.debug("Invitación creada para", playerUuid.toString(), "al clan", clanTag);
    }

    public boolean hasInvite(UUID playerUuid) {
        if (!invites.containsKey(playerUuid)) {
            return false;
        }

        long inviteTime = inviteTimestamps.getOrDefault(playerUuid, 0L);
        if (System.currentTimeMillis() - inviteTime > INVITE_TIMEOUT) {
            removeInvite(playerUuid);
            return false;
        }

        return true;
    }

    public String getInviteClan(UUID playerUuid) {
        if (!hasInvite(playerUuid)) {
            return null;
        }
        return invites.get(playerUuid);
    }

    public void removeInvite(UUID playerUuid) {
        invites.remove(playerUuid);
        inviteTimestamps.remove(playerUuid);
        ClanLogger.debug("Invitación removida para", playerUuid.toString());
    }

    public void clearExpiredInvites() {
        long currentTime = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, Long> entry : inviteTimestamps.entrySet()) {
            if (currentTime - entry.getValue() > INVITE_TIMEOUT) {
                expired.add(entry.getKey());
            }
        }

        for (UUID uuid : expired) {
            removeInvite(uuid);
        }

        if (!expired.isEmpty()) {
            ClanLogger.debug("Invitaciones expiradas eliminadas:", String.valueOf(expired.size()));
        }
    }
}