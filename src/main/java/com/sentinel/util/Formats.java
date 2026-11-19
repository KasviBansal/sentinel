package com.sentinel.util;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formats {
    public static final Locale INDIA = Locale.forLanguageTag("en-IN");
    public static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM uuuu · HH:mm", INDIA);
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd MMM", INDIA);
    public static final DateTimeFormatter SHORT_DATE_YEAR = DateTimeFormatter.ofPattern("MMM uuuu", INDIA);

    private Formats() { }

    public static String money(BigDecimal value) {
        String[] parts = value.stripTrailingZeros().toPlainString().split("\\.");
        String whole = parts[0];
        if (whole.length() > 3) {
            String rest = whole.substring(0, whole.length() - 3);
            whole = rest.replaceAll("(?<=\\d)(?=(?:\\d{2})+$)", ",") + "," + whole.substring(whole.length() - 3);
        }
        return "₹" + whole + (parts.length == 2 ? "." + (parts[1].length() == 1 ? parts[1] + "0" : parts[1]) : "");
    }
}
