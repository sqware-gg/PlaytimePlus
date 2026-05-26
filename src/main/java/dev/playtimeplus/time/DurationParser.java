package dev.playtimeplus.time;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static OptionalLong parseMillis(String value) {
        if (value == null) {
            return OptionalLong.empty();
        }
        String normalized = value.trim().replace(" ", "").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return OptionalLong.empty();
        }
        if (normalized.matches("\\d+")) {
            return OptionalLong.of(parseLong(normalized) * 1000L);
        }

        Matcher matcher = TOKEN.matcher(normalized);
        int position = 0;
        long total = 0L;
        while (matcher.find()) {
            if (matcher.start() != position) {
                return OptionalLong.empty();
            }
            long amount = parseLong(matcher.group(1));
            long unitMillis = switch (matcher.group(2).charAt(0)) {
                case 's' -> 1000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                case 'w' -> 604_800_000L;
                default -> 0L;
            };
            total = saturatedAdd(total, saturatedMultiply(amount, unitMillis));
            position = matcher.end();
        }

        if (position != normalized.length() || total <= 0L) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(total);
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static long saturatedMultiply(long left, long right) {
        if (left == 0L || right == 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private static long saturatedAdd(long left, long right) {
        long result = left + right;
        if (((left ^ result) & (right ^ result)) < 0L) {
            return Long.MAX_VALUE;
        }
        return result;
    }
}
