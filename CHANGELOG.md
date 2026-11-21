# Changelog

## 1.5.1

* **Xaero's Minimap / Better PvP**: every dinosaur (including the Megalodon and the Pteranodon) now has a head icon on
  the entity radar, like vanilla mobs, instead of its name. Before, Xaero could not find the head of the T-Rex,
  Velociraptor, Spinosaurus, Brachiosaurus and Parasaurolophus (it hangs off the neck) and fell back to the name.
  The icons are bundled resource files (`assets/xaerominimap/entity/icon/definition/dinosaurs/*.json`) that render the
  head of each dinosaur's own model; long snouts and crests are turned three-quarter and the sizes are evened out.
  No dependency: without Xaero the files are simply unused. Tested with Xaero's Minimap 26.6.2.

## 1.5.0

* New: **Dinosaur Armor** (chestplate, leggings, boots), the next step after netherite, made on the smithing table.
  * **Dinosaur Upgrade** smithing template: Netherite Upgrade surrounded by 8 Dinosaur Bones. Used up like vanilla templates;
    duplicate it with 7 Dinosaur Bones + a Bone Block (template on top, bone block in the middle) for 2.
  * Smithing: Dinosaur Upgrade + Netherite Chestplate + Megalodon Tooth, + Netherite Leggings + Parasaurolophus Crest,
    + Netherite Boots + Raptor Claw. Keeps enchantments, name, trim and damage.
  * Above netherite by what netherite is above diamond: toughness 4 (3 / 2), knockback resistance 0.2 (0.1 / 0),
    durability x41 (x37 / x33: 656 / 615 / 533), enchantability 20 (15 / 10); armor points 10 / 8 / 4 (netherite 8 / 6 / 3); full set with the skull helmet: 26 armor.
    Fire resistant, all armor enchantments and trims, repaired with Dinosaur Bones.
  * Bony worn textures (ribs, sternum, vertebrae, bone shoulder plates, knee caps, toe claws) with dark netherite accents;
    item icons; the smithing table shows armor and trophy icons in the empty slots.
* **T-Rex Skull Helmet** is the set's helmet: toughness 3.5 -> 4, knockback resistance 0.15 -> 0.2, enchantability 15 -> 20
  (armor 4 and durability 506 unchanged), so it is not below the rest of the set.
* Guide book: Dinosaur Armor page (stats table against netherite, the three smithing recipes), Dinosaur Upgrade page
  (both recipes); Raptor Claw, Parasaurolophus Crest, Megalodon Tooth and Dinosaur Bones pages show what they upgrade.
  Smithing recipes are drawn as a smithing-table row. English and Polish.
* Drop rates unchanged: Raptor Claw 0-1 (Velociraptor), Parasaurolophus Crest 0-1, Megalodon Tooth 1-3, +1 per Looting level.

## 1.4.1

* Crafting the **Dinosaur Bone Bow** from an enchanted bow now keeps the bow's enchantments, custom name, repair cost and
  other data. Damage carries over as the same fraction of durability (e.g. 100/384 damaged becomes 300/1152), so crafting never repairs.
  Same shaped recipe (new recipe type `dinosaurs:crafting_shaped_keep_components`); the crafting result shows the enchanted bow.
* Guide book bow page: "Enchantments from the bow are kept". English and Polish.

## 1.4.0

* New item: **Dinosaur Bones**. The seven large dinosaurs drop them instead of vanilla bones (T. rex 2-4,
  Spinosaurus 2-4, Brachiosaurus 2-5, Triceratops / Stegosaurus / Ankylosaurus 1-3, Parasaurolophus 1-2; +1 per Looting level).
  Velociraptor, Pteranodon and Megalodon keep vanilla bones. 1 Dinosaur Bones = 4 bone meal.
* New item: **Dinosaur Bone Bow**. Shaped recipe: Bow in the middle, Dinosaur Bones left, right and on top, Netherite Ingot below.
  Full draw in 15 ticks (bow: 20), arrows 1.5x faster (4.5 vs 3; full-charge damage 9 vs 6), 1152 durability (bow: 384),
  enchantability 15, fire resistant, all bow enchantments, repaired with Dinosaur Bones. Own pull animation textures and FOV zoom.
* **Infinity and Mending work together on the Dinosaur Bone Bow** (anvil and `/enchant`). Other bows keep the vanilla rule.
* Guide book: pages for Dinosaur Bones (with the dinosaurs that drop them) and the bow (stats table against the vanilla bow);
  dinosaur drop lists updated. English and Polish.

## 1.3.0

* New rare drop: **T-Rex Skull**. Only when a player kills the T. rex: 4% chance, 6/8/10% with Looting I/II/III.
* New item: **T-Rex Skull Helmet**. Shapeless recipe: T-Rex Skull + Leather + Netherite Ingot.
  When worn it is a 3D T. rex skull on your head (snout, teeth, eye sockets), on players, armor stands and mobs.
  Better than netherite: 4 armor, 3.5 toughness, 0.15 knockback resistance, 506 durability (netherite: 3 / 3 / 0.1 / 407),
  enchantability 15, fire resistant, takes helmet enchantments, repaired with a Netherite Ingot or a T-Rex Skull.
* Guide book: pages for the skull and the helmet (with a stats table against the netherite helmet),
  and the skull is listed in the T. rex drops. English and Polish.

## 1.2.0

* New item: **Dinosaur Field Guide**, an illustrated guide book. Shapeless recipe: Book + Leather + any dinosaur trophy
  (T-Rex Tooth, Raptor Claw, Megalodon Tooth, Brachiosaurus Hide or Parasaurolophus Crest; tag `#dinosaurs:guide_book_trophies`).
  Book + Leather alone stays free for other mods' guide books.
* The book has a clickable table of contents, a page for each of the 10 dinosaurs with a 3D picture (drag to rotate),
  behaviour, biomes and spawn conditions and drops, every item with its crafting grid (click an ingredient to open its page),
  and the current spawn settings. Back, contents and page-turn buttons. English and Polish.

## 1.1.0

* New drops: T-Rex Tooth (T. rex), Raptor Claw (Velociraptor), Megalodon Tooth (Megalodon),
  Brachiosaurus Hide (Brachiosaurus), Parasaurolophus Crest (Parasaurolophus).
* New item: **Amulet of Dino Protection** (string, gold ingot, 2 raptor claws, T-Rex tooth, megalodon tooth).
  Hostile dinosaurs ignore a player carrying it until that player attacks them; the attacked dinosaur may then fight back.
* Optional **Trinkets** support (Trinkets Updated): the amulet fits the chest/necklace slot.
* New item: **Dinosaur Saddle** (2 brachiosaurus hide, leather, 2 string, parasaurolophus crest, iron ingot).
  Use it on a Triceratops to saddle it, then ride and steer it. Saddle is visible on the mob, saved, and dropped on death;
  sneak + empty hand removes it.
* **Config** (`config/dinosaurs.json`): disable hostile dinosaur spawns in daylight, toggle natural spawning per species.
  In-game screen with Mod Menu + Cloth Config (both optional).
* Two advancements, recipe book unlocks, English and Polish translations for everything new.

## 1.0.0

* First release: 10 prehistoric mobs with their own biomes, spawn eggs, English and Polish.
