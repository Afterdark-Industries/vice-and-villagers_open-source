"""Create the native seven-column cactus model from the user's voxel reference.

The silhouette, height variation, muted green flesh and raised ivory cross thorns
are geometry and small native pixel textures. No image plane or background is used.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/trippycraft"
TEXTURES = ASSETS / "textures/block"
MODELS = ASSETS / "models/block"
PREVIEW = ROOT / "build/asset-previews/mescaline_cactus_preview.png"

# x, z, width, flesh height: central stalk surrounded by six staggered columns.
COLUMNS = [(8, 3.5, 4.6, 14.3), (3.5, 6.1, 4.4, 11.5),
           (12.5, 6.1, 4.4, 11.5), (8, 8, 4.2, 9.8),
           (3.5, 10.9, 4.4, 9.8), (12.5, 10.9, 4.4, 9.8),
           (8, 12.8, 4.6, 9.1)]


def texture(name, palette, seed, stripes=False):
    rng = random.Random(seed)
    result = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            index = rng.choices(range(len(palette)), [1, 3, 6, 3, 1])[0]
            if stripes and x % 5 in (1, 2):
                index = min(4, index + 1)
            result.putpixel((x, y), palette[index])
    result.save(TEXTURES / f"{name}.png")
    return result


def box(start, end, side, top=None):
    x0, y0, z0 = start
    x1, y1, z1 = end
    return {"from": list(start), "to": list(end), "faces": {
        "down": {"texture": f"#{side}", "uv": [x0, z0, x1, z1]},
        "up": {"texture": f"#{top or side}", "uv": [x0, z0, x1, z1]},
        "north": {"texture": f"#{side}", "uv": [x0, 16-y1, x1, 16-y0]},
        "south": {"texture": f"#{side}", "uv": [x0, 16-y1, x1, 16-y0]},
        "west": {"texture": f"#{side}", "uv": [z0, 16-y1, z1, 16-y0]},
        "east": {"texture": f"#{side}", "uv": [z0, 16-y1, z1, 16-y0]},
    }}


def column(elements, x, z, width, height):
    half = width / 2
    bevel = 0.55
    # Two interlocking bodies clip the corners and leave a deliberately stepped top.
    elements.append(box((x-half+bevel, 0, z-half), (x+half-bevel, height-0.5, z+half), "flesh", "top"))
    elements.append(box((x-half, 0, z-half+bevel), (x+half, height-0.5, z+half-bevel), "flesh", "top"))
    elements.append(box((x-half+0.45, height-0.5, z-half+0.45),
                        (x+half-0.45, height, z+half-0.45), "flesh", "top"))
    # Raised ivory crown cross and tan attachment, as in the reference.
    elements.append(box((x-0.42, height, z-0.42), (x+0.42, height+0.35, z+0.42), "base"))
    elements.append(box((x-1.0, height+0.2, z-0.35), (x+1.0, height+0.6, z+0.35), "thorn"))
    elements.append(box((x-0.35, height+0.2, z-1.0), (x+0.35, height+0.6, z+1.0), "thorn"))
    elements.append(box((x-0.38, height+0.3, z-0.38), (x+0.38, height+1.3, z+0.38), "thorn"))


def side_thorn(elements, x, z, y, axis, sign):
    near = z if axis == "z" else x
    base0, base1 = sorted((near, near+sign*0.22))
    depth0, depth1 = sorted((near+sign*0.22, near+sign*0.65))
    if axis == "z":
        elements.append(box((x-0.4, y-0.4, base0), (x+0.4, y+0.4, base1), "base"))
        elements.append(box((x-1.0, y-0.35, depth0), (x+1.0, y+0.35, depth1), "thorn"))
        elements.append(box((x-0.35, y-1.0, depth0), (x+0.35, y+1.0, depth1), "thorn"))
    else:
        elements.append(box((base0, y-0.4, z-0.4), (base1, y+0.4, z+0.4), "base"))
        elements.append(box((depth0, y-0.35, z-1.0), (depth1, y+0.35, z+1.0), "thorn"))
        elements.append(box((depth0, y-1.0, z-0.35), (depth1, y+1.0, z+0.35), "thorn"))


def model():
    elements = []
    for x, z, width, height in COLUMNS:
        column(elements, x, z, width, height)
    side_thorn(elements, 8, 15.1, 4.4, "z", 1)
    side_thorn(elements, 3.5, 13.1, 5.4, "z", 1)
    side_thorn(elements, 12.5, 13.1, 5.4, "z", 1)
    side_thorn(elements, 1.3, 6.1, 6.0, "x", -1)
    side_thorn(elements, 14.7, 6.1, 6.0, "x", 1)
    return {
        "ambientocclusion": True,
        "textures": {
            "particle": "trippycraft:block/mescaline_cactus_side",
            "flesh": "trippycraft:block/mescaline_cactus_side",
            "top": "trippycraft:block/mescaline_cactus_top",
            "thorn": "trippycraft:block/mescaline_cactus_thorn",
            "base": "trippycraft:block/mescaline_cactus_thorn_base",
        },
        "elements": elements,
    }


def render(model_data, textures):
    """Render actual JSON cuboids with a depth buffer for the native-model preview."""
    import numpy as np

    size = 740
    zoom = 22
    canvas = Image.new("RGB", (size, size), (39, 43, 39))
    pixels = np.asarray(canvas).copy()
    depths = np.full((size, size), -float("inf"))

    def project(point):
        x, y, z = point
        return np.array([size/2 + (x-z)*math.cos(math.pi/6)*zoom,
                         530 + (x+z-16)*0.5*zoom-y*zoom,
                         x+z+y])

    def triangle(points, color):
        a, b, c = points
        xlow = max(0, int(math.floor(min(a[0], b[0], c[0]))))
        xhigh = min(size-1, int(math.ceil(max(a[0], b[0], c[0]))))
        ylow = max(0, int(math.floor(min(a[1], b[1], c[1]))))
        yhigh = min(size-1, int(math.ceil(max(a[1], b[1], c[1]))))
        if xlow > xhigh or ylow > yhigh:
            return
        xx, yy = np.meshgrid(np.arange(xlow, xhigh+1)+0.5, np.arange(ylow, yhigh+1)+0.5)
        denom = (b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
        if abs(denom) < 1e-9:
            return
        wa = ((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/denom
        wb = ((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/denom
        wc = 1-wa-wb
        depth = wa*a[2]+wb*b[2]+wc*c[2]
        region = depths[ylow:yhigh+1, xlow:xhigh+1]
        mask = (wa >= -1e-7) & (wb >= -1e-7) & (wc >= -1e-7) & (depth >= region)
        region[mask] = depth[mask]
        pixels[ylow:yhigh+1, xlow:xhigh+1][mask] = color

    for element in model_data["elements"]:
        x0, y0, z0 = element["from"]
        x1, y1, z1 = element["to"]
        face_specs = [
            ("up", (x0, y1, z0), (x1-x0, 0, 0), (0, 0, z1-z0), 1.0),
            ("south", (x0, y1, z1), (x1-x0, 0, 0), (0, y0-y1, 0), 0.72),
            ("east", (x1, y1, z0), (0, 0, z1-z0), (0, y0-y1, 0), 0.85),
        ]
        for face, start, uvector, vvector, shade in face_specs:
            face_data = element["faces"][face]
            tex = textures[face_data["texture"][1:]]
            uv = face_data["uv"]
            nu, nv = max(1, math.ceil(uv[2]-uv[0])), max(1, math.ceil(uv[3]-uv[1]))
            for iu in range(nu):
                for iv in range(nv):
                    corners = [project(tuple(start[k]+uvector[k]*u+vvector[k]*v for k in range(3)))
                               for u, v in ((iu/nu, iv/nv), ((iu+1)/nu, iv/nv),
                                            ((iu+1)/nu, (iv+1)/nv), (iu/nu, (iv+1)/nv))]
                    tu = int(uv[0]+(uv[2]-uv[0])*(iu+0.5)/nu) % 16
                    tv = int(uv[1]+(uv[3]-uv[1])*(iv+0.5)/nv) % 16
                    color = tuple(round(c*shade) for c in tex.getpixel((tu, tv)))
                    triangle([corners[0], corners[1], corners[2]], color)
                    triangle([corners[0], corners[2], corners[3]], color)
    result = Image.fromarray(pixels)
    draw = ImageDraw.Draw(result)
    draw.text((34, 34), "MESCALINE CACTUS", fill=(234, 229, 201), font=ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 27))
    draw.text((34, 676), "Native seven-column block model | ivory thorns | muted sage greens", fill=(184, 192, 171),
              font=ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 16))
    return result


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    textures = {
        "flesh": texture("mescaline_cactus_side", [(48, 78, 60), (60, 93, 68), (71, 106, 77), (83, 120, 86), (106, 138, 96)], 4401, True),
        "top": texture("mescaline_cactus_top", [(80, 112, 76), (104, 133, 86), (117, 145, 95), (137, 159, 108), (165, 178, 124)], 4402),
        "thorn": texture("mescaline_cactus_thorn", [(185, 165, 124), (215, 198, 156), (232, 218, 180), (244, 232, 200), (255, 245, 221)], 4403),
        "base": texture("mescaline_cactus_thorn_base", [(135, 107, 60), (157, 128, 75), (183, 153, 93), (207, 178, 111), (224, 199, 139)], 4404),
    }
    block_model = model()
    (MODELS / "mescaline_cactus.json").write_text(json.dumps(block_model, indent=2)+"\n", encoding="utf-8")
    render(block_model, textures).save(PREVIEW)
    print(f"Built {len(block_model['elements'])} native cuboids and four 16x16 textures: {PREVIEW}")


if __name__ == "__main__":
    main()
