# Подготовка к Google Play — версия 2.1

Технические параметры проекта:
- `targetSdk = 36` (Android 16)
- `applicationId = com.ivan.finflow`
- Google Play icon: `play-store/app-icon-512-transparent.png`
- обычный APK: `.github/workflows/build-apk.yml`
- подписанный AAB для Google Play: `.github/workflows/build-play-aab.yml`

## Важно про обновления
После первой публикации не меняйте `applicationId`. Для каждого обновления повышайте `versionCode`.

## Секреты GitHub для AAB
Workflow `Build Google Play AAB` ожидает четыре Repository Secrets:
- `ANDROID_KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Upload keystore нужно создать один раз и хранить безопасно. Один и тот же upload key понадобится для последующих обновлений.
