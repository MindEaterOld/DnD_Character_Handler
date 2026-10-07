# Каталог общих компонентов и скелеты

Все компоненты — `app/src/main/java/com/dndcharacterhandler/presentation/components/`. Колонка «файлов» — в скольких файлах компонент реально используется (замер 2026-10-03). Перед созданием нового элемента искать здесь; отличие в размере, цвете, тексте или иконке закрывается параметром.

## Каркас экрана

| Компонент | Файлов | Роль |
|---|---|---|
| `ScreenBackground { }` | 9 | Фон темы на всё окно (радиальный градиент или иллюстрация темы — `LocalThemeBackdrop`); экран рисует только свою часть, фон бесшовно продолжается под нижней панелью и системными полосами |
| `CharacterScreenHeader(character, onOpenDrawer, onOpenDice)` | 7 | Шапка экрана персонажа: меню, кубы, имя и подзаголовок |
| `PinnedCharacterHeader(…)`, `Modifier.fadeUnderHeader()`, `CharacterHeaderInset` | 6 | Шапка закреплена сверху поверх списка (выбор владельца 2026-10-06): список — `contentPadding(top = CharacterHeaderInset + свой отступ)` и `.fadeUnderHeader()` (уходящее под шапку растворяется), шапка — после списка в том же `Box`, чтобы меню и кубы получали нажатия. Вариант `PinnedCharacterHeader(name, …, onNameClick)` — по имени, пока персонаж грузится; на обзоре тап по имени переименовывает. Что выше `CharacterHeaderFadeEnd`, в покое растворяется: обзор опускает портрет так, чтобы его рамка (с учётом `ThemeLook.portraitArtworkTop`) начиналась ниже |
| `ScreenTopActions(onOpenDrawer, onOpenDice)` | 9 | Верхний ряд: «гамбургер» слева, d20 справа (кнопки 44dp, иконки 28) |
| `ScreenTopActionButton(onClick) { Icon }` | 2 | Голая иконка верхней панели без заливки, тап-зона 44dp — единственная кнопка без подложки (меню, кубы, отдых столбиком под кубами на обзоре) |
| `PortraitSideButton(icon, contentDescription, onClick, add, on)` | 4 | Кнопка у портрета на обзоре: «монета» 44dp — подложка `surface.card`, обводка от `text.label` сверху к `border.muted` снизу, тень `ornament.dropShadow` на 2dp ниже; иконка 24dp `text.primary`; `add = true` — маленький серый «+» в углу (кнопка состояний); `on` — переключатель (вдохновение): вкл. — подсветка `accent.inspiration` 12 % и иконка в нём. Иконки `SideIconConditions`, `SideIconShortRest`, `SideIconLongRest`, `SideIconInspiration` |
| `BottomNavigationBar` | — | Панель вкладок: обводка поверх фона, без своей заливки |
| `FadingLazyColumn(…)`, `Modifier.fadingVerticalScroll()`, `Modifier.fadingEdges(state)` | 15 | Мягкие края прокрутки в поп-апах и шторках: уходящее за край растворяется на 24dp, край — только пока туда есть что листать |
| `EditSheet(title, onDismiss) { … }` | 1 | Шторка снизу: ручка, заголовок `titleLarge` засечками, содержимое прокручивается, без кнопок и крестика (отметки применяются сразу), не выше 75 % экрана; в «Гравюре» гравированная панель без нижней кромки |
| `ShortRestDialog(…)`, `LongRestDialog(…)`, `RestHitPoints(current, max, gain, exact)`, `HitDicePools(…)`, `RestRows(title, rows)` | 2 | Короткий отдых (`overview/ShortRestDialog.kt`): хиты с полосой и прибавкой зелёным, кости хитов кубиками в скине игрока (выбрана — золото, доступна — заливка кнопки, потрачена — контур и 30 %), ряд на размер кости при мультиклассе, список «Восстановится»; `RestHitPoints`, `HitDicePools` (на долгом — потраченные зелёным, «вернутся») и `RestRows` (строка — иконка, имя, «было → станет», свой цвет иконки) — общие для окон отдыха |
| `FloatingAddButton(onClick)` | 5 | Круглая плавающая «+» 58dp в правом нижнем углу (`padding(end = 24.dp, bottom = 15.dp)`) |
| `LocalFloatingButtonsInset` | — | Нижнее поле списка, чтобы последние элементы выезжали из-под плавающих кнопок; приложение задаёт на экран (`SingleFloatingButtonInset` 110, `NoFloatingButtonInset` 16) |
| `OverlayCloseButton(onClick)` | 5 | Круглый крестик полноэкранного оверлея (портрет, стол кубов), сам держится вне системных полос |

