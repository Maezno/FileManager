import os
from PIL import Image, ImageDraw, ImageFilter

size = 512
img = Image.new("RGBA", (size, size), (0, 0, 0, 0))

# 1. Fondo estilizado con esquinas redondeadas idéntico a MediaRecovery PC
bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
bg_draw = ImageDraw.Draw(bg)

margin = 28
r = 110
rect = [margin, margin, size - margin, size - margin]

mask = Image.new("L", (size, size), 0)
mask_draw = ImageDraw.Draw(mask)
mask_draw.rounded_rectangle(rect, radius=r, fill=255)

# Gradiente de fondo oscuro elegante (#0D1117 a #161F30)
for y in range(size):
    factor = y / size
    r_col = int(13 + (22 - 13) * factor)
    g_col = int(17 + (31 - 17) * factor)
    b_col = int(23 + (48 - 23) * factor)
    bg_draw.line([(0, y), (size, y)], fill=(r_col, g_col, b_col, 255))

base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
base.paste(bg, (0, 0), mask)

# Borde acentuado con resplandor cian/azul (#388BFD)
border_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
border_draw = ImageDraw.Draw(border_layer)
border_draw.rounded_rectangle(rect, radius=r, outline=(56, 139, 253, 220), width=6)
base = Image.alpha_composite(base, border_layer)

center_x, center_y = size // 2, size // 2

# Aro exterior de disco / escaneo (mismo estilo MediaRecovery)
draw_main = ImageDraw.Draw(base)
outer_ring = [88, 88, size - 88, size - 88]
draw_main.ellipse(outer_ring, outline=(25, 38, 58, 210), width=14)

# Arco de progreso circular brillante (#58A6FF)
draw_main.arc(outer_ring, start=-35, end=205, fill=(88, 166, 255, 255), width=14)
# Arco verde éxito / búsqueda inteligente (#3FB950)
draw_main.arc(outer_ring, start=205, end=275, fill=(63, 185, 80, 255), width=14)

# Sombra suave de la carpeta central
shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
sh_draw = ImageDraw.Draw(shadow)
sh_draw.rounded_rectangle([148, 195, 372, 345], radius=24, fill=(0, 0, 0, 160))
shadow = shadow.filter(ImageFilter.GaussianBlur(10))
base = Image.alpha_composite(base, shadow)
draw_main = ImageDraw.Draw(base)

# 2. Carpeta central estilizada
# Solapa trasera + pestaña superior de la carpeta (#1F6FEB)
draw_main.rounded_rectangle([148, 174, 248, 235], radius=16, fill=(31, 111, 235, 255))
draw_main.rounded_rectangle([148, 196, 366, 336], radius=22, fill=(31, 111, 235, 255))
# Conector diagonal suave de la pestaña de la carpeta
draw_main.polygon([(232, 174), (264, 196), (232, 210)], fill=(31, 111, 235, 255))

# Hoja / documento asomando dentro de la carpeta (#A5D6FF)
draw_main.rounded_rectangle([172, 192, 342, 240], radius=10, fill=(165, 214, 255, 235))

# Cuerpo frontal de la carpeta (#58A6FF) con borde luminoso
draw_main.rounded_rectangle(
    [146, 214, 366, 340],
    radius=22,
    fill=(88, 166, 255, 255),
    outline=(165, 214, 255, 255),
    width=3
)

# Líneas de índice / metadatos dentro de la carpeta
draw_main.line([(176, 252), (265, 252)], fill=(13, 17, 23, 190), width=8)
draw_main.line([(176, 276), (242, 276)], fill=(13, 17, 23, 150), width=8)
draw_main.line([(176, 300), (225, 300)], fill=(13, 17, 23, 120), width=8)

# 3. Lupa / Nodo de búsqueda inteligente en verde esmeralda (#3FB950)
mag_cx, mag_cy, mag_r = 316, 296, 38
# Mango de la lupa
draw_main.line([(mag_cx + 24, mag_cy + 24), (mag_cx + 52, mag_cy + 52)], fill=(13, 17, 23, 230), width=20)
draw_main.line([(mag_cx + 24, mag_cy + 24), (mag_cx + 50, mag_cy + 50)], fill=(63, 185, 80, 255), width=13)

# Círculo de la lupa
draw_main.ellipse(
    [mag_cx - mag_r, mag_cy - mag_r, mag_cx + mag_r, mag_cy + mag_r],
    fill=(13, 17, 23, 240),
    outline=(63, 185, 80, 255),
    width=10
)

# Pulso / onda dentro de la lupa (guiño directo a MediaRecovery)
pulse_pts = [
    (mag_cx - 22, mag_cy + 4),
    (mag_cx - 10, mag_cy + 4),
    (mag_cx - 3, mag_cy - 14),
    (mag_cx + 5, mag_cy + 16),
    (mag_cx + 12, mag_cy - 6),
    (mag_cx + 22, mag_cy + 4)
]
draw_main.line(pulse_pts, fill=(63, 185, 80, 255), width=5, joint="curve")

os.makedirs("Resources", exist_ok=True)
ico_sizes = [(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)]
base.save(os.path.join("Resources", "app_icon.ico"), format="ICO", sizes=ico_sizes)
base.save(os.path.join("Resources", "app_icon.png"), format="PNG")

# Guardar drawable para la UI interna de Android
drawable_dir = os.path.join("app", "src", "main", "res", "drawable")
os.makedirs(drawable_dir, exist_ok=True)
base.resize((256, 256), Image.Resampling.LANCZOS).save(os.path.join(drawable_dir, "app_logo.png"), format="PNG")

# Generar mipmaps para todas las densidades de Android
mipmaps = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Crear versión circular para ic_launcher_round
round_mask = Image.new("L", (size, size), 0)
round_draw = ImageDraw.Draw(round_mask)
round_draw.ellipse([margin, margin, size - margin, size - margin], fill=255)
round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
round_img.paste(base, (0, 0), round_mask)
round_border = Image.new("RGBA", (size, size), (0, 0, 0, 0))
ImageDraw.Draw(round_border).ellipse([margin, margin, size - margin, size - margin], outline=(56, 139, 253, 220), width=6)
round_img = Image.alpha_composite(round_img, round_border)

for folder, px in mipmaps.items():
    out_dir = os.path.join("app", "src", "main", "res", folder)
    os.makedirs(out_dir, exist_ok=True)
    base.resize((px, px), Image.Resampling.LANCZOS).save(os.path.join(out_dir, "ic_launcher.png"), format="PNG")
    round_img.resize((px, px), Image.Resampling.LANCZOS).save(os.path.join(out_dir, "ic_launcher_round.png"), format="PNG")

print("OK: Iconos generados en Resources/ y app/src/main/res/")
