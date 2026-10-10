# -*- coding: utf-8 -*-
"""Tracing a picture into one even-odd path, pure Python (no numpy here): a 0..1 field of what is filled, a box blur
for smooth edges, marching squares at 0.5, loops linked by edge, small specks dropped, Ramer-Douglas-Peucker."""
import sys
from PIL import Image, ImageDraw

sys.setrecursionlimit(10000)


def box_blur(field, w, h, r):
    """A separable box blur of radius r over a list-of-rows field."""
    if r <= 0:
        return field
    out = []
    for row in field:
        acc = [0.0]
        for v in row:
            acc.append(acc[-1] + v)
        out.append([(acc[min(w, x + r + 1)] - acc[max(0, x - r)]) / (min(w, x + r + 1) - max(0, x - r)) for x in range(w)])
    cols = []
    res = [[0.0] * w for _ in range(h)]
    for x in range(w):
        acc = [0.0]
        for y in range(h):
            acc.append(acc[-1] + out[y][x])
        for y in range(h):
            res[y][x] = (acc[min(h, y + r + 1)] - acc[max(0, y - r)]) / (min(h, y + r + 1) - max(0, y - r))
    return res


def interp(a, b):
    return a / (a - b) if a != b else 0.5


def contours(field, w, h, iso=0.5):
    f = [[v - iso for v in row] for row in field]
    segments = []
    for y in range(h - 1):
        row, nxt = f[y], f[y + 1]
        for x in range(w - 1):
            tl, tr, br, bl = row[x], row[x + 1], nxt[x + 1], nxt[x]
            case = (tl > 0) * 8 + (tr > 0) * 4 + (br > 0) * 2 + (bl > 0)
            if case in (0, 15):
                continue
            top = (('h', x, y), (x + interp(tl, tr), y))
            right = (('v', x + 1, y), (x + 1, y + interp(tr, br)))
            bottom = (('h', x, y + 1), (x + interp(bl, br), y + 1))
            left = (('v', x, y), (x, y + interp(tl, bl)))
            pairs = {
                1: [(left, bottom)], 2: [(bottom, right)], 3: [(left, right)], 4: [(top, right)],
                6: [(top, bottom)], 7: [(left, top)], 8: [(left, top)], 9: [(top, bottom)],
                11: [(top, right)], 12: [(left, right)], 13: [(bottom, right)], 14: [(left, bottom)],
            }
            if case in (5, 10):
                centre = (tl + tr + br + bl) / 4 > 0
                if case == 5:
                    pairs[5] = [(left, top), (bottom, right)] if centre else [(left, bottom), (top, right)]
                else:
                    pairs[10] = [(left, bottom), (top, right)] if centre else [(left, top), (bottom, right)]
            segments.extend(pairs[case])
    ends = {}
    for i, (a, b) in enumerate(segments):
        ends.setdefault(a[0], []).append(i)
        ends.setdefault(b[0], []).append(i)
    used = [False] * len(segments)
    loops = []
    for start in range(len(segments)):
        if used[start]:
            continue
        used[start] = True
        a, b = segments[start]
        loop = [a[1], b[1]]
        key, first = b[0], a[0]
        while key != first:
            nxt = [i for i in ends.get(key, []) if not used[i]]
            if not nxt:
                break
            i = nxt[0]
            used[i] = True
            p, q = segments[i]
            if p[0] == key:
                loop.append(q[1]); key = q[0]
            else:
                loop.append(p[1]); key = p[0]
        loops.append(loop)
    return loops


def area(loop):
    return 0.5 * sum(loop[i][0] * loop[i - 1][1] - loop[i - 1][0] * loop[i][1] for i in range(len(loop)))


def rdp(points, eps):
    if len(points) < 3:
        return points
    (x1, y1), (x2, y2) = points[0], points[-1]
    dx, dy = x2 - x1, y2 - y1
    norm = (dx * dx + dy * dy) ** 0.5 or 1e-9
    far, index = 0, 0
    for i in range(1, len(points) - 1):
        d = abs(dy * (points[i][0] - x1) - dx * (points[i][1] - y1)) / norm
        if d > far:
            far, index = d, i
    if far > eps:
        return rdp(points[:index + 1], eps)[:-1] + rdp(points[index:], eps)
    return [points[0], points[-1]]


def simplify_loop(loop, eps):
    b = max(range(len(loop)), key=lambda i: (loop[i][0] - loop[0][0]) ** 2 + (loop[i][1] - loop[0][1]) ** 2)
    return rdp(loop[:b + 1], eps)[:-1] + rdp(loop[b:] + [loop[0]], eps)[:-1]


def trace(field, w, h, blur=2, min_area=40.0, eps=0.4, margin=1.02):
    """The path (for a square viewport of the returned side) and the simplified loops in viewport coordinates."""
    side, path, loops, _, _ = trace_box(field, w, h, blur, min_area, eps, margin)
    return side, path, loops


def trace_box(field, w, h, blur=2, min_area=40.0, eps=0.4, margin=1.02):
    """As trace, and where the picture's origin lands: the viewport's (ox, oy) in the picture's coordinates."""
    smooth = box_blur(field, w, h, blur)
    loops = [l for l in contours(smooth, w, h) if abs(area(l)) > min_area]
    xs = [p[0] for l in loops for p in l]
    ys = [p[1] for l in loops for p in l]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    side = max(x1 - x0, y1 - y0) * margin
    ox = x0 - (side - (x1 - x0)) / 2
    oy = y0 - (side - (y1 - y0)) / 2
    simple = [[(p[0] - ox, p[1] - oy) for p in simplify_loop(l, eps)] for l in loops]
    simple = [l for l in simple if len(l) >= 3]
    path = ''.join('M' + ' L'.join('%.1f %.1f' % p for p in l) + 'Z' for l in simple)
    return side, path, simple, ox, oy


def preview(loops, side, out_path, size=420, fill=(198, 163, 108), bg=(18, 16, 22)):
    """The loops filled even-odd, gold on the dark card."""
    s = size / side
    mask = Image.new('1', (size, size), 0)
    for l in loops:
        tmp = Image.new('1', (size, size), 0)
        ImageDraw.Draw(tmp).polygon([(p[0] * s, p[1] * s) for p in l], fill=1)
        mask = Image.frombytes('1', (size, size), bytes(a ^ b for a, b in zip(mask.tobytes(), tmp.tobytes())))
    im = Image.new('RGB', (size, size), bg)
    im.paste(Image.new('RGB', (size, size), fill), (0, 0), mask)
    im.save(out_path)
    return im
