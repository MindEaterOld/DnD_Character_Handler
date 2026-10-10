# Каталог общих компонентов и скелеты

Все компоненты — `app/src/main/java/com/dndcharacterhandler/presentation/components/`. Колонка «файлов» — в скольких файлах компонент реально используется (замер 2026-10-03). Перед созданием нового элемента искать здесь; отличие в размере, цвете, тексте или иконке закрывается параметром.

## Каркас экрана

| Компонент | Файлов | Роль |
|---|---|---|
| `ScreenBackground { }` | 9 | Фон темы на всё окно (радиальный градиент или иллюстрация темы — `LocalThemeBackdrop`); экран рисует только свою часть, фон бесшовно продолжается под нижней панелью и системными полосами |
| `CharacterScreenHeader(character, onOpenDrawer, onOpenDice)` | 7 | Шапка экрана персонажа: меню, кубы, имя и подзаголовок |
| `PinnedCharacterHeader(…, backdrop)`, `rememberHeaderBackdrop(listState)`, `Modifier.headerBackdrop(backdrop)`, `CharacterHeaderInset` | 6 | **Одна шапка на все экраны персонажа** (обзор тоже, владелец 2026-10-09; копий под экран не делать): `val backdrop = rememberHeaderBackdrop(listState)`, у `LazyColumn` — `state = listState`, `.headerBackdrop(backdrop)` и `contentPadding(top = CharacterHeaderInset + свой отступ)` (статус-бар + 56dp: экраны рисуются под статус-баром, `DndCharacterApp` верхнего отступа не даёт), шапка — после списка в том же `Box`: `PinnedCharacterHeader(character, onOpenDrawer, onOpenDice, backdrop)`; на обзоре по имени — `PinnedCharacterHeader(name, …, backdrop, onNameClick)`. Пока персонаж грузится — `CharacterLoadingScreen(onOpenDrawer, onOpenDice)` (та же шапка без имени, «Загрузка…» посередине), своих заглушек не делать. Под шапкой, куда уехал список, тап не проходит к спрятанной карточке. Слои размытия пишутся только на Android 12+ (ниже размытия нет — только тон). Внизу экраны уходят под панель вкладок: конец списка — `LocalFloatingButtonsInset`, «+» — `padding(bottom = FloatingAddButtonBottom)` |
| `ScreenTopActions(onOpenDrawer, onOpenDice)` | 9 | Верхний ряд: «гамбургер» слева, d20 справа (кнопки 44dp, иконки 28) |
| `ScreenTopActionButton(onClick) { Icon }` | 2 | Голая иконка верхней панели без заливки, тап-зона 44dp — единственная кнопка без подложки (меню, кубы, отдых столбиком под кубами на обзоре) |
| `PortraitSideButton(icon, contentDescription, onClick, add, on)` | 4 | Кнопка у портрета (отдыхи, вдохновение, «+XP»): голая белая иконка, как меню и куб в шапке, — 28dp в зоне касания 44dp, мягкая тень `ornament.dropShadow` вокруг для яркого арта, без монеты (владелец, 2026-10-10: B). Колонки стоят на вертикали меню и куба, шаг `PortraitSideStep` 52dp от иконки шапки и между кнопками. `on` — включённое вдохновение золотом `accent.inspiration`, `add` — «+» перед словом |
| `BottomNavigationBar(…, backdrop, backdropOrigin)` | — | Панель вкладок: плашка с обводкой, сквозь неё размыто видны экраны (слой, который пишет `DndCharacterApp`; блюр 24dp поверх `surface.card` и тон `surface.card` 70 %, как у шапки обзора); при открытой клавиатуре скрыта. В «Классике» плашка — пилюля (`TabBarHeight` 62, `RoundedCornerShape(50)`), вкладки — круги `TabSize` 46 в `Row(padding(TabInset 8), SpaceBetween)`: центры крайних совпадают с центрами торцов, выбранная — золото 15 %, иконка 28 золотом, остальные 26 `text.subtle` |
| `FadingLazyColumn(…)`, `Modifier.fadingVerticalScroll()`, `Modifier.fadingEdges(state)` | 15 | Мягкие края прокрутки в поп-апах и шторках: уходящее за край растворяется на 24dp, край — только пока туда есть что листать |
| `EditSheet(title, onDismiss) { … }` | 1 | Шторка снизу: ручка, заголовок `titleLarge` засечками, содержимое прокручивается, без кнопок и крестика (отметки применяются сразу), не выше 75 % экрана; в «Гравюре» гравированная панель без нижней кромки. Шторка — своё окно, и Material красит значки статус-бара по теме телефона (светлая — чёрные значки): `EditSheet` сам держит их светлыми, через `(LocalView.current.parent as DialogWindowProvider).window` — свою `ModalBottomSheet` в обход него не заводить |
| `ShortRestDialog(…)`, `LongRestDialog(…)`, `RestHitPoints(current, max, gain, exact)`, `HitDicePools(…)`, `RestRows(title, rows)` | 2 | Короткий отдых (`overview/ShortRestDialog.kt`): хиты с полосой и прибавкой зелёным, кости хитов кубиками в скине игрока (выбрана — золото, доступна — заливка кнопки, потрачена — контур и 30 %), ряд на размер кости при мультиклассе, список «Восстановится»; `RestHitPoints`, `HitDicePools` (на долгом — потраченные зелёным, «вернутся») и `RestRows` (строка — иконка, имя, «было → станет», свой цвет иконки) — общие для окон отдыха |
| `FloatingAddButton(onClick)` | 5 | Круглая плавающая «+» 58dp в правом нижнем углу (`padding(end = 24.dp, bottom = FloatingAddButtonBottom)` — над панелью вкладок) |
| `LocalFloatingButtonsInset` | — | Нижнее поле списка, чтобы последние элементы выезжали из-под плавающих кнопок; приложение задаёт на экран (`SingleFloatingButtonInset` 110, `NoFloatingButtonInset` 16) |
| `OverlayCloseButton(onClick)` | 5 | Круглый крестик полноэкранного оверлея (портрет, стол кубов), сам держится вне системных полос |

