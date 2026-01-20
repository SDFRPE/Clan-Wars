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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClanWarPendingMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;
    private final Map<Integer, ClanWar> slotToWar;

    public ClanWarPendingMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&e&lDESAFÍOS PENDIENTES", 45);
        this.plugin = plugin;
        this.clan = clan;
        this.slotToWar = new HashMap<Integer, ClanWar>();
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        List<ClanWar> pendingWars = new ArrayList<ClanWar>();
        for (ClanWar war : ClanWarManager.getInstance().getPendingWars()) {
            if (war.getChallengedClanTag().equals(clan.getTag()) && !war.isExpired()) {
                pendingWars.add(war);
            }
        }

        if (pendingWars.isEmpty()) {
            inventory.setItem(22, createItem(XMaterial.BARRIER, "&c&lNo hay desafíos pendientes",
                    "&7No tienes desafíos por aceptar"));
            inventory.setItem(40, createItem(XMaterial.ARROW, "&e&l← Volver"));
            return;
        }

        int slot = 10;
        for (ClanWar war : pendingWars) {
            if (slot >= 34) break;

            Clan challengerClan = ClanManager.getInstance().getClan(war.getChallengerClanTag());
            String challengerName = challengerClan != null ? challengerClan.getName() : war.getChallengerClanTag();
            String challengerTag = challengerClan != null ? challengerClan.getFormattedTag() : war.getChallengerClanTag();

            XMaterial material;
            String timeDisplay;

            if (war.isExpired()) {
                material = XMaterial.RED_BANNER;
                timeDisplay = "&c&l✖ EXPIRADO";
            } else {
                long remaining = war.getTimeUntilExpiration();
                int minutes = war.getRemainingMinutes();

                if (minutes > 5) {
                    material = XMaterial.GREEN_BANNER;
                } else if (minutes > 2) {
                    material = XMaterial.YELLOW_BANNER;
                } else {
                    material = XMaterial.ORANGE_BANNER;
                }

                timeDisplay = war.getFormattedTimeRemaining();
            }

            String scheduledTime = war.getFormattedScheduledTime();

            ItemStack warItem = createItem(material, "&e&lDesafío de " + war.getChallengerClanTag(),
                    "&7Clan: &f" + challengerName,
                    "&7Desafiante: " + challengerTag,
                    "",
                    "&7Fecha programada:",
                    "&e" + scheduledTime,
                    "",
                    "&7Tiempo para aceptar: " + timeDisplay,
                    "",
                    "&a&l✔ Click IZQUIERDO para ACEPTAR",
                    "&c&l✖ Click DERECHO para RECHAZAR");

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
            return;
        }

        ClanWar war = slotToWar.get(Integer.valueOf(slot));
        if (war != null) {
            if (war.isExpired()) {
                playSound(XSound.ENTITY_VILLAGER_NO);
                player.sendMessage(c("&6═══════════════════════════════════"));
                player.sendMessage(c("&c&l✖ DESAFÍO EXPIRADO"));
                player.sendMessage(c("&7Este desafío ha expirado"));
                player.sendMessage(c("&7El plazo de 10 minutos terminó"));
                player.sendMessage(c("&6═══════════════════════════════════"));

                player.closeInventory();
                new ClanWarPendingMenu(player, plugin, clan).open();
                return;
            }

            if (event.isLeftClick()) {
                playSound(XSound.ENTITY_PLAYER_LEVELUP);
                player.closeInventory();

                if (ClanWarManager.getInstance().acceptChallenge(war.getWarId())) {
                    player.sendMessage(c("&6═══════════════════════════════════"));
                    player.sendMessage(c("&a&l✔ DESAFÍO ACEPTADO"));
                    player.sendMessage(c("&7Has aceptado el desafío de " + war.getChallengerClanTag()));
                    player.sendMessage(c(""));
                    player.sendMessage(c("&7Fecha programada:"));
                    player.sendMessage(c("&e" + war.getFormattedScheduledTime()));
                    player.sendMessage(c(""));
                    player.sendMessage(c("&7La guerra comenzará en la fecha programada"));
                    player.sendMessage(c("&6═══════════════════════════════════"));
                } else {
                    player.sendMessage(c("&c✖ Error al aceptar el desafío."));
                    player.sendMessage(c("&7El desafío puede haber expirado."));
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }

            } else if (event.isRightClick()) {
                playSound(XSound.UI_BUTTON_CLICK);
                player.closeInventory();

                if (ClanWarManager.getInstance().cancelChallenge(war.getWarId())) {
                    player.sendMessage(c("&6═══════════════════════════════════"));
                    player.sendMessage(c("&c&l✖ DESAFÍO RECHAZADO"));
                    player.sendMessage(c("&7Has rechazado el desafío de " + war.getChallengerClanTag()));
                    player.sendMessage(c("&6═══════════════════════════════════"));
                } else {
                    player.sendMessage(c("&c✖ Error al rechazar el desafío."));
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }
            }
        }
    }
}