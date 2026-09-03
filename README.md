<div align="center">
  <h1>mReports</h1>
  <p>Auditable player reports and a focused moderation queue.</p>

  <p>
    <a href="https://papermc.io/software/paper"><img alt="Paper" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/paper_vector.svg"></a>
    <a href="https://purpurmc.org"><img alt="Purpur" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/purpur_vector.svg"></a>
    <a href="https://papermc.io/software/folia"><img alt="Folia" height="56" src="https://raw.githubusercontent.com/miklires/mCommand/main/docs/assets/folia-available.png"></a>
  </p>
</div>

## What it does

- Guides players through a configurable category GUI or accepts a detailed command report.
- Gives every report a stable numeric ID and stores it in a versioned local H2 database.
- Merges repeated open reports from the same reporter instead of flooding the queue.
- Prevents self-reports, exempt-target reports and rapid submission spam.
- Lets one staff member claim a report, then resolve or reject it with a note.
- Records creation, merges, claims, notes and closing decisions in an audit trail.
- Captures a short, bounded recent-chat snapshot from the reported player when a report is accepted.
- Recovers the open queue after restart and removes expired closed reports by retention policy.

## Requirements

- Java 25
- Paper, Purpur or Folia 26.2

mChat, mBans and PlaceholderAPI are optional integration targets. mReports runs without them.

## Commands

- `/report <player>` — open the category GUI.
- `/report <player> <category> [details]` — submit directly.
- `/reports` — open the staff queue (console receives a text queue).
- `/reports claim|view|resolve|reject|note <id> [text]` — process a stable report ID.
- `/reports reload` — reload submission limits and categories.

Permissions: `mreports.use`, `mreports.notify`, `mreports.exempt`, `mreports.staff`, `mreports.reload`.

## Storage and privacy

Database work runs on a dedicated virtual thread. Stored data includes player UUIDs/names, report text, moderation notes and audit events. Configure `storage.retention-days` according to your server policy. Anonymous metrics are disabled until a real bStats project ID is assigned.

## Build

```bash
./gradlew clean build
```

The plugin JAR is written to `build/libs`; the Bukkit Services API is built in `api/build/libs`.

Licensed under the MIT License.
