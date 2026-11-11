#!/usr/bin/env python3
"""Generates lang files, loot tables and spawn egg item models."""
import json, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/dinosaurs")
D = os.path.join(ROOT, "src/main/resources/data/dinosaurs")

MOBS = {  # id: (English, Polish, bones min, bones max, bone meal max)
    "t_rex": ("Tyrannosaurus Rex", "Tyranozaur", 4, 8, 3),
    "velociraptor": ("Velociraptor", "Welociraptor", 0, 2, 0),
    "spinosaurus": ("Spinosaurus", "Spinozaur", 4, 7, 2),
    "triceratops": ("Triceratops", "Triceratops", 2, 5, 1),
    "stegosaurus": ("Stegosaurus", "Stegozaur", 2, 5, 1),
    "ankylosaurus": ("Ankylosaurus", "Ankylozaur", 2, 5, 1),
    "brachiosaurus": ("Brachiosaurus", "Brachiozaur", 5, 9, 3),
    "parasaurolophus": ("Parasaurolophus", "Parazaurolof", 1, 3, 0),
    "megalodon": ("Megalodon", "Megalodon", 3, 6, 2),
    "pteranodon": ("Pteranodon", "Pteranodon", 0, 2, 0),
}

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write("\n")

def item_entry(name, lo, hi, modifier_key):
    return {"type": "minecraft:item", "name": name, modifier_key: [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
        {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0}, "enchantment": "minecraft:looting"},
    ]}

def main(modifier_key):
    en = {"itemGroup.dinosaurs.dinosaurs": "Dinosaurs"}
    pl = {"itemGroup.dinosaurs.dinosaurs": "Dinozaury"}
    for mid, (e, p, lo, hi, bm) in MOBS.items():
        en[f"entity.dinosaurs.{mid}"] = e
        pl[f"entity.dinosaurs.{mid}"] = p
        en[f"item.dinosaurs.{mid}_spawn_egg"] = f"{e} Spawn Egg"
        pl[f"item.dinosaurs.{mid}_spawn_egg"] = f"Jajo przywołania: {p}"
        pools = [{"rolls": 1, "entries": [item_entry("minecraft:bone", lo, hi, modifier_key)]}]
        if bm:
            pools.append({"rolls": 1, "entries": [item_entry("minecraft:bone_meal", 0, bm, modifier_key)]})
        w(f"{D}/loot_table/entities/{mid}.json", {"type": "minecraft:entity", "pools": pools, "random_sequence": f"dinosaurs:entities/{mid}"})
        w(f"{A}/items/{mid}_spawn_egg.json", {"model": {"type": "minecraft:model", "model": f"dinosaurs:item/{mid}_spawn_egg"}})
        w(f"{A}/models/item/{mid}_spawn_egg.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"dinosaurs:item/{mid}_spawn_egg"}})
    w(f"{A}/lang/en_us.json", en)
    w(f"{A}/lang/pl_pl.json", pl)

if __name__ == "__main__":
    import sys
    main(sys.argv[1] if len(sys.argv) > 1 else "modifier")
