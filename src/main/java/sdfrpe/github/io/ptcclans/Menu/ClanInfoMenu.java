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
import sdfrpe.github.io.ptcclans.Integration.CreditosIntegration;
import sdfrpe.github.io.ptcclans.Managers.ClanInvitationManager;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanRequestManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanMember;
import sdfrpe.github.io.ptcclans.Models.ClanRequest;
import sdfrpe.github.io.ptcclans.Models.ClanRole;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;
import sdfrpe.github.io.ptcclans.Utils.PlayerNameCache;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClanInfoMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;

    public ClanInfoMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, clan.getFormattedTag() + " &7- &eInformación", 54);
        this.plugin = plugin;
        this.clan = clan;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        XMaterial bannerMaterial = XMaterial.BLUE_BANNER;
        if (clan.getColorCode().contains("&a") || clan.getColorCode().contains("&2")) {
            bannerMaterial = XMaterial.GREEN_BANNER;
        } else if (clan.getColorCode().contains("&c") || clan.getColorCode().contains("&4")) {
            bannerMaterial = XMaterial.RED_BANNER;
        } else if (clan.getColorCode().contains("&e") || clan.getColorCode().contains("&6")) {
            bannerMaterial = XMaterial.YELLOW_BANNER;
        }

        inventory.setItem(4, createItem(bannerMaterial, clan.getFormattedTag(),
                "&7Nombre: " + clan.getFormattedName(),
                "&7TAG: " + clan.getFormattedTag(),
                "&7Creado: &e" + formatDate(clan.getCreationDate()),
                "",
                "&7Color TAG: " + clan.getColorCode() + "█████"));

        inventory.setItem(20, createItem(XMaterial.EMERALD, "&a&lEstadísticas Generales",
                "&7Miembros: &e" + clan.getMemberCount() + "&7/&e" + plugin.getSettings().getMaxClanMembers(),
                "",
                "&7Victorias: &a" + clan.getWins(),
                "&7Derrotas: &c" + clan.getLosses(),
                "&7Ratio V/D: &6" + String.format("%.2f", clan.getWinRate()) + "%",
                "",
                "&7Kills: &e" + clan.getKills(),
                "&7Deaths: &c" + clan.getDeaths(),
                "&7K/D: &6" + String.format("%.2f", clan.getKDRatio())));

        String leaderName = PlayerNameCache.getInstance().getName(clan.getLeaderUuid());
        Player leader = Bukkit.getPlayer(clan.getLeaderUuid());

        ItemStack leaderSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) leaderSkull.getItemMeta();
        if (skullMeta != null) {
            if (leader != null) {
                skullMeta.setOwner(leader.getName());
            } else if (!leaderName.equals("Desconocido")) {
                skullMeta.setOwner(leaderName);
            }
            skullMeta.setDisplayName(c("&6&lLíder"));
            java.util.List<String> skullLore = new java.util.ArrayList<String>();
            skullLore.add(c("&7" + leaderName));
            skullMeta.setLore(skullLore);
            leaderSkull.setItemMeta(skullMeta);
        }
        inventory.setItem(22, leaderSkull);

        long coLeaders = 0;
        long members = 0;
        for (ClanMember member : clan.getMembers()) {
            if (member.getRole() == ClanRole.CO_LEADER) {
                coLeaders++;
            } else if (member.getRole() == ClanRole.MEMBER) {
                members++;
            }
        }

        inventory.setItem(24, createItem(XMaterial.PLAYER_HEAD, "&e&lMiembros",
                "&7Líder: &61",
                "&7Co-Líderes: &e" + coLeaders,
                "&7Miembros: &f" + members,
                "&7Total: &a" + clan.getMemberCount(),
                "",
                "&eClick para ver la lista"));

        boolean isPlayerInClan = clan.isMember(player.getUniqueId());
        boolean isLeader = clan.isLeader(player.getUniqueId());
        boolean isCoLeader = clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER);

        if (isPlayerInClan) {
            if (isLeader && plugin.getSettings().canManageClans()) {
                inventory.setItem(38, createItem(XMaterial.CHEST, "&6&lGestionar Clan",
                        "&7Opciones de administración",
                        "",
                        "&eClick para abrir"));
            } else if (plugin.getSettings().canManageClans()) {
                inventory.setItem(38, createItem(XMaterial.RED_WOOL, "&c&l← Salir del Clan",
                        "&7Abandonar el clan",
                        "",
                        "&c&lAdvertencia: &7No podrás volver",
                        "&7a menos que te inviten de nuevo",
                        "",
                        "&eClick para salir"));
            } else {
                inventory.setItem(38, createItem(XMaterial.GREEN_WOOL, "&a&lTu Clan",
                        "&7Eres miembro de este clan"));
            }

            if (isCoLeader) {
                int pendingRequests = ClanRequestManager.getInstance().getRequests(clan.getTag()).size();
                if (pendingRequests > 0) {
                    inventory.setItem(42, createItem(XMaterial.WRITABLE_BOOK, "&e&l📬 Solicitudes Pendientes",
                            "&7Solicitudes: &6" + pendingRequests,
                            "",
                            "&eClick para ver"));
                } else {
                    inventory.setItem(42, createItem(XMaterial.BOOK, "&7&lSin Solicitudes",
                            "&7No hay solicitudes pendientes"));
                }
            }
        } else {
            Clan playerClan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
            if (playerClan == null) {
                if (clan.getMemberCount() >= plugin.getSettings().getMaxClanMembers()) {
                    inventory.setItem(40, createItem(XMaterial.BARRIER, "&c&lClan Lleno",
                            "&7Este clan ha alcanzado el límite",
                            "&7Miembros: &c" + clan.getMemberCount() + "&7/&c" + plugin.getSettings().getMaxClanMembers()));
                } else if (ClanRequestManager.getInstance().hasRequest(clan.getTag(), player.getUniqueId())) {
                    inventory.setItem(40, createItem(XMaterial.YELLOW_WOOL, "&e&lSolicitud Enviada",
                            "&7Ya enviaste una solicitud",
                            "&7Espera la respuesta del líder"));
                } else {
                    inventory.setItem(40, createItem(XMaterial.EMERALD, "&a&l✉ Solicitar Unirse",
                            "&7Envía una solicitud al clan",
                            "&7Costo: &e" + plugin.getSettings().getInvitationCost() + " Créditos",
                            "",
                            "&eClick para solicitar"));
                }
            } else {
                inventory.setItem(40, createItem(XMaterial.BARRIER, "&c&lYa Estás en un Clan",
                        "&7Sal de tu clan actual primero"));
            }
        }

        inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        playSound(XSound.UI_BUTTON_CLICK);

        switch (slot) {
            case 24:
                player.closeInventory();
                new ClanMembersMenu(player, plugin, clan, false).open();
                break;

            case 38:
                if (clan.isLeader(player.getUniqueId()) && plugin.getSettings().canManageClans()) {
                    player.closeInventory();
                    new ClanManageMenu(player, plugin, clan).open();
                } else if (clan.isMember(player.getUniqueId()) && !clan.isLeader(player.getUniqueId()) && clicked.getType() == Material.WOOL) {
                    handleLeave();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }
                break;

            case 40:
                if (clicked.getType() == Material.EMERALD) {
                    handleRequestJoin();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }
                break;

            case 42:
                if (clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER)) {
                    player.closeInventory();
                    new ClanRequestsMenu(player, plugin, clan).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }
                break;

            case 49:
                player.closeInventory();
                new ClanMainMenu(player, plugin).open();
                break;
        }
    }

    private void handleLeave() {
        if (!plugin.getSettings().canManageClans()) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&c✖ Solo disponible en el lobby."));
            player.closeInventory();
            return;
        }

        clan.removeMember(player.getUniqueId());
        ClanManager.getInstance().clearPlayerClanMapping(player.getUniqueId());
        ClanManager.getInstance().saveClan(clan);
        ClanInvitationManager.getInstance().removeAllInvitations(player.getUniqueId());
        ClanRequestManager.getInstance().removeAllRequests(player.getUniqueId());

        playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
        player.closeInventory();
        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&e&l← HAS SALIDO DEL CLAN"));
        player.sendMessage(c("&7Clan: " + clan.getFormattedTag()));
        player.sendMessage(c("&7Ya no eres miembro"));
        player.sendMessage(c("&6═══════════════════════════════════"));

        ClanLogger.info("Jugador salió del clan:", player.getName(), "→", clan.getTag());
    }

    private void handleRequestJoin() {
        if (ClanManager.getInstance().getPlayerClan(player.getUniqueId()) != null) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&c✖ Ya estás en un clan."));
            player.closeInventory();
            return;
        }

        if (clan.getMemberCount() >= plugin.getSettings().getMaxClanMembers()) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&c✖ Este clan está lleno."));
            player.closeInventory();
            return;
        }

        if (ClanRequestManager.getInstance().hasRequest(clan.getTag(), player.getUniqueId())) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&c✖ Ya enviaste una solicitud a este clan."));
            player.closeInventory();
            return;
        }

        int requestCost = plugin.getSettings().getInvitationCost();

        if (!CreditosIntegration.hasCredits(player, requestCost)) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ CRÉDITOS INSUFICIENTES"));
            player.sendMessage(c("&7Necesitas: &e" + requestCost + " Créditos"));
            player.sendMessage(c("&7Tienes: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        if (!CreditosIntegration.removeCredits(player, requestCost, "Solicitud para unirse a clan [" + clan.getTag() + "]")) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ ERROR AL PROCESAR PAGO"));
            player.sendMessage(c("&7No se pudo descontar los créditos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            return;
        }

        try {
            ClanRequest request = new ClanRequest(clan.getTag(), player.getUniqueId());
            ClanRequestManager.getInstance().addRequest(request);
            ClanRequestManager.getInstance().notifyLeaders(clan.getTag(), player.getName());

            playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&a&l✔ SOLICITUD ENVIADA"));
            player.sendMessage(c("&7Clan: " + clan.getFormattedTag()));
            player.sendMessage(c("&7Costo: &e-" + requestCost + " Créditos"));
            player.sendMessage(c("&7Saldo actual: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
            player.sendMessage(c("&7La solicitud expira en &e5 minutos"));
            player.sendMessage(c("&6═══════════════════════════════════"));

            ClanLogger.info("Solicitud enviada:", player.getName(), "→", clan.getTag());
        } catch (Exception e) {
            CreditosIntegration.addCredits(player, requestCost, "Reembolso - Error enviando solicitud a clan [" + clan.getTag() + "]");
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ ERROR AL ENVIAR SOLICITUD"));
            player.sendMessage(c("&7Los créditos han sido reembolsados"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            ClanLogger.error("Error enviando solicitud:", e.getMessage());
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(new Date(timestamp));
    }
}