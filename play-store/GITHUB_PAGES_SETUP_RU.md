# Как получить публичную ссылку на Privacy Policy через GitHub Pages

1. Убедитесь, что в репозитории есть папка `docs` из этого архива.
2. GitHub → **Settings → Pages**.
3. В разделе **Build and deployment** выберите **Deploy from a branch**.
4. Branch: `main`, Folder: `/docs`.
5. Нажмите **Save**.
6. GitHub покажет адрес сайта. Обычно он выглядит как:
   `https://<username>.github.io/<repository>/`
7. Для Google Play укажите ссылку:
   `https://<username>.github.io/<repository>/privacy.html`

Перед включением Pages замените `DEVELOPER_NAME` и `SUPPORT_EMAIL` в `docs/privacy.html` и `SUPPORT_EMAIL` в `docs/support.html`.
