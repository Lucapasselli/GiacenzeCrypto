#!/usr/bin/env python3
"""Genera la locandina 9:16 della scheda Microsoft Store.

Produce Locandina1440x2160.png e la sua riduzione Locandina720x1080.png.
Si lancia dalla radice del progetto:  python3 packaging/msix/StoreListing/genera-locandina.py

I colori sono quelli di logo.png (nero, oliva A8A834, grigio chiaro E7E7E7) e il carattere e' lo
stesso incluso nell'applicazione (src/main/resources/Fonts): la locandina deve somigliare al
programma, non a un'altra cosa. Poco testo, e nessun marchio di terzi.
"""

from PIL import Image, ImageDraw, ImageFont
import pathlib

RADICE = pathlib.Path(__file__).resolve().parents[3]
FONTS = RADICE / "src/main/resources/Fonts"
USCITA = pathlib.Path(__file__).resolve().parent

L, A = 1440, 2160
NERO = (8, 8, 8)
OLIVA = (168, 168, 52)
CHIARO = (231, 231, 231)

bold = lambda d: ImageFont.truetype(str(FONTS / "NotoSans-Bold.ttf"), d)
regular = lambda d: ImageFont.truetype(str(FONTS / "NotoSans-Regular.ttf"), d)

img = Image.new("RGB", (L, A), NERO)
dis = ImageDraw.Draw(img)

# Fondo: schiarita verticale appena percettibile, cosi' il nero non sembra un errore di stampa
for y in range(A):
    k = 1.0 - abs(y - A * 0.32) / (A * 1.15)
    k = max(0.0, k)
    dis.line([(0, y), (L, y)], fill=(int(8 + 20 * k), int(8 + 20 * k), int(8 + 14 * k)))

# Alone oliva dietro il logo, disegnato come cerchi concentrici trasparenti
alone = Image.new("RGBA", (L, A), (0, 0, 0, 0))
da = ImageDraw.Draw(alone)
cx, cy = L // 2, 640
for r in range(560, 0, -8):
    op = int(26 * (1 - r / 560) ** 2)
    da.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(*OLIVA, op))
img = Image.alpha_composite(img.convert("RGBA"), alone).convert("RGB")
dis = ImageDraw.Draw(img)

# Logo
logo = Image.open(RADICE / "logo.png").convert("RGBA").resize((620, 620), Image.LANCZOS)
img.paste(logo, (cx - 310, cy - 310), logo)


def centrato(testo, font, y, colore):
    """Scrive il testo centrato in orizzontale e restituisce la y sotto la riga."""
    x0, y0, x1, y1 = dis.textbbox((0, 0), testo, font=font)
    dis.text((cx - (x1 - x0) / 2 - x0, y), testo, font=font, fill=colore)
    return y + (y1 - y0) + (y0 - 0)


y = 1080
centrato("Giacenze Crypto", bold(136), y, CHIARO)

y = 1290
dis.rectangle([cx - 140, y, cx + 140, y + 7], fill=OLIVA)

y = 1400
centrato("Plusvalenze e giacenze in criptovaluta", regular(62), y, CHIARO)
centrato("per la dichiarazione dei redditi italiana", regular(62), y + 92, CHIARO)

# Nessuna riga finale che nomini il sistema operativo o il negozio: e' materiale promozionale, e i
# marchi altrui in un'immagine promozionale sono una contestazione gratuita.
y = 1720
for riga in ("Metodo LIFO", "Quadro RT e Quadro W/RW", "I dati restano sul tuo computer"):
    centrato(riga, regular(56), y, OLIVA)
    y += 108

img.save(USCITA / "Locandina1440x2160.png")
img.resize((720, 1080), Image.LANCZOS).save(USCITA / "Locandina720x1080.png")
print("scritte", USCITA / "Locandina1440x2160.png", "e la riduzione 720x1080")
