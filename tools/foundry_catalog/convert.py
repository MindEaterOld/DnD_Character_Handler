"""Builds the app's character catalog (app/src/main/assets/character_catalog.json) from the Foundry
compendium dumps in ./export (see export_foundry.js).

Russian comes from AG Fifthpendium (full PHB 2024 plus supplements, names like 'Ярость [Rage]').
English comes from the dnd5e SRD 5.2 packs for the documents both share (same Foundry ids), and from
the texts the app shipped before this catalog (./legacy) for the rest.

Usage: python convert.py [--out PATH]
"""
import argparse
import collections
import json
import os
import re
import sys

from foundry_text import description_text, split_name, table_text

HERE = os.path.dirname(os.path.abspath(__file__))
EXPORT = os.path.join(HERE, 'export')
LEGACY = os.path.join(HERE, 'legacy')
REPO = os.path.dirname(os.path.dirname(HERE))
SRD_2024 = os.path.join(REPO, 'external', '5e-database', 'src', '2024', 'en')
DEFAULT_OUT = os.path.join(REPO, 'app', 'src', 'main', 'assets', 'character_catalog.json')

PRIMARY_PACKS = [
    'ag-fifthpendium.classes', 'ag-fifthpendium.feats', 'ag-fifthpendium.origins',
    'ag-fifthpendium-dndbeyond.options', 'ag-fifthpendium-eberron.options',
    'ag-fifthpendium-forgotten-realms.options', 'ag-fifthpendium-ravenloft.options',
]
SRD_PACKS = ['dnd5e.classes24', 'dnd5e.feats24', 'dnd5e.origins24']

BOOKS = {
    'PHB 2024': ("Player's Handbook (2024)", 'Книга игрока (2024)'),
    'DMG 2024': ("Dungeon Master's Guide (2024)", 'Руководство мастера подземелий (2024)'),
    'TPC 2024': ('The Pugilist Class', 'Класс Кулачник'),
    'UA': ('Unearthed Arcana', 'Раскопанные тайны'),
    'EFA': ('Eberron: Forge of the Artificer', 'Эберрон: Кузня Артефактора'),
    'EE': ('Exploring Eberron', 'Исследуя Эберрон'),
    'HoF': ('Heroes of Faerûn', 'Герои Фейруна'),
    'ABoH': ("Astarion's Book of Hungers", 'Книга страстей Астариона'),
    'LFL': ('Lorwyn: First Light', 'Лорвин: Первый свет'),
    'RHW': ('Ravenloft: The Horrors Within', 'Рейвенлофт: Ужасы внутри'),
}
BOOK_ALIASES = {'Hof': 'HoF'}

# Feature and feat subtypes (Foundry's CONFIG.DND5E.featureTypes), as the app labels them.
SUBTYPES = {
    'channelDivinity': ('Channel Divinity', 'Проведение божественной силы'),
    'eldritchInvocation': ('Eldritch Invocation', 'Потусторонняя инвокация'),
    'maneuver': ('Maneuver', 'Боевой приём'),
    'metamagic': ('Metamagic', 'Метамагия'),
    'fightingStyle': ('Fighting Style', 'Боевой стиль'),
    'artificerInfusion': ('Artificer Infusion', 'Инфузия'),
    'ki': ('Focus', 'Сосредоточенность'),
    'general': ('General Feat', 'Общая черта'),
    'origin': ('Origin Feat', 'Черта происхождения'),
    'epicBoon': ('Epic Boon', 'Эпический дар'),
    'dragonmark': ('Dragonmark', 'Метка дракона'),
    'darkGift': ('Dark Gift', 'Тёмный дар'),
}


def load_pack(pack_id):
    with open(os.path.join(EXPORT, f'{pack_id}.json'), encoding='utf-8') as f:
        return json.load(f)


def load_json(path, default=None):
    if not os.path.exists(path):
        return default
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def normalize(name):
    name = (name or '').lower().replace('’', "'").replace('ё', 'е')
    return re.sub(r'[^a-zа-я0-9]+', ' ', name).strip()


def advancements(doc):
    raw = (doc.get('system') or {}).get('advancement') or {}
    return list(raw.values()) if isinstance(raw, dict) else list(raw)


def ref_uuid(entry):
    return entry.get('uuid') if isinstance(entry, dict) else entry


