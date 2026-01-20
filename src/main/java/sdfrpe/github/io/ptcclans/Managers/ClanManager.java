package sdfrpe.github.io.ptcclans.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Database.ClanAPI;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanMember;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem;
import sdfrpe.github.io.ptcclans.Utils.ClanLogSystem.LogCategory;
import sdfrpe.github.io.ptcclans.Utils.PlayerNameCache;

import java.util.*;

public class ClanManager {
    private static ClanManager instance;
    private final PTCClans plugin;
    private final Gson gson;
    private final ClanAPI clanAPI;
    private final Map<String, Clan> clans;
    private final Map<UUID, String> playerToClan;
    private final Set<String> recentlyDeletedClans;
    private final Map<String, Long> deletionTimestamps;
    private static final long DELETION_CACHE_DURATION = 10000;

    private ClanManager(PTCClans plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.clanAPI = new ClanAPI();
        this.clans = new HashMap<String, Clan>();
        this.playerToClan = new HashMap<UUID, String>();
        this.recentlyDeletedClans = new HashSet<String>();
        this.deletionTimestamps = new HashMap<String, Long>();

        startDeletionCacheCleaner();
    }

    public static ClanManager getInstance() {
        return instance;
    }

    public static void initialize(PTCClans plugin) {
        if (instance == null) {
            instance = new ClanManager(plugin);
            instance.loadAllClans();
        }
    }

