package sdfrpe.github.io.ptcclans.Models;

import java.util.UUID;

public class ClanMember {
    private UUID uuid;
    private String playerName;
    private ClanRole role;
    private long joinDate;
    private int warKills;
    private int warDeaths;

    public ClanMember(UUID uuid, ClanRole role) {
        this.uuid = uuid;
        this.playerName = null;
        this.role = role;
        this.joinDate = System.currentTimeMillis();
        this.warKills = 0;
        this.warDeaths = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public ClanRole getRole() {
        return role;
    }

    public void setRole(ClanRole role) {
        this.role = role;
    }

    public long getJoinDate() {
        return joinDate;
    }

    public int getWarKills() {
        return warKills;
    }

    public void setWarKills(int warKills) {
        this.warKills = warKills;
    }

    public void addWarKill() {
        this.warKills++;
    }

    public int getWarDeaths() {
        return warDeaths;
    }

    public void setWarDeaths(int warDeaths) {
        this.warDeaths = warDeaths;
    }

    public void addWarDeath() {
        this.warDeaths++;
    }

    public double getKDRatio() {
        return warDeaths == 0 ? warKills : (double) warKills / warDeaths;
    }
}