class Catalog:
    def __init__(self):
        self.packs = {pid: load_pack(pid) for pid in PRIMARY_PACKS}
        self.srd_packs = {pid: load_pack(pid) for pid in SRD_PACKS}
        self.docs = {}          # uuid -> primary document
        self.docs_by_id = {}    # Foundry _id -> primary document
        for pack in self.packs.values():
            for doc in pack['documents']:
                self.docs[doc['uuid']] = doc
                self.docs_by_id[doc['_id']] = doc
        self.srd = {}           # _id -> SRD document
        self.srd_by_uuid = {}
        for pack in self.srd_packs.values():
            for doc in pack['documents']:
                self.srd[doc['_id']] = doc
                self.srd_by_uuid[doc['uuid']] = doc
        self.spell_index = {}
        self.equipment_index = {}
        for name, target in (('ag-fifthpendium.spells', self.spell_index), ('dnd5e.spells24', self.spell_index),
                             ('ag-fifthpendium.equipment', self.equipment_index), ('dnd5e.equipment24', self.equipment_index)):
            data = load_json(os.path.join(EXPORT, f'index.{name}.json'), {'index': []})
            for entry in data['index']:
                target[entry['uuid']] = entry
        self.embeds = load_json(os.path.join(EXPORT, 'embeds.json'), {})
        self.warnings = collections.Counter()

    # --- names and texts -------------------------------------------------------------------------

    def english_twin(self, doc):
        return self.srd.get(doc['_id'])

    def names(self, doc):
        ru, en = split_name(doc['name'])
        twin = self.english_twin(doc)
        if twin:
            en = twin['name']
        return {'en': en, 'ru': ru}

    def resolve_name(self, uuid, lang):
        doc = self.docs.get(uuid)
        if doc is None and uuid.split('.')[-1] in self.docs_by_id and '.Item.' in uuid:
            doc = self.docs_by_id[uuid.split('.')[-1]]
        if doc is not None:
            names = self.names(doc)
            return (names['en'] or names['ru']) if lang == 'en' else names['ru']
        srd = self.srd_by_uuid.get(uuid)
        if srd is not None:
            return srd['name']
        entry = self.spell_index.get(uuid) or self.equipment_index.get(uuid) or self.embeds.get(uuid)
        if entry:
            ru, en = split_name(entry['name'])
            return (en or ru) if lang == 'en' else ru
        # Links into book journals ("...JournalEntry.phbSpells0000000") have no name to show.
        self.warnings['unresolved link'] += 1
        return None

    def render_embed(self, uuid, lang):
        entry = self.embeds.get(uuid)
        if not entry:
            return None
        ru, en = split_name(entry.get('name') or '')
        name = (en or ru) if lang == 'en' else ru
        if entry.get('table'):
            inline = lambda value: description_text(value, lang, self.resolve_name, lambda *_: None)
            return table_text(entry['table'], name, lang, inline)
        if entry['documentName'] in ('JournalEntryPage', 'JournalEntry'):
            return description_text(entry.get('html') or '', lang, self.resolve_name, lambda *_: None)
        # Items and actors (spells, magic items, statblocks) are referenced by name only.
        return name

    def text(self, html, lang):
        return description_text(html, lang, self.resolve_name, self.render_embed)

    def texts(self, doc):
        ru = self.text(((doc.get('system') or {}).get('description') or {}).get('value'), 'ru')
        twin = self.english_twin(doc)
        en = self.text(((twin.get('system') or {}).get('description') or {}).get('value'), 'en') if twin else ''
        return {'en': en, 'ru': ru}

    @staticmethod
    def book(doc):
        book = ((doc.get('system') or {}).get('source') or {}).get('book') or ''
        return BOOK_ALIASES.get(book, book)

    # --- advancement ---------------------------------------------------------------------------

    def item_ref(self, uuid):
        """Catalog id for a referenced document, or ('spell', id) / ('equipment', id), or None."""
        if uuid in self.docs:
            # Supplements keep their new spells in the options packs, next to the features.
            if self.docs[uuid]['type'] == 'spell':
                return 'spell', uuid
            return 'item', self.docs[uuid]['_id']
        last = uuid.split('.')[-1]
        if uuid in self.spell_index:
            return 'spell', uuid
        if uuid in self.equipment_index:
            return 'equipment', uuid
        if last in self.docs_by_id:
            return 'item', last
        self.warnings['unknown advancement ref'] += 1
        return None

    def advancement_title(self, owner, advancement):
        title = {'ru': (advancement.get('title') or '').strip(), 'en': ''}
        twin = self.english_twin(owner)
        if twin:
            for other in advancements(twin):
                if other.get('_id') == advancement.get('_id'):
                    title['en'] = (other.get('title') or '').strip()
        return title

    def advancement(self, owner, adv):
        conf = adv.get('configuration') or {}
        kind = adv['type']
        step = {'id': adv.get('_id'), 'type': kind}
        if adv.get('level') is not None:
            step['level'] = adv['level']
        title = self.advancement_title(owner, adv)
        if title['ru'] or title['en']:
            step['title'] = title
        if adv.get('classRestriction'):
            step['classRestriction'] = adv['classRestriction']
        hint = (adv.get('hint') or '').strip()
        if hint:
            step['hint'] = {'ru': hint, 'en': ''}

        if kind == 'Trait':
            step['mode'] = conf.get('mode') or 'default'
            step['grants'] = list(conf.get('grants') or [])
            step['choices'] = [{'count': c.get('count') or 1, 'pool': list(c.get('pool') or [])} for c in conf.get('choices') or []]
            if conf.get('allowReplacements'):
                step['allowReplacements'] = True
        elif kind in ('ItemGrant', 'ItemChoice'):
            items, spells, equipment = [], [], []
            source = conf.get('items') if kind == 'ItemGrant' else conf.get('pool')
            for entry in source or []:
                ref = self.item_ref(ref_uuid(entry))
                if ref is None:
                    continue
                {'item': items, 'spell': spells, 'equipment': equipment}[ref[0]].append(ref[1])
            step['items'] = items
            if spells:
                step['spells'] = spells
            if equipment:
                step['equipment'] = equipment
            if conf.get('optional'):
                step['optional'] = True
            spell = conf.get('spell') or {}
            if spell and (spell.get('ability') or spell.get('preparation') or spell.get('method') or spell.get('uses')):
                step['spell'] = {k: spell[k] for k in ('ability', 'preparation', 'method', 'prepared', 'uses') if spell.get(k)}
            if kind == 'ItemChoice':
                choices = conf.get('choices') or {}
                step['counts'] = {k: v['count'] for k, v in choices.items() if isinstance(v, dict) and v.get('count')}
                replacements = sorted(int(k) for k, v in choices.items() if isinstance(v, dict) and v.get('replacement'))
                if replacements:
                    step['replacementLevels'] = replacements
                step['itemType'] = conf.get('type') or ''
                restriction = conf.get('restriction') or {}
                cleaned = {k: restriction[k] for k in ('type', 'subtype', 'list', 'level') if restriction.get(k)}
                if cleaned:
                    step['restriction'] = cleaned
        elif kind == 'ScaleValue':
            step['identifier'] = conf.get('identifier') or ''
            step['scaleType'] = conf.get('type') or 'string'
            values = {}
            for level, value in (conf.get('scale') or {}).items():
                if not isinstance(value, dict):
                    values[level] = value
                elif value.get('faces'):
                    values[level] = f"{value.get('number') or ''}d{value['faces']}"
                elif 'value' in value:
                    values[level] = value['value']
            step['values'] = values
            units = (conf.get('distance') or {}).get('units')
            if units:
                step['units'] = units
        elif kind == 'AbilityScoreImprovement':
            step['points'] = conf.get('points') or 0
            if conf.get('cap') is not None:
                step['cap'] = conf.get('cap')
            fixed = {k: v for k, v in (conf.get('fixed') or {}).items() if v}
            if fixed:
                step['fixed'] = fixed
            if conf.get('locked'):
                step['locked'] = list(conf['locked'])
            if conf.get('max') is not None:
                step['max'] = conf['max']
            recommendation = conf.get('recommendation')
            if recommendation:
                ref = self.item_ref(recommendation)
                if ref and ref[0] == 'item':
                    step['recommendation'] = ref[1]
        elif kind == 'Size':
            step['sizes'] = list(conf.get('sizes') or [])
        elif kind in ('HitPoints', 'Subclass'):
            pass
        else:
            self.warnings[f'advancement {kind} kept without details'] += 1
        return step

    def advancement_list(self, doc):
        return [self.advancement(doc, adv) for adv in advancements(doc)]