## Поп-апы

| Компонент | Файлов | Роль |
|---|---|---|
| `EditDialog(title, onDismiss, onConfirm, …)` | 14 | **Единственный поп-ап приложения.** Заголовок с крестиком в углу (закрыть без сохранения, кнопки «Отмена» нет), содержимое прокручивается целиком, внизу один ряд: «Удалить» — круглая тональная (красный 15 % за иконкой) слева, главное действие («Сохранить» по умолчанию, заливка `primary`) справа. `confirmIsDanger` — главное действие красным (подтверждение удаления). Без `onConfirm` и `onDelete` нижнего ряда нет — тап по пункту делает работу. `titleActions` — кнопки перед крестиком, `titleLeading` — перед заголовком (стрелка назад) |
| `DeleteCharacterDialog` | — | Подтверждение удаления персонажа |
| `SettingsDialog` | — | Настройки (язык), изменения сразу |
| `WeaponMasteryDialog` | — | Правила свойства мастерства оружия |

Черновик правки в поп-апе при смерти процесса теряется намеренно — `rememberSaveable` в поп-апы не добавлять.

## Карточки и статы

| Компонент | Файлов | Роль |
|---|---|---|
| `BorderLabelCard(label, …)` | 3 | Рамка с подписью в разрыве верхней обводки (как у outlined-поля); основа карточек статов, характеристик и HP. `frameOverContent = true` — рамка поверх содержимого, для картинки во всю карточку (арты характеристик); `notchMarks = true` — ромбики цвета рамки на концах разрыва под подпись (разрыв шире, 8dp); `labelModifier` — на текст подписи (арты рисуют её трижды ради глубокой тени) |
| `SheetLabel(label)`, `SheetOrnament(airAbove, airBelow)` (`components/SheetParts.kt`) | 4 | Подпись поля листа золотыми разреженными капителями; линия с ромбиком между частями панели |
| `OutlinedPanel { }` | 1 | Рамка без подписи вокруг столбца строк — «лист» владений и защит на обзоре: обводка по поверхности карточки, в «Гравюре» гравированная рамка, рябь строки не выходит за углы. Поля внутри — `SheetPanel { SheetRow(…) }` в `attributes/AttributesScreen.kt`, оформлены как биография (владелец 2026-10-08): иконка 24 золотом `primary` по центру подписи и первой строки, подпись `BiographyLabel` (золотые прописные с разрядкой), под ней значения через запятую `bodyLarge`, «Нет» тихим `text.subtle`; поля разделяет воздух, без линий |
| `MiniStatCard(label, value, icon?, onClick?)` | 6 | **Карточка стата**: `BorderLabelCard` со значением, шагает вниз по шкале на длинном значении. Единственная карточка этого вида — брать её везде, где экран показывает статы |
| `StatCardRow { }` | 6 | Ряд карточек статов наверху экрана, общий кегль значений (`AutoSizeGroup`) |
| `MiniStatCardIcon(imageVector)` | — | Обычная иконка перед значением стата |
| `MiniStatCardHeight` | — | Высота любой карточки стата вместе с подписью на рамке |
| `ExpandableCard(…)` | 2 | Карточка-аккордеон: шапка (слот `leading`, заголовок, подзаголовок, шеврон), тело и действия при раскрытии (особенности, заклинания, предметы) |
| `CardEditButton(onClick)` / `CardActionButton(label, icon, onClick)` | 3 / 1 | «Изменить» и соседние действия внизу раскрытой карточки — текстовые кнопки (исключение из правила подложки) |
| `LimitProgressBar(…)` | 2 | Подпись «текущее / максимум» и полоса под ней (вес инвентаря, подготовленные заклинания); `overLimit` красит в опасный цвет |
| `SizeToggle(…)` | 2 | Размер существа: три фигуры в рамке карточки стата (Character Wizard). `CreatureSize.figure` — фигура размера, она же его иконка (меняется с размером): гном, вампир в плаще, каменный голем из Game Icons (CC BY 3.0) |
| `SelectableDot(selected, …)` | 2 | Точка-переключатель: «надето», «подготовлено» |
| `RollMarker(effects)` / `RollMarker(worse, better)` + `changedValueColor(delta)` | 6 | Как состояния меняют бросок: две красные стрелки вниз — помеха, две зелёные вверх — преимущество, красный крестик — автопровал. Насколько сдвинулось число — его цвет: красный ниже, зелёный выше. Сочетаются. В `MiniStatCard` — `valueMarker` (стрелки встают на место иконки) и `valueColor` |
| `ConcentrationToggle(concentrating, onToggle, enabled)` | 1 | Концентрация у заклинания: круг 36dp у отметки «подготовлено», выкл. — `surface.button`, вкл. — золото `primary`; не `enabled`, пока персонаж выведен из строя |
| `EndConcentrationDialog(spellName, onEnd, onDismiss)` | 2 | «Прервать концентрацию?» — с экрана заклинаний и из столбика состояний |
| `Condition.icon` / `.nameKey` / `.accent()` (`ConditionVisuals.kt`) | 2 | Значок, ключ названия и цвет состояния |
| `StepButton(icon, contentDescription, onClick, enabled, size)` / `NumberStepperField(label, value, onValueChange, minValue)` | 5 | Степпер: круг заливки `surface.button`, иконка `Remove`/`Add` (половина круга) в `text.primary`, у края — `text.subtle`; 48dp в окнах хитов и состояний, 30dp в поле «− число +», 28dp в ресурсах боя |
| `GameSystemCard(system, onClick)` / `GameSystemSheet(selected, onPick, onDismiss)` / `GameSystemEmblem(family, size)` (`components/GameSystemPicker.kt`) | 1 | Игровая система в шторке персонажей (владелец, 2026-10-10): карточка-ячейка в золотой рамке с ромбами у подписи «Игровая система» — эмблема игры в двух кольцах на мягком золотом свечении, название игры `titleLarge`, редакция `bodyLarge` `text.muted`, значок «развернуть»; тап — шторка выбора: игры группами с эмблемой, редакции переключателями (выбранная — золото), у систем без листа «Скоро». Эмблема — никогда не логотип игры. `gameSystemName(system)` — «Pathfinder (2-я редакция)» |

