# PlayerNotes Pro

Staff moderation notes for Minecraft servers — track player history, flag high-risk joins, and keep your team aligned with a polished GUI and optional integrations.

**Current beta:** `0.9.1-beta`

> **Beta status:** This release is intended for testing on Paper, Purpur, and Spigot (1.19.4 – 1.21.x). Features and configuration may change before a stable release. Please report issues with `/pn debug` output and relevant console logs.

## Requirements

| | |
|---|---|
| **Server software** | Paper, Purpur, Spigot |
| **Minecraft versions** | 1.19.4 – 1.21.x |
| **Java** | 17+ |

## Features

- **Player notes** — Add notes with type (Info, Warning, Suspect, Punishment, Staff) and priority (Low, Normal, High, Critical)
- **Staff GUI** — Inventory menus to browse, view details, archive, and delete notes
- **Note creation flow** — Select type → priority → chat input (cancel or 60s timeout)
- **Commands** — Full `/pn` command set for console and in-game staff
- **Join alerts** — Notify staff when flagged players join (configurable minimum priority)
- **Storage** — SQLite (default) or MySQL/MariaDB with HikariCP connection pooling
- **PlaceholderAPI** — Optional placeholders for note counts, flags, and latest note (cached, async)
- **Discord webhooks** — Optional notifications for note create/archive/delete and flagged joins
- **Diagnostics** — `/pn debug`, `/pn discordtest`, and startup config sanity warnings

## Commands

| Command | Description |
|---|---|
| `/pn <player>` | Open the notes GUI (players only) |
| `/pn list <player>` | List active notes in chat |
| `/pn add <player> <text>` | Add a note (Info / Normal priority) |
| `/pn archive <id>` | Archive a note |
| `/pn edit <id> <text>` | Update note content |
| `/pn remove <id>` | Permanently delete a note |
| `/pn version` | Show plugin version |
| `/pn reload` | Reload configuration |
| `/pn debug` | Show diagnostics (`playernotes.admin`) |
| `/pn discordtest` | Send a test Discord webhook (`playernotes.admin`) |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `playernotes.use` | Access `/pn version` | `true` |
| `playernotes.view` | View player notes | `op` |
| `playernotes.add` | Add notes | `op` |
| `playernotes.archive` | Archive notes | `op` |
| `playernotes.edit` | Edit note content | `op` |
| `playernotes.remove` | Delete notes | `op` |
| `playernotes.reload` | Reload config | `op` |
| `playernotes.notify` | Receive join alerts | `op` |
| `playernotes.admin` | Full access (debug, discordtest) | `op` |

---

## Beta testing

### Installation