def uses_of(doc):
    uses = (doc.get('system') or {}).get('uses') or {}
    maximum = str(uses.get('max') or '').strip()
    recovery = [
        {k: r[k] for k in ('period', 'type', 'formula') if r.get(k)}
        for r in uses.get('recovery') or [] if r.get('period')
    ]
    if not maximum and not recovery:
        return None
    return {'max': maximum, 'recovery': recovery}


def activation_of(doc):
    for activity in ((doc.get('system') or {}).get('activities') or {}).values():
        activation = (activity or {}).get('activation') or {}
        if activation.get('type'):
            return activation['type']
    return None


def build(catalog):
    classes, subclasses, species, backgrounds, features = [], [], [], [], []
    owners = collections.defaultdict(list)  # item id -> [{owner, level, via}]

    def note_owner(owner, step):
        for item_id in step.get('items') or []:
            levels = [int(k) for k in (step.get('counts') or {}).keys()] if step['type'] == 'ItemChoice' else []
            level = step.get('level') or (min(levels) if levels else None)
            owners[item_id].append({'owner': owner['_id'], 'level': level,
                                    'via': 'grant' if step['type'] == 'ItemGrant' else 'choice'})

    def base(doc):
        entry = {'id': doc['_id'], 'name': catalog.names(doc), 'book': catalog.book(doc)}
        identifier = (doc.get('system') or {}).get('identifier')
        if identifier:
            entry['identifier'] = identifier
        return entry

    for pack in catalog.packs.values():
        for doc in pack['documents']:
            system = doc.get('system') or {}
            kind = doc['type']
            if kind == 'spell':
                continue
            steps = catalog.advancement_list(doc)
            for step in steps:
                note_owner(doc, step)
            if kind == 'class':
                entry = base(doc)
                entry['hitDie'] = int(((system.get('hd') or {}).get('denomination') or 'd8').lstrip('d') or 8)
                primary = system.get('primaryAbility') or {}
                entry['primaryAbilities'] = list(primary.get('value') or [])
                entry['primaryAbilitiesAll'] = bool(primary.get('all'))
                spellcasting = system.get('spellcasting') or {}
                if spellcasting.get('progression') and spellcasting['progression'] != 'none':
                    entry['spellcasting'] = {'progression': spellcasting['progression'], 'ability': spellcasting.get('ability') or ''}
                entry['advancement'] = steps
                classes.append(entry)
            elif kind == 'subclass':
                entry = base(doc)
                entry['classIdentifier'] = system.get('classIdentifier') or ''
                spellcasting = system.get('spellcasting') or {}
                if spellcasting.get('progression') and spellcasting['progression'] != 'none':
                    entry['spellcasting'] = {'progression': spellcasting['progression'], 'ability': spellcasting.get('ability') or ''}
                entry['text'] = catalog.texts(doc)
                entry['advancement'] = steps
                subclasses.append(entry)
            elif kind == 'race':
                entry = base(doc)
                entry['text'] = catalog.texts(doc)
                movement = {k: v for k, v in (system.get('movement') or {}).items() if v and k not in ('units', 'hover', 'ignoredDifficultTerrain', 'special')}
                if movement:
                    entry['movement'] = movement
                senses = {k: v for k, v in (system.get('senses') or {}).items() if v and k not in ('units', 'special')}
                if senses:
                    entry['senses'] = senses
                creature = system.get('type') or {}
                if creature.get('value'):
                    entry['creatureType'] = creature['value']
                entry['advancement'] = steps
                species.append(entry)
            elif kind == 'background':
                entry = base(doc)
                entry['text'] = catalog.texts(doc)
                entry['advancement'] = steps
                backgrounds.append(entry)
            else:
                entry = base(doc)
                entry['type'] = kind
                feature_type = system.get('type') or {}
                if feature_type.get('value'):
                    entry['featureType'] = feature_type['value']
                if feature_type.get('subtype'):
                    entry['subtype'] = feature_type['subtype']
                entry['text'] = catalog.texts(doc)
                requirements = (system.get('requirements') or '').strip()
                if requirements:
                    entry['requirements'] = requirements
                prerequisites = system.get('prerequisites') or {}
                if prerequisites.get('level'):
                    entry['prerequisiteLevel'] = prerequisites['level']
                if prerequisites.get('repeatable'):
                    entry['repeatable'] = True
                uses = uses_of(doc)
                if uses:
                    entry['uses'] = uses
                activation = activation_of(doc)
                if activation:
                    entry['activation'] = activation
                if steps:
                    entry['advancement'] = steps
                features.append(entry)

    by_id = {c['id']: ('class', c) for c in classes}
    by_id.update({s['id']: ('subclass', s) for s in subclasses})
    by_id.update({s['id']: ('species', s) for s in species})
    by_id.update({b['id']: ('background', b) for b in backgrounds})
    by_id.update({f['id']: ('feature', f) for f in features})

    # Where each feature comes from: the class, subclass, species, background or feat granting it.
    for feature in features:
        grants = []
        for entry in owners.get(feature['id'], []):
            if entry['owner'] in by_id:
                grants.append(entry)
        if grants:
            feature['grantedBy'] = sorted(grants, key=lambda g: (g['level'] or 0, g['owner']))
            levels = [g['level'] for g in grants if g['level']]
            if levels:
                feature['level'] = min(levels)
        feature['kind'] = feature_kind(feature, by_id)
        if 'level' not in feature:
            parsed = re.search(r'\s(\d+)(?:\s|$|\()', ' ' + feature.get('requirements', '') + ' ')
            if feature.get('prerequisiteLevel'):
                feature['level'] = feature['prerequisiteLevel']
            elif parsed and feature['kind'] in ('classFeature', 'subclassFeature'):
                feature['level'] = int(parsed.group(1))

    # Spells and equipment the advancements point at: short ids (the Foundry _id, shared by the
    # Russian and the English pack) with both names.
    def by_short_id(index, prefix):
        return {uuid.split('.')[-1]: entry for uuid, entry in index.items() if uuid.startswith(prefix)}

    ru_spells = by_short_id(catalog.spell_index, 'Compendium.ag-fifthpendium')
    en_spells = by_short_id(catalog.spell_index, 'Compendium.dnd5e.')
    ru_equipment = by_short_id(catalog.equipment_index, 'Compendium.ag-fifthpendium')
    en_equipment = by_short_id(catalog.equipment_index, 'Compendium.dnd5e.')
    spells = {}
    equipment = {}
    for entry in list(classes) + subclasses + species + backgrounds + features:
        for step in entry.get('advancement') or []:
            short_spells = []
            for uuid in step.get('spells') or []:
                short = uuid.split('.')[-1]
                ru_entry, en_entry = ru_spells.get(short), en_spells.get(short)
                if ru_entry is None and uuid in catalog.docs:
                    doc = catalog.docs[uuid]
                    ru_entry = {'name': doc['name'], 'level': (doc.get('system') or {}).get('level')}
                ru, en = split_name(ru_entry['name']) if ru_entry else ('', '')
                if en_entry:
                    en = en_entry['name']
                spells[short] = {'name': {'en': en, 'ru': ru}, 'level': (ru_entry or en_entry).get('level')}
                short_spells.append(short)
            if short_spells:
                step['spells'] = short_spells
            short_equipment = []
            for uuid in step.get('equipment') or []:
                short = uuid.split('.')[-1]
                ru_entry, en_entry = ru_equipment.get(short), en_equipment.get(short)
                ru, en = split_name(ru_entry['name']) if ru_entry else ('', '')
                if en_entry:
                    en = en_entry['name']
                equipment[short] = {'name': {'en': en, 'ru': ru}, 'type': (ru_entry or en_entry).get('type')}
                short_equipment.append(short)
            if short_equipment:
                step['equipment'] = short_equipment
    return {
        'classes': classes, 'subclasses': subclasses, 'species': species, 'backgrounds': backgrounds,
        'features': features, 'spells': spells, 'equipment': equipment,
    }