## Текст, картинки, эффекты

| Компонент | Файлов | Роль |
|---|---|---|
| `AutoSizeText(…)` + `LocalAutoSizeGroup` | 1 | Текст, который шагает вниз **по шкале темы** (не произвольными sp), пока не влезет в `maxLines` без разрыва слов; группа даёт ряду общий кегль |
| `AppImage(imageRef, contentDescription, framing, fallback)` | 3 | Картинка по ссылке проекта (`res:drawable/…`, ассеты, файл, `content://`), декодирует вне главного потока, пока грузит — `fallback`; с `framing` (`PortraitFraming`) показывает выбранную часть портрета вместо центрального кропа |
| `rememberAppImagePainter(imageRef)` | 2 | Тот же `Painter`, что рисует `AppImage`, — для своего `Canvas` (окно кадрирования) |
| `portraitVignette(area, foot, opaque)`, `portraitHeroHeight(width, topInset)`, `portraitHeroFootShare(height)` (`overview/PortraitHero.kt`) | 2 | Виньетка портрета (бока, верх, низ растворяются в фон, маски DstIn в своём слое) и его размеры на этом телефоне: ими рисуют и портрет обзора, и окно «Кадрировать портрет» (`PortraitFramingDialog(…, heroAspect, footShare)`) — что в окне, то и на обзоре |
| `Modifier.saturation(s)` | 1 | Насыщенность содержимого: 1 как есть, 0 — чёрно-белое (портрет погибшего); работает на любом содержимом |
| `SkullIcon` | 1 | Череп спасбросков от смерти (`ImageVector`, тонируется `Icon`) |
| `PortraitHero(portraitUri, characterName, dead, framing, onClick, fallback, topInset) { поверх арта }` (`overview/PortraitHero.kt`) | 1 | Портрет обзора: арт без рамки во всю ширину экрана, низ растворяется в фоне; монеты и блок выживания кладутся в его слот. Рядом: `Modifier.bleed(margin)` — пункт колонки шириной до краёв экрана; `artShadow()` + `Modifier.deepShadow()` — глубокая тень под текстом на арте |
| `D20Outline` (`dice/D20Outline.kt`) | — | Контурный d20 кнопки кубов |

