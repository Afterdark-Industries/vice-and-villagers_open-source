#!/usr/bin/env python3
"""Generate Vice and Villagers's five Minecraft 1.21.1 village bar templates.

The output is deterministic GZIP NBT and intentionally uses only Python's
standard library so release artifacts can always be reproduced.
"""

from __future__ import annotations

import gzip
import io
import json
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


TAG_BYTE = 1
TAG_INT = 3
TAG_DOUBLE = 6
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
DATA_VERSION = 3955
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/data/trippycraft/structure/village/bars"
BLUEPRINT = ROOT / "scripts/custom_tavern_blueprint.json"
RAVE_FIXTURES = {
    (3, 4, 4): "minecraft:pearlescent_froglight",
    (6, 4, 4): "minecraft:verdant_froglight",
    (3, 4, 7): "minecraft:ochre_froglight",
    (6, 4, 7): "minecraft:sea_lantern",
}


@dataclass(frozen=True)
class Tag:
    kind: int
    value: Any


def byte(value: int) -> Tag:
    return Tag(TAG_BYTE, value)


def integer(value: int) -> Tag:
    return Tag(TAG_INT, value)


def string(value: str) -> Tag:
    return Tag(TAG_STRING, value)


def list_tag(kind: int, values: Iterable[Any]) -> Tag:
    return Tag(TAG_LIST, (kind, list(values)))


def compound(values: dict[str, Tag]) -> Tag:
    return Tag(TAG_COMPOUND, values)


def write_string(out: io.BufferedIOBase, value: str) -> None:
    raw = value.encode("utf-8")
    out.write(struct.pack(">H", len(raw)))
    out.write(raw)


def write_payload(out: io.BufferedIOBase, tag: Tag) -> None:
    if tag.kind == TAG_BYTE:
        out.write(struct.pack(">b", tag.value))
    elif tag.kind == TAG_INT:
        out.write(struct.pack(">i", tag.value))
    elif tag.kind == TAG_DOUBLE:
        out.write(struct.pack(">d", tag.value))
    elif tag.kind == TAG_STRING:
        write_string(out, tag.value)
    elif tag.kind == TAG_LIST:
        child_kind, values = tag.value
        out.write(struct.pack(">bi", child_kind, len(values)))
        for value in values:
            child = value if isinstance(value, Tag) else Tag(child_kind, value)
            if child.kind != child_kind:
                raise ValueError("NBT list element type mismatch")
            write_payload(out, child)
    elif tag.kind == TAG_COMPOUND:
        for name, child in tag.value.items():
            out.write(struct.pack(">b", child.kind))
            write_string(out, name)
            write_payload(out, child)
        out.write(b"\x00")
    else:
        raise ValueError(f"Unsupported NBT tag {tag.kind}")


def write_root(path: Path, root: Tag) -> None:
    raw = io.BytesIO()
    raw.write(struct.pack(">bH", TAG_COMPOUND, 0))
    write_payload(raw, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(filename=path, mode="wb", compresslevel=9, mtime=0) as target:
        target.write(raw.getvalue())


class Template:
    def __init__(self, name: str, size: tuple[int, int, int]) -> None:
        self.name = name
        self.size = size
        self.blocks: dict[tuple[int, int, int], str] = {}

    def set(self, x: int, y: int, z: int, state: str) -> None:
        if not (0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]):
            raise ValueError(f"{self.name}: block outside template at {(x, y, z)}")
        self.blocks[(x, y, z)] = state

    def finalize(self) -> None:
        # Explicit air above floor level makes late placement clear foliage and
        # snow cleanly. Unspecified y=0 cells preserve the surrounding terrain.
        for x in range(self.size[0]):
            for y in range(1, self.size[1]):
                for z in range(self.size[2]):
                    self.blocks.setdefault((x, y, z), "minecraft:air")

    def to_nbt(self) -> Tag:
        self.finalize()
        palette_states = list(dict.fromkeys(self.blocks.values()))
        palette_index = {state: index for index, state in enumerate(palette_states)}

        palette = []
        for state in palette_states:
            name, properties = parse_state(state)
            payload = {"Name": string(name)}
            if properties:
                payload["Properties"] = compound({key: string(value) for key, value in properties.items()})
            palette.append(compound(payload))

        blocks = []
        for pos, state in sorted(self.blocks.items(), key=lambda entry: (entry[0][1], entry[0][2], entry[0][0])):
            blocks.append(compound({
                "pos": list_tag(TAG_INT, pos),
                "state": integer(palette_index[state]),
            }))

        return compound({
            "DataVersion": integer(DATA_VERSION),
            "size": list_tag(TAG_INT, self.size),
            "palette": list_tag(TAG_COMPOUND, palette),
            "blocks": list_tag(TAG_COMPOUND, blocks),
            "entities": list_tag(TAG_COMPOUND, []),
        })


