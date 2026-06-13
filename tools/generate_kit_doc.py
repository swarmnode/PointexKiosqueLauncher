#!/usr/bin/env python3
"""Génère la notice PDF du kit technicien PrepaKiosquePointex.

Usage :
    pip install fpdf2
    python tools/generate_kit_doc.py [sortie.pdf]

Par défaut, écrit `Notice-PrepaKiosquePointex.pdf` dans le dossier du script.
La police Segoe UI de Windows est embarquée pour les accents français.
"""

import sys
from pathlib import Path

from fpdf import FPDF

FONT_DIR = Path("C:/Windows/Fonts")

ACCENT = (0, 90, 160)
TEXT = (30, 30, 30)
MUTED = (105, 105, 105)


class NoticePdf(FPDF):
    def header(self):
        if self.page_no() == 1:
            return
        self.set_font("segoe", "", 8)
        self.set_text_color(*MUTED)
        self.cell(0, 6, "Kit PrepaKiosquePointex - notice technicien", align="R")
        self.ln(10)

    def footer(self):
        self.set_y(-15)
        self.set_font("segoe", "", 8)
        self.set_text_color(*MUTED)
        self.cell(0, 10, f"Page {self.page_no()}/{{nb}}", align="C")

    def title_block(self, title, subtitle):
        self.set_font("segoe", "B", 22)
        self.set_text_color(*ACCENT)
        self.multi_cell(0, 10, title, new_x="LMARGIN", new_y="NEXT")
        self.set_font("segoe", "", 11)
        self.set_text_color(*MUTED)
        self.multi_cell(0, 6, subtitle, new_x="LMARGIN", new_y="NEXT")
        self.ln(4)

    def section(self, text):
        self.ln(3)
        self.set_font("segoe", "B", 14)
        self.set_text_color(*ACCENT)
        self.multi_cell(0, 8, text, new_x="LMARGIN", new_y="NEXT")
        self.ln(1)

    def para(self, text):
        self.set_font("segoe", "", 10.5)
        self.set_text_color(*TEXT)
        self.multi_cell(0, 5.5, text, new_x="LMARGIN", new_y="NEXT")
        self.ln(1.5)

    def bullet(self, text, indent=6):
        self.set_font("segoe", "", 10.5)
        self.set_text_color(*TEXT)
        self.set_x(self.l_margin + indent)
        self.multi_cell(0, 5.5, f"•  {text}", new_x="LMARGIN", new_y="NEXT")
        self.ln(0.5)

    def step(self, number, text):
        self.set_font("segoe", "B", 10.5)
        self.set_text_color(*ACCENT)
        self.set_x(self.l_margin + 2)
        self.cell(8, 5.5, f"{number}.")
        self.set_font("segoe", "", 10.5)
        self.set_text_color(*TEXT)
        self.multi_cell(0, 5.5, text, new_x="LMARGIN", new_y="NEXT")
        self.ln(1)


