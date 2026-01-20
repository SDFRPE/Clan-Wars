package sdfrpe.github.io.ptcclans.Placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanMember;

public class PTCClansExpansion extends PlaceholderExpansion {
    private final PTCClans plugin;

    public PTCClansExpansion(PTCClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ptcclans";
    }

    @Override
    public String getAuthor() {
        return "PTCClans";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        if (identifier.equals("has_clan")) {
            return clan != null ? "true" : "false";
        }

        if (clan == null) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "clan_tag":
                return clan.getTag();

            case "clan_name":
                return clan.getName();

            case "clan_display_name":
                return clan.getDisplayName();

            case "clan_formatted_name":
                return clan.getFormattedName();

            case "clan_clean_name":
                return clan.getCleanName();

            case "clan_formatted_tag":
                return clan.getFormattedTag();

            case "clan_color":
                return clan.getColorCode();

            case "clan_members":
                return String.valueOf(clan.getMemberCount());

            case "clan_max_members":
                return String.valueOf(plugin.getSettings().getMaxClanMembers());

            case "clan_wins":
                return String.valueOf(clan.getWins());

            case "clan_losses":
                return String.valueOf(clan.getLosses());

            case "clan_winrate":
                return String.format("%.2f", clan.getWinRate());

            case "clan_kills":
                return String.valueOf(clan.getKills());

            case "clan_deaths":
                return String.valueOf(clan.getDeaths());

            case "clan_kd":
                return String.format("%.2f", clan.getKDRatio());

            case "clan_leader":
                Player leader = Bukkit.getPlayer(clan.getLeaderUuid());
                return leader != null ? leader.getName() : "Unknown";

            case "clan_role":
                ClanMember member = clan.getMember(player.getUniqueId());
                if (member != null) {
                    switch (member.getRole()) {
                        case LEADER:
                            return "Líder";
                        case CO_LEADER:
                            return "Co-Líder";
                        case MEMBER:
                            return "Miembro";
                    }
                }
                return "";

            case "clan_role_color":
                ClanMember mem = clan.getMember(player.getUniqueId());
                if (mem != null) {
                    switch (mem.getRole()) {
                        case LEADER:
                            return "&6";
                        case CO_LEADER:
                            return "&e";
                        case MEMBER:
                            return "&f";
                    }
                }
                return "&7";

            default:
                return null;
        }
    }
}