def parse_state(state: str) -> tuple[str, dict[str, str]]:
    if "[" not in state:
        return state, {}
    name, raw = state[:-1].split("[", 1)
    properties = {}
    for pair in raw.split(","):
        key, value = pair.split("=", 1)
        properties[key] = value
    return name, properties


# The supplied tavern is the shared shape. These substitutions keep every
# stair direction and connection property intact while adapting its materials
# to the five vanilla village families.
REGIONAL_PALETTES: dict[str, dict[str, str]] = {
    "oak_taproom": {
        "minecraft:black_stained_glass_pane": "minecraft:brown_stained_glass_pane",
    },
    "desert_cantina": {
        "minecraft:polished_granite": "minecraft:smooth_sandstone",
        "minecraft:oak_planks": "minecraft:sandstone",
        "minecraft:oak_log": "minecraft:stripped_acacia_log",
        "minecraft:spruce_stairs": "minecraft:red_sandstone_stairs",
        "minecraft:cobblestone": "minecraft:cut_sandstone",
        "minecraft:mossy_cobblestone": "minecraft:chiseled_sandstone",
        "minecraft:spruce_fence": "minecraft:acacia_fence",
        "minecraft:spruce_pressure_plate": "minecraft:acacia_pressure_plate",
        "minecraft:spruce_door": "minecraft:acacia_door",
        "minecraft:black_stained_glass_pane": "minecraft:orange_stained_glass_pane",
    },
    "savanna_tavern": {
        "minecraft:polished_granite": "minecraft:orange_terracotta",
        "minecraft:oak_planks": "minecraft:acacia_planks",
        "minecraft:oak_log": "minecraft:stripped_acacia_log",
        "minecraft:spruce_stairs": "minecraft:acacia_stairs",
        "minecraft:cobblestone": "minecraft:mud_bricks",
        "minecraft:mossy_cobblestone": "minecraft:packed_mud",
        "minecraft:spruce_fence": "minecraft:acacia_fence",
        "minecraft:spruce_pressure_plate": "minecraft:acacia_pressure_plate",
        "minecraft:spruce_door": "minecraft:acacia_door",
        "minecraft:black_stained_glass_pane": "minecraft:yellow_stained_glass_pane",
    },
    "snowy_lodge": {
        "minecraft:polished_granite": "minecraft:polished_diorite",
        "minecraft:oak_planks": "minecraft:spruce_planks",
        "minecraft:oak_log": "minecraft:stripped_spruce_log",
        "minecraft:cobblestone": "minecraft:stone_bricks",
        "minecraft:mossy_cobblestone": "minecraft:packed_ice",
        "minecraft:black_stained_glass_pane": "minecraft:light_blue_stained_glass_pane",
    },
    "taiga_alehouse": {
        "minecraft:polished_granite": "minecraft:polished_andesite",
        "minecraft:oak_planks": "minecraft:spruce_planks",
        "minecraft:oak_log": "minecraft:stripped_spruce_log",
        "minecraft:spruce_stairs": "minecraft:dark_oak_stairs",
        "minecraft:black_stained_glass_pane": "minecraft:green_stained_glass_pane",
    },
}


def replace_block_name(state: str, replacements: dict[str, str]) -> str:
    name, properties = parse_state(state)
    replaced = replacements.get(name, name)
    if not properties:
        return replaced
    serialized = ",".join(f"{key}={value}" for key, value in properties.items())
    return f"{replaced}[{serialized}]"


def custom_tavern(name: str, blueprint: dict[str, Any]) -> Template:
    size = tuple(blueprint["size"])
    if len(size) != 3:
        raise ValueError("Custom tavern blueprint must have three size values")
    template = Template(name, size)
    station = tuple(blueprint["station"])
    replacements = REGIONAL_PALETTES[name]
    for state, positions in blueprint["states"].items():
        for raw_pos in positions:
            pos = tuple(raw_pos)
            # The builder used six station blocks as a back-bar surface. Keep
            # the central job site only; barrels retain the storage-counter
            # appearance without advertising five extra villager POIs.
            if state == "trippycraft:bartender_station" and pos != station:
                output_state = "minecraft:barrel[facing=up,open=false]"
            else:
                output_state = replace_block_name(state, replacements)
            template.set(*pos, output_state)
    for pos, state in RAVE_FIXTURES.items():
        template.set(*pos, state)
    return template


def main() -> None:
    with BLUEPRINT.open("r", encoding="utf-8") as source:
        blueprint = json.load(source)
    templates = [custom_tavern(name, blueprint) for name in REGIONAL_PALETTES]
    for template in templates:
        path = OUTPUT / f"{template.name}.nbt"
        write_root(path, template.to_nbt())
        print(f"generated {path.relative_to(ROOT)} ({path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
