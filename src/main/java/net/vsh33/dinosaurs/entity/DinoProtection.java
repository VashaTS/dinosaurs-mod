package net.vsh33.dinosaurs.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.compat.TrinketsCompat;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Amulet of Dino Protection: hostile dinosaurs ignore a player who carries the amulet (anywhere in the inventory,
 * or in a Trinkets slot when Trinkets is installed) until that player attacks that particular dinosaur.
 * Each hostile dinosaur keeps its own set of players that provoked it.
 */
public final class DinoProtection {
	private static final boolean TRINKETS = FabricLoader.getInstance().isModLoaded("trinkets");
	private static boolean trinketsFailed;

	private final Set<UUID> provokedBy = new HashSet<>();

	public static boolean hasAmulet(Player player) {
		if (player.getInventory().contains(stack -> stack.getItem() == ModItems.AMULET_OF_DINO_PROTECTION)) {
			return true;
		}
		if (TRINKETS && !trinketsFailed) {
			try {
				return TrinketsCompat.isEquipped(player, ModItems.AMULET_OF_DINO_PROTECTION);
			} catch (Throwable t) {
				trinketsFailed = true;
				Dinosaurs.LOGGER.warn("Trinkets integration failed, only the normal inventory will be checked for the amulet", t);
			}
		}
		return false;
	}

	/** Called whenever the dinosaur is damaged: remembers the player who attacked it. */
	public void onHurt(DamageSource source) {
		if (source.getEntity() instanceof Player player) {
			provokedBy.add(player.getUUID());
		}
	}

	/** True if the dinosaur must leave this target alone (protected by the amulet and not provoked). */
	public boolean isShielded(LivingEntity target) {
		return target instanceof Player player && !provokedBy.contains(player.getUUID()) && hasAmulet(player);
	}

	/** Drops a target that is protected, e.g. one assigned by a pack mate's alert. Call from aiStep on the server. */
	public void tick(Mob mob) {
		if (mob.tickCount % 5 == 0 && mob.getTarget() != null && isShielded(mob.getTarget())) {
			mob.setTarget(null);
		}
	}
}
