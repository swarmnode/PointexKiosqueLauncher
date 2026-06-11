#!/usr/bin/env python3
"""Generate the .ico icon for qr-display-winforms.

Draws the same "QR glyph on Pointex blue" design used for the
qr-display-android adaptive icon (see
qr-display-android/src/main/res/drawable/ic_launcher_foreground.xml),
rasterized at high resolution and saved as a multi-size .ico.

Usage:
    python tools/generate_app_icon.py
"""
from PIL import Image, ImageDraw

POINTEX_BLUE = "#1565C0"
WHITE = "#FFFFFF"

# Source canvas is drawn at 4x the 108x108 vector viewport for crisp downsizing.
SCALE = 4
SIZE = 108 * SCALE


def rect(draw: ImageDraw.ImageDraw, x: float, y: float, w: float, h: float, fill: str) -> None:
    x, y, w, h = x * SCALE, y * SCALE, w * SCALE, h * SCALE
    draw.rectangle([x, y, x + w, y + h], fill=fill)


def main() -> None:
    image = Image.new("RGB", (SIZE, SIZE), POINTEX_BLUE)
    draw = ImageDraw.Draw(image)

    # Three QR finder-pattern corners (top-left, top-right, bottom-left).
    for cx, cy in ((19, 19), (65, 19), (19, 65)):
        rect(draw, cx, cy, 24, 24, WHITE)
        rect(draw, cx + 4, cy + 4, 16, 16, POINTEX_BLUE)
        rect(draw, cx + 8, cy + 8, 8, 8, WHITE)

    # Bottom-right data modules.
    for dx, dy in ((65, 65), (77, 65), (65, 77), (77, 77)):
        rect(draw, dx, dy, 8, 8, WHITE)

    image.save(
        "qr-display-winforms/icon.ico",
        sizes=[(16, 16), (32, 32), (48, 48), (256, 256)],
    )
    print("Icone enregistree dans qr-display-winforms/icon.ico")


if __name__ == "__main__":
    main()
