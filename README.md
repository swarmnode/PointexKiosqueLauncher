# PointexKioskLauncher

Préparer une tablette ou une caisse Android en mode kiosque Pointex **en 5 minutes**, sans avoir à debloater ni verrouiller l'appareil à la main.

## Le problème

Configurer un appareil Android pour qu'il n'affiche que les applications Pointex/Fiducial (suppression du compte Google, désactivation du Bureau/Récents, restrictions diverses, etc.) est un travail manuel long et fastidieux à refaire sur chaque appareil.

## La solution

1. Réinitialisation usine de l'appareil.
2. Sur l'écran d'accueil de l'assistant de configuration, taper 6 fois → le scanner de QR code s'ouvre.
3. Scanner le QR code de provisioning (mis à jour automatiquement à chaque release, voir [Releases](#releases)).
4. L'appareil télécharge l'APK, vérifie son empreinte, l'installe et le configure automatiquement en tant que **Device Owner**.
5. L'application se verrouille (mode kiosque) sur les applications Pointex/Fiducial autorisées — plus rien d'autre n'est accessible.

Aucun PC ni câble nécessaire sur le terrain.

## Afficher le QR code facilement

Deux petites applications **PrepaKiosquePointex** affichent uniquement le QR code de provisioning à jour, pour que l'admin puisse le montrer au technicien sans ouvrir de navigateur :

- `PrepaKiosquePointex.exe` (Windows)
- `PrepaKiosquePointex.apk` (téléphone Android du technicien)

## Releases

Chaque release (`vX.Y.Z`) contient :

- `app-release.apk` — l'application kiosque
- `provisioning_qr.png` — le QR code de provisioning à jour pour cette release
- `PrepaKiosquePointex.apk` / `PrepaKiosquePointex-win.zip` — les afficheurs de QR code

## Administration

Une fois l'appareil verrouillé, un appui long sur le coin en bas à droite de l'écran d'accueil ouvre le menu admin (protégé par PIN), permettant d'accéder aux paramètres système ou d'installer/désinstaller des applications Pointex via SFTP.
