# JET for iOS

**[Русский](#русский) · [English](#english)**

---

## Русский

JET — оболочка для iPhone (iOS 17+), которая принимает Android-файлы `.apk` и закрепляет их на домашнем экране как ярлыки. Интерфейс в стиле «стекла»: размытые материалы, полупрозрачные карточки, светлая небесно-голубая тема с облаками и фирменная иконка (белый пассажирский самолёт и надпись JET на ярко-синем фоне, `Assets/AppIcon-1024.png`).

### Что уже есть
- **Главный экран:** сетка ярлыков (иконка + название). Если ярлыков ещё нет, показывается стеклянная карточка-заглушка.
- **«+» / импорт:** выбор одного или нескольких `.apk` из «Файлов». JET также открывает `.apk` через меню «Поделиться» → «Открыть в JET». Дальше идёт конвейер импорта:
  1. копирование в песочницу;
  2. проверка, что это APK (`AndroidManifest.xml`), подсчёт dex-файлов и ABI нативных библиотек, извлечение иконки;
  3. вызов `jet_translate_apk` из **JetBridge**;
  4. сборка папки приложения и добавление ярлыка.
- **Запуск:** нажатие на ярлык открывает приложение на весь экран внутри `JetHostView` (это `UIView`, касания передаются в мост).
- **Настройки:** «О программе» (версия, версия моста, статус рантайма), размеры хранилища, «Очистить кэш», «Удалить все ярлыки».

> **Честно о статусе (Milestone 1).** В `.ipa` теперь встроен настоящий рантайм JET (заглушки больше нет): AOT-скомпилированный код приложения (DEX → C, без JIT — на iOS он запрещён), нативный движок Android-вью (инфляция layout-ов, measure/layout по правилам AOSP: LinearLayout с весами, TableLayout/TableRow, селекторы состояний, Toast) и отрисовка через CoreGraphics в `JetSurfaceView`; касания (нажатие, клик, долгое нажатие) доходят до слушателей приложения. Пока поддерживается **одно** открытое приложение, скомпилированное в сборку: **Heading Calculator 1.3** (F-Droid, GPL-3.0, `Modules/heading/`). Импортируйте его APK с F-Droid — откроется его настоящий первый экран, и он реагирует на нажатия. Любой другой APK импортируется, но помечается «нет готового модуля»: для произвольных APK нужен интерпретатор DEX (следующий этап). На физическом iPhone сборка ещё не проверялась — проверены хост-тесты и симулятор в CI.

### Рантайм JET
- `Runtime/` — рантайм (jetrt.h, rt.c, kernel.c — ядро java.lang + движок Android-вью) и мост C-API `jet_bridge_aot.c`; компилируется прямо в таргет Xcode.
- `Modules/<name>/` — модули приложений, сгенерированные `tools/aot/gen_module.sh` **только из открытых APK** (см. NOTICE модуля). Сами APK в репозиторий не попадают.
- `scripts/test_runtime.sh` — контракт-тест C-API и тест отрисовки/касаний на Linux/macOS (APK скачивается с F-Droid с проверкой sha256).
- Контракт (функции, коды возврата, потоки, display list): [`docs/RUNTIME_LINK.md`](docs/RUNTIME_LINK.md).

### Установка через E-SIGN (без Mac)
1. Откройте вкладку **Actions** этого репозитория → workflow **iOS IPA (JET runtime)** → последний успешный запуск → артефакт **JET-…-runtime-unsigned-ipa** (zip с файлом `.ipa`). Для тегов `v*` тот же `.ipa` прикрепляется к **Releases**.
2. Скачайте `.ipa` на iPhone (Safari → «Файлы»). Если скачался zip, распакуйте его в «Файлах».
3. В **E-SIGN**: «Файлы»/«Импорт» → выберите `JET-…-unsigned.ipa`.
4. Нажмите на него → **Подписать**. Выберите свой сертификат (`.p12` + `.mobileprovision`, импортированные в E-SIGN) → подпишите → **Установить**.
5. Если iOS попросит, разрешите профиль в **Настройки → Основные → VPN и управление устройством**. На iOS 16+ может понадобиться включить **Режим разработчика**.

Тот же `.ipa` подходит для **AltStore** и **Sideloadly**: они подписывают его вашим Apple ID на своей стороне.

### Как работает CI
Файл `.github/workflows/ios-ipa.yml` запускается на `macos-14` при push в `main`, pull request, теге `v*` или вручную.

Шаги по порядку:
1. Проверка, что в репозиторий не попали `.apk`/`.aab`/`.xapk`/`.apks`/`.obb`. Если попали, сборка падает.
2. Выбор Xcode 15.x.
3. `brew install xcodegen`.
4. `scripts/sync_icon.sh` — берёт вашу иконку из `Assets/`, если она есть.
5. `xcodegen generate` создаёт `JET.xcodeproj` из `project.yml`.
6. `plutil -lint Info.plist`.
7. Сборка `xcodebuild -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO` — Release, arm64, **без подписи**.
8. `scripts/package_ipa.sh` упаковывает `Payload/JET.app` в `JET-<версия>-unsigned.ipa`.
9. Загрузка артефакта; на тегах — ещё и в Release.

**Секреты не нужны.** Неподписанная сборка не использует ни сертификаты, ни Apple ID: подписывает E-SIGN / AltStore / Sideloadly. Позже можно добавить отдельный подписанный job с секретами (например `APPLE_ID`, сертификат, профиль). Он необязателен, и в текущем workflow его нет.

### Локальная сборка (Mac)
```bash
brew install xcodegen
xcodegen generate
open JET.xcodeproj          # своя команда в Signing & Capabilities → запуск на iPhone (dev/ad-hoc)
# или без подписи, как в CI:
xcodebuild -project JET.xcodeproj -scheme JET -configuration Release -destination 'generic/platform=iOS' \
  -derivedDataPath build/DerivedData CODE_SIGNING_ALLOWED=NO build
scripts/package_ipa.sh build/DerivedData/Build/Products/Release-iphoneos/JET.app dist/JET-unsigned.ipa
```

### Иконка
Положите `Assets/AppIcon-1024.png` (1024×1024, без прозрачности). Если файла нет, используется заглушка, нарисованная `scripts/make_placeholder_icon.py`.

### ⚠️ APK никогда не коммитятся
**Закрытые APK никогда не коммитятся.** Нельзя добавлять в этот репозиторий **никакие** `.apk`/`.aab`/`.xapk`/`.apks`/`.obb`, особенно закрытые/проприетарные. Их распространение нарушает права авторов. `.gitignore` их исключает, а CI падает, если такой файл всё же попал в git. Импортируйте APK только на своём устройстве и только те, которыми вы вправе пользоваться.

### Структура
```
project.yml                     XcodeGen (iPhone, iOS 17, без подписи / ad-hoc)
.github/workflows/ios-ipa.yml   CI: неподписанный .ipa
JET/App                         точка входа SwiftUI
JET/Views                       главный экран, плитки, импорт, хост-экран, настройки
JET/Theme                       стеклянные стили, фон, силуэт истребителя
JET/Models                      ярлыки и хранилище
JET/Pipeline                    ZIP-чтение, инспекция APK, конвейер импорта
JET/Bridge                      C-API JetBridge + Swift-обёртка
JET/Debug                       смоук-тест по аргументам запуска (CI-симулятор)
Runtime/                        AOT-рантайм, движок Android-вью, мост C-API, тесты
Modules/                        AOT-модули открытых приложений (сгенерированный C)
tools/aot/                      DEX→C (Soot Jimple → C), ресурсы → C, gen_module.sh
docs/RUNTIME_LINK.md            контракт C-API (ABI v2)
JET/Resources                   Info.plist, Assets.xcassets
scripts/                        иконка, упаковка .ipa, guard репозитория, тест рантайма
Assets/                         слот для вашей иконки
legacy/                         старый шаблон «одно APK → одно приложение» (в сборку не входит)
```

---

## English

JET is an iPhone shell (iOS 17+) that takes Android `.apk` files and pins them to its home screen as shortcuts. The UI is glass-style: blur materials, translucent cards, a light sky-blue theme with drifting clouds, and the JET icon (white passenger jet and JET lettering on bright blue, `Assets/AppIcon-1024.png`), which also appears inside the app.

### What's in it
- **Home:** a grid of shortcuts (icon + name). With no shortcuts yet, a glass empty-state card is shown instead.
- **"+" / Import:** pick one or more `.apk` files from Files. JET also opens `.apk` files from the share sheet via "Open in JET". The import pipeline then runs:
  1. copy the file into the sandbox;
  2. check that it is an APK (`AndroidManifest.xml`), count dex files and native-library ABIs, extract the launcher icon;
  3. call `jet_translate_apk` in **JetBridge**;
  4. assemble the app folder and add the shortcut.
- **Launch:** tapping a shortcut opens the app fullscreen inside `JetHostView` (a `UIView`; touches are forwarded to the bridge).
- **Settings:** About (version, bridge version, runtime status), storage sizes, Clear cache, Remove all shortcuts.

> **Status, honestly (Milestone 1).** The `.ipa` now contains the real JET runtime (the stub is gone). It has three parts:
> - App code compiled ahead of time (DEX → C, no JIT, which iOS forbids).
> - A native Android view engine: layout inflation; AOSP measure/layout for LinearLayout with weights and for TableLayout/TableRow; state-list selectors; Toast.
> - CoreGraphics rendering into `JetSurfaceView`. Touches (press, click, long press) reach the app's own listeners.
>
> Right now exactly **one** open-source app is compiled in: **Heading Calculator 1.3** (F-Droid, GPL-3.0, `Modules/heading/`). Import its APK from F-Droid and its real first screen opens and responds to taps. Any other APK imports but is marked "No prebuilt module"; arbitrary APKs need a DEX interpreter, which is the next milestone. Not yet verified on a physical iPhone: it is verified by host tests and by the CI simulator smoke test.

### JET runtime
- `Runtime/` holds the runtime and the bridge. The runtime is jetrt.h, rt.c and kernel.c (java.lang core plus the Android view engine). The C-API bridge is `jet_bridge_aot.c`. All of it is compiled straight into the Xcode target.
- `Modules/<name>/` holds the app modules. `tools/aot/gen_module.sh` generates them, **from open-source APKs only** (see each module's NOTICE). APKs themselves are never committed.
- `scripts/test_runtime.sh` runs the C-API contract test and a render/touch test on Linux or macOS. It downloads the APK from F-Droid and checks its sha256.
- The contract (functions, return codes, threading, display list) is in [`docs/RUNTIME_LINK.md`](docs/RUNTIME_LINK.md).

### Install with E-SIGN (no Mac needed)
1. Open this repo's **Actions** tab → **iOS IPA (JET runtime)** → the latest green run → artifact **JET-…-runtime-unsigned-ipa** (a zip containing the `.ipa`). For `v*` tags the same `.ipa` is attached to **Releases**.
2. Download the `.ipa` on your iPhone (Safari → Files). If you got a zip, unzip it in Files.
3. In **E-SIGN**: Files/Import → choose `JET-…-unsigned.ipa`.
4. Tap it → **Sign**. Pick your certificate (the `.p12` + `.mobileprovision` imported into E-SIGN) → sign → **Install**.
5. If iOS asks, trust the profile under **Settings → General → VPN & Device Management**. On iOS 16+ you may need to turn on **Developer Mode**.

The same `.ipa` works with **AltStore** and **Sideloadly**, which sign it with your Apple ID on their side.

### How CI works
`.github/workflows/ios-ipa.yml` runs on `macos-14` on a push to `main`, a pull request, a `v*` tag, or a manual run.

The steps, in order:
1. Check that no `.apk`/`.aab`/`.xapk`/`.apks`/`.obb` is tracked. If one is, the build fails.
2. Select Xcode 15.x.
3. `brew install xcodegen`.
4. `scripts/sync_icon.sh` uses your icon from `Assets/` if there is one.
5. `xcodegen generate` creates `JET.xcodeproj` from `project.yml`.
6. `plutil -lint Info.plist`.
7. `xcodebuild -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO` builds Release for arm64 with **no signing**.
8. `scripts/package_ipa.sh` zips `Payload/JET.app` into `JET-<version>-unsigned.ipa`.
9. Upload the artifact, and on tags also attach it to the Release.

**No secrets are needed.** The unsigned build uses no certificates and no Apple ID; E-SIGN, AltStore or Sideloadly does the signing. A separate signed job with secrets (e.g. `APPLE_ID`, a certificate, a profile) can be added later. It's optional and isn't part of the current workflow.

### Local build (Mac)
```bash
brew install xcodegen
xcodegen generate
open JET.xcodeproj          # set your team under Signing & Capabilities → run on iPhone (dev/ad-hoc)
# or unsigned, exactly like CI:
xcodebuild -project JET.xcodeproj -scheme JET -configuration Release -destination 'generic/platform=iOS' \
  -derivedDataPath build/DerivedData CODE_SIGNING_ALLOWED=NO build
scripts/package_ipa.sh build/DerivedData/Build/Products/Release-iphoneos/JET.app dist/JET-unsigned.ipa
```

### App icon
Drop `Assets/AppIcon-1024.png` (1024×1024, no alpha). Without it, the placeholder drawn by `scripts/make_placeholder_icon.py` is used.

### ⚠️ Never commit APKs
**Closed APKs must never be committed.** Do **not** add any `.apk`/`.aab`/`.xapk`/`.apks`/`.obb` to this repository, and especially not closed or proprietary ones. Redistributing them violates their authors' rights. `.gitignore` excludes them, and CI fails if one is tracked anyway. Import APKs only on your own device, and only apps you have the right to use.

### Layout
```
project.yml                     XcodeGen (iPhone, iOS 17, unsigned / ad-hoc capable)
.github/workflows/ios-ipa.yml   CI: unsigned .ipa
JET/App                         SwiftUI entry point
JET/Views                       home, tiles, import progress, host screen, settings
JET/Theme                       glass styles, aurora background, fighter-jet glyph
JET/Models                      shortcuts + on-disk store
JET/Pipeline                    ZIP reader, APK inspector, import pipeline
JET/Bridge                      JetBridge C API + Swift wrapper
JET/Debug                       launch-argument smoke test (CI simulator)
Runtime/                        AOT runtime, Android view engine, C API bridge, tests
Modules/                        AOT modules of open-source apps (generated C)
tools/aot/                      DEX->C (Soot Jimple -> C), resources -> C, gen_module.sh
docs/RUNTIME_LINK.md            C API contract (ABI v2)
JET/Resources                   Info.plist, Assets.xcassets
scripts/                        icon, .ipa packaging, repository guard, runtime test
Assets/                         slot for your icon artwork
legacy/                         old one-APK-per-app template (not part of the build)
```
