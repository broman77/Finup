# Run in PowerShell on a computer with Java/JDK installed.
$ErrorActionPreference = "Stop"
$alias = "upload"
$keystore = "saldonest-upload.jks"
Write-Host "Creating Google Play upload key. Keep this file private and NEVER commit it to GitHub." -ForegroundColor Yellow
$storePass = Read-Host "Create a strong keystore password"
$keyPass = Read-Host "Create a strong key password (can be the same)"
keytool -genkeypair -v -keystore $keystore -alias $alias -keyalg RSA -keysize 4096 -validity 10000 -storepass $storePass -keypass $keyPass -dname "CN=SaldoNest Upload, OU=Android, O=SaldoNest, C=DE"
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $keystore))
$b64 = [Convert]::ToBase64String($bytes)
Write-Host "`nAdd these Repository secrets in GitHub Settings -> Secrets and variables -> Actions:" -ForegroundColor Green
Write-Host "ANDROID_KEYSTORE_BASE64 = $b64"
Write-Host "KEYSTORE_PASSWORD = $storePass"
Write-Host "KEY_ALIAS = $alias"
Write-Host "KEY_PASSWORD = $keyPass"
Write-Host "`nBack up $keystore and the passwords offline." -ForegroundColor Yellow
