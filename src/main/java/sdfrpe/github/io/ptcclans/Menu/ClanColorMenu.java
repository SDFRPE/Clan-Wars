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

public class ClanColorMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;

    public ClanColorMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&e&lPERSONALIZAR CLAN", 27);
        this.plugin = plugin;
        this.clan = clan;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        inventory.setItem(11, createItem(XMaterial.NAME_TAG, "&e&lEditar Nombre Formateado",
                "&7Nombre actual:",
                clan.getFormattedName(),
                "",
                "&7Puedes usar códigos de color:",
                "&7&a&a &c&c &e&e &b&b &d&d &f&f",
                "&7&l&l (negrita) &o&o (cursiva) &n&n (subrayado)",
                "&7&m&m (tachado)",
                "",
                "&c&lNO permitido: &k&k (obfuscado)",
                "",
                "&eClick para editar"));

        inventory.setItem(15, createItem(XMaterial.ORANGE_WOOL, "&e&lColor del TAG",
                "&7TAG actual: " + clan.getFormattedTag(),
                "",
                "&7Cambia el color del [TAG]",
                "",
                "&eClick para cambiar"));

        inventory.setItem(22, createItem(XMaterial.PAPER, "&6&lVista Previa",
                "&7TAG: " + clan.getFormattedTag(),
                "&7Nombre: " + clan.getFormattedName(),
                "",
                "&7Combinado: " + clan.getFormattedTag() + " " + clan.getFormattedName()));

        inventory.setItem(26, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        playSound(XSound.UI_BUTTON_CLICK);

        switch (slot) {
            case 11:
                player.closeInventory();
                ChatInputManager.setPendingAction(player, ChatInputManager.ActionType.EDIT_CLAN_NAME, clan.getTag());
                player.sendMessage(c("&6═══════════════════════════════════"));
                player.sendMessage(c("&e&lEDITAR NOMBRE DEL CLAN"));
                player.sendMessage(c("&7Nombre actual: " + clan.getFormattedName()));
                player.sendMessage(c(""));
                player.sendMessage(c("&7Escribe el nuevo nombre con formato"));
                player.sendMessage(c("&7Ejemplo: &a&lMy &c&lClan"));
                player.sendMessage(c("&7Permitido: &a&a &c&c &l&l &o&o &n&n &m&m"));
                player.sendMessage(c("&c&lProhibido: &k&k (obfuscado)"));
                player.sendMessage(c(""));
                player.sendMessage(c("&7O escribe &ccancel &7para cancelar"));
                player.sendMessage(c("&6═══════════════════════════════════"));
                break;

            case 15:
                player.closeInventory();
                new ClanTagColorMenu(player, plugin, clan).open();
                break;

            case 26:
                player.closeInventory();
                new ClanManageMenu(player, plugin, clan).open();
                break;
        }
    }
}