## Поп-апы

| Компонент | Файлов | Роль |
|---|---|---|
| `EditDialog(title, onDismiss, onConfirm, …)` | 14 | **Единственный поп-ап приложения.** Заголовок с крестиком в углу (закрыть без сохранения, кнопки «Отмена» нет), содержимое прокручивается целиком, внизу один ряд: «Удалить» — круглая тональная (красный 16 % за иконкой) слева, главное действие («Сохранить» по умолчанию, заливка `primary`) справа. `confirmIsDanger` — главное действие красным (подтверждение удаления). Без `onConfirm` и `onDelete` нижнего ряда нет — тап по пункту делает работу. `titleActions` — кнопки перед крестиком, `titleLeading` — перед заголовком (стрелка назад) |
| `DeleteCharacterDialog` | — | Подтверждение удаления персонажа |
| `SettingsDialog` | — | Настройки (язык), изменения сразу |
| `WeaponMasteryDialog` | — | Правила свойства мастерства оружия |

Черновик правки в поп-апе при смерти процесса теряется намеренно — `rememberSaveable` в поп-апы не добавлять.

## Карточки и статы

| Компонент | Файлов | Роль |
|---|---|---|
| `BorderLabelCard(label, …)` | 3 | Рамка с подписью в разрыве верхней обводки (как у outlined-поля); основа карточек статов, характеристик и HP |
| `ArmorClassShield(label, value, worse, better, onClick)` (`overview/`) | 1 | Класс брони в щите на обзоре (вариант D1 с доски): контур щита и второй контур внутри, подпись над числом 40sp (пока размер временных хитов), стрелки состояний перед числом; рябь внутри щита. Стоит между инициативой и скоростью |
| `OutlinedPanel { }` | 1 | Рамка без подписи вокруг столбца строк — «лист» владений и защит на обзоре: обводка по поверхности карточки, в «Гравюре» гравированная рамка, рябь строки не выходит за углы. Строки внутри — `SheetRow` (иконка 22 и имя в столбце 140dp, значения через запятую, «Нет» тихим `text.subtle`, линия `border.muted` между строками) в `attributes/AttributesScreen.kt` |
| `MiniStatCard(label, value, icon?, onClick?, compact?)` | 6 | **Карточка стата**: `BorderLabelCard` со значением, шагает вниз по шкале на длинном значении. Единственная карточка этого вида — брать её везде, где экран показывает статы. `compact = true` — тише: 60dp, значение `titleLarge` 22, иконка `MiniStatCardIcon(…, MiniStatCardCompactIconSize)` (бонус мастерства и чувства под боевой тройкой обзора) |
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

## Текст, картинки, эффекты

| Компонент | Файлов | Роль |
|---|---|---|
| `AutoSizeText(…)` + `LocalAutoSizeGroup` | 1 | Текст, который шагает вниз **по шкале темы** (не произвольными sp), пока не влезет в `maxLines` без разрыва слов; группа даёт ряду общий кегль |
| `AppImage(imageRef, contentDescription, framing, fallback)` | 3 | Картинка по ссылке проекта (`res:drawable/…`, ассеты, файл, `content://`), декодирует вне главного потока, пока грузит — `fallback`; с `framing` (`PortraitFraming`) показывает выбранную часть портрета вместо центрального кропа |
| `rememberAppImagePainter(imageRef)` | 2 | Тот же `Painter`, что рисует `AppImage`, — для своего `Canvas` (окно кадрирования) |
| `octagonPath(center, radius)`, `OctagonShape` (`overview/PortraitFramingDialog.kt`) | 2 | Восьмиугольник портрета: контур для `Canvas` и форма для `clip` |
| `Modifier.saturation(s)` | 1 | Насыщенность содержимого: 1 как есть, 0 — чёрно-белое (портрет погибшего); работает на любом содержимом |
| `SkullIcon` | 1 | Череп спасбросков от смерти (`ImageVector`, тонируется `Icon`) |
| `GothicPortraitFrame(onClick, progress, progressColor, plaque) { портрет }` | 1 | Рама портрета на обзоре — она же полоса опыта: кольцо 4dp по контуру от левого края таблички в нижней грани обратно в неё; в «Классике» рисованный восьмиугольник, в «Гравюре» — картинка темы, кольцо внутри; `plaque` — табличка уровня (`LevelPlaque`) |
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
    FloatingAddButton(align BottomEnd, padding end 24, bottom 15)
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

