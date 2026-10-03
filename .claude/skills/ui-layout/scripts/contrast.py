"""WCAG contrast of design_tokens.json colours, the translucent ones laid over the real background first.

Usage:
  python contrast.py <foreground> <background> [<under the background>]
  python contrast.py --table

A colour is a token name — `text.primary`, `surface.card`, `accent.heal` (colors.app) or
`materialTheme.primary` / `primary` (colors.materialTheme) — or a hex: `#C6A36C`, `#29E85C5C` (ARGB).
A translucent background (a fill at 16 %) is laid over the third colour, by default the dialog's
`materialTheme.surface`.

Thresholds: text 4.5, large text (18sp+, or 14sp+ bold), icons and meaningful marks 3.0; a fill
that has to stand out from what is around it by lightness about 1.4. Below that a tinted fill (the
danger red at 16 %) can still stand out by its hue, which this number doesn't see.
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
TOKENS = json.load(open(os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'design_tokens.json'), encoding='utf-8'))['colors']


def resolve(name):
    if name.startswith('#'):
        return name
    if name.startswith('materialTheme.'):
        return TOKENS['materialTheme'][name.split('.', 1)[1]]
    if '.' in name:
        group, key = name.split('.', 1)
        return TOKENS['app'][group][key]
    return TOKENS['materialTheme'][name]


def argb(value):
    hex_digits = resolve(value).lstrip('#')
    alpha = int(hex_digits[:2], 16) / 255 if len(hex_digits) == 8 else 1.0
    rgb = hex_digits[-6:]
    return tuple(int(rgb[i:i + 2], 16) for i in (0, 2, 4)), alpha


def over(color, under):
    (rgb, alpha), (base, _) = argb(color), argb(under)
    return tuple(rgb[i] * alpha + base[i] * (1 - alpha) for i in range(3))


def luminance(rgb):
    def channel(v):
        v /= 255
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    return 0.2126 * channel(rgb[0]) + 0.7152 * channel(rgb[1]) + 0.0722 * channel(rgb[2])


def contrast(foreground, background, under='materialTheme.surface'):
    back = over(background, under)
    back_hex = '#%02X%02X%02X' % tuple(round(c) for c in back)
    front = over(foreground, back_hex)
    a, b = luminance(front), luminance(back)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)


def verdict(ratio):
    if ratio >= 4.5:
        return 'текст'
    if ratio >= 3.0:
        return 'крупный текст, иконка'
    if ratio >= 1.4:
        return 'заливка отличима по светлоте'
    return 'по светлоте не отличима: выделяется только оттенком или не выделяется'


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    if sys.argv[1:] == ['--table']:
        backs = ['surface.card', 'materialTheme.surface', 'surface.selected', 'materialTheme.primaryContainer', 'materialTheme.primary']
        fronts = ['text.' + k for k in TOKENS['app']['text']] + ['accent.' + k for k in ('inspiration', 'heal', 'dangerHpZero', 'hpTemporary')] + \
                 ['materialTheme.onPrimary', 'materialTheme.onPrimaryContainer', 'materialTheme.onSurfaceVariant']
        print('%-34s' % '' + ''.join('%17s' % b.split('.')[-1] for b in backs))
        for f in fronts:
            print('%-34s' % f + ''.join('%17.1f' % contrast(f, b) for b in backs))
    else:
        ratio = contrast(*sys.argv[1:4])
        print('%.2f:1  %s' % (ratio, verdict(ratio)))
