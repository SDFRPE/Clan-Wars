package sdfrpe.github.io.ptcclans.Menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import sdfrpe.github.io.ptcclans.PTCClans;
import sdfrpe.github.io.ptcclans.Managers.ClanManager;
import sdfrpe.github.io.ptcclans.Managers.ClanWarManager;
import sdfrpe.github.io.ptcclans.Models.Clan;
import sdfrpe.github.io.ptcclans.Models.ClanWar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ClanWarReadyMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;
    private final Map<Integer, ClanWar> slotToWar;

    public ClanWarReadyMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&6&lGUERRAS PROGRAMADAS", 45);
        this.plugin = plugin;
        this.clan = clan;
        this.slotToWar = new HashMap<Integer, ClanWar>();
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        List<ClanWar> readyWars = new ArrayList<ClanWar>();
        for (ClanWar war : ClanWarManager.getInstance().getReadyWars()) {
            if (war.isParticipant(clan.getTag())) {
                readyWars.add(war);
            }
        }

        if (readyWars.isEmpty()) {
            inventory.setItem(22, createItem(XMaterial.BARRIER, "&c&lNo hay guerras programadas",
                    "&7No tienes guerras aceptadas"));
            inventory.setItem(40, createItem(XMaterial.ARROW, "&e&l← Volver"));
            return;
        }

        int slot = 10;
        for (ClanWar war : readyWars) {
            if (slot >= 34) break;

            boolean isChallenger = war.getChallengerClanTag().equals(clan.getTag());
            String opponentTag = isChallenger ? war.getChallengedClanTag() : war.getChallengerClanTag();
            Clan opponentClan = ClanManager.getInstance().getClan(opponentTag);

            String opponentName = opponentClan != null ? opponentClan.getName() : opponentTag;
            String scheduledDate = formatDate(war.getScheduledTime());

            long timeUntilWar = war.getScheduledTime() - System.currentTimeMillis();
            boolean isReady = war.isReadyToStart();

            XMaterial material = isReady ? XMaterial.LIME_BANNER : XMaterial.YELLOW_BANNER;

            List<String> lore = new ArrayList<String>();
            lore.add("&7Oponente: &f" + opponentName);
            lore.add("&7Tu rol: " + (isChallenger ? "&6Desafiante" : "&eDesafiado"));
            lore.add("");
            lore.add("&7Programada para:");
            lore.add("&e" + scheduledDate);
            lore.add("&7Zona horaria: &eAmerica/Lima");
            lore.add("");

            if (isReady) {
                lore.add("&a&l✔ LISTA PARA INICIAR");
                lore.add("&7Arena: &a" + (war.getArenaKey() != null ? war.getArenaKey() : "Sin asignar"));
                lore.add("");
                lore.add("&aConéctate a un servidor CW para jugar");
            } else {
                long hours = timeUntilWar / (1000 * 60 * 60);
                long minutes = (timeUntilWar / (1000 * 60)) % 60;

                if (hours > 24) {
                    long days = hours / 24;
                    lore.add("&7Inicia en: &e" + days + " día(s)");
                } else if (hours > 0) {
                    lore.add("&7Inicia en: &e" + hours + "h " + minutes + "m");
                } else {
                    lore.add("&7Inicia en: &e" + minutes + " minutos");
                }

                lore.add("");
                lore.add("&7Espera a que llegue la hora programada");
            }

            ItemStack warItem = createItem(material, "&6&lGuerra vs " + opponentTag,
                    lore.toArray(new String[0]));

            inventory.setItem(slot, warItem);
            slotToWar.put(Integer.valueOf(slot), war);
            slot += 2;
        }

        inventory.setItem(40, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        if (slot == 40) {
            playSound(XSound.UI_BUTTON_CLICK);
            player.closeInventory();
            new ClanWarListMenu(player, plugin, clan).open();
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        return sdf.format(new Date(timestamp));
    }
}