"""Turns Foundry description HTML, with its enrichers (@UUID, [[/r]], @Embed, &Reference ...), into
the plain text the app shows.

Numbers that depend on the character — scale values, ability modifiers, proficiency, class levels —
stay in the text as {=FORMULA} tokens holding the Foundry formula (e.g. {=@scale.barbarian.rages},
{=1d8 + @abilities.wis.mod}); the app fills them in for the character, or shows a readable label.
"""
import html
import re
from html.parser import HTMLParser

# (English, Russian); Russian ability names in the genitive, as in "спасбросок Мудрости".
ABILITIES = {
    'str': ('Strength', 'Силы'), 'dex': ('Dexterity', 'Ловкости'), 'con': ('Constitution', 'Телосложения'),
    'int': ('Intelligence', 'Интеллекта'), 'wis': ('Wisdom', 'Мудрости'), 'cha': ('Charisma', 'Харизмы'),
}
SKILLS = {
    'acr': ('Acrobatics', 'Акробатика'), 'ani': ('Animal Handling', 'Уход за животными'), 'arc': ('Arcana', 'Магия'),
    'ath': ('Athletics', 'Атлетика'), 'dec': ('Deception', 'Обман'), 'his': ('History', 'История'),
    'ins': ('Insight', 'Проницательность'), 'itm': ('Intimidation', 'Запугивание'), 'inv': ('Investigation', 'Анализ'),
    'med': ('Medicine', 'Медицина'), 'nat': ('Nature', 'Природа'), 'prc': ('Perception', 'Внимательность'),
    'prf': ('Performance', 'Выступление'), 'per': ('Persuasion', 'Убеждение'), 'rel': ('Religion', 'Религия'),
    'slt': ('Sleight of Hand', 'Ловкость рук'), 'ste': ('Stealth', 'Скрытность'), 'sur': ('Survival', 'Выживание'),
}
CURRENCY = {'pp': ('PP', 'пм'), 'gp': ('GP', 'зм'), 'ep': ('EP', 'эм'), 'sp': ('SP', 'см'), 'cp': ('CP', 'мм')}
# Damage types as Foundry's damage enricher names them ("8d6 Fire" / "8d6 Огонь", the ru-ru labels).
DAMAGE_TYPES = {
    'acid': ('Acid', 'Кислота'), 'bludgeoning': ('Bludgeoning', 'Дробящий'), 'cold': ('Cold', 'Холод'),
    'fire': ('Fire', 'Огонь'), 'force': ('Force', 'Сила'), 'lightning': ('Lightning', 'Молния'),
    'necrotic': ('Necrotic', 'Некротический'), 'piercing': ('Piercing', 'Колющий'), 'poison': ('Poison', 'Яд'),
    'psychic': ('Psychic', 'Психический'), 'radiant': ('Radiant', 'Лучистый'), 'slashing': ('Slashing', 'Режущий'),
    'thunder': ('Thunder', 'Гром')
}
# English names of the rules &Reference[...] points at, by key without spaces or hyphens. The Russian
# names are the ones Foundry shows (Fifthpendium's rules glossary), loaded by load_reference_labels.
REFERENCES_EN = {
    'advantage': 'Advantage', 'attack': 'Attack', 'blinded': 'Blinded', 'blindsight': 'Blindsight',
    'bloodied': 'Bloodied', 'bonusaction': 'Bonus Action', 'brightlight': 'Bright Light', 'charmed': 'Charmed',
    'concentration': 'Concentration', 'construct': 'Construct', 'criticalhits': 'Critical Hit', 'd20test': 'D20 Test',
    'darkness': 'Darkness', 'darkvision': 'Darkvision', 'dash': 'Dash', 'deafened': 'Deafened',
    'deathsaves': 'Death Saving Throws', 'difficultterrain': 'Difficult Terrain', 'dimlight': 'Dim Light',
    'disadvantage': 'Disadvantage', 'disengage': 'Disengage', 'dodge': 'Dodge', 'exhaustion': 'Exhaustion',
    'expertise': 'Expertise', 'flying': 'Fly Speed', 'friendly': 'Friendly', 'frightened': 'Frightened',
    'grappled': 'Grappled', 'halfcover': 'Half Cover', 'help': 'Help', 'hide': 'Hide', 'highjump': 'High Jump',
    'hover': 'Hover', 'immunity': 'Immunity', 'incapacitated': 'Incapacitated', 'influence': 'Influence',
    'initiative': 'Initiative', 'inspiration': 'Heroic Inspiration', 'invisible': 'Invisible', 'jumping': 'Jumping',
    'longjump': 'Long Jump', 'longrest': 'Long Rest', 'magic': 'Magic', 'material': 'Material', 'necrotic': 'Necrotic',
    'opportunityattacks': 'Opportunity Attacks', 'paralyzed': 'Paralyzed', 'petrified': 'Petrified',
    'poisoned': 'Poisoned', 'prone': 'Prone', 'radiant': 'Radiant', 'reaction': 'Reaction', 'ready': 'Ready',
    'resistance': 'Resistance', 'restrained': 'Restrained', 'ritual': 'Ritual', 'sap': 'Sap', 'search': 'Search',
    'shortrest': 'Short Rest', 'size': 'Size', 'slow': 'Slow', 'speed': 'Speed', 'study': 'Study', 'stunned': 'Stunned',
    'threequarterscover': 'Three-Quarters Cover', 'topple': 'Topple', 'totalcover': 'Total Cover',
    'tremorsense': 'Tremorsense', 'truesight': 'Truesight', 'unarmedstrike': 'Unarmed Strike',
    'unconscious': 'Unconscious', 'utilize': 'Utilize', 'vex': 'Vex', 'vulnerability': 'Vulnerability',
    'cleave': 'Cleave', 'graze': 'Graze', 'nick': 'Nick', 'push': 'Push',
}
REFERENCES_RU = {}


