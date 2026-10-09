# Токены: design_tokens.json

Единственный источник цветов, шрифтов и кеглей — `app/src/main/assets/design_tokens.json`. Его читает `presentation/theme/DesignTokens.kt` (`loadDesignTokenSet`): кегли общие, а **цвета — свои у каждой темы**, `themes.<тема>.colors` (`classic`, `engraved`; с 2026-10-05). `DnDTheme(theme)` раздаёт палитру выбранной темы через `LocalDesignTokens` и `MaterialTheme.colorScheme`, кегли — через `MaterialTheme.typography`. У каждой темы полный набор тех же ролей, это проверяет `DesignTokenThemesTest`. Правило файла (`usage.rule`): если нужен новый шрифт, кегль или цвет — спросить владельца до того, как добавлять; новый цвет вписывается в палитру **каждой** темы.

Таблицы цветов ниже — палитра «Классики» (`themes.classic.colors`); у «Гравюры» те же роли, свои значения. Контраст по палитре темы: `scripts/contrast.py --theme engraved …`.

Частоты в скобках сняты замером по `presentation/` (2026-10-03) — ориентир «что канон, а что исключение».

## Как обращаться в коде

| Что | Как |
|---|---|
| Цвет палитры приложения | `val colors = LocalDesignTokens.current.colors` → `colors.text.primary`, `colors.surface.card`, `colors.border.panel`, `colors.accent.heal` |
| Цвет Material | `MaterialTheme.colorScheme.primary` и т.д. (значения из `themes.<тема>.colors.materialTheme`; чего там нет — тёмные значения Material по умолчанию) |
| Что тема рисует иначе, кроме цветов | `LocalThemeLook.current`: `frames` (`FrameStyle`), `portrait` (`PortraitStyle`), `xpBar` (`XpBarStyle`); фон темы — `LocalThemeBackdrop` (`presentation/theme/ThemeLook.kt`) |
| Шкала шрифтов | `MaterialTheme.typography.bodyLarge` и т.д. |
| Особый кегль | `val t = LocalDesignTokens.current.typography` → `MaterialTheme.typography.headlineMedium.copy(fontSize = t.hpCurrent.fontSizeSp.sp, lineHeight = (t.hpCurrent.lineHeightSp ?: t.hpCurrent.fontSizeSp).sp)`; у `hpMaximum` есть ещё `alpha` |

Новый цвет добавляется в три места разом: `design_tokens.json` (в палитру каждой темы) → data-класс группы в `DesignTokens.kt` (с дефолтом) → использование через токен. Только после одобрения.

## Шрифты

| Роль | Семейство | Начертания |
|---|---|---|
| `display` — заголовки, крупные числа | `FontFamily.Serif` | headline `Bold`, title `SemiBold` |
| `body` — текст, подписи | `FontFamily.SansSerif` | `Normal`, акцент `Medium` |

Других шрифтов в интерфейсе нет. Шрифты кубиков (`assets/fonts/dice/`) — содержимое скинов, не UI.

## Шкала кеглей (`typography.materialTheme`)

| Стиль | Семейство | Кегль / интерлиньяж | Применений | Роль |
|---|---|---|---|---|
| `headlineMedium` | display Bold | 28 / 32 | 27 | крупные числа, имя |
| `titleLarge` | display SemiBold | 22 | 12 | заголовок поп-апа, подпись HP |
| `titleMedium` | display SemiBold | 18 | 33 | заголовок карточки, секции, статус |
| `bodyLarge` | body | 16 / 22 | 110 | **основной текст** |
| `bodyMedium` | body | 14 / 20 | 68 | вторичный текст, описания |
| `labelMedium` | body Medium | 12 | 11 | подписи на рамках, мелкие метки |

## Особые кегли (`typography.overviewOverrides`)

Именованные размеры для конкретных мест; брать их, а не вводить свой `fontSize`.

| Токен | База | Кегль / интерлиньяж | Где |
|---|---|---|---|
| `characterName` | headlineMedium | 32 / 35 | имя на обзоре |
| `portraitInitial` | headlineMedium | 40 | буква в портрете-заглушке |
| `actionButtonLabel` | bodyLarge Medium | 12 / 13 | подписи кнопок отдыха и вдохновения |
| `xpLabel` | bodyLarge | 16 | «EXP 0 / 300» |
| `hpCurrent` | headlineMedium | 64 / 68 | текущие хиты |
| `hpTemporary` | headlineMedium | 40 / 44 | временные хиты |
| `hpMaximum` | headlineMedium | 40 / 44, alpha 0.7 | максимум хитов |
| `hpLabel` | titleLarge | 22 | «HP» на рамке |
| `miniStatValue` | headlineMedium | 28 / 30 | значение карточки стата |
| `miniStatLabel` | bodyLarge | 12 | подпись карточки стата |
| `subtitleToken` | bodyLarge | 16 | «Вид • Класс • Уровень» |
| `shortRestDiceCount` | headlineMedium | 32 | «2/3» костей хитов |
| `shortRestDieToken` | bodyLarge | 18 | «d10» |
| `shortRestCounterButton` | headlineMedium | 28 | «−» и «+» короткого отдыха |
| `shortRestCounterValue` | headlineMedium | 40 | число костей к трате |

## Палитра приложения (`themes.classic.colors.app`)

