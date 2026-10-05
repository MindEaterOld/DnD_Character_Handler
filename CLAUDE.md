# Project guidelines

## Design system — colors, fonts, font sizes

Colors, fonts, and font sizes (type scale / "кегли") must come **only** from the design tokens file:

- `app/src/main/assets/design_tokens.json`

Rules:
- **Colors** — use only the values defined under `themes.<theme>.colors.materialTheme` (Material color scheme) and `themes.<theme>.colors.app` (the app palette, grouped by role: `text`, `background`, `surface`, `border`, `accent`, `progress`, `ornament`). Every theme has its whole palette with the same roles (see **Themes** below). Do not invent new hex values. In code, reference the app palette through the token accessor `LocalDesignTokens.current.colors.<group>.<name>` (e.g. `colors.text.primary`, `colors.surface.card`, `colors.border.muted`, `colors.accent.heal`) rather than hardcoding `Color(0x…)` literals. Examples: `border.muted #30FFFFFF`, `text.muted #D2CAC2`, `accent.hpTemporary #69B7FF` (blue), `accent.inspiration #FFD86B` (gold), `accent.heal #8AD178` (green).
- **Fonts & font sizes** — use the `MaterialTheme.typography.*` styles, which map to the sizes defined under `typography.materialTheme` (`headlineMedium` 28sp, `titleLarge` 22sp, `titleMedium` 18sp, `bodyLarge` 16sp, `bodyMedium` 14sp, `labelMedium` 12sp). Do not hardcode arbitrary `fontSize` values.
- **Never add new colors or font sizes (кегли) on your own.** If a new color or size seems necessary, you must first request it from the project owner and get explicit approval before adding it. No new value goes into the code without that approval.
- When touching older code that uses non-token hex values, align it to the tokens.

## Themes — one set of screens, a look per theme

The player picks the app's look in the settings, under the language (owner's choice, 2026-10-05): **Classic** — the app as it was — and **Engraving** — ivory ink on charcoal paper over an engraving of a dragon (ChatGPT, 2026-10-04). Themes change only how things look; the screens, their logic and their layout are the same in every theme.

