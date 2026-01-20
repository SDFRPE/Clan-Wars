package sdfrpe.github.io.ptcclans.Models;

import java.util.UUID;

public class ClanRequest {
    private String clanTag;
    private UUID requesterUuid;
    private long timestamp;
    private long expirationTime;

    public ClanRequest(String clanTag, UUID requesterUuid) {
        this.clanTag = clanTag;
        this.requesterUuid = requesterUuid;
        this.timestamp = System.currentTimeMillis();
        this.expirationTime = timestamp + (5 * 60 * 1000);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getClanTag() {
        return clanTag;
    }

    public UUID getRequesterUuid() {
        return requesterUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }
}