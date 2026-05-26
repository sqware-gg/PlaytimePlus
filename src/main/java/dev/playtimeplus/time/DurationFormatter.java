package dev.playtimeplus.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class DurationFormatter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private DurationFormatter() {
    }

    public static String compact(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        if (seconds == 0L) {
            return "0s";
        }

        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        List<String> parts = new ArrayList<>();
        add(parts, days, "d");
        add(parts, hours, "h");
        add(parts, minutes, "m");
        add(parts, seconds, "s");
        return String.join(" ", parts.stream().limit(3).toList());
    }

    public static String timestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return "never";
        }
        return DATE_TIME.format(Instant.ofEpochMilli(epochMillis));
    }

    private static void add(List<String> parts, long value, String suffix) {
        if (value > 0L) {
            parts.add(value + suffix);
        }
    }
}
