# Asset structure

This project uses two different storage strategies for visual resources.

## 1. `res/drawable`

Use `res/drawable` for static application resources that ship with the app UI.

Android does not support nested folders inside `res/drawable`, so grouping is done by filename prefix:

- `ic_ui_*` - UI icons
- `decor_*` - decorative frames, corners, dividers, overlays
- `ic_game_*` - built-in game icons that are still shipped as app resources

Examples:

- `ic_ui_menu.xml`
- `ic_ui_settings.xml`
- `decor_card_corner.xml`
- `ic_game_inventory_weapon.xml`

## 2. `assets/icons`

Use `assets/icons` for content-style icon libraries that are better addressed by path as data.

Folders:

- `assets/icons/items/`
- `assets/icons/spells/`
- `assets/icons/attacks/`
- `assets/icons/features/`
- `assets/icons/categories/`

These paths are suitable for model fields such as `InventoryItem.icon` or `Attack.icon`.

Examples:

- `icons/items/longsword.png`
- `icons/spells/fireball.png`
- `icons/attacks/unarmed_strike.png`

Reference these from models as plain asset paths, for example:

- `icons/items/longsword.png`
- `icons/spells/fireball.png`

## 3. `assets/portraits/placeholders`

Use this folder for built-in portrait placeholders that ship with the app.

Examples:

- `portraits/placeholders/default_mage.png`
- `portraits/placeholders/default_rogue.png`

## 4. `assets/systems/<key>/` — a game system's own files

What belongs to one game system (its catalogs, its texts of rules) lies in its folder, named by its `GameSystem.key`
(docs/GAME_SYSTEMS.md); what every system shares (design tokens, the interface's texts, icons, portraits, fonts,
sounds) stays at the root.

- `systems/dnd5e_2024/character_catalog.json` — classes, species, backgrounds, feats, spells (tools/foundry_catalog).
- `systems/dnd5e_2024/inventory_text_ru.json` — the inventory's items in Russian.
- `systems/dnd5e_2024/srd/` — two SRD 5.2 files from the 5e-database submodule, copied in at build time
  (`dnd5eSrdAssets` in app/build.gradle.kts).

Code reads the paths from one place per system (`Dnd5eAssets`), never as string literals.

## 5. User-provided media

User portraits and imported custom icons should remain in app-local file storage and continue to be exported through `.dndchar` archives.

They should not be copied into `res/` or committed into the built-in `assets/icons` catalog.

## Reference formats

The shared image renderer accepts these formats:

- `drawable:ic_ui_menu`
- `res:drawable/ic_ui_menu`
- `icons/items/longsword.png`
- `portraits/placeholders/default_mage.png`
- `content://...`
- `file://...`
- absolute local file paths
