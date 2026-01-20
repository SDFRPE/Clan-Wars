package sdfrpe.github.io.ptcclans.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanMember;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNameCache {
    private static PlayerNameCache instance;
    private final Map<UUID, String> nameCache;

    private PlayerNameCache() {
        this.nameCache = new HashMap<UUID, String>();
    }

    public static PlayerNameCache getInstance() {
        if (instance == null) {
            instance = new PlayerNameCache();
        }
        return instance;
    }

    public void cacheName(UUID uuid, String name) {
        if (uuid == null || name == null) return;
        nameCache.put(uuid, name);
        ClanLogger.debug("Nombre cacheado:", uuid.toString(), "→", name);
    }

    public String getName(UUID uuid) {
        if (uuid == null) return "Desconocido";

        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            String name = onlinePlayer.getName();
            cacheName(uuid, name);
            return name;
        }

        String cachedName = nameCache.get(uuid);
        if (cachedName != null) {
            return cachedName;
        }

        Clan clan = ClanManager.getInstance().getPlayerClan(uuid);
        if (clan != null) {
            ClanMember member = clan.getMember(uuid);
            if (member != null && member.getPlayerName() != null) {
                cacheName(uuid, member.getPlayerName());
                return member.getPlayerName();
            }
        }

        return "Desconocido";
    }

    public void updateClanMemberNames(Clan clan) {
        if (clan == null) return;

        for (ClanMember member : clan.getMembers()) {
            UUID uuid = member.getUuid();
            String cachedName = nameCache.get(uuid);

            if (cachedName != null && member.getPlayerName() == null) {
                member.setPlayerName(cachedName);
                ClanLogger.debug("Nombre actualizado en miembro:", uuid.toString(), "→", cachedName);
            } else if (member.getPlayerName() != null) {
                cacheName(uuid, member.getPlayerName());
            }

            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null) {
                String name = onlinePlayer.getName();
                member.setPlayerName(name);
                cacheName(uuid, name);
            }
        }
    }

    public void loadFromClan(Clan clan) {
        if (clan == null) return;

        for (ClanMember member : clan.getMembers()) {
            if (member.getPlayerName() != null) {
                cacheName(member.getUuid(), member.getPlayerName());
            }
        }
    }

    public void clear() {
        nameCache.clear();
        ClanLogger.debug("Cache de nombres limpiado");
    }

    public int getCacheSize() {
        return nameCache.size();
    }
}