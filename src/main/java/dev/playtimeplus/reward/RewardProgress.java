package dev.playtimeplus.reward;

public record RewardProgress(String ruleId, long thresholdMillis, long remainingMillis, boolean available) {
}
