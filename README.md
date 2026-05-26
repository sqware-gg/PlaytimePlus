# PlaytimePlus

PlaytimePlus is a Paper plugin for playtime tracking, active-time leaderboards, AFK detection, manual `/afk`, optional AFK kicking, sleep-ignore handling, and PlaceholderAPI placeholders.

It is intended to replace small standalone playtime and AFK plugins with one configurable plugin.

## Compatibility

- Server software: Paper
- API target: Paper `26.1.2.build.65-stable`
- Java: `25+`
- Build tool: Maven wrapper
- Optional hook: PlaceholderAPI

## Features

- Tracks active playtime and AFK time separately.
- Calculates total playtime as active plus AFK.
- Persists player data in `plugins/PlaytimePlus/players.yml`.
- Automatically marks idle players AFK after a configurable threshold.
- Optional warning before automatic AFK.
- Manual `/afk [reason]` command.
- Clears AFK when a player performs real activity.
- Movement thresholding to avoid camera-only anti-AFK scripts counting as activity by default.
- Optional AFK auto-kick with bypass permission.
- AFK players can be ignored by vanilla sleep checks.
- Active, total, and AFK leaderboards.
- Configurable interval and milestone rewards.
- Reward commands, messages, permission gates, and AFK exclusion.
- Admin set, add, reset, save, reload, and status commands.
- Config-safe updates through `config-new.yml`.
- Optional `%playtimeplus_*%` PlaceholderAPI placeholders.

## Installation

1. Build or download `PlaytimePlus-0.1.0.jar`.
2. Stop the Paper server.
3. Put the jar in the server `plugins` folder.
4. Start the server once to generate `plugins/PlaytimePlus/config.yml`.
5. Review AFK thresholds, broadcast messages, kick settings, and permissions.
6. Restart the server, or run `/playtimeplus reload`.

## Player Commands

```text
/playtime
/playtime <player>
/playtime top [active|total|afk] [page]
/afk [reason]
```

Examples:

```text
/playtime
/playtime Hilal_h18
/playtime top active
/playtime top total 2
/afk eating
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

Durations accept compact units:

```text
30m
2h
1d12h
3600s
```

When setting or adding `total`, PlaytimePlus adjusts active playtime. AFK time remains separate unless the `afk` metric is changed directly.

## Rewards

PlaytimePlus supports configurable playtime rewards in `config.yml`.

Reward types:

```text
interval   - repeats every configured duration
milestone  - claims once after the configured duration
```

Reward metrics:

```text
active  - rewards only non-AFK time
total   - rewards active plus AFK time
afk     - rewards AFK time
```

Example interval reward:

```yml
rewards:
  enabled: true
  rules:
    active-30m-money:
      enabled: true
      type: "interval"
      metric: "active"
      every: "30m"
      permission: ""
      require-online: true
      require-not-afk: true
      max-claims-per-check: 2
      commands:
        - "eco give {player} 100"
      message: "&7Earned &#57F287$100 &7for &#2b98fd{threshold}&7 active playtime."
```

Example milestone reward:

```yml
rewards:
  enabled: true
  rules:
    active-1h-key:
      enabled: true
      type: "milestone"
      metric: "active"
      at: "1h"
      commands:
        - "crate key give {player} vote 1"
      message: "&7You reached &f{threshold}&7 active playtime and received a reward."
```

Reward placeholders in commands and messages:

```text
{player}
{uuid}
{reward}
{reward_name}
{type}
{metric}
{claim}
{threshold}
{active}
{total}
{afk}
{session}
{active_seconds}
{total_seconds}
{afk_seconds}
{session_seconds}
```

Reward claim counters are stored in `players.yml`, so milestone rewards cannot be farmed by relogging. Interval rewards store the number of claimed intervals and can catch up after a long server tick or manual playtime adjustment, capped by `max-claims-per-check`.

## Permissions

```text
playtimeplus.playtime        - view own playtime, default true
playtimeplus.playtime.others - view other players, default true
playtimeplus.top             - view leaderboards, default true
playtimeplus.afk             - use /afk, default true
playtimeplus.notify          - receive AFK broadcasts, default true
playtimeplus.afk.bypass      - bypass automatic AFK marking, default op
playtimeplus.kick.bypass     - bypass AFK auto-kicks, default op
playtimeplus.admin           - use admin commands, default op
```

## PlaceholderAPI

If PlaceholderAPI is installed and `placeholders.enabled` is true, these placeholders are registered:

```text
%playtimeplus_total%
%playtimeplus_active%
%playtimeplus_afk%
%playtimeplus_session%
%playtimeplus_total_seconds%
%playtimeplus_active_seconds%
%playtimeplus_afk_seconds%
%playtimeplus_session_seconds%
%playtimeplus_is_afk%
%playtimeplus_afk_reason%
%playtimeplus_joins%
%playtimeplus_last_seen%
%playtimeplus_rank_active%
%playtimeplus_rank_total%
%playtimeplus_rank_afk%
%playtimeplus_rewards_claimed%
%playtimeplus_next_reward%
%playtimeplus_next_reward_time%
%playtimeplus_next_reward_seconds%
```

## Data

Player data is stored in:

```text
plugins/PlaytimePlus/players.yml
```

Each record stores the last known name, active milliseconds, AFK milliseconds, join count, first seen time, and last seen time.

## Build From Source

Use JDK 25:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot'
.\mvnw.cmd package
```

The compiled jar is written to:

```text
target/PlaytimePlus-0.1.0.jar
```

## Updating

PlaytimePlus does not overwrite your existing `config.yml`. If the bundled config changes, the plugin writes `plugins/PlaytimePlus/config-new.yml` so you can compare and copy new options.

Player data is stored separately in `players.yml`.

## License

PlaytimePlus is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
