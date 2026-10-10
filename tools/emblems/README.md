# Game systems' emblems

The drawer's game system card shows each game's own emblem (`GameSystemEmblems.kt` in
`app/src/main/java/com/dndcharacterhandler/presentation/components/`), one colour, as `ImageVector` paths. These
scripts made the ones that weren't vectors already. Pure Python with Pillow, no numpy.

- `trace_lib.py` — a 0..1 field of what is filled → box blur → marching squares at 0.5 → loops → Ramer–Douglas–Peucker
  → one even-odd path in a square viewport; `preview()` fills it gold on the card's dark to compare.
- `trace_mark.py` — a one-coloured mark on a dark ground traced as it is. Vampire: The Masquerade's ankh came from the
  owner's picture (red on black): `python tools/emblems/trace_mark.py ankh.jpg 105 0.35`.
- `draw_starfinder.py` — the Starfinder Society's compass has no flat version, so it is drawn by the original's
  measures and then traced.

The others: D&D's ampersand is Simple Icons' path (CC0); Pathfinder's Glyph of the Open Road is Andrew Eakett's vector
from PathfinderWiki, with a ring round it drawn in code. The marks are their owners' (Wizards of the Coast, Paizo,
Paradox Interactive): see docs/BACKLOG.md before the app is published.
