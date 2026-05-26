package dev.playtimeplus.reward;

import java.util.List;

public record RewardSettings(
        boolean enabled,
        boolean announceConsole,
        int defaultMaxClaimsPerCheck,
        List<RewardRule> rules
) {
    public static RewardSettings disabled() {
        return new RewardSettings(false, false, 1, List.of());
    }
}
