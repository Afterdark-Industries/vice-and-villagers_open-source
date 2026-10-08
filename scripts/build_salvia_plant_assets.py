"""Build Salvia's native voxel block models and a model contact sheet.

The supplied plant reference is interpreted as geometry: low opposing broad leaves,
a single green stem and staggered purple florets with pale lavender tips.
"""

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/trippycraft"
TEXTURES = ASSETS / "textures/block"
MODELS = ASSETS / "models/block"
PREVIEW = ROOT / "build/asset-previews/salvia-plant-preview.png"


def texture(name, palette, seed, stem=False):
    rng = random.Random(seed)
    image = Image.new("RGB", (16, 16))
    pixels = image.load()
    for y in range(16):
        for x in range(16):
            index = rng.choices(range(len(palette)), [1, 3, 5, 3, 1])[0]
            if stem:
                index = min(4, max(0, index + (1 if x in (4, 5, 10) else 0)))
            elif name == "salvia_leaf" and x in (7, 8):
                index = max(index, 3)
            pixels[x, y] = palette[index]
    image.save(TEXTURES / f"{name}.png")
    return image


def box(start, end, material):
    x0, y0, z0 = start
    x1, y1, z1 = end
    return {
        "from": list(start),
        "to": list(end),
        "faces": {
            "down": {"texture": f"#{material}", "uv": [x0, z0, x1, z1]},
            "up": {"texture": f"#{material}", "uv": [x0, z0, x1, z1]},
            "north": {"texture": f"#{material}", "uv": [x0, 16-y1, x1, 16-y0]},
            "south": {"texture": f"#{material}", "uv": [x0, 16-y1, x1, 16-y0]},
            "west": {"texture": f"#{material}", "uv": [z0, 16-y1, z1, 16-y0]},
            "east": {"texture": f"#{material}", "uv": [z0, 16-y1, z1, 16-y0]},
        },
    }


def radial_box(elements, direction, distance, length, width, height, thickness, material):
    """Axis-aligned stepped pieces retain the reference's deliberately blocky silhouette."""
    axis, sign = direction
    near = 8 + sign * distance
    far = 8 + sign * (distance + length)
    low, high = min(near, far), max(near, far)
    if axis == "x":
        elements.append(box((low, height, 8-width/2), (high, height+thickness, 8+width/2), material))
    else:
        elements.append(box((8-width/2, height, low), (8+width/2, height+thickness, high), material))


def leaf(elements, direction, height, scale):
    # Broad lance-shaped blades with stepped shoulders and a slim root at the stem.
    for distance, length, width, rise in (
        (0.35, 1.25, 1.35, 0.0),
        (1.6, 1.7, 3.6, 0.18),
        (3.3, 1.8, 4.4, 0.38),
        (5.1, 1.4, 3.25, 0.58),
        (6.5, 0.8, 1.6, 0.78),
    ):
        radial_box(elements, direction, distance*scale, length*scale, width*scale,
                   height+rise*scale, 0.55*scale, "leaf")


def flower(elements, direction, height, reach, pale=True, scale=1):
    radial_box(elements, direction, 0.45, 0.65*scale, 0.75*scale, height, 0.75*scale, "stem")
    radial_box(elements, direction, 0.75, 1.0*scale, 1.2*scale, height-0.1, 1.0*scale, "flower")
    radial_box(elements, direction, 1.6, reach*scale, 1.25*scale, height+0.45, 0.95*scale, "flower")
    if pale:
        radial_box(elements, direction, 1.6+reach*scale, 0.8*scale, 1.45*scale,
                   height+0.4, 0.8*scale, "flower_tip")
        radial_box(elements, direction, 1.6+reach*scale-0.35, 0.85*scale, 1.1*scale,
                   height-0.35, 0.8*scale, "flower_tip")


