package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;

import java.util.HashMap;
import java.util.Map;

public class ClanTagColorMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;
    private final Map<Integer, String> slotToColor;

    public ClanTagColorMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&e&lCOLOR DEL TAG", 54);
        this.plugin = plugin;
        this.clan = clan;
        this.slotToColor = new HashMap<Integer, String>();
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        inventory.setItem(4, createItem(XMaterial.ORANGE_WOOL, "&e&lColor Actual",
                "&7Tu TAG usa el color:",
                clan.getFormattedTag(),
                "",
                "&7Selecciona un nuevo color abajo"));

        addColorOption(10, XMaterial.RED_WOOL, "&4Dark Red", "&4");
        addColorOption(11, XMaterial.RED_WOOL, "&cRed", "&c");
        addColorOption(12, XMaterial.ORANGE_WOOL, "&6Gold", "&6");
        addColorOption(13, XMaterial.YELLOW_WOOL, "&eYellow", "&e");
        addColorOption(14, XMaterial.LIME_WOOL, "&aGreen", "&a");
        addColorOption(15, XMaterial.GREEN_WOOL, "&2Dark Green", "&2");
        addColorOption(16, XMaterial.CYAN_WOOL, "&3Dark Aqua", "&3");

        addColorOption(19, XMaterial.LIGHT_BLUE_WOOL, "&bAqua", "&b");
        addColorOption(20, XMaterial.BLUE_WOOL, "&1Dark Blue", "&1");
        addColorOption(21, XMaterial.BLUE_WOOL, "&9Blue", "&9");
        addColorOption(22, XMaterial.PURPLE_WOOL, "&5Dark Purple", "&5");
        addColorOption(23, XMaterial.MAGENTA_WOOL, "&dLight Purple", "&d");
        addColorOption(24, XMaterial.WHITE_WOOL, "&fWhite", "&f");
        addColorOption(25, XMaterial.GRAY_WOOL, "&7Gray", "&7");

        addColorOption(28, XMaterial.GRAY_WOOL, "&8Dark Gray", "&8");
        addColorOption(29, XMaterial.BLACK_WOOL, "&0Black", "&0");
        addColorOption(30, XMaterial.RED_WOOL, "&4&lBold Dark Red", "&4&l");
        addColorOption(31, XMaterial.RED_WOOL, "&c&lBold Red", "&c&l");
        addColorOption(32, XMaterial.YELLOW_WOOL, "&e&lBold Yellow", "&e&l");
        addColorOption(33, XMaterial.LIME_WOOL, "&a&lBold Green", "&a&l");
        addColorOption(34, XMaterial.LIGHT_BLUE_WOOL, "&b&lBold Aqua", "&b&l");

        inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    private void addColorOption(int slot, XMaterial material, String name, String colorCode) {
        ItemStack item = createItem(material, name,
                "&7Vista previa: " + colorCode + "[" + clan.getTag() + "]",
                "",
                "&eClick para seleccionar");
        inventory.setItem(slot, item);
        slotToColor.put(Integer.valueOf(slot), colorCode);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        if (slot == 49) {
            playSound(XSound.UI_BUTTON_CLICK);
            player.closeInventory();
            new ClanColorMenu(player, plugin, clan).open();
            return;
        }

        String selectedColor = slotToColor.get(Integer.valueOf(slot));
        if (selectedColor != null) {
            playSound(XSound.ENTITY_PLAYER_LEVELUP);

            clan.setColorCode(selectedColor);
            ClanManager.getInstance().saveClan(clan);

            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&a&lCOLOR DEL TAG ACTUALIZADO"));
            player.sendMessage(c("&7Nuevo TAG: " + selectedColor + "[" + clan.getTag() + "]"));
            player.sendMessage(c("&6═══════════════════════════════════"));
        }
    }
}