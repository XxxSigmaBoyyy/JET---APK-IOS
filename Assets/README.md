# App icon slot / Слот для иконки

Put your artwork here as `AppIcon-1024.png` (1024×1024, PNG, **no transparency**).
`scripts/sync_icon.sh` (also run by CI) copies it into `JET/Resources/Assets.xcassets/AppIcon.appiconset/`.
Without it the placeholder from `scripts/make_placeholder_icon.py` is used.

Положите сюда свою иконку `AppIcon-1024.png` (1024×1024, PNG, **без прозрачности**).
`scripts/sync_icon.sh` (его запускает и CI) копирует её в каталог ассетов. Если файла нет — используется заглушка.
