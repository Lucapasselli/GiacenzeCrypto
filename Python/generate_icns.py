from PIL import Image
import os

img = Image.open('logo.png').convert('RGBA')
sizes = [
    (16, False), (16, True),
    (32, False), (32, True),
    (128, False), (128, True),
    (256, False), (256, True),
    (512, False), (512, True),
    (1024, False)
]

os.makedirs('icon.iconset', exist_ok=True)

for size, retina in sizes:
    scale = 2 if retina else 1
    final_size = size * scale
    resized = img.resize((final_size, final_size), Image.LANCZOS)
    suffix = f'{size}x{size}{"@2x" if retina else ""}'
    resized.save(f'icon.iconset/icon_{suffix}.png')
