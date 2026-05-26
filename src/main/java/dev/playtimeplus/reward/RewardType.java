package dev.playtimeplus.reward;

import java.util.Locale;
import java.util.Optional;

public enum RewardType {
    INTERVAL,
    MILESTONE;

    public static Optional<RewardType> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
