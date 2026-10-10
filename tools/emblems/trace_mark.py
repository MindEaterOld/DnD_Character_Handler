# -*- coding: utf-8 -*-
"""Traces a one-coloured mark on a dark ground (as the Vampire: The Masquerade ankh, red on black) into one even-odd
path: what is brighter than `iso` in the red channel is the mark.

    python tools/emblems/trace_mark.py picture.jpg [iso=105] [eps=0.35] [out_dir]

Prints the viewport's side and the path; writes mark_trace.png (gold on the card) into out_dir.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from trace_lib import preview, trace  # noqa: E402

src = sys.argv[1]
iso = float(sys.argv[2]) if len(sys.argv) > 2 else 105.0
eps = float(sys.argv[3]) if len(sys.argv) > 3 else 0.35
out = sys.argv[4] if len(sys.argv) > 4 else '.'

im = Image.open(src).convert('RGB')
w, h = im.size
px = im.load()
# 0..1 around the iso level, so the edge falls between pixels where the picture's own antialiasing puts it.
field = [[min(1.0, max(0.0, 0.5 + (px[x, y][0] - iso) / 60.0)) for x in range(w)] for y in range(h)]
side, path, loops = trace(field, w, h, blur=0, min_area=200, eps=eps, margin=1.02)
print('%.1f' % side)
print(path)
preview(loops, side, os.path.join(out, 'mark_trace.png'))
