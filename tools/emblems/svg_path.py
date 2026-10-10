# -*- coding: utf-8 -*-
"""An SVG path's subpaths as polygons: M/m, L/l, H/h, V/v, C/c, Z/z (what the Pathfinder glyph uses), cubic curves cut
into short lines."""
import re

_TOKEN = re.compile(r'[MmLlHhVvCcZz]|-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?')


def polygons(d, steps=12):
    tokens = _TOKEN.findall(d)
    out, current = [], []
    x = y = 0.0
    start = (0.0, 0.0)
    cmd = None
    i = 0

    def num():
        nonlocal i
        v = float(tokens[i])
        i += 1
        return v

    while i < len(tokens):
        if re.match(r'[A-Za-z]', tokens[i]):
            cmd = tokens[i]
            i += 1
            if cmd in 'Zz':
                if current:
                    out.append(current)
                current = []
                x, y = start
                continue
        rel = cmd.islower()
        c = cmd.upper()
        if c == 'M':
            nx, ny = num(), num()
            x, y = (x + nx, y + ny) if rel else (nx, ny)
            if current:
                out.append(current)
            current = [(x, y)]
            start = (x, y)
            cmd = 'l' if rel else 'L'      # further pairs are lines
        elif c == 'L':
            nx, ny = num(), num()
            x, y = (x + nx, y + ny) if rel else (nx, ny)
            current.append((x, y))
        elif c == 'H':
            nx = num()
            x = x + nx if rel else nx
            current.append((x, y))
        elif c == 'V':
            ny = num()
            y = y + ny if rel else ny
            current.append((x, y))
        elif c == 'C':
            pts = [num() for _ in range(6)]
            if rel:
                pts = [pts[0] + x, pts[1] + y, pts[2] + x, pts[3] + y, pts[4] + x, pts[5] + y]
            x0, y0 = x, y
            for k in range(1, steps + 1):
                t = k / steps
                mt = 1 - t
                bx = mt ** 3 * x0 + 3 * mt * mt * t * pts[0] + 3 * mt * t * t * pts[2] + t ** 3 * pts[4]
                by = mt ** 3 * y0 + 3 * mt * mt * t * pts[1] + 3 * mt * t * t * pts[3] + t ** 3 * pts[5]
                current.append((bx, by))
            x, y = pts[4], pts[5]
    if current:
        out.append(current)
    return out
