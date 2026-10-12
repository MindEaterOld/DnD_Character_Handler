# -*- coding: utf-8 -*-
"""A test character that lights every accent colour (owner's wish, 2026-10-11), as an archive to import:
python tools/test_characters/make_accent_character.py  ->  tools/test_characters/Accent_test.dndchar

Where each accent shows (the six of 2026-10-11):
  bright           the portrait's star (inspiration on); a combat spell's material cost (Chromatic Orb)
  bright           the header's experience rule at the last level (20)
  magic            the temporary hit points (+15) and their bar; the ability modifiers; invisible's chip;
                   the spells' level tags in combat; the long rest's temporary hit points
  heal             the hit points' bar; advantage arrows (invisible: attacks); Cure Wounds; the short rest's average
  danger           a hindering condition's chip (charmed); an empty resource; the trash in a pop-up
  fire             fire damage (Fire Bolt, the flaming sword); exhaustion's chip, if you put it on (no exhaustion here:
                   its -2 would turn every ability modifier red and hide their blue)
  magic            cold damage (Ray of Frost, the frost spear)
  arcane           lightning damage (Lightning Bolt, the storm staff)
  heal             poison damage (Poison Spray, the poisoned dagger)
  text.muted       any other damage (the longsword's slashing, Chromatic Orb's acid)
  magic            magic items' names in the inventory and the combat's weapon pick
  fire / text.muted / bright  the inventory's copper, silver and gold coins
"""
import json
import os
import time
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
now = int(time.time() * 1000)


def attack(name, damage_type, die, ability='STRENGTH', magical=0):
    return {'name': name, 'isProficient': True, 'calculationMode': 'AUTOMATIC', 'ability': ability,
            'damageDiceCount': 1, 'damageDieType': die, 'magicalBonus': magical, 'applyAbilityModifierToDamage': True,
            'attackBonusOrSaveDc': '', 'damage': '', 'damageType': damage_type}


def weapon(name, damage_type, dice, magical, equipped=True, base=None):
    details = {'weaponClass': 'MARTIAL', 'rangeType': 'MELEE',
               'damages': [{'dice': dice, 'damageType': damage_type}], 'properties': []}
    if base:
        details['baseWeaponId'] = base
    return {'name': name, 'description': '', 'isMagical': magical, 'magicalBonus': 1 if magical else 0,
            'category': 'WEAPON', 'weight': 3.0, 'quantity': 1, 'isEquipped': equipped, 'weaponDetails': details}


def item(name, category, magical, quantity=1, weight=1.0):
    return {'name': name, 'description': '', 'isMagical': magical, 'magicalBonus': 1 if magical else 0,
            'category': category, 'weight': weight, 'quantity': quantity, 'isEquipped': False}


def spell(catalog_id, name, level, school, **kw):
    s = {'catalogId': catalog_id, 'name': name, 'level': level, 'school': school, 'isPrepared': True,
         'isAlwaysPrepared': False, 'description': '', 'higherLevelDescription': '', 'range': '60 feet',
         'castingTime': '1 action', 'duration': 'Instantaneous', 'components': 'V, S',
         'material': '', 'materialCost': '', 'isRitual': False, 'requiresConcentration': False, 'attackType': '',
         'availableClasses': 'wizard', 'damageType': '', 'damageBase': '', 'damageBonusValue': 0,
         'damageBonusIsModifier': False, 'altDamageBase': '', 'altDamageType': '', 'altDamageBonusValue': 0,
         'altDamageBonusIsModifier': False, 'damage': '', 'saveAbility': '', 'saveEffect': '', 'areaOfEffect': '',
         'healBase': '', 'healBonusValue': 0, 'healBonusIsModifier': False, 'healing': ''}
    s.update(kw)
    return s


spells = [
    spell('phbsplFireBolt00', 'Fire Bolt', 0, 'evo', range='120 feet', attackType='ranged', damageBase='1d10',
          damageType='Fire', damage='1: 1d10\n5: 2d10\n11: 3d10\n17: 4d10'),
    spell('phbsplRayofFrost', 'Ray of Frost', 0, 'evo', attackType='ranged', damageBase='1d8', damageType='Cold',
          damage='1: 1d8\n5: 2d8\n11: 3d8\n17: 4d8'),
    spell('phbsplPoisonSpra', 'Poison Spray', 0, 'nec', range='30 feet', attackType='ranged', damageBase='1d12',
          damageType='Poison', damage='1: 1d12\n5: 2d12\n11: 3d12\n17: 4d12'),
    spell('phbsplChromaticO', 'Chromatic Orb', 1, 'evo', range='90 feet', components='V, S, M', materialCost='50',
          attackType='ranged', damageBase='3d8', damageType='Acid'),
    spell('phbsplCureWounds', 'Cure Wounds', 1, 'abj', range='Touch', healBase='2d8', healBonusIsModifier=True,
          healing='1: 2d8 + MOD'),
    spell('phbsplLightningB', 'Lightning Bolt', 3, 'evo', range='Self', components='V, S, M', saveAbility='DEX',
          saveEffect='half', damageBase='8d6', damageType='Lightning', areaOfEffect='line, 100 ft'),
]

