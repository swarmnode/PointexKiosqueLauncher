#!/usr/bin/env python3
"""Generate a Device Owner provisioning QR code for PointexKioskLauncher.

On a factory-reset device, tapping 6 times on the welcome screen of the
Android setup wizard opens a QR code scanner. Scanning the QR code produced
by this script downloads the APK from the given URL, verifies its checksum,
installs it and provisions it as Device Owner -- entirely on-device, no
PC/adb required.

Re-run this script (and redistribute the QR code) after every release build,
since the checksum changes whenever the APK changes.

Usage:
    python tools/generate_provisioning_qr.py <apk_path> <download_url> [options]

Example:
    python tools/generate_provisioning_qr.py \\
        app/build/outputs/apk/release/app-release.apk \\
        https://updates.example.com/PointexKioskLauncher.apk \\
        --wifi-ssid "Pointex-Atelier" --wifi-password "********"
"""
import argparse
import base64
import hashlib
import json

import qrcode

ADMIN_COMPONENT = "com.pointex.kiosklauncher/com.pointex.kiosklauncher.admin.KioskAdminReceiver"


def sha256_base64(path: str) -> str:
    """SHA-256 of the file, web-safe base64 encoded (Android's expected format)."""
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            digest.update(chunk)
    return base64.urlsafe_b64encode(digest.digest()).decode("ascii")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("apk", help="Path to the release APK")
    parser.add_argument("url", help="HTTPS URL where the APK is hosted")
    parser.add_argument("--wifi-ssid", help="WiFi SSID to join during provisioning")
    parser.add_argument("--wifi-password", help="WiFi password (WPA/WPA2)")
    parser.add_argument("--admin-component", default=ADMIN_COMPONENT, help="DeviceAdminReceiver component name")
    parser.add_argument("--locale", default="fr_FR")
    parser.add_argument("--timezone", default="Europe/Paris")
    parser.add_argument("--output", default="provisioning_qr.png", help="Output PNG path")
    args = parser.parse_args()

    checksum = sha256_base64(args.apk)

    payload = {
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": args.admin_component,
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": args.url,
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": checksum,
        "android.app.extra.PROVISIONING_LOCALE": args.locale,
        "android.app.extra.PROVISIONING_TIME_ZONE": args.timezone,
        "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": True,
        "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": False,
    }

    if args.wifi_ssid:
        payload["android.app.extra.PROVISIONING_WIFI_SSID"] = args.wifi_ssid
        if args.wifi_password:
            payload["android.app.extra.PROVISIONING_WIFI_PASSWORD"] = args.wifi_password
            payload["android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE"] = "WPA"

    payload_json = json.dumps(payload)
    print(payload_json)
    print(f"\nChecksum SHA-256 (base64 url-safe): {checksum}")

    qrcode.make(payload_json).save(args.output)
    print(f"QR code enregistré dans : {args.output}")


if __name__ == "__main__":
    main()
