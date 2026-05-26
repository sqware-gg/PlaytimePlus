package dev.playtimeplus.time;

import java.util.Locale;
import java.util.Optional;

public enum TimeMetric {
    ACTIVE("active"),
    TOTAL("total"),
    AFK("afk");

    private final String key;

    TimeMetric(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<TimeMetric> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (TimeMetric metric : values()) {
            if (metric.key.equals(normalized)) {
                return Optional.of(metric);
            }
        }
        return Optional.empty();
    }
}
