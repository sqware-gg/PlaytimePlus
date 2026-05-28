# PlaytimePlus

PlaytimePlus is a Paper playtime plugin for Minecraft servers. It tracks active time, total time, AFK time, sessions, leaderboards, rewards, `/afk`, PlaceholderAPI placeholders, and optional AFK kicking.

Use it as a replacement for separate playtime, AFK, leaderboard, and playtime reward plugins.

## Features

- Tracks active playtime and AFK time separately.
- Calculates total playtime as active plus AFK.
- Stores player data in `plugins/PlaytimePlus/players.yml`.
- Automatic AFK detection with optional warnings.
- Manual `/afk [reason]`.
- Clears AFK on real activity.
- Movement thresholding to reduce camera-only anti-AFK scripts.
- Optional AFK auto-kick and sleep-ignore handling.
- Active, total, and AFK leaderboards.
- Interval and milestone rewards.
- Reward placeholders for commands and messages.
- PlaceholderAPI support.
- Config-safe updates through `config-new.yml`.

## Requirements

- Paper `26.1.2+`
- Java `25+`
- Optional: PlaceholderAPI
- Maven wrapper included

## Player Commands

```text
/playtime
/playtime <player>
/playtime top [active|total|afk] [page]
/afk [reason]
```

## Admin Commands

```text
/playtimeplus status
/playtimeplus reload
/playtimeplus save
/playtimeplus rewards
/playtimeplus reset <player>
/playtimeplus set <player> <active|total|afk> <duration>
/playtimeplus add <player> <active|total|afk> <duration>
```

Durations accept compact units: `30m`, `2h`, `1d12h`, `3600s`.

## Rewards

Reward types:

```text
interval   - repeats every configured duration
milestone  - claims once after the configured duration
```

Reward metrics:

```text
active  - non-AFK time
total   - active plus AFK time
afk     - AFK time
```

Example:

```yaml
rewards:
  enabled: true
  rules:
    active-30m-money:
      enabled: true
      type: "interval"
      metric: "active"
      every: "30m"
      require-not-afk: true
      commands:
        - "eco give {player} 100"
      message: "&7Earned $100 for {threshold} active playtime."
```

Useful reward placeholders:

```text
{player}, {uuid}, {reward}, {reward_name}, {type}, {metric}, {claim},
{threshold}, {threshold_short}, {active}, {active_short}, {total},
{total_short}, {afk}, {afk_short}, {session}, {session_short},
{active_seconds}, {total_seconds}, {afk_seconds}, {session_seconds}
```

Reward claim counters are stored in `players.yml`, so milestone rewards cannot be farmed by relogging.

## Permissions

```text
playtimeplus.playtime        - view own playtime, default true
playtimeplus.playtime.others - view other players, default true
playtimeplus.top             - view leaderboards, default true
playtimeplus.afk             - use /afk, default true
playtimeplus.notify          - receive AFK broadcasts, default true
playtimeplus.afk.bypass      - bypass automatic AFK marking, default op
playtimeplus.kick.bypass     - bypass AFK auto-kicks, default op
playtimeplus.admin           - admin commands, default op
```

## PlaceholderAPI

Common placeholders:

```text
%playtimeplus_total%
%playtimeplus_active%
%playtimeplus_afk%
%playtimeplus_session%
%playtimeplus_total_short%
%playtimeplus_active_short%
%playtimeplus_afk_short%
%playtimeplus_session_short%
%playtimeplus_total_seconds%
%playtimeplus_active_seconds%
%playtimeplus_afk_seconds%
%playtimeplus_session_seconds%
%playtimeplus_is_afk%
%playtimeplus_rank_active%
%playtimeplus_rank_total%
%playtimeplus_rank_afk%
%playtimeplus_rewards_claimed%
%playtimeplus_next_reward%
%playtimeplus_next_reward_time%
%playtimeplus_next_reward_time_short%
%playtimeplus_next_reward_seconds%
```

`*_short` placeholders show up to two non-zero units, such as `1d 1h` or `1h 1m`.

## Build

Use JDK 25:

```powershell
.\mvnw.cmd package
```

The jar is written to `target/PlaytimePlus-0.1.0.jar`.

## Support

- Website: https://sqware.gg
- Discord: https://discord.sqware.gg

PlaytimePlus is licensed under the Apache License, Version 2.0.