1. Build the plugin (see [Build](#build)) or use a provided beta JAR.
2. Copy `cp_playernotes-0.9.1-beta.jar` into your server's `plugins/` folder.
3. Start the server once to generate default config files.
4. Stop the server, edit configuration (see checklist below), then start again — or use `/pn reload` after editing live configs.

### First setup checklist

- [ ] Confirm Java 17+ and a supported server version (1.19.4 – 1.21.x)
- [ ] Grant staff permissions (`playernotes.view`, `playernotes.add`, etc.)
- [ ] Test `/pn <player>` GUI opens and notes can be added via the GUI flow
- [ ] Test `/pn add`, `/pn list`, `/pn archive`, and `/pn remove`
- [ ] Run `/pn debug` and review database, Discord, and PlaceholderAPI status
- [ ] Configure join alerts if needed (`join-alerts` in `config.yml`)
- [ ] Configure Discord webhooks if needed (see below)
- [ ] Configure MySQL if needed (see below) — **requires server restart** to switch storage type
- [ ] Install PlaceholderAPI on the server if you want placeholders, or set `hooks.placeholderapi: false`
- [ ] Check console for `[Config]` sanity warnings after startup or `/pn reload`

### Discord webhook setup

1. In Discord: **Server Settings → Integrations → Webhooks → New Webhook**
2. Copy the webhook URL (must start with `https://discord.com/api/webhooks/` or `https://discordapp.com/api/webhooks/`)
3. Edit `plugins/cp_playernotes/config.yml`:

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
  username: "PlayerNotes Pro"
  avatar-url: ""
  debug-payload: false
  notify:
    note-created: true
    critical-note-created: true
    note-archived: true
    note-deleted: true
    flagged-player-join: true
```

4. Run `/pn reload`
5. Run `/pn discordtest` (requires `playernotes.admin`) to verify delivery
6. If testing fails, set `discord.debug-payload: true`, reload, and check console for the exact JSON payload

### MySQL setup example

Create the database **before** starting the plugin:

```sql
CREATE DATABASE playernotes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'playernotes'@'localhost' IDENTIFIED BY 'your-secure-password';
GRANT ALL PRIVILEGES ON playernotes.* TO 'playernotes'@'localhost';
FLUSH PRIVILEGES;
```

Then in `config.yml`:

```yaml
storage:
  type: mysql

  mysql:
    host: "localhost"
    port: 3306
    database: "playernotes"
    username: "playernotes"
    password: "your-secure-password"
    use-ssl: false
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

Tables are created automatically on first connect. **Restart the server** after changing `storage.type`.

Default SQLite file: `plugins/cp_playernotes/playernotes.db` (configurable via `storage.sqlite.file`).

### PlaceholderAPI placeholders

Install the **PlaceholderAPI** plugin on your server, then enable the hook:

```yaml
hooks:
  placeholderapi: true
```

| Placeholder | Description |
|---|---|
| `%playernotes_active_count%` | Active non-archived notes |
| `%playernotes_critical_count%` | Active critical notes |
| `%playernotes_high_risk_count%` | Active high + critical notes |
| `%playernotes_flagged%` | `yes` if high-risk count > 0, else `no` |
| `%playernotes_latest_note%` | Newest active note (max 32 chars, or `none`) |

Placeholders use a 30-second per-player cache. Values may show `loading` while refreshing.

If PlaceholderAPI is **not** installed, set `hooks.placeholderapi: false` to suppress config warnings.

---

## Troubleshooting

### Discord webhook not sending

1. Confirm `discord.enabled: true` and `webhook-url` is set correctly
2. Run `/pn discordtest` and read the in-game result
3. Check console for HTTP status codes (401/404 = bad URL, 400 = payload issue)
4. Enable `discord.debug-payload: true` and inspect the logged JSON
5. Ensure the webhook URL uses `https://discord.com/api/webhooks/...` format

### PlaceholderAPI not installed warning

If you see `[Config] PlaceholderAPI hook is enabled in config but PlaceholderAPI is not installed`:

- Install PlaceholderAPI on the server, **or**
- Set `hooks.placeholderapi: false` in `config.yml` and run `/pn reload`

This warning does not disable the plugin.

### Old config files not updating

New config keys are merged from defaults inside the JAR when you reload, but **existing keys in your file are not overwritten**. To pick up new options:

1. Compare your `config.yml` with a fresh copy from the JAR or repository defaults
2. Add missing keys manually, or
3. Back up your config, delete the file, restart to regenerate, then re-apply your settings

Same applies to `messages.yml` and `gui.yml`.

### MySQL: database must exist before startup

The plugin connects to an **existing** database. It creates tables automatically but does **not** create the database itself. If startup fails:

1. Create the database with SQL (see MySQL setup above)
2. Verify host, port, username, and password in `config.yml`
3. Check console for `[Config]` warnings about empty MySQL fields
4. Run `/pn debug` after a successful start to confirm `Database: connected`

---

## Config files

| File | Purpose |
|---|---|
| `config.yml` | Storage, join alerts, Discord, hooks |
| `messages.yml` | Chat messages (MiniMessage format) |
| `gui.yml` | GUI titles, materials, slots, labels |

See inline comments in each file for detailed option descriptions.

## Build

From the project root:

```bash
mvn clean package
```

Output: `target/cp_playernotes-0.9.1-beta.jar`

If Maven is not on your PATH:

```bash
../cp_waterfight/mvnw.cmd -f pom.xml clean package
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

## License

Copyright © CodingPlugs. All rights reserved.