## Кубики (`presentation/dice/`)

- `DieIcon(type, look, showNumbers, mark, …)` — значок кубика в текущем скине (`LocalDiceSkin`), с меткой на передней грани вместо числа.
- `LocalDiceRoller` + `DiceRollRequest` — любой бросок из экрана идёт на 3D-стол; тихих случайных значений не бывает.

## Не использовать

`ReusableComponents.kt`: `StatCard`, `AttackCard`, `InfoRow`, `InfoCard`, `NoteCard`, `SpellRow`, `FeatureRow`, `InventoryItemRow`, `SectionDivider` — ноль применений, остатки каркаса. `CharacterHeader` и `PlaceholderSection` живут только в `PlaceholderScreen`.

## Скелет экрана персонажа

Эталон — `features/FeaturesScreen.kt`:

```
ScreenBackground
  Box(fillMaxSize)
    LazyColumn(contentPadding = 24 / 24 / top 4 / bottom LocalFloatingButtonsInset, spacedBy 10)
      item CharacterScreenHeader
      item StatCardRow { MiniStatCard ×3 (weight 1f) }
      item поиск / фильтр
      section title + items(ExpandableCard, key = id)
    FloatingAddButton(align BottomEnd, padding end 24, bottom FloatingAddButtonBottom)
поп-апы (EditDialog) — вне ScreenBackground, по флагам состояния
```

