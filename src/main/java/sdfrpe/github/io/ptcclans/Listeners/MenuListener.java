package sdfrpe.github.io.ptcclans.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import sdfrpe.github.io.ptcclans.Menu.ClanMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {
    private static final Map<UUID, ClanMenu> activeMenus = new HashMap<UUID, ClanMenu>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clicked = event.getClickedInventory();

        if (clicked == null) return;

        ClanMenu menu = activeMenus.get(player.getUniqueId());
        if (menu == null) return;

        if (!clicked.equals(menu.getInventory())) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        menu.handleClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        ClanMenu menu = activeMenus.get(player.getUniqueId());

        if (menu != null) {
            menu.onClose(player);
            activeMenus.remove(player.getUniqueId());
        }
    }

    public static void registerMenu(Player player, ClanMenu menu) {
        activeMenus.put(player.getUniqueId(), menu);
    }

    public static void unregisterMenu(Player player) {
        activeMenus.remove(player.getUniqueId());
    }

    public static ClanMenu getActiveMenu(Player player) {
        return activeMenus.get(player.getUniqueId());
    }
}