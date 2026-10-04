# Инструменты и пайплайны

Пайплайны — проверенные последовательности шагов. Нашёл новый рабочий приём или наступил на ловушку — дописать сюда, с датой, если это факт о среде.

## Инструменты

Gradle в оболочке Claude не запускается (AF_UNIX). Всё, что требует сборки, идёт **через Android Studio**: она должна быть открыта с проектом и с включённым плагином MCP Server (Settings → Tools → MCP Server).

### Studio MCP — сборка, тесты, терминал

Сервер на `http://127.0.0.1:64342/stream`, в `.mcp.json` проекта он записан как `android-studio`. Если его инструменты не загружены в сессию, использовать клиент скилла:

```bash
python .claude/skills/ui-layout/scripts/studio_mcp.py list
python .claude/skills/ui-layout/scripts/studio_mcp.py call build_project '{"projectPath":"D:/Reps/DnD_Character_Handler","timeout":600000}'
python .claude/skills/ui-layout/scripts/studio_mcp.py call execute_terminal_command @args.json
```

`projectPath` передавать **всегда**, иначе сервер отвечает «не определить проект».

| Задача | Инструмент | Время |
|---|---|---|
| Собрать, получить ошибки компиляции | `build_project` → `{"isSuccess":true,"problems":[]}` | ~30 с |
| Ошибки и предупреждения файла | `get_file_problems` (`filePath`, `errorsOnly`) | быстро |
| Поставить на эмулятор | `execute_terminal_command`: `$env:JAVA_HOME = 'D:/Apps/AndroidStudio/jbr'; ./gradlew.bat :app:installDebug --console=plain` | ~30 с |
| Юнит-тесты | то же, `./gradlew.bat :app:testDebugUnitTest --tests '*LocalizationLimitsTest' --console=plain` | от 1 с |

Аргументы для `execute_terminal_command` писать в JSON-файл из Python (`json.dump`), а не строкой в shell: кавычки и `$env:` иначе ломаются. Пути — прямыми слэшами. `executeInShell: true`, `truncateMode: "NONE"` с `maxLinesCount` — и читать хвост вывода (`command_exit_code`, `BUILD SUCCESSFUL`). `execute_run_configuration` для `app` не стартует («Process not started») — ставить через `installDebug`.

### Android CLI — рендер превью в PNG

Google Android CLI 1.0 (установлен через `winget install -e --id Google.AndroidCLI`):

```bash
A="$LOCALAPPDATA/Microsoft/WinGet/Packages/Google.AndroidCLI_Microsoft.Winget.Source_8wekyb3d8bbwe/android.exe"
"$A" studio check     # видит Studio (Quail 1) и проект «DnD Character Handler»
"$A" studio render-compose-preview --project="DnD Character Handler" \
  --output-image-file="<scratchpad>/overview.png" \
  "D:/Reps/DnD_Character_Handler/app/src/main/java/com/dndcharacterhandler/presentation/overview/OverviewScreen.kt" \
  OverviewScreenPreview
```

- `--project` обязателен: из Git Bash проект по текущей папке не угадывается.
- Путь к файлу — абсолютный, имя — имя `@Preview`-функции (список — `kit.md`, «Превью»).
- `--print-semantics` печатает дерево узлов с текстами и границами в пикселях кадра — это инструмент замера.
- Рендер ~10 с. Картинку смотреть через `Read`, владельцу — `SendUserFile` (`display: render`).
- **Рендер берёт последнюю сборку Studio.** После правки кода — сначала `build_project`.

### Эмулятор и adb

```bash
"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd Pixel_8 -no-snapshot-save   # в фоне (run_in_background)
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell getprop sys.boot_completed                                          # 1 — загрузился (~25 с)
"$ADB" shell monkey -p com.dndcharacterhandler -c android.intent.category.LAUNCHER 1
"$ADB" exec-out screencap -p > shot.png                                          # 1080×2400
"$ADB" shell input tap X Y ; "$ADB" shell input text 13 ; "$ADB" shell input keyevent KEYCODE_BACK
"$ADB" shell uiautomator dump /sdcard/ui.xml && "$ADB" exec-out cat /sdcard/ui.xml   # узлы с bounds
"$ADB" logcat -d | grep NuPlayerDriver                                           # проигрывался ли звук
```

`Pixel_8` — 412dp в ширину, как превью; плотность 2.625. Скриншот при чтении показывается 900×2000: координаты для `input tap` умножать на 1.2. Окно эмулятора открыто у владельца на экране — он видит то же самое.

## Пайплайн: правка → глаза

