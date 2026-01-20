package sdfrpe.github.io.ptcclans.Utils;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatInputManager {
    private static final Map<UUID, PendingAction> pendingActions = new HashMap<UUID, PendingAction>();

    public static void setPendingAction(Player player, ActionType type, Object data) {
        pendingActions.put(player.getUniqueId(), new PendingAction(type, data));
    }

    public static PendingAction getPendingAction(Player player) {
        return pendingActions.get(player.getUniqueId());
    }

    public static void clearPendingAction(Player player) {
        pendingActions.remove(player.getUniqueId());
    }

    public static boolean hasPendingAction(Player player) {
        return pendingActions.containsKey(player.getUniqueId());
    }

    public static class PendingAction {
        private final ActionType type;
        private final Object data;

        public PendingAction(ActionType type, Object data) {
            this.type = type;
            this.data = data;
        }

        public ActionType getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

    public enum ActionType {
        CREATE_CLAN_NAME,
        CREATE_CLAN_TAG,
        INVITE_MEMBER,
        EDIT_CLAN_NAME,
        EDIT_CLAN_TAG,
        TRANSFER_LEADERSHIP
    }
}