def build(output: Path) -> None:
    pdf = NoticePdf(format="A4")
    pdf.set_margins(18, 16, 18)
    pdf.set_auto_page_break(True, margin=18)
    pdf.add_font("segoe", "", FONT_DIR / "segoeui.ttf")
    pdf.add_font("segoe", "B", FONT_DIR / "segoeuib.ttf")
    pdf.add_page()

    pdf.title_block(
        "Kit PrepaKiosquePointex",
        "Mise en service d'un appareil Android en kiosque Pointex - notice technicien",
    )

    pdf.section("Contenu du kit")
    pdf.bullet("PrepaKiosquePointex-USB.bat : script de mise en service par câble USB (Android 13 et plus, ou si le QR code échoue).")
    pdf.bullet("adb.exe + AdbWinApi.dll + AdbWinUsbApi.dll + libwinpthread-1.dll : outil Android embarqué, aucune installation nécessaire sur le PC (NOTICE.txt : licences).")
    pdf.bullet("PointexKiosqueLauncher.apk : l'application kiosque Pointex installée par le script.")
    pdf.bullet("PrepaKiosquePointex.exe (Windows) et PrepaKiosquePointex.apk (Android) : affichent le QR code de provisioning à jour (connexion internet requise).")
    pdf.para("Copiez l'intégralité du dossier sur la clé USB : le script utilise les fichiers posés à côté de lui.")

    pdf.section("Méthode 1 - QR code (Android 12 et moins)")
    pdf.step(1, "Réinitialisez l'appareil aux paramètres d'usine.")
    pdf.step(2, "Sur l'écran de bienvenue de la configuration initiale, tapez 6 fois au même endroit : le scanner de QR code s'ouvre.")
    pdf.step(3, "Affichez le QR avec PrepaKiosquePointex.exe (PC) ou PrepaKiosquePointex.apk (autre appareil Android), puis scannez-le.")
    pdf.step(4, "L'appareil se connecte au Wi-Fi de l'atelier (intégré au QR), télécharge l'application et se configure seul en Device Owner.")
    pdf.para("Si l'appareil affiche « Impossible de configurer l'appareil », il s'agit probablement d'un Android 13 ou plus : utilisez la méthode 2.")

    pdf.section("Méthode 2 - Câble USB (Android 13 et plus)")
    pdf.step(1, "Réinitialisez l'appareil aux paramètres d'usine et passez la configuration initiale SANS ajouter de compte Google.")
    pdf.step(2, "Activez les options développeur : Réglages > À propos du téléphone > tapez 7 fois sur « Numéro de build ».")
    pdf.step(3, "Activez « Débogage USB » dans Réglages > Options pour les développeurs.")
    pdf.step(4, "Branchez l'appareil au PC en USB et acceptez « Autoriser le débogage USB » sur l'appareil.")
    pdf.step(5, "Double-cliquez sur PrepaKiosquePointex-USB.bat : il installe l'application du kit (ou télécharge la dernière version si l'APK n'est pas sur la clé) et configure l'appareil en Device Owner.")

    pdf.section("Si le Device Owner est impossible (mode kiosque limité)")
    pdf.para("Certains appareils ne peuvent pas être configurés en Device Owner (compte Google indélébile, autre application propriétaire). L'application propose alors un mode dégradé :")
    pdf.step(1, "Sur l'écran « Configuration requise », touchez « Définir un code de verrouillage de l'appareil » et créez un code connu du technicien uniquement : sortir du kiosque exigera ce code.")
    pdf.step(2, "IMPORTANT - toujours dans Sécurité, touchez la roue dentée à côté de « Verrouillage de l'écran » puis réglez « Verrouiller automatiquement » sur « Jamais » et désactivez le verrouillage instantané par le bouton Marche/Arrêt. Sans cela, le code serait demandé à chaque réveil de l'écran ; ainsi il n'est demandé que pour sortir du kiosque (et une fois après un redémarrage complet).")
    pdf.step(3, "Touchez « Configurer en mode kiosque limité ».")
    pdf.step(4, "Acceptez la demande « Activer cette appli d'administration de l'appareil » : elle empêche la désinstallation de l'application Pointex par les voies normales.")
    pdf.step(5, "Acceptez la demande « Définir comme application d'accueil par défaut » : le bouton Accueil ramènera toujours au kiosque.")
    pdf.para("Pour désinstaller l'application lors d'une maintenance : Réglages > Sécurité > Applications d'administration de l'appareil, désactivez « Pointex » (un avertissement s'affiche), puis désinstallez normalement.")
    pdf.para("En mode limité, les installations d'applications demandent une confirmation à l'écran. La protection contre l'accès au système repose sur le code de verrouillage défini à l'étape 1.")
    pdf.step(6, "Installation/mise à jour des apps en mode limité : la première fois, Android demande d'autoriser ce lanceur à « installer des applications inconnues » (l'app ouvre directement le bon écran de réglages) ; activez l'autorisation puis réessayez. Une fois accordée, installations et mises à jour passent par une simple confirmation à l'écran. Mettre à jour ne nécessite pas de désinstaller l'ancienne version (les données sont conservées). Pour installer une version ANTÉRIEURE (retour arrière), désinstallez d'abord l'application via le bouton « Désinstaller » (les données seront perdues), puis installez la version voulue.")
    pdf.step(7, "Optionnel - protection d'accès renforcée : dans le menu administrateur, « Activer le service de protection » (accordez l'accessibilité à Pointex dans la liste), puis réglez « Protection d'accès » sur « Paramètres seulement » ou « toutes les apps ». Toute application protégée ouverte exigera alors le code administrateur.")

    pdf.section("Cas particulier : appareils Sunmi (et constructeurs avec Device Owner d'usine)")
    pdf.para("Les terminaux Sunmi embarquent déjà leur propre Device Owner (RemoteControl). Le mode Device Owner complet est donc IMPOSSIBLE : le script USB installe bien l'application, mais l'étape Device Owner se termine par « device owner is already set » — c'est NORMAL, ignorez cette erreur. L'appareil se configure en mode kiosque limité. Séquence validée sur Sunmi L2s PRO (Android 12) :")
    pdf.step(1, "Lancez Pointex Kiosk Launcher (depuis le launcher Sunmi). L'écran « Configuration requise » s'affiche.")
    pdf.step(2, "Touchez « Configurer en mode kiosque limité ».")
    pdf.step(3, "Acceptez « Activer cette appli d'administration de l'appareil » (empêche la désinstallation).")
    pdf.step(4, "Acceptez « Application d'accueil par défaut » et sélectionnez « Pointex Kiosk Launcher » : le bouton Accueil ramènera toujours au kiosque.")
    pdf.step(5, "Protection d'accès : appui long sur le coin inférieur droit de l'écran d'accueil, saisissez le code administrateur. Réglez « Protection d'accès » sur « toutes les apps » (ou « Paramètres seulement »), puis « Activer le service de protection » : dans la liste d'accessibilité, ouvrez « Pointex Kiosk Launcher », activez-le et touchez « Autoriser » (contrôle total). Toute application non autorisée ouverte exigera alors le code.")
    pdf.step(6, "Première installation/mise à jour d'une app Pointex : autorisez le lanceur à « installer des applications inconnues » quand l'écran de réglages s'ouvre, puis réessayez.")
    pdf.para("Désinstaller le kiosque pour maintenance : Réglages > Sécurité > Applications d'administration de l'appareil > désactivez « Pointex Kiosk Launcher » (un avertissement s'affiche), puis désinstallez. En mode limité il n'y a pas d'épinglage d'écran : aucune confirmation d'épinglage n'apparaît, et le code de verrouillage de l'appareil reste optionnel.")

    pdf.section("Après la mise en service")
    pdf.step(1, "Installez les applications Pointex : bouton « Installer une application Pointex » (serveur SFTP, identifiants pré-remplis).")
    pdf.step(2, "Accès administrateur : appui long dans le coin inférieur droit de l'écran d'accueil. L'écran affiche un nombre à 5 chiffres ; saisissez le code de réponse correspondant, calculé selon la règle interne (communiquée séparément aux techniciens). Le menu permet de gérer les applications, lancer une application installée, le Wi-Fi (IP fixe), la carte SIM et les Paramètres.")
    pdf.para("Après 5 codes erronés, la saisie est bloquée 30 secondes.")
    pdf.para("Au démarrage de l'appareil, si une seule application Pointex est installée, elle se lance automatiquement (une fois par démarrage : revenir à l'accueil du kiosque reste possible).")

    pdf.section("Dépannage")
    pdf.bullet("« adb introuvable » : les fichiers adb.exe et les 3 DLL doivent être dans le même dossier que le script.")
    pdf.bullet("« Aucun appareil détecté » : vérifiez le câble, le débogage USB et la fenêtre d'autorisation sur l'appareil. Sur certains modèles, installez le pilote USB du constructeur sur le PC.")
    pdf.bullet("« Échec de la configuration Device Owner » : un compte Google est encore présent sur l'appareil, ou une autre application est déjà propriétaire. Refaites une réinitialisation usine sans ajouter de compte.")
    pdf.bullet("Mise à jour du kit : re-téléchargez PointexKiosqueLauncher.apk depuis la page GitHub des releases (releases/latest) et remplacez-le sur la clé.")

    pdf.output(str(output))
    print(f"Notice generee : {output}")


if __name__ == "__main__":
    default = Path(__file__).parent / "Notice-PrepaKiosquePointex.pdf"
    build(Path(sys.argv[1]) if len(sys.argv) > 1 else default)
