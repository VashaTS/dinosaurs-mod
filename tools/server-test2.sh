#!/usr/bin/env bash
# Second smoke test: loot tables and entities ticking for 30 s in a force-loaded chunk.
set -u
cd "$(dirname "$0")/.."
LOG=/tmp/dino-server2.log; FIFO=/tmp/dino-in2
rm -f "$FIFO"; mkfifo "$FIFO"
exec 3<>"$FIFO"
./gradlew --no-daemon runServer <&3 > "$LOG" 2>&1 &
cmd() { echo "$1" >&3; sleep "${2:-0.3}"; }
for i in $(seq 1 300); do grep -q "Done (" "$LOG" && break; grep -q "BUILD FAILED" "$LOG" && { echo FAILED; exit 1; }; sleep 1; done
MOBS="t_rex velociraptor spinosaurus triceratops stegosaurus ankylosaurus brachiosaurus parasaurolophus megalodon pteranodon"
cmd "forceload add -32 -32 32 32" 5
for m in $MOBS; do cmd "loot spawn 0 200 0 loot dinosaurs:entities/$m" 0.5; done
cmd "kill @e[type=item]" 1
for m in $MOBS; do cmd "execute positioned 0 0 0 positioned over motion_blocking_no_leaves run summon dinosaurs:$m ~ ~ ~" 0.5; done
cmd "execute positioned 0 0 0 positioned over motion_blocking_no_leaves run summon minecraft:zombie ~ ~ ~" 0.5
sleep 30
for m in $MOBS; do cmd "execute if entity @e[type=dinosaurs:$m]" 0.5; done
cmd "data get entity @e[type=dinosaurs:pteranodon,limit=1] Pos" 1
cmd "data get entity @e[type=dinosaurs:t_rex,limit=1] Health" 1
cmd "stop" 15
grep -E "System chat|Exception|ERROR|Caused by" "$LOG" | tail -50