1. Правка кода (токены, компоненты по `SKILL.md`).
2. `build_project` — без ошибок.
3. Рендер затронутых экранных превью → посмотреть PNG.
4. Если затронуты поп-ап, анимация, звук, реальные данные — `installDebug` → запуск → скриншоты на эмуляторе.
5. Тексты трогались — `LocalizationLimitsTest`.
6. Отчёт; что не видели глазами — в «Проверить на устройстве».

## Пайплайн: проверка поп-апа на эмуляторе

Поп-апы (`AlertDialog`) в превью не рисуются, только так:

1. Скриншот экрана → координаты элемента, который открывает поп-ап (×1.2).
2. Тап → скриншот: открылось ли то, что нужно. Число на карточке HP открывает разные поп-апы: текущее — урон и лечение, максимум — «Изменить макс. HP».
3. Поле ввода: тап по полю, потом `input text`. После появления клавиатуры поп-ап уезжает вверх — **переснять скриншот** перед следующим тапом.
4. Режим поп-апа (`remember` в экране) переживает закрытие: при повторном открытии он прежний.
5. Изменённые данные тестового персонажа вернуть как было тем же способом и показать итоговый скриншот.

## Пайплайн: проверка, после которой интерфейсом не откатить

Новый портрет вместо пустого, смерть, удалённые вещи — интерфейс назад не вернёт. Тогда база целиком:

```bash
export MSYS_NO_PATHCONV=1; P=com.dndcharacterhandler
for f in dnd_character_handler.db dnd_character_handler.db-wal; do "$ADB" exec-out "run-as $P cat databases/$f" > "db/$f"; done   # до проверки
# ... проверка ...
"$ADB" shell am force-stop $P
"$ADB" shell "run-as $P rm databases/dnd_character_handler.db-wal databases/dnd_character_handler.db-shm"
"$ADB" exec-in run-as $P sh -c 'cat > databases/dnd_character_handler.db' < db/dnd_character_handler.db
```

- Если копия вместе с `-wal` — вернуть оба файла; `-shm` удалить всегда, SQLite соберёт его сам. Чужой `-wal` поверх своей базы её испортит.
- **Python `sqlite3.connect` на копии сливает `-wal` в базу и удаляет его** при закрытии. Читать копию — `sqlite3.connect('file:...db?mode=ro', uri=True)`; если уже открывал без этого — `-wal` больше нет, возвращать одну базу.
- Картинку для портрета — `adb push` в `/sdcard/Pictures/`, затем `adb shell content call --uri content://media --method scan_volume --arg external_primary`, чтобы её увидел выбор файла. После проверки удалить.
- Звук проверяется без ушей: `adb shell dumpsys audio | grep dndcharacter` — строка `new player` с временем на каждое проигрывание.

## Пайплайн: замер вместо глазомера

- **Превью:** `--print-semantics` даёт `left/top/right/bottom` в пикселях кадра. Плотность = ширина кадра / 412 (рендер 1082 px → 2.626). Отступ в dp = разница в px / плотность.
- **Эмулятор:** `uiautomator dump` → `bounds="[x1,y1][x2,y2]"`, плотность 2.625.
- Узел внутри `Row`/`Column` имеет ширину своего содержимого, а не доступного места: доступную ширину считать от контейнера (ширина минус поля, соседи и `spacedBy`).
- Сравнивать расстояния от одного и того же края, а не абсолютные координаты.

## Пайплайн: лимит символов (`maxChars`)

1. Найти ширину под текст на 412dp: ширина элемента минус поля, иконки и зазоры. Удобно взять из `--print-semantics`.
2. `maxChars = floor(ширина_dp / (0.58 × кегль_sp))` — формула из `CLAUDE.md` (0.58 — средний кириллический знак Roboto, русский самый широкий).
3. Проверить все 5 переводов; не влезает — сократить с точкой (`Предыст.`).
4. Записать `"maxChars": N` в ключ, прогнать `LocalizationLimitsTest`.
5. Сомнение, что формула верна для места (жирный шрифт, ряд с `weight`), — подставить на эмуляторе самый длинный перевод и снять скриншот.

Ключи добавлять скриптом, который пишет **UTF-8 как есть** (`ensure_ascii=False`), сохраняя формат файла: `maxChars` первым, затем `en ru de fr es`.

## Пайплайн: доска вариантов

Шаблон — `templates/VariantBoard.kt` (проверен рендером 2026-10-03).

