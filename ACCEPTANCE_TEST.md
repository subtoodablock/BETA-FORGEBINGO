# ForgeBingo in-game acceptance

Complete this checklist after the backend migration and Edge Function are deployed. Do not submit to Plugin Hub until every required check passes.

## Prepare

1. Create a temporary classic board and team on the ForgeBingo website.
2. Configure two incomplete tiles using safe, obtainable drops:
   - one NPC-loot rule with an item ID and an empty Allowed monsters list;
   - one rule with an item ID plus the numeric ID of a specific NPC.
3. Create a revocable test API key in Account Settings.
4. Launch the development client with JDK 11:

   ```powershell
   $env:JAVA_HOME = '<path to Eclipse Temurin JDK 11>'
   .\gradlew.bat run
   ```

5. Sign into RuneLite. For a Jagex Account, launch through the Jagex Launcher using RuneLite's local development flow if direct login is unavailable.

## Required checks

- Paste the test key into ForgeBingo settings and confirm the connected account name.
- Select the temporary classic board and verify its grid, tile details, and shared total.
- Compare several tiles with the website and confirm each uses the same icon; an Item ID sprite should appear only when the website tile has no usable icon.
- Confirm tile details are read-only and the plugin offers no manual complete or undo action.
- Click several tiles and confirm each smoothly expands from its grid position into a readable card with the website photo at top-left, tile name, optional description, and every task with its completion marker. Confirm the card never shows “Any monster” or “Drop progress”.
- Confirm every task has a slim progress bar beneath it, no bar is wider than the task area, partial quantity progress is gold, and completed tasks are emerald.
- Confirm tiles without descriptions do not show an empty description section. Close details using the X, Escape, and click-away paths, and confirm the card smoothly shrinks back into the correct grid tile.
- Leave **Upload proof screenshots** disabled, obtain the configured NPC drop, and confirm team-wide progress advances without a new proof.
- Confirm the item-only rule advances from any NPC that drops its accepted item.
- Confirm the restricted rule advances for the accepted item from its allowed NPC.
- Confirm the same accepted item from a different NPC does not advance the restricted rule.
- Confirm an item dropped by another player does not advance either rule.
- Confirm the same drop event is not counted twice after a forced refresh/retry.
- Select a Battleship board and confirm only your own team's board is visible, with no opponent toggle, opponent tiles, opponent progress, proofs, ships, or fleet coordinates.
- Obtain a configured NPC drop on Battleship and confirm automation advances only your own team's tile.
- Enable screenshot uploads, reset/use a multi-quantity test tile, obtain one matching NPC drop, and confirm a private proof appears after the server confirms partial progress. Finish the quantity and confirm the completion drop also receives proof.
- Confirm each proof frame is captured about one second after RuneLite registers its matching NPC drop, so the loot notification is visible before upload.
- Open the proof as an authorized teammate/leader and confirm an unauthorized account cannot access it.
- Briefly disconnect the network, verify the last good board remains visible, reconnect, and confirm automatic recovery.
- Revoke the test key on the website and confirm the plugin reports it as invalid.

The screenshot contains only the RuneLite game frame, but it can include in-game chat and UI. Inspect the proof before approving release.

## Release gate

Record the RuneLite version, plugin commit, backend migration version, tester, and result. Only a human in-game pass authorizes creation of the Plugin Hub manifest pull request.

- RuneLite version:
- Plugin commit:
- Backend migration version:
- Tester:
- Test date:
- Result: PENDING
