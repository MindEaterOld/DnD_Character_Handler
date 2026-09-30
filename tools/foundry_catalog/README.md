# Character catalog from Foundry

`app/src/main/assets/character_catalog.json` — classes, subclasses, species, backgrounds and every
feature, option and feat with their level progression — is built from the Foundry VTT compendiums
of the D&D world:

- **AG Fifthpendium** (Russian): PHB 2024 classes, feats and origins, plus the D&D Beyond, Eberron,
  Forgotten Realms and Ravenloft option packs. Names come as `Ярость [Rage]`.
- **dnd5e SRD 5.2** (English): English names and texts for the documents both share (same ids).
- `legacy/feature_catalog_extra.json` and the 5e-database SRD files: English texts the app shipped
  before, and the old catalog ids that saved features still carry (mapped in `legacyIds`).

The raw dumps (`export/`) are not committed.

## Updating

1. Start the receiver: `python receiver.py` (listens on 127.0.0.1:8766, writes to `export/`).
2. In Foundry, logged in as the Gamemaster, run `export_foundry.js` — paste it into the browser
   console (F12), or run it through the drawbridge MCP (`foundry_execute`).
3. `python convert.py` rewrites the asset and prints what it matched and what it couldn't.
4. Run the unit tests (`CharacterCatalogTest` checks the shipped file).

## Text format

Descriptions are plain text: paragraphs on their own lines, `• ` list items, table rows as
`cell | cell`. Numbers that depend on the character stay as `{=FORMULA}` tokens holding the Foundry
formula (`{=@scale.barbarian.rages}`, `{=1d8 + @abilities.wis.mod}`); the app fills them in with
`CatalogFormulaText`.
