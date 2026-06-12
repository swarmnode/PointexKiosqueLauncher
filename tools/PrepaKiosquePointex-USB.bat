@echo off
setlocal

rem Provisionne un appareil PointexKioskLauncher en Device Owner via USB.
rem A utiliser sur Android 13+ ou si le QR code de provisioning ne fonctionne
rem pas (Google bloque le provisioning DPC personnalise par QR sur Android 13+).
rem
rem Prerequis sur l'appareil :
rem   - Reinitialisation usine
rem   - Options developpeur activees (Reglages > A propos > taper 7x sur "Numero de build")
rem   - Debogage USB active, cable USB branche, autoriser l'ordinateur
rem
rem Prerequis sur le PC : aucun si le kit est complet. adb est utilise depuis
rem le dossier de ce script s'il s'y trouve (adb.exe, AdbWinApi.dll,
rem AdbWinUsbApi.dll, libwinpthread-1.dll), sinon depuis le PATH. De meme,
rem l'APK PointexKiosqueLauncher.apk pose a cote du script est installe
rem directement (kit hors-ligne) ; sinon la derniere version est telechargee
rem avec curl.

set "APK_URL=https://github.com/swarmnode/PointexKiosqueLauncher/releases/latest/download/PointexKiosqueLauncher.apk"
set "ADMIN_COMPONENT=com.pointex.kiosklauncher/com.pointex.kiosklauncher.admin.KioskAdminReceiver"

set "ADB=%~dp0adb.exe"
if not exist "%ADB%" set "ADB=adb"

"%ADB%" version >nul 2>nul
if errorlevel 1 (
    echo [ERREUR] adb introuvable. Placez adb.exe et ses DLL a cote de ce script
    echo ou installez Android platform-tools et ajoutez-le au PATH.
    pause
    exit /b 1
)

echo Verification de l'appareil...
"%ADB%" get-state >nul 2>nul
if errorlevel 1 (
    echo [ERREUR] Aucun appareil detecte. Verifiez le cable USB et le debogage USB.
    pause
    exit /b 1
)

if exist "%~dp0PointexKiosqueLauncher.apk" (
    echo Utilisation de l'APK fourni avec le kit.
    set "APK_FILE=%~dp0PointexKiosqueLauncher.apk"
) else (
    echo Telechargement de la derniere version de PointexKioskLauncher...
    set "APK_FILE=%TEMP%\PointexKiosqueLauncher.apk"
    curl -fL -o "%TEMP%\PointexKiosqueLauncher.apk" "%APK_URL%"
    if errorlevel 1 (
        echo [ERREUR] Echec du telechargement.
        pause
        exit /b 1
    )
)

echo Installation de l'application...
"%ADB%" install -r "%APK_FILE%"
if errorlevel 1 (
    echo [ERREUR] Echec de l'installation.
    pause
    exit /b 1
)

echo Configuration en tant que proprietaire de l'appareil (Device Owner)...
"%ADB%" shell dpm set-device-owner %ADMIN_COMPONENT%
if errorlevel 1 (
    echo.
    echo [ERREUR] Echec de la configuration Device Owner.
    echo Verifiez qu'aucun compte Google n'est configure sur l'appareil
    echo et qu'aucune autre application n'est deja proprietaire.
    pause
    exit /b 1
)

echo.
echo Provisioning termine avec succes !
echo L'appareil va se verrouiller en mode kiosque au prochain lancement de l'application.
pause