def feature_kind(feature, by_id):
    owner_kinds = {by_id[g['owner']][0] for g in feature.get('grantedBy', [])}
    via_choice = all(g['via'] == 'choice' for g in feature.get('grantedBy', [])) if feature.get('grantedBy') else False
    feature_type = feature.get('featureType')
    if feature['type'] != 'feat':
        return 'item'
    if feature_type == 'feat':
        return 'feat'
    if feature_type == 'race' or 'species' in owner_kinds:
        return 'speciesTrait'
    if 'background' in owner_kinds:
        return 'backgroundFeature'
    if feature_type == 'class' or owner_kinds & {'class', 'subclass'}:
        if via_choice or (not owner_kinds and feature.get('subtype')):
            return 'classOption'
        if 'subclass' in owner_kinds and 'class' not in owner_kinds:
            return 'subclassFeature'
        return 'classFeature'
    if 'feature' in owner_kinds:
        return 'featureOption'
    return 'other'


# --- English texts and ids from the catalog the app used before --------------------------------------

def legacy_entries():
    """The old catalog's entries: (old id, English name, owner name, English description)."""
    entries = []
    for name, prefix, owner_key in (('5e-SRD-Features.json', 'CLASS', 'class'), ('5e-SRD-Traits.json', 'RACE', 'species'),
                                    ('5e-SRD-Feats.json', 'FEAT', None)):
        for item in load_json(os.path.join(SRD_2024, name), []):
            if prefix == 'FEAT' and (item.get('type') or '').lower() != 'origin':
                continue
            owner = ''
            if owner_key == 'class':
                owner = ((item.get('subclass') or {}).get('name')) or ((item.get('class') or {}).get('name')) or ''
            elif owner_key == 'species':
                owner = ', '.join(s.get('name', '') for s in item.get('species') or [])
            description = item.get('description') or '\n'.join(item.get('desc') or [])
            entries.append((f'{prefix}:{item["index"]}', item['name'], owner, description))
    for item in load_json(os.path.join(LEGACY, 'feature_catalog_extra.json'), []):
        entries.append((item['id'], item['name'], item.get('category', ''), item.get('description', '')))
    return entries


