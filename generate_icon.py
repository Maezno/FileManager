"""Genera el ícono de Qubit Explorer: una Q con carpeta y un qubit al final de la cola."""
import os

from PIL import Image, ImageDraw, ImageFilter

S = 2048
VIOLET, AQUA, WHITE = (129, 140, 248), (56, 189, 248), (243, 244, 246)
CX, CY = S * 0.49, S * 0.49


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
    R, w = S * 0.245, S * 0.058
    d.ellipse([CX - R, CY - R, CX + R, CY + R], fill=255)
    r = R - w
    d.ellipse([CX - r, CY - r, CX + r, CY + r], fill=0)
    # Corte de la Q alrededor de la cola
    k = 0.7071
    a, b = R * 0.70, R * 1.22
    p1, p2 = (CX + a * k, CY + a * k), (CX + b * k, CY + b * k)
    d.line([p1, p2], fill=0, width=int(w * 2.0))
    # Cola
    tail = Image.new("L", (S, S), 0)
    t = ImageDraw.Draw(tail)
    a2, b2 = R * 0.74, R * 1.17
    q1, q2 = (CX + a2 * k, CY + a2 * k), (CX + b2 * k, CY + b2 * k)
    t.line([q1, q2], fill=255, width=int(w * 0.8))
    t.ellipse([q1[0] - w * .4, q1[1] - w * .4, q1[0] + w * .4, q1[1] + w * .4], fill=255)
    # Carpeta
    folder = Image.new("L", (S, S), 0)
    f = ImageDraw.Draw(folder)
    fw, fh = S * 0.118, S * 0.082
    rad = S * 0.018
    f.rounded_rectangle([CX - fw, CY - fh * 1.28, CX - fw * .1, CY], radius=rad, fill=255)
    f.rounded_rectangle([CX - fw, CY - fh * .95, CX + fw, CY + fh * 1.05], radius=rad, fill=255)
    # Qubit: el punto al final de la cola
    dot = Image.new("L", (S, S), 0)
    g = ImageDraw.Draw(dot)
    dr = w * 0.78
    g.ellipse([q2[0] - dr, q2[1] - dr, q2[0] + dr, q2[1] + dr], fill=255)
    return ring, tail, folder, dot


def foreground(mono=False):
    ring, tail, folder, dot = masks()
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    if mono:
        out.paste((255, 255, 255, 255), mask=ring)
        out.paste((255, 255, 255, 255), mask=tail)
        out.paste((255, 255, 255, 255), mask=folder)
        out.paste((255, 255, 255, 255), mask=dot)
        return out
    grad = gradient(VIOLET, AQUA).convert("RGBA")
    out.paste(grad, mask=ring)
    out.paste(grad, mask=tail)
    out.paste(WHITE + (255,), mask=folder)
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
icon.resize((1024, 1024), Image.LANCZOS).save(os.path.join("Resources", "app_icon.png"))
icon.resize((512, 512), Image.LANCZOS).save(os.path.join("Resources", "app_icon_play_store.png"))
icon.resize((256, 256), Image.LANCZOS).save(os.path.join("Resources", "app_icon.ico"), sizes=[(16, 16), (32, 32), (48, 48), (256, 256)])