## Скелет поп-апа

```
EditDialog(title, onDismiss, onConfirm?, onDelete?)
  текст-состояние (bodyLarge, text.muted)       «Текущие HP: 0 / 13»
  ряд переключателей режима                     выбранный — отличается заливкой
  OutlinedTextField(label = режим)
  итог (bodyLarge, text.primary)                «Итог: 0 / 13»
  предупреждение (bodyLarge, accent.dangerHpZero), если действие опасно
```

Колонка содержимого `EditDialog` уже даёт `spacedBy(12.dp)` — свои отступы между рядами не добавлять.

Портрет обзора (владелец 2026-10-09, «арт до краёв» и «И»): `PortraitHero` в `overview/PortraitHero.kt` — арт без рамки во всю ширину экрана (пункт списка расширен `Modifier.bleed(24.dp)`), от самого верха телефона под статус-баром (`topInset`), 412 × 540 на эталоне плюс статус-бар, низ растворяется в фоне на 220dp альфа-маской (`CompositingStrategy.Offscreen` + `BlendMode.DstIn`). В его слот `OverviewScreen` кладёт: монеты (`PortraitSideButton`) на `CharacterHeaderInset + PortraitCoinsTop` (16dp) — слева короткий и долгий отдых, справа вдохновение и «+XP» (`label = text("overview_xp_coin")`, `add = true` — «+» перед словом внутри монеты, нарисован под словом и заходит под «X» на 1,5dp, без обводок); внизу `SurvivalBlock` — `ConditionChips` (`overview/Conditions.kt`), ряд `HpRoundButton` (урон) · `HpNumbers` + `HpBar` · `HpRoundButton` (лечение), под ним два `ArtStat` (класс брони, скорость) или при 0 хитов `DeathSavesRow`. Текст на арте читается тенью `artShadow()` + `Modifier.deepShadow()` (тень трижды), не плашкой. Бонус мастерства и пассивная внимательность — `SectionStat` под заголовком «Навыки» (`attributes/AttributesScreen.kt`). Удалены 2026-10-09: рамка `GothicPortraitFrame`, `StatsPanel`/`PanelStat`/`BackedStat` (от них остались `SheetLabel` и `SheetOrnament` в `components/SheetParts.kt`), щит `ArmorClassShield`, лоток спасбросков `DeathSavesTray`, колонка `ConditionsColumn` — в истории git.

Шапка (владелец 2026-10-09, одна на все экраны): `PinnedCharacterHeader` в `components/PinnedCharacterHeader.kt` — `CharacterScreenHeader(…, nameShadow)` под `statusBarsPadding()`, в покое без подложки, лежит на том, что под ней. Подложка проявляется с прокруткой (`HeaderBackdrop.progress` 0…1 за 56dp): список пишется в свой слой (`Modifier.headerBackdrop`: `layer.record { drawContent() }; drawLayer(layer)`), шапка рисует этот слой в `graphicsLayer { renderEffect = BlurEffect(24dp); clip = true }` поверх `surface.card` и тонирует `surface.card` 70 %; линия `ornament.stroke` на всю ширину экрана. Без библиотек (Haze собран под Kotlin 2.x, проект на 1.9). Ловушка: блюр, нарисованный через отдельный `GraphicsLayer.record { drawLayer(content) }` в `drawBehind`, не размывал — работает `renderEffect` у модификатора `graphicsLayer`; и под блюром нужна непрозрачная подложка, иначе чёткий список просвечивает сквозь полупрозрачные размытые края.

