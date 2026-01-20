package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Models.Clan;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ClanWarScheduleDateMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan challengerClan;
    private final Clan challengedClan;
    private final Calendar calendar;
    private final TimeZone timezone;

    public ClanWarScheduleDateMenu(Player player, PTCClans plugin, Clan challengerClan, Clan challengedClan) {
        super(player, "&6&lPROGRAMAR FECHA", 54);
        this.plugin = plugin;
        this.challengerClan = challengerClan;
        this.challengedClan = challengedClan;
        this.timezone = TimeZone.getTimeZone("America/Lima");
        this.calendar = Calendar.getInstance(timezone);
        this.calendar.set(Calendar.HOUR_OF_DAY, 0);
        this.calendar.set(Calendar.MINUTE, 0);
        this.calendar.set(Calendar.SECOND, 0);
        this.calendar.set(Calendar.MILLISECOND, 0);
    }

    public ClanWarScheduleDateMenu(Player player, PTCClans plugin, Clan challengerClan, Clan challengedClan, Calendar cal) {
        super(player, "&6&lPROGRAMAR FECHA", 54);
        this.plugin = plugin;
        this.challengerClan = challengerClan;
        this.challengedClan = challengedClan;
        this.timezone = TimeZone.getTimeZone("America/Lima");
        this.calendar = (Calendar) cal.clone();
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy");
        monthFormat.setTimeZone(timezone);

        inventory.setItem(4, createItem(XMaterial.CLOCK, "&6&lSeleccionar Fecha",
                "&7Mes actual: &e" + monthFormat.format(calendar.getTime()),
                "",
                "&7Desafiante: " + challengerClan.getFormattedTag(),
                "&7Desafiado: " + challengedClan.getFormattedTag(),
                "",
                "&7Zona horaria: &eAmerica/Lima (Perú)"));

        Calendar today = Calendar.getInstance(timezone);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar displayCal = (Calendar) calendar.clone();
        displayCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = displayCal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int startSlot = 19 + (firstDayOfWeek - 1);

        for (int day = 1; day <= daysInMonth; day++) {
            displayCal.set(Calendar.DAY_OF_MONTH, day);

            int slot = startSlot + (day - 1);
            if (slot >= 44) break;

            boolean isPast = displayCal.before(today);
            boolean isToday = displayCal.equals(today);

            XMaterial material;
            String name;
            String[] lore;

            if (isPast) {
                material = XMaterial.RED_STAINED_GLASS_PANE;
                name = "&c&l" + day;
                lore = new String[]{"&cFecha pasada", "&7No disponible"};
            } else if (isToday) {
                material = XMaterial.YELLOW_STAINED_GLASS_PANE;
                name = "&e&l" + day + " &7(Hoy)";
                lore = new String[]{"&eHoy", "&7Click para seleccionar"};
            } else {
                material = XMaterial.GREEN_STAINED_GLASS_PANE;
                name = "&a&l" + day;
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                dayFormat.setTimeZone(timezone);
                lore = new String[]{"&7" + dayFormat.format(displayCal.getTime()), "&aClick para seleccionar"};
            }

            inventory.setItem(slot, createItem(material, name, lore));
        }

        inventory.setItem(48, createItem(XMaterial.ARROW, "&e&l← Mes Anterior"));
        inventory.setItem(50, createItem(XMaterial.ARROW, "&e&lMes Siguiente →"));
        inventory.setItem(49, createItem(XMaterial.BARRIER, "&c&lCancelar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        playSound(XSound.UI_BUTTON_CLICK);

        if (slot == 48) {
            calendar.add(Calendar.MONTH, -1);
            player.closeInventory();
            new ClanWarScheduleDateMenu(player, plugin, challengerClan, challengedClan, calendar).open();
            return;
        }

        if (slot == 50) {
            calendar.add(Calendar.MONTH, 1);
            player.closeInventory();
            new ClanWarScheduleDateMenu(player, plugin, challengerClan, challengedClan, calendar).open();
            return;
        }

        if (slot == 49) {
            playSound(XSound.ENTITY_VILLAGER_NO);
            player.closeInventory();
            new ClanWarConfirmMenu(player, plugin, challengerClan, challengedClan).open();
            return;
        }

        if (slot >= 19 && slot <= 43) {
            Calendar displayCal = (Calendar) calendar.clone();
            displayCal.set(Calendar.DAY_OF_MONTH, 1);

            int firstDayOfWeek = displayCal.get(Calendar.DAY_OF_WEEK);
            int startSlot = 19 + (firstDayOfWeek - 1);

            int day = slot - startSlot + 1;
            int daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH);

            if (day >= 1 && day <= daysInMonth) {
                displayCal.set(Calendar.DAY_OF_MONTH, day);

                Calendar today = Calendar.getInstance(timezone);
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                if (displayCal.before(today)) {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&c✖ No puedes seleccionar una fecha pasada."));
                    return;
                }

                playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
                player.closeInventory();
                new ClanWarScheduleTimeMenu(player, plugin, challengerClan, challengedClan, displayCal).open();
            }
        }
    }
}