def stage_model(stage):
    heights = [3.8, 7.5, 12.5, 16]
    elements = [box((7.45, 0, 7.45), (8.55, heights[stage], 8.55), "stem")]
    leaf(elements, ("x", -1), 0.75, [0.42, 0.7, 0.92, 1][stage])
    leaf(elements, ("x", 1), 1.15, [0.42, 0.7, 0.92, 1][stage])
    if stage > 0:
        leaf(elements, ("z", -1), 2.5, [0, 0.55, 0.8, 0.86][stage])
        leaf(elements, ("z", 1), 3.05, [0, 0.55, 0.8, 0.86][stage])
    if stage > 1:
        leaf(elements, ("x", -1), 4.25, 0.67)
        leaf(elements, ("x", 1), 4.85, 0.67)
        if stage == 2:
            for direction, y in [(("x", -1), 8), (("z", 1), 9.4), (("x", 1), 10.2)]:
                flower(elements, direction, y, 0.5, pale=False, scale=0.75)
            elements.append(box((7.3, 11.2, 7.3), (8.7, 12.7, 8.7), "flower"))
        else:
            for direction, y, reach in [
                (("z", 1), 6.15, 0.3), (("x", -1), 7.1, 1.1),
                (("x", 1), 8.1, 1.2), (("z", -1), 9.5, 0.8),
                (("x", -1), 10.6, 0.9), (("x", 1), 11.8, 1.1),
                (("z", 1), 13.0, 0.6), (("x", -1), 14.0, 0.4),
            ]:
                flower(elements, direction, y, reach, pale=y < 13)
            elements.append(box((7.25, 14.45, 7.25), (8.75, 15.25, 8.75), "flower"))
            elements.append(box((7.4, 15.25, 7.4), (8.6, 16, 8.6), "flower"))
    return {
        "render_type": "minecraft:cutout",
        "ambientocclusion": False,
        "textures": {
            "particle": "trippycraft:block/salvia_leaf",
            "leaf": "trippycraft:block/salvia_leaf",
            "stem": "trippycraft:block/salvia_stem",
            "flower": "trippycraft:block/salvia_flower",
            "flower_tip": "trippycraft:block/salvia_flower_tip",
        },
        "elements": elements,
    }


def render(model, textures, canvas, offset):
    # Isometric face render using the same JSON cuboids and pixel textures, with a painter pass.
    polygons = []
    zoom = 16
    def project(point):
        x, y, z = point
        return (offset[0] + (x-z)*math.cos(math.pi/6)*zoom,
                offset[1] + (x+z-16)*math.sin(math.pi/6)*zoom-y*zoom)
    for element in model["elements"]:
        x0, y0, z0 = element["from"]
        x1, y1, z1 = element["to"]
        material = element["faces"]["up"]["texture"][1:]
        tex = textures[material]
        face_specs = [
            ("up", (x0, y1, z0), (x1-x0, 0, 0), (0, 0, z1-z0), 1),
            ("south", (x0, y1, z1), (x1-x0, 0, 0), (0, y0-y1, 0), 0.76),
            ("east", (x1, y1, z0), (0, 0, z1-z0), (0, y0-y1, 0), 0.88),
        ]
        for face, start, uvector, vvector, shade in face_specs:
            uv = element["faces"][face]["uv"]
            usteps = max(1, math.ceil(uv[2]-uv[0]))
            vsteps = max(1, math.ceil(uv[3]-uv[1]))
            for iu in range(usteps):
                for iv in range(vsteps):
                    corners = []
                    for u, v in ((iu/usteps, iv/vsteps), ((iu+1)/usteps, iv/vsteps),
                                 ((iu+1)/usteps, (iv+1)/vsteps), (iu/usteps, (iv+1)/vsteps)):
                        corners.append(tuple(start[i]+uvector[i]*u+vvector[i]*v for i in range(3)))
                    tu = int(uv[0]+(uv[2]-uv[0])*(iu+0.5)/usteps) % 16
                    tv = int(uv[1]+(uv[3]-uv[1])*(iv+0.5)/vsteps) % 16
                    color = tuple(int(c*shade) for c in tex.getpixel((tu, tv)))
                    depth = sum(p[0]+p[2]+p[1]*0.75 for p in corners)/4
                    polygons.append((depth, [project(p) for p in corners], color))
    draw = ImageDraw.Draw(canvas)
    for _, polygon, color in sorted(polygons, key=lambda p: p[0]):
        draw.polygon(polygon, fill=color)


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    textures = {
        "leaf": texture("salvia_leaf", [(42, 77, 24), (61, 105, 30), (79, 132, 40), (108, 158, 49), (142, 188, 69)], 214),
        "stem": texture("salvia_stem", [(54, 85, 28), (72, 111, 31), (97, 138, 37), (120, 160, 43), (144, 179, 54)], 386, True),
        "flower": texture("salvia_flower", [(82, 31, 112), (106, 37, 151), (127, 48, 181), (151, 62, 213), (176, 87, 232)], 718),
        "flower_tip": texture("salvia_flower_tip", [(144, 89, 195), (165, 111, 217), (185, 136, 230), (206, 168, 243), (224, 200, 251)], 845),
    }
    stages = [stage_model(stage) for stage in range(4)]
    for stage, model in enumerate(stages):
        (MODELS / f"salvia_divinorum_plant_stage{stage}.json").write_text(json.dumps(model, indent=2)+"\n", encoding="utf-8")
    canvas = Image.new("RGB", (1280, 430), (29, 32, 30))
    draw = ImageDraw.Draw(canvas)
    for stage, model in enumerate(stages):
        render(model, textures, canvas, (160+stage*320, 335))
        draw.text((135+stage*320, 385), f"Age {stage}", fill=(224, 231, 219))
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(PREVIEW)
    print(f"Built four plant stages and four 16x16 textures. Preview: {PREVIEW}")


if __name__ == "__main__":
    main()
