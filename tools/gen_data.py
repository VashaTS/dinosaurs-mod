#!/usr/bin/env python3
"""Generates lang files, loot tables, item models, recipes, advancements and Trinkets data."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from book_lang import BOOK_LANG
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
    # 26.3+: "modifier": [{"type": ...}]   26.2: "functions": [{"function": ...}]
    tkey = "type" if modifier_key == "modifier" else "function"
    return {"type": "minecraft:item", "name": name, modifier_key: [
        {tkey: "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
        {tkey: "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0}, "enchantment": "minecraft:looting"},
    ]}

# Unique drops: mob -> (item, min, max). Count is uniform(min, max) + up to 1 per Looting level.
DROPS = {
    "t_rex": ("t_rex_tooth", 1, 2),
    "velociraptor": ("raptor_claw", 0, 1),
    "megalodon": ("megalodon_tooth", 1, 3),
    "brachiosaurus": ("brachiosaurus_hide", 1, 3),
    "parasaurolophus": ("parasaurolophus_crest", 0, 1),
}

# Rare drops (killed by a player only): mob -> (item, chance without Looting, chance with Looting I, + per extra level).
# T-Rex Skull: 4% normally; Looting I/II/III: 6/8/10%.
RARE_DROPS = {
    "t_rex": ("t_rex_skull", 0.04, 0.06, 0.02),
}

# Items with a hand-made 3D model (written by gen_skull.py), not a flat item/generated one.
CUSTOM_MODELS = {"t_rex_skull", "t_rex_skull_helmet"}

def rare_pool(item, base, looting, per_level, modifier_key):
    chance = {"enchanted_chance": {"type": "minecraft:linear", "base": looting, "per_level_above_first": per_level},
              "enchantment": "minecraft:looting", "unenchanted_chance": base}
    entries = [{"type": "minecraft:item", "name": f"dinosaurs:{item}"}]
    if modifier_key == "modifier":  # 26.3: one "condition" object
        return {"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [
            {"type": "minecraft:killed_by_player"}, {"type": "minecraft:random_chance_with_enchanted_bonus", **chance}]}, "entries": entries}
    return {"rolls": 1, "conditions": [  # 26.2: list of "conditions"
        {"condition": "minecraft:killed_by_player"}, {"condition": "minecraft:random_chance_with_enchanted_bonus", **chance}], "entries": entries}

ITEMS = {  # id: (English, Polish)
    "t_rex_tooth": ("T-Rex Tooth", "Ząb tyranozaura"),
    "raptor_claw": ("Raptor Claw", "Pazur raptora"),
    "megalodon_tooth": ("Megalodon Tooth", "Ząb megalodona"),
    "amulet_of_dino_protection": ("Amulet of Dino Protection", "Amulet ochrony przed dinozaurami"),
    "brachiosaurus_hide": ("Brachiosaurus Hide", "Skóra brachiozaura"),
    "parasaurolophus_crest": ("Parasaurolophus Crest", "Grzebień parazaurolofa"),
    "dinosaur_saddle": ("Dinosaur Saddle", "Siodło na dinozaura"),
    "guide_book": ("Dinosaur Field Guide", "Przewodnik po dinozaurach"),
    "t_rex_skull": ("T-Rex Skull", "Czaszka tyranozaura"),
    "t_rex_skull_helmet": ("T-Rex Skull Helmet", "Hełm z czaszki tyranozaura"),
    "tab_icon": ("Dinosaurs", "Dinozaury"),
}

EXTRA_LANG = {  # key: (English, Polish)
    "item.dinosaurs.amulet_of_dino_protection.desc": ("Hostile dinosaurs ignore you until you attack them", "Wrogie dinozaury ignorują cię, dopóki ich nie zaatakujesz"),
    "item.dinosaurs.dinosaur_saddle.desc": ("Use on a Triceratops to ride it", "Użyj na triceratopsie, aby na nim jeździć"),
    "item.dinosaurs.t_rex_skull.desc": ("Rare T. rex trophy. Craft it into a helmet", "Rzadkie trofeum z tyranozaura. Zrób z niej hełm"),
    "item.dinosaurs.t_rex_skull_helmet.desc": ("Stronger than netherite. Fire resistant", "Mocniejszy niż netheryt. Odporny na ogień"),
    "config.dinosaurs.title": ("Dinosaurs", "Dinozaury"),
    "config.dinosaurs.category.spawning": ("Spawning", "Pojawianie się"),
    "config.dinosaurs.hostile_daylight": ("Hostile dinosaurs spawn in daylight", "Wrogie dinozaury pojawiają się za dnia"),
    "config.dinosaurs.hostile_daylight.tooltip": (
        "When OFF, T-Rex, Velociraptor, Spinosaurus and Megalodon only spawn at night, in darkness (light level 7 or less).",
        "Gdy WYŁĄCZONE, tyranozaur, welociraptor, spinozaur i megalodon pojawiają się tylko w nocy, w ciemności (poziom światła 7 lub mniej)."),
    "config.dinosaurs.spawn": ("Spawn: %s", "Pojawianie się: %s"),
    "config.dinosaurs.spawn.tooltip": (
        "Natural spawning of this dinosaur. Spawn eggs, spawners and /summon always work.",
        "Naturalne pojawianie się tego dinozaura. Jaja przywołania, spawnery i /summon działają zawsze."),
    "advancements.dinosaurs.amulet.title": ("Dino Whisperer", "Zaklinacz dinozaurów"),
    "advancements.dinosaurs.amulet.description": ("Get an Amulet of Dino Protection", "Zdobądź amulet ochrony przed dinozaurami"),
    "advancements.dinosaurs.ride_triceratops.title": ("Three Horns, One Rider", "Trzy rogi, jeden jeździec"),
    "advancements.dinosaurs.ride_triceratops.description": ("Ride a saddled Triceratops", "Pojedź na osiodłanym triceratopsie"),
    "modmenu.descriptionTranslation.dinosaurs": (
        "Adds well-known dinosaurs as new mobs. Each species spawns in its own biomes; carnivores are hostile, herbivores are passive or neutral. Craft an Amulet of Dino Protection, a saddle to ride a Triceratops and a T-Rex Skull Helmet.",
        "Dodaje znane dinozaury jako nowe moby. Każdy gatunek pojawia się w swoich biomach; mięsożercy są wrodzy, roślinożercy pokojowi lub neutralni. Wytwórz amulet ochrony przed dinozaurami, siodło, by jeździć na triceratopsie, i hełm z czaszki tyranozaura."),
}

RECIPES = {
    "amulet_of_dino_protection": dict(pattern=["SGS", "RTR", " M "], key={"S": "minecraft:string", "G": "minecraft:gold_ingot", "R": "dinosaurs:raptor_claw", "T": "dinosaurs:t_rex_tooth", "M": "dinosaurs:megalodon_tooth"}, category="equipment", unlock="dinosaurs:t_rex_tooth"),
    "dinosaur_saddle": dict(pattern=["HLH", "SCS", " I "], key={"H": "dinosaurs:brachiosaurus_hide", "L": "minecraft:leather", "S": "minecraft:string", "C": "dinosaurs:parasaurolophus_crest", "I": "minecraft:iron_ingot"}, category="equipment", unlock="dinosaurs:brachiosaurus_hide"),
}

SHAPELESS = {  # id: (ingredients, result, count, unlock item or #tag[, category])
    "leather_from_brachiosaurus_hide": (["dinosaurs:brachiosaurus_hide"], "minecraft:leather", 2, "dinosaurs:brachiosaurus_hide"),
    # Book + Leather alone is left free for other mods' guide books; the dinosaur trophy makes this one unique.
    "guide_book": (["minecraft:book", "minecraft:leather", "#dinosaurs:guide_book_trophies"], "dinosaurs:guide_book", 1, "#dinosaurs:guide_book_trophies"),
    # No vanilla or other dinosaurs recipe uses skull + leather + netherite ingot (checked by the server game test).
    "t_rex_skull_helmet": (["dinosaurs:t_rex_skull", "minecraft:leather", "minecraft:netherite_ingot"], "dinosaurs:t_rex_skull_helmet", 1, "dinosaurs:t_rex_skull", "equipment"),
}

# Any unique dinosaur drop works as the third guide-book ingredient.
TROPHIES = ["dinosaurs:parasaurolophus_crest", "dinosaurs:brachiosaurus_hide", "dinosaurs:raptor_claw", "dinosaurs:t_rex_tooth", "dinosaurs:megalodon_tooth"]

def riding_predicate(modifier_key):
    if modifier_key == "modifier":  # 26.3
        return {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:vehicle": {"minecraft:entity_type": "dinosaurs:triceratops"}}}
    return [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:vehicle": {"minecraft:entity_type": "dinosaurs:triceratops"}}}]

def data_files(modifier_key):
    for rid, r in RECIPES.items():
        w(f"{D}/recipe/{rid}.json", {"type": "minecraft:crafting_shaped", "category": r["category"], "key": r["key"], "pattern": r["pattern"], "result": {"id": f"dinosaurs:{rid}"}})
        w(f"{D}/advancement/recipes/equipment/{rid}.json", {
            "parent": "minecraft:recipes/root",
            "criteria": {
                "has_ingredient": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": r["unlock"]}]}},
                "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {("recipes" if modifier_key == "modifier" else "recipe"): f"dinosaurs:{rid}"}},
            },
            "requirements": [["has_the_recipe", "has_ingredient"]],
            "rewards": {"recipes": [f"dinosaurs:{rid}"]},
        })
    for rid, (ingredients, result, count, unlock, *rest) in SHAPELESS.items():
        category = rest[0] if rest else "misc"
        res = {"id": result, "count": count} if count != 1 else {"id": result}
        w(f"{D}/recipe/{rid}.json", {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients, "result": res})
        w(f"{D}/advancement/recipes/{category}/{rid}.json", {
            "parent": "minecraft:recipes/root",
            "criteria": {
                "has_ingredient": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": unlock}]}},
                "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {("recipes" if modifier_key == "modifier" else "recipe"): f"dinosaurs:{rid}"}},
            },
            "requirements": [["has_the_recipe", "has_ingredient"]],
            "rewards": {"recipes": [f"dinosaurs:{rid}"]},
        })
    w(f"{D}/tags/item/guide_book_trophies.json", {"replace": False, "values": TROPHIES})
    # The skull helmet: repaired with netherite or another skull, enchantable like any helmet.
    w(f"{D}/tags/item/repairs_t_rex_skull_helmet.json", {"replace": False, "values": ["minecraft:netherite_ingot", "dinosaurs:t_rex_skull"]})
    w(os.path.join(ROOT, "src/main/resources/data/minecraft/tags/item/head_armor.json"), {"replace": False, "values": ["dinosaurs:t_rex_skull_helmet"]})
    w(f"{D}/advancement/amulet.json", {
        "parent": "minecraft:adventure/root",
        "criteria": {"amulet": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "dinosaurs:amulet_of_dino_protection"}]}}},
        "display": {"icon": {"id": "dinosaurs:amulet_of_dino_protection"}, "title": {"translate": "advancements.dinosaurs.amulet.title"}, "description": {"translate": "advancements.dinosaurs.amulet.description"}, "frame": "goal"},
        "requirements": [["amulet"]],
    })
    w(f"{D}/advancement/ride_triceratops.json", {
        "parent": "minecraft:husbandry/root",
        "criteria": {"ride": {"trigger": "minecraft:started_riding", "conditions": {"player": riding_predicate(modifier_key)}}},
        "display": {"icon": {"id": "dinosaurs:dinosaur_saddle"}, "title": {"translate": "advancements.dinosaurs.ride_triceratops.title"}, "description": {"translate": "advancements.dinosaurs.ride_triceratops.description"}},
        "requirements": [["ride"]],
    })
    # Trinkets / Trinkets Updated (optional): the amulet fits the necklace slot, which we enable for players.
    T = os.path.join(ROOT, "src/main/resources/data/trinkets")
    w(f"{T}/tags/item/chest/necklace.json", {"replace": False, "values": ["dinosaurs:amulet_of_dino_protection"]})
    w(f"{T}/entities/dinosaurs.json", {"entities": ["player"], "slots": ["chest/necklace"]})

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
        if mid in DROPS:
            item, dlo, dhi = DROPS[mid]
            pools.append({"rolls": 1, "entries": [item_entry(f"dinosaurs:{item}", dlo, dhi, modifier_key)]})
        if mid in RARE_DROPS:
            pools.append(rare_pool(*RARE_DROPS[mid], modifier_key))
        w(f"{D}/loot_table/entities/{mid}.json", {"type": "minecraft:entity", "pools": pools, "random_sequence": f"dinosaurs:entities/{mid}"})
        w(f"{A}/items/{mid}_spawn_egg.json", {"model": {"type": "minecraft:model", "model": f"dinosaurs:item/{mid}_spawn_egg"}})
        w(f"{A}/models/item/{mid}_spawn_egg.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"dinosaurs:item/{mid}_spawn_egg"}})
    for iid, (e, p) in ITEMS.items():
        en[f"item.dinosaurs.{iid}"] = e
        pl[f"item.dinosaurs.{iid}"] = p
        w(f"{A}/items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"dinosaurs:item/{iid}"}})
        if iid not in CUSTOM_MODELS:
            w(f"{A}/models/item/{iid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"dinosaurs:item/{iid}"}})
    for k, (e, p) in list(EXTRA_LANG.items()) + list(BOOK_LANG.items()):
        en[k] = e
        pl[k] = p
    data_files(modifier_key)
    w(f"{A}/lang/en_us.json", en)
    w(f"{A}/lang/pl_pl.json", pl)

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "functions")  # 26.2 uses "functions", 26.3 uses "modifier"
