package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanWarManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Utils.ClanLogger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class ClanWarScheduleTimeMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan challengerClan;
    private final Clan challengedClan;
    private final Calendar selectedDate;
    private final TimeZone timezone;
    private int selectedHour;

    public ClanWarScheduleTimeMenu(Player player, PTCClans plugin, Clan challengerClan, Clan challengedClan, Calendar selectedDate) {
        super(player, "&6&lPROGRAMAR HORA", 54);
        this.plugin = plugin;
        this.challengerClan = challengerClan;
        this.challengedClan = challengedClan;
        this.selectedDate = selectedDate;
        this.timezone = TimeZone.getTimeZone("America/Lima");
        this.selectedHour = -1;
    }

    public ClanWarScheduleTimeMenu(Player player, PTCClans plugin, Clan challengerClan, Clan challengedClan, Calendar selectedDate, int hour) {
        super(player, "&6&lPROGRAMAR HORA", 54);
        this.plugin = plugin;
        this.challengerClan = challengerClan;
        this.challengedClan = challengedClan;
        this.selectedDate = selectedDate;
        this.timezone = TimeZone.getTimeZone("America/Lima");
        this.selectedHour = hour;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(timezone);

        String timeDisplay = selectedHour == -1 ?
                "&c--:00" :
                String.format("&e%02d:00", selectedHour);

        inventory.setItem(4, createItem(XMaterial.CLOCK, "&6&lSeleccionar Hora",
                "&7Fecha: &e" + dateFormat.format(selectedDate.getTime()),
                "&7Hora seleccionada: " + timeDisplay,
                "",
                "&7Desafiante: " + challengerClan.getFormattedTag(),
                "&7Desafiado: " + challengedClan.getFormattedTag(),
                "",
                "&c&lNOTA: Solo horas exactas",
                "&7Los enfrentamientos duran 60 minutos",
                "&7Zona horaria: &eAmerica/Lima (Perú)"));

        int[] hours = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        int[] hourSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38};

        Calendar now = Calendar.getInstance(timezone);
        Calendar testTime = (Calendar) selectedDate.clone();

        boolean isToday = isSameDay(selectedDate, now);

        for (int i = 0; i < hours.length && i < hourSlots.length; i++) {
            int hour = hours[i];
            testTime.set(Calendar.HOUR_OF_DAY, hour);
            testTime.set(Calendar.MINUTE, 0);
            testTime.set(Calendar.SECOND, 0);
            testTime.set(Calendar.MILLISECOND, 0);

            boolean isPast = isToday && testTime.before(now);
            boolean isSelected = selectedHour == hour;

            int warsAtHour = ClanWarManager.getInstance().getWarsCountAtHour(testTime.getTimeInMillis());
            boolean isFull = warsAtHour >= 2;
            boolean clanHasWar = ClanWarManager.getInstance().clanHasWarAtHour(challengerClan.getTag(), testTime.getTimeInMillis()) ||
                    ClanWarManager.getInstance().clanHasWarAtHour(challengedClan.getTag(), testTime.getTimeInMillis());

            XMaterial material;
            String name;
            String[] lore;

            if (isPast) {
                material = XMaterial.RED_STAINED_GLASS_PANE;
                name = "&c&l" + String.format("%02d:00", hour);
                lore = new String[]{"&cHora pasada", "&7No disponible"};
            } else if (clanHasWar) {
                material = XMaterial.ORANGE_STAINED_GLASS_PANE;
                name = "&6&l" + String.format("%02d:00", hour);
                lore = new String[]{
                        "&6&lUno de los clanes ya tiene guerra",
                        "&7Un clan solo puede tener 1 guerra por hora",
                        "",
                        "&cNo disponible para este enfrentamiento"
                };
            } else if (isFull) {
                material = XMaterial.YELLOW_STAINED_GLASS_PANE;
                name = "&e&l" + String.format("%02d:00", hour);
                lore = new String[]{
                        "&e&lHora completa",
                        "&7Ya hay 2 guerras programadas",
                        "&7Máximo: 2 guerras por hora",
                        "",
                        "&cNo disponible"
                };
            } else if (isSelected) {
                material = XMaterial.LIME_DYE;
                name = "&a&l✔ " + String.format("%02d:00", hour);
                lore = new String[]{
                        "&aSeleccionado",
                        "",
                        "&7Guerras en esta hora: &e" + warsAtHour + "&7/&e2",
                        "&7Duración: &e60 minutos",
                        "",
                        "&eClick para confirmar"
                };
            } else {
                material = XMaterial.LIME_STAINED_GLASS_PANE;
                name = "&a&l" + String.format("%02d:00", hour);
                lore = new String[]{
                        "&aDisponible",
                        "",
                        "&7Guerras en esta hora: &e" + warsAtHour + "&7/&e2",
                        "&7Duración: &e60 minutos",
                        "",
                        "&eClick para seleccionar"
                };
            }

            inventory.setItem(hourSlots[i], createItem(material, name, lore));
        }

        inventory.setItem(40, createItem(XMaterial.PAPER, "&6&lInformación",
                "&7- Solo horas exactas (XX:00)",
                "&7- Duración: 60 minutos",
                "&7- Máximo: 2 guerras por hora",
                "&7- Un clan: 1 guerra por hora",
                "",
                "&ePuedes tener múltiples guerras",
                "&een diferentes horas"));

        if (selectedHour != -1) {
            inventory.setItem(49, createItem(XMaterial.LIME_WOOL, "&a&l✔ CONFIRMAR",
                    "&7Hora seleccionada: &e" + String.format("%02d:00", selectedHour),
                    "&7Duración: &e60 minutos",
                    "",
                    "&aClick para continuar"));
        } else {
            inventory.setItem(49, createItem(XMaterial.RED_WOOL, "&c&lSelecciona una hora",
                    "&7Debes seleccionar una hora",
                    "&7antes de continuar"));
        }

        inventory.setItem(48, createItem(XMaterial.ARROW, "&e&l← Volver a Fecha"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        playSound(XSound.UI_BUTTON_CLICK);

        if (slot == 48) {
            player.closeInventory();
            new ClanWarScheduleDateMenu(player, plugin, challengerClan, challengedClan, selectedDate).open();
            return;
        }

        if (slot == 49) {
            if (selectedHour == -1) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.sendMessage(c("&c✖ Debes seleccionar una hora."));
                return;
            }

            Calendar finalTime = (Calendar) selectedDate.clone();
            finalTime.set(Calendar.HOUR_OF_DAY, selectedHour);
            finalTime.set(Calendar.MINUTE, 0);
            finalTime.set(Calendar.SECOND, 0);
            finalTime.set(Calendar.MILLISECOND, 0);

            Calendar now = Calendar.getInstance(timezone);
            if (finalTime.before(now)) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.sendMessage(c("&c✖ La fecha y hora seleccionada ya pasó."));
                player.sendMessage(c("&7Por favor selecciona una fecha/hora futura."));
                return;
            }

            long finalTimeMillis = finalTime.getTimeInMillis();

            int warsAtHour = ClanWarManager.getInstance().getWarsCountAtHour(finalTimeMillis);
            if (warsAtHour >= 2) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.sendMessage(c("&c✖ Esta hora ya tiene el máximo de guerras (2)."));
                player.sendMessage(c("&7Por favor selecciona otra hora."));
                return;
            }

            if (ClanWarManager.getInstance().clanHasWarAtHour(challengerClan.getTag(), finalTimeMillis)) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.sendMessage(c("&c✖ Tu clan ya tiene una guerra programada a esta hora."));
                player.sendMessage(c("&7Un clan solo puede tener 1 guerra por hora."));
                player.sendMessage(c("&7Puedes programar otra guerra en una hora diferente."));
                return;
            }

            if (ClanWarManager.getInstance().clanHasWarAtHour(challengedClan.getTag(), finalTimeMillis)) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.sendMessage(c("&c✖ El clan rival ya tiene una guerra programada a esta hora."));
                player.sendMessage(c("&7Por favor selecciona otra hora."));
                return;
            }

            playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
            player.closeInventory();
            new ClanWarScheduleConfirmMenu(player, plugin, challengerClan, challengedClan, finalTime).open();
            return;
        }

        int[] hours = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        int[] hourSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38};

        for (int i = 0; i < hourSlots.length; i++) {
            if (slot == hourSlots[i]) {
                int hour = hours[i];

                Calendar now = Calendar.getInstance(timezone);
                Calendar testTime = (Calendar) selectedDate.clone();
                testTime.set(Calendar.HOUR_OF_DAY, hour);
                testTime.set(Calendar.MINUTE, 0);
                testTime.set(Calendar.SECOND, 0);
                testTime.set(Calendar.MILLISECOND, 0);

                boolean isToday = isSameDay(selectedDate, now);
                boolean isPast = isToday && testTime.before(now);

                if (isPast) {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&c✖ Esta hora ya pasó."));
                    return;
                }

                long testTimeMillis = testTime.getTimeInMillis();

                int warsAtHour = ClanWarManager.getInstance().getWarsCountAtHour(testTimeMillis);
                if (warsAtHour >= 2) {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&c✖ Esta hora ya tiene el máximo de guerras (2)."));
                    player.sendMessage(c("&7Por favor selecciona otra hora."));
                    return;
                }

                if (ClanWarManager.getInstance().clanHasWarAtHour(challengerClan.getTag(), testTimeMillis)) {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&c✖ Tu clan ya tiene una guerra programada a esta hora."));
                    player.sendMessage(c("&7Un clan solo puede tener 1 guerra por hora."));
                    player.sendMessage(c("&7Puedes programar otra guerra en una hora diferente."));
                    return;
                }

                if (ClanWarManager.getInstance().clanHasWarAtHour(challengedClan.getTag(), testTimeMillis)) {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&c✖ El clan rival ya tiene una guerra programada a esta hora."));
                    player.sendMessage(c("&7Por favor selecciona otra hora."));
                    return;
                }

                selectedHour = hour;
                playSound(XSound.ENTITY_EXPERIENCE_ORB_PICKUP);
                player.closeInventory();
                new ClanWarScheduleTimeMenu(player, plugin, challengerClan, challengedClan, selectedDate, selectedHour).open();
                return;
            }
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}