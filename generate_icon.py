"""Genera el ícono de Qubit Explorer: un aro con carpeta y un nodo encendido en el corte, igual que VidForge."""
import os

from PIL import Image, ImageDraw, ImageFilter

S = 2048
VIOLET, AQUA, WHITE = (129, 140, 248), (56, 189, 248), (243, 244, 246)
CX, CY = S * 0.5, S * 0.5


def gradient(c1, c2):
    small = Image.new("RGB", (64, 64))
    for y in range(64):
        for x in range(64):
            t = min(max(((x + y) / 126 - 0.25) / 0.5, 0), 1)
            small.putpixel((x, y), tuple(int(a + (b - a) * t) for a, b in zip(c1, c2)))
    return small.resize((S, S), Image.BICUBIC)


def background():
    return gradient((30, 35, 54), (10, 12, 18)).convert("RGBA")


def masks():
    ring = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(ring)
    R, w = S * 0.272, S * 0.062
    d.ellipse([CX - R, CY - R, CX + R, CY + R], fill=255)
    r = R - w
    d.ellipse([CX - r, CY - r, CX + r, CY + r], fill=0)
    # Nodo: va sobre la línea media del aro, en el mismo eje de la cola de la Q
    k = 0.7071
    mid = R - w / 2
    nx, ny = CX + mid * k, CY + mid * k
    dr = w * 0.86
    # Corte del aro alrededor del nodo
    cut = dr + w * 0.5
    d.ellipse([nx - cut, ny - cut, nx + cut, ny + cut], fill=0)
    # Carpeta: centrada contando la pestaña
    glyph = Image.new("L", (S, S), 0)
    f = ImageDraw.Draw(glyph)
    fw, fh = S * 0.13, S * 0.09
    rad = S * 0.02
    oy = CY + fh * 0.115
    f.rounded_rectangle([CX - fw, oy - fh * 1.28, CX - fw * .1, oy], radius=rad, fill=255)
    f.rounded_rectangle([CX - fw, oy - fh * .95, CX + fw, oy + fh * 1.05], radius=rad, fill=255)
    dot = Image.new("L", (S, S), 0)
    ImageDraw.Draw(dot).ellipse([nx - dr, ny - dr, nx + dr, ny + dr], fill=255)
    return ring, glyph, dot


def foreground(mono=False):
    ring, glyph, dot = masks()
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    if mono:
        out.paste((255, 255, 255, 255), mask=ring)
        out.paste((255, 255, 255, 255), mask=glyph)
        out.paste((255, 255, 255, 255), mask=dot)
        return out
    grad = gradient(VIOLET, AQUA).convert("RGBA")
    out.paste(grad, mask=ring)
    out.paste(WHITE + (255,), mask=glyph)
    glow = dot.filter(ImageFilter.GaussianBlur(S * 0.022)).point(lambda v: int(v * .7))
    layer = Image.new("RGBA", (S, S), AQUA + (0,))
    layer.putalpha(glow)
    out = Image.alpha_composite(out, layer)
    out.paste(WHITE + (255,), mask=dot)
    return out


def full(fg, zoom=1.0):
    im = Image.alpha_composite(background(), fg)
    if zoom != 1.0:
        c = int(S / zoom)
        o = (S - c) // 2
        im = im.crop((o, o, o + c, o + c))
    return im


def rounded(im, size, ratio):
    im = im.resize((size * 4, size * 4), Image.LANCZOS)
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size * 4 - 1, size * 4 - 1], radius=int(size * 4 * ratio), fill=255)
    im.putalpha(m)
    return im.resize((size, size), Image.LANCZOS)


res = os.path.join("app", "src", "main", "res")
fg = foreground()
mono = foreground(mono=True)
icon = full(fg, 1.45)

for name, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    folder = os.path.join(res, "mipmap-" + name)
    layer = px * 108 // 48
    fg.resize((layer, layer), Image.LANCZOS).save(os.path.join(folder, "ic_launcher_foreground.png"))
    mono.resize((layer, layer), Image.LANCZOS).save(os.path.join(folder, "ic_launcher_monochrome.png"))
    rounded(icon, px, 0.22).save(os.path.join(folder, "ic_launcher.png"))
    rounded(icon, px, 0.5).save(os.path.join(folder, "ic_launcher_round.png"))

icon.resize((256, 256), Image.LANCZOS).save(os.path.join(res, "drawable", "app_logo.png"))
os.makedirs("Resources", exist_ok=True)
icon.resize((1024, 1024), Image.LANCZOS).save(os.path.join("Resources", "app_icon.png"))
icon.resize((512, 512), Image.LANCZOS).save(os.path.join("Resources", "app_icon_play_store.png"))
icon.resize((256, 256), Image.LANCZOS).save(os.path.join("Resources", "app_icon.ico"), sizes=[(16, 16), (32, 32), (48, 48), (256, 256)])
