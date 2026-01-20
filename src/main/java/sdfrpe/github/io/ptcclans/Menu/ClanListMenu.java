package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;

import java.util.ArrayList;
import java.util.List;

public class ClanListMenu extends ClanMenu {
    private final PTCClans plugin;
    private final int page;
    private static final int ITEMS_PER_PAGE = 28;

    public ClanListMenu(Player player, PTCClans plugin, int page) {
        super(player, "&6&lLISTA DE CLANES &7(Página " + (page + 1) + ")", 54);
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        List<Clan> allClans = new ArrayList<Clan>(ClanManager.getInstance().getAllClans());

        if (allClans.isEmpty()) {
            inventory.setItem(22, createItem(XMaterial.BARRIER, "&c&lNo hay clanes registrados",
                    "&7Sé el primero en crear uno!"));
            inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
            return;
        }

        int totalPages = (int) Math.ceil((double) allClans.size() / ITEMS_PER_PAGE);
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allClans.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27 || slot == 35 || slot == 36) {
                slot += 2;
            }
            if (slot >= 44) break;

            Clan clan = allClans.get(i);
            XMaterial material = XMaterial.BLUE_BANNER;

            if (clan.getColorCode().contains("&a") || clan.getColorCode().contains("&2")) {
                material = XMaterial.GREEN_BANNER;
            } else if (clan.getColorCode().contains("&c") || clan.getColorCode().contains("&4")) {
                material = XMaterial.RED_BANNER;
            } else if (clan.getColorCode().contains("&e") || clan.getColorCode().contains("&6")) {
                material = XMaterial.YELLOW_BANNER;
            }

            ItemStack clanItem = createItem(material, clan.getFormattedTag(),
                    "&7Nombre: &f" + clan.getName(),
                    "&7Miembros: &e" + clan.getMemberCount() + "&7/&e" + plugin.getSettings().getMaxClanMembers(),
                    "",
                    "&7Victorias: &a" + clan.getWins(),
                    "&7Derrotas: &c" + clan.getLosses(),
                    "&7Ratio V/D: &6" + String.format("%.2f", clan.getWinRate()) + "%",
                    "",
                    "&7K/D: &e" + clan.getKills() + "&7/&c" + clan.getDeaths(),
                    "",
                    "&eClick para ver más información");

            inventory.setItem(slot, clanItem);
            slot++;
        }

        inventory.setItem(45, createItem(XMaterial.BOOK, "&e&lInformación",
                "&7Total de clanes: &6" + allClans.size(),
                "&7Página: &6" + (page + 1) + "&7/&6" + totalPages));

        if (page > 0) {
            inventory.setItem(48, createItem(XMaterial.ARROW, "&a&l← Página Anterior"));
        }

        if (page < totalPages - 1) {
            inventory.setItem(50, createItem(XMaterial.ARROW, "&a&lPágina Siguiente →"));
        }

        inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        playSound(XSound.UI_BUTTON_CLICK);

        List<Clan> allClans = new ArrayList<Clan>(ClanManager.getInstance().getAllClans());

        if (slot >= 10 && slot <= 43) {
            int index = calculateClanIndex(slot);
            if (index >= 0 && index < allClans.size()) {
                Clan selectedClan = allClans.get(page * ITEMS_PER_PAGE + index);
                if (selectedClan != null) {
                    player.closeInventory();
                    new ClanInfoMenu(player, plugin, selectedClan).open();
                }
            }
        } else if (slot == 48 && page > 0) {
            player.closeInventory();
            new ClanListMenu(player, plugin, page - 1).open();
        } else if (slot == 50) {
            int totalPages = (int) Math.ceil((double) allClans.size() / ITEMS_PER_PAGE);
            if (page < totalPages - 1) {
                player.closeInventory();
                new ClanListMenu(player, plugin, page + 1).open();
            }
        } else if (slot == 49) {
            player.closeInventory();
            new ClanMainMenu(player, plugin).open();
        }
    }

    private int calculateClanIndex(int slot) {
        int[] validSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < validSlots.length; i++) {
            if (validSlots[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}