# Токены: design_tokens.json

Единственный источник цветов, шрифтов и кеглей — `app/src/main/assets/design_tokens.json`. Его читает `presentation/theme/DesignTokens.kt` (`loadDesignTokenSet`): кегли общие, а **цвета — свои у каждой темы**, `themes.<тема>.colors` (`classic`, `engraved`; с 2026-10-05). `DnDTheme(theme)` раздаёт палитру выбранной темы через `LocalDesignTokens` и `MaterialTheme.colorScheme`, кегли — через `MaterialTheme.typography`. У каждой темы полный набор тех же ролей, это проверяет `DesignTokenThemesTest`. Правило файла (`usage.rule`): если нужен новый шрифт, кегль или цвет — спросить владельца до того, как добавлять; новый цвет вписывается в палитру **каждой** темы.

Таблицы цветов ниже — палитра «Классики» (`themes.classic.colors`); у «Гравюры» те же роли, свои значения. Контраст по палитре темы: `scripts/contrast.py --theme engraved …`.

Частоты в скобках сняты замером по `presentation/` (2026-10-03) — ориентир «что канон, а что исключение».

## Как обращаться в коде

| Что | Как |
|---|---|
| Цвет палитры приложения | `val colors = LocalDesignTokens.current.colors` → `colors.text.primary`, `colors.surface.card`, `colors.border.default`, `colors.accent.heal` |
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

## Палитра приложения (`themes.<graphite|plum>.colors.app`)

Слоты (2026-10-11): 9 цветов на тему — N1, N3–N9 и золото (N2, чёрный тени и тёмный текст на золоте слиты с N1 — владелец, 2026-10-11); каждый слот — шаг лестницы из десяти нейтральных (N0 самый тёмный … N9 цвет текста) или этот шаг на ступени прозрачности. Место в интерфейсе берёт слот по смыслу; одинаковые по смыслу места не могут разойтись по цвету. Новый слот — только через владельца.

| Слот | Шаг | Графит / Слива | Роль |
|---|---|---|---|
| `text.primary` | N9 | #F1EFEC / #F0EFF0 | значения, имена, иконки |
| `text.muted` | N8 | #CACACD / #CCC7CC | подписи, вторичный текст, полоса опыта |
| `text.subtle` | N7 | #9F9FA4 / #9F939F | выключенное, пустое, тихое |
| `background.page` | N1 | #0E0E10 / #0E0B0E | фон всех экранов, один цвет |
| `surface.card` | N3 | #19191B / #181318 | карточки и рамки статов |
| `surface.raised` | N4 | #242427 / #241C24 | над карточками: выбранная строка, поп-апы и шторки (Material surfaceContainerHigh) |
| `surface.button` | N5 | #39393D / #362B36 | **стандартная заливка кнопки**, 1.58:1 к карточке; текст и иконка `text.primary` |
| `border.default` | N9 · 30 % | #4DF1EFEC / #4DF0EFF0 | рамки и обводки |
| `border.strong` | N9 · 50 % | #80F1EFEC / #80F0EFF0 | обводка выбранного |
| `border.faint` | N9 · 15 % | #26F1EFEC / #26F0EFF0 | самые тихие линии, дорожки полос, разделители |
| `border.rule` | N7 · 30 % | #4D9F9FA4 / #4D9F939F | линии заголовков разделов и шапки |
| `shadow.drop` | N1 · 50 % | #800E0E10 / #800E0B0E | мягкая тень под надписями и знаками на арте |
| `accent.*` | — | общие у обеих тем | красный, зелёный, синий, оранжевый, золото вдохновения, монеты, типы урона — сводятся следующим шагом |

## Цвета Material (`themes.<тема>.colors.materialTheme`)

Роли Material разложены по той же лестнице (фон N1, поверхности N3–N5, обводка N6, текст N9/N8); золото — `primary` #C6A36C, текст на нём `onPrimary` = фон N1; `primaryContainer` = приподнятая N4, `onPrimaryContainer` = N9, `secondary`/`onSecondary` = золото и текст на нём (2026-10-11). Компоненты Material берут их сами; в своём коде — `MaterialTheme.colorScheme.primary` для золота.

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