def reference_key(key):
    return re.sub(r'[\s-]+', '', key.lower())


def load_reference_labels(labels):
    """Russian names for &Reference keys, as Foundry shows them (export/references.json)."""
    REFERENCES_RU.update({reference_key(k): v for k, v in labels.items() if v})

NAME_WITH_ENGLISH = re.compile(r'^(.*?)\s*\[([^\[\]]+)\]\s*$')


def split_name(name):
    """'Ярость [Rage]' -> ('Ярость', 'Rage'); a name without the English part -> (name, '')."""
    name = (name or '').strip()
    match = NAME_WITH_ENGLISH.match(name)
    if match and match.group(1):
        return match.group(1).strip(), match.group(2).strip()
    return name, ''


def pick(pair, lang):
    return pair[0] if lang == 'en' else pair[1]


def formula_text(formula, lang):
    """A roll formula as text: plain dice stay as they are, character-dependent parts become a token."""
    formula = ' '.join(formula.split())
    if '@' in formula:
        return '{=' + formula + '}'
    return formula


def _roll(command, body, label, lang):
    body = ' '.join(body.split())
    if command in ('r', 'roll', 'damage', 'heal', 'healing'):
        # Drop options (average) and flavour (#...); keep the formula and, like Foundry's damage
        # enricher, the damage type after it ("8d6 Fire", "8d6 Огонь").
        body = body.split('#')[0]
        types = []
        for option in body.split(' '):
            if option.startswith('type='):
                types += [t for t in re.split(r'[|,/]', option[len('type='):]) if t]
        parts = [p for p in body.split(' ') if p and '=' not in p]
        # A trailing damage type ("1d8 slashing") is a word after the formula.
        while parts and re.fullmatch(r'[a-z]+', parts[-1]) and parts[-1] not in ('d', 'floor', 'ceil'):
            types.insert(0, parts.pop())
        formula = ' '.join(parts)
        if label:
            return label
        if not formula:
            return ''
        named = [pick(DAMAGE_TYPES[t], lang) for t in types if t in DAMAGE_TYPES] if command == 'damage' else []
        joiner = ' or ' if lang == 'en' else ' или '
        return formula_text(formula, lang) + (' ' + joiner.join(named) if named else '')
    if command == 'award':
        match = re.fullmatch(r'(\d+)\s*([a-z]{2})', body.lower())
        if match and match.group(2) in CURRENCY:
            return f'{match.group(1)} {pick(CURRENCY[match.group(2)], lang)}'
        return label or body
    if command == 'save':
        if label:
            return label
        ability = next((a for a in body.split() if a in ABILITIES), None)
        dc = re.search(r'(?:dc=)?(\d+)', body)
        if not ability:
            return ''
        if lang == 'en':
            return f'DC {dc.group(1)} {ABILITIES[ability][0]} saving throw' if dc else f'{ABILITIES[ability][0]} saving throw'
        return f'спасбросок {ABILITIES[ability][1]}' + (f' Сл {dc.group(1)}' if dc else '')
    if command in ('check', 'skill'):
        if label:
            return label
        tokens = body.split()
        skill = next((t for t in tokens if t in SKILLS), None)
        dc = next((t for t in tokens if t.isdigit()), None)
        name = pick(SKILLS[skill], lang) if skill else ' '.join(tokens)
        if dc:
            return f'{name} (DC {dc})' if lang == 'en' else f'{name} (Сл {dc})'
        return name
    if command == 'item':
        if label:
            return label
        name = body.split(' activity=')[0]
        ru, en = split_name(name)
        return (en or ru) if lang == 'en' else ru
    return label or body


