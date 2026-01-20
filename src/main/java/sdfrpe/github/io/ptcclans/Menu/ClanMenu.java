package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sdfrpe.github.io.ptcclans.Listeners.MenuListener;

import java.util.ArrayList;
import java.util.List;

public abstract class ClanMenu {
    protected Inventory inventory;
    protected Player player;

    public ClanMenu(Player player, String title, int size) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, size, c(title));
    }

    public abstract void setMenuItems();

    public abstract void handleClick(InventoryClickEvent event);

    public void open() {
        setMenuItems();
        MenuListener.registerMenu(player, this);
        player.openInventory(inventory);
        playSound(XSound.BLOCK_CHEST_OPEN);
    }

    public void onClose(Player player) {
    }

    protected ItemStack createItem(XMaterial material, String name, String... lore) {
        ItemStack item = material.parseItem();
        if (item == null) {
            item = new ItemStack(Material.STONE);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(c(name));
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<String>();
                for (String line : lore) {
                    loreList.add(c(line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createItem(XMaterial material, int amount, String name, String... lore) {
        ItemStack item = createItem(material, name, lore);
        item.setAmount(amount);
        return item;
    }

    protected void fillBorder(XMaterial material) {
        ItemStack borderItem = createItem(material, " ");
        int size = inventory.getSize();

        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, borderItem);
            }
        }
    }

    protected void playSound(XSound sound) {
        sound.play(player);
    }

    protected String c(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public Inventory getInventory() {
        return inventory;
    }
}