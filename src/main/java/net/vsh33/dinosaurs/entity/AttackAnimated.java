package net.vsh33.dinosaurs.entity;

/** Lets the client renderer open the jaw for a few ticks after a bite. */
public interface AttackAnimated {
	byte ATTACK_EVENT = 4;

	int dinosaurs$getAttackTicks();
}
