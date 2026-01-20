package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanInvitationManager;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanRequestManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanMember;
import sdfrpe.github.io.ptcclans.Models.ClanRole;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;
import sdfrpe.github.io.ptcclans.Utils.PlayerNameCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClanMembersMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;
    private final boolean kickMode;
    private final Map<Integer, UUID> slotToUuid;

    public ClanMembersMenu(Player player, PTCClans plugin, Clan clan, boolean kickMode) {
        super(player, "&6&lMIEMBROS DE " + clan.getTag(), 54);
        this.plugin = plugin;
        this.clan = clan;
        this.kickMode = kickMode;
        this.slotToUuid = new HashMap<Integer, UUID>();
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        List<ClanMember> members = clan.getMembers();
        int slot = 10;

        for (ClanMember member : members) {
            if (slot >= 44) break;
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27 || slot == 35 || slot == 36) {
                slot += 2;
            }

            String memberName = PlayerNameCache.getInstance().getName(member.getUuid());
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            boolean isOnline = memberPlayer != null && memberPlayer.isOnline();

            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

            if (skullMeta != null) {
                if (memberPlayer != null) {
                    skullMeta.setOwner(memberPlayer.getName());
                } else if (!memberName.equals("Desconocido")) {
                    skullMeta.setOwner(memberName);
                }

                String roleColor = "&f";
                String roleName = "Miembro";

                if (member.getRole() == ClanRole.LEADER) {
                    roleColor = "&6";
                    roleName = "Líder";
                } else if (member.getRole() == ClanRole.CO_LEADER) {
                    roleColor = "&e";
                    roleName = "Co-Líder";
                }

                skullMeta.setDisplayName(c(roleColor + "⚫ " + memberName));

                List<String> lore = new ArrayList<String>();
                lore.add(c("&7Rango: " + roleColor + roleName));
                lore.add(c("&7Estado: " + (isOnline ? "&a● En línea" : "&c● Desconectado")));
                lore.add(c(""));
                lore.add(c("&7Kills en guerras: &e" + member.getWarKills()));
                lore.add(c("&7Deaths en guerras: &c" + member.getWarDeaths()));
                lore.add(c("&7K/D: &6" + String.format("%.2f", member.getKDRatio())));

                if (kickMode) {
                    if (member.getRole() == ClanRole.LEADER) {
                        lore.add(c(""));
                        lore.add(c("&c&lNo se puede expulsar al líder"));
                    } else if (member.getRole() == ClanRole.CO_LEADER && !clan.isLeader(player.getUniqueId())) {
                        lore.add(c(""));
                        lore.add(c("&c&lSolo el líder puede expulsar Co-Líderes"));
                    } else {
                        lore.add(c(""));
                        lore.add(c("&c&lClick para EXPULSAR"));
                    }
                } else if (clan.isLeader(player.getUniqueId()) && member.getRole() != ClanRole.LEADER) {
                    lore.add(c(""));
                    if (member.getRole() == ClanRole.CO_LEADER) {
                        lore.add(c("&e&lClick Izquierdo: &7Degradar a Miembro"));
                    } else {
                        lore.add(c("&a&lClick Izquierdo: &7Promover a Co-Líder"));
                    }
                }

                skullMeta.setLore(lore);
                skull.setItemMeta(skullMeta);
            }

            inventory.setItem(slot, skull);
            slotToUuid.put(slot, member.getUuid());
            slot++;
        }

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
            new ClanManageMenu(player, plugin, clan).open();
            return;
        }

        if (clicked.getType() == Material.SKULL_ITEM && slotToUuid.containsKey(slot)) {
            UUID targetUuid = slotToUuid.get(slot);
            ClanMember targetMember = clan.getMember(targetUuid);

            if (targetMember == null) return;

            if (kickMode) {
                handleKick(targetUuid, targetMember);
            } else if (clan.isLeader(player.getUniqueId()) && targetMember.getRole() != ClanRole.LEADER && event.getClick() == ClickType.LEFT) {
                handleRoleChange(targetUuid, targetMember);
            }
        }
    }

    private void handleKick(UUID targetUuid, ClanMember targetMember) {
        if (targetMember.getRole() == ClanRole.LEADER) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&c✖ No puedes expulsar al líder del clan."));
            return;
        }

        if (targetMember.getRole() == ClanRole.CO_LEADER && !clan.isLeader(player.getUniqueId())) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&c✖ Solo el líder puede expulsar a Co-Líderes."));
            return;
        }

        if (!clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER)) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.sendMessage(c("&c✖ No tienes permiso para expulsar miembros."));
            return;
        }

        String targetName = PlayerNameCache.getInstance().getName(targetUuid);

        clan.removeMember(targetUuid);
        ClanManager.getInstance().saveClan(clan);
        ClanInvitationManager.getInstance().removeAllInvitations(targetUuid);
        ClanRequestManager.getInstance().removeAllRequests(targetUuid);

        playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
        player.closeInventory();
        player.sendMessage(c("&6═══════════════════════════════════"));
        player.sendMessage(c("&c&l✔ MIEMBRO EXPULSADO"));
        player.sendMessage(c("&7Jugador: &e" + targetName));
        player.sendMessage(c("&7Ha sido removido del clan"));
        player.sendMessage(c("&6═══════════════════════════════════"));

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(c("&6═══════════════════════════════════"));
            targetPlayer.sendMessage(c("&c&l✖ EXPULSADO DEL CLAN"));
            targetPlayer.sendMessage(c("&7Has sido expulsado de " + clan.getFormattedTag()));
            targetPlayer.sendMessage(c("&7Por: &e" + player.getName()));
            targetPlayer.sendMessage(c("&6═══════════════════════════════════"));
        }

        ClanLogger.info("Miembro expulsado:", targetName, "→", clan.getTag(), "por", player.getName());
    }

    private void handleRoleChange(UUID targetUuid, ClanMember targetMember) {
        String targetName = PlayerNameCache.getInstance().getName(targetUuid);

        if (targetMember.getRole() == ClanRole.CO_LEADER) {
            targetMember.setRole(ClanRole.MEMBER);
            ClanManager.getInstance().saveClan(clan);

            playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&e&l↓ MIEMBRO DEGRADADO"));
            player.sendMessage(c("&7Jugador: &e" + targetName));
            player.sendMessage(c("&7Nuevo rango: &fMiembro"));
            player.sendMessage(c("&6═══════════════════════════════════"));

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(c("&6═══════════════════════════════════"));
                targetPlayer.sendMessage(c("&e&l↓ CAMBIO DE RANGO"));
                targetPlayer.sendMessage(c("&7Has sido degradado a &fMiembro"));
                targetPlayer.sendMessage(c("&7En " + clan.getFormattedTag()));
                targetPlayer.sendMessage(c("&7Por: &e" + player.getName()));
                targetPlayer.sendMessage(c("&6═══════════════════════════════════"));
            }

            ClanLogger.info("Miembro degradado:", targetName, "→ MEMBER en", clan.getTag());

        } else if (targetMember.getRole() == ClanRole.MEMBER) {
            targetMember.setRole(ClanRole.CO_LEADER);
            ClanManager.getInstance().saveClan(clan);

            playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&a&l↑ MIEMBRO PROMOVIDO"));
            player.sendMessage(c("&7Jugador: &e" + targetName));
            player.sendMessage(c("&7Nuevo rango: &eCo-Líder"));
            player.sendMessage(c("&6═══════════════════════════════════"));

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(c("&6═══════════════════════════════════"));
                targetPlayer.sendMessage(c("&a&l↑ PROMOCIÓN"));
                targetPlayer.sendMessage(c("&7¡Felicidades! Has sido promovido"));
                targetPlayer.sendMessage(c("&7Nuevo rango: &eCo-Líder"));
                targetPlayer.sendMessage(c("&7En " + clan.getFormattedTag()));
                targetPlayer.sendMessage(c("&7Por: &e" + player.getName()));
                targetPlayer.sendMessage(c("&6═══════════════════════════════════"));
            }

            ClanLogger.info("Miembro promovido:", targetName, "→ CO_LEADER en", clan.getTag());
        }
    }
}