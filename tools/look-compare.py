"""Puts same-named screenshots from two folders side by side (A left, B right), half size, with a label strip.

usage: python tools/look-compare.py <dirA> <dirB> <outDir>
"""
import os
import sys

from PIL import Image, ImageDraw


def main():
    a_dir, b_dir, out_dir = sys.argv[1:4]
    os.makedirs(out_dir, exist_ok=True)
    a_label, b_label = os.path.basename(a_dir), os.path.basename(b_dir)
    names = sorted(n for n in os.listdir(a_dir) if n.endswith(".png") and os.path.exists(os.path.join(b_dir, n)))
    for n in names:
        a = Image.open(os.path.join(a_dir, n)).convert("RGB")
        b = Image.open(os.path.join(b_dir, n)).convert("RGB")
        w, h = a.width // 2, a.height // 2
        a, b = a.resize((w, h), Image.LANCZOS), b.resize((w, h), Image.LANCZOS)
        out = Image.new("RGB", (w * 2 + 8, h + 28), (20, 20, 20))
        out.paste(a, (0, 28))
        out.paste(b, (w + 8, 28))
        d = ImageDraw.Draw(out)
        d.text((8, 8), f"{a_label}  |  {n}", fill=(230, 230, 230))
        d.text((w + 16, 8), b_label, fill=(230, 230, 230))
        out.save(os.path.join(out_dir, n))
        print(os.path.join(out_dir, n))


if __name__ == "__main__":
    main()
