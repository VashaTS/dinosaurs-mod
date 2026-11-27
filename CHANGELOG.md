# Changelog

## 1.7.2

* **Armor trims now show on the Dinosaur Armor icons.** The Dinosaur Chestplate, Leggings and Boots already took a trim on
  the smithing table and showed it when worn, but the item icon stayed the same. They now have trimmed item models like
  vanilla armor: the trim pattern shows in the trim material's colour in the inventory, hotbar and smithing table
  preview (every vanilla trim material).
* The **T-Rex Skull Helmet** can no longer be trimmed. It is worn as its 3D skull model, so a trim could never show on it
  (the smithing table took the trim and nothing changed). It still takes every helmet enchantment (Respiration, Aqua
  Affinity, Protection, Thorns, Unbreaking, Mending, Curse of Binding...). Helmets trimmed before 1.7.2 keep their
  (invisible) trim.

## 1.7.1

* Fix: the **Megalodon disappeared in front of the player** when they swam up to it.
  * Rendering: the game hid the whole shark as soon as its hitbox (5 blocks) left the screen, but the model is 14 blocks
    long, so looking at its head or tail up close made it vanish. Dinosaurs are now culled with a box that covers the
    whole model (all species, the long-necked and long-tailed ones had the same issue to a smaller degree).
  * Despawning: like every water mob, a Megalodon could despawn at random once no player had been within 32 blocks for
    30 seconds, even while a player could see it and was swimming towards it (with the Amulet of Dino Protection it does
    not come at you, so this was easy to hit). It now never despawns at random while a player is within 96 blocks; it
    still despawns at once more than 128 blocks away, and name-tagged ones stay as before.

## 1.7.0

* New item: **Dino Collar**. Shaped recipe: T-Rex Tooth ×3 (top row) / Leather, Dinosaur Bones, Leather / Iron Chain
  (bottom middle). A player carrying the **Amulet of Dino Protection** (inventory or Trinkets necklace slot) uses it on a
  **T. rex**: the collar is used up and the player becomes the T. rex's owner (saved as the `Owner` UUID). A collared T. rex
  * wears the collar visibly (studded leather band with tooth spikes and an iron ring on its neck);
  * follows its owner like a tamed wolf and teleports next to them when more than 12 blocks behind;
  * attacks whatever its owner attacks and whatever attacks its owner (wolf rules: not creepers, not the owner's other
    pets, not players the owner may not hurt), and never attacks its owner;
  * is neutral to other players: it no longer hunts them, only players who attack its owner (or the T. rex itself);
  * never despawns (also not on Peaceful) and does not stop players from sleeping nearby;
  * owner: sneak + use with an empty hand to make it sit / follow again (it stands up when hurt); any meat heals it;
  * drops its collar when it dies; the owner gets the death message.
  * Without the amulet the collar does nothing (message on the action bar); a collared T. rex keeps its first owner.
* **Megalodon is twice as big**: hitbox 5 × 3.6 blocks (was 2.5 × 1.8), model and render scale doubled; it needs
  4 blocks of water to spawn. Guide book picture rescaled; the Xaero's Minimap head icon is unchanged.
* **Spawn frequency settings** (config file and the Cloth Config screen, new *Spawn frequency* tab): a spawn weight per
  species (0-100, defaults = the previous built-in weights) and a global percentage (0-500%, default 100%). They go into
  the biome spawn lists when a world loads, so they apply after re-joining the world / restarting the server (said in
  the tooltips and in the screen). The on/off switches still apply immediately.
* Guide book: Dino Collar page with its recipe; the T-Rex Tooth page mentions it. The item list uses tighter rows (names
  longer than two lines shortened, full name in the tooltip) so it still fits on its two pages. English and Polish.
* Advancement *Who's a Good Rex?* (collar a T. rex) and the collar's recipe advancement (unlocked by a T-Rex Tooth).

## 1.6.0

* New weapon: **Dinosaur Claw**, a clawed gauntlet: a netherite and leather glove with bone knuckle plates and three
  sharp claws sticking out of the knuckles. Flat icon in the inventory; in the hands a 3D model worn on the fist
  (first and third person, either hand).
  * Shaped recipe: Raptor Claw, T-Rex Tooth, Raptor Claw / Netherite Ingot, Dinosaur Bones, Netherite Ingot / Stick (bottom middle).
  * **11 attack damage** (netherite sword 8), attack speed 1.6 (same), durability 2501 (netherite 2031), enchantability 20 (15).
  * **Shorter reach**: 2.5 blocks instead of 3 while in the main hand (`entity_interaction_range` -0.5, which vanilla
    uses as the attack reach of weapons without an `attack_range` component). Shown in the tooltip.
  * Fights like a sword (`#minecraft:swords`): sweeping attack, all sword enchantments, cuts cobwebs. Fire resistant,
    repaired with Dinosaur Bones.
* Guide book: Dinosaur Claw page with its stats (damage, speed, reach, durability, enchanting) against the netherite
  sword and the recipe; the Raptor Claw and T-Rex Tooth pages show the recipe, the Dinosaur Bones page mentions it.
  English and Polish.

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
