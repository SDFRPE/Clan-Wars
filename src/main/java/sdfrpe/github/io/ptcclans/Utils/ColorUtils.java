package sdfrpe.github.io.ptcclans.Utils;

import org.bukkit.ChatColor;

import java.util.*;

public class ColorUtils {
    private static final Map<String, String> COLOR_CODES = new LinkedHashMap<>();

    static {
        COLOR_CODES.put("negro", "&0");
        COLOR_CODES.put("azul_oscuro", "&1");
        COLOR_CODES.put("verde_oscuro", "&2");
        COLOR_CODES.put("cian_oscuro", "&3");
        COLOR_CODES.put("rojo_oscuro", "&4");
        COLOR_CODES.put("morado", "&5");
        COLOR_CODES.put("dorado", "&6");
        COLOR_CODES.put("gris", "&7");
        COLOR_CODES.put("gris_oscuro", "&8");
        COLOR_CODES.put("azul", "&9");
        COLOR_CODES.put("verde", "&a");
        COLOR_CODES.put("cian", "&b");
        COLOR_CODES.put("rojo", "&c");
        COLOR_CODES.put("rosa", "&d");
        COLOR_CODES.put("amarillo", "&e");
        COLOR_CODES.put("blanco", "&f");
    }

    public static String getColorCode(String colorName) {
        return COLOR_CODES.getOrDefault(colorName.toLowerCase(), "&f");
    }

    public static boolean isValidColor(String colorName) {
        return COLOR_CODES.containsKey(colorName.toLowerCase());
    }

    public static String formatColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> getAvailableColors() {
        return new ArrayList<>(COLOR_CODES.keySet());
    }

    public static String getColorDisplay(String colorName) {
        String code = getColorCode(colorName);
        return ChatColor.translateAlternateColorCodes('&', code + colorName.toUpperCase());
    }
}