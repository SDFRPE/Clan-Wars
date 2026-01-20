package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Integration.CreditosIntegration;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Models.Clan;

public class ClanDeleteConfirmMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;

    public ClanDeleteConfirmMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&4&lELIMINAR CLAN", 27);
        this.plugin = plugin;
        this.clan = clan;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.RED_STAINED_GLASS_PANE);

        int deletionCost = plugin.getSettings().getClanDeletionCost();
        long currentCredits = CreditosIntegration.getPlayerCredits(player);
        boolean hasEnough = currentCredits >= deletionCost;

        inventory.setItem(4, createItem(XMaterial.BARRIER, "&4&lADVERTENCIA",
                "&c&lEsta acción es IRREVERSIBLE",
                "",
                "&7Clan a eliminar: " + clan.getFormattedTag(),
                "&7Miembros: &e" + clan.getMemberCount(),
                "&7Victorias: &a" + clan.getWins(),
                "&7Derrotas: &c" + clan.getLosses(),
                "",
                "&c&lTODOS los datos serán eliminados"));

        if (hasEnough) {
            inventory.setItem(11, createItem(XMaterial.RED_WOOL, "&4&lCONFIRMAR ELIMINACIÓN",
                    "&7Costo: &e" + deletionCost + " Créditos",
                    "&7Tu saldo: &e" + CreditosIntegration.formatCredits(currentCredits) + " Créditos",
                    "&7Saldo después: &e" + CreditosIntegration.formatCredits(currentCredits - deletionCost) + " Créditos",
                    "",
                    "&c&l¡CUIDADO! No podrás recuperar",
                    "&c&lel clan después de eliminarlo",
                    "",
                    "&eClick para ELIMINAR PERMANENTEMENTE"));
        } else {
            inventory.setItem(11, createItem(XMaterial.BARRIER, "&c&lCRÉDITOS INSUFICIENTES",
                    "&7Necesitas: &e" + deletionCost + " Créditos",
                    "&7Tienes: &e" + CreditosIntegration.formatCredits(currentCredits) + " Créditos",
                    "&7Faltan: &c" + (deletionCost - currentCredits) + " Créditos",
                    "",
                    "&c&lNo puedes eliminar el clan",
                    "&7Juega más partidas para ganar créditos"));
        }

        inventory.setItem(15, createItem(XMaterial.GREEN_WOOL, "&a&lCANCELAR",
                "&7No eliminar el clan",
                "",
                "&eClick para volver"));

        inventory.setItem(13, createItem(XMaterial.PAPER, "&e&lInformación",
                "&7Al eliminar el clan:",
                "",
                "&c● Se eliminará permanentemente",
                "&c● Todos los miembros serán removidos",
                "&c● Se perderán todas las estadísticas",
                "&c● Se cobrarán " + deletionCost + " créditos",
                "",
                "&7Piénsalo bien antes de continuar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 11) {
            int deletionCost = plugin.getSettings().getClanDeletionCost();

            if (!CreditosIntegration.hasCredits(player, deletionCost)) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.closeInventory();
                player.sendMessage(c("&6═══════════════════════════════════"));
                player.sendMessage(c("&c&lCRÉDITOS INSUFICIENTES"));
                player.sendMessage(c("&7Necesitas: &e" + deletionCost + " Créditos"));
                player.sendMessage(c("&7Tienes: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
                player.sendMessage(c("&6═══════════════════════════════════"));
                return;
            }

            if (!CreditosIntegration.removeCredits(player, deletionCost, "Eliminación de clan [" + clan.getTag() + "]")) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.closeInventory();
                player.sendMessage(c("&6═══════════════════════════════════"));
                player.sendMessage(c("&c&lERROR AL PROCESAR PAGO"));
                player.sendMessage(c("&7No se pudo descontar los créditos"));
                player.sendMessage(c("&7El clan NO ha sido eliminado"));
                player.sendMessage(c("&7Intenta nuevamente o contacta a un admin"));
                player.sendMessage(c("&6═══════════════════════════════════"));
                return;
            }

            ClanManager.getInstance().deleteClan(clan.getTag());

            playSound(XSound.ENTITY_ENDER_DRAGON_GROWL);
            player.closeInventory();
            player.sendMessage(c("&6═══════════════════════════════════"));
            player.sendMessage(c("&c&l✖ CLAN ELIMINADO"));
            player.sendMessage(c("&7El clan " + clan.getFormattedTag() + " &7ha sido eliminado"));
            player.sendMessage(c("&7Costo: &e-" + deletionCost + " Créditos"));
            player.sendMessage(c("&7Saldo actual: &e" + CreditosIntegration.formatCredits(CreditosIntegration.getPlayerCredits(player)) + " Créditos"));
            player.sendMessage(c("&6═══════════════════════════════════"));
            playSound(XSound.ENTITY_ITEM_BREAK);

        } else if (slot == 15) {
            playSound(XSound.UI_BUTTON_CLICK);
            player.closeInventory();
            new ClanManageMenu(player, plugin, clan).open();
        }
    }
}