    private void startDeletionCacheCleaner() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            deletionTimestamps.entrySet().removeIf(entry -> {
                boolean shouldRemove = (now - entry.getValue()) > DELETION_CACHE_DURATION;
                if (shouldRemove) {
                    recentlyDeletedClans.remove(entry.getKey());
                    ClanLogSystem.debug(LogCategory.CLAN, "Limpiado cache de eliminación para clan:", entry.getKey());
                }
                return shouldRemove;
            });
        }, 200L, 200L);
    }

    public void loadAllClans() {
        try {
            JsonObject response = clanAPI.getAllClans();

            if (response == null) {
                ClanLogSystem.warn(LogCategory.API, "No se pudo conectar a la API para cargar clanes");
                return;
            }

            if (!response.get("error").getAsBoolean()) {
                JsonArray dataArray = response.getAsJsonArray("data");

                for (int i = 0; i < dataArray.size(); i++) {
                    JsonObject clanJson = dataArray.get(i).getAsJsonObject();
                    Clan clan = gson.fromJson(clanJson.toString(), Clan.class);
                    clans.put(clan.getTag(), clan);

                    for (ClanMember member : clan.getMembers()) {
                        playerToClan.put(member.getUuid(), clan.getTag());
                    }

                    populateMemberNames(clan);
                    PlayerNameCache.getInstance().loadFromClan(clan);

                    ClanLogSystem.debug(LogCategory.CLAN, "Clan cargado desde API:", clan.getTag());
                }

                ClanLogSystem.info(LogCategory.CLAN, "Total de clanes cargados desde API:", String.valueOf(clans.size()));
            } else {
                ClanLogSystem.error(LogCategory.API, "Error en respuesta de API al cargar clanes");
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error cargando clanes desde API:", e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateMemberNames(Clan clan) {
        if (clan == null) return;

        boolean needsSave = false;

        for (ClanMember member : clan.getMembers()) {
            if (member.getPlayerName() == null) {
                Player onlinePlayer = Bukkit.getPlayer(member.getUuid());
                if (onlinePlayer != null) {
                    member.setPlayerName(onlinePlayer.getName());
                    PlayerNameCache.getInstance().cacheName(member.getUuid(), onlinePlayer.getName());
                    needsSave = true;
                    ClanLogSystem.debug(LogCategory.CLAN, "Nombre online encontrado:", onlinePlayer.getName());
                } else {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getUuid());
                    if (offlinePlayer != null && offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                        member.setPlayerName(offlinePlayer.getName());
                        PlayerNameCache.getInstance().cacheName(member.getUuid(), offlinePlayer.getName());
                        needsSave = true;
                        ClanLogSystem.debug(LogCategory.CLAN, "Nombre offline encontrado:", offlinePlayer.getName());
                    }
                }
            } else {
                PlayerNameCache.getInstance().cacheName(member.getUuid(), member.getPlayerName());
            }
        }

        if (needsSave) {
            saveClan(clan);
        }
    }

    public void saveClan(Clan clan) {
        try {
            String clanJson = gson.toJson(clan);
            JsonObject response = clanAPI.saveClan(clan.getTag(), clanJson);

            if (response != null && !response.get("error").getAsBoolean()) {
                clans.put(clan.getTag(), clan);

                for (ClanMember member : clan.getMembers()) {
                    playerToClan.put(member.getUuid(), clan.getTag());
                }

                ClanLogSystem.debug(LogCategory.DATABASE, "Clan guardado en API:", clan.getTag());
            } else {
                ClanLogSystem.error(LogCategory.API, "Error guardando clan en API:", clan.getTag());
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.DATABASE, "Error guardando clan:", clan.getTag(), e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteClan(String tag) {
        try {
            ClanLogSystem.info(LogCategory.CLAN, "Iniciando eliminación de clan:", tag);

            Clan clan = clans.get(tag);
            if (clan != null) {
                for (ClanMember member : clan.getMembers()) {
                    Player player = Bukkit.getPlayer(member.getUuid());
                    if (player != null && player.isOnline()) {
                        player.sendMessage("§6═══════════════════════════════════");
                        player.sendMessage("§c§l✖ CLAN ELIMINADO");
                        player.sendMessage("§7El clan " + clan.getFormattedTag() + " §7ha sido eliminado");
                        player.sendMessage("§7Ya no eres miembro");
                        player.sendMessage("§6═══════════════════════════════════");
                    }
                    playerToClan.remove(member.getUuid());
                    ClanLogSystem.debug(LogCategory.CLAN, "Limpiado playerToClan para jugador:", member.getUuid().toString());
                }
            }

            clans.remove(tag);
            ClanLogSystem.debug(LogCategory.CLAN, "Clan removido de memoria:", tag);

            recentlyDeletedClans.add(tag);
            deletionTimestamps.put(tag, System.currentTimeMillis());
            ClanLogSystem.debug(LogCategory.CLAN, "Clan agregado a cache de eliminados:", tag);

            boolean success = clanAPI.deleteClan(tag);

            if (success) {
                ClanInvitationManager.getInstance().removeAllInvitationsByClan(tag);
                ClanRequestManager.getInstance().removeAllRequestsByClan(tag);
                ClanLogSystem.logClanDeleted(tag, "System");
            } else {
                ClanLogSystem.error(LogCategory.API, "Error eliminando clan de API:", tag);
                ClanLogSystem.warn(LogCategory.CLAN, "El clan fue removido de memoria pero puede haber quedado en la API");
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.CLAN, "Error eliminando clan:", tag, e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean createClan(String name, String tag, UUID leaderUuid) {
        if (clanExists(tag)) {
            ClanLogSystem.debug(LogCategory.CLAN, "Intento de crear clan existente:", tag);
            return false;
        }

        if (recentlyDeletedClans.contains(tag)) {
            ClanLogSystem.debug(LogCategory.CLAN, "Intento de crear clan recientemente eliminado:", tag);
            return false;
        }

        if (getPlayerClan(leaderUuid) != null) {
            ClanLogSystem.debug(LogCategory.CLAN, "Jugador ya está en un clan:", leaderUuid.toString());
            return false;
        }

        try {
            Clan clan = new Clan(name, tag, leaderUuid);

            Player leader = Bukkit.getPlayer(leaderUuid);
            if (leader != null) {
                ClanMember leaderMember = clan.getMember(leaderUuid);
                if (leaderMember != null) {
                    leaderMember.setPlayerName(leader.getName());
                    PlayerNameCache.getInstance().cacheName(leaderUuid, leader.getName());
                }
            }

            String clanJson = gson.toJson(clan);
            JsonObject response = clanAPI.saveClan(tag, clanJson);

            if (response != null && !response.get("error").getAsBoolean()) {
                clans.put(tag, clan);
                playerToClan.put(leaderUuid, tag);

                recentlyDeletedClans.remove(tag);
                deletionTimestamps.remove(tag);

                String creatorName = leader != null ? leader.getName() : leaderUuid.toString();
                ClanLogSystem.logClanCreated(tag, name, creatorName);
                return true;
            } else {
                ClanLogSystem.error(LogCategory.API, "Error creando clan en API:", tag);
                return false;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.CLAN, "Error creando clan:", tag, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Clan getClan(String tag) {
        if (recentlyDeletedClans.contains(tag)) {
            ClanLogSystem.debug(LogCategory.CLAN, "Clan solicitado está en lista de eliminados:", tag);
            return null;
        }

        Clan cachedClan = clans.get(tag);
        if (cachedClan != null) {
            return cachedClan;
        }

        return forceLoadClan(tag);
    }

    public Clan forceLoadClan(String tag) {
        if (recentlyDeletedClans.contains(tag)) {
            ClanLogSystem.debug(LogCategory.CLAN, "Clan solicitado está en lista de eliminados:", tag);
            return null;
        }

        try {
            JsonObject response = clanAPI.getClan(tag);

            if (response != null && !response.get("error").getAsBoolean()) {
                JsonObject clanData = response.getAsJsonObject("data");
                Clan clan = gson.fromJson(clanData.toString(), Clan.class);
                clans.put(tag, clan);

                for (ClanMember member : clan.getMembers()) {
                    playerToClan.put(member.getUuid(), clan.getTag());
                }

                populateMemberNames(clan);
                PlayerNameCache.getInstance().loadFromClan(clan);

                ClanLogSystem.debug(LogCategory.SYNC, "Clan forzado a cargar desde API:", tag);
                return clan;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error forzando carga de clan desde API:", tag, e.getMessage());
        }

        return null;
    }

    public void ensureClansLoaded(String... tags) {
        ClanLogSystem.debug(LogCategory.SYNC, "Asegurando clanes cargados:", String.join(", ", tags));
        for (String tag : tags) {
            if (tag != null && !clans.containsKey(tag)) {
                forceLoadClan(tag);
            }
        }
    }

    public Clan getPlayerClan(UUID playerUuid) {
        String clanTag = playerToClan.get(playerUuid);

        if (clanTag != null) {
            if (recentlyDeletedClans.contains(clanTag)) {
                ClanLogSystem.debug(LogCategory.CLAN, "Clan del jugador está recientemente eliminado, limpiando cache:", clanTag);
                playerToClan.remove(playerUuid);
                return null;
            }

            Clan clan = clans.get(clanTag);
            if (clan != null) {
                return clan;
            }
        }

        try {
            JsonObject response = clanAPI.getPlayerClan(playerUuid.toString());

            if (response != null && !response.get("error").getAsBoolean()) {
                JsonObject clanData = response.getAsJsonObject("data");
                Clan clan = gson.fromJson(clanData.toString(), Clan.class);

                if (recentlyDeletedClans.contains(clan.getTag())) {
                    ClanLogSystem.warn(LogCategory.API, "API devolvió clan recientemente eliminado, ignorando:", clan.getTag());
                    return null;
                }

                clans.put(clan.getTag(), clan);

                for (ClanMember member : clan.getMembers()) {
                    playerToClan.put(member.getUuid(), clan.getTag());
                }

                populateMemberNames(clan);
                PlayerNameCache.getInstance().loadFromClan(clan);

                ClanLogSystem.debug(LogCategory.CLAN, "Clan de jugador cargado desde API:", playerUuid.toString());
                return clan;
            }
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error obteniendo clan de jugador desde API:", playerUuid.toString(), e.getMessage());
        }

        return null;
    }

    public boolean clanExists(String tag) {
        if (recentlyDeletedClans.contains(tag)) {
            ClanLogSystem.debug(LogCategory.CLAN, "Verificación de existencia: clan recientemente eliminado:", tag);
            return false;
        }

        if (clans.containsKey(tag)) {
            return true;
        }

        try {
            JsonObject response = clanAPI.getClan(tag);
            return response != null && !response.get("error").getAsBoolean();
        } catch (Exception e) {
            ClanLogSystem.error(LogCategory.API, "Error verificando existencia de clan:", tag, e.getMessage());
            return false;
        }
    }

    public Collection<Clan> getAllClans() {
        return clans.values();
    }

    public boolean addMemberToClan(String tag, UUID playerUuid) {
        Clan clan = getClan(tag);
        if (clan == null) {
            ClanLogSystem.debug(LogCategory.CLAN, "Clan no encontrado para agregar miembro:", tag);
            return false;
        }
        if (getPlayerClan(playerUuid) != null) {
            ClanLogSystem.debug(LogCategory.CLAN, "Jugador ya está en un clan:", playerUuid.toString());
            return false;
        }
        if (clan.getMemberCount() >= plugin.getSettings().getMaxClanMembers()) {
            ClanLogSystem.debug(LogCategory.CLAN, "Clan lleno:", tag);
            return false;
        }

        ClanMember newMember = new ClanMember(playerUuid, sdfrpe.github.io.ptcclans.Models.ClanRole.MEMBER);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            newMember.setPlayerName(player.getName());
            PlayerNameCache.getInstance().cacheName(playerUuid, player.getName());
        }

        clan.addMember(newMember);
        playerToClan.put(playerUuid, tag);
        saveClan(clan);
        ClanLogSystem.info(LogCategory.CLAN, "Miembro agregado a clan:", tag, "-", playerUuid.toString());
        return true;
    }

    public boolean removeMemberFromClan(String tag, UUID playerUuid) {
        Clan clan = getClan(tag);
        if (clan == null) {
            ClanLogSystem.debug(LogCategory.CLAN, "Clan no encontrado para remover miembro:", tag);
            return false;
        }
        if (clan.isLeader(playerUuid)) {
            ClanLogSystem.debug(LogCategory.CLAN, "No se puede remover al líder:", playerUuid.toString());
            return false;
        }

        clan.removeMember(playerUuid);
        playerToClan.remove(playerUuid);
        saveClan(clan);
        ClanLogSystem.info(LogCategory.CLAN, "Miembro removido de clan:", tag, "-", playerUuid.toString());
        return true;
    }

    public void saveAllClans() {
        ClanLogSystem.info(LogCategory.DATABASE, "Guardando todos los clanes en API...");
        int saved = 0;
        int failed = 0;

        for (Clan clan : clans.values()) {
            try {
                String clanJson = gson.toJson(clan);
                JsonObject response = clanAPI.saveClan(clan.getTag(), clanJson);

                if (response != null && !response.get("error").getAsBoolean()) {
                    saved++;
                } else {
                    failed++;
                    ClanLogSystem.error(LogCategory.DATABASE, "Error guardando clan:", clan.getTag());
                }
            } catch (Exception e) {
                failed++;
                ClanLogSystem.error(LogCategory.DATABASE, "Error guardando clan:", clan.getTag(), e.getMessage());
            }
        }

        ClanLogSystem.info(LogCategory.DATABASE, "Clanes guardados:", String.valueOf(saved), "- Fallidos:", String.valueOf(failed));
    }

    public void refreshClanFromAPI(String tag) {
        if (recentlyDeletedClans.contains(tag)) {
            ClanLogSystem.debug(LogCategory.SYNC, "No se puede refrescar clan eliminado:", tag);
            return;
        }

        forceLoadClan(tag);
    }

    public void refreshAllClansFromAPI() {
        ClanLogSystem.info(LogCategory.SYNC, "Refrescando todos los clanes desde API...");
        clans.clear();
        playerToClan.clear();
        loadAllClans();
    }

    public void clearPlayerClanMapping(UUID playerUuid) {
        String removedTag = playerToClan.remove(playerUuid);
        ClanLogSystem.debug(LogCategory.CLAN, "Limpiado playerToClan para:", playerUuid.toString(),
                removedTag != null ? "- Tag: " + removedTag : "- Sin clan");
    }
}