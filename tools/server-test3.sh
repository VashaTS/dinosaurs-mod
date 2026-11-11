#!/usr/bin/env bash
# 1.1.0 smoke test: new loot tables, recipes/advancements parse, saddled Triceratops NBT round trip, config file.
# EXTRA_ARGS="-PwithTrinkets=true" tools/server-test3.sh   -> same with Trinkets Updated loaded
set -u
cd "$(dirname "$0")/.."
LOG=/tmp/dino-server3.log; FIFO=/tmp/dino-in3
rm -rf "$FIFO" run/world; mkfifo "$FIFO"
mkdir -p run; echo "eula=true" > run/eula.txt
printf "online-mode=false\ndifficulty=normal\nlevel-seed=12345\nmax-tick-time=-1\npause-when-empty-seconds=-1\n" > run/server.properties
exec 3<>"$FIFO"
./gradlew --no-daemon runServer ${EXTRA_ARGS:-} <&3 > "$LOG" 2>&1 &
cmd() { echo "$1" >&3; sleep "${2:-0.3}"; }
for i in $(seq 1 300); do grep -q "Done (" "$LOG" && break; grep -q "BUILD FAILED" "$LOG" && { echo FAILED; tail -40 "$LOG"; exit 1; }; sleep 1; done
cmd "forceload add -16 -16 16 16" 4
for m in t_rex velociraptor megalodon brachiosaurus parasaurolophus; do cmd "loot spawn 0 200 0 loot dinosaurs:entities/$m" 0.5; done
cmd "execute if entity @e[type=item,nbt={Item:{id:\"dinosaurs:t_rex_tooth\"}}]" 0.5
cmd "execute if entity @e[type=item,nbt={Item:{id:\"dinosaurs:megalodon_tooth\"}}]" 0.5
cmd "execute if entity @e[type=item,nbt={Item:{id:\"dinosaurs:brachiosaurus_hide\"}}]" 0.5
cmd "kill @e[type=item]" 1
cmd "execute positioned 0 0 0 positioned over motion_blocking_no_leaves run summon dinosaurs:triceratops ~ ~ ~ {Saddled:1b,CustomName:\"Trike\"}" 1
cmd "execute positioned 0 0 0 positioned over motion_blocking_no_leaves run summon dinosaurs:triceratops ~2 ~ ~" 1
cmd "data get entity @e[type=dinosaurs:triceratops,name=Trike,limit=1] Saddled" 1
cmd "save-all flush" 4
cmd "kill @e[type=dinosaurs:triceratops,name=Trike]" 2
cmd "execute if entity @e[type=item,nbt={Item:{id:\"dinosaurs:dinosaur_saddle\"}}]" 1
cmd "recipe give @a dinosaurs:amulet_of_dino_protection" 0.5
cmd "advancement grant @a only dinosaurs:amulet" 0.5
cmd "give @a dinosaurs:amulet_of_dino_protection" 0.5
cmd "stop" 15
grep -E "System chat|Exception|ERROR|Caused by|Couldn't|Parsing|dinosaurs" "$LOG" | grep -v "Can't keep up" | tail -60
echo "--- recipes/advancements loaded:"; grep -E "Loaded [0-9]+ (recipes|advancements)" "$LOG"
echo "--- config:"; cat run/config/dinosaurs.json
