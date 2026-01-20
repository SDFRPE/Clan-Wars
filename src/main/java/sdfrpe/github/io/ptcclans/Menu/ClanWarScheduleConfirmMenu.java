package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanWarManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanWar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class ClanWarScheduleConfirmMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan challengerClan;
    private final Clan challengedClan;
    private final Calendar scheduledTime;

    public ClanWarScheduleConfirmMenu(Player player, PTCClans plugin, Clan challengerClan, Clan challengedClan, Calendar scheduledTime) {
        super(player, "&6&lCONFIRMAR DESAFÍO", 45);
        this.plugin = plugin;
        this.challengerClan = challengerClan;
        this.challengedClan = challengedClan;
        this.scheduledTime = scheduledTime;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.ORANGE_STAINED_GLASS_PANE);

        SimpleDateFormat fullFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        fullFormat.setTimeZone(TimeZone.getTimeZone("America/Lima"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/Lima"));

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        timeFormat.setTimeZone(TimeZone.getTimeZone("America/Lima"));

        inventory.setItem(4, createItem(XMaterial.DIAMOND_SWORD, "&c&lDESAFÍO DE GUERRA",
                "&7Desafiante: " + challengerClan.getFormattedTag(),
                "&7Desafiado: " + challengedClan.getFormattedTag(),
                "",
                "&7El clan rival tendrá &e10 minutos",
                "&7para aceptar el desafío"));

        inventory.setItem(11, createItem(XMaterial.CLOCK, "&e&lFecha Programada",
                "&7" + dateFormat.format(scheduledTime.getTime()),
                "",
                "&7Hora: &e" + timeFormat.format(scheduledTime.getTime()),
                "&7Zona horaria: &eAmerica/Lima (Perú)",
                "",
                "&7La guerra iniciará en esta fecha"));

        inventory.setItem(13, createItem(XMaterial.PAPER, "&6&lInformación del Rival",
                "&7Nombre: &f" + challengedClan.getName(),
                "&7Tag: " + challengedClan.getFormattedTag(),
                "&7Miembros: &e" + challengedClan.getMemberCount(),
                "",
                "&7Victorias: &a" + challengedClan.getWins(),
                "&7Derrotas: &c" + challengedClan.getLosses(),
                "&7Ratio: &6" + String.format("%.2f", challengedClan.getWinRate()) + "%"));

        inventory.setItem(15, createItem(XMaterial.EMERALD, "&a&lTu Clan",
                "&7Nombre: &f" + challengerClan.getName(),
                "&7Tag: " + challengerClan.getFormattedTag(),
                "&7Miembros: &e" + challengerClan.getMemberCount(),
                "",
                "&7Victorias: &a" + challengerClan.getWins(),
                "&7Derrotas: &c" + challengerClan.getLosses(),
                "&7Ratio: &6" + String.format("%.2f", challengerClan.getWinRate()) + "%"));

        inventory.setItem(29, createItem(XMaterial.LIME_WOOL, "&a&l✔ ENVIAR DESAFÍO",
                "&7Fecha: &e" + fullFormat.format(scheduledTime.getTime()),
                "",
                "&7El desafío será enviado y el clan",
                "&7rival tendrá &e10 minutos &7para aceptar",
                "",
                "&7El desafío es &aGRATUITO",
                "",
                "&a&lClick para CONFIRMAR"));

        inventory.setItem(31, createItem(XMaterial.ARROW, "&e&l← Cambiar Hora",
                "&7Volver a seleccionar la hora",
                "",
                "&eClick para cambiar"));

        inventory.setItem(33, createItem(XMaterial.RED_WOOL, "&c&l✖ CANCELAR",
                "&7Cancelar el desafío",
                "&7No se enviará nada",
                "",
                "&cClick para cancelar"));

        inventory.setItem(40, createItem(XMaterial.BOOK, "&6&lResumen",
                "&7Fecha completa:",
                "&e" + fullFormat.format(scheduledTime.getTime()),
                "",
                "&7Plazo de aceptación: &e10 minutos",
                "&7Zona horaria: &eAmerica/Lima"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 29) {
            playSound(XSound.ENTITY_PLAYER_LEVELUP);
            player.closeInventory();

            long scheduledTimeMillis = scheduledTime.getTimeInMillis();
            ClanWar war = ClanWarManager.getInstance().createChallenge(
                    challengerClan.getTag(),
                    challengedClan.getTag(),
                    scheduledTimeMillis
            );

            if (war != null) {
                SimpleDateFormat fullFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                fullFormat.setTimeZone(TimeZone.getTimeZone("America/Lima"));

                player.sendMessage(c("&6═══════════════════════════════════"));
                player.sendMessage(c("&a&l✔ DESAFÍO ENVIADO"));
                player.sendMessage(c("&7Has desafiado al clan " + challengedClan.getFormattedTag()));
                player.sendMessage(c(""));
                player.sendMessage(c("&7Fecha programada:"));
                player.sendMessage(c("&e" + fullFormat.format(scheduledTime.getTime())));
                player.sendMessage(c("&7Zona horaria: &eAmerica/Lima (Perú)"));
                player.sendMessage(c(""));
                player.sendMessage(c("&7El clan rival tiene &e10 minutos"));
                player.sendMessage(c("&7para aceptar el desafío"));
                player.sendMessage(c("&6═══════════════════════════════════"));
                playSound(XSound.ENTITY_PLAYER_LEVELUP);
            } else {
                player.sendMessage(c("&c✖ Error al enviar el desafío."));
                player.sendMessage(c("&7Verifica que no tengas desafíos pendientes."));
                playSound(XSound.ENTITY_VILLAGER_NO);
            }

        } else if (slot == 31) {
            playSound(XSound.UI_BUTTON_CLICK);
            player.closeInventory();
            new ClanWarScheduleTimeMenu(player, plugin, challengerClan, challengedClan, scheduledTime).open();

        } else if (slot == 33) {
            playSound(XSound.UI_BUTTON_CLICK);
            player.closeInventory();
            new ClanWarChallengeMenu(player, plugin, challengerClan, 0).open();
        }
    }
}