Карточки характеристик (владелец 2026-10-08: по три в ряд, В2): `AbilityScoreCard` в `attributes/AttributesScreen.kt` — `BorderLabelCard(border = primary, frameOverContent = true, notchMarks = true)` (Д1: золотая линия 1dp, ромбики у названия; название `titleLarge` `text.primary` с той же тенью, что у модификатора, — тень под буквами, не плашка), тело `aspectRatio(7f / 12f)` (таро) с фоном `surface.card`; арт `ability_art_<ability>` (`drawable-nodpi`, `ContentScale.Crop`, по верху) на всю карточку, чистый до черты и от неё на `AbilityCardArtFade` (42dp, от низа) уходит в `surface.card` — не выше черты (Р3); поверх всего виньетка портрета (радиальный градиент `.55 → ornament.dropShadow`, растянутый в овал). Внизу по центру: `RollMarker` и модификатор 40 (`hpTemporary`) с тенью `ornament.dropShadow` (blur 12dp, текст рисуется трижды через `drawWithContent`), золотая черта 1dp на `AbilityCardRuleShare` (76 %) ширины, тающая к краям (`Brush.horizontalGradient` прозрачный → золото → золото → прозрачный), значение `titleLarge` `text.label` с той же тенью. Арты пока заглушки из концепта владельца (313 × 262, мутнеют при растяжении); финальные — 7:12, от 700 × 1200, главное в верхних двух третях, низ потемнее под цифры.

Спасброски (владелец 2026-10-08: S1, двумя рамками): раздел сразу после характеристик, `SavingThrowGroups` в `attributes/AttributesScreen.kt` — две рамки `OutlinedPanel(cornerRadius = 7.dp)` рядом через 10dp, как группы навыков, в каждой три `SaveLine` (строка 36dp, как `SkillLine`): `TrainingDot` (общий с навыками), название `attributes_save_<ability>` (`maxChars` 14: полстроки 115dp при 14sp), `RollMarker`, бонус; владение — золото. Тап — бросок спасброска. Карточки характеристик без строки «Спас.».

Выбор многих из списка (владения, языки, инструменты, мастерство, защиты; выбор владельца с досок 2026-10-05): не чекбоксы, а метки `ToggleChip(role = Role.Checkbox)` в группах, все группы открыты. У группы — название `titleMedium`, сколько выбрано («· 2», `bodyMedium text.label`) и, если правила знают «всю группу», метка «Всё» справа. Свой вариант — золотая метка с крестиком, добавляется полем и «+» (`StepButton`), Enter тоже добавляет; набранное, но не добавленное, «Сохранить» тоже забирает. Образец — `OptionGroup` / `OptionChip` / `CustomEntries` в `attributes/AttributesScreen.kt`.

Выбор одного из картинок со своим цветом (мировоззрение, выбор владельца с досок 2026-10-06): сетка карточек в `EditDialog`, у карточки иконка 44dp в цвете её группы и подпись `labelMedium` в две строки; выбранная — свой цвет на 15 % и обводка 1.5dp им же, остальные `surface.button`; тап выбирает и закрывает. Образец — `biography/AlignmentDialog.kt`, иконки — `biography/AlignmentIcons.kt`.

Текст, который пишут прямо на листе (идеалы, привязанности, слабости; выбор владельца с досок 2026-10-06): в рамке раздела (`OutlinedPanel`) иконка 24dp золотом (`primary`) по центру подписи и первой строки текста (как все иконки блока биографии, кроме мировоззрения), справа подпись золотыми прописными (`BiographyLabel`: `labelMedium`, разрядка 1sp, `primary`), под ней `BasicTextField` без своей рамки, `bodyLarge text.primary`, подсказка тем же кеглем в `text.subtle`; черновик сохраняется при уходе из поля и при уходе с экрана, как «История персонажа». Тап по подписи ставит курсор в текст. Части блока разделяет `BiographyOrnament` (линия с ромбиком, `ornament.stroke` / `ornament.middle`), мировоззрение — медальон `AlignmentSeal` на верхней кромке рамки. Образец — `BiographyPersonaSection` / `BiographyInlineText` в `biography/BiographyScreen.kt`.

