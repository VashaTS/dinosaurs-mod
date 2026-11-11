#!/usr/bin/env bash
# Headless smoke test: starts the dev server, summons every mob, force-loads chunks in each
# home biome to trigger world-generation spawns, then counts entities and stops the server.
set -u
cd "$(dirname "$0")/.."
LOG=/tmp/dino-server.log; FIFO=/tmp/dino-in
rm -f "$FIFO" run/world -r; mkfifo "$FIFO"
mkdir -p run; echo "eula=true" > run/eula.txt
printf "online-mode=false\ndifficulty=normal\nlevel-seed=${SEED:-12345}\nmax-tick-time=-1\npause-when-empty-seconds=-1\n" > run/server.properties
exec 3<>"$FIFO"
./gradlew --no-daemon runServer <&3 > "$LOG" 2>&1 &
cmd() { echo "$1" >&3; sleep "${2:-0.3}"; }
for i in $(seq 1 300); do grep -q "Done (" "$LOG" && break; grep -q "BUILD FAILED" "$LOG" && { echo FAILED; exit 1; }; sleep 1; done
MOBS="t_rex velociraptor spinosaurus triceratops stegosaurus ankylosaurus brachiosaurus parasaurolophus megalodon pteranodon"
for m in $MOBS; do cmd "summon dinosaurs:$m 0 120 0"; done
sleep 5
for b in plains jungle swamp taiga birch_forest savanna stony_peaks windswept_hills meadow; do
  cmd "locate biome minecraft:$b" 4
  line=$(grep "The nearest minecraft:$b is at" "$LOG" | tail -1)
  xyz=$(echo "$line" | sed -E 's/.*at \[(-?[0-9]+), (~|-?[0-9]+), (-?[0-9]+)\].*/\1 \3/')
  if [ -n "$xyz" ]; then set -- $xyz; cmd "forceload add $(( $1 - 48 )) $(( $2 - 48 )) $(( $1 + 48 )) $(( $2 + 48 ))" 6; fi
done
sleep 20
for m in $MOBS; do cmd "execute if entity @e[type=dinosaurs:$m]" 0.5; done
cmd "kill @e[type=dinosaurs:t_rex]" 2
cmd "execute if entity @e[type=minecraft:item]" 1
cmd "stop" 15
grep -E "System chat|Exception|ERROR|Caused by" "$LOG" | grep -v "Can't keep up" | tail -60