character = {
    'id': 0, 'gameSystem': 'dnd5e_2024', 'name': 'Тест акцентов', 'race': 'Эльф', 'characterClass': 'Волшебник',
    'subclass': '', 'level': 20, 'currentHp': 97, 'maxHp': 164, 'temporaryHp': 15,
    'deathSaveSuccesses': 0, 'deathSaveFailures': 0, 'conditions': 'invisible,charmed', 'exhaustion': 0,
    'hitDieSides': 6, 'spentHitDice': 8, 'hasInspiration': True,
    'armorClass': 15, 'baseArmorClass': 12, 'armorClassMode': 'AUTOMATIC',
    'copperPieces': 37, 'silverPieces': 120, 'goldPieces': 2450,
    'speed': 30, 'initiative': 0, 'initiativeBonus': 0, 'spellcastingAbility': 'INTELLIGENCE',
    'spellSlotMaximums': '4,3,3,3,3,2,2,1,1', 'spellSlotRemaining': '2,3,1,3,3,0,2,1,0',
    'spellSlotsRestoreOnShortRest': False, 'spellSlotsRestoreOnLongRest': True,
    'experience': 355000,
    'strength': 8, 'dexterity': 16, 'constitution': 14, 'intelligence': 20, 'wisdom': 12, 'charisma': 10,
    'strengthSaveProficient': False, 'dexteritySaveProficient': False, 'constitutionSaveProficient': False,
    'intelligenceSaveProficient': True, 'wisdomSaveProficient': True, 'charismaSaveProficient': False,
    'passivePerceptionBonus': 0, 'darkvisionMode': 'AUTO', 'darkvisionManualFeet': 0,
    'armorProficiencies': '', 'weaponProficiencies': '', 'toolProficiencies': '', 'languageProficiencies': '',
    'weaponMasteries': '', 'defenses': '|'.join(sorted(['dr:fire', 'dr:cold', 'dr:lightning', 'dr:acid', 'dr:thunder', 'di:poison', 'di:force', 'ci:poisoned', 'ci:exhaustion', 'dv:necrotic', 'dv:psychic', 'dv:radiant', 'dv:bludgeoning'])), 'alignment': 'Chaotic Good', 'background': '', 'faith': '', 'homeland': '',
    'age': '', 'gender': '', 'height': '', 'weight': '', 'eyes': '', 'hair': '', 'skin': '', 'size': 'MEDIUM',
    'personalityTraits': '', 'ideals': '', 'bonds': '', 'flaws': '',
    'biography': 'Тестовый персонаж: на его экранах видны все акцентные цвета.',
    'createdAt': now, 'updatedAt': now, 'classes': [], 'advancements': [],
}

manifest = {
    'schemaVersion': 27, 'gameSystem': 'dnd5e_2024', 'exportedAt': now, 'character': character,
    'skills': [{'name': 'skill_' + n, 'isProficient': n in ('arcana', 'history', 'investigation'),
                'isExpertise': n == 'arcana', 'hasJackOfAllTrades': False}
               for n in ('acrobatics', 'animal_handling', 'arcana', 'athletics', 'deception', 'history', 'insight',
                         'intimidation', 'investigation', 'medicine', 'nature', 'perception', 'performance',
                         'persuasion', 'religion', 'sleight_of_hand', 'stealth', 'survival')],
    'attacks': [
        attack('Пылающий меч', 'Fire', 'd8', magical=1),
        attack('Морозное копьё', 'Cold', 'd6'),
        attack('Посох бури', 'Lightning', 'd6', ability='INTELLIGENCE'),
        attack('Отравленный кинжал', 'Poison', 'd4', ability='DEXTERITY'),
        attack('Кислотный плевок', 'Acid', 'd6', ability='CONSTITUTION'),
        attack('Громовой молот', 'Thunder', 'd8'),
        attack('Волшебный клинок', 'Force', 'd6', ability='INTELLIGENCE'),
        attack('Коса жнеца', 'Necrotic', 'd10'),
        attack('Шёпот разума', 'Psychic', 'd6', ability='CHARISMA'),
        attack('Солнечное копьё', 'Radiant', 'd8', ability='WISDOM'),
        attack('Длинный меч', 'Slashing', 'd8'),
    ],
    'combatResources': [
        {'name': 'Магическое восстановление', 'currentUses': 0, 'maximumUses': 1,
         'restoresOnShortRest': False, 'restoresOnLongRest': True},
        {'name': 'Знаки силы', 'currentUses': 2, 'maximumUses': 3,
         'restoresOnShortRest': True, 'restoresOnLongRest': True},
    ],
    'inventoryItems': [
        weapon('Пылающий меч', 'Fire', '1d8', True),
        weapon('Длинный меч', 'Slashing', '1d8', False, equipped=False, base='longsword'),
        item('Плащ защиты', 'OTHER', True),
        item('Зелье лечения', 'CONSUMABLE', False, quantity=3, weight=0.5),
        item('Верёвка', 'OTHER', False, weight=5.0),
    ],
    'spells': spells,
    'spellAttacks': spells,
    'features': [],
    'notes': [{'title': 'Где какие акценты', 'createdDate': now, 'updatedDate': now, 'isPinned': True,
               'content': 'Обзор: вдохновение, опыт на 20 уровне, временные хиты, состояния, истощение, модификаторы.\n'
                          'Бой: типы урона у атак и заклинаний, преимущество от невидимости, стоимость компонента.\n'
                          'Снаряжение: монеты, магические предметы.\n'
                          'Отдых: короткий (лечение костями) и длинный (что пройдёт).'}],
}

out = os.path.join(HERE, 'Accent_test.dndchar')
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('manifest.json', json.dumps(manifest, ensure_ascii=False, indent=2))
print(out)
