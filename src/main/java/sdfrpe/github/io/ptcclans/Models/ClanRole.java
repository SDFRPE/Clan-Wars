package sdfrpe.github.io.ptcclans.Models;

public enum ClanRole {
    LEADER(3),
    CO_LEADER(2),
    MEMBER(1);

    private final int permissionLevel;

    ClanRole(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public boolean hasPermission(ClanRole requiredRole) {
        return this.permissionLevel >= requiredRole.permissionLevel;
    }

    public String getDisplayName() {
        switch (this) {
            case LEADER:
                return "&6&lLÍDER";
            case CO_LEADER:
                return "&e&lCO-LÍDER";
            case MEMBER:
                return "&7Miembro";
            default:
                return "&7Desconocido";
        }
    }
}