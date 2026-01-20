package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanInvitationManager;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanRequestManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanMember;
import sdfrpe.github.io.ptcclans.Models.ClanRequest;
import sdfrpe.github.io.ptcclans.Models.ClanRole;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;
import sdfrpe.github.io.ptcclans.Utils.PlayerNameCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClanRequestsMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;
    private final Map<Integer, UUID> slotToUuid;

    public ClanRequestsMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&e&lSOLICITUDES PENDIENTES", 54);
        this.plugin = plugin;
        this.clan = clan;
        this.slotToUuid = new HashMap<Integer, UUID>();
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        List<ClanRequest> requests = ClanRequestManager.getInstance().getRequests(clan.getTag());

        if (requests.isEmpty()) {
            inventory.setItem(22, createItem(XMaterial.BARRIER, "&c&lSin Solicitudes",
                    "&7No hay solicitudes pendientes"));
            inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
            return;
        }

        int slot = 10;
        for (ClanRequest request : requests) {
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27 || slot == 35 || slot == 36) {
                slot += 2;
            }
            if (slot >= 44) break;

            String requesterName = PlayerNameCache.getInstance().getName(request.getRequesterUuid());
            Player requester = Bukkit.getPlayer(request.getRequesterUuid());

            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            if (skullMeta != null) {
                if (requester != null) {
                    skullMeta.setOwner(requester.getName());
                } else if (!requesterName.equals("Desconocido")) {
                    skullMeta.setOwner(requesterName);
                }
                skullMeta.setDisplayName(c("&e" + requesterName));
                java.util.List<String> lore = new java.util.ArrayList<String>();
                lore.add(c("&7UUID: &f" + request.getRequesterUuid().toString().substring(0, 8) + "..."));
                lore.add(c("&7Estado: " + (requester != null && requester.isOnline() ? "&aConectado" : "&cDesconectado")));
                lore.add(c(""));
                lore.add(c("&aClick izquierdo: &7Aceptar"));
                lore.add(c("&cClick derecho: &7Rechazar"));
                skullMeta.setLore(lore);
                skull.setItemMeta(skullMeta);
            }

            inventory.setItem(slot, skull);
            slotToUuid.put(Integer.valueOf(slot), request.getRequesterUuid());
            slot++;
        }

        inventory.setItem(45, createItem(XMaterial.BOOK, "&e&lInformación",
                "&7Solicitudes: &6" + requests.size(),
                "",
                "&7Click izquierdo: &aAceptar",
                "&7Click derecho: &cRechazar"));

        inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        if (slot == 49) {
            playSound(XSound.UI_BUTTON_CLICK);
            player.closeInventory();
            new ClanInfoMenu(player, plugin, clan).open();
            return;
        }

        UUID requesterUuid = slotToUuid.get(Integer.valueOf(slot));
        if (requesterUuid != null) {
            if (event.isLeftClick()) {
                handleAcceptRequest(requesterUuid);
            } else if (event.isRightClick()) {
                handleDenyRequest(requesterUuid);
            }
        }
    }

    private void handleAcceptRequest(UUID requesterUuid) {
        if (!ClanRequestManager.getInstance().hasRequest(clan.getTag(), requesterUuid)) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&cEsta solicitud ya no existe."));
            player.closeInventory();
            new ClanRequestsMenu(player, plugin, clan).open();
            return;
        }

        if (ClanManager.getInstance().getPlayerClan(requesterUuid) != null) {
            ClanRequestManager.getInstance().removeRequest(clan.getTag(), requesterUuid);
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&cEl jugador ya está en otro clan."));
            player.closeInventory();
            new ClanRequestsMenu(player, plugin, clan).open();
            return;
        }

        if (clan.getMemberCount() >= plugin.getSettings().getMaxClanMembers()) {
            ClanRequestManager.getInstance().removeRequest(clan.getTag(), requesterUuid);
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&cEl clan está lleno."));
            player.closeInventory();
            new ClanRequestsMenu(player, plugin, clan).open();
            return;
        }

        String requesterName = PlayerNameCache.getInstance().getName(requesterUuid);

        ClanMember newMember = new ClanMember(requesterUuid, ClanRole.MEMBER);
        newMember.setPlayerName(requesterName);

        clan.addMember(newMember);
        ClanManager.getInstance().saveClan(clan);
        ClanRequestManager.getInstance().removeAllRequests(requesterUuid);
        ClanInvitationManager.getInstance().removeAllInvitations(requesterUuid);

        playSound(XSound.ENTITY_PLAYER_LEVELUP);
        player.sendMessage(c("&aHas aceptado a &e" + requesterName + " &aen el clan."));

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(c("&6═══════════════════════════════════"));
            requester.sendMessage(c("&a&lSOLICITUD ACEPTADA"));
            requester.sendMessage(c("&7Has sido aceptado en " + clan.getFormattedTag()));
            requester.sendMessage(c("&7Miembros: &e" + clan.getMemberCount() + "&7/&e" + plugin.getSettings().getMaxClanMembers()));
            requester.sendMessage(c("&6═══════════════════════════════════"));
        }

        ClanLogger.info("Solicitud aceptada:", requesterName, "→", clan.getTag(), "por", player.getName());

        player.closeInventory();
        new ClanRequestsMenu(player, plugin, clan).open();
    }

    private void handleDenyRequest(UUID requesterUuid) {
        String requesterName = PlayerNameCache.getInstance().getName(requesterUuid);

        ClanRequestManager.getInstance().removeRequest(clan.getTag(), requesterUuid);

        playSound(XSound.UI_BUTTON_CLICK);
        player.sendMessage(c("&c✖ Has rechazado a &e" + requesterName + "&c."));

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(c("&6═══════════════════════════════════"));
            requester.sendMessage(c("&c&lSOLICITUD RECHAZADA"));
            requester.sendMessage(c("&7Tu solicitud a " + clan.getFormattedTag() + " &7fue rechazada"));
            requester.sendMessage(c("&6═══════════════════════════════════"));
        }

        ClanLogger.info("Solicitud rechazada:", requesterName, "→", clan.getTag(), "por", player.getName());

        player.closeInventory();
        new ClanRequestsMenu(player, plugin, clan).open();
    }
}