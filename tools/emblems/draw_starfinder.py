# -*- coding: utf-8 -*-
"""Draws the Starfinder Society's compass in one colour by the original's measures (1000 units, centre 500,500) and
traces it into one even-odd path for StarfinderEmblem (app/.../presentation/components/GameSystemEmblems.kt).

The parts: the bezel round the bottom with its two fins; the ring with six slots and four teeth on its inner edge; the
space inside left dark; the thin inner ring; four short points under four long ones (cut free by a gap) — every point
a triangle with straight sides, split along its axis into its two faces, its tip whole; the hub, the Pathfinder
Society's glyph cut in it as on the original. Drawn at 3x, brought down to 1000 for smooth edges.

    python tools/emblems/draw_starfinder.py <pathfinder_society_symbol.svg> [out_dir] [--disc]

Prints the viewport's side and the path; writes starfinder_drawn.png and starfinder_trace.png into out_dir.
"""
import math
import os
import re
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from svg_path import polygons  # noqa: E402
from trace_lib import preview, trace  # noqa: E402

DISC = '--disc' in sys.argv
args = [a for a in sys.argv[1:] if a != '--disc']
GLYPH_SVG = args[0]
OUT = args[1] if len(args) > 1 else '.'
K = 3
N = 1000 * K
C = 500 * K
ON, OFF = 255, 0
img = Image.new('L', (N, N), OFF)
d = ImageDraw.Draw(img)


def at(r, deg):
    """A point r from the centre, deg clockwise from north (canvas pixels)."""
    a = math.radians(deg)
    return (C + r * K * math.sin(a), C - r * K * math.cos(a))


def axis(deg):
    """(along, across) -> canvas pixels, for a point toward deg."""
    a = math.radians(deg)
    ux, uy = math.sin(a), -math.cos(a)
    px, py = -uy, ux
    return lambda along, across: (C + (along * ux + across * px) * K, C + (along * uy + across * py) * K)


def disc(r, fill):
    d.ellipse((C - r * K, C - r * K, C + r * K, C + r * K), fill=fill)


def band(r_in, r_out, start, end, fill, steps=180):
    pts = [at(r_out, start + (end - start) * i / steps) for i in range(steps + 1)]
    pts += [at(r_in, end - (end - start) * i / steps) for i in range(steps + 1)]
    d.polygon(pts, fill=fill)


def point(deg, base_r, base_half, tip, ridge_from, ridge_to, grow=0.0):
    """A compass point: a triangle from its base (hidden under the hub) to its tip, straight sides; its two faces
    split by a ridge that narrows to nothing before the tip. With `grow`, only the enlarged triangle is cut (its gap)."""
    p = axis(deg)
    if grow:
        d.polygon([p(base_r - grow, -(base_half + grow)), p(tip + grow * 2.4, 0), p(base_r - grow, base_half + grow)], fill=OFF)
        return
    d.polygon([p(base_r, -base_half), p(tip, 0), p(base_r, base_half)], fill=ON)
    d.polygon([p(ridge_from, -2.6), p(ridge_to, 0), p(ridge_from, 2.6)], fill=OFF)


# The bezel behind the ring, round the bottom, its ends rising into fins.
band(388, 430, 80, 280, ON)
for side in (1, -1):
    d.polygon([at(388, 80 * side), at(430, 80 * side), at(428, 64 * side)], fill=ON)
# The ring, its slots (top half) and the teeth cut in its inner edge.
band(300, 374, 0, 360, ON)
for deg in (15, 45, 75, -15, -45, -75):
    p = axis(deg)
    d.polygon([p(318, -11), p(362, -11), p(362, 11), p(318, 11)], fill=OFF)
for deg in (30, 62, -30, -62):
    d.polygon([at(296, deg - 6), at(338, deg), at(296, deg + 6)], fill=OFF)
# The space inside the ring stays dark; the thin inner ring; inside it, with --disc, the disc the star lies on.
band(186, 202, 0, 360, ON)
if DISC:
    disc(178, ON)
# Short points under the long ones, their tips just touching the inner ring (owner, 2026-10-10), each cut free on
# the disc with --disc; then the long ones cut free and drawn.
for deg in (45, 135, 225, 315):
    if DISC:
        point(deg, 40, 56, 186, 0, 0, grow=8)
    point(deg, 40, 56, 186, 80, 174)
for deg in (0, 90, 180, 270):
    point(deg, 40, 51, 490, 0, 0, grow=8)
for deg in (0, 90, 180, 270):
    point(deg, 40, 51, 490, 80, 470)
# The hub, cut free; the Pathfinder Society's glyph cut in it, as on the original's.
disc(82, OFF)
disc(68, ON)
src = open(GLYPH_SVG, encoding='utf-8').read()
glyph = re.search(r'<path\s+d="([^"]+)"\s+id="path3068"', src).group(1)
centre, scale = (354.76, 370.06), 68 / 222       # the glyph's medallion centre; its reach (190.6) to 86 % of the hub
mask = Image.new('1', (N, N), 0)
for poly in polygons(glyph):
    tmp = Image.new('1', (N, N), 0)
    ImageDraw.Draw(tmp).polygon([(C + (x - centre[0]) * scale * K, C + (y - centre[1]) * scale * K) for x, y in poly], fill=1)
    mask = Image.frombytes('1', (N, N), bytes(a ^ b for a, b in zip(mask.tobytes(), tmp.tobytes())))
img.paste(OFF, (0, 0), mask)

small = img.resize((1000, 1000), Image.LANCZOS)
small.save(os.path.join(OUT, 'starfinder_drawn.png'))
px = small.load()
field = [[px[x, y] / 255 for x in range(1000)] for y in range(1000)]
side, path, loops = trace(field, 1000, 1000, blur=1, min_area=6, eps=0.4, margin=1.0)
print('%.1f' % side)
print(path)
preview(loops, side, os.path.join(OUT, 'starfinder_trace.png'))
