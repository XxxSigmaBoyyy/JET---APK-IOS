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

> **Честно о статусе.** По умолчанию (и в CI без настроенного рантайма) JetBridge — заглушка (`JET/Bridge/JetBridgeStub.c`): APK проверяется и закрепляется, но Android-код **не исполняется**, экран приложения показывает «Runtime not linked». Подключаемый рантайм JET (`libjetruntime.a`, см. ниже) сейчас умеет запускать **только** один заранее AOT-скомпилированный открытый APK (демо — Heading Calculator); любой другой APK честно получает статус «не поддерживается». На iOS нельзя JIT и загружать код, поэтому код приложения компилируется заранее и линкуется в `.ipa`.

### Рантайм JET (необязательно)
- `scripts/build_host_lib.sh --proto ../proto [--install]` — собирает `dist/JetRuntime` (arm64-apple-ios `libjetruntime.a`, `JetBridge.h`, `manifest.json`) и прогоняет контракт-тесты C-API на Linux.
- Локально: `--install`, затем `JET_RUNTIME=YES xcodegen generate`. В CI: переменные репозитория `JET_RUNTIME_URL` (https) и `JET_RUNTIME_SHA256`; `scripts/fetch_runtime.sh` проверяет архив. Без них собирается заглушка.
- Контракт (функции, коды возврата, потоки, формат бандла, ограничения): [`docs/RUNTIME_LINK.md`](docs/RUNTIME_LINK.md).
- `Vendor/JetRuntime/` и `dist/` **никогда не коммитятся**; модули собираются только из открытых APK.

### Установка через E-SIGN (без Mac)
1. Откройте вкладку **Actions** этого репозитория → workflow **iOS unsigned IPA** → последний успешный запуск → артефакт **JET-…-unsigned-ipa** (zip с файлом `.ipa`). Для тегов `v*` тот же `.ipa` прикрепляется к **Releases**.
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
JET/Bridge                      C-API JetBridge + заглушка + Swift-обёртка
Runtime/                        реализация C-API на AOT-рантайме + контракт-тест
docs/RUNTIME_LINK.md            контракт C-API и порядок линковки
JET/Resources                   Info.plist, Assets.xcassets
scripts/                        иконка, упаковка .ipa, сборка/загрузка рантайма
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

> **Status, honestly.** By default (and in CI without a configured runtime) JetBridge is a stub (`JET/Bridge/JetBridgeStub.c`): APKs are inspected and pinned, but Android code **does not run**; the app screen shows "Runtime not linked". The optional JET runtime (`libjetruntime.a`, below) currently runs **only** one prebuilt AOT-compiled open-source APK (demo: Heading Calculator); any other APK is reported as unsupported. iOS allows no JIT and no code loading, so app code is compiled ahead of time and linked into the `.ipa`.

### JET runtime (optional)
- `scripts/build_host_lib.sh --proto ../proto [--install]` builds `dist/JetRuntime` (arm64-apple-ios `libjetruntime.a`, `JetBridge.h`, `manifest.json`) and runs the C API contract tests on Linux.
- Locally: `--install`, then `JET_RUNTIME=YES xcodegen generate`. In CI: repository variables `JET_RUNTIME_URL` (https) and `JET_RUNTIME_SHA256`; `scripts/fetch_runtime.sh` verifies the archive. Without them the stub is built.
- Contract (functions, return codes, threading, bundle format, limits): [`docs/RUNTIME_LINK.md`](docs/RUNTIME_LINK.md).
- `Vendor/JetRuntime/` and `dist/` are **never committed**; modules are built from open-source APKs only.

### Install with E-SIGN (no Mac needed)
1. Open this repo's **Actions** tab → **iOS unsigned IPA** → the latest green run → artifact **JET-…-unsigned-ipa** (a zip containing the `.ipa`). For `v*` tags the same `.ipa` is attached to **Releases**.
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
JET/Bridge                      JetBridge C API + stub + Swift wrapper
Runtime/                        C API implementation on the AOT runtime + contract test
docs/RUNTIME_LINK.md            C API contract and link recipe
JET/Resources                   Info.plist, Assets.xcassets
scripts/                        icon, .ipa packaging, runtime build/fetch
Assets/                         slot for your icon artwork
legacy/                         old one-APK-per-app template (not part of the build)
```
