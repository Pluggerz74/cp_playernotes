# PlayerNotes Pro

Staff moderation notes for Minecraft servers — track player history, flag high-risk joins, and keep your team aligned with a polished GUI and optional integrations.

**Beta release:** `0.9.0-beta`

## Requirements

| | |
|---|---|
| **Server software** | Paper, Purpur, Spigot |
| **Minecraft versions** | 1.19.4 – 1.21.x |
| **Java** | 17+ |

## Features

- **Player notes** — Add notes with type (Info, Warning, Suspect, Punishment, Staff) and priority (Low, Normal, High, Critical)
- **Staff GUI** — Apple-style inventory menus to browse, view, archive, and delete notes
- **Chat input flow** — Create notes from the GUI with in-game chat (cancel or 60s timeout)
- **Commands** — Full `/pn` command set for console and in-game staff
- **Join alerts** — Notify staff when flagged players join (configurable minimum priority)
- **Storage** — SQLite (default) or MySQL/MariaDB with HikariCP connection pooling
- **PlaceholderAPI** — Optional placeholders for note counts, flags, and latest note (cached, async)
- **Discord webhooks** — Optional notifications for note create/archive/delete and flagged joins
- **Diagnostics** — `/pn debug` and startup config sanity warnings

## Commands

| Command | Description |
|---|---|
| `/pn <player>` | Open the notes GUI (players only) |
| `/pn list <player>` | List active notes in chat |
| `/pn add <player> <text>` | Add a note (Info / Normal priority) |
| `/pn archive <id>` | Archive a note |
| `/pn remove <id>` | Permanently delete a note |
| `/pn version` | Show plugin version |
| `/pn reload` | Reload configuration |
| `/pn debug` | Show diagnostics (admin) |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `playernotes.use` | Access `/pn version` | `true` |
| `playernotes.view` | View player notes | `op` |
| `playernotes.add` | Add notes | `op` |
| `playernotes.archive` | Archive notes | `op` |
| `playernotes.remove` | Delete notes | `op` |
| `playernotes.reload` | Reload config | `op` |
| `playernotes.notify` | Receive join alerts | `op` |
| `playernotes.admin` | Full access (includes debug) | `op` |

## Storage

Default backend is **SQLite** (`plugins/cp_playernotes/playernotes.db`).

For **MySQL/MariaDB**, set in `config.yml`:

```yaml
storage:
  type: mysql

  mysql:
    host: "localhost"
    port: 3306
    database: "playernotes"
    username: "root"
    password: "your-password"
```

The database must exist before startup; tables are created automatically.

## Hooks

### PlaceholderAPI (optional)

Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) and enable in config:

```yaml
hooks:
  placeholderapi: true
```

| Placeholder | Description |
|---|---|
| `%playernotes_active_count%` | Active non-archived notes |
| `%playernotes_critical_count%` | Active critical notes |
| `%playernotes_high_risk_count%` | Active high + critical notes |
| `%playernotes_flagged%` | `yes` / `no` |
| `%playernotes_latest_note%` | Newest active note (max 32 chars) |

### Discord webhooks (optional)

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/..."
```

Events: note created, critical note, archived, deleted, flagged player join.

## Setup

1. Build or download the plugin JAR (see below).
2. Place `cp_playernotes-0.9.0-beta.jar` in your server's `plugins/` folder.
3. Start the server once to generate config files.
4. Edit `plugins/cp_playernotes/config.yml` (storage, join alerts, Discord, hooks).
5. Run `/pn reload` or restart the server.
6. Grant staff permissions and use `/pn <player>` to open the GUI.

Config files:

- `config.yml` — Storage, join alerts, Discord, hooks
- `messages.yml` — Chat messages (MiniMessage)
- `gui.yml` — GUI titles, materials, slots, labels

## Build

From the project root:

```bash
mvn clean package
```

Output: `target/cp_playernotes-0.9.0-beta.jar`

If Maven is not on your PATH, use the Maven wrapper from a sibling project:

```bash
../cp_waterfight/mvnw.cmd -f pom.xml clean package
```

## License

Copyright © CodingPlugs. All rights reserved.
