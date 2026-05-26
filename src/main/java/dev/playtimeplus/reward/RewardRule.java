package dev.playtimeplus.reward;

import dev.playtimeplus.time.TimeMetric;
import java.util.List;

public record RewardRule(
        String id,
        RewardType type,
        TimeMetric metric,
        long thresholdMillis,
        String permission,
        boolean requireOnline,
        boolean requireNotAfk,
        int maxClaimsPerCheck,
        List<String> commands,
        String message
) {
    public String displayName() {
        return id.replace('-', ' ');
    }
}