Выбор многих из списка (владения, языки, инструменты, мастерство, защиты; выбор владельца с досок 2026-10-05): не чекбоксы, а метки `ToggleChip(role = Role.Checkbox)` в группах, все группы открыты. У группы — название `titleMedium`, сколько выбрано («· 2», `bodyMedium text.label`) и, если правила знают «всю группу», метка «Всё» справа. Свой вариант — золотая метка с крестиком, добавляется полем и «+» (`StepButton`), Enter тоже добавляет; набранное, но не добавленное, «Сохранить» тоже забирает. Образец — `OptionGroup` / `OptionChip` / `CustomEntries` в `attributes/AttributesScreen.kt`.

Выбор одного из картинок со своим цветом (мировоззрение, выбор владельца с досок 2026-10-06): сетка карточек в `EditDialog`, у карточки иконка 44dp в цвете её группы и подпись `labelMedium` в две строки; выбранная — свой цвет на 12 % и обводка 1.5dp им же, остальные `surface.button`; тап выбирает и закрывает. Образец — `biography/AlignmentDialog.kt`, иконки — `biography/AlignmentIcons.kt`.

Текст, который пишут прямо на листе (идеалы, привязанности, слабости; выбор владельца с досок 2026-10-06): в рамке раздела (`OutlinedPanel`) иконка 24dp золотом (`primary`) по центру подписи и первой строки текста (как все иконки блока биографии, кроме мировоззрения), справа подпись золотыми прописными (`BiographyLabel`: `labelMedium`, разрядка 1sp, `primary`), под ней `BasicTextField` без своей рамки, `bodyLarge text.primary`, подсказка тем же кеглем в `text.subtle`; черновик сохраняется при уходе из поля и при уходе с экрана, как «История персонажа». Тап по подписи ставит курсор в текст. Части блока разделяет `BiographyOrnament` (линия с ромбиком, `ornament.stroke` / `ornament.middle`), мировоззрение — медальон `AlignmentSeal` на верхней кромке рамки. Образец — `BiographyPersonaSection` / `BiographyInlineText` в `biography/BiographyScreen.kt`.

Рост и размер вместе (`biography/HeightSizeDialog.kt`, выбор владельца с досок 2026-10-06: H2): ростомер с золотой ручкой — тянуть вверх-вниз или тапнуть, `EditDialog(scrollable = false)`, чтобы окно не прокручивалось под пальцем; фигура размера (гном, вампир, голем) ростом с выбранное у шкалы, позади неё справа человек 175 см для сравнения — `text.primary` на 30 %, без подписи (выбор владельца 2026-10-06). Фигуры — фон: пунктиры, шкала, подписи и ручка поверх, с подложкой цвета окна (`surfaceContainerHigh`: линия под пунктиром, тень под буквами), чтобы читаться на светлой фигуре. Размер считает `sizeForHeightCm` (domain/rules).

## Превью

Экранные превью живут в `<экран>/<Экран>ScreenPreview.kt` (у обзора — внутри `OverviewScreen.kt`), устройство `spec:width=412dp,height=915dp`, `showSystemUi = true`. Статы (характеристики, навыки, владения, защиты) — не отдельный экран, а секция списка обзора под его карточками (`AttributesSection` отдаёт обзору свои пункты списка и сама держит свои поп-апы); `AttributesScreenPreview` показывает эту секцию отдельно. Так же заметки — секция списка биографии (`NotesSection`; последняя карточка — «Добавить заметку» с заливкой кнопки, плавающей «+» нет), `NotesScreenPreview` показывает её отдельно:

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
