# Plugin Hub submission

Submit only after the in-game acceptance checklist passes and the public repository contains the accepted release commit.

Create `plugins/forgebingo` in a fork of `runelite/plugin-hub` with:

```properties
repository=https://github.com/subtoodablock/forgebingo-runelite.git
commit=<full 40-character accepted release commit>
authors=subtoodablock
warning=This plugin submits your ForgeBingo API key, board requests and actions, matched NPC loot data, and IP address to a 3rd-party server not controlled or verified by the RuneLite Developers. If screenshot proof is enabled, RuneLite game-frame images are also submitted.
```

The warning is required because the plugin communicates with ForgeBingo's third-party service. Do not place Supabase service credentials, Lovable source, or user API keys in either repository.
