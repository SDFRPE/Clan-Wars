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
import java.util.Collection;
import java.util.List;

public class ClanWarChallengeMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;
    private final int page;
    private static final int ITEMS_PER_PAGE = 28;

    public ClanWarChallengeMenu(Player player, PTCClans plugin, Clan clan, int page) {
        super(player, "&c&lDESAFIAR CLAN &7(Página " + (page + 1) + ")", 54);
        this.plugin = plugin;
        this.clan = clan;
        this.page = page;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        List<Clan> availableClans = new ArrayList<Clan>();
        Collection<Clan> allClans = ClanManager.getInstance().getAllClans();
        for (Clan c : allClans) {
            if (!c.getTag().equals(clan.getTag())) {
                availableClans.add(c);
            }
        }

        if (availableClans.isEmpty()) {
            inventory.setItem(22, createItem(XMaterial.BARRIER, "&c&lNo hay clanes disponibles",
                    "&7No hay otros clanes para desafiar"));
            inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
            return;
        }

        int totalPages = (int) Math.ceil((double) availableClans.size() / ITEMS_PER_PAGE);
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, availableClans.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27 || slot == 35 || slot == 36) {
                slot += 2;
            }
            if (slot >= 44) break;

            Clan targetClan = availableClans.get(i);
            XMaterial material = XMaterial.RED_BANNER;

            if (targetClan.getColorCode().contains("&a") || targetClan.getColorCode().contains("&2")) {
                material = XMaterial.GREEN_BANNER;
            } else if (targetClan.getColorCode().contains("&e") || targetClan.getColorCode().contains("&6")) {
                material = XMaterial.YELLOW_BANNER;
            } else if (targetClan.getColorCode().contains("&b") || targetClan.getColorCode().contains("&3")) {
                material = XMaterial.CYAN_BANNER;
            }

            ItemStack clanItem = createItem(material, targetClan.getFormattedTag(),
                    "&7Nombre: &f" + targetClan.getName(),
                    "&7Miembros: &e" + targetClan.getMemberCount(),
                    "",
                    "&7Victorias: &a" + targetClan.getWins(),
                    "&7Derrotas: &c" + targetClan.getLosses(),
                    "&7Ratio: &6" + String.format("%.2f", targetClan.getWinRate()) + "%",
                    "",
                    "&c&lClick para DESAFIAR");

            inventory.setItem(slot, clanItem);
            slot++;
        }

        inventory.setItem(45, createItem(XMaterial.BOOK, "&e&lInformación",
                "&7Clanes disponibles: &6" + availableClans.size(),
                "&7Página: &6" + (page + 1) + "&7/&6" + totalPages,
                "",
                "&7Los desafíos deben ser aceptados",
                "&7por el clan rival"));

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

        List<Clan> availableClans = new ArrayList<Clan>();
        Collection<Clan> allClans = ClanManager.getInstance().getAllClans();
        for (Clan c : allClans) {
            if (!c.getTag().equals(clan.getTag())) {
                availableClans.add(c);
            }
        }

        if (slot >= 10 && slot <= 43) {
            int index = calculateClanIndex(slot);
            if (index >= 0 && index < availableClans.size()) {
                Clan targetClan = availableClans.get(page * ITEMS_PER_PAGE + index);
                if (targetClan != null) {
                    player.closeInventory();
                    new ClanWarConfirmMenu(player, plugin, clan, targetClan).open();
                }
            }
        } else if (slot == 48 && page > 0) {
            player.closeInventory();
            new ClanWarChallengeMenu(player, plugin, clan, page - 1).open();
        } else if (slot == 50) {
            int totalPages = (int) Math.ceil((double) availableClans.size() / ITEMS_PER_PAGE);
            if (page < totalPages - 1) {
                player.closeInventory();
                new ClanWarChallengeMenu(player, plugin, clan, page + 1).open();
            }
        } else if (slot == 49) {
            player.closeInventory();
            new ClanWarListMenu(player, plugin, clan).open();
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