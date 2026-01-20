package sdfrpe.github.io.ptcclans.Models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Clan {
    private String name;
    private String displayName;
    private String tag;
    private UUID leaderUuid;
    private List<ClanMember> members;
    private String colorCode;
    private long creationDate;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int elo;

    public Clan(String name, String tag, UUID leaderUuid) {
        this.name = name;
        this.displayName = name;
        this.tag = tag.toUpperCase();
        this.leaderUuid = leaderUuid;
        this.members = new ArrayList<ClanMember>();
        this.colorCode = "&b";
        this.creationDate = System.currentTimeMillis();
        this.wins = 0;
        this.losses = 0;
        this.kills = 0;
        this.deaths = 0;
        this.elo = 1000;

        this.members.add(new ClanMember(leaderUuid, ClanRole.LEADER));
    }

    public String getFormattedTag() {
        return colorCode + "[" + tag + "]";
    }

    public String getFormattedName() {
        if (displayName == null || displayName.isEmpty()) {
            return name;
        }
        return displayName.replace("&", "§");
    }

    public String getCleanName() {
        if (displayName == null || displayName.isEmpty()) {
            return name;
        }
        return displayName.replaceAll("&[0-9a-fk-or]", "");
    }

    public boolean isMember(UUID uuid) {
        for (ClanMember member : members) {
            if (member.getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLeader(UUID uuid) {
        return leaderUuid.equals(uuid);
    }

    public boolean hasPermission(UUID uuid, ClanRole requiredRole) {
        ClanMember member = getMember(uuid);
        if (member == null) return false;

        switch (requiredRole) {
            case LEADER:
                return member.getRole() == ClanRole.LEADER;
            case CO_LEADER:
                return member.getRole() == ClanRole.LEADER || member.getRole() == ClanRole.CO_LEADER;
            case MEMBER:
                return true;
            default:
                return false;
        }
    }

    public ClanMember getMember(UUID uuid) {
        for (ClanMember member : members) {
            if (member.getUuid().equals(uuid)) {
                return member;
            }
        }
        return null;
    }

    public void addMember(ClanMember member) {
        if (!isMember(member.getUuid())) {
            members.add(member);
        }
    }

    public void removeMember(UUID uuid) {
        members.removeIf(member -> member.getUuid().equals(uuid));
    }

    public int getMemberCount() {
        return members.size();
    }

    public double getWinRate() {
        int total = wins + losses;
        if (total == 0) return 0.0;
        return (wins * 100.0) / total;
    }

    public double getKDRatio() {
        if (deaths == 0) return kills;
        return (double) kills / deaths;
    }

    public void addWin() {
        this.wins++;
    }

    public void addLoss() {
        this.losses++;
    }

    public void addKill() {
        this.kills++;
    }

    public void addDeath() {
        this.deaths++;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTag() {
        return tag;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public List<ClanMember> getMembers() {
        return members;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = Math.max(0, Math.min(5000, elo));
    }

    public void addElo(int amount) {
        setElo(this.elo + amount);
    }

    public void subtractElo(int amount) {
        setElo(this.elo - amount);
    }
}