1. Скопировать шаблон **в тот же пакет**, где живёт элемент. Если элемент `private` — дописать `@Preview` доски временно в конец его же файла.
2. Плитки: `"A  сейчас"` + варианты; подпись — что меняется и откуда значение (`B  radius 14 = dieToken`). Плитка 412dp шириной с полями экрана 24 — элемент в своём реальном размере. Высоту доски (`heightDp`) подогнать под содержимое.
3. Тексты в доске — настоящие (`LocalizationRepository(context).getStrings(AppLanguage.RUSSIAN)`), тема — `DnDTheme`, фон — `ScreenBackground`.
4. `build_project` → `render-compose-preview` → `Read` → `SendUserFile`.
5. **Удалить** файл доски (или временный `@Preview`) и снова `build_project`. Проверить `git status`: доски в рабочей копии быть не должно.
6. Если варианты зависят от состояния (выбран / не выбран, длинный перевод) — строки доски = состояния, колонки = варианты (`maxItemsInEachRow`).

Приёмы, проверенные 2026-10-03 на досках обзора:

- **Доска для приватных композаблов экрана** (портрет, карточка HP, трей — всё `private` в `OverviewScreen.kt`): скопировать файл в скретчпад (`git diff --quiet` до этого — файл чистый), дописать блок досок в конец файла и недостающие импорты в начало, собрать, отрендерить, **вернуть файл из копии** и пересобрать. `grep` по именам досок после возврата должен дать 0.
- Экран в плитке собирается из настоящих кусков в `Column` с теми же полями (24), `spacedBy(10)` и теми же `offset`, что у `LazyColumn` экрана, — так плитка совпадает с экраном.
- Плитки — `data class BoardTile(val title: String, val details: String, val content: @Composable () -> Unit)` и вызов `BoardTile("A  …", "…") { … }`. `Triple(...)` с `@Composable`-лямбдой не компилируется.
- Несколько досок — несколько `@Preview` в одном блоке, рендер каждой по имени.
- Портрет в превью — настоящая картинка: `AppImage` в режиме превью (`LocalInspectionMode`) декодирует её сразу, а не в фоне (с 2026-10-03; до этого была буква-заглушка).

## Пайплайн: новый цвет или кегль

1. Сначала доказать, что ни один токен не подходит по роли (`tokens.md`, порядок подбора — `color.md`): кандидаты прогнаны через `scripts/contrast.py`, и ни один не проходит порог или не держит иерархию.
2. Показать доску: текущее, ближайшие существующие токены, предлагаемое новое (подпись «новое» и контраст к фону).
3. Только после «да» владельца: значение в `design_tokens.json` → поле в data-классе группы `DesignTokens.kt` (с дефолтом и чтением из JSON) → использование через токен. Для кегля — в `typography.overviewOverrides` или шкалу, и в `DesignTypographyTokens`.

## Пайплайн: проверка без Studio

Если Studio не запущена: компиляция и юнит-тесты возможны bundled-компилятором Studio (`kotlinc` из `D:/Apps/AndroidStudio/plugins/Kotlin/kotlinc`, рецепт classpath — в памяти Claude «Gradle cannot run here»). Рендер превью и установка без Studio невозможны — сказать об этом в отчёте, а не писать «проверено».

## Чеклист приёмки

- [ ] Каждый элемент, у которого есть аналог в `kit.md`, взят из кита.
- [ ] Повторяющийся элемент вынесен в `components/`, приватных копий нет.
- [ ] Цвета — токены (`colors.*` или `MaterialTheme.colorScheme`), нет `Color(0x…)` вне `theme/`, нет новых альфа-вариантов без одобрения.
- [ ] Шрифты и кегли — `MaterialTheme.typography.*` или именованный токен; нет произвольного `fontSize`.
- [ ] Отступы, радиусы, обводки — из ряда `tokens.md`.
- [ ] Кнопки с подложкой, статы с обводкой без фона; выбранный вариант переключателя виден по заливке.
- [ ] Контраст к реальному фону посчитан (`contrast.py`): текст ≥ 4.5, иконки и метки ≥ 3, заливка кнопки отличима; акценты только по смыслу; состояние не держится на одном цвете.
- [ ] Все тексты локализованы на 5 языков, числа — плейсхолдеры, у фиксированной ширины есть `maxChars`, `LocalizationLimitsTest` зелёный.
- [ ] Новые ключи добавлены в карты строк превью.
- [ ] Иконки без подписи с `contentDescription`; мелкие кнопки с тап-зоной 40–48dp.
- [ ] Экранные превью обновлены и отрендерены; поп-апы проверены на эмуляторе.
- [ ] Временные доски удалены, `git status` чистый от них, `git diff --stat` соответствует замыслу.
- [ ] Не проверенное глазами записано в `docs/BACKLOG.md`.