INLINE_ROLL = re.compile(r'\[\[(/?)([^\[\]]*(?:\[[^\[\]]*\][^\[\]]*)*)\]\](?:\{([^{}]*(?:\[\[[^\]]*\]\][^{}]*)*)\})?')


def replace_inline_rolls(text, lang):
    def replace(match):
        slash, body, label = match.group(1), match.group(2).strip(), match.group(3)
        if label:
            label = replace_inline_rolls(label, lang)
        if slash:
            command, _, rest = body.partition(' ')
            return _roll(command.lower(), rest if rest else '', label, lang)
        if body.startswith('lookup '):
            return label or formula_text(body[len('lookup '):].strip(), lang)
        # A bare deferred roll: [[@scale.x.y]], [[4 + @prof]], [[(floor(@classes.druid.levels / 3))]].
        return label or formula_text(body, lang)
    previous = None
    while previous != text:
        previous = text
        text = INLINE_ROLL.sub(replace, text)
    return text


class _TextBuilder(HTMLParser):
    """Block-aware HTML -> text: paragraphs and headings on their own lines, list items as bullets,
    table rows as cells joined with ' | '. GM-only secret sections and images are dropped."""

    BLOCKS = {'p', 'div', 'section', 'article', 'aside', 'blockquote', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
              'figcaption', 'caption', 'dl', 'dt', 'dd', 'hr'}

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.lines = []
        self.current = []
        self.skip_depth = 0
        self.list_stack = []
        self.row = None
        self.cell = None

    def _flush(self, prefix=''):
        text = ' '.join(''.join(self.current).split())
        self.current = []
        if text:
            self.lines.append(prefix + text)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        classes = (attrs.get('class') or '').split()
        if self.skip_depth:
            if tag not in ('br', 'hr', 'img', 'input'):
                self.skip_depth += 1
            return
        if 'secret' in classes or tag in ('img', 'figure', 'script', 'style', 'template'):
            if tag not in ('img',):
                self.skip_depth = 1
            return
        if tag == 'br':
            self.current.append('\n')
            return
        if tag in ('ul', 'ol'):
            self._flush()
            self.list_stack.append([tag, 0])
            return
        if tag == 'li':
            self._flush()
            if self.list_stack:
                self.list_stack[-1][1] += 1
            return
        if tag == 'tr':
            self._flush()
            self.row = []
            return
        if tag in ('td', 'th'):
            self.cell = []
            return
        if tag in self.BLOCKS:
            self._flush()

    def handle_endtag(self, tag):
        if self.skip_depth:
            self.skip_depth -= 1
            return
        if tag in ('td', 'th') and self.cell is not None and self.row is not None:
            self.row.append(' '.join(''.join(self.cell).split()))
            self.cell = None
            return
        if tag == 'tr' and self.row is not None:
            cells = [c for c in self.row]
            if any(cells):
                self.lines.append(' | '.join(cells))
            self.row = None
            return
        if tag == 'li':
            prefix = '• '
            if self.list_stack and self.list_stack[-1][0] == 'ol':
                prefix = f'{self.list_stack[-1][1]}. '
            self._flush(prefix)
            return
        if tag in ('ul', 'ol'):
            self._flush()
            if self.list_stack:
                self.list_stack.pop()
            return
        if tag in self.BLOCKS:
            self._flush()

    def handle_data(self, data):
        if self.skip_depth:
            return
        if self.cell is not None:
            self.cell.append(data)
        else:
            self.current.append(data)

    def text(self):
        self._flush()
        lines = []
        for line in self.lines:
            for part in line.split('\n'):
                part = ' '.join(part.split())
                if part:
                    lines.append(part)
        return '\n'.join(lines)


