<div align="center">
  <h1>mReports</h1>
  <p>Auditable player reports, evidence snapshots, and a focused moderation queue.</p>

  <p>
    <a href="https://papermc.io/software/paper"><img alt="Available for Paper" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/paper_vector.svg"></a>
    <a href="https://purpurmc.org"><img alt="Available for Purpur" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/purpur_vector.svg"></a>
    <a href="https://papermc.io/software/folia"><img alt="Available for Folia" height="56" src="https://raw.githubusercontent.com/miklires/mCommand/main/docs/assets/folia-available.png"></a>
  </p>
  <p>
    <a href="https://github.com/miklires/mReports"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-source-181717?logo=github"></a>
    <a href="https://modrinth.com/plugin/mreports"><img alt="Modrinth" src="https://img.shields.io/badge/Modrinth-download-00AF5C?logo=modrinth&logoColor=white"></a>
    <a href="https://discord.gg/pes25cnWKy"><img alt="Discord" src="https://img.shields.io/badge/Discord-support-5865F2?logo=discord&logoColor=white"></a>
  </p>
  <p>
    <img alt="Release 1.1.0" src="https://img.shields.io/badge/release-1.1.0-0ea5e9">
    <img alt="Java 25" src="https://img.shields.io/badge/Java-25-f59e0b?logo=openjdk">
    <img alt="Minecraft 26.2" src="https://img.shields.io/badge/Minecraft-26.2-62b47a">
  </p>
</div>

## Features

- Guided category GUI and direct command reports.
- Stable numeric IDs, searchable history, priorities, handler claiming, release, notes, resolve and reject actions.
- Persistent H2 storage with schema migration, asynchronous database work, restart recovery and retention cleanup.
- Duplicate merging, self-report prevention, exempt targets, cooldowns and rolling-window rate limits.
- Bounded recent-chat evidence captured when a report is accepted; commands and unrelated player data are not recorded.
- Optional Discord webhook events with strict HTTPS host validation, timeouts, bounded pending requests and disabled mentions.
- English interface by default; switch to Russian with `language: ru_RU`.
- Paper, Purpur and Folia scheduling support, public Bukkit Services API and a non-invasive Modrinth update check.

## Requirements

- Java 25
- Paper, Purpur or Folia 26.2

No economy, proxy or database server is required. mChat, mBans and PlaceholderAPI may be installed independently; mReports works without them.

## Quick start

1. Put `mReports-1.1.0.jar` in `plugins/` and start the server.
2. Review `plugins/mReports/config.yml`. The generated defaults are ready to use.
3. Grant `mreports.staff` to moderators and `mreports.notify` to staff who should receive new-report alerts.
4. Players can run `/report <player>`; moderators open `/reports`.

The plugin validates configured ranges on startup and reload. Invalid values are replaced with safe defaults. Existing report data remains in `plugins/mReports/data/`.

## Commands

| Command | Purpose | Permission |
|---|---|---|
| `/report <player>` | Open the category GUI | `mreports.use` |
| `/report <player> <category> [details]` | Submit directly | `mreports.use` |
| `/myreports` | View your ten latest submissions | `mreports.history.own` |
| `/reports` | Open the staff queue; text list in console | `mreports.staff` |
| `/reports view <id>` | Inspect a report | `mreports.staff` |
| `/reports evidence <id>` | Read its stored chat snapshot | `mreports.staff` |
| `/reports claim <id>` / `release <id>` | Assign or release a handler | `mreports.staff` |
| `/reports priority <id> <low\|normal\|high\|urgent>` | Change priority | `mreports.staff` |
| `/reports note <id> <text>` | Add an audit note | `mreports.staff` |
| `/reports resolve <id> [note]` / `reject <id> [note]` | Close a report | `mreports.staff` |
| `/reports search <text>` | Search IDs, players, categories and details | `mreports.staff` |
| `/reports history <player>` | View reports involving a cached player | `mreports.staff` |
| `/reports reload` | Validate and reload configuration | `mreports.reload` |

Additional permissions are `mreports.notify`, `mreports.exempt`, and `mreports.bypass.cooldown`.

## Configuration

The default categories are `CHEATING`, `CHAT`, `TEAMING`, `GRIEFING`, and `OTHER`. Up to 32 unique categories are supported. Submission length, cooldown, duplicate window, rolling report limit, closed-report retention and all evidence memory bounds are configurable.

Discord delivery is off by default. Set `discord.enabled: true` and provide a Discord HTTPS `/api/webhooks/...` URL. The URL is never printed to logs. Update checks only report that a newer Modrinth version exists; they never download or install files.

Metrics remain disabled until mReports receives its own public bStats project ID.

## Storage and privacy

The local H2 database stores player UUIDs and names, report text, staff notes, status changes and audit events. When enabled, recent public chat from the reported player is held in bounded memory and copied into an accepted report. It does not capture commands, inventory data, IP addresses, images or video. Set retention and evidence limits to match your server's privacy policy.

## API and build

Other plugins can obtain `MReportsApi` from Bukkit's Services Manager to submit, find, search and list reports. API methods return `CompletableFuture` values and must not be synchronously joined on the server thread.

```bash
./gradlew clean build
```

The distributable JAR is `build/libs/mReports-1.1.0.jar`; the separate API artifact is in `api/build/libs`.

[Report an issue](https://github.com/miklires/mReports/issues) · [View source](https://github.com/miklires/mReports) · [Join Discord](https://discord.gg/pes25cnWKy)

Licensed under the MIT License.
