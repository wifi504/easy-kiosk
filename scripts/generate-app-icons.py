"""Generate Android launcher sizes mechanically from the user-supplied logo, in Actions only."""
from pathlib import Path
import os
from PIL import Image, ImageDraw

if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('Icon generation must run in GitHub Actions')
root = Path(__file__).resolve().parents[1]
source = Image.open(root / 'src/assets/images/easy-kiosk-logo.png').convert('RGBA')
if source.width != source.height:
    raise SystemExit('The supplied logo must be square')
res = root / 'android/app/src/main/res'
scales = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
blue = source.getpixel((0, 0))
# The provided blue artwork has a white mark. Preserve that mark for themed icons.
mono = Image.new('RGBA', source.size)
mono.putdata([(255, 255, 255, max(0, min(255, (min(r, g, b) - 160) * 255 // 95))) for r, g, b, a in source.getdata()])
for density, scale in scales.items():
    folder = res / ('mipmap-' + density)
    folder.mkdir(parents=True, exist_ok=True)
    size, adaptive, inset = round(48 * scale), round(108 * scale), round(84 * scale)
    flat = source.resize((size, size), Image.Resampling.LANCZOS)
    flat.save(folder / 'ic_launcher.png')
    mask = Image.new('L', (size, size)); ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    rounded = flat.copy(); rounded.putalpha(mask); rounded.save(folder / 'ic_launcher_round.png')
    Image.new('RGBA', (adaptive, adaptive), blue).save(folder / 'ic_launcher_background.png')
    for name, image in [('foreground', source), ('monochrome', mono)]:
        canvas = Image.new('RGBA', (adaptive, adaptive))
        image = image.resize((inset, inset), Image.Resampling.LANCZOS)
        offset = (adaptive - inset) // 2
        canvas.alpha_composite(image, (offset, offset))
        canvas.save(folder / ('ic_launcher_' + name + '.png'))
# CI review artifact: supplied logo and common adaptive launcher masks.
preview = Image.new('RGB', (768, 256), '#eeeeee')
preview.paste(source.resize((256, 256), Image.Resampling.LANCZOS), (0, 0))
folder = res / 'mipmap-xxxhdpi'
background = Image.open(folder / 'ic_launcher_background.png').convert('RGBA')
background.alpha_composite(Image.open(folder / 'ic_launcher_foreground.png'))
# Android's normal viewport is the central 72dp of the 108dp adaptive canvas.
edge = round(18 * 4)
icon = background.crop((edge, edge, background.width - edge, background.height - edge)).resize((224, 224), Image.Resampling.LANCZOS)
for column, radius in [(1, 112), (2, 46)]:
    mask = Image.new('L', icon.size); ImageDraw.Draw(mask).rounded_rectangle((0, 0, 223, 223), radius=radius, fill=255)
    preview.paste(icon, (column * 256 + 16, 16), mask)
out = Path(os.environ['RUNNER_TEMP']) / 'easy-kiosk-logo-preview.png'
preview.save(out)
print('Generated launcher resources from the supplied logo; preview:', out)