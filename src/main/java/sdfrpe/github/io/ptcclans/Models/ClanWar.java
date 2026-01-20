package sdfrpe.github.io.ptcclans.Models;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClanWar {
    private UUID warId;
    private String challengerClanTag;
    private String challengedClanTag;
    private long scheduledTime;
    private long creationTime;
    private long acceptDeadline;
    private boolean accepted;
    private boolean finished;
    private String winnerClanTag;
    private String arenaKey;
    private Map<UUID, Integer> playerKills;
    private Map<UUID, Integer> playerDeaths;
    private boolean isTestWar;
    private int warDurationMinutes;
    private boolean team1Connected;
    private boolean team2Connected;
    private long forfeitCheckStartTime;
    private boolean forfeitChecked;
    private String forfeitReason;

    public ClanWar(String challengerClanTag, String challengedClanTag) {
        this.warId = UUID.randomUUID();
        this.challengerClanTag = challengerClanTag;
        this.challengedClanTag = challengedClanTag;
        this.scheduledTime = System.currentTimeMillis() + (60 * 60 * 1000);
        this.creationTime = System.currentTimeMillis();
        this.acceptDeadline = this.creationTime + (10 * 60 * 1000);
        this.accepted = false;
        this.finished = false;
        this.winnerClanTag = null;
        this.arenaKey = null;
        this.playerKills = new HashMap<>();
        this.playerDeaths = new HashMap<>();
        this.isTestWar = false;
        this.warDurationMinutes = 60;
        this.team1Connected = false;
        this.team2Connected = false;
        this.forfeitCheckStartTime = 0;
        this.forfeitChecked = false;
        this.forfeitReason = null;
    }

    public UUID getWarId() {
        return warId;
    }

    public String getChallengerClanTag() {
        return challengerClanTag;
    }

    public String getChallengedClanTag() {
        return challengedClanTag;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public long getScheduledEndTime() {
        return scheduledTime + (warDurationMinutes * 60 * 1000);
    }

    public int getWarDurationMinutes() {
        return warDurationMinutes;
    }

    public void setWarDurationMinutes(int warDurationMinutes) {
        this.warDurationMinutes = warDurationMinutes;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getAcceptDeadline() {
        return acceptDeadline;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getWinnerClanTag() {
        return winnerClanTag;
    }

    public void setWinnerClanTag(String winnerClanTag) {
        this.winnerClanTag = winnerClanTag;
    }

    public String getArenaKey() {
        return arenaKey;
    }

    public void setArenaKey(String arenaKey) {
        this.arenaKey = arenaKey;
    }

    public Map<UUID, Integer> getPlayerKills() {
        return playerKills;
    }

    public Map<UUID, Integer> getPlayerDeaths() {
        return playerDeaths;
    }

    public void addKill(UUID playerUuid) {
        playerKills.put(playerUuid, playerKills.getOrDefault(playerUuid, 0) + 1);
    }

    public void addDeath(UUID playerUuid) {
        playerDeaths.put(playerUuid, playerDeaths.getOrDefault(playerUuid, 0) + 1);
    }

    public int getTotalKills(String clanTag) {
        return playerKills.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalDeaths(String clanTag) {
        return playerDeaths.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isParticipant(String clanTag) {
        return challengerClanTag.equals(clanTag) || challengedClanTag.equals(clanTag);
    }

    public boolean isReadyToStart() {
        if (isTestWar) {
            return accepted && !finished;
        }
        return accepted && !finished && System.currentTimeMillis() >= scheduledTime;
    }

    public String getOpponentTag(String clanTag) {
        if (challengerClanTag.equals(clanTag)) {
            return challengedClanTag;
        } else if (challengedClanTag.equals(clanTag)) {
            return challengerClanTag;
        }
        return null;
    }

    public boolean isExpired() {
        if (accepted || isTestWar) {
            return false;
        }
        return System.currentTimeMillis() > acceptDeadline;
    }

    public boolean hasExpiredByTime() {
        if (isTestWar) {
            return false;
        }
        long twoHoursInMillis = 2 * 60 * 60 * 1000;
        return System.currentTimeMillis() > (scheduledTime + twoHoursInMillis);
    }

    public long getTimeUntilExpiration() {
        if (accepted || isTestWar) {
            return 0;
        }
        long remaining = acceptDeadline - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public int getRemainingMinutes() {
        long remaining = getTimeUntilExpiration();
        return (int) TimeUnit.MILLISECONDS.toMinutes(remaining);
    }

    public boolean isTestWar() {
        return isTestWar;
    }

    public void setTestWar(boolean testWar) {
        this.isTestWar = testWar;
    }

    public int getRemainingSeconds() {
        long remaining = getTimeUntilExpiration();
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remaining);
        return (int) (totalSeconds % 60);
    }

    public String getFormattedTimeRemaining() {
        if (isExpired()) {
            return "&c&lEXPIRADO";
        }

        if (accepted) {
            return "&a&lACEPTADO";
        }

        int minutes = getRemainingMinutes();
        int seconds = getRemainingSeconds();

        if (minutes > 0) {
            return "&e" + minutes + "m " + seconds + "s";
        } else {
            return "&c" + seconds + "s";
        }
    }

    public String getFormattedScheduledTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        return sdf.format(new Date(scheduledTime));
    }

    public boolean isTeam1Connected() {
        return team1Connected;
    }

    public void setTeam1Connected(boolean team1Connected) {
        this.team1Connected = team1Connected;
    }

    public boolean isTeam2Connected() {
        return team2Connected;
    }

    public void setTeam2Connected(boolean team2Connected) {
        this.team2Connected = team2Connected;
    }

    public boolean areBothTeamsConnected() {
        return team1Connected && team2Connected;
    }

    public long getForfeitCheckStartTime() {
        return forfeitCheckStartTime;
    }

    public void setForfeitCheckStartTime(long forfeitCheckStartTime) {
        this.forfeitCheckStartTime = forfeitCheckStartTime;
    }

    public boolean isForfeitChecked() {
        return forfeitChecked;
    }

    public void setForfeitChecked(boolean forfeitChecked) {
        this.forfeitChecked = forfeitChecked;
    }

    public String getForfeitReason() {
        return forfeitReason;
    }

    public void setForfeitReason(String forfeitReason) {
        this.forfeitReason = forfeitReason;
    }

    public boolean wasForfeit() {
        return forfeitReason != null && !forfeitReason.isEmpty();
    }
}