- **Never two copies of a screen.** What a theme draws differently goes into the theme, not into the screen:
  - its **palette** — `themes.<key>.colors` in `design_tokens.json`; code reads it as before, `LocalDesignTokens.current.colors.<group>.<name>` and `MaterialTheme.colorScheme`, and gets the chosen theme's values;
  - its **look** — `ThemeLook` in `presentation/theme/ThemeLook.kt`: the backdrop (`LocalThemeBackdrop`), the frames of cards, stats, buttons and the tab bar (`FrameStyle`), the portrait (`PortraitStyle`), the XP bar (`XpBarStyle`). Components read it through `LocalThemeLook` (`BorderLabelCard`, `ScreenBackground`, `BottomNavigationBar`, the overview's portrait, HP card, its buttons and XP bar); a screen never asks which theme is on.
- If a theme needs a part drawn another way, add a style to `ThemeLook` and a variant inside the component — never a branch on the theme in a screen.
- **Every theme has its whole palette with the same roles** as the classic one; `DesignTokenThemesTest` checks it. A new colour goes into every theme's palette — and, as always, only with the owner's approval.
- Previews: a screen's preview takes a theme (`DnDTheme(theme)`); the overview has one per theme. Boards show both themes when a change touches both.
- `AppTheme` (`domain/model`) is the list of themes; the pick is stored with the language (`app_theme`) and applied at the app's root by `DnDTheme(theme)`.

## Parallel work — two agents in one tree

Another agent (ChatGPT) may work in this working tree at the same time:

- Agree who works on which screens and files; a shared file (`DndCharacterApp.kt`, `localization.json`, `design_tokens.json`, `docs/BACKLOG.md`, `components/`) is changed by one at a time.
- Before changing a file, check it isn't changed by someone else (`git status` / `git diff -- <file>`); if it is, leave it.
- Commit small and often, **only your own files, by name** — never `git add -A`, `git commit -a`, or `git checkout -- .` / `git stash` / `git reset` over the whole tree.
- Keep the project building: add new files first, wire them in last.

## Buttons vs. stats — fill or outline

What can be pressed as an action and what shows a value must look different at a glance:

- **Buttons** are always round (a circle, for an icon alone) or a rectangle with rounded corners — never sharp corners. A button has a **fill that stands out** from the background and from the cards around it; the fill is what says "press me". The fill comes from the button palette, never from a card's surface: `surface.button` (#1A171D) is next to `surface.card` (#17141B) and does not stand out.
- **Stats and cells** (abilities, HP, AC, speed, list cells, info cards) have an **outline and no fill of their own** — the dark card surface, like the background. They stay that way even when a tap on them opens an editor.
- So: **fill = button, outline without a fill = stat.** Never give a stat a button's fill, and never make a button outline-only (an outlined `Surface` or `OutlinedButton` as an action).
- Only **significant, often pressed** actions are buttons with a fill (Save, Inspiration, the HP actions, the floating buttons, steppers). Minor actions stay plain text buttons (`TextButton`) — Cancel, a link in a list, "Edit" at the bottom of an unfolded card. That is the one exception.
- A **toggle** (one option of a row to pick from, or an on/off button like Inspiration) is a button too: the picked option differs from the others by its fill's colour or transparency, not only by its outline.
- **The top bar's icons are the one exception**: the menu, the dice and (on the overview) the rests in a column under the dice are bare icons with no fill (`ScreenTopActionButton`) — the owner's choice, 2026-10-03.
- **Button palette** (approved 2026-10-03; fills and their text/icon colours by role):
  - **standard button**: `surface.button` #3B3840 (1.58:1 against the cards), text and icon `text.primary`;
  - main action (Save) and a toggle that is on: `materialTheme.primary` #C6A36C, text and icon `onPrimary`;
  - **toggles** (the options of a row or a list to pick from: modes, language, size, units, filters, the skins, the fonts, the dice chips) — the picked one gold `primary` with `onPrimary` text, the others `surface.button` with `text.primary`, no outline. In code: `toggleFill(selected)` / `toggleContent(selected)` / `toggleRadioColors()` and `ToggleChip` in `components/Toggle.kt` (owner's choice from boards, 2026-10-04). A pick that is its own colour keeps it (a condition, the healing kind: its accent at 12 %);
  - **Inspiration** is its own toggle, not a filled button: a compass rose of eight points (`InspirationStar`, 96dp, no outline) in the lower right corner of the portrait's octagon — `text.label` while off (the screen's colour for an idle mark), `accent.inspiration` while on, with a white heart and a breathing glow of `accent.inspiration` fading out (owner's choice from boards, 2026-10-03);
  - HP actions: their accent (`accent.dangerHpZero`, `accent.heal`, `accent.hpTemporary`) at **12 %** as the fill behind an icon and label in the full accent — at 12 % even the red label reads (4.6:1). On the overview, Heal (left) and Damage (right) hang from the hit points' card beside the death saves' skull tab, as tabs of their own; temporary hit points are a kind of healing, picked by a toggle in the Healing pop-up (the picked kind in its accent at 12 %) — owner's choice, 2026-10-03;
  - stepper (− and + beside a number): `StepButton`, a 48dp circle of `surface.button` with the icon in `text.primary`;
  - concentration on a spell: `ConcentrationToggle`, a 36dp circle beside the prepared dot — `surface.button` while off, `primary` gold while on (owner's choice, 2026-10-03). Its sign everywhere is `ConcentrationIcon`: an open palm (Material Symbols Rounded's) holding a filled spark, a small spark beside it (owner's choice from boards, 2026-10-04);
  - dangerous action (confirm deletion): `accent.dangerHpZero`, text `text.primary`;
  - delete icon (the trash in a pop-up): `accent.dangerHpZero` at 16% behind an `accent.dangerHpZero` icon; it always asks first, «Удалить? Это действие нельзя отменить.» (`EditDialog` does it, owner's choice 2026-10-04);
  - `surface.option` and `surface.selected` are the old fills; what still has them (Character Wizard's choice cards) moves to the palette when redone.
- **The dice table's result panel** is as wide as the sheet's content, its top level with the top bar's dice button, the cross that closes the table in its corner (owner's choice, 2026-10-04).

## Conditions — how the sheet shows them (owner's choices, 2026-10-03)

- **On the overview** the conditions go down the left of the portrait, as the rests go down the right: an outlined mark each (they are stats), exhaustion as its level in `accent.damageFire`, concentration's mark in `primary` gold, then a filled "+" that opens the picker. Three marks at most, the rest fold into "+N".
- **A value the conditions change** (a check, a save, a skill, initiative, an attack bonus, speed, AC against attacks) shows the value as it is now, and two things that combine (owner's choice, 2026-10-04):
  - **how the d20 is rolled** — `RollMarker` beside the value: two arrows down in `accent.dangerHpZero` for disadvantage, two up in `accent.heal` for advantage, a red cross for an outright fail (no roll);
  - **how far the value moved** — its colour, `changedValueColor`: `accent.dangerHpZero` when lower (exhaustion's −2), `accent.heal` when higher. Speed has only the colour; AC only the arrows (how attacks against the character are rolled).
- **Concentration** is a card on the Spells screen in place of the class: lit in gold while a spell is held, the spell's name as much as fits, cut with a dot.
- In Russian exhaustion is «Истощение» (the owner's word); the other names and languages are as the catalog and the 2024 books have them.
- Damage types in Russian are the catalog's words everywhere (combat, inventory, the spell editor, defenses): radiant is **«Лучистый»**, never «Сияние» or «Излучение» (owner's choice, 2026-10-04); force «Сила», lightning «Молния», thunder «Гром», necrotic «Некротический», psychic «Психический».

## Naming — Character Wizard

The step-by-step character building and level-up system is called **Character Wizard**. It's a name: write it in English in every language and never translate it (not «мастер», «Assistent», «assistant», «asistente»). Use it in texts and when talking to the project owner.

## Localization — length limits (`maxChars`)

Texts live in `app/src/main/assets/localization.json` (`"key": {"en", "ru", "de", "fr", "es"}`). A key shown in a fixed-width element carries `"maxChars": N`, the most characters that fit there:

```json
"biography_background": {
  "maxChars": 11,
  "en": "Background",
  "ru": "Предыстория",
  ...
}
```

Rules:
- **No translation may be longer than `maxChars`** (counted in characters). When translating or editing a limited key, keep every language within it. If a language can't say it in that many characters, shorten the word and end it with a dot (`Предыст.`, `Hintergr.`); never go over the limit.
- **When adding a key for a fixed-width element** (a card label, a chip, a tab, a button in a row), work out its `maxChars` from the layout and add it.
- How to work it out: at the reference width **412dp** (the device of the screen previews), take the width the text gets, in dp (the element's width minus its paddings, icons and gaps), and divide it by **0.58 × the font size in sp** (an average Cyrillic character in Roboto; Russian is the widest of our languages), rounding down. Example: the Features screen's Class/Species/Background cards are 113dp wide; the label on the border gets 81dp at 12sp → 81 / 7.0 = 11.
- A key used by several elements gets the limit of the tightest one. If shortening it would hurt the other places, give that element its own key instead.
- `LocalizationLimitsTest` checks every limited key; run it after changing translations.

## UI layout

Layout work (screens, pop-ups, cards, buttons, variant boards) follows the `/ui-layout` skill in `.claude/skills/ui-layout/`: the reuse catalogue, the tokens as the code reads them, and the pipelines to build through Android Studio, render previews to PNG and check on the emulator. New layout pipelines and pitfalls are written down there.

## Backlog

Wishes, bugs and technical debt live in `docs/BACKLOG.md` (in Russian, for the project owner). Check it when choosing the next task or when asked "what's left"; add what you notice (a bug, a debt, an owner's wish) to its section; when something is done, move it to "Готово" with the date. Things built but not yet seen on a device go under "Проверить на устройстве".

## Git workflow

- **Do not create separate branches.** Commit directly to `main` and push immediately.
- This overrides the default "branch first on the default branch" behavior — for this project, work straight on `main`.
