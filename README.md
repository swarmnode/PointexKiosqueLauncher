# PointexKioskLauncher

Préparer une télécommande ou une caisse Android en mode kiosque Pointex **en 5 minutes**, sans avoir à debloater ni verrouiller l'appareil à la main.

## Le problème

Configurer un appareil Android pour qu'il n'affiche que les applications Pointex/Fiducial (suppression du compte Google, désactivation du Bureau/Récents, restrictions diverses, etc.) est un travail manuel long et fastidieux à refaire sur chaque appareil.

## La solution

### Android 12 et antérieur : QR code (sans PC)

1. Réinitialisation usine de l'appareil.
2. Sur l'écran d'accueil de l'assistant de configuration, taper 6 fois → le scanner de QR code s'ouvre.
3. Scanner le QR code de provisioning (mis à jour automatiquement à chaque release, voir [Releases](#releases)).
4. L'appareil télécharge l'APK, vérifie son empreinte, l'installe et le configure automatiquement en tant que **Device Owner**.
5. L'application se verrouille (mode kiosque) sur les applications Pointex/Fiducial autorisées — plus rien d'autre n'est accessible.

Aucun câble nécessaire sur le terrain.

### Android 13 et plus récent : provisioning USB

Depuis Android 13, Google bloque le provisioning Device Owner par QR code pour les applications qui ne sont pas sur sa liste blanche de DPC ("Can't set up device / Contact your IT admin"). Sur ces appareils, utiliser `tools/PrepaKiosquePointex-USB.bat` :

1. Réinitialisation usine de l'appareil.
2. Activer le débogage USB (Réglages → À propos du téléphone → taper 7 fois sur "Numéro de build" pour activer les options développeur, puis Réglages → Options pour les développeurs → Débogage USB).
3. Brancher l'appareil en USB sur un PC équipé de `curl`. Pour ADB, deux options : copier `adb.exe` + `AdbWinApi.dll` + `AdbWinUsbApi.dll` + `libwinpthread-1.dll` (depuis [platform-tools](https://developer.android.com/tools/releases/platform-tools)) dans le même dossier que le script — kit autonome, rien à installer — ou avoir [ADB](https://developer.android.com/tools/releases/platform-tools) dans le PATH.
4. Lancer `tools/PrepaKiosquePointex-USB.bat` : il télécharge la dernière version, l'installe et configure l'appareil en tant que Device Owner.
5. Au premier lancement de l'application, elle se verrouille (mode kiosque) comme ci-dessus.

## Afficher le QR code facilement

Deux petites applications **PrepaKiosquePointex** affichent uniquement le QR code de provisioning à jour, pour que l'admin puisse le montrer au technicien sans ouvrir de navigateur :

- `PrepaKiosquePointex.exe` (Windows)
- `PrepaKiosquePointex.apk` (téléphone Android du technicien)

## Releases

Chaque release (`vX.Y.Z`) contient :

- `PointexKiosqueLauncher.apk` — l'application kiosque (aussi publiée sous l'ancien nom `app-release.apk` le temps que les QR imprimés avant le renommage disparaissent)
- `provisioning_qr.png` — le QR code de provisioning à jour pour cette release
- `PrepaKiosquePointex.apk` / `PrepaKiosquePointex.exe` — les afficheurs de QR code

## Administration

Une fois l'appareil verrouillé, un appui long sur le coin en bas à droite de l'écran d'accueil ouvre le menu admin (protégé par PIN), permettant d'accéder aux paramètres système ou d'installer/désinstaller des applications Pointex via SFTP.
