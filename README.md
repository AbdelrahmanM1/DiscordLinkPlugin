# Aquix Discord Link Plugin

A Minecraft server plugin for linking Minecraft player accounts with Discord accounts securely via verification codes.

---

## Features

- Link Minecraft players with their Discord accounts using `/linkdiscord <discord_id>` command.
- Secure verification with 6-digit codes and 5-minute expiration.
- Prevent duplicate linking (one Discord ID per Minecraft player).
- Verification code expiration handling and retry mechanism.
- Storage of links and pending verifications in an SQLite database.
- Easy unlinking of Discord accounts.
- Simple and efficient SQL database management.

---

## Commands

| Command                      | Description                                           |
| ---------------------------- | ----------------------------------------------------- |
| `/linkdiscord <discord_id>`  | Start linking process, generates a verification code.|
| `/verifylink <code>`         | Complete linking by providing the verification code. |
| `/unlinkdiscord`             | Unlink your Discord account from your Minecraft player (if implemented). |

---

## Installation

1. Download the plugin JAR file.
2. Place the JAR in your Minecraft server's `plugins` folder.
3. Start or restart your server.
4. The plugin will create necessary database tables automatically.

---

## Configuration

- The plugin uses an SQLite database by default.
- No additional configuration is needed for basic usage.
- For advanced users, you can customize the database location inside the plugin source code.

---

## Database Schema

The plugin uses two tables:

- `pending_verifications`:
  - `uuid` (TEXT) — Minecraft player UUID
  - `discord_id` (TEXT) — Discord user ID
  - `code` (TEXT) — verification code
  - `created_at` (DATETIME) — timestamp of the request

- `links`:
  - `uuid` (TEXT) — Minecraft player UUID
  - `discord_id` (TEXT) — Discord user ID

---

## Development

- Java 17+ (or adjust to your target)
- Bukkit/Spigot/Paper API
- SQLite for persistent storage

---

## How it works

1. Player runs `/linkdiscord <discord_id>` — plugin generates a 6-digit code and stores it with a timestamp.
2. Player uses `/verifylink <code>` — plugin checks if the code matches and is still valid (within 5 minutes).
3. On successful verification, the plugin links the player UUID to the Discord ID and deletes the pending request.

---

## Troubleshooting

- If the verification code expires immediately, check your server's time and timezone settings.
- Check server console logs for detailed plugin error messages.
- Ensure your database file is writable by the server.

---

## Contribution

Feel free to fork this project, open issues, or submit pull requests!

---


## Contact

Created by Abdelrahman Moharram

---

*Thank you for using the Aquix Discord Link Plugin!*
