"""Guide book texts (English, Polish). Imported by gen_data.py. Spawn facts mirror ModEntities.java."""

BOOK_LANG = {
    "item.dinosaurs.guide_book.desc": ("Right-click to read about every dinosaur", "Kliknij PPM, by poczytać o dinozaurach"),
    "book.dinosaurs.title_heading": ("Dinosaur Field Guide", "Przewodnik po dinozaurach"),
    "book.dinosaurs.intro": (
        "Every dinosaur: how it looks, where it lives and what it drops. Plus every recipe.",
        "Wszystkie dinozaury: jak wyglądają, gdzie żyją i co z nich wypada. Do tego receptury."),
    "book.dinosaurs.heading.book_recipe": ("How to craft", "Jak wytworzyć"),
    "book.dinosaurs.heading.index": ("Contents", "Spis treści"),
    "book.dinosaurs.index_hint": (
        "Click an item in a recipe to open its page. Drag a dinosaur picture to turn it.",
        "Kliknij przedmiot w recepturze, by otworzyć jego stronę. Przeciągnij obrazek dinozaura, by go obrócić."),
    "book.dinosaurs.category.dinosaurs": ("Dinosaurs", "Dinozaury"),
    "book.dinosaurs.category.items": ("Items", "Przedmioty"),
    "book.dinosaurs.category.settings": ("Settings", "Ustawienia"),
    "book.dinosaurs.heading.habitat": ("Where to find", "Gdzie szukać"),
    "book.dinosaurs.heading.drops": ("Drops", "Łupy"),
    "book.dinosaurs.heading.dropped_by": ("Dropped by:", "Wypada z:"),
    "book.dinosaurs.heading.crafting": ("Crafting", "Wytwarzanie"),
    "book.dinosaurs.heading.shapeless": ("Shapeless", "Bez kształtu"),
    "book.dinosaurs.heading.used_in": ("Used for", "Zastosowanie"),
    "book.dinosaurs.heading.smithing": ("Smithing Table", "Stół kowalski"),
    "book.dinosaurs.title.dinosaur_armor": ("Dinosaur Armor", "Dinozaurowa zbroja"),
    "book.dinosaurs.drag_to_rotate": ("Drag to rotate", "Przeciągnij, by obrócić"),
    "book.dinosaurs.nav.back": ("Back", "Wstecz"),
    "book.dinosaurs.nav.index": ("Contents", "Spis treści"),
    "book.dinosaurs.status.on": ("ON", "WŁĄCZONE"),
    "book.dinosaurs.status.off": ("OFF", "WYŁĄCZONE"),
    "book.dinosaurs.settings.daylight": (
        "Hostile dinosaurs can spawn in daylight. When OFF, they spawn only at night, in the dark.",
        "Wrogie dinozaury mogą pojawiać się za dnia. Gdy WYŁĄCZONE, tylko nocą, w ciemności."),
    "book.dinosaurs.settings.how": (
        "Change it in dinosaurs.json (config folder) or with Mod Menu + Cloth Config. On a server, the server's file counts.",
        "Zmień to w pliku dinosaurs.json (folder config) lub przez Mod Menu + Cloth Config. Na serwerze liczy się plik serwera."),
    "book.dinosaurs.settings.legend": ("Green: spawns. Red: turned off.", "Zielony: pojawia się. Czerwony: wyłączony."),
    "book.dinosaurs.settings.spawning": ("Natural spawning", "Pojawianie się"),
    "book.dinosaurs.stats.heading": ("Stats", "Statystyki"),
    "book.dinosaurs.stats.armor": ("Armor", "Pancerz"),
    "book.dinosaurs.stats.toughness": ("Armor Toughness", "Twardość pancerza"),
    "book.dinosaurs.stats.knockback": ("Knockback Resistance", "Odporność na odrzut"),
    "book.dinosaurs.stats.durability": ("Durability", "Trwałość"),
    "book.dinosaurs.stats.arrow_damage": ("Arrow Damage", "Obrażenia strzały"),
    "book.dinosaurs.stats.arrow_speed": ("Arrow Speed", "Prędkość strzały"),
    "book.dinosaurs.stats.draw_ticks": ("Draw (ticks)", "Naciąganie (ticki)"),
    "book.dinosaurs.stats.enchantability": ("Enchantability", "Zaklinalność"),
    "book.dinosaurs.stats.enchanting": ("Enchanting", "Zaklinanie"),
    "book.dinosaurs.stats.attack_damage": ("Damage", "Obrażenia"),
    "book.dinosaurs.stats.attack_speed": ("Speed", "Szybkość"),

    # ---- Dinosaurs: behaviour (left page) and where to find them (right page)
    "book.dinosaurs.dino.t_rex": (
        "Hostile. The king of predators: huge, tough and hits very hard. Attacks players on sight.",
        "Wrogi. Król drapieżników: ogromny, wytrzymały i bije bardzo mocno. Atakuje graczy od razu."),
    "book.dinosaurs.habitat.t_rex": (
        "Plains, Sunflower Plains, Forest and Dark Forest. Always alone and rare: at most one in a 64-block area. Needs open sky. Never on Peaceful.",
        "Równiny, Słonecznikowe równiny, Lasy i Gęsty las. Zawsze sam i rzadki: najwyżej jeden na 64 bloki. Potrzebuje otwartego nieba. Nie na poziomie Pokojowym."),
    "book.dinosaurs.dino.velociraptor": (
        "Hostile. Small and very fast. Hunts in packs: hit one and the whole pack comes for you.",
        "Wrogi. Mały i bardzo szybki. Poluje w stadzie: uderz jednego, a ruszy na ciebie cała grupa."),
    "book.dinosaurs.habitat.velociraptor": (
        "Savanna, Savanna Plateau, Windswept Savanna, Badlands, Wooded Badlands and Eroded Badlands. Packs of 2-4 under open sky. Never on Peaceful.",
        "Sawanna, Płaskowyż sawanny, Wyżynna sawanna, Badlandy, Zalesione i Zerodowane badlandy. Stada po 2-4 pod otwartym niebem. Nie na poziomie Pokojowym."),
    "book.dinosaurs.dino.spinosaurus": (
        "Hostile. A giant hunter with a sail on its back. Happily wades through water to reach you.",
        "Wrogi. Olbrzymi łowca z żaglem na grzbiecie. Bez wahania wchodzi do wody, by cię dopaść."),
    "book.dinosaurs.habitat.spinosaurus": (
        "Mangrove Swamp and Beach. Always alone: at most one in a 64-block area, under open sky. Never on Peaceful.",
        "Las namorzynowy i Plaża. Zawsze sam: najwyżej jeden na 64 bloki, pod otwartym niebem. Nie na poziomie Pokojowym."),
    "book.dinosaurs.dino.megalodon": (
        "Hostile. A gigantic prehistoric shark. It only hunts players in the water, so use a boat!",
        "Wrogi. Gigantyczny prehistoryczny rekin. Poluje tylko na graczy w wodzie, więc pływaj łódką!"),
    "book.dinosaurs.habitat.megalodon": (
        "Deep Ocean, Deep Cold, Deep Lukewarm and Deep Frozen Ocean, at least 10 blocks below sea level. Very rare: one in a 96-block area. Never on Peaceful.",
        "Głęboki ocean, głęboki zimny, letni i zamarznięty ocean, min. 10 bloków pod poziomem morza. Bardzo rzadki: jeden na 96 bloków. Nie na poziomie Pokojowym."),
    "book.dinosaurs.dino.triceratops": (
        "Neutral. Leaves you alone until hit, then charges with its horns. Saddle it to ride it.",
        "Neutralny. Nie zaczepia, dopóki go nie uderzysz, wtedy szarżuje. Osiodłaj go, by na nim jeździć."),
    "book.dinosaurs.habitat.triceratops": (
        "Plains, Sunflower Plains and Meadow. Herds of 2-3 on grass, in bright light. Plant-eaters never despawn, like cows.",
        "Równiny, Słonecznikowe równiny i Górska łąka. Stada po 2-3 na trawie, w jasnym miejscu. Roślinożercy nie znikają, jak krowy."),
    "book.dinosaurs.dino.stegosaurus": (
        "Neutral. A calm plant-eater with back plates and a spiked tail. Hit one and the herd fights back.",
        "Neutralny. Spokojny roślinożerca z płytami i kolczastym ogonem. Uderzony - całe stado się broni."),
    "book.dinosaurs.habitat.stegosaurus": (
        "Forest, Flower Forest, Birch Forest and Old Growth Birch Forest. Groups of 1-3 on grass, in bright light.",
        "Lasy, Kwiecisty las, Las brzozowy i Starodrzew brzozowy. Grupy po 1-3 na trawie, w jasnym miejscu."),
    "book.dinosaurs.dino.ankylosaurus": (
        "Neutral. A living tank: very thick armor and a club tail. Fights back when attacked.",
        "Neutralny. Żywy czołg: bardzo gruby pancerz i ogon jak maczuga. Broni się, gdy go zaatakujesz."),
    "book.dinosaurs.habitat.ankylosaurus": (
        "Taiga, Old Growth Pine Taiga, Old Growth Spruce Taiga, Windswept Hills, Windswept Forest and Windswept Gravelly Hills. Groups of 1-2 on grass, in bright light.",
        "Tajga, Starodrzew sosnowy i świerkowy, Wyżyny, Wyżynny las i Żwirowe wyżyny. Grupy po 1-2 na trawie, w jasnym miejscu."),
    "book.dinosaurs.dino.brachiosaurus": (
        "Passive. The biggest dinosaur, with a very long neck. Never attacks; runs from attackers and carnivores.",
        "Pokojowy. Największy dinozaur, z bardzo długą szyją. Nie atakuje; ucieka przed napastnikami i drapieżnikami."),
    "book.dinosaurs.habitat.brachiosaurus": (
        "Jungle, Sparse Jungle and Bamboo Jungle. Groups of 1-2 on grass, in bright light.",
        "Dżungla, Przerzedzona dżungla i Las bambusowy. Grupy po 1-2 na trawie, w jasnym miejscu."),
    "book.dinosaurs.dino.parasaurolophus": (
        "Passive. A duck-billed plant-eater with a long head crest. Runs from attackers and carnivores.",
        "Pokojowy. Kaczodzioby roślinożerca z długim grzebieniem. Ucieka przed napastnikami i drapieżnikami."),
    "book.dinosaurs.habitat.parasaurolophus": (
        "Swamp and River. Herds of 2-4 on grass, in bright light.",
        "Bagna i Rzeka. Stada po 2-4 na trawie, w jasnym miejscu."),
    "book.dinosaurs.dino.pteranodon": (
        "Passive flying reptile. Glides around, then perches on the highest point nearby. Flees when hurt.",
        "Pokojowy latający gad. Szybuje, potem siada na najwyższym punkcie w okolicy. Uderzony ucieka."),
    "book.dinosaurs.habitat.pteranodon": (
        "Windswept Hills, Windswept Gravelly Hills, Stony Peaks and Jagged Peaks. Groups of 1-3 under open sky, in bright light.",
        "Wyżyny, Żwirowe wyżyny, Skaliste szczyty i Ostre szczyty. Grupy po 1-3 pod otwartym niebem, w jasnym miejscu."),

    # ---- Items
    "book.dinosaurs.item.guide_book": (
        "This book. Shapeless: Book + Leather + any dinosaur trophy (tooth, claw, hide or crest). Right-click to read.",
        "Ta książka. Wytwarzanie bez kształtu: książka + skóra + dowolne trofeum dinozaura (ząb, pazur, skóra lub grzebień). PPM, by czytać."),
    "book.dinosaurs.item.amulet_of_dino_protection": (
        "Keep it in your inventory (or the Trinkets necklace slot) and hostile dinosaurs ignore you. Attack one and it may fight back.",
        "Trzymaj go w ekwipunku (lub w slocie naszyjnika Trinkets), a wrogie dinozaury cię zignorują. Zaatakowany może oddać."),
    "book.dinosaurs.item.dinosaur_saddle": (
        "Use it on a Triceratops, then right-click to ride and steer. Sneak with an empty hand to take it off.",
        "Użyj na triceratopsie, potem PPM, by jeździć i sterować. Kucnij z pustą ręką, by je zdjąć."),
    "book.dinosaurs.item.t_rex_tooth": (
        "1-2 from every Tyrannosaurus Rex, more with Looting. Part of the amulet and of the Dinosaur Claw.",
        "1-2 z każdego tyranozaura, więcej z Grabieżą. Składnik amuletu i dinozaurowego pazura."),
    "book.dinosaurs.item.raptor_claw": (
        "Up to 1 from a Velociraptor, more with Looting. The amulet and the Dinosaur Claw need 2. Also makes Dinosaur Boots.",
        "Do 1 z welociraptora, więcej z Grabieżą. Amulet i dinozaurowy pazur potrzebują 2. Daje też dinozaurowe buty."),
    "book.dinosaurs.item.megalodon_tooth": (
        "1-3 from a Megalodon, more with Looting. Part of the amulet and of the Dinosaur Chestplate.",
        "1-3 z megalodona, więcej z Grabieżą. Składnik amuletu i dinozaurowego napierśnika."),
    "book.dinosaurs.item.brachiosaurus_hide": (
        "1-3 from a Brachiosaurus, more with Looting. Makes the saddle, or 2 Leather.",
        "1-3 z brachiozaura, więcej z Grabieżą. Z niej zrobisz siodło albo 2 skóry."),
    "book.dinosaurs.item.parasaurolophus_crest": (
        "Up to 1 from a Parasaurolophus, more with Looting. Part of the saddle and of the Dinosaur Leggings.",
        "Do 1 z parazaurolofa, więcej z Grabieżą. Składnik siodła i dinozaurowych nogawic."),
    "book.dinosaurs.item.dinosaur_bones": (
        "Large dinosaurs drop these instead of bones, more with Looting. For the Bone Bow, the Claw, the Upgrade or 4 Bone Meal.",
        "Duże dinozaury upuszczają je zamiast kości, więcej z Grabieżą. Na łuk, pazur, ulepszenie lub 4 mączki kostne."),
    "book.dinosaurs.item.dinosaur_bone_bow": (
        "Faster draw, faster arrows that hit harder. Infinity and Mending work together on it! Enchantments from the bow are kept.",
        "Szybciej naciąga, strzały lecą szybciej i mocniej biją. Nieskończoność i Naprawa działają razem! Zaklęcia z łuku zostają."),
    "book.dinosaurs.item.t_rex_skull": (
        "Rare: 4% of T. rexes killed by a player drop one, up to 10% with Looting. Craft it with Leather and a Netherite Ingot into a helmet.",
        "Rzadka: wypada z 4% tyranozaurów zabitych przez gracza, do 10% z Grabieżą. Ze skórą i sztabką netherytu zrobisz z niej hełm."),
    "book.dinosaurs.item.t_rex_skull_helmet": (
        "Wear a T. rex skull on your head. Fire resistant like netherite. Repair it with a Netherite Ingot or a T-Rex Skull.",
        "Noś czaszkę tyranozaura na głowie. Odporny na ogień jak netheryt. Napraw go sztabką netherytu lub czaszką tyranozaura."),
    "book.dinosaurs.item.dinosaur_upgrade_smithing_template": (
        "Smithing template that turns netherite chestplate, leggings and boots into Dinosaur Armor. Used up like the Netherite Upgrade. 7 Dinosaur Bones and a Bone Block copy it.",
        "Szablon kowalski, który zmienia netherytowy napierśnik, nogawice i buty w dinozaurową zbroję. Zużywa się jak netherytowe ulepszenie. 7 kości dinozaura i blok kości go powielą."),
    "book.dinosaurs.item.dinosaur_claw": (
        "A sword with a sickle claw: hits harder than netherite, sweeps, takes sword enchantments. Fire resistant. Repair: Dinosaur Bones.",
        "Miecz z sierpowatym pazurem: bije mocniej niż netheryt, tnie zamaszyście, zaklęcia miecza. Nie płonie. Naprawa: kości dinozaura."),
    "book.dinosaurs.item.dinosaur_armor": (
        "Enchantments stay. Better than netherite in every stat. Repair: Dinosaur Bones. Helmet: T-Rex Skull Helmet.",
        "Zaklęcia zostają. Lepsza od netherytu we wszystkim. Naprawa: kości dinozaura. Hełm: z czaszki tyranozaura."),
}
