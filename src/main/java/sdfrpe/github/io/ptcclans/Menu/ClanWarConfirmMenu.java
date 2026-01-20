package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Models.Clan;

public class ClanWarConfirmMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan challengerClan;
    private final Clan challengedClan;

    public ClanWarConfirmMenu(Player player, PTCClans plugin, Clan challengerClan, Clan challengedClan) {
        super(player, "&c&lCONFIRMAR DESAFÍO", 27);
        this.plugin = plugin;
        this.challengerClan = challengerClan;
        this.challengedClan = challengedClan;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.RED_STAINED_GLASS_PANE);

        inventory.setItem(4, createItem(XMaterial.DIAMOND_SWORD, "&c&lDESAFÍO DE GUERRA",
                "&7Tu clan: " + challengerClan.getFormattedTag(),
                "&7VS",
                "&7Clan rival: " + challengedClan.getFormattedTag(),
                "",
                "&7Debes programar la fecha y hora",
                "&7para el enfrentamiento"));

        inventory.setItem(11, createItem(XMaterial.CLOCK, "&6&l⚡ PROGRAMAR HORARIO",
                "&7Establece la fecha y hora",
                "&7para el enfrentamiento",
                "",
                "&7El clan rival tendrá &e10 minutos",
                "&7para aceptar el desafío",
                "",
                "&7Zona horaria: &eAmerica/Lima (Perú)",
                "",
                "&e&lClick para programar"));

        inventory.setItem(15, createItem(XMaterial.RED_WOOL, "&c&lCANCELAR",
                "&7Cancelar el desafío",
                "",
                "&eClick para volver"));

        inventory.setItem(13, createItem(XMaterial.PAPER, "&e&lInformación del Rival",
                "&7Nombre: &f" + challengedClan.getName(),
                "&7Tag: " + challengedClan.getFormattedTag(),
                "&7Miembros: &e" + challengedClan.getMemberCount(),
                "",
                "&7Victorias: &a" + challengedClan.getWins(),
                "&7Derrotas: &c" + challengedClan.getLosses(),
                "&7Ratio: &6" + String.format("%.2f", challengedClan.getWinRate()) + "%",
                "",
                "&7K/D: &6" + String.format("%.2f", challengedClan.getKDRatio())));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 11) {
            playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
            player.closeInventory();
            new ClanWarScheduleDateMenu(player, plugin, challengerClan, challengedClan).open();

        } else if (slot == 15) {
            playSound(XSound.UI_BUTTON_CLICK);
            player.closeInventory();
            new ClanWarChallengeMenu(player, plugin, challengerClan, 0).open();
        }
    }
}