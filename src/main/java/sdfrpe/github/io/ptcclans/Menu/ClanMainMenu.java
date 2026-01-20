package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Utils.ChatInputManager;

public class ClanMainMenu extends ClanMenu {
    private final PTCClans plugin;

    public ClanMainMenu(Player player, PTCClans plugin) {
        super(player, "&6&lSISTEMA DE CLANES", 27);
        this.plugin = plugin;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        Clan playerClan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        if (playerClan != null) {
            inventory.setItem(11, createItem(XMaterial.BLUE_BANNER, "&b&lMi Clan",
                    "&7Nombre: " + playerClan.getFormattedName(),
                    "&7TAG: " + playerClan.getFormattedTag(),
                    "&7Miembros: &e" + playerClan.getMemberCount(),
                    "",
                    "&eClick para ver información"));
        } else {
            inventory.setItem(11, createItem(XMaterial.BARRIER, "&c&lSin Clan",
                    "&7No perteneces a ningún clan",
                    "",
                    "&eÚnete o crea uno!"));
        }

        inventory.setItem(13, createItem(XMaterial.PAPER, "&e&lLista de Clanes",
                "&7Ver todos los clanes registrados",
                "",
                "&eClick para abrir"));

        if (plugin.getSettings().canManageClans()) {
            if (playerClan == null) {
                inventory.setItem(15, createItem(XMaterial.EMERALD, "&a&lCrear Clan",
                        "&7Costo: &e" + plugin.getSettings().getClanCreationCost() + " Créditos",
                        "",
                        "&eClick para crear"));
            } else {
                inventory.setItem(15, createItem(XMaterial.CHEST, "&6&lGestionar Clan",
                        "&7Invitar, expulsar, promover...",
                        "",
                        "&eClick para abrir"));
            }
        } else {
            inventory.setItem(15, createItem(XMaterial.BARRIER, "&c&lGestión Bloqueada",
                    "&7Solo disponible en el lobby",
                    "",
                    "&cConéctate al lobby para gestionar"));
        }

        if (playerClan != null) {
            inventory.setItem(22, createItem(XMaterial.DIAMOND_SWORD, "&c&l⚔Guerras de Clanes",
                    "&7Ver y gestionar guerras",
                    "",
                    "&eClick para abrir"));
        } else {
            inventory.setItem(22, createItem(XMaterial.BARRIER, "&c&lGuerras Bloqueadas",
                    "&7Necesitas estar en un clan",
                    "",
                    "&cÚnete a un clan primero"));
        }

        inventory.setItem(26, createItem(XMaterial.RED_STAINED_GLASS_PANE, "&c&lCerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        playSound(XSound.UI_BUTTON_CLICK);

        Clan playerClan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());

        switch (slot) {
            case 11:
                if (playerClan != null) {
                    player.closeInventory();
                    new ClanInfoMenu(player, plugin, playerClan).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNo perteneces a ningún clan."));
                }
                break;

            case 13:
                player.closeInventory();
                new ClanListMenu(player, plugin, 0).open();
                break;

            case 15:
                if (!plugin.getSettings().canManageClans()) {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cSolo disponible en el lobby."));
                    break;
                }

                if (playerClan == null) {
                    player.closeInventory();
                    ChatInputManager.setPendingAction(player, ChatInputManager.ActionType.CREATE_CLAN_NAME, null);
                    player.sendMessage(c("&6═══════════════════════════════════"));
                    player.sendMessage(c("&e&lCREAR CLAN"));
                    player.sendMessage(c("&7Escribe en el chat el &enombre &7de tu clan"));
                    player.sendMessage(c("&7Ejemplo: &eLos Guerreros, Team Alpha"));
                    player.sendMessage(c("&7Rango: &e" + plugin.getSettings().getMinNameLength() + "-" + plugin.getSettings().getMaxNameLength() + " &7caracteres"));
                    player.sendMessage(c("&7O escribe &ccancel &7para cancelar"));
                    player.sendMessage(c("&6═══════════════════════════════════"));
                } else {
                    player.closeInventory();
                    new ClanManageMenu(player, plugin, playerClan).open();
                }
                break;

            case 22:
                if (playerClan != null) {
                    player.closeInventory();
                    new ClanWarListMenu(player, plugin, playerClan).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cNecesitas estar en un clan."));
                }
                break;

            case 26:
                player.closeInventory();
                playSound(XSound.BLOCK_CHEST_OPEN);
                break;
        }
    }
}