# 5e Database Integration

The project uses `5e-bits/5e-database` as a git submodule in `external/5e-database`.

Why this setup:
- We keep the upstream history and can pull new releases without copying files by hand.
- The app reads two 2024 SRD JSON files from the submodule: `app/build.gradle.kts` (task `dnd5eSrdAssets`) copies
  them into the assets at build time, under D&D 5e (2024)'s own folder `systems/dnd5e_2024/srd/` (docs/GAME_SYSTEMS.md).
- Character inventory remains local to each character; catalog items are only templates.

Useful commands:

```powershell
git submodule update --init --recursive
git submodule update --remote external/5e-database
git add external/5e-database
git commit -m "Update 5e database"
```

Current asset source:
- `external/5e-database/src/2024/en`

Currently loaded by the app:
- `5e-SRD-Equipment.json`
- `5e-SRD-Magic-Items.json`

Flow in the app:
1. The inventory screen opens the SRD catalog from the `+` button.
2. Choosing a catalog item creates a new `InventoryItem` for the current character.
3. The saved character item is independent from the source catalog entry.
