package sdfrpe.github.io.ptcclans.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanMember;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;
import sdfrpe.github.io.ptcclans.Utils.PlayerNameCache;

public class PlayerConnectionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerNameCache.getInstance().cacheName(player.getUniqueId(), player.getName());

        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null) {
                boolean needsSave = false;

                if (member.getPlayerName() == null) {
                    member.setPlayerName(player.getName());
                    needsSave = true;
                    ClanLogger.debug("Nombre guardado en miembro:", player.getName());
                } else if (!member.getPlayerName().equals(player.getName())) {
                    member.setPlayerName(player.getName());
                    needsSave = true;
                    ClanLogger.debug("Nombre actualizado en miembro:", player.getName());
                }

                if (needsSave) {
                    ClanManager.getInstance().saveClan(clan);
                }
            }
        }
    }
}