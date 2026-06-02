from pathlib import Path
import cv2
import numpy as np

src = Path('W:/02_Education/HNC L4/GlosCol/logo.png')
out_svg = Path('W:/04_Software_Projects/Apps/CMD/assets/townsend-precision-labs-logo.svg')
out_vector = Path('W:/04_Software_Projects/Apps/CMD/app/src/main/res/drawable/ic_cmd_logo.xml')
img = cv2.imread(str(src))
if img is None:
    raise SystemExit(f'Missing source image: {src}')
h, w = img.shape[:2]
b, g, r = cv2.split(img)
mask = ((r > 135) & (b > 90) & (g < 125) & ((r.astype(int) - g.astype(int)) > 50)).astype(np.uint8) * 255
kernel = np.ones((3, 3), np.uint8)
mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel, iterations=1)
mask = cv2.medianBlur(mask, 3)

def contours_to_path(mask_img, offset=(0, 0), scale=(1, 1), epsilon=1.2, min_area=6):
    contours, _ = cv2.findContours(mask_img, cv2.RETR_LIST, cv2.CHAIN_APPROX_NONE)
    parts = []
    ox, oy = offset
    sx, sy = scale
    contours = sorted(contours, key=cv2.contourArea, reverse=True)
    for c in contours:
        area = abs(cv2.contourArea(c))
        if area < min_area:
            continue
        approx = cv2.approxPolyDP(c, epsilon, True)
        pts = approx.reshape(-1, 2)
        if len(pts) < 3:
            continue
        cmds = []
        x0 = (pts[0][0] + ox) * sx
        y0 = (pts[0][1] + oy) * sy
        cmds.append(f'M{x0:.2f},{y0:.2f}')
        for x, y in pts[1:]:
            cmds.append(f'L{(x + ox) * sx:.2f},{(y + oy) * sy:.2f}')
        cmds.append('Z')
        parts.append(' '.join(cmds))
    return ' '.join(parts), len(parts)

full_path, n = contours_to_path(mask, epsilon=1.1, min_area=8)
svg = f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" role="img" aria-labelledby="title desc">
  <title id="title">Townsend Precision Labs Ltd logo</title>
  <desc id="desc">Faithful vector trace of the original Townsend Precision Labs Ltd logo from the HNC/GlosCol source image.</desc>
  <rect width="100%" height="100%" fill="none"/>
  <path fill="#d62b7f" fill-rule="evenodd" d="{full_path}"/>
</svg>
'''
out_svg.parent.mkdir(parents=True, exist_ok=True)
out_svg.write_text(svg, encoding='utf-8')

rows, cols = np.indices(mask.shape)
ys, xs = np.where((mask > 0) & (cols < 470))
x0, x1 = xs.min(), xs.max()
y0, y1 = ys.min(), ys.max()
pad = 18
x0 = max(0, x0 - pad)
y0 = max(0, y0 - pad)
x1 = min(w - 1, x1 + pad)
y1 = min(h - 1, y1 + pad)
crop = mask[y0:y1 + 1, x0:x1 + 1]
side = max(x1 - x0 + 1, y1 - y0 + 1)
square = np.zeros((side, side), dtype=np.uint8)
dx = (side - (x1 - x0 + 1)) // 2
dy = (side - (y1 - y0 + 1)) // 2
square[dy:dy + crop.shape[0], dx:dx + crop.shape[1]] = crop
icon_path, ni = contours_to_path(square, epsilon=1.0, min_area=5, scale=(108 / side, 108 / side))
vector = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#121017" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#D66BA0" android:fillType="evenOdd" android:pathData="{icon_path}" />
</vector>
'''
out_vector.write_text(vector, encoding='utf-8')
print(f'full contours={n}, icon contours={ni}, emblem bbox={(x0, y0, x1, y1)}, side={side}, svg chars={len(svg)}, vector chars={len(vector)}')
