# Подпись AAB для Google Play

Upload key — это секретный ключ, которым подписывается AAB перед загрузкой в Play Console. Google Play App Signing затем подписывает APK для пользователей своим app signing key.

## Самый безопасный вариант
Пусть взрослый владелец Google Play Developer аккаунта запустит `GENERATE_UPLOAD_KEY_WINDOWS.ps1` на своём компьютере. Скрипт создаст `saldonest-upload.jks` и покажет четыре значения для GitHub Secrets.

Никогда не загружайте `.jks`, пароли или Base64-строку в обычные файлы репозитория.

После добавления секретов откройте GitHub → Actions → **Build Google Play AAB** → Run workflow. Готовый `app-release.aab` появится в Artifacts.
