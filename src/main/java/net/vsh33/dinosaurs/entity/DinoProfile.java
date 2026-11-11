package net.vsh33.dinosaurs.entity;

import net.minecraft.sounds.SoundEvent;

/** Per-species sound and behaviour settings shared by the dinosaur entity classes. */
public record DinoProfile(SoundEvent ambient, SoundEvent hurt, SoundEvent death, SoundEvent step, float pitch, float volume, boolean neutral, boolean packHunter, int xp) {
}