def html_to_text(value):
    builder = _TextBuilder()
    builder.feed(value or '')
    builder.close()
    return builder.text()


EMBED = re.compile(r'@[Ee]mbed\[([^\]\s]+)([^\]]*)\](?:\{([^}]*)\})?')
UUID = re.compile(r'@UUID\[([^\]]+)\](?:\{([^}]*)\})?')
# The ampersand is sometimes stored HTML-escaped.
REFERENCE = re.compile(r'&(?:amp;)?[Rr]eference\[([^\]]+)\](?:\{([^}]*)\})?')
PLACEHOLDER = '\u0001EMBED{}\u0001'


def description_text(value, lang, resolve_name, render_embed):
    """Plain text of a description in [lang] ('en' or 'ru').

    resolve_name(uuid, lang) -> name of a linked document, or None.
    render_embed(uuid, lang) -> text block for an embedded table/page/item, or None.
    """
    if not value:
        return ''
    embeds = []

    def embed(match):
        block = render_embed(match.group(1), lang)
        if not block:
            return match.group(3) or ''
        embeds.append(block)
        # A paragraph of its own, so the block's lines don't merge with the text around it.
        return '<p>' + PLACEHOLDER.format(len(embeds) - 1) + '</p>'

    value = EMBED.sub(embed, value)
    value = UUID.sub(lambda m: m.group(2) or resolve_name(m.group(1), lang) or '', value)

    def reference(match):
        if match.group(2):
            return match.group(2)
        # "Unconscious apply=false": options after the name aren't part of it.
        key = ' '.join(word for word in match.group(1).split('|')[0].split() if '=' not in word)
        normalized = reference_key(key)
        if lang == 'ru' and normalized in REFERENCES_RU:
            return REFERENCES_RU[normalized]
        return REFERENCES_EN.get(normalized) or key

    value = REFERENCE.sub(reference, value)
    value = replace_inline_rolls(value, lang)
    text = html_to_text(value)
    for index, block in enumerate(embeds):
        text = text.replace(PLACEHOLDER.format(index), block)
    return html.unescape(text).strip()


def table_text(table, name, lang, inline_text):
    """A roll table as text: its name, then 'range | result' rows."""
    lines = [name] if name else []
    formula = (table.get('formula') or '').strip()
    rows = []
    for result in table.get('results') or []:
        low, high = (result.get('range') or [None, None])[:2]
        span = '' if low is None else (str(low) if low == high else f'{low}–{high}')
        rows.append(f'{span} | {inline_text(result.get("text") or "")}'.strip(' |'))
    if formula and rows:
        lines.append(f'{formula} | ' + ('Result' if lang == 'en' else 'Результат'))
    lines.extend(rows)
    return '\n'.join(lines)
