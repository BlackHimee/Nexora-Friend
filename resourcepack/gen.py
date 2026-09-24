"""
Generates a pixel-art "Nexora Social" GUI texture pack for Minecraft:
 - textures/gui/container/generic_54.png  (used by ALL chest-style menus: 27/45/54 slots)
 - textures/gui/container/anvil.png       (used by the player-search screen)

These override the VANILLA container textures directly - no plugin code
changes needed. Slot hitboxes / item render positions are computed by the
client from fixed vanilla constants (independent of this artwork), so even
if a pixel offset here is a little off it will only ever be cosmetic, never
break functionality.

Palette: dark indigo/navy panel, bronze-gold trim, teal-blue slot recesses -
matches the plugin's existing "&8 dark / &b blue / &6 gold" color scheme.
"""
from PIL import Image, ImageDraw
import math
import random

random.seed(42)

# ---- palette -----------------------------------------------------------
BG_DEEP      = (18, 16, 34, 255)     # darkest panel base
BG_MID       = (28, 24, 52, 255)     # panel base
BG_LIGHT     = (40, 34, 70, 255)     # subtle panel highlight speckle
BORDER_DARK  = (12, 10, 22, 255)     # outer border shadow
GOLD_DARK    = (94, 68, 24, 255)
GOLD         = (176, 132, 48, 255)
GOLD_LIGHT   = (226, 182, 92, 255)
SLOT_BG      = (14, 12, 26, 255)     # recessed slot fill
SLOT_SHADOW  = (6, 5, 12, 255)       # slot top/left inner shadow
SLOT_HILITE  = (54, 74, 96, 255)     # slot bottom/right inner rim (teal glow)
SLOT_BORDER  = (70, 96, 122, 255)    # slot outer ring, cool blue-grey
TITLE_STRIP  = (24, 20, 44, 255)
TRANSPARENT  = (0, 0, 0, 0)

CANVAS = 256


