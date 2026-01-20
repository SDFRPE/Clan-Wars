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

import java.util.List;

public class ClanWarListMenu extends ClanMenu {
    private final PTCClans plugin;
    private final Clan clan;

    public ClanWarListMenu(Player player, PTCClans plugin, Clan clan) {
        super(player, "&c&lGUERRAS DE CLANES", 54);
        this.plugin = plugin;
        this.clan = clan;
    }

    @Override
    public void setMenuItems() {
        fillBorder(XMaterial.GLASS_PANE);

        ClanWar activeWar = getTrueActiveWar(clan.getTag());
        List<ClanWar> pendingWars = ClanWarManager.getInstance().getPendingWars();
        List<ClanWar> readyWars = ClanWarManager.getInstance().getReadyWars();

        inventory.setItem(4, createItem(XMaterial.DIAMOND_SWORD, "&c&lEstado de Guerras",
                "&7Clan: " + clan.getFormattedTag(),
                "",
                "&7Guerras activas: " + (activeWar != null ? "&a1" : "&c0"),
                "&7Guerras pendientes: &e" + countPendingForClan(pendingWars),
                "&7Guerras programadas: &6" + countReadyForClan(readyWars),
                "",
                "&7Victorias totales: &a" + clan.getWins(),
                "&7Derrotas totales: &c" + clan.getLosses()));

        if (plugin.getSettings().canExecuteWarCommands()) {
            inventory.setItem(20, createItem(XMaterial.EMERALD, "&a&lDesafiar Clan",
                    "&7Desafía a otro clan a una guerra",
                    "",
                    "&7Debes programar fecha y hora",
                    "&7El rival tiene &e10 minutos &7para aceptar",
                    "",
                    "&7Máximo guerras simultáneas: &e" + plugin.getSettings().getMaxSimultaneousWars(),
                    "",
                    "&eClick para abrir"));
        } else {
            inventory.setItem(20, createItem(XMaterial.BARRIER, "&c&lDesafiar Clan",
                    "&7Solo disponible en el lobby",
                    "",
                    "&cConéctate al lobby para desafiar"));
        }

        int pendingCount = countPendingForClan(pendingWars);
        if (pendingCount > 0) {
            inventory.setItem(22, createItem(XMaterial.PAPER, pendingCount, "&e&lDesafíos Pendientes",
                    "&7Tienes &e" + pendingCount + " &7desafíos por aceptar",
                    "",
                    "&c&lPLAZO DE 10 MINUTOS",
                    "&7Los desafíos expiran si no se aceptan",
                    "",
                    "&eClick para ver"));
        } else {
            inventory.setItem(22, createItem(XMaterial.PAPER, "&7&lSin Desafíos Pendientes",
                    "&7No tienes desafíos por aceptar"));
        }

        int readyCount = countReadyForClan(readyWars);
        if (readyCount > 0) {
            inventory.setItem(24, createItem(XMaterial.CLOCK, readyCount, "&6&lGuerras Programadas",
                    "&7Tienes &6" + readyCount + " &7guerras programadas",
                    "",
                    "&7Guerras aceptadas con fecha/hora definida",
                    "",
                    "&eClick para ver"));
        } else {
            inventory.setItem(24, createItem(XMaterial.CLOCK, "&7&lSin Guerras Programadas",
                    "&7No tienes guerras programadas"));
        }

        if (activeWar != null) {
            displayActiveWar(activeWar);
        }

        inventory.setItem(49, createItem(XMaterial.ARROW, "&e&l← Volver"));
    }

    private ClanWar getTrueActiveWar(String clanTag) {
        for (ClanWar war : ClanWarManager.getInstance().getPendingWars()) {
            if (war.isParticipant(clanTag) && war.getArenaKey() != null && !war.isFinished()) {
                return war;
            }
        }

        for (ClanWar war : ClanWarManager.getInstance().getReadyWars()) {
            if (war.isParticipant(clanTag) && war.getArenaKey() != null && !war.isFinished()) {
                return war;
            }
        }

        return null;
    }

    private void displayActiveWar(ClanWar war) {
        boolean isChallenger = war.getChallengerClanTag().equals(clan.getTag());
        String opponentTag = isChallenger ? war.getChallengedClanTag() : war.getChallengerClanTag();
        Clan opponentClan = ClanManager.getInstance().getClan(opponentTag);

        XMaterial material = XMaterial.RED_BANNER;
        String opponentName = opponentClan != null ? opponentClan.getName() : opponentTag;

        inventory.setItem(31, createItem(material, "&c&lGUERRA ACTIVA",
                "&7Oponente: &f" + opponentTag,
                "&7Nombre: &f" + opponentName,
                "",
                "&7Tu rol: " + (isChallenger ? "&6Desafiante" : "&eDesafiado"),
                "&7Arena: &a" + war.getArenaKey(),
                "",
                "&eConéctate a un servidor CW para jugar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        playSound(XSound.UI_BUTTON_CLICK);

        switch (slot) {
            case 20:
                if (plugin.getSettings().canExecuteWarCommands()) {
                    if (clan.isLeader(player.getUniqueId())) {
                        player.closeInventory();
                        new ClanWarChallengeMenu(player, plugin, clan, 0).open();
                    } else {
                        playSound(XSound.ENTITY_VILLAGER_NO);
                        player.sendMessage(c("&cSolo el líder puede desafiar a otros clanes."));
                    }
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                    player.sendMessage(c("&cSolo disponible en el lobby."));
                }
                break;

            case 22:
                int pendingCount = countPendingForClan(ClanWarManager.getInstance().getPendingWars());
                if (pendingCount > 0) {
                    player.closeInventory();
                    new ClanWarPendingMenu(player, plugin, clan).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }
                break;

            case 24:
                int readyCount = countReadyForClan(ClanWarManager.getInstance().getReadyWars());
                if (readyCount > 0) {
                    player.closeInventory();
                    new ClanWarReadyMenu(player, plugin, clan).open();
                } else {
                    playSound(XSound.ENTITY_VILLAGER_NO);
                }
                break;

            case 49:
                player.closeInventory();
                new ClanMainMenu(player, plugin).open();
                break;
        }
    }

    private int countPendingForClan(List<ClanWar> wars) {
        int count = 0;
        for (ClanWar war : wars) {
            if (war.getChallengedClanTag().equals(clan.getTag()) && !war.isAccepted() && !war.isExpired()) {
                count++;
            }
        }
        return count;
    }

    private int countReadyForClan(List<ClanWar> wars) {
        int count = 0;
        for (ClanWar war : wars) {
            if (war.isParticipant(clan.getTag()) && war.isAccepted()) {
                count++;
            }
        }
        return count;
    }
}