# -*- coding: utf-8 -*-
"""Draws the Starfinder Society's compass in one colour by the original's measures (1000 units, centre 500,500) and
traces it into one even-odd path for StarfinderEmblem (app/.../presentation/components/GameSystemEmblems.kt).

The parts: the bezel round the bottom with its two fins, the ring with six slots and four teeth on its inner edge,
the space inside left dark, the thin inner ring, four short points under four long ones (cut free by a gap), the hub
with a small four-point star. Drawn at 3x, brought down to 1000 for smooth edges.

    python tools/emblems/draw_starfinder.py [out_dir]

Prints the viewport's side and the path; writes starfinder_drawn.png and starfinder_trace.png into out_dir.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from trace_lib import preview, trace  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else '.'
K = 3
N = 1000 * K
C = 500 * K
ON, OFF = 255, 0
img = Image.new('L', (N, N), OFF)
d = ImageDraw.Draw(img)


def at(r, deg):
    """A point r from the centre, deg clockwise from north (canvas units)."""
    a = math.radians(deg)
    return (C + r * K * math.sin(a), C - r * K * math.cos(a))


def disc(r, fill):
    d.ellipse((C - r * K, C - r * K, C + r * K, C + r * K), fill=fill)


def band(r_in, r_out, start, end, fill, steps=180):
    """An arc of a ring from start to end degrees (clockwise from north)."""
    pts = [at(r_out, start + (end - start) * i / steps) for i in range(steps + 1)]
    pts += [at(r_in, end - (end - start) * i / steps) for i in range(steps + 1)]
    d.polygon(pts, fill=fill)


def kite(deg, tip, shoulder_r, half, fill, grow=0.0):
    """A compass point toward deg: centre, shoulder, tip, shoulder; grown by `grow` units all round (for its gap)."""
    a = math.radians(deg)
    ux, uy = math.sin(a), -math.cos(a)
    px, py = -uy, ux

    def p(along, across):
        return (C + (along * ux + across * px) * K, C + (along * uy + across * py) * K)

    g = grow
    d.polygon([p(-g, 0), p(shoulder_r, -(half + g)), p(tip + g * 2.2, 0), p(shoulder_r, half + g)], fill=fill)


# The bezel behind the ring, round the bottom, its ends rising into fins.
band(388, 430, 80, 280, ON)
for side in (1, -1):
    d.polygon([at(388, 80 * side), at(430, 80 * side), at(428, 64 * side)], fill=ON)
# The ring, its slots (top half) and the teeth cut in its inner edge.
band(300, 374, 0, 360, ON)
for deg in (15, 45, 75, -15, -45, -75):
    half_w, r0, r1 = 11, 318, 362
    a = math.radians(deg)
    ux, uy = math.sin(a), -math.cos(a)
    px, py = -uy, ux
    d.polygon([(C + (r * ux + s * px) * K, C + (r * uy + s * py) * K)
               for r, s in ((r0, -half_w), (r1, -half_w), (r1, half_w), (r0, half_w))], fill=OFF)
for deg in (30, 62, -30, -62):
    d.polygon([at(296, deg - 6), at(338, deg), at(296, deg + 6)], fill=OFF)
# The space inside the ring stays dark; the thin inner ring.
band(186, 202, 0, 360, ON)
# Short points under the long ones.
for deg in (45, 135, 225, 315):
    kite(deg, 212, 78, 38, ON)
# Long points, each cut free by a gap first.
for deg in (0, 90, 180, 270):
    kite(deg, 490, 88, 42, OFF, grow=9)
for deg in (0, 90, 180, 270):
    kite(deg, 490, 88, 42, ON)
# The hub: cut free, filled, a small four-point star cut in it.
disc(80, OFF)
disc(66, ON)
for deg in (0, 90, 180, 270):
    kite(deg, 34, 10, 9, OFF)

small = img.resize((1000, 1000), Image.LANCZOS)
small.save(os.path.join(OUT, 'starfinder_drawn.png'))
px = small.load()
field = [[px[x, y] / 255 for x in range(1000)] for y in range(1000)]
side, path, loops = trace(field, 1000, 1000, blur=1, min_area=20, eps=0.45, margin=1.0)
print('%.1f' % side)
print(path)
preview(loops, side, os.path.join(OUT, 'starfinder_trace.png'))