def speckled_panel(draw, x0, y0, x1, y1):
    """Fill a rect with a subtle noisy indigo panel texture."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            r = random.random()
            if r < 0.06:
                draw.point((x, y), fill=BG_LIGHT)
            elif r < 0.10:
                draw.point((x, y), fill=BG_DEEP)
            else:
                draw.point((x, y), fill=BG_MID)


def ornate_border(img, draw, x0, y0, x1, y1, thickness=3):
    """Bronze-gold beveled border with corner accents, drawn inward from the rect."""
    # outer dark shadow line
    draw.rectangle([x0, y0, x1 - 1, y1 - 1], outline=BORDER_DARK, width=1)
    # gold band
    draw.rectangle([x0 + 1, y0 + 1, x1 - 2, y1 - 2], outline=GOLD_DARK, width=1)
    draw.rectangle([x0 + 2, y0 + 2, x1 - 3, y1 - 3], outline=GOLD, width=1)
    # inner highlight sliver
    draw.line([(x0 + thickness, y0 + thickness), (x1 - thickness - 1, y0 + thickness)], fill=GOLD_LIGHT)
    draw.line([(x0 + thickness, y0 + thickness), (x0 + thickness, y1 - thickness - 1)], fill=GOLD_LIGHT)

    # corner diamond accents
    def corner(cx, cy):
        pts = [(cx, cy - 3), (cx + 3, cy), (cx, cy + 3), (cx - 3, cy)]
        draw.polygon(pts, fill=GOLD_LIGHT, outline=GOLD_DARK)

    corner(x0 + 3, y0 + 3)
    corner(x1 - 4, y0 + 3)
    corner(x0 + 3, y1 - 4)
    corner(x1 - 4, y1 - 4)


def draw_slot(draw, x, y, size=18):
    """One 18x18 vanilla-grid inventory slot, recessed + cool-glow rim."""
    draw.rectangle([x, y, x + size - 1, y + size - 1], fill=SLOT_BG)
    # inner shadow (top+left) / rim highlight (bottom+right) for a recessed look
    draw.line([(x, y), (x + size - 1, y)], fill=SLOT_SHADOW)
    draw.line([(x, y), (x, y + size - 1)], fill=SLOT_SHADOW)
    draw.line([(x, y + size - 1), (x + size - 1, y + size - 1)], fill=SLOT_HILITE)
    draw.line([(x + size - 1, y), (x + size - 1, y + size - 1)], fill=SLOT_HILITE)
    draw.rectangle([x, y, x + size - 1, y + size - 1], outline=SLOT_BORDER)


def title_strip(draw, x0, y0, x1, y1):
    draw.rectangle([x0, y0, x1, y1], fill=TITLE_STRIP)
    draw.line([(x0, y1), (x1, y1)], fill=GOLD_DARK)
    # small arcane dot ornaments along the strip
    for x in range(x0 + 10, x1 - 10, 14):
        draw.point((x, (y0 + y1) // 2), fill=GOLD_LIGHT)


def build_generic_54():
    img = Image.new("RGBA", (CANVAS, CANVAS), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    WIDTH = 176
    TOP_MARGIN = 17
    ROWS_TOP = 6                       # vanilla always authors 6 rows in this file
    SLOT = 18
    LEFT = 7

    top_h = TOP_MARGIN + ROWS_TOP * SLOT           # 17 + 108 = 125
    ornate_border(img, draw, 0, 0, WIDTH, top_h + 2)
    speckled_panel(draw, 2, TOP_MARGIN - 1, WIDTH - 2, top_h)
    title_strip(draw, 2, 0, WIDTH - 3, TOP_MARGIN - 2)

    for row in range(ROWS_TOP):
        for col in range(9):
            draw_slot(draw, LEFT + col * SLOT, TOP_MARGIN + row * SLOT)

    # ---- player-inventory block (vanilla samples this fixed block from
    #      y=126 downward and stitches it under however many container rows
    #      are actually shown) --------------------------------------------
    PLAYER_Y = 126
    player_h = 3 * SLOT + 4 + SLOT + 8             # 3 rows + gap + hotbar + margin
    ornate_border(img, draw, 0, PLAYER_Y - 2, WIDTH, PLAYER_Y + player_h)
    speckled_panel(draw, 2, PLAYER_Y, WIDTH - 2, PLAYER_Y + player_h - 2)

    for row in range(3):
        for col in range(9):
            draw_slot(draw, LEFT + col * SLOT, PLAYER_Y + row * SLOT)

    hotbar_y = PLAYER_Y + 3 * SLOT + 4
    for col in range(9):
        draw_slot(draw, LEFT + col * SLOT, hotbar_y)

    return img


def build_anvil():
    img = Image.new("RGBA", (CANVAS, CANVAS), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    WIDTH = 176
    HEIGHT = 166

    ornate_border(img, draw, 0, 0, WIDTH, HEIGHT)
    speckled_panel(draw, 2, 2, WIDTH - 2, HEIGHT - 2)
    title_strip(draw, 2, 0, WIDTH - 3, 15)

    # text-field backdrop (where the rename box sits)
    draw.rectangle([59, 20, 143, 32], fill=SLOT_BG, outline=SLOT_BORDER)

    # the three anvil slots: input / (unused second input) / result
    draw_slot(draw, 27, 47)
    draw_slot(draw, 76, 47)
    draw_slot(draw, 134, 47)

    # player inventory block, same as generic_54
    PLAYER_Y = 84
    for row in range(3):
        for col in range(9):
            draw_slot(draw, 7 + col * 18, PLAYER_Y + row * 18)
    hotbar_y = PLAYER_Y + 3 * 18 + 4
    for col in range(9):
        draw_slot(draw, 7 + col * 18, hotbar_y)

    return img


def build_pack_icon():
    img = Image.new("RGBA", (128, 128), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    speckled_panel(draw, 0, 0, 128, 128)
    ornate_border(img, draw, 0, 0, 128, 128, thickness=4)
    # simple stylised "N" glyph
    draw.line([(40, 96), (40, 32)], fill=GOLD_LIGHT, width=6)
    draw.line([(40, 32), (88, 96)], fill=GOLD_LIGHT, width=6)
    draw.line([(88, 96), (88, 32)], fill=GOLD_LIGHT, width=6)
    return img


if __name__ == "__main__":
    import os
    out = "/home/user/Nexora-Friend/resourcepack"
    gui_dir = f"{out}/assets/minecraft/textures/gui/container"
    os.makedirs(gui_dir, exist_ok=True)

    build_generic_54().save(f"{gui_dir}/generic_54.png")
    build_anvil().save(f"{gui_dir}/anvil.png")
    build_pack_icon().save(f"{out}/pack.png")
    print("done")