| Группа.имя | Значение | Роль |
|---|---|---|
| `text.primary` | #F7F2EA | основной текст и числа |
| `text.warmPrimary` | #FFF6EA | тёплый светлый |
| `text.action` | #F1ECE5 | подпись кнопки действия |
| `text.muted` | #D2CAC2 | вторичный текст |
| `text.label` | #C2BBB3 | подписи на рамках, неактивные иконки |
| `text.miniLabel` | #BEB6AE | подпись карточки стата |
| `text.subtle` | #AAA29A | третичный, неактивный |
| `text.icon` | #F3EEE6 | иконки |
| `background.radialStart/Middle/End` | #1A161D / #0E0B11 / #09070D | радиальный фон окна (`ScreenBackground`) |
| `surface.card` | #17141B | фон карточек и статов |
| `surface.button` | #3B3840 | **стандартная заливка кнопки** (одобрена 2026-10-03), 1.58:1 к карточке; текст и иконка `text.primary` |
| `surface.option` | #1A171D | прежний «фон кнопки», почти как `card`: строки выбора и варианты переключателей, пока их не переведут на палитру |
| `surface.selected` | #3A3244 | выбранный вариант |
| `surface.inspiration` | #2A2419 | включённое вдохновение |
| `surface.portrait*` | #141118, #3B3840 / #18151C / #0F0C12 | портрет и его заглушка |
| `border.default` | #4DFFFFFF | обводка кнопок и полей |
| `border.panel` | #4DFFFFFF | обводка панелей и карточек |
| `border.miniCard` | #4DFFFFFF | обводка карточки стата |
| `border.selected` | #80FFF6EA | обводка выбранного |
| `border.muted` | #26FFFFFF | тихая обводка |
| `accent.inspiration` | #FFD86B | золото: вдохновение, Level UP |
| `accent.xpCapped` | #E0B84E | полный опыт |
| `accent.hpTemporary` | #69B7FF | временные хиты |
| `accent.heal` | #8AD178 | лечение, успехи, «стабилен» |
| `accent.dangerHpZero` | #E85C5C | 0 хитов, провалы, удаление |
| `accent.coinCopper/Silver/Gold` | #C9824B / #C4C8D2 / #E0B548 | монеты |
| `accent.magical` | #69B7FF | магические предметы |
| `accent.damageFire/Cold/Lightning/Poison/Other` | #FF8A3D / #7BB7FF / #CFB6FF / #A8D76F / #D5C6B2 | типы урона |
| `progress.xpFill` / `xpTrack` | #D7D1CC / #26FFFFFF | полоса опыта |
| `ornament.*` | outer #26FFFFFF, middle #80C7C1BB, innerGlow #4DFFFFFF, inner #E9E2D9, shadow #26000000, stroke #4DA19892, dot #2D2730 | орнамент портрета |

## Цвета Material (`themes.classic.colors.materialTheme`)

| Роль | Значение | Где видно |
|---|---|---|
| `primary` / `onPrimary` | #C6A36C / #22170C | заливка «Сохранить» — эталон главной кнопки |
| `primaryContainer` / `onPrimaryContainer` | #49321A / #F3DDB8 | тёмная бронза |
| `secondary` / `onSecondary` | #9E7B5A / #21150C | |
| `background` / `onBackground` | #120E18 / #F0E7DA | |
| `surface` / `onSurface` | #1A1521 / #F0E7DA | фон поп-апов |
| `surfaceVariant` / `onSurfaceVariant` | #2A2231 / #CABFB3 | заливка плавающей кнопки «+» |
| `outline` / `outlineVariant` | #706359 / #423830 | рамки полей ввода |

Прозрачности — одна шкала (владелец, 2026-10-09: вариант А): `alpha` в JSON, в коде `LocalDesignTokens.current.alpha.<ступень>`. **faint 15 %** — подсветка выбора своим цветом (кнопки урона/лечения в «Гравюре», вид лечения, состояния, мировоззрение, вкладка, вдохновение), красное под корзиной, самые бледные линии и отблески; **line 30 %** — линии и обводки, потраченная кость хитов; **half 50 %** — обводка выбранного, подсказка пустого поля, узоры, тень под надписями; **veil 70 %** — заливка карточек поверх фона, тон шапки и панели вкладок, картинка фона. У цветов #AARRGGBB альфа — тоже ступень: 26 / 4D / 80 / B3. Заливка, которая должна закрывать фон, — непрозрачная. Новая прозрачность — через владельца, с рендерами.

## Размерные токены

В JSON есть, **но в код не проведены** (`DesignTokens` читает только `typography` и `colors`; `LocalDnDSpacing` в `Theme.kt` повторяет `spacingDp` руками и нигде не используется) — значения пишутся в коде как `dp` и должны совпадать с рядом ниже.

| Группа | Значения |
|---|---|
| `spacingDp` | xs 4, sm 8, md 16, lg 24, xl 32 |
| `radiiDp` | token 12, dieToken 14, actionButton 20, miniCard 24, hpCard 30 |
| `strokesDp` | hairline 1, portraitOuter 3, portraitInner 2, xpBar 11 |

Что реально встречается в коде:

- `Arrangement.spacedBy`: **8** (49), **10** (40), **6** (38), 4 (17), 12 (14), 2 (11), 16 (5).
- Горизонтальные поля внутри элементов: **12** (23), **14** (21), 8 (10), 16 и 20 (по 7); вертикальные: **12** (24), 10 (12), 8 и 4 (по 8).
- Поля экрана: `LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = LocalFloatingButtonsInset.current))` — у 8 экранов, между элементами `spacedBy(10.dp)`.
- `RoundedCornerShape`: **10** (45), **12** (22), 8 (10), 14 (4), 16, 20, 26 единично; карточка HP 30, карточка стата 24, кнопки обзора 20. Пилюля — `RoundedCornerShape(50)` или `CircleShape`.
- Обводка — **1dp** (81 применение); исключение 1.5 у кружков спасбросков.
- Иконки: 28 (9), 24 (6), 26 (4), 22 (4), 32, 18, 20.

Значения вне этого ряда (7, 9, 13…) не вводить; два единичных `7.dp` и `9.dp` в коде — кандидаты на выравнивание при следующей правке.
