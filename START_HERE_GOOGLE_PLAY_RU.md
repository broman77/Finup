# SaldoNest 1.0 — с чего начать публикацию в Google Play

Проект технически подготовлен для первого релиза:
- `targetSdk = 36`;
- `versionName = 1.0`;
- `versionCode = 13`;
- package/applicationId: `com.ivan.finflow` (оставлен прежним, чтобы не ломать обновление локальной установленной версии);
- обычный APK: `.github/workflows/build-apk.yml`;
- подписанный AAB: `.github/workflows/build-play-aab.yml`;
- Google Play assets: папка `play-store`;
- сайт Privacy Policy/Support: папка `docs`.

## 1. Сначала заполните личные поля
В `docs/privacy.html`, `docs/support.html` и `play-store/LEGAL_FIELDS_TO_FILL.txt` замените:
- `DEVELOPER_NAME`;
- `SUPPORT_EMAIL`.

Аккаунт Google Play Developer, платеж и проверку личности должен оформлять взрослый владелец аккаунта (18+).

## 2. Загрузите проект на GitHub
Загрузите содержимое папки `FinFlowAndroid` в репозиторий. Проверьте наличие:
- `.github/workflows/build-apk.yml`
- `.github/workflows/build-play-aab.yml`
- `app`
- `docs`
- `play-store`

## 3. Включите GitHub Pages
Инструкция: `play-store/GITHUB_PAGES_SETUP_RU.md`.
После включения получите публичную ссылку на `privacy.html`.

## 4. Создайте upload key
Взрослый владелец запускает:
`play-store/signing/GENERATE_UPLOAD_KEY_WINDOWS.ps1`

Затем добавляет 4 GitHub Secrets:
- `ANDROID_KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## 5. Соберите Google Play AAB
GitHub → Actions → **Build Google Play AAB** → Run workflow.
Скачайте artifact **SaldoNest-Google-Play-AAB**. Внутри будет `app-release.aab`.

## 6. Создайте приложение в Play Console
- Название: `SaldoNest — учёт финансов`
- Тип: App
- Категория: Finance
- Бесплатное: Yes

Тексты лежат в `play-store/STORE_LISTING_RU.md` и `STORE_LISTING_EN.md`.

## 7. Загрузите графику
- `play-store/app-icon-512.png`
- `play-store/feature-graphic-1024x500.png`
- 4 актуальных скриншота текущей версии приложения.

Инструкция по скриншотам: `play-store/SCREENSHOT_CAPTURE_GUIDE_RU.md`.

## 8. Заполните App content
Черновик ответов: `play-store/PLAY_CONSOLE_ANSWERS_RU.md`.
Особенно проверьте:
- Data safety;
- Financial features declaration;
- Content rating;
- Target audience;
- Ads;
- App access.

## 9. Тестирование
Для новых личных аккаунтов Google Play, созданных после 13 ноября 2023 г., требуется закрытое тестирование минимум с 12 тестировщиками, подключёнными непрерывно не менее 14 дней, прежде чем можно запросить production access.

## 10. Первый Production release
После выполнения требований тестирования:
- Production → Create release;
- загрузите `app-release.aab`;
- release name: `1.0`;
- release notes: `Первый публичный выпуск SaldoNest.`
- отправьте изменения на review.

## Обновления после публикации
Для каждой новой версии повышайте `versionCode`. `applicationId` после первого релиза менять нельзя.