def attach_legacy(result, catalog):
    names = {}
    for kind in ('classes', 'subclasses', 'species', 'backgrounds'):
        for entry in result[kind]:
            names[entry['id']] = entry['name']
    candidates = collections.defaultdict(list)
    for feature in result['features']:
        for label in (feature['name']['en'], feature['name']['ru']):
            if label:
                candidates[normalize(label)].append(feature)
    legacy_ids = {}
    unmatched = []
    for old_id, name, owner, description in legacy_entries():
        found = candidates.get(normalize(name), [])
        if not found:
            # "Darkvision (60 ft.)", "Breath Weapon: Acid": the old catalog split some traits by option.
            base_name = re.split(r'\s*[(:]', name)[0]
            found = candidates.get(normalize(base_name), [])
        if len(found) > 1 and owner:
            owner_names = {normalize(part) for part in owner.split(', ')}
            narrowed = [f for f in found if any(normalize(names.get(g['owner'], {}).get('en', '')) in owner_names
                                                for g in f.get('grantedBy', []))]
            if not narrowed:
                # Fifthpendium subclass names sometimes differ ("Path of the Berserker" vs "Berserker").
                narrowed = [f for f in found if any(any(n and (n in normalize(names.get(g['owner'], {}).get('en', ''))
                                                                or normalize(names.get(g['owner'], {}).get('en', '')) in n)
                                                            for n in owner_names)
                                                        for g in f.get('grantedBy', []))]
            found = narrowed or found[:1]
        if not found:
            unmatched.append((old_id, name, owner))
            continue
        feature = found[0]
        legacy_ids[old_id] = feature['id']
        if not feature['text']['en'] and description:
            feature['text']['en'] = description
        if not feature['name']['en']:
            feature['name']['en'] = name
    result['legacyIds'] = legacy_ids
    return unmatched


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', default=DEFAULT_OUT)
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding='utf-8')

    catalog = Catalog()
    result = build(catalog)
    unmatched = attach_legacy(result, catalog)
    books = sorted({e['book'] for kind in ('classes', 'subclasses', 'species', 'backgrounds', 'features') for e in result[kind] if e['book']})
    output = {
        'version': 1,
        'sources': {pid: pack.get('packageVersion') for pid, pack in {**catalog.packs, **catalog.srd_packs}.items()},
        'books': {b: {'en': BOOKS.get(b, (b, b))[0], 'ru': BOOKS.get(b, (b, b))[1]} for b in books},
        'subtypes': {k: {'en': v[0], 'ru': v[1]} for k, v in SUBTYPES.items()},
        **result,
    }
    with open(args.out, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(output, f, ensure_ascii=False, separators=(',', ':'), sort_keys=False)

    kinds = collections.Counter(f['kind'] for f in result['features'])
    print(f"classes {len(result['classes'])}, subclasses {len(result['subclasses'])}, species {len(result['species'])}, "
          f"backgrounds {len(result['backgrounds'])}, features {len(result['features'])} {dict(kinds)}")
    english = sum(1 for f in result['features'] if f['text']['en'])
    print(f"features with English text: {english}/{len(result['features'])}; legacy ids mapped: {len(result['legacyIds'])}, unmatched: {len(unmatched)}")
    for entry in unmatched[:40]:
        print('   unmatched legacy', entry)
    for warning, count in catalog.warnings.most_common():
        print(f'   warning: {warning} x{count}')
    print('books:', books)
    print('wrote', args.out, os.path.getsize(args.out), 'bytes')


if __name__ == '__main__':
    main()