Рост и размер вместе (`biography/HeightSizeDialog.kt`, выбор владельца с досок 2026-10-06: H2): ростомер с золотой ручкой — тянуть вверх-вниз или тапнуть, `EditDialog(scrollable = false)`, чтобы окно не прокручивалось под пальцем; фигура размера (гном, вампир, голем) ростом с выбранное у шкалы, позади неё справа человек 175 см для сравнения — `text.primary` на 30 %, без подписи (выбор владельца 2026-10-06). Фигуры — фон: пунктиры, шкала, подписи и ручка поверх, с подложкой цвета окна (`surfaceContainerHigh`: линия под пунктиром, тень под буквами), чтобы читаться на светлой фигуре. Размер считает `sizeForHeightCm` (domain/dnd5e/rules).

Окна внешности (`biography/BiographyEditors.kt`, выбор владельца с досок 2026-10-07):
- Выбор одного из нескольких с картинкой (пол, G1): ряд карточек `weight(1f)`, высота 96, углы 12, `toggleFill` / `toggleContent`, иконка 36dp над подписью `bodyMedium`; тап выбирает и закрывает, вариант «своё слово» открывает поле и даёт `onConfirm` (без него у `EditDialog` нет кнопок). `GenderDialog`.
- Число со степперами (возраст, A1): `StepButton` − и + вплотную к большому числу (зазор 16dp, место числа — 80dp, на четыре цифры, чтобы кнопки не съезжали под пальцем; `headlineMedium`, `BasicTextField` по центру; владелец 2026-10-08: «слишком далеко»), под ним слово единицы (`yearsWord` — «год / года / лет» по правилам русского). Удержание степпера листает (`Modifier.repeatWhileHeld`, ловит касание в проходе `Initial`, не трогая сам `StepButton`). `AgeDialog`.
- Линейка веса (`weightRuler`): с ростом — от ИМТ 12 до 42 для него, без роста — 10–150 кг; пустой вес открывается серединой линейки (не сохраняется без «Сохранить»). Число с единицей на одной базовой линии (вес): поле шириной в свой текст (`BigNumberField(fitted = true)` меряет текст `rememberTextMeasurer`) и единица `bodyLarge text.label` справа, оба `alignByBaseline()`. `IntrinsicSize` у `BasicTextField` не годится: после программной смены значения поле не перемерялось и число пропадало.
- Шкала из цветных полос (ИМТ, W1): полосы раздельные, с зазором 3dp, ширина — по длине подписи, а не по доле величины (`Build.span`); метка — кружок `text.primary` на подложке `surface.card`, идёт вдоль своей полосы (`Build.along`); подписи `labelMedium` под полосами теми же весами, текущая — `text.primary`, остальные `text.subtle`. `BmiScale`.
- Цвет персонажа (глаза, волосы, кожа; C2): `FlowRow` плашек высотой 36, углы 10, кружок 18dp цвета образца без обводки (O2, 2026-10-08: чёрные почти сливаются с фоном, но название рядом), подпись `bodyMedium`; последняя плашка — «+ Свой». Образцы — `LocalDesignTokens.current.swatches` (раздел `swatches` в `design_tokens.json`, не палитра темы). В ячейке листа — кружок 12dp перед значением (`BiographyRow.dot`). Телосложение — только в окне веса, в ячейке листа его нет (владелец 2026-10-08: «индикатор чисто для поп-апа»).

Заметки на месте (`notes/NotesScreen.kt`, N1, 2026-10-07): карточка `OutlinedPanel`; свёрнутая — `NoteFolded`, раскрытая — `NoteEditor`, разложены одинаково: булавка (`NotePinMark`) только в строке заголовка, текст всегда от края карточки (поля `BasicTextField` без рамки: заголовок `titleLarge`, текст `bodyLarge text.muted`; внизу круглые 40dp — булавка `toggleFill`, корзина `dangerHpZero` 15 %). Сохраняет при уходе фокуса из карточки (`onFocusChanged` на колонке, `hasFocus`) и при выходе из композиции; новая заметка (id 0) отдаётся один раз (`handedOver`), пустая исчезает. Удаление — сразу, с `LocalAppSnackbar.show(…, common_undo)`. Новая заметка поднимается над клавиатурой целиком: `BringIntoViewRequester` на карточке, перезапуск по `WindowInsets.ime` — окно приложения при клавиатуре сдвигается целиком (adjustPan), и запрос доходит до системы как `requestRectangleOnScreen`. «+» экрана прячется при `WindowInsets.isImeVisible`.

