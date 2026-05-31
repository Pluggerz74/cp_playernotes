# Changelog

All notable changes to **PlayerNotes Pro** (`cp_playernotes`) are documented here.

## [0.9.3-beta] — 2026-05-31

### Added
- Staff audit logging for note create, edit, archive, and delete actions
- `/pn history <player>` command to view recent audit entries for a player
- `player_notes_audit` table for SQLite and MySQL/MariaDB
- Audit config options (`audit.enabled`, `audit.max-history-command-results`)
- Audit log status in `/pn debug` output
- `playernotes.history` permission

## [0.9.2-beta] — 2026-05-31

### Added
- GUI pagination (7 notes per page with previous/next navigation)
- Archive filter toggle (ACTIVE / ARCHIVED / ALL) in the notes GUI
- Note editing from the detail GUI (chat input flow)
- `/pn edit <id> <new text>` command
- `playernotes.edit` permission

### Changed
- Page and filter state preserved across detail view, archive/delete, and chat input flows
- Empty pages clamp to the last available page after archive or delete

## [0.9.1-beta] — 2026-05-31

### Fixed
- Discord webhook JSON payload issue (invalid JSON caused HTTP 400 responses)

### Added
- Discord webhook test command (`/pn discordtest`)
- MySQL/MariaDB storage support with HikariCP connection pooling
- PlaceholderAPI support (optional placeholders for note counts and flags)
- Staff join alerts for high-risk players
- Note detail GUI (view, archive, delete)
- Type and priority selection flow for GUI note creation
- Diagnostics command (`/pn debug`)
- Config sanity checks on startup and reload
- Discord webhook payload debug logging (`discord.debug-payload`)

### Changed
- Discord webhook payloads now built with Gson for reliable JSON encoding
- Improved config file comments and README beta testing documentation

## [0.9.0-beta] — Initial beta

- Core player notes system with SQLite storage
- Staff GUI, commands, and chat input flow
- Discord webhook notifications (initial release)
- Basic configuration and MiniMessage messages
