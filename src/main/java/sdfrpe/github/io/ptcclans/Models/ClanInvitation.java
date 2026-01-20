package sdfrpe.github.io.ptcclans.Models;

import java.util.UUID;

public class ClanInvitation {
    private String clanTag;
    private UUID inviterUuid;
    private UUID invitedUuid;
    private long timestamp;
    private long expirationTime;

    public ClanInvitation(String clanTag, UUID inviterUuid, UUID invitedUuid) {
        this.clanTag = clanTag;
        this.inviterUuid = inviterUuid;
        this.invitedUuid = invitedUuid;
        this.timestamp = System.currentTimeMillis();
        this.expirationTime = timestamp + (5 * 60 * 1000);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getClanTag() {
        return clanTag;
    }

    public UUID getInviterUuid() {
        return inviterUuid;
    }

    public UUID getInvitedUuid() {
        return invitedUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }
}