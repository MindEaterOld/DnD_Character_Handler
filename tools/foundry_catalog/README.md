# Character catalog from Foundry

`app/src/main/assets/systems/dnd5e_2024/character_catalog.json` — classes, subclasses, species, backgrounds and every
feature, option and feat with their level progression, and the spells with the spell lists they are
on — is built from the Foundry VTT compendiums of the D&D world:

- **AG Fifthpendium** (Russian): PHB 2024 classes, feats and origins, plus the D&D Beyond, Eberron,
  Forgotten Realms and Ravenloft option packs. Names come as `Ярость [Rage]`.
- **dnd5e SRD 5.2** (English): English names and texts for the documents both share (same ids).
- **Spells**: the Fifthpendium and SRD 5.2 spell packs (card data and both texts) and the spell
  lists of the classes, subclasses and dragonmarks from `dnd5e.registry.spellLists`; Character
  Wizard's spell choices and the Spells screen's catalog both use them. Range, casting time and
  duration are written in the app's (SRD) wording ("60 feet", "1 bonus action", "Up to 1 minute");
  attack, save, damage, healing and area come from the spells' activities; the higher-level paragraph
  has its own field.
- **Containers** (`containers`): the bags, cases and equipment packs of the SRD 5.2 equipment pack,
  with their capacity (lb; a Bag of Holding's contents weigh nothing), weight, price, text and what
  they hold — a pack's tinderbox, rations and its waterskin with the water. Contents point at the
  standalone item of their kind, so their names come in both languages; Character Wizard's packs
  and the inventory's bags are built from them.
- **Weapon mastery** (`weaponMasteries`): the eight mastery properties with their rules (Russian from
  the Fifthpendium's PHB journal, English from SRD 5.2) and each base weapon's property; the sheet,
  the inventory and the attacks show them for the weapons the character has mastered.
- `legacy/feature_catalog_extra.json` and the 5e-database SRD files: English texts the app shipped
  before, and the old catalog ids that saved features still carry (mapped in `legacyIds`).

The raw dumps (`export/`) are not committed.

## Updating

1. Start the receiver: `python receiver.py` (listens on 127.0.0.1:8766, writes to `export/`).
2. In Foundry, logged in as the Gamemaster, run `export_foundry.js` — paste it into the browser
   console (F12), or run it through the drawbridge MCP (`foundry_execute`).
3. `python convert.py` rewrites the asset and prints what it matched and what it couldn't.
4. Run the unit tests (`CharacterCatalogTest` checks the shipped file).

## Translations

Most non-SRD entries only come in Russian. English written by hand goes into
`translations/en.json`, keyed by the entry id (the Foundry document id, the same across re-imports):

```json
{
  "phbftActor000000": { "name": "Actor", "text": "Your experience on stage..." }
}
```

`convert.py` merges it over everything else on every run, so the catalog can be rebuilt from
Foundry at any time without losing translations. Either field may be left out. Never edit
`character_catalog.json` by hand — the next run overwrites it.

Proficiency and language labels missing from the ru-ru module get their Russian from
`translations/ru_traits.json`, keyed by the trait key (`"languages:standard:faerun:chondathan":
"Чондатанский"`); `convert.py` lists any label still without one, and `CharacterCatalogTest`
fails on it. The Faerûn languages follow the Russian *Heroes of Faerûn* («Стандартные языки
Фаэруна»).

Russian names of container contents the Fifthpendium has no item for (a waterskin's water, the
pockets of Heward's Handy Haversack) go into `translations/ru_equipment.json`, keyed by the Foundry
identifier (`"water-pint": "Вода (пинта)"`); `convert.py` warns about any still missing.

`translations/missing_en.json` is rewritten on every run: the entries still without an English
name or text (id, kind, book, owner, Russian and English name), PHB first — the list to translate
from.

## Text format

Descriptions are plain text: paragraphs on their own lines, `• ` list items, table rows as
`cell | cell`. Numbers that depend on the character stay as `{=FORMULA}` tokens holding the Foundry
formula (`{=@scale.barbarian.rages}`, `{=1d8 + @abilities.wis.mod}`); the app fills them in with
`CatalogFormulaText`.
