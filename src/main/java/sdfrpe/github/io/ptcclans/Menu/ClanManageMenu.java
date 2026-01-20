package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanRole;
import sdfrpe.github.io.ptcclans.Utils.ChatInputManager;

public class ClanManageMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;

    public ClanManageMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&6&lGESTIONAR CLAN", 45);
        this.plugin = plugin;
        this.clan = clan;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        boolean isLeader = clan.isLeader(player.getUniqueId());
        boolean isCoLeader = clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER);

        if (isCoLeader) {
            inventory.setItem(11, createItem(XMaterial.PLAYER_HEAD, "&a&lInvitar Jugador",
                    "&7Invita un jugador a tu clan",
                    "&7Costo: &6" + plugin.getSettings().getInvitationCost() + " Coins",
                    "",
                    "&eClick para abrir"));
        } else {
            inventory.setItem(11, createItem(XMaterial.BARRIER, "&c&lInvitar Jugador",
                    "&7Requiere: &eCo-Líder"));
        }

        if (isCoLeader) {
            inventory.setItem(13, createItem(XMaterial.IRON_SWORD, "&c&lExpulsar Miembro",
                    "&7Remueve un miembro del clan",
                    "",
                    "&eClick para abrir"));
        } else {
            inventory.setItem(13, createItem(XMaterial.BARRIER, "&c&lExpulsar Miembro",
                    "&7Requiere: &eCo-Líder"));
        }

        if (isLeader) {
            inventory.setItem(15, createItem(XMaterial.GOLD_INGOT, "&6&lPromover/Degradar",
                    "&7Cambia el rango de miembros",
                    "",
                    "&eClick para abrir"));
        } else {
            inventory.setItem(15, createItem(XMaterial.BARRIER, "&c&lPromover/Degradar",
                    "&7Requiere: &6Líder"));
        }

        if (isCoLeader) {
            inventory.setItem(20, createItem(XMaterial.ORANGE_WOOL, "&e&lCambiar Color",
                    "&7Personaliza el color del clan",
                    "&7Color actual: " + clan.getColorCode() + "█████",
                    "",
                    "&eClick para cambiar"));
        } else {
            inventory.setItem(20, createItem(XMaterial.BARRIER, "&c&lCambiar Color",
                    "&7Requiere: &eCo-Líder"));
        }

        if (isLeader) {
            inventory.setItem(22, createItem(XMaterial.DIAMOND, "&b&lTransferir Liderazgo",
                    "&7Transfiere el liderazgo del clan",
                    "",
                    "&c&l¡Cuidado! &7Perderás el liderazgo",
                    "&7Pasarás a ser miembro normal",
                    "",
                    "&eClick para transferir"));
        } else {
            inventory.setItem(22, createItem(XMaterial.BARRIER, "&c&lTransferir Liderazgo",
                    "&7Requiere: &6Líder"));
        }

        inventory.setItem(24, createItem(XMaterial.PLAYER_HEAD, "&e&lVer Miembros",
                "&7Lista completa de miembros",
                "",
                "&eClick para ver"));

        if (isLeader) {
            inventory.setItem(31, createItem(XMaterial.RED_WOOL, "&4&lEliminar Clan",
                    "&7Elimina permanentemente el clan",
                    "&7Costo: &6" + plugin.getSettings().getClanDeletionCost() + " Coins",
                    "",
                    "&c&l¡ADVERTENCIA! &7Esta acción es irreversible",
                    "",
                    "&eClick para confirmar"));
        } else {
            inventory.setItem(31, createItem(XMaterial.BARRIER, "&c&lEliminar Clan",
                    "&7Requiere: &6Líder"));
        }

        inventory.setItem(40, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        boolean isLeader = clan.isLeader(player.getUniqueId());
        boolean isCoLeader = clan.hasPermission(player.getUniqueId(), ClanRole.CO_LEADER);

        switch (slot) {
            case 11:
                if (isCoLeader) {
                    playSound(XSound.UI_BUTTON_CLICK);
                    player.closeInventory();
                    ChatInputManager.setPendingAction(player, ChatInputManager.ActionType.INVITE_MEMBER, null);
                    player.sendMessage(c("&6═══════════════════════════════════"));
                    player.sendMessage(c("&e&lINVITAR JUGADOR"));
                    player.sendMessage(c("&7Escribe el nombre del jugador a invitar"));
                    player.sendMessage(c("&7Costo: &6" + plugin.getSettings().getInvitationCost() + " Coins"));
                    player.sendMessage(c("&7O escribe &ccancel &7para cancelar"));
                    player.sendMessage(c("&6═══════════════════════════════════"));
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNecesitas ser Co-Líder o superior."));
                }
                break;

            case 13:
                if (isCoLeader) {
                    playSound(XSound.UI_BUTTON_CLICK);
                    player.closeInventory();
                    new ClanMembersMenu(player, plugin, clan, true).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNecesitas ser Co-Líder o superior."));
                }
                break;

            case 15:
                if (isLeader) {
                    playSound(XSound.UI_BUTTON_CLICK);
                    player.closeInventory();
                    new ClanMembersMenu(player, plugin, clan, false).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNecesitas ser Líder."));
                }
                break;

            case 20:
                if (isCoLeader) {
                    playSound(XSound.UI_BUTTON_CLICK);
                    player.closeInventory();
                    new ClanColorMenu(player, plugin, clan).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNecesitas ser Co-Líder o superior."));
                }
                break;

            case 22:
                if (isLeader) {
                    playSound(XSound.UI_BUTTON_CLICK);
                    player.closeInventory();
                    ChatInputManager.setPendingAction(player, ChatInputManager.ActionType.TRANSFER_LEADERSHIP, null);
                    player.sendMessage(c("&6═══════════════════════════════════"));
                    player.sendMessage(c("&b&lTRANSFERIR LIDERAZGO"));
                    player.sendMessage(c("&7Escribe el nombre del nuevo líder"));
                    player.sendMessage(c("&7Debe ser un miembro del clan"));
                    player.sendMessage(c(""));
                    player.sendMessage(c("&c&l¡ADVERTENCIA!"));
                    player.sendMessage(c("&7Perderás el liderazgo del clan"));
                    player.sendMessage(c("&7Pasarás a ser miembro normal"));
                    player.sendMessage(c(""));
                    player.sendMessage(c("&7O escribe &ccancel &7para cancelar"));
                    player.sendMessage(c("&6═══════════════════════════════════"));
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNecesitas ser Líder."));
                }
                break;

            case 24:
                playSound(XSound.UI_BUTTON_CLICK);
                player.closeInventory();
                new ClanMembersMenu(player, plugin, clan, false).open();
                break;

            case 31:
                if (isLeader) {
                    playSound(XSound.UI_BUTTON_CLICK);
                    player.closeInventory();
                    new ClanDeleteConfirmMenu(player, plugin, clan).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNecesitas ser Líder."));
                }
                break;

            case 40:
                playSound(XSound.UI_BUTTON_CLICK);
                player.closeInventory();
                new ClanInfoMenu(player, plugin, clan).open();
                break;

            default:
                if (clicked.getType() != Material.STAINED_GLASS_PANE && clicked.getType() != Material.THIN_GLASS) {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }
                break;
        }
    }
}