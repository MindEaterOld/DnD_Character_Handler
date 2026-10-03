# Project guidelines

## Design system — colors, fonts, font sizes

Colors, fonts, and font sizes (type scale / "кегли") must come **only** from the design tokens file:

- `app/src/main/assets/design_tokens.json`

Rules:
- **Colors** — use only the values defined under `colors.materialTheme` (Material color scheme) and `colors.app` (the app palette, grouped by role: `text`, `background`, `surface`, `border`, `accent`, `progress`, `ornament`). Do not invent new hex values. In code, reference the app palette through the token accessor `LocalDesignTokens.current.colors.<group>.<name>` (e.g. `colors.text.primary`, `colors.surface.card`, `colors.border.muted`, `colors.accent.heal`) rather than hardcoding `Color(0x…)` literals. Examples: `border.muted #30FFFFFF`, `text.muted #D2CAC2`, `accent.hpTemporary #69B7FF` (blue), `accent.inspiration #FFD86B` (gold), `accent.heal #8AD178` (green).
- **Fonts & font sizes** — use the `MaterialTheme.typography.*` styles, which map to the sizes defined under `typography.materialTheme` (`headlineMedium` 28sp, `titleLarge` 22sp, `titleMedium` 18sp, `bodyLarge` 16sp, `bodyMedium` 14sp, `labelMedium` 12sp). Do not hardcode arbitrary `fontSize` values.
- **Never add new colors or font sizes (кегли) on your own.** If a new color or size seems necessary, you must first request it from the project owner and get explicit approval before adding it. No new value goes into the code without that approval.
- When touching older code that uses non-token hex values, align it to the tokens.

## Buttons vs. stats — fill or outline

What can be pressed as an action and what shows a value must look different at a glance:

- **Buttons** are always round (a circle, for an icon alone) or a rectangle with rounded corners — never sharp corners. A button has a **fill that stands out** from the background and from the cards around it; the fill is what says "press me". The fill comes from the button palette, never from a card's surface: `surface.button` (#1A171D) is next to `surface.card` (#17141B) and does not stand out.
- **Stats and cells** (abilities, HP, AC, speed, list cells, info cards) have an **outline and no fill of their own** — the dark card surface, like the background. They stay that way even when a tap on them opens an editor.
- So: **fill = button, outline without a fill = stat.** Never give a stat a button's fill, and never make a button outline-only (an outlined `Surface` or `OutlinedButton` as an action).
- **Button palette** (fills and their text/icon colours, by role) — being worked out with the project owner; until it is approved and in `design_tokens.json`, use what is approved so far:
  - main action (Save): `materialTheme.primary` #C6A36C, text `onPrimary`;
  - dangerous action (confirm deletion): `accent.dangerHpZero`, text `text.primary`;
  - delete icon (the trash in a pop-up): `accent.dangerHpZero` at 16% behind an `accent.dangerHpZero` icon.

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

## Backlog

Wishes, bugs and technical debt live in `docs/BACKLOG.md` (in Russian, for the project owner). Check it when choosing the next task or when asked "what's left"; add what you notice (a bug, a debt, an owner's wish) to its section; when something is done, move it to "Готово" with the date. Things built but not yet seen on a device go under "Проверить на устройстве".

## Git workflow

- **Do not create separate branches.** Commit directly to `main` and push immediately.
- This overrides the default "branch first on the default branch" behavior — for this project, work straight on `main`.
