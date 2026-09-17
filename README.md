# ForgeBingo RuneLite Companion

ForgeBingo is a Java 11 read-only RuneLite sidebar companion for Classic and Battleship ForgeBingo team boards.

## Features

- Classic and Battleship boards show only the team board that your ForgeBingo account belongs to. Opponent boards, progress, proofs, ships, and fleet coordinates are never requested or displayed.
- View an adaptive, text-free 3×3 through 10×10 ForgeBingo grid with website colors, completion states, and the same tile icons used on the ForgeBingo website. Automated loot tiles fall back to RuneLite's item sprite only when no website icon is available.
- Click any tile to smoothly zoom it into a larger, scrollable read-only card with the website photo in the top-left, the tile name, optional description, and every task with a compact progress bar. Partial objective progress is gold and completed tasks are emerald. Automation details stay hidden. Close it with the corner X, Escape, or by clicking outside, and it shrinks back into its grid position.
- Listen only for RuneLite's `NpcLootReceived` event.
- Send only loot with item IDs configured on the selected board.
- Treat an empty allowed-monsters list as any NPC, or locally require one of the configured NPC IDs.
- Aggregate accepted item IDs into one team-wide quantity on the server.
- Optionally upload the next RuneLite game frame after a confirmed match.
- Check the selected board revision every five seconds and download the full board only after a change, with a periodic safety refresh. Retain the last good board during outages and retry with exponential backoff capped at five minutes.

Board creation and editing, manual completion and undo, clan administration, proof review, and Battleship gameplay remain on the [ForgeBingo website](https://www.forgebingo.com).

## Privacy

The plugin connects to the ForgeBingo service at `lrzkmuipnfjszxatjiox.supabase.co`. It sends the configured API key, board requests, and only NPC drops whose item IDs match an automation rule cached for the selected board. Your IP address is necessarily visible to that service.

Screenshot uploads are disabled by default. When enabled, every matching NPC drop captures the next RuneLite game frame one second after RuneLite registers the drop and uploads it to each tile the server confirms as a match, including drops that add partial progress. The game frame may include in-game chat and UI; desktop content and window chrome are excluded. Proofs are private PNG images, limited to 10 MB and 20 proofs per tile.

ForgeBingo does not inspect PvP loot or general inventory changes. Revoke an API key at any time from ForgeBingo Account Settings.

## Development

1. Install Eclipse Temurin JDK 11.
2. Run `./gradlew test build` (`gradlew.bat test build` on Windows).
3. Run `./gradlew run` to launch the RuneLite development client.
4. Generate a revocable API key on the website and paste it into RuneLite's ForgeBingo settings.

Do not commit Supabase service credentials or the private Lovable source to this repository.

## Private tester setup

Repository collaborators can test the plugin without receiving any shared ForgeBingo credentials:

1. Accept the private GitHub repository invitation.
2. Clone the repository, or use **Code → Download ZIP** and extract it.
3. Install Eclipse Temurin JDK 11 and set `JAVA_HOME` if Java is not already available.
4. From this repository, run `gradlew.bat test` and then `gradlew.bat run` on Windows. On macOS or Linux, use `./gradlew test` and `./gradlew run`.
5. Generate a personal, revocable test API key from the ForgeBingo website and enter it in the plugin settings. Never send API keys through GitHub, chat, screenshots, or bug reports.
6. Follow `ACCEPTANCE_TEST.md` and report results without including account credentials or private proof images.

The development client is separate from the tester's normal RuneLite installation. Testers using a Jagex Account may also need RuneLite's documented local-development login flow.