## Превью

Экранные превью живут в `<экран>/<Экран>ScreenPreview.kt` (у обзора — внутри `OverviewScreen.kt`), устройство `spec:width=412dp,height=915dp`, `showSystemUi = true`. Статы (характеристики, навыки, владения, защиты) — не отдельный экран, а секция списка обзора под его карточками (`AttributesSection` отдаёт обзору свои пункты списка и сама держит свои поп-апы); `AttributesScreenPreview` показывает эту секцию отдельно. Так же заметки — секция списка биографии (`NotesSection`; новую открывает плавающий «+» экрана, первая заметка в превью раскрыта), `NotesScreenPreview` показывает её отдельно:

`AttributesScreenPreview`, `BiographyScreenPreview`, `CombatScreenPreview`, `FeaturesScreenPreview`, `InventoryScreenPreview`, `InventoryContainersPreview`, `NotesScreenPreview`, `SpellsScreenPreview`, `OverviewScreenPreview`, `OverviewDyingPreview`, `OverviewDeadPreview`, `CharacterManagerDrawerPreview`, `CharacterManagerDrawerEmptyPreview`, `DiceSkinsPreview`, `SplashScreenPreview`, `SpellSlotsConfigDialogPreview`.

Строки в превью — рукописная карта `LocalizedStrings(language, mapOf(...))`: новый ключ экрана добавлять и туда.

Темы (2026-10-05): `DnDTheme(theme)` в превью — палитра и вид темы; что тема рисует иначе — `ThemeLook` (`LocalThemeLook`: `frames`, `portrait`, `xpBar`), его читают компоненты, а не экраны. У обзора превью в обеих темах (`ClassicOverviewRussianPreview`, `EngravedOverviewRussianPreview`).

Общие части, добавленные 2026-10-04:
- `components/TabIcons.kt` — `TabIconOverview`, `TabIconCombat`, `TabIconInventory`, `TabIconSpells`, `TabIconFeatures`, `TabIconBiography`: иконки вкладок нижней панели (щит с фигурой, скрещённые мечи, мешок с верёвкой, волшебная палочка, розетка с лентами, свиток), залитые, на сетке 24. Почти все из Material Design Icons; палочка — Material Symbols Rounded `wand_stars`; мешок — тело MDI `sack` с нашей горловиной и верёвкой, вырезанной зазором (собран булевыми операциями над контурами).
- `components/Toggle.kt` — `toggleFill(selected)`, `toggleContent(selected)`, `toggleRadioColors()`, `ToggleChip`: любой выбор из ряда или списка (выбранный — золото).
- `components/CardEditButton.kt` — `CardMainButton`: главное действие внизу раскрытой карточки с заливкой кнопки («Сотворить»), после текстовых «Изменить»/«Переложить».
- `components/AppSnackbar.kt` — `LocalAppSnackbar.current.show(message, actionLabel, onAction)`: уведомление сверху с «Отменить». Стол кубов рисуется поверх него — для действия с броском подпись идёт в итог на столе (`RollInput.resultTitle`).
- `combat/SpellCast.kt` — `SpellCastDialog`: окно сотворения (ячейки, концентрация, бросок) для экрана заклинаний и боя; `RollDialog` принимает `header`, `onRoll`, `enabled`, `castLabel`.

Превью частей, которые на устройстве зависят от удачи или не рисуются в превью экрана:
- `RollResultsPreview` в `combat/RollDialog.kt` — панели итога над 3D-столом (`DiceResultPanel`): натуральная 20 с кнопкой крита, натуральная 1, помеха, урон заклинания со СЛ, второй бросок крита. Строки берёт из `localization.json` (`LocalizationRepository(LocalContext.current)`), русские.
