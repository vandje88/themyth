package zhaf.media.themyth.core;

import java.text.NumberFormat;
import java.util.Locale;

public final class Texts {

    private Texts() {
    }

    public static String initial(String name) {
        if (name == null) {
            return "?";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public static String pretty(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String[] parts = raw.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    public static String number(long value) {
        return NumberFormat.getIntegerInstance().format(value);
    }

    public static String number(double value) {
        return number(Math.round